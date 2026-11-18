package com.emanuel5014.trainable.domain.csvimport

import com.emanuel5014.trainable.platform.decodeUtf16
import com.emanuel5014.trainable.platform.decodeWindows1252

/**
 * Bytes of a CSV file to text. The app writes UTF-8, but a file that went through a spreadsheet can be UTF-16 ("Unicode
 * text") or the old Windows code page; accents would turn into garbage if the wrong one were assumed.
 */
object CsvTextDecoder {
    fun decode(bytes: ByteArray): String {
        if (bytes.size >= 2) {
            val first = bytes[0].toInt() and 0xFF
            val second = bytes[1].toInt() and 0xFF
            if (first == 0xFF && second == 0xFE) return decodeUtf16(bytes, 2, bigEndian = false)
            if (first == 0xFE && second == 0xFF) return decodeUtf16(bytes, 2, bigEndian = true)
        }
        return try {
            bytes.decodeToString(throwOnInvalidSequence = true)
        } catch (e: CharacterCodingException) {
            decodeWindows1252(bytes)
        }
    }
}
