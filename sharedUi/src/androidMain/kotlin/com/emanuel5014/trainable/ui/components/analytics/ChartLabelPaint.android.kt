package com.emanuel5014.trainable.ui.components.analytics

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas

internal actual class ChartLabelPaint actual constructor() {
    private val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 32f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }

    actual fun measure(text: String): Float = paint.measureText(text)

    actual val ascent: Float get() = paint.ascent()
    actual val descent: Float get() = paint.descent()

    actual fun draw(canvas: Canvas, text: String, centerX: Float, baselineY: Float) =
        canvas.nativeCanvas.drawText(text, centerX, baselineY, paint)
}
