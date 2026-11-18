package com.emanuel5014.trainable.data.ai

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
    private val MAGIC = "LITERTLM".encodeToByteArray()

    /** The only format version the bundled engine reads. */
    private const val SUPPORTED_MAJOR_VERSION = 1

    /** Magic plus the major version as a little-endian 32-bit number. */
    const val HEADER_BYTES = 12

    /** A re-uploaded file may differ a little from the one the sizes were taken from. */
    private const val SIZE_TOLERANCE = 0.03

    fun check(header: ByteArray, sizeBytes: Long): ModelImportCheck {
        if (header.size < HEADER_BYTES) return ModelImportCheck.NotLiteRtLm
        if (!header.copyOf(MAGIC.size).contentEquals(MAGIC)) return ModelImportCheck.NotLiteRtLm
        // The major version as a little-endian 32-bit number, as java.nio.ByteBuffer read it before
        val major = (0 until 4).fold(0) { acc, i -> acc or ((header[MAGIC.size + i].toInt() and 0xff) shl (8 * i)) }
        if (major != SUPPORTED_MAJOR_VERSION) return ModelImportCheck.NotLiteRtLm

        val variant = AiModelVariant.entries.firstOrNull {
            abs(sizeBytes - it.fileSizeBytes) <= it.fileSizeBytes * SIZE_TOLERANCE
        } ?: return ModelImportCheck.UnsupportedModel
        return ModelImportCheck.Supported(variant)
    }
}
