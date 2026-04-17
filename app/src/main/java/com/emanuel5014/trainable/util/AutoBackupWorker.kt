package com.emanuel5014.trainable.util

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupManager: BackupManager,
    private val userPrefsRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val folderUriString = userPrefsRepository.autoBackupFolderUri.first()
            val maxBackups = userPrefsRepository.autoBackupMaxCount.first()
            val includeImages = userPrefsRepository.autoBackupIncludeImages.first()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
            val fileName = "Trainable_AutoBackup_${dateFormat.format(Date())}.zip"

            if (folderUriString.isNullOrEmpty()) {
                val internalBackupDir = File(context.filesDir, "auto_backups")
                if (!internalBackupDir.exists()) internalBackupDir.mkdirs()
                cleanupOldBackups(internalBackupDir, maxBackups)
                
                val backupFile = File(internalBackupDir, fileName)
                backupManager.exportDatabaseToFile(backupFile, includeImages)
            } else {
                val uri = Uri.parse(folderUriString)
                val success = backupManager.exportDatabaseToFolder(uri, fileName, includeImages)
                // Note: Folder cleanup for SAF is complex, we focus on saving for now
                if (!success) return Result.retry()
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun cleanupOldBackups(backupDir: File, maxBackups: Int) {
        val backups = backupDir.listFiles { file -> file.extension == "zip" }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        backups.drop(maxBackups).forEach { it.delete() }
    }

    companion object {
        const val WORK_NAME = "auto_backup_work"

        fun schedule(context: Context, frequencyDays: Int = 1) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            // Removed initial delay to allow immediate feedback when enabled
            val backupRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                frequencyDays.toLong(), TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                backupRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
