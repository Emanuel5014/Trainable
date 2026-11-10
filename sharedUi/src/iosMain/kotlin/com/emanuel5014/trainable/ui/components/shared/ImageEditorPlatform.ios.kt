package com.emanuel5014.trainable.ui.components.shared

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Rect as SkRect

/** Photos reach the editor already upright (the picker applies the orientation), so no EXIF handling here. */
actual fun decodeEditorImage(bytes: ByteArray): ImageBitmap? =
    runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

actual fun imageEditorDialogProperties(): DialogProperties = DialogProperties(
    dismissOnBackPress = true,
    dismissOnClickOutside = false,
    usePlatformDefaultWidth = false,
    usePlatformInsets = false
)

/** Same geometry as the Android version (fit, zoom, rotate around the centre, pan; JPEG 85), drawn with Skia. */
actual suspend fun cropAndRotateImage(
    originalBitmap: ImageBitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotationAngle: Float,
    cropRect: Rect,
    containerSize: Size
): ByteArray = withContext(Dispatchers.Default) {
    val originalWidth = originalBitmap.width
    val originalHeight = originalBitmap.height

    val imageAspect = originalWidth.toFloat() / originalHeight
    val containerAspect = containerSize.width / containerSize.height
    val baseWidth = if (imageAspect > containerAspect) containerSize.width else containerSize.height * imageAspect
    val baseScale = baseWidth / originalWidth

    val maxDimension = 1280f
    val exportScale = if (cropRect.width > cropRect.height) {
        (maxDimension / cropRect.width).coerceAtMost(2.0f)
    } else {
        (maxDimension / cropRect.height).coerceAtMost(2.0f)
    }
    val destWidth = (cropRect.width * exportScale).toInt()
    val destHeight = (cropRect.height * exportScale).toInt()

    val source = Image.makeFromBitmap(originalBitmap.asSkiaBitmap())
    val surface = Surface.makeRasterN32Premul(destWidth, destHeight)
    try {
        val canvas = surface.canvas
        // Android leaves the area outside the photo transparent, which its JPEG encoder writes as black.
        canvas.clear(Color.BLACK)
        // Canvas calls apply to the image in reverse order, i.e. the Android matrix steps read bottom to top:
        // centre the image on its origin, scale, rotate, then move to the output centre plus the user's pan.
        canvas.translate(destWidth / 2f + offsetX * exportScale, destHeight / 2f + offsetY * exportScale)
        canvas.rotate(rotationAngle)
        val totalScale = baseScale * scale * exportScale
        canvas.scale(totalScale, totalScale)
        canvas.translate(-originalWidth / 2f, -originalHeight / 2f)
        val bounds = SkRect.makeWH(originalWidth.toFloat(), originalHeight.toFloat())
        val paint = Paint().apply { isAntiAlias = true }
        canvas.drawImageRect(source, bounds, bounds, SamplingMode.LINEAR, paint, true)
        val data = checkNotNull(surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, 85)) {
            "JPEG encoding failed"
        }
        data.bytes
    } finally {
        surface.close()
        source.close()
    }
}
