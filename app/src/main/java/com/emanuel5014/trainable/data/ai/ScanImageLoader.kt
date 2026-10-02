package com.emanuel5014.trainable.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import com.emanuel5014.trainable.util.ImageOrientation
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** What kind of picture is about to be shown to the vision model. */
enum class ScanImageMode {
    /** A whole sheet (a routine card): upright and resized, nothing else. */
    FULL_PAGE,

    /** A few lines of handwriting somewhere on a page: cropped to the writing and contrast-normalised. */
    HANDWRITING_BLOCK
}

/**
 * Turns the photo the user picked or took into the file handed to the vision model.
 *
 * Camera apps save portrait shots sideways plus an EXIF tag; `BitmapFactory` ignores the tag, so the
 * model used to read every photo turned by 90° and returned nonsense. The picture is always made
 * upright first.
 */
internal object ScanImageLoader {

    private const val FULL_PAGE_MAX_PX = 1280
    private const val HANDWRITING_DECODE_MAX_PX = 4096
    private const val HANDWRITING_MODEL_PX = 1024

    /** Never let the padded picture get more elongated than this (long side / short side). */
    private const val MAX_ASPECT = 4f / 3f

    fun prepare(context: Context, uri: Uri, mode: ScanImageMode): File {
        val bitmap = when (mode) {
            ScanImageMode.FULL_PAGE -> decodeUpright(context, uri, FULL_PAGE_MAX_PX, exactLongestSide = true)
            ScanImageMode.HANDWRITING_BLOCK -> handwritingBlock(decodeUpright(context, uri, HANDWRITING_DECODE_MAX_PX, exactLongestSide = false))
        }
        val file = File(context.cacheDir, "ai_scan_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        return file
    }

    /**
     * Decodes [uri] with the EXIF rotation applied. With [exactLongestSide] the result's longest
     * side is [maxSide] (when the photo is bigger); otherwise it is only guaranteed not to exceed it.
     */
    fun decodeUpright(context: Context, uri: Uri, maxSide: Int, exactLongestSide: Boolean): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) error("Could not read image")

        // Exact mode decodes at the coarsest power of two that is still at least maxSide, then scales down
        var sample = 1
        if (exactLongestSide) {
            while (longest / (sample * 2) >= maxSide) sample *= 2
        } else {
            while (longest / sample > maxSide) sample *= 2
        }

        var decoded: Bitmap? = null
        while (decoded == null) {
            try {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?: error("Could not read image")
            } catch (_: OutOfMemoryError) {
                if (sample >= 16) throw IllegalStateException("Image too large to read")
                sample *= 2
            }
        }

        val upright = ImageOrientation.applyTo(context, uri, decoded!!)
        return if (exactLongestSide && max(upright.width, upright.height) > maxSide) upright.scaledToLongestSide(maxSide) else upright
    }

    private fun handwritingBlock(upright: Bitmap): Bitmap {
        // Find the writing on a small copy…
        val analysis = upright.scaledToLongestSide(ScanImagePreprocessor.ANALYSIS_SIZE, recycleSource = false)
        val pixels = IntArray(analysis.width * analysis.height)
        analysis.getPixels(pixels, 0, analysis.width, 0, 0, analysis.width, analysis.height)
        val region = ScanImagePreprocessor.findTextRegion(ScanImagePreprocessor.toGray(pixels, analysis.width, analysis.height))
        val scale = upright.width.toFloat() / analysis.width
        if (analysis !== upright) analysis.recycle()

        // …then cut it out of the full-resolution picture
        val cropped = if (region != null) {
            val left = (region.left * scale).toInt().coerceIn(0, upright.width - 1)
            val top = (region.top * scale).toInt().coerceIn(0, upright.height - 1)
            val right = (region.right * scale).toInt().coerceIn(left + 1, upright.width)
            val bottom = (region.bottom * scale).toInt().coerceIn(top + 1, upright.height)
            Bitmap.createBitmap(upright, left, top, right - left, bottom - top).also { upright.recycle() }
        } else upright

        val sized = if (max(cropped.width, cropped.height) > HANDWRITING_MODEL_PX) {
            cropped.scaledToLongestSide(HANDWRITING_MODEL_PX)
        } else cropped

        val enhanced = enhance(sized)
        return padToAspect(enhanced)
    }

    private fun enhance(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val gray = ScanImagePreprocessor.enhance(ScanImagePreprocessor.toGray(pixels, w, h))
        for (i in pixels.indices) {
            val v = gray.pixels[i].toInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(v, v, v)
        }
        source.recycle()
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    /** White margins so a wide strip of text isn't stretched into a square by the model's resizer. */
    private fun padToAspect(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val longer = max(w, h)
        val shorter = min(w, h)
        if (longer.toFloat() / shorter <= MAX_ASPECT) return source
        val paddedShorter = kotlin.math.ceil(longer / MAX_ASPECT).toInt()
        val padW = if (w >= h) w else paddedShorter
        val padH = if (w >= h) paddedShorter else h
        val out = Bitmap.createBitmap(padW, padH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(source, (padW - w) / 2f, (padH - h) / 2f, null)
        source.recycle()
        return out
    }

    private fun Bitmap.scaledToLongestSide(side: Int, recycleSource: Boolean = true): Bitmap {
        val longest = max(width, height)
        if (longest <= side) return this
        val ratio = side.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            this,
            max(1, (width * ratio).toInt()),
            max(1, (height * ratio).toInt()),
            true
        )
        if (recycleSource && scaled !== this) recycle()
        return scaled
    }
}
