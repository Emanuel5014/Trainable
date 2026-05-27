package com.emanuel5014.trainable.util.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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

            if (folderUriString.isNullOrEmpty()) {
                // Should not happen if UI enforces it, but handle it gracefully
                return Result.failure()
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
            val fileName = "Trainable_AutoBackup_${dateFormat.format(Date())}.zip"

            val uri = Uri.parse(folderUriString)
            val success = backupManager.exportDatabaseToFolder(uri, fileName, includeImages)
            if (success) {
                cleanupOldBackupsSaf(uri, maxBackups)
            } else {
                return Result.retry()
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun cleanupOldBackups(backupDir: File, maxBackups: Int) {
        // No longer used for internal backups, but kept for potential cleanup of legacy files
        val backups = backupDir.listFiles { file -> file.extension == "zip" }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (backups.size > maxBackups) {
            backups.drop(maxBackups).forEach { it.delete() }
        }
    }

    private fun cleanupOldBackupsSaf(folderUri: Uri, maxBackups: Int) {
        try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri,
                DocumentsContract.getTreeDocumentId(folderUri)
            )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )

            val backups = mutableListOf<BackupFileInfo>()

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameColumn)
                    if (name.startsWith("Trainable_AutoBackup_") && name.endsWith(".zip")) {
                        backups.add(
                            BackupFileInfo(
                                id = cursor.getString(idColumn),
                                lastModified = cursor.getLong(modifiedColumn)
                            )
                        )
                    }
                }
            }

            if (backups.size > maxBackups) {
                backups.sortByDescending { it.lastModified }
                backups.drop(maxBackups).forEach { oldBackup ->
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, oldBackup.id)
                    DocumentsContract.deleteDocument(context.contentResolver, docUri)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private data class BackupFileInfo(val id: String, val lastModified: Long)

    companion object {
        const val WORK_NAME = "auto_backup_work"

        fun schedule(
            context: Context, 
            frequencyDays: Int = 1, 
            policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE
        ) {
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
                policy,
                backupRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
