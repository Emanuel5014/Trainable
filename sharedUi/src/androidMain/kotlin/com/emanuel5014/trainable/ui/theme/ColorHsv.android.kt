package com.emanuel5014.trainable.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

actual fun Color.Companion.fromHSV(hue: Float, saturation: Float, value: Float): Color {
	val hsv = floatArrayOf(hue, saturation, value)
	return Color(android.graphics.Color.HSVToColor(hsv))
}

actual fun Color.toHSV(): FloatArray {
	val hsv = floatArrayOf(0f, 0f, 0f)
	android.graphics.Color.colorToHSV(this.toArgb(), hsv)
	return hsv
}