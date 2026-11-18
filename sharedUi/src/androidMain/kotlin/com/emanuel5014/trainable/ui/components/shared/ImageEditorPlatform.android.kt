package com.emanuel5014.trainable.ui.components.shared

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

actual fun decodeEditorImage(bytes: ByteArray): ImageBitmap? =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()

actual fun imageEditorDialogProperties(): DialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    dismissOnBackPress = true,
    dismissOnClickOutside = false
)

actual suspend fun cropAndRotateImage(
    originalBitmap: ImageBitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotationAngle: Float,
    cropRect: Rect,
    containerSize: Size
): ByteArray = cropAndRotateBitmap(
    originalBitmap.asAndroidBitmap(), scale, offsetX, offsetY, rotationAngle, cropRect, containerSize
)

private suspend fun cropAndRotateBitmap(
    originalBitmap: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotationAngle: Float,
    cropRect: Rect,
    containerSize: Size
): ByteArray = withContext(Dispatchers.Default) {
    val originalWidth = originalBitmap.width
    val originalHeight = originalBitmap.height

    // 1. Calculate the base scale of the image when fit to container
    val imageAspect = originalWidth.toFloat() / originalHeight
    val containerAspect = containerSize.width / containerSize.height

    val baseWidth: Float
    if (imageAspect > containerAspect) {
        baseWidth = containerSize.width
    } else {
        baseWidth = containerSize.height * imageAspect
    }
    val baseScale = baseWidth / originalWidth

    // 2. We want to output a high-quality cropped bitmap.
    // Let's target a max resolution based on crop bounds to preserve quality, up to 1280px max dimension.
    val maxDimension = 1280f
    val exportScale = if (cropRect.width > cropRect.height) {
        (maxDimension / cropRect.width).coerceAtMost(2.0f)
    } else {
        (maxDimension / cropRect.height).coerceAtMost(2.0f)
    }

    val destWidth = (cropRect.width * exportScale).toInt()
    val destHeight = (cropRect.height * exportScale).toInt()

    val croppedBitmap = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(croppedBitmap)

    // Paint to enable anti-aliasing and filter bitmap scaling
    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    val matrix = Matrix()

    // Translate source image to have its origin at its center
    matrix.postTranslate(-originalWidth / 2f, -originalHeight / 2f)

    // Apply base scale + user zoom (scale) + export scale
    val totalScale = baseScale * scale * exportScale
    matrix.postScale(totalScale, totalScale)

    // Rotate
    matrix.postRotate(rotationAngle)

    // Translate to center of output canvas, adding user pan offset (scaled to output coordinates)
    matrix.postTranslate(destWidth / 2f + offsetX * exportScale, destHeight / 2f + offsetY * exportScale)

    canvas.drawBitmap(originalBitmap, matrix, paint)

    val outputStream = ByteArrayOutputStream()
    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
    val bytes = outputStream.toByteArray()

    // Recycle bitmaps
    croppedBitmap.recycle()

    bytes
}
