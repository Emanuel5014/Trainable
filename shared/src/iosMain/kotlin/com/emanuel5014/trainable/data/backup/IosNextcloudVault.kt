package com.emanuel5014.trainable.data.backup

import com.emanuel5014.trainable.data.ai.remote.IosAiKeyCrypto
import com.emanuel5014.trainable.data.repository.IosAesGcm
import com.emanuel5014.trainable.data.repository.IosCryptoBridge

/**
 * The Nextcloud password, AES-256-GCM with its own device key in the Keychain (this device only, readable after the
 * first unlock so a background backup can use it; never in backups or iCloud) — the iOS side of Android's Keystore key.
 */
class IosNextcloudVault(aes: () -> IosAesGcm? = { IosCryptoBridge.aesGcm }) : PasswordVault {
    private val crypto = IosAiKeyCrypto(aes, keychainService = "com.emanuel5014.trainable.nextcloud", keychainAccount = "nextcloud_password_key")
    override fun encrypt(plain: String): Pair<ByteArray, ByteArray> = crypto.encrypt(plain)
    override fun decrypt(ciphertext: ByteArray, iv: ByteArray): String? = crypto.decrypt(ciphertext, iv)
}
