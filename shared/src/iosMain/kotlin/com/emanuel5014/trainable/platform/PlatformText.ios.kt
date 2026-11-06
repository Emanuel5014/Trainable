package com.emanuel5014.trainable.platform

import platform.Foundation.NSLocale
import platform.Foundation.NSLock
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterRoundHalfUp
import platform.Foundation.NSString
import platform.Foundation.currentLocale
import platform.Foundation.decomposedStringWithCanonicalMapping

internal actual fun formatDecimals(value: Double, decimals: Int, deviceLocale: Boolean): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        usesGroupingSeparator = false
        minimumIntegerDigits = 1u
        minimumFractionDigits = decimals.toULong()
        maximumFractionDigits = decimals.toULong()
        roundingMode = NSNumberFormatterRoundHalfUp
        locale = if (deviceLocale) IosAppLocale.formattingLocale() else NSLocale(localeIdentifier = "en_US_POSIX")
    }
    return formatter.stringFromNumber(NSNumber(double = value)) ?: value.toString()
}

@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun decomposeAccents(text: String): String = (text as NSString).decomposedStringWithCanonicalMapping

internal actual fun decodeUtf16(bytes: ByteArray, offset: Int, bigEndian: Boolean): String {
    val units = (bytes.size - offset) / 2
    val chars = CharArray(units) { i ->
        val a = bytes[offset + 2 * i].toInt() and 0xFF
        val b = bytes[offset + 2 * i + 1].toInt() and 0xFF
        (if (bigEndian) (a shl 8) or b else (b shl 8) or a).toChar()
    }
    val out = StringBuilder(units + 1)
    var i = 0
    while (i < chars.size) {
        val ch = chars[i]
        when {
            ch.isHighSurrogate() && i + 1 < chars.size && chars[i + 1].isLowSurrogate() -> {
                out.append(ch).append(chars[i + 1])
                i++
            }
            ch.isSurrogate() -> out.append(REPLACEMENT)
            else -> out.append(ch)
        }
        i++
    }
    // A last odd byte is half a character
    if ((bytes.size - offset) % 2 != 0) out.append(REPLACEMENT)
    return out.toString()
}

internal actual fun decodeWindows1252(bytes: ByteArray): String = buildString(bytes.size) {
    for (byte in bytes) {
        val code = byte.toInt() and 0xFF
        append(if (code in 0x80..0x9F) WINDOWS_1252_HIGH[code - 0x80] else code.toChar())
    }
}

private const val REPLACEMENT = '�'

// 0x80..0x9F, the only bytes where the code page differs from Latin-1; the five unused ones decode as U+FFFD
private const val WINDOWS_1252_HIGH =
    "€�‚ƒ„…†‡ˆ‰Š‹Œ�Ž�" +
        "�‘’“”•–—˜™š›œ�žŸ"

internal actual class ConcurrentStringMap actual constructor() {
    private val lock = NSLock()
    private val map = HashMap<String, String>()

    actual operator fun get(key: String): String? = locked { map[key] }

    actual operator fun set(key: String, value: String) = locked { map[key] = value }

    actual val size: Int get() = locked { map.size }

    actual fun clear() = locked { map.clear() }

    private inline fun <T> locked(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}
