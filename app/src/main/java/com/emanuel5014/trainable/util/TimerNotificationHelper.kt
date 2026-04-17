package com.emanuel5014.trainable.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val runningChannelId = "rest_timer_running_channel_v1"
    private val finishedChannelId = "rest_timer_finished_channel_v1"
    private val notificationId = 1001

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
        }
    }

    /**
     * Starts or updates the timer notification.
     * Uses Android's internal Chronometer to update the countdown in the System UI
     * without requiring a notify() call every second.
     */
    fun startOrUpdateTimerNotification(remainingSeconds: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, TimerNotificationReceiver::class.java).apply { action = TimerNotificationReceiver.ACTION_SKIP }
        val skipPendingIntent = PendingIntent.getBroadcast(context, 1, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val addIntent = Intent(context, TimerNotificationReceiver::class.java).apply { action = TimerNotificationReceiver.ACTION_ADD_30S }
        val addPendingIntent = PendingIntent.getBroadcast(context, 2, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

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
            .setWhen(System.currentTimeMillis() + (remainingSeconds * 1000L))
            .addAction(0, "+30s", addPendingIntent)
            .addAction(0, context.getString(R.string.skip_rest), skipPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
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

        val notification = NotificationCompat.Builder(context, finishedChannelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(context.getString(R.string.rest_timer))
            .setContentText(context.getString(R.string.rest_end))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(0, context.getString(R.string.dismiss), dismissPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun cancelTimer() {
        notificationManager.cancel(notificationId)
    }
}
