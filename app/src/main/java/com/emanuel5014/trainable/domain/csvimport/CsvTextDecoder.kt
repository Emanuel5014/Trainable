package com.emanuel5014.trainable.domain.csvimport

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Bytes of a CSV file to text. The app writes UTF-8, but a file that went through a spreadsheet can be UTF-16 ("Unicode
 * text") or the old Windows code page; accents would turn into garbage if the wrong one were assumed.
 */
object CsvTextDecoder {
    private val windows1252: Charset = Charset.forName("windows-1252")

    fun decode(bytes: ByteArray): String {
        if (bytes.size >= 2) {
            val first = bytes[0].toInt() and 0xFF
            val second = bytes[1].toInt() and 0xFF
            if (first == 0xFF && second == 0xFE) return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            if (first == 0xFE && second == 0xFF) return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: CharacterCodingException) {
            String(bytes, windows1252)
        }
    }
}
