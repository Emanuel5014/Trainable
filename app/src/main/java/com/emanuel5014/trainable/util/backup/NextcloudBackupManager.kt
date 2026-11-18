package com.emanuel5014.trainable.util.backup

import com.emanuel5014.trainable.data.backup.NextcloudBackupService
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Android's entry point for the Nextcloud backups; the logic is the shared [NextcloudBackupService]. */
@Singleton
class NextcloudBackupManager @Inject constructor(private val service: NextcloudBackupService) {

    suspend fun getActiveConfig(): NextcloudConfig? = service.activeConfig()

    suspend fun backupNow(includeImages: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) { service.backupNow(includeImages) }

    suspend fun listBackups(): Result<List<NextcloudBackupFile>> = withContext(Dispatchers.IO) { service.list() }

    suspend fun restoreBackup(backupFile: NextcloudBackupFile): Result<Unit> = withContext(Dispatchers.IO) { service.restore(backupFile) }

    suspend fun performAutoBackup(maxBackups: Int, includeImages: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) { service.performAutoBackup(maxBackups, includeImages) }
}
