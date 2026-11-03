package com.emanuel5014.trainable.platform

import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal actual fun formatDecimals(value: Double, decimals: Int, deviceLocale: Boolean): String =
    String.format(if (deviceLocale) Locale.getDefault() else Locale.ROOT, "%.${decimals}f", value)

internal actual fun decomposeAccents(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)

internal actual fun decodeUtf16(bytes: ByteArray, offset: Int, bigEndian: Boolean): String =
    String(bytes, offset, bytes.size - offset, if (bigEndian) Charsets.UTF_16BE else Charsets.UTF_16LE)

private val windows1252 = charset("windows-1252")

internal actual fun decodeWindows1252(bytes: ByteArray): String = String(bytes, windows1252)

internal actual class ConcurrentStringMap actual constructor() {
    private val map = ConcurrentHashMap<String, String>()

    actual operator fun get(key: String): String? = map[key]

    actual operator fun set(key: String, value: String) {
        map[key] = value
    }

    actual val size: Int get() = map.size

    actual fun clear() = map.clear()
}
