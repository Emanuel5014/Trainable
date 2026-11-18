package com.emanuel5014.trainable.data.repository

/**
 * The cryptography of the physical checks, the same bytes on every platform so that a backup opens anywhere:
 * PBKDF2-HMAC-SHA256 (600,000 iterations, 256-bit key) from the password and a 16-byte salt; AES-256-GCM with a
 * 12-byte IV and a 128-bit tag. Photos are stored as `iv || ciphertext+tag`; the validation block keeps its IV apart.
 * Keys are the raw 32 key bytes. The device-bound part ([wrapPassword]) is Android Keystore on Android and the
 * Keychain on iOS: it only lets this device unlock without asking the password again.
 */
interface PhysicalCheckCrypto {
    fun deriveKey(password: String, salt: ByteArray): ByteArray
    fun generateSalt(): ByteArray

    /** `iv || ciphertext+tag` with a fresh random IV. */
    fun encryptData(data: ByteArray, key: ByteArray): ByteArray
    fun decryptData(encryptedData: ByteArray, key: ByteArray): ByteArray

    /** The validation block: (ciphertext+tag, iv) with a fresh random IV. */
    fun encryptWithIv(data: ByteArray, key: ByteArray): Pair<ByteArray, ByteArray>
    /** Throws when the key is wrong (the tag does not match). */
    fun decryptWithIv(encrypted: ByteArray, iv: ByteArray, key: ByteArray): ByteArray

    /** The password sealed by a key that never leaves this device: (ciphertext, iv). */
    fun wrapPassword(password: String): Pair<ByteArray, ByteArray>
    /** Null when the device key cannot open it (another device, a restored backup). */
    fun unwrapPassword(wrapped: ByteArray, iv: ByteArray): String?
}

/** The photo files of the physical checks, in the app's private `physical_checks` folder. */
interface PhysicalCheckFiles {
    /** The names of the files (not folders) in the folder. */
    fun list(): List<String>
    fun exists(name: String): Boolean
    fun read(name: String): ByteArray
    fun write(name: String, bytes: ByteArray)
    fun delete(name: String)
    /** The photo made smaller as stored (at most 1280 px, JPEG); the bytes unchanged when they are not an image. */
    fun compressImage(bytes: ByteArray): ByteArray
}
