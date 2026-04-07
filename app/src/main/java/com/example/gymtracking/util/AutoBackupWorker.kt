package com.example.gymtracking.util

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.gymtracking.data.repository.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupManager: BackupManager,
    private val userPrefsRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val folderUri = userPrefsRepository.autoBackupFolderUri.first()
            val maxBackups = userPrefsRepository.autoBackupMaxCount.first()

            if (folderUri == null) {
                val internalBackupDir = File(context.filesDir, "auto_backups")
                if (!internalBackupDir.exists()) internalBackupDir.mkdirs()
                cleanupOldBackups(internalBackupDir, maxBackups)
                createBackupToFile(internalBackupDir)
            } else {
                val uri = Uri.parse(folderUri)
                val success = exportToCustomFolder(uri, maxBackups)
                if (!success) return Result.retry()
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun createBackupToFile(backupDir: File): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
        val fileName = "Trainable_AutoBackup_${dateFormat.format(Date())}.zip"
        val backupFile = File(backupDir, fileName)
        return backupManager.exportDatabaseToFile(backupFile)
    }

    private suspend fun exportToCustomFolder(folderUri: Uri, maxBackups: Int): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
            val fileName = "Trainable_AutoBackup_${dateFormat.format(Date())}.zip"
            
            val children = context.contentResolver.query(folderUri, null, null, null, null)
            val existingBackups = mutableListOf<Pair<String, Long>>()
            
            children?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)
                    if (name.startsWith("Trainable_AutoBackup_") && name.endsWith(".zip")) {
                        existingBackups.add(name to cursor.getLong(cursor.getColumnIndex("_id")))
                    }
                }
            }

            existingBackups.sortedBy { it.first }.take(kotlin.math.max(0, existingBackups.size - maxBackups)).forEach { (_, id) ->
                try {
                    context.contentResolver.delete(Uri.withAppendedPath(folderUri, id.toString()), null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(folderUri, flags)
            
            val dbFile = context.getDatabasePath("gym_tracking_database")
            val walFile = context.getDatabasePath("gym_tracking_database-wal")
            val shmFile = context.getDatabasePath("gym_tracking_database-shm")
            
            context.contentResolver.openOutputStream(folderUri)?.use { fos ->
                ZipOutputStream(fos).use { zos ->
                    val filesToZip = listOfNotNull(
                        dbFile.takeIf { it.exists() },
                        walFile.takeIf { it.exists() },
                        shmFile.takeIf { it.exists() }
                    )

                    for (file in filesToZip) {
                        FileInputStream(file).use { fis ->
                            val zipEntry = ZipEntry(file.name)
                            zos.putNextEntry(zipEntry)
                            fis.copyTo(zos)
                            zos.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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

            val backupRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                frequencyDays.toLong(), TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)
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
