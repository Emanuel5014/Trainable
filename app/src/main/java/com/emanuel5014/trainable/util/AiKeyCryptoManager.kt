package com.emanuel5014.trainable.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypts the API key of a remote AI provider with a Keystore key that never leaves the device. */
@Singleton
class AiKeyCryptoManager @Inject constructor() {

    private val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private val KEY_ALIAS = "ai_api_key"
    private val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
    private val GCM_TAG_LENGTH = 128

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /** Returns (ciphertext, iv). */
    fun encrypt(plain: String): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
        return Pair(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)), cipher.iv)
    }

    /** Null when the key can't be decrypted (typically a restore onto another device). */
    fun decrypt(ciphertext: ByteArray, iv: ByteArray): String? {
        return try {
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeystoreKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
