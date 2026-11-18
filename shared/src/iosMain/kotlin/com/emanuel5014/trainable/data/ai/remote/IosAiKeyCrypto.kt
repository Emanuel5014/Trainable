@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.data.ai.remote

import com.emanuel5014.trainable.data.repository.IosAesGcm
import com.emanuel5014.trainable.data.repository.IosCryptoBridge
import com.emanuel5014.trainable.data.repository.toByteArray
import com.emanuel5014.trainable.data.repository.toNSData
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import platform.darwin.NSObject

/**
 * The remote AI's API keys, AES-256-GCM with a device key kept in the Keychain (this device only, after first
 * unlock: never in backups or iCloud), like Android's Keystore key. A key saved elsewhere can't be read here.
 */
class IosAiKeyCrypto(
    private val aes: () -> IosAesGcm? = { IosCryptoBridge.aesGcm },
    /** Keychain item of the device key; Nextcloud uses its own (IosNextcloudVault). */
    private val keychainService: String = "com.emanuel5014.trainable.ai",
    private val keychainAccount: String = "ai_api_key"
) : AiKeyCrypto {

    override fun encrypt(plain: String): Pair<ByteArray, ByteArray> {
        val gcm = aes() ?: error("AES-GCM bridge not installed")
        val key = deviceKey(create = true) ?: error("Keychain write failed")
        val iv = randomBytes(IV_BYTES)
        val sealed = gcm.seal(key.toNSData(), iv.toNSData(), plain.encodeToByteArray().toNSData()) ?: error("AES-GCM seal failed")
        return Pair(sealed.toByteArray(), iv)
    }

    override fun decrypt(ciphertext: ByteArray, iv: ByteArray): String? = try {
        val gcm = aes()
        val key = deviceKey(create = false)
        if (gcm == null || key == null) null
        else gcm.open(key.toNSData(), iv.toNSData(), ciphertext.toNSData())?.toByteArray()?.decodeToString()
    } catch (_: Exception) {
        null
    }

    /** The device key from the Keychain, made (and stored) the first time when [create]. */
    private fun deviceKey(create: Boolean): ByteArray? {
        readKeychainKey()?.let { return it }
        if (!create) return null
        val key = randomBytes(KEY_BYTES)
        return key.takeIf { storeKeychainKey(it) }
    }

    /** A Keychain query for the device key; [extra] adds its own entries, retaining NSObjects through its second argument. */
    private fun <T> withQuery(
        extra: (dict: CFMutableDictionaryRef?, retain: (NSObject) -> CFTypeRef?) -> Unit,
        block: (CFDictionaryRef?) -> T
    ): T {
        val retained = mutableListOf<CFTypeRef?>()
        val retain: (NSObject) -> CFTypeRef? = { value -> CFBridgingRetain(value).also { retained += it } }
        val dict = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        try {
            // Typed CF constants added one by one: a Kotlin map bridged to NSDictionary crashed here (see IosPhysicalCheck)
            CFDictionaryAddValue(dict, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(dict, kSecAttrService, retain(NSString.create(string = keychainService)))
            CFDictionaryAddValue(dict, kSecAttrAccount, retain(NSString.create(string = keychainAccount)))
            extra(dict, retain)
            return block(dict)
        } finally {
            CFRelease(dict)
            retained.forEach { CFRelease(it) }
        }
    }

    private fun readKeychainKey(): ByteArray? = withQuery({ dict, _ ->
        CFDictionaryAddValue(dict, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(dict, kSecMatchLimit, kSecMatchLimitOne)
    }) { query ->
        memScoped {
            val result = alloc<CFTypeRefVar>()
            if (SecItemCopyMatching(query, result.ptr) != errSecSuccess) return@memScoped null
            (CFBridgingRelease(result.value) as? NSData)?.toByteArray()?.takeIf { it.size == KEY_BYTES }
        }
    }

    private fun storeKeychainKey(key: ByteArray): Boolean {
        withQuery({ _, _ -> }) { SecItemDelete(it) }
        return withQuery({ dict, retain ->
            CFDictionaryAddValue(dict, kSecValueData, retain(key.toNSData()))
            CFDictionaryAddValue(dict, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        }) { SecItemAdd(it, null) } == errSecSuccess
    }

    private companion object {
        const val KEY_BYTES = 32
        const val IV_BYTES = 12

        fun randomBytes(count: Int): ByteArray {
            val bytes = ByteArray(count)
            val status = bytes.usePinned { SecRandomCopyBytes(kSecRandomDefault, count.convert(), it.addressOf(0)) }
            check(status == errSecSuccess) { "SecRandomCopyBytes failed: $status" }
            return bytes
        }
    }
}
