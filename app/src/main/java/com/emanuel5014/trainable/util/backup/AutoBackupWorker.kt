package com.emanuel5014.trainable.util.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.emanuel5014.trainable.data.backup.AutoBackupOutcome
import com.emanuel5014.trainable.data.backup.AutoBackupRunner
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val runner: AutoBackupRunner
) : CoroutineWorker(context, workerParams) {

    /** The shared run (local folder, then Nextcloud); WorkManager retries when everything failed. */
    override suspend fun doWork(): Result = when (runner.run()) {
        AutoBackupOutcome.Retry -> Result.retry()
        AutoBackupOutcome.Nothing, AutoBackupOutcome.Success -> Result.success()
    }

    companion object {
        const val WORK_NAME = "auto_backup_work"

        fun schedule(
            context: Context, 
            frequencyDays: Int = 1, 
            requiresNetwork: Boolean = false,
            wifiOnly: Boolean = false,
            policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE
        ) {
            val constraintsBuilder = Constraints.Builder()
                .setRequiresBatteryNotLow(true)

            if (requiresNetwork) {
                constraintsBuilder.setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
            }

            // Removed initial delay to allow immediate feedback when enabled
            val backupRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                frequencyDays.toLong(), TimeUnit.DAYS
            )
                .setConstraints(constraintsBuilder.build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                policy,
                backupRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Schedules (or cancels) the single backup job from what the preferences say: it runs at the
         * more frequent of the two destinations, and only waits for a network when Nextcloud is on.
         */
        suspend fun scheduleFromPreferences(
            context: Context,
            prefs: UserPreferencesRepository,
            policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE
        ) {
            val localEnabled = prefs.autoBackupEnabled.first()
            val nextcloudEnabled = prefs.nextcloudAutoBackupEnabled.first()
            if (!localEnabled && !nextcloudEnabled) {
                cancel(context)
                return
            }
            val localFrequency = prefs.autoBackupFrequency.first()
            val nextcloudFrequency = prefs.nextcloudAutoBackupFrequency.first()
            val frequency = when {
                localEnabled && nextcloudEnabled -> minOf(localFrequency, nextcloudFrequency)
                localEnabled -> localFrequency
                else -> nextcloudFrequency
            }
            schedule(
                context = context,
                frequencyDays = frequency,
                requiresNetwork = nextcloudEnabled,
                wifiOnly = prefs.nextcloudWifiOnly.first(),
                policy = policy
            )
        }
    }
}
