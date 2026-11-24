package com.emanuel5014.trainable.util.notification

import com.emanuel5014.trainable.ui.screens.dashboard.LiveWorkoutSnapshot
import com.emanuel5014.trainable.ui.screens.dashboard.SavedTimerChange
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
import com.emanuel5014.trainable.util.TimerAdjustment
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
import com.emanuel5014.trainable.data.repository.WorkoutRepository

@Singleton
class TimerNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefsRepository: UserPreferencesRepository,
    workoutRepository: WorkoutRepository
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

    /** The live card of the whole workout: the rest notification's id, so the two can never show together. */
    private val liveChannelId = "workout_live_channel_v1"
    private val liveRenderer = LiveCardRenderer(context)
    private val liveLock = Any()
    /** The "workout live card" setting. */
    @Volatile private var liveEnabled = false
    /** What the open workout last said of its sets; kept in the preferences so a recreated process can redraw it. */
    @Volatile private var liveCard: WorkoutLiveCard? = null
    /** The timers as the calls below left them, kept also without a live card so turning it on mid-rest works. */
    @Volatile private var liveTimers = LiveTimers()
    /** A button of the card found no open workout: the card says to open the app until the workout speaks again. */
    @Volatile private var liveNeedsApp = false
    /** What the card was last drawn from, so nothing is posted again when nothing changed. */
    private var liveDrawn: LiveCardInput? = null
    private var liveRenderJob: kotlinx.coroutines.Job? = null
    /** The "separate alert at timer end" setting, and the finished state whose plain alert is on screen. */
    @Volatile private var liveSeparateAlert = true
    private var liveAlertMode: LiveMode? = null
    private val liveAlertNotificationId = 1004

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** What the + and - buttons of the timer notifications move the countdown by (Workout settings). */
    @Volatile private var addSeconds = TimerAdjustment.DEFAULT_ADD_SECONDS
    @Volatile private var subtractSeconds = TimerAdjustment.DEFAULT_SUBTRACT_SECONDS

    /** Which buttons the timer notifications carry (Workout settings). */
    @Volatile private var showTimeButtons = true
    @Volatile private var showSkipButton = true
    @Volatile private var addEnabled = true
    @Volatile private var subtractEnabled = true

    /**
     * The session the rest, warm-up and EMOM notifications belong to, told by the open workout. Kept here, outside
     * the workout screen, because the notifications outlive it: deleting that session from the dashboard or the
     * history must end them too.
     */
    @Volatile var workoutSessionId: Int? = null

    /** The set "Set done" would log, as the last rest notification announced it. */
    @Volatile private var lastNextSetKey: NextSetKey? = null

    init {
        scope.launch {
            workoutRepository.deletedSessions.collect { id ->
                if (id == workoutSessionId || id == liveCard?.sessionId) {
                    cancelTimer()
                    cancelWarmupTimer()
                    cancelEmom()
                    endLiveCard(id)
                    workoutSessionId = null
                }
            }
        }
        // Read now, not later: a button pressed after the process was recreated must find the card it pressed
        kotlinx.coroutines.runBlocking {
            liveEnabled = userPrefsRepository.workoutLiveCard.first()
            liveCard = WorkoutLiveCard.decode(userPrefsRepository.workoutLiveCardState.first())
        }
        scope.launch { userPrefsRepository.workoutLiveCardAlert.collect { liveSeparateAlert = it } }
        scope.launch {
            userPrefsRepository.workoutLiveCard.collect { enabled ->
                val was = liveEnabled
                liveEnabled = enabled
                if (was && !enabled) dropLiveCard()
            }
        }
        scope.launch { userPrefsRepository.timerAddSeconds.collect { addSeconds = it } }
        scope.launch { userPrefsRepository.timerSubtractSeconds.collect { subtractSeconds = it } }
        scope.launch { userPrefsRepository.timerShowTimeButtons.collect { showTimeButtons = it } }
        scope.launch { userPrefsRepository.timerShowSkipButton.collect { showSkipButton = it } }
        scope.launch { userPrefsRepository.timerAddEnabled.collect { addEnabled = it } }
        scope.launch { userPrefsRepository.timerSubtractEnabled.collect { subtractEnabled = it } }
    }
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

    private fun emomPendingIntent(action: EmomAction): PendingIntent {
        val (name, requestCode) = when (action) {
            EmomAction.DONE -> TimerNotificationReceiver.ACTION_EMOM_DONE to 21
            EmomAction.PAUSE -> TimerNotificationReceiver.ACTION_EMOM_PAUSE to 22
            EmomAction.RESUME -> TimerNotificationReceiver.ACTION_EMOM_RESUME to 23
            EmomAction.STOP -> TimerNotificationReceiver.ACTION_EMOM_STOP to 24
            EmomAction.BOUNDARY -> TimerNotificationReceiver.ACTION_EMOM_BOUNDARY to 25
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
        liveTimers = liveTimers.copy(emom = info, emomFinishedTitle = null, emomFinishedText = null)
        if (isLive()) return requestLiveRender()
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
                .setWhen(info.countdownEndsAt!!)
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
        liveTimers = liveTimers.copy(emom = null, emomFinishedTitle = title, emomFinishedText = text)
        if (isLive()) return renderLive(LiveCardAlert(emomFinishedChannelId, NotificationCompat.DEFAULT_ALL, silenceVibration = false))
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
        val pendingIntent = emomPendingIntent(EmomAction.BOUNDARY)
        try {
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (_: SecurityException) {
            AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelEmomBoundaryAlarm() {
        alarmManager.cancel(emomPendingIntent(EmomAction.BOUNDARY))
    }

    fun cancelEmom() {
        liveTimers = liveTimers.copy(emom = null, emomFinishedTitle = null, emomFinishedText = null)
        if (isLive()) {
            cancelEmomBoundaryAlarm()
            return requestLiveRender()
        }
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

            // The live card of the whole workout: silent like the running timers, its alerts use their channels
            val liveChannel = NotificationChannel(
                liveChannelId,
                context.getString(R.string.live_card_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.timer_notifications_desc)
                setShowBadge(false)
                setSound(null, null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(liveChannel)
        }
    }

    /**
     * Starts or updates the timer notification.
     * Uses Android's internal Chronometer to update the countdown in the System UI
     * without requiring a notify() call every second.
     */
    /** "Next: Squat - Set 2: 60 kg × 8", or null when there is no next set to announce. */
    private fun nextSetLabelOf(
        exerciseName: String?, nextSetNumber: Int?, nextSetWeight: Float?, nextSetReps: Int?, previousReps: Int?,
        weightUnit: String?, nextSetRepsLabel: String?, nextSetDetail: String?, nextSetText: String?
    ): String? {
    return if (exerciseName != null && nextSetNumber != null && nextSetText != null) {
        context.getString(R.string.notification_next_set_text, exerciseName, nextSetNumber, nextSetText)
    } else if (exerciseName != null && nextSetNumber != null && nextSetWeight != null && (nextSetReps != null || nextSetRepsLabel != null) && weightUnit != null) {
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
    }

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
        nextSetDetail: String? = null,
        /** Replaces weight × reps for a timed set ("45s · 20 reps"). */
        nextSetText: String? = null,
        /** The set "Set done" may log once the rest ends; null hides that button. */
        nextSetKey: NextSetKey? = null
    ) {
        var triggerTime = System.currentTimeMillis() + (remainingSeconds * 1000L)
        lastNextSetKey = nextSetKey
        val restTotal = if (totalSeconds != null && totalSeconds > 0) totalSeconds else remainingSeconds
        // The per-second redraws of a running rest name the same end give or take a second: keep the one on the card
        liveTimers.restEndsAtMillis?.let { shown -> if (kotlin.math.abs(shown - triggerTime) < 1500 && liveTimers.restTotalSeconds == restTotal) triggerTime = shown }
        liveTimers = liveTimers.copy(restEndsAtMillis = triggerTime, restTotalSeconds = restTotal, restFinished = false)
        if (isLive()) {
            cancelCustomVibration()
            if (scheduleAlarm) scheduleRestFinish(triggerTime, sessionId)
            return requestLiveRender()
        }
        
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

        val subtractIntent = Intent(context, TimerNotificationReceiver::class.java).apply {
            action = TimerNotificationReceiver.ACTION_SUBTRACT
            putExtra(TimerNotificationReceiver.EXTRA_SESSION_ID, sessionId)
        }
        val subtractPendingIntent = PendingIntent.getBroadcast(context, 5, subtractIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextSetLabel = nextSetLabelOf(exerciseName, nextSetNumber, nextSetWeight, nextSetReps, previousReps,
            weightUnit, nextSetRepsLabel, nextSetDetail, nextSetText)
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
            .apply {
                if (showTimeButtons && subtractEnabled) addAction(0, "−${subtractSeconds}s", subtractPendingIntent)
                if (showTimeButtons && addEnabled) addAction(0, "+${addSeconds}s", addPendingIntent)
                if (showSkipButton) addAction(0, context.getString(R.string.skip_rest), skipPendingIntent)
            }
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

        if (scheduleAlarm) scheduleRestFinish(triggerTime, sessionId)
    }

    /** The alarm that ends the rest on time, also with the screen off. */
    private fun scheduleRestFinish(triggerTime: Long, sessionId: Int) {
        val finishIntent = Intent(context, TimerNotificationReceiver::class.java).apply {
            action = TimerNotificationReceiver.ACTION_TIMER_FINISHED
            putExtra(TimerNotificationReceiver.EXTRA_SESSION_ID, sessionId)
        }
        val finishPendingIntent = PendingIntent.getBroadcast(context, 4, finishIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerTime, finishPendingIntent)
    }

    /** The rest is over; [next] (from the open workout) is the set the card now announces. */
    fun showRestFinished(next: RestNextSet? = null) {
        if (next != null) {
            lastNextSetLabel = nextSetLabelOf(next.exerciseName, next.setNumber, next.weight, next.reps, next.previousReps,
                next.weightUnit, next.repsLabel, next.detail, next.text)
            lastNextSetKey = next.key
        }
        val alreadyFinished = liveTimers.restFinished
        liveTimers = liveTimers.copy(restEndsAtMillis = null, restFinished = true)
        if (isLive()) {
            // The alarm and the open workout both say the rest is over: it rings once
            if (alreadyFinished) return renderLive(null)
            val (custom, seconds) = lockscreenVibration()
            if (custom) startCustomVibration(seconds)
            val defaults = if (custom) NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS else NotificationCompat.DEFAULT_ALL
            return renderLive(LiveCardAlert(finishedChannelId, defaults, silenceVibration = custom))
        }
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

        val (useCustomVibration, durationSeconds) = lockscreenVibration()

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
            .apply {
                if (showTimeButtons && addEnabled) addAction(0, "+${addSeconds}s", restActionIntent(TimerNotificationReceiver.ACTION_REST_MORE, 6))
                if (lastNextSetKey != null) addAction(0, context.getString(R.string.next_set_done), restActionIntent(TimerNotificationReceiver.ACTION_REST_NEXT_SET, 7))
            }
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

    /** With the phone locked or the screen off, the long vibration of the settings (when it has a length) replaces the usual one. */
    private fun lockscreenVibration(): Pair<Boolean, Int> {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isLockedOrScreenOff = keyguardManager?.isKeyguardLocked == true || powerManager?.isInteractive == false
        if (!isLockedOrScreenOff) return false to 0
        val seconds = kotlinx.coroutines.runBlocking { userPrefsRepository.timerFinishedLockscreenVibrationDuration.first() }
        return (seconds > 0) to seconds
    }

    private fun restActionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimerNotificationReceiver::class.java).apply {
            this.action = action
            workoutSessionId?.let { putExtra(TimerNotificationReceiver.EXTRA_SESSION_ID, it) }
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** A "Set done" / "+s" press reached no open workout: say so, with no button that would do nothing again. */
    fun showOpenAppToContinue() {
        cancelCustomVibration()
        if (isLive()) {
            liveNeedsApp = true
            return renderLive(null)
        }
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text = context.getString(R.string.open_app_to_continue)
        val notification = NotificationCompat.Builder(context, finishedChannelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(context.getString(R.string.rest_timer))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Redraws or removes the rest / warm-up notification after a button pressed outside the workout ([SavedTimers]),
     * announcing what the workout last said (its dashboard copy), so the card looks as if the workout had done it.
     */
    suspend fun followSavedTimer(change: SavedTimerChange) {
        val end = change.end
        if (end == null) {
            if (change.warmup) cancelWarmupTimer() else cancelTimer()
            return
        }
        if (!userPrefsRepository.timerNotificationsEnabled.first() || !hasNotificationPermission()) return
        val remaining = ((end - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
        if (change.warmup) {
            startOrUpdateWarmupTimerNotification(remaining, change.total)
            return
        }
        val next = LiveWorkoutSnapshot.decode(userPrefsRepository.liveWorkoutSnapshot.first())
            ?.takeIf { it.sessionId == change.sessionId }?.next
        startOrUpdateTimerNotification(
            remaining, change.sessionId,
            next?.exerciseName, next?.setNumber, next?.weight, next?.reps, next?.previousReps, next?.weightUnit,
            totalSeconds = change.total,
            nextSetRepsLabel = next?.repsLabel,
            nextSetDetail = next?.detail,
            nextSetText = next?.text,
            nextSetKey = next?.keyToken?.let { NextSetKey.parse(it) }
        )
    }

    fun cancelTimer() {
        liveTimers = liveTimers.copy(restEndsAtMillis = null, restFinished = false)
        if (isLive()) {
            cancelFinishAlarm()
            cancelCustomVibration()
            return requestLiveRender()
        }
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
        var triggerTime = System.currentTimeMillis() + (remainingSeconds * 1000L)
        val warmupTotal = if (totalSeconds != null && totalSeconds > 0) totalSeconds else remainingSeconds
        liveTimers.warmupEndsAtMillis?.let { shown -> if (kotlin.math.abs(shown - triggerTime) < 1500 && liveTimers.warmupTotalSeconds == warmupTotal) triggerTime = shown }
        liveTimers = liveTimers.copy(warmupEndsAtMillis = triggerTime, warmupTotalSeconds = warmupTotal, warmupFinished = false)
        if (isLive()) {
            if (scheduleAlarm) scheduleWarmupFinish(triggerTime)
            return requestLiveRender()
        }

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

        val subtractIntent = Intent(context, TimerNotificationReceiver::class.java).apply {
            action = TimerNotificationReceiver.ACTION_WARMUP_SUBTRACT
        }
        val subtractPendingIntent = PendingIntent.getBroadcast(
            context, 15, subtractIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
            .apply {
                if (showTimeButtons && subtractEnabled) addAction(0, "−${subtractSeconds}s", subtractPendingIntent)
                if (showTimeButtons && addEnabled) addAction(0, "+${addSeconds}s", addPendingIntent)
                if (showSkipButton) addAction(0, context.getString(R.string.skip_rest), skipPendingIntent)
            }
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

        if (scheduleAlarm) scheduleWarmupFinish(triggerTime)
    }

    private fun scheduleWarmupFinish(triggerTime: Long) {
        val finishIntent = Intent(context, TimerNotificationReceiver::class.java).apply {
            action = TimerNotificationReceiver.ACTION_WARMUP_FINISHED
        }
        val finishPendingIntent = PendingIntent.getBroadcast(
            context, 14, finishIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerTime, finishPendingIntent)
    }

    fun showWarmupTimerFinished() {
        val warmupAlreadyFinished = liveTimers.warmupFinished
        liveTimers = liveTimers.copy(warmupEndsAtMillis = null, warmupFinished = true)
        if (isLive() && warmupAlreadyFinished) return renderLive(null)
        if (isLive()) return renderLive(LiveCardAlert(warmupFinishedChannelId, NotificationCompat.DEFAULT_ALL, silenceVibration = false))
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
        liveTimers = liveTimers.copy(warmupEndsAtMillis = null, warmupFinished = false)
        if (isLive()) {
            cancelWarmupFinishAlarm()
            return requestLiveRender()
        }
        notificationManager.cancel(warmupNotificationId)
        cancelWarmupFinishAlarm()
    }

    fun cancelWarmupFinishAlarm() {
        val finishIntent = Intent(context, TimerNotificationReceiver::class.java).apply { action = TimerNotificationReceiver.ACTION_WARMUP_FINISHED }
        val finishPendingIntent = PendingIntent.getBroadcast(context, 14, finishIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(finishPendingIntent)
    }

    // ---- Live card of the whole workout ----

    /** A live card is on: the timer calls above draw in it instead of in their own notifications. */
    fun isLive(): Boolean = liveEnabled && liveCard != null

    /** What the open workout says of its sets now (sent again on every change). */
    fun showLiveCard(card: WorkoutLiveCard) {
        val previous = liveCard
        liveCard = card
        workoutSessionId = card.sessionId
        liveNeedsApp = false
        // A finished warm-up or EMOM run is over once the workout moved on
        if (previous != null && previous != card) liveTimers = liveTimers.copy(warmupFinished = false, emomFinishedTitle = null, emomFinishedText = null)
        if (previous == null && liveEnabled) {
            // The card takes the place of the warm-up and EMOM notifications (the rest one shares its id)
            notificationManager.cancel(warmupNotificationId)
            notificationManager.cancel(emomNotificationId)
        }
        scope.launch { userPrefsRepository.setWorkoutLiveCardState(card.encode()) }
        if (liveEnabled) requestLiveRender()
    }

    /** The workout [sessionId] is over (finished, cancelled, deleted) or the setting went off: its card goes. */
    fun endLiveCard(sessionId: Int) {
        val card = liveCard ?: return
        if (card.sessionId != sessionId) return
        dropLiveCard()
        liveCard = null
        liveTimers = LiveTimers()
        scope.launch { userPrefsRepository.setWorkoutLiveCardState(null) }
    }

    /** Takes the drawn card away (the notification shares the rest's id). */
    private fun dropLiveCard() {
        synchronized(liveLock) {
            liveRenderJob?.cancel()
            liveRenderJob = null
            if (liveDrawn != null) notificationManager.cancel(notificationId)
            liveDrawn = null
            if (liveAlertMode != null) notificationManager.cancel(liveAlertNotificationId)
            liveAlertMode = null
        }
    }

    /** Draws the card a moment later: a tick in the workout calls several of the functions above in a row. */
    private fun requestLiveRender() {
        synchronized(liveLock) {
            liveRenderJob?.cancel()
            liveRenderJob = scope.launch {
                delay(LIVE_RENDER_DELAY_MILLIS)
                renderLive(null)
            }
        }
    }

    /**
     * Draws what is pending now. A broadcast receiver calls it before it lets the process go, so the card it changed is
     * drawn even if the process is frozen right after.
     */
    fun flushLiveCard() {
        if (isLive()) renderLive(null)
    }

    /** Draws the card now; [alert] rings once on its channel (end of a rest, a warm-up, an EMOM run). */
    private fun renderLive(alert: LiveCardAlert?) {
        synchronized(liveLock) {
            liveRenderJob?.cancel()
            liveRenderJob = null
            val card = liveCard ?: return
            if (!liveEnabled || !hasNotificationPermission()) return
            val timers = liveTimers
            val mode = liveModeOf(card, timers)
            val settings = LiveButtonSettings(showTimeButtons, addEnabled, subtractEnabled, showSkipButton)
            val input = LiveCardInput(
                card = card,
                timers = timers,
                mode = mode,
                buttons = liveButtonsOf(mode, card, settings, lastNextSetKey, liveNeedsApp),
                segments = liveSegmentsOf(mode, card, timers),
                needsApp = liveNeedsApp,
                addSeconds = addSeconds,
                subtractSeconds = subtractSeconds,
                // Only the finished card writes the time down; elsewhere the system counts it
                nowMillis = if (mode == LiveMode.ALL_DONE) (liveDrawn?.takeIf { it.mode == LiveMode.ALL_DONE }?.nowMillis ?: System.currentTimeMillis()) else 0L,
                countdownPermille = when (mode) {
                    LiveMode.REST -> timers.restEndsAtMillis?.let { countdownPermille(it, timers.restTotalSeconds) }
                    LiveMode.WARMUP -> timers.warmupEndsAtMillis?.let { countdownPermille(it, timers.warmupTotalSeconds) }
                    else -> null
                }
            )
            // The plain alert of a finished timer goes as soon as the card shows something else
            if (liveAlertMode != null && liveAlertMode != mode) {
                notificationManager.cancel(liveAlertNotificationId)
                liveAlertMode = null
            }
            if (alert == null && input == liveDrawn) return
            liveDrawn = input
            if (alert != null && liveSeparateAlert) {
                // The card updates in silence; the alert that rings is a plain notification watches forward
                notificationManager.notify(notificationId, liveRenderer.build(input, liveChannelId, null))
                notificationManager.notify(liveAlertNotificationId, liveRenderer.buildAlert(input, alert))
                liveAlertMode = mode
            } else {
                notificationManager.notify(notificationId, liveRenderer.build(input, liveChannelId, alert))
            }
        }
    }

    /**
     * How much of a countdown ending at [endsAt] after [totalSeconds] is gone, 0..1000, in whole seconds: the open
     * workout redraws a running rest every second (from Android 16), and so does the bar.
     */
    private fun countdownPermille(endsAt: Long, totalSeconds: Int): Int {
        if (totalSeconds <= 0) return 0
        val remaining = ((endsAt - System.currentTimeMillis() + 999) / 1000).coerceIn(0, totalSeconds.toLong())
        return ((totalSeconds - remaining) * 1000 / totalSeconds).toInt()
    }

    private companion object {
        const val LIVE_RENDER_DELAY_MILLIS = 60L
    }
}
