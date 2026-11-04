package com.emanuel5014.trainable.ui.components.analytics

import androidx.compose.ui.graphics.Canvas

/** The white bold value label of a chart point (32 px, centred), drawn the way each platform draws text on a canvas. */
internal expect class ChartLabelPaint() {
    fun measure(text: String): Float

    /** Distance from the baseline to the top of the text: negative, as on Android. */
    val ascent: Float
    val descent: Float

    fun draw(canvas: Canvas, text: String, centerX: Float, baselineY: Float)
}
