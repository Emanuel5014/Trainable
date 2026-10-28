package com.emanuel5014.trainable.data.ai.remote

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import com.emanuel5014.trainable.data.ai.ScanImageLoader
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Finds a JPEG of the photo small enough to send: quality first, then size. */
object ImageSizeFitter {
    const val MAX_BYTES = 3_500_000
    const val START_SIDE = 2048
    private const val MIN_SIDE = 512
    private val QUALITIES = listOf(85, 70, 55)

    /** [encode] gives the JPEG of the photo scaled to [maxSide] on its longest side at [quality]. */
    fun fit(encode: (maxSide: Int, quality: Int) -> ByteArray): ByteArray {
        for (quality in QUALITIES) {
            val bytes = encode(START_SIDE, quality)
            if (bytes.size <= MAX_BYTES) return bytes
        }
        var side = START_SIDE
        while (side > MIN_SIDE) {
            side = max(side * 3 / 4, MIN_SIDE)
            val bytes = encode(side, QUALITIES.last())
            if (bytes.size <= MAX_BYTES) return bytes
        }
        throw ScanBackendException.PayloadTooLarge()
    }
}

/** The photo as a remote model should see it: upright, in colour, whole, as base64 JPEG. */
object RemoteImagePrep {

    suspend fun prepare(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val source = ScanImageLoader.decodeUpright(context, uri, ImageSizeFitter.START_SIDE, exactLongestSide = false)
        try {
            val jpeg = ImageSizeFitter.fit { maxSide, quality ->
                val longest = max(source.width, source.height)
                val bitmap = if (longest > maxSide) {
                    val ratio = maxSide.toFloat() / longest
                    Bitmap.createScaledBitmap(source, max(1, (source.width * ratio).toInt()), max(1, (source.height * ratio).toInt()), true)
                } else source
                try {
                    ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
                } finally {
                    if (bitmap !== source) bitmap.recycle()
                }
            }
            Base64.encodeToString(jpeg, Base64.NO_WRAP)
        } finally {
            source.recycle()
        }
    }
}
