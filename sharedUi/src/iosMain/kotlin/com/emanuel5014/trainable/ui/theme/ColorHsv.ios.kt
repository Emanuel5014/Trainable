package com.emanuel5014.trainable.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

actual fun Color.Companion.fromHSV(hue: Float, saturation: Float, value: Float): Color =
    Color.hsv(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))

actual fun Color.toHSV(): FloatArray {
    val argb = toArgb()
    val red = ((argb ushr 16) and 255) / 255f
    val green = ((argb ushr 8) and 255) / 255f
    val blue = (argb and 255) / 255f
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta) % 6f)
        max == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    return floatArrayOf(if (hue < 0f) hue + 360f else hue, if (max == 0f) 0f else delta / max, max)
}
