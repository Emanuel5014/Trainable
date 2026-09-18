package com.emanuel5014.trainable.util.backup

import android.content.Context
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConfig
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudWebDavClient
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.util.NextcloudCryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextcloudBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val webDavClient: NextcloudWebDavClient,
    private val userPrefsRepository: UserPreferencesRepository,
    private val cryptoManager: NextcloudCryptoManager
) {

    suspend fun getActiveConfig(): NextcloudConfig? {
        val serverUrl = userPrefsRepository.nextcloudServerUrl.first() ?: return null
        val username = userPrefsRepository.nextcloudUsername.first() ?: return null
        val encryptedHex = userPrefsRepository.nextcloudEncryptedPassword.first() ?: return null
        val ivHex = userPrefsRepository.nextcloudPasswordIv.first() ?: return null
        val remoteFolder = userPrefsRepository.nextcloudRemoteFolder.first()

        val encryptedBytes = decodeHex(encryptedHex) ?: return null
        val ivBytes = decodeHex(ivHex) ?: return null
        val password = cryptoManager.decryptPassword(encryptedBytes, ivBytes) ?: return null

        return NextcloudConfig(
            serverUrl = serverUrl,
            username = username,
            passwordOrToken = password,
            remoteFolder = remoteFolder
        )
    }

    suspend fun backupNow(includeImages: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        val config = getActiveConfig() ?: return@withContext Result.failure(Exception("Nextcloud not configured"))
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
        val fileName = "Trainable_Backup_${dateFormat.format(Date())}.zip"

        val tempFile = backupManager.exportDatabaseToTempFile(fileName, includeImages)
            ?: return@withContext Result.failure(Exception("Failed to generate backup archive"))

        try {
            val uploadResult = webDavClient.uploadFile(config, tempFile, fileName)
            uploadResult
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    suspend fun listBackups(): Result<List<NextcloudBackupFile>> = withContext(Dispatchers.IO) {
        val config = getActiveConfig() ?: return@withContext Result.failure(Exception("Nextcloud not configured"))
        webDavClient.listBackups(config)
    }

    suspend fun restoreBackup(backupFile: NextcloudBackupFile): Result<Unit> = withContext(Dispatchers.IO) {
        val config = getActiveConfig() ?: return@withContext Result.failure(Exception("Nextcloud not configured"))
        val tempFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.zip")

        try {
            val downloadResult = webDavClient.downloadFile(config, backupFile.name, tempFile)
            if (downloadResult.isFailure) return@withContext downloadResult

            val importSuccess = backupManager.importDatabaseFromFile(tempFile)
            if (importSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to import database from downloaded backup"))
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    suspend fun performAutoBackup(maxBackups: Int, includeImages: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val config = getActiveConfig() ?: return@withContext Result.failure(Exception("Nextcloud not configured"))
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
        val fileName = "Trainable_AutoBackup_${dateFormat.format(Date())}.zip"

        val tempFile = backupManager.exportDatabaseToTempFile(fileName, includeImages)
            ?: return@withContext Result.failure(Exception("Failed to generate auto-backup archive"))

        try {
            val uploadResult = webDavClient.uploadFile(config, tempFile, fileName)
            if (uploadResult.isFailure) return@withContext uploadResult

            // Retention policy: cleanup older auto-backups
            cleanupOldNextcloudBackups(config, maxBackups)
            Result.success(Unit)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private suspend fun cleanupOldNextcloudBackups(config: NextcloudConfig, maxBackups: Int) {
        try {
            val listResult = webDavClient.listBackups(config)
            val files = listResult.getOrNull() ?: return

            // Filter auto-backups
            val autoBackups = files.filter { it.name.startsWith("Trainable_AutoBackup_") }
                .sortedByDescending { it.lastModified.time }

            if (autoBackups.size > maxBackups) {
                val toDelete = autoBackups.drop(maxBackups)
                for (oldBackup in toDelete) {
                    webDavClient.deleteFile(config, oldBackup.name)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun decodeHex(hex: String): ByteArray? {
        return try {
            val len = hex.length
            val data = ByteArray(len / 2)
            for (i in 0 until len step 2) {
                data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            }
            data
        } catch (e: Exception) {
            null
        }
    }
}
