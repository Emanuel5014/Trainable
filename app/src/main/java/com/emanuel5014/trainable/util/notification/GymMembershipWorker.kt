package com.emanuel5014.trainable.util.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class GymMembershipWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userPrefsRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val enabled = userPrefsRepository.gymMembershipExpiryNotificationsEnabled.first()
        if (!enabled) return Result.success()

        val expiryDate = userPrefsRepository.gymMembershipExpiryDate.first() ?: return Result.success()
        val daysBefore = userPrefsRepository.gymMembershipExpiryNotificationDaysBefore.first()
        val lastNotified = userPrefsRepository.lastNotifiedExpiryDate.first()

        // Don't notify again for the same expiry date if already notified
        if (lastNotified == expiryDate) return Result.success()

        val today = System.currentTimeMillis()
        val diff = expiryDate - today
        val daysLeft = TimeUnit.MILLISECONDS.toDays(diff).toInt()

        if (daysLeft in 0..daysBefore) {
            sendNotification(daysLeft)
            userPrefsRepository.setLastNotifiedExpiryDate(expiryDate)
        }

        return Result.success()
    }

    private fun sendNotification(daysLeft: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "gym_membership_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.gym_membership_notifications)
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(context.getString(R.string.gym_membership_expiring))
            .setContentText(context.getString(R.string.gym_membership_expiring_desc, daysLeft))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2001, notification)
    }

    companion object {
        private const val WORK_NAME = "gym_membership_expiry_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<GymMembershipWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
