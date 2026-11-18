package com.emanuel5014.trainable.ui.theme

import androidx.compose.ui.graphics.Color

expect fun Color.Companion.fromHSV(hue: Float, saturation: Float, value: Float): Color
expect fun Color.toHSV(): FloatArray
