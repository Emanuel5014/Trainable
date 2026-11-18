package com.emanuel5014.trainable.ui.components.shared

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.window.DialogProperties

/** The photo opened in [ImageEditorDialog], or null when the bytes are not an image. */
expect fun decodeEditorImage(bytes: ByteArray): ImageBitmap?

/**
 * Draws [originalBitmap] as the editor shows it (fit to [containerSize], then [scale], pan and [rotationAngle]) and
 * returns what lies inside [cropRect] as a JPEG (quality 85, longest side about 1280 px).
 */
expect suspend fun cropAndRotateImage(
    originalBitmap: ImageBitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotationAngle: Float,
    cropRect: Rect,
    containerSize: Size
): ByteArray

/** Full-screen, Back closes it, a tap outside does not. iOS also draws under the status bar and home indicator. */
expect fun imageEditorDialogProperties(): DialogProperties
