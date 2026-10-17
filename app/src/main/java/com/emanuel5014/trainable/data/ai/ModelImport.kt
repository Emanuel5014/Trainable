package com.emanuel5014.trainable.data.ai

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/** What [ModelFileValidator] makes of a file the user picked. */
sealed interface ModelImportCheck {
    /** A model the app supports, and which of them it is. */
    data class Supported(val variant: AiModelVariant) : ModelImportCheck

    /** Not a LiteRT-LM file the engine can read. */
    data object NotLiteRtLm : ModelImportCheck

    /** A LiteRT-LM file, but not one of the models the app is built for. */
    data object UnsupportedModel : ModelImportCheck
}

/** Why a model could not be imported, for the screen to put in words. */
sealed interface ModelImportError {
    data object NotLiteRtLm : ModelImportError
    data object UnsupportedModel : ModelImportError
    data class NotEnoughSpace(val neededBytes: Long) : ModelImportError
    data object CannotRead : ModelImportError

    /** The same model is already being downloaded or imported. */
    data object Busy : ModelImportError
}

/**
 * Tells whether a picked file is one of the models the app downloads, from what it can read before copying
 * gigabytes: the first bytes (every LiteRT-LM file starts with `LITERTLM` and a format version) and the size
 * (the Gemma 4 builds for GPU, web or a specific chip are different files of other sizes). The file name is
 * not trusted: other apps rename what they download.
 */
object ModelFileValidator {
    private val MAGIC = "LITERTLM".toByteArray(Charsets.US_ASCII)

    /** The only format version the bundled engine reads. */
    private const val SUPPORTED_MAJOR_VERSION = 1

    /** Magic plus the major version as a little-endian 32-bit number. */
    const val HEADER_BYTES = 12

    /** A re-uploaded file may differ a little from the one the sizes were taken from. */
    private const val SIZE_TOLERANCE = 0.03

    fun check(header: ByteArray, sizeBytes: Long): ModelImportCheck {
        if (header.size < HEADER_BYTES) return ModelImportCheck.NotLiteRtLm
        if (!header.copyOf(MAGIC.size).contentEquals(MAGIC)) return ModelImportCheck.NotLiteRtLm
        val major = ByteBuffer.wrap(header, MAGIC.size, 4).order(ByteOrder.LITTLE_ENDIAN).int
        if (major != SUPPORTED_MAJOR_VERSION) return ModelImportCheck.NotLiteRtLm

        val variant = AiModelVariant.entries.firstOrNull {
            abs(sizeBytes - it.fileSizeBytes) <= it.fileSizeBytes * SIZE_TOLERANCE
        } ?: return ModelImportCheck.UnsupportedModel
        return ModelImportCheck.Supported(variant)
    }

    /** The first [HEADER_BYTES] bytes of [input], or all of it when it is shorter. */
    fun readHeader(input: InputStream): ByteArray {
        val buffer = ByteArray(HEADER_BYTES)
        var read = 0
        while (read < buffer.size) {
            val count = input.read(buffer, read, buffer.size - read)
            if (count < 0) break
            read += count
        }
        return buffer.copyOf(read)
    }
}

/** The source ended before, or ran past, the size it was checked with: what was copied is not that file. */
class IncompleteModelCopy(val copied: Long, val expected: Long) :
    IOException("Copied $copied bytes of $expected")

/**
 * Copies [input] to [target] and tells [onProgress] the fraction done whenever it passes a whole percent (never at 0).
 * Stops as soon as the coroutine is cancelled. The caller removes [target] when this throws.
 *
 * @throws IncompleteModelCopy when the source is not exactly [expectedBytes] long.
 */
suspend fun copyModel(
    input: InputStream,
    target: File,
    expectedBytes: Long,
    bufferSize: Int = 1 shl 20,
    onProgress: (Float) -> Unit
) {
    val buffer = ByteArray(bufferSize)
    var copied = 0L
    var lastPercent = 0
    target.outputStream().use { output ->
        while (true) {
            currentCoroutineContext().ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            copied += read
            // A source that runs past its size is not the file that was checked
            if (copied > expectedBytes) throw IncompleteModelCopy(copied, expectedBytes)
            val percent = (copied * 100 / expectedBytes).toInt()
            if (percent > lastPercent) {
                lastPercent = percent
                onProgress(copied.toFloat() / expectedBytes)
            }
        }
    }
    if (copied != expectedBytes) throw IncompleteModelCopy(copied, expectedBytes)
}
