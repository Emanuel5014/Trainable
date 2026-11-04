package com.emanuel5014.trainable.ui.components.analytics

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint

internal actual class ChartLabelPaint actual constructor() {
    private val font = Font(null, 32f).apply { isEmboldened = true }
    private val paint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        isAntiAlias = true
    }

    actual fun measure(text: String): Float = font.measureTextWidth(text)

    actual val ascent: Float get() = font.metrics.ascent
    actual val descent: Float get() = font.metrics.descent

    actual fun draw(canvas: Canvas, text: String, centerX: Float, baselineY: Float) {
        canvas.nativeCanvas.drawString(text, centerX - measure(text) / 2f, baselineY, font, paint)
    }
}
