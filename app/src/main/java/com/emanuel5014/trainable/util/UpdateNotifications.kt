package com.emanuel5014.trainable.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.remote.GitHubRelease

/** The updater's notifications: download progress (the foreground service's), ready to install, failed. */
object UpdateNotifications {

    const val PROGRESS_ID = 2010
    private const val RESULT_ID = 2011
    private const val CHANNEL_ID = "app_update_channel"

    /** Tapping a notification opens the app with the update dialog on screen. */
    const val ACTION_SHOW_UPDATE = "com.emanuel5014.trainable.action.SHOW_UPDATE"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.update_channel_desc)
                setShowBadge(false)
            }
        )
    }

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        PROGRESS_ID,
        Intent(context, MainActivity::class.java)
            .setAction(ACTION_SHOW_UPDATE)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** [downloaded]/[total] in bytes; a null fraction (size unknown or checking the file) shows a moving bar. */
    fun progress(context: Context, release: GitHubRelease, downloaded: Long, total: Long, verifying: Boolean): Notification {
        ensureChannel(context)
        val cancel = PendingIntent.getService(
            context,
            1,
            Intent(context, UpdateDownloadService::class.java).setAction(UpdateDownloadService.ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = when {
            verifying -> context.getString(R.string.update_verifying)
            total > 0 -> context.getString(
                R.string.update_progress_of,
                Formatter.formatShortFileSize(context, downloaded),
                Formatter.formatShortFileSize(context, total)
            )
            else -> Formatter.formatShortFileSize(context, downloaded)
        }
        val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(context.getString(R.string.update_notification_downloading, release.tagName))
            .setContentText(text)
            .setProgress(100, percent, verifying || total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(context))
            .apply {
                if (!verifying) addAction(0, context.getString(R.string.update_cancel_download), cancel)
            }
            .build()
    }

    /** Opens Android's installer confirmation directly (the app was in the background when it was ready). */
    fun showReadyToInstall(context: Context, release: GitHubRelease, confirm: Intent) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            RESULT_ID,
            confirm,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notify(
            context,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(context.getString(R.string.update_notification_ready))
                .setContentText(context.getString(R.string.update_notification_ready_text, release.tagName))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        )
    }

    fun showFailed(context: Context, release: GitHubRelease) {
        ensureChannel(context)
        notify(
            context,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(context.getString(R.string.update_notification_failed))
                .setContentText(context.getString(R.string.update_notification_failed_text, release.tagName))
                .setContentIntent(openAppIntent(context))
                .setAutoCancel(true)
                .build()
        )
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(RESULT_ID)
    }

    // Without the notification permission Android drops it; the dialog in the app still shows the state
    @SuppressLint("MissingPermission")
    private fun notify(context: Context, notification: Notification) {
        runCatching { NotificationManagerCompat.from(context).notify(RESULT_ID, notification) }
    }
}
