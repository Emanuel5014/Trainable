package com.emanuel5014.trainable.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

/**
 * Camera apps store photos with the pixels in the sensor's orientation plus an EXIF tag saying how
 * to turn them upright. `BitmapFactory` ignores that tag, so every decoded photo has to be
 * corrected explicitly or portrait shots come out rotated by 90°.
 */
object ImageOrientation {

    /** How to turn stored pixels upright: rotate clockwise by [rotationDegrees], then mirror horizontally if [mirrored]. */
    data class Transform(val rotationDegrees: Int, val mirrored: Boolean) {
        val isIdentity: Boolean get() = rotationDegrees == 0 && !mirrored
        /** Width and height swap for quarter turns. */
        val swapsAxes: Boolean get() = rotationDegrees == 90 || rotationDegrees == 270
    }

    fun transformFor(exifOrientation: Int): Transform = when (exifOrientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Transform(0, true)
        ExifInterface.ORIENTATION_ROTATE_180 -> Transform(180, false)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> Transform(180, true)
        ExifInterface.ORIENTATION_TRANSPOSE -> Transform(90, true)
        ExifInterface.ORIENTATION_ROTATE_90 -> Transform(90, false)
        ExifInterface.ORIENTATION_TRANSVERSE -> Transform(270, true)
        ExifInterface.ORIENTATION_ROTATE_270 -> Transform(270, false)
        else -> Transform(0, false)
    }

    fun readExifOrientation(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    /** Returns [bitmap] turned upright (recycling the input when a new bitmap had to be made). */
    fun applyTo(bitmap: Bitmap, exifOrientation: Int): Bitmap {
        val transform = transformFor(exifOrientation)
        if (transform.isIdentity) return bitmap
        val matrix = Matrix().apply {
            if (transform.rotationDegrees != 0) setRotate(transform.rotationDegrees.toFloat())
            if (transform.mirrored) postScale(-1f, 1f)
        }
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                .also { if (it !== bitmap) bitmap.recycle() }
        } catch (_: OutOfMemoryError) {
            bitmap
        }
    }

    /** Decodes-side convenience: reads the tag from [uri] and applies it. */
    fun applyTo(context: Context, uri: Uri, bitmap: Bitmap): Bitmap =
        applyTo(bitmap, readExifOrientation(context, uri))
}
