package com.emanuel5014.trainable.platform

// Text handling that the Kotlin standard library has no common API for. The Android side keeps the JVM calls the app
// always used, so numbers and imported files come out exactly as before.

/**
 * [value] with exactly [decimals] decimals, rounded half up and without grouping. With [deviceLocale] the decimal
 * separator is the one of the phone's language ("82,5" in Italian), otherwise a dot.
 */
internal expect fun formatDecimals(value: Double, decimals: Int, deviceLocale: Boolean): String

/** Canonical decomposition (NFD): "è" becomes "e" followed by a combining grave accent. */
internal expect fun decomposeAccents(text: String): String

/** UTF-16 bytes from [offset] on (after the byte order mark) to text; broken pairs become U+FFFD. */
internal expect fun decodeUtf16(bytes: ByteArray, offset: Int, bigEndian: Boolean): String

/** Bytes in the old Windows code page (windows-1252) to text. */
internal expect fun decodeWindows1252(bytes: ByteArray): String

/** A string-to-string map that several threads may read and write at once. */
internal expect class ConcurrentStringMap() {
    operator fun get(key: String): String?
    operator fun set(key: String, value: String)
    val size: Int
    fun clear()
}
