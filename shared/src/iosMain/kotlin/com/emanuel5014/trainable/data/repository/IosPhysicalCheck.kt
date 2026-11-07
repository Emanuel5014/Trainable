@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.data.repository

import kotlinx.cinterop.*
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.CoreCrypto.kCCSuccess
import platform.CoreFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.*
import platform.darwin.NSObject
import platform.Security.*
import platform.UIKit.*
import platform.posix.memcpy
import kotlin.math.max
import kotlin.math.min

/**
 * AES-256-GCM, which Kotlin/Native cannot reach (CryptoKit is Swift only): the Xcode app installs a CryptoKit
 * implementation in [IosCryptoBridge] at launch. [seal] returns ciphertext followed by the 16-byte tag; [open]
 * takes that and returns null when the key or the data are wrong.
 */
interface IosAesGcm {
    fun seal(key: NSData, nonce: NSData, plaintext: NSData): NSData?
    fun open(key: NSData, nonce: NSData, sealed: NSData): NSData?
}

/** Set by the Swift app before the Compose UI starts. */
object IosCryptoBridge {
    var aesGcm: IosAesGcm? = null
}

fun ByteArray.toNSData(): NSData = if (isEmpty()) NSData() else usePinned {
    NSData.dataWithBytes(it.addressOf(0), size.toULong())
}

fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply { usePinned { memcpy(it.addressOf(0), bytes, length) } }
}

/**
 * The physical-check cryptography on iOS, byte-compatible with Android's PhysicalCheckCryptoManager: PBKDF2 from
 * CommonCrypto, AES-GCM from CryptoKit ([IosCryptoBridge]). The device key that wraps the password for automatic
 * unlocking is a random AES key kept in the Keychain (this device only), as Android keeps one in the Keystore.
 */
class IosPhysicalCheckCrypto : PhysicalCheckCrypto {

    private val aes: IosAesGcm
        get() = IosCryptoBridge.aesGcm ?: error("AES-GCM bridge not installed")

    override fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val passwordBytes = password.encodeToByteArray()
        val key = ByteArray(DERIVED_KEY_BYTES)
        val status = passwordBytes.usePinned { p ->
            salt.usePinned { s ->
                key.usePinned { k ->
                    CCKeyDerivationPBKDF(
                        kCCPBKDF2,
                        if (passwordBytes.isEmpty()) null else p.addressOf(0).reinterpret(),
                        passwordBytes.size.convert(),
                        if (salt.isEmpty()) null else s.addressOf(0).reinterpret(),
                        salt.size.convert(),
                        kCCPRFHmacAlgSHA256,
                        ITERATIONS.convert(),
                        k.addressOf(0).reinterpret(),
                        key.size.convert()
                    )
                }
            }
        }
        check(status == kCCSuccess) { "PBKDF2 failed: $status" }
        return key
    }

    override fun generateSalt(): ByteArray = randomBytes(16)

    override fun encryptData(data: ByteArray, key: ByteArray): ByteArray {
        val (sealed, iv) = encryptWithIv(data, key)
        return iv + sealed
    }

    override fun decryptData(encryptedData: ByteArray, key: ByteArray): ByteArray {
        require(encryptedData.size >= GCM_IV_LENGTH + 16) { "Data too short" }
        return decryptWithIv(
            encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size),
            encryptedData.copyOfRange(0, GCM_IV_LENGTH),
            key
        )
    }

    override fun encryptWithIv(data: ByteArray, key: ByteArray): Pair<ByteArray, ByteArray> {
        val iv = randomBytes(GCM_IV_LENGTH)
        val sealed = aes.seal(key.toNSData(), iv.toNSData(), data.toNSData()) ?: error("AES-GCM seal failed")
        return Pair(sealed.toByteArray(), iv)
    }

    override fun decryptWithIv(encrypted: ByteArray, iv: ByteArray, key: ByteArray): ByteArray =
        aes.open(key.toNSData(), iv.toNSData(), encrypted.toNSData())?.toByteArray()
            ?: error("AES-GCM open failed")

    override fun wrapPassword(password: String): Pair<ByteArray, ByteArray> =
        encryptWithIv(password.encodeToByteArray(), deviceKey(create = true)!!)

    override fun unwrapPassword(wrapped: ByteArray, iv: ByteArray): String? = try {
        deviceKey(create = false)?.let { decryptWithIv(wrapped, iv, it).decodeToString() }
    } catch (e: Exception) {
        null
    }

    /** The device key from the Keychain, made (and stored) the first time when [create]. */
    private fun deviceKey(create: Boolean): ByteArray? {
        readKeychainKey()?.let { return it }
        if (!create) return null
        val key = randomBytes(32)
        check(storeKeychainKey(key)) { "Keychain write failed" }
        return key
    }

    /** A Keychain query for the device key; [extra] adds its own entries, retaining NSObjects through [retain]. */
    private fun <T> withQuery(
        extra: (dict: CFMutableDictionaryRef?, retain: (NSObject) -> CFTypeRef?) -> Unit,
        block: (CFDictionaryRef?) -> T
    ): T {
        val retained = mutableListOf<CFTypeRef?>()
        val retain: (NSObject) -> CFTypeRef? = { value -> CFBridgingRetain(value).also { retained += it } }
        val dict = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        try {
            CFDictionaryAddValue(dict, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(dict, kSecAttrService, retain(NSString.create(string = KEYCHAIN_SERVICE)))
            CFDictionaryAddValue(dict, kSecAttrAccount, retain(NSString.create(string = KEYCHAIN_ACCOUNT)))
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
            (CFBridgingRelease(result.value) as? NSData)?.toByteArray()?.takeIf { it.size == 32 }
        }
    }

    private fun storeKeychainKey(key: ByteArray): Boolean {
        withQuery({ _, _ -> }) { SecItemDelete(it) }
        return withQuery({ dict, retain ->
            CFDictionaryAddValue(dict, kSecValueData, retain(key.toNSData()))
            CFDictionaryAddValue(dict, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
        }) { SecItemAdd(it, null) } == errSecSuccess
    }

    private companion object {
        const val ITERATIONS = 600000
        const val DERIVED_KEY_BYTES = 32
        const val GCM_IV_LENGTH = 12
        const val KEYCHAIN_SERVICE = "com.emanuel5014.trainable.physicalcheck"
        const val KEYCHAIN_ACCOUNT = "physical_check_password_key"

        fun randomBytes(count: Int): ByteArray {
            val bytes = ByteArray(count)
            val status = bytes.usePinned { SecRandomCopyBytes(kSecRandomDefault, count.convert(), it.addressOf(0)) }
            check(status == errSecSuccess) { "SecRandomCopyBytes failed: $status" }
            return bytes
        }
    }
}

/** The photos in `Documents/physical_checks`, made smaller as on Android: at most 1280 px, JPEG quality 80. */
class IosPhysicalCheckFiles(private val directory: () -> String = { documentsFolder("physical_checks") }) : PhysicalCheckFiles {
    private fun path(name: String): String {
        require(safeExerciseMediaName(name)) { "Bad name" }
        return directory() + "/" + name
    }

    override fun list(): List<String> {
        val dir = directory()
        val manager = NSFileManager.defaultManager
        return manager.contentsOfDirectoryAtPath(dir, null).orEmpty().mapNotNull { it as? String }.filter { name ->
            memScoped {
                val isDir = alloc<BooleanVar>()
                manager.fileExistsAtPath("$dir/$name", isDir.ptr) && !isDir.value
            }
        }
    }

    override fun exists(name: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path(name))

    override fun read(name: String): ByteArray =
        NSData.dataWithContentsOfFile(path(name))?.toByteArray() ?: error("Cannot read $name")

    override fun write(name: String, bytes: ByteArray) {
        check(bytes.toNSData().writeToFile(path(name), atomically = true)) { "Cannot write $name" }
    }

    override fun delete(name: String) {
        NSFileManager.defaultManager.removeItemAtPath(path(name), null)
    }

    override fun compressImage(bytes: ByteArray): ByteArray = encodeIosJpeg(bytes, maxSide = 1280, quality = 80) ?: bytes
}

/** Encodes an upright JPEG without upscaling the source. */
@OptIn(ExperimentalForeignApi::class)
fun encodeIosJpeg(bytes: ByteArray, maxSide: Int, quality: Int): ByteArray? = try {
    val image = UIImage.imageWithData(bytes.toNSData())
    if (image == null) null else {
        val (width, height) = image.size.useContents { width to height }
        val ratio = min(1.0, maxSide.toDouble() / max(width, height))
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(width * ratio, height * ratio), true, 1.0)
        val scaled = try {
            image.drawInRect(CGRectMake(0.0, 0.0, width * ratio, height * ratio))
            UIGraphicsGetImageFromCurrentImageContext()
        } finally {
            UIGraphicsEndImageContext()
        }
        scaled?.let { UIImageJPEGRepresentation(it, quality / 100.0) }?.toByteArray()
    }
} catch (_: Exception) {
    null
}
