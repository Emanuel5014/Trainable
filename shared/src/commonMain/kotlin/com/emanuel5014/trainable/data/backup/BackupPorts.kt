package com.emanuel5014.trainable.data.backup

/** Encrypts the Nextcloud password with a key that never leaves the device (Android Keystore / iOS Keychain). */
interface PasswordVault {
    fun encrypt(plain: String): Pair<ByteArray, ByteArray>
    fun decrypt(ciphertext: ByteArray, iv: ByteArray): String?
}

/** The platform's backup ZIP (G1 format): made into a temporary file, or applied as a restore. */
interface BackupArchiver {
    /** The path of a new temporary ZIP named [fileName], or null when it could not be made. */
    suspend fun createZip(fileName: String, includeImages: Boolean): String?
    /** Replaces the app data with the ZIP at [path] (Android at once, iOS on the next launch). */
    suspend fun applyZip(path: String): Boolean
    fun discard(path: String)
    /** Where a downloaded backup is kept until it is applied. */
    fun tempPath(name: String): String
}

/** The folder the user chose for automatic local backups (Android SAF tree / iOS Files bookmark). */
interface LocalBackupFolder {
    suspend fun isAvailable(): Boolean
    /** Makes the backup straight into the folder as [name]. */
    suspend fun exportTo(name: String, includeImages: Boolean): Boolean
    /** `Trainable_AutoBackup_*` files with their last change in epoch millis. */
    suspend fun listAutoBackups(): List<Pair<String, Long>>
    suspend fun delete(name: String)
}

/** Plans the next automatic run from the preferences (Android WorkManager / iOS BGTaskScheduler). */
interface BackupScheduler {
    suspend fun reschedule()
}
