package com.emanuel5014.trainable.data.backup

import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConfig
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudWebDav
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

/** Nextcloud backups (formerly Android's NextcloudBackupManager): same files, same texts, same retention. */
class NextcloudBackupService(
    private val prefs: UserPreferencesRepository,
    private val webDav: NextcloudWebDav,
    private val vault: PasswordVault,
    private val archiver: BackupArchiver,
    private val clock: () -> Long
) {
    suspend fun activeConfig(): NextcloudConfig? {
        val serverUrl = prefs.nextcloudServerUrl.first() ?: return null
        val username = prefs.nextcloudUsername.first() ?: return null
        val encrypted = prefs.nextcloudEncryptedPassword.first()?.let(::decodeHex) ?: return null
        val iv = prefs.nextcloudPasswordIv.first()?.let(::decodeHex) ?: return null
        val password = vault.decrypt(encrypted, iv) ?: return null
        return NextcloudConfig(serverUrl, username, password, prefs.nextcloudRemoteFolder.first())
    }

    /** Trims every field and keeps the password only encrypted (lowercase hex), as Settings and onboarding did. */
    suspend fun saveConfig(config: NextcloudConfig) {
        val (encrypted, iv) = vault.encrypt(config.passwordOrToken.trim())
        prefs.setNextcloudConfig(config.serverUrl.trim(), config.username.trim(), encrypted.toHex(), iv.toHex(),
            config.remoteFolder.trim().ifEmpty { "Trainable/Backups" })
    }

    suspend fun disconnect() = prefs.clearNextcloudConfig()

    suspend fun backupNow(includeImages: Boolean): Result<Unit> {
        val config = activeConfig() ?: return notConfigured()
        val name = BackupNames.manual(clock())
        val zip = archiver.createZip(name, includeImages) ?: return Result.failure(Exception("Failed to generate backup archive"))
        return try { webDav.upload(config, zip, name) } finally { archiver.discard(zip) }
    }

    suspend fun list(): Result<List<NextcloudBackupFile>> {
        val config = activeConfig() ?: return notConfigured()
        return webDav.list(config)
    }

    suspend fun restore(file: NextcloudBackupFile): Result<Unit> {
        val config = activeConfig() ?: return notConfigured()
        val temp = archiver.tempPath(file.name)
        return try {
            webDav.download(config, file.name, temp).onFailure { return Result.failure(it) }
            if (archiver.applyZip(temp)) Result.success(Unit)
            else Result.failure(Exception("Failed to import database from downloaded backup"))
        } finally { archiver.discard(temp) }
    }

    suspend fun performAutoBackup(max: Int, includeImages: Boolean): Result<Unit> {
        val config = activeConfig() ?: return notConfigured()
        val name = BackupNames.automatic(clock())
        val zip = archiver.createZip(name, includeImages) ?: return Result.failure(Exception("Failed to generate auto-backup archive"))
        return try {
            webDav.upload(config, zip, name).onFailure { return Result.failure(it) }
            try {
                val files = webDav.list(config).getOrNull()
                if (files != null) BackupRetention.toDelete(files, { it.name }, { it.lastModifiedMillis }, max)
                    .forEach { webDav.delete(config, it.name) }
            } catch (e: CancellationException) { throw e } catch (_: Exception) {}
            Result.success(Unit)
        } finally { archiver.discard(zip) }
    }

    private fun <T> notConfigured(): Result<T> = Result.failure(Exception("Nextcloud not configured"))
}

internal fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

/** Android's decoder: two hex digits per byte, null when the text is not hex. */
internal fun decodeHex(hex: String): ByteArray? {
    if (hex.length % 2 != 0) return null
    return ByteArray(hex.length / 2) { i ->
        val hi = hex[2 * i].digitToIntOrNull(16) ?: return null
        val lo = hex[2 * i + 1].digitToIntOrNull(16) ?: return null
        ((hi shl 4) + lo).toByte()
    }
}
