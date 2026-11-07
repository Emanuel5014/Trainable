package com.emanuel5014.trainable.data.ai.remote

/**
 * Encrypts the API key of a remote AI provider with a key that never leaves the device (Android Keystore,
 * iOS Keychain). A key saved on one device can't be read on another: [decrypt] answers null then.
 */
interface AiKeyCrypto {
    /** Returns (ciphertext, iv). */
    fun encrypt(plain: String): Pair<ByteArray, ByteArray>

    /** Null when the key can't be decrypted (typically a restore onto another device). */
    fun decrypt(ciphertext: ByteArray, iv: ByteArray): String?
}
