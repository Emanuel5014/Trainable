package com.emanuel5014.trainable.util.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.util.WeightUnitConverter
import android.app.KeyguardManager
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.domain.emom.EmomClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import android.os.PowerManager
import android.media.AudioAttributes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** One button of the EMOM notification; [label] is already in the app language. */
data class EmomNotificationAction(val action: TimerNotificationReceiver.EmomAction, val label: String)

/** What the ongoing EMOM notification shows, all text already in the app language. */
data class EmomNotificationInfo(
    val title: String,
    val text: String,
    /** Extra line for the expanded notification where the system has no richer layout. */
    val detail: String?,
    /** Epoch millis at which the countdown reaches zero; null while the run is paused. */
    val countdownEndsAt: Long?,
    /** Seconds of the whole run already elapsed, for the live progress bar. */
    val runElapsedSeconds: Int,
    val rounds: Int,
    val actions: List<EmomNotificationAction>
)

@Singleton
class TimerNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefsRepository: UserPreferencesRepository
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val runningChannelId = "rest_timer_running_channel_v1"
    private val finishedChannelId = "rest_timer_finished_channel_v1"
    private val notificationId = 1001
    private var lastNextSetLabel: String? = null

    private val warmupRunningChannelId = "warmup_timer_running_channel_v1"
    private val warmupFinishedChannelId = "warmup_timer_finished_channel_v1"
    private val warmupNotificationId = 1002

    private val emomRunningChannelId = "emom_running_channel_v1"
    private val emomFinishedChannelId = "emom_finished_channel_v1"
    private val emomNotificationId = 1003

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var vibrationJob: kotlinx.coroutines.Job? = null
    private var vibrator: Vibrator? = null

    private fun getVibrator(): Vibrator? {
        if (vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
        return vibrator
    }

    private fun startCustomVibration(durationSeconds: Int) {
        cancelCustomVibration()
        val v = getVibrator() ?: return
        val durationMillis = durationSeconds * 1000L
        
        vibrationJob = scope.launch {
            val pattern = longArrayOf(0, 1000, 1000) // 1s vibrate, 1s pause
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0), audioAttributes)
            } else {
                v.vibrate(pattern, 0, audioAttributes)
            }
            delay(durationMillis)
            cancelCustomVibration()
        }
    }

    fun cancelCustomVibration() {
        vibrationJob?.cancel()
        vibrationJob = null
        getVibrator()?.cancel()
    }

    /** Haptic signals of an EMOM run: a tick before each round, GO when it starts, and the finish. */
    enum class EmomCue { TICK, GO, DONE }

    fun vibrateEmomCue(cue: EmomCue) {
        val v = getVibrator()?.takeIf { it.hasVibrator() } ?: return
        val effect = when (cue) {
            EmomCue.TICK -> VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
            EmomCue.GO -> VibrationEffect.createWaveform(longArrayOf(0, 260, 90, 260), -1)
            EmomCue.DONE -> VibrationEffect.createWaveform(longArrayOf(0, 160, 90, 160, 90, 420), -1)
        }
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
        @Suppress("DEPRECATION")
        v.vibrate(effect, audioAttributes)
    }

    // ---- EMOM ----

    private fun emomPendingIntent(action: TimerNotificationReceiver.EmomAction): PendingIntent {
        val (name, requestCode) = when (action) {
            TimerNotificationReceiver.EmomAction.DONE -> TimerNotificationReceiver.ACTION_EMOM_DONE to 21
            TimerNotificationReceiver.EmomAction.PAUSE -> TimerNotificationReceiver.ACTION_EMOM_PAUSE to 22
            TimerNotificationReceiver.EmomAction.RESUME -> TimerNotificationReceiver.ACTION_EMOM_RESUME to 23
            TimerNotificationReceiver.EmomAction.STOP -> TimerNotificationReceiver.ACTION_EMOM_STOP to 24
            TimerNotificationReceiver.EmomAction.BOUNDARY -> TimerNotificationReceiver.ACTION_EMOM_BOUNDARY to 25
        }
        val intent = Intent(context, TimerNotificationReceiver::class.java).apply { this.action = name }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun emomContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        return PendingIntent.getActivity(
            context, 20, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Shows or updates the ongoing EMOM notification. The system counts the countdown down on its
     * own (chronometer), so it only needs posting when the round, the phase or the buttons change;
     * from Android 16 it is a live update whose bar has one segment per round.
     */
    fun showEmomNotification(info: EmomNotificationInfo) {
        val builder = NotificationCompat.Builder(context, emomRunningChannelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(info.title)
            .setContentText(info.text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(emomContentIntent())

        if (info.countdownEndsAt != null) {
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(info.countdownEndsAt)
        } else {
            builder.setUsesChronometer(false).setShowWhen(false)
        }
        info.actions.forEach { builder.addAction(0, it.label, emomPendingIntent(it.action)) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val rounds = info.rounds.coerceAtLeast(1)
            val progressStyle = NotificationCompat.ProgressStyle()
                .setStyledByProgress(true)
                .setProgressSegments(List(rounds) { NotificationCompat.ProgressStyle.Segment(EmomClock.MINUTE_SECONDS) })
                .setProgress(info.runElapsedSeconds.coerceIn(0, rounds * EmomClock.MINUTE_SECONDS))
            builder.setStyle(progressStyle).setRequestPromotedOngoing(true)
        } else {
            val expanded = listOfNotNull(info.text, info.detail).joinToString("\n")
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(expanded))
        }

        notificationManager.notify(emomNotificationId, builder.build())
    }

    /** Replaces the ongoing EMOM notification with an alerting one saying the run is over. */
    fun showEmomFinished(title: String, text: String) {
        cancelEmomBoundaryAlarm()
        val notification = NotificationCompat.Builder(context, emomFinishedChannelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(emomContentIntent())
            .build()
        notificationManager.notify(emomNotificationId, notification)
    }

    /** Wakes the workout when the next round starts, even with the screen off. */
    fun scheduleEmomBoundaryAlarm(triggerAtMillis: Long) {
        val pendingIntent = emomPendingIntent(TimerNotificationReceiver.EmomAction.BOUNDARY)
        try {
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (_: SecurityException) {
            AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelEmomBoundaryAlarm() {
        alarmManager.cancel(emomPendingIntent(TimerNotificationReceiver.EmomAction.BOUNDARY))
    }

    fun cancelEmom() {
        notificationManager.cancel(emomNotificationId)
        cancelEmomBoundaryAlarm()
    }

    init {
        createNotificationChannels()
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canPostPromotedNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            notificationManager.canPostPromotedNotifications()
        } else {
            false
        }
    }

    fun isLiveNotificationSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.rest_timer)
            
            // Channel for the ongoing timer (Silent but visible on lock screen)
            val runningChannel = NotificationChannel(
                runningChannelId,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.timer_notifications_desc)
                setShowBadge(false)
                setSound(null, null) // Silent updates
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // Channel for the finished timer (Alerting)
            val finishedChannel = NotificationChannel(
                finishedChannelId,
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.timer_notifications_desc)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(runningChannel)
            notificationManager.createNotificationChannel(finishedChannel)

            // Warmup timer channels
            val warmupName = context.getString(R.string.warmup_timer)
            val warmupRunningChannel = NotificationChannel(
                warmupRunningChannelId,
                warmupName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.warmup_timer_notifications_desc)
                setShowBadge(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val warmupFinishedChannel = NotificationChannel(
                warmupFinishedChannelId,
                warmupName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.warmup_timer_notifications_desc)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(warmupRunningChannel)
            notificationManager.createNotificationChannel(warmupFinishedChannel)

            // EMOM channels: the live countdown stays silent (the haptic cues do the alerting), the finish alerts
            val emomName = context.getString(R.string.emom_notification_channel)
            val emomRunningChannel = NotificationChannel(
                emomRunningChannelId,
                emomName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.timer_notifications_desc)
                setShowBadge(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val emomFinishedChannel = NotificationChannel(
                emomFinishedChannelId,
                emomName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.timer_notifications_desc)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(emomRunningChannel)
            notificationManager.createNotificationChannel(emomFinishedChannel)
        }
    }

    /**
     * Starts or updates the timer notification.
     * Uses Android's internal Chronometer to update the countdown in the System UI
     * without requiring a notify() call every second.
     */
    fun startOrUpdateTimerNotification(
        remainingSeconds: Int,
        sessionId: Int,
        exerciseName: String? = null,
        nextSetNumber: Int? = null,
        nextSetWeight: Float? = null,
        nextSetReps: Int? = null,
        previousReps: Int? = null,
        weightUnit: String? = null,
        totalSeconds: Int? = null,
        scheduleAlarm: Boolean = true,
        nextSetRepsLabel: String? = null,
        nextSetDetail: String? = null
    ) {
        val triggerTime = System.currentTimeMillis() + (remainingSeconds * 1000L)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, TimerNotificationReceiver::class.java).apply { 
            action = TimerNotificationReceiver.ACTION_SKIP
            putExtra(TimerNotificationReceiver.EXTRA_SESSION_ID, sessionId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(context, 1, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val addIntent = Intent(context, TimerNotificationReceiver::class.java).apply { 
            action = TimerNotificationReceiver.ACTION_ADD_30S
            putExtra(TimerNotificationReceiver.EXTRA_SESSION_ID, sessionId)
        }
        val addPendingIntent = PendingIntent.getBroadcast(context, 2, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextSetLabel = if (exerciseName != null && nextSetNumber != null && nextSetWeight != null && (nextSetReps != null || nextSetRepsLabel != null) && weightUnit != null) {
            val formattedWeight = WeightUnitConverter.formatWithUnit(
                WeightUnitConverter.convertDisplay(nextSetWeight, weightUnit),
                weightUnit
            )
            val base = if (nextSetRepsLabel != null || nextSetDetail != null) {
                // Advanced prescription: string reps ("MAX") plus e.g. "75% · Pause 2″"
                context.getString(
                    R.string.notification_next_set_detail,
                    exerciseName, nextSetNumber, formattedWeight, nextSetRepsLabel ?: nextSetReps.toString()
                ) + (nextSetDetail?.let { " · $it" } ?: "")
            } else {
                context.getString(R.string.notification_next_set, exerciseName, nextSetNumber, formattedWeight, nextSetReps)
            }
            if (previousReps != null && previousReps != nextSetReps && nextSetRepsLabel == null) {
                "$base ${context.getString(R.string.notification_last_reps, previousReps)}"
            } else base
        } else null
        lastNextSetLabel = nextSetLabel

        val total = if (totalSeconds != null && totalSeconds > 0) totalSeconds else remainingSeconds
        val elapsed = (total - remainingSeconds).coerceAtLeast(0)
        val progress = if (total > 0) ((elapsed.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

        val notification = NotificationCompat.Builder(context, runningChannelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(context.getString(R.string.rest_timer))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(triggerTime)
            .setContentText(nextSetLabel)
            .addAction(0, "+30s", addPendingIntent)
            .addAction(0, context.getString(R.string.skip_rest), skipPendingIntent)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    val progressStyle = NotificationCompat.ProgressStyle()
                        .setStyledByProgress(true)
                        .setProgress(progress)
                    setStyle(progressStyle)
                    setRequestPromotedOngoing(true)
                } else {
                    setStyle(NotificationCompat.BigTextStyle().bigText(nextSetLabel))
                }
            }
            .build()

        notificationManager.notify(notificationId, notification)
        cancelCustomVibration()

        if (scheduleAlarm) {
            // Schedule Alarm for exact finish
            val finishIntent = Intent(context, TimerNotificationReceiver::class.java).apply { 
                action = TimerNotificationReceiver.ACTION_TIMER_FINISHED
                putExtra(TimerNotificationReceiver.EXTRA_SESSION_ID, sessionId)
            }
            val finishPendingIntent = PendingIntent.getBroadcast(context, 4, finishIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                finishPendingIntent
            )
        }
    }

    fun showRestFinished() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, TimerNotificationReceiver::class.java).apply { action = TimerNotificationReceiver.ACTION_DISMISS }
        val dismissPendingIntent = PendingIntent.getBroadcast(context, 3, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val contentText = if (lastNextSetLabel != null) {
            context.getString(R.string.rest_end) + " — $lastNextSetLabel"
        } else {
            context.getString(R.string.rest_end)
        }

        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isLockedOrScreenOff = keyguardManager?.isKeyguardLocked == true || powerManager?.isInteractive == false

        var useCustomVibration = false
        var durationSeconds = 0

        if (isLockedOrScreenOff) {
            durationSeconds = kotlinx.coroutines.runBlocking {
                userPrefsRepository.timerFinishedLockscreenVibrationDuration.first()
            }
            if (durationSeconds > 0) {
                useCustomVibration = true
            }
        }

        val notificationBuilder = NotificationCompat.Builder(context, finishedChannelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(context.getString(R.string.rest_timer))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, context.getString(R.string.dismiss), dismissPendingIntent)

        if (useCustomVibration) {
            // Override defaults to play sound and lights, but disable standard vibration
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
            notificationBuilder.setVibrate(longArrayOf(0))
            startCustomVibration(durationSeconds)
        } else {
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    fun cancelTimer() {
        notificationManager.cancel(notificationId)
        cancelFinishAlarm()
        cancelCustomVibration()
    }

    fun cancelFinishAlarm() {
        val finishIntent = Intent(context, TimerNotificationReceiver::class.java).apply { action = TimerNotificationReceiver.ACTION_TIMER_FINISHED }
        val finishPendingIntent = PendingIntent.getBroadcast(context, 4, finishIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(finishPendingIntent)
    }

    // ---- Warmup / General Timer ----

    fun startOrUpdateWarmupTimerNotification(
        remainingSeconds: Int,
        totalSeconds: Int? = null,
        scheduleAlarm: Boolean = true
    ) {
        val triggerTime = System.currentTimeMillis() + (remainingSeconds * 1000L)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 10, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, TimerNotificationReceiver::class.java).apply {
            action = TimerNotificationReceiver.ACTION_WARMUP_SKIP
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context, 11, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val addIntent = Intent(context, TimerNotificationReceiver::class.java).apply {
            action = TimerNotificationReceiver.ACTION_WARMUP_ADD_30S
        }
        val addPendingIntent = PendingIntent.getBroadcast(
            context, 12, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val warmupDesc = context.getString(R.string.warmup_timer_description)

        val total = if (totalSeconds != null && totalSeconds > 0) totalSeconds else remainingSeconds
        val elapsed = (total - remainingSeconds).coerceAtLeast(0)
        val progress = if (total > 0) ((elapsed.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100) else 0

        val notification = NotificationCompat.Builder(context, warmupRunningChannelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(context.getString(R.string.warmup_timer))
            .setContentText(warmupDesc)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(triggerTime)
            .addAction(0, "+30s", addPendingIntent)
            .addAction(0, context.getString(R.string.skip_rest), skipPendingIntent)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    val progressStyle = NotificationCompat.ProgressStyle()
                        .setStyledByProgress(true)
                        .setProgress(progress)
                    setStyle(progressStyle)
                    setRequestPromotedOngoing(true)
                } else {
                    setStyle(NotificationCompat.BigTextStyle().bigText(warmupDesc))
                }
            }
            .build()

        notificationManager.notify(warmupNotificationId, notification)

        if (scheduleAlarm) {
            val finishIntent = Intent(context, TimerNotificationReceiver::class.java).apply {
                action = TimerNotificationReceiver.ACTION_WARMUP_FINISHED
            }
            val finishPendingIntent = PendingIntent.getBroadcast(
                context, 14, finishIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager, AlarmManager.RTC_WAKEUP, triggerTime, finishPendingIntent
            )
        }
    }

    fun showWarmupTimerFinished() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 10, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissIntent = Intent(context, TimerNotificationReceiver::class.java).apply {
            action = TimerNotificationReceiver.ACTION_WARMUP_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, 13, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, warmupFinishedChannelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(context.getString(R.string.warmup_timer))
            .setContentText(context.getString(R.string.warmup_timer_finished))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.warmup_timer_finished)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, context.getString(R.string.dismiss), dismissPendingIntent)
            .build()

        notificationManager.notify(warmupNotificationId, notification)
    }

    fun cancelWarmupTimer() {
        notificationManager.cancel(warmupNotificationId)
        cancelWarmupFinishAlarm()
    }

    fun cancelWarmupFinishAlarm() {
        val finishIntent = Intent(context, TimerNotificationReceiver::class.java).apply { action = TimerNotificationReceiver.ACTION_WARMUP_FINISHED }
        val finishPendingIntent = PendingIntent.getBroadcast(context, 14, finishIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(finishPendingIntent)
    }
}
