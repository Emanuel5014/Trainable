package com.emanuel5014.trainable.data.ai.remote

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import com.emanuel5014.trainable.data.ai.ScanImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

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
