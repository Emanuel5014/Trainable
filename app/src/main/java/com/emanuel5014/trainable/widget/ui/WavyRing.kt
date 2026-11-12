package com.emanuel5014.trainable.widget.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The weekly goal's wavy arc: from 12 o'clock clockwise for [progress] of a turn, radius waving by [amplitudePx]
 * around a base radius that keeps the stroke inside a [sizePx] square.
 */
fun wavyRingPoints(
    sizePx: Float,
    strokePx: Float,
    amplitudePx: Float,
    progress: Float,
    waves: Int = 14,
    steps: Int = 240
): List<Offset> {
    val p = progress.coerceIn(0f, 1f)
    if (p <= 0f) return emptyList()
    val c = sizePx / 2f
    val base = c - strokePx / 2f - amplitudePx
    val count = (steps * p).toInt().coerceAtLeast(2)
    return (0..count).map { i ->
        val t = (i.toFloat() / count) * p * 2f * PI.toFloat()
        val r = base + amplitudePx * sin(waves * t)
        Offset(c + r * sin(t), c - r * cos(t))
    }
}

/** The ring as a bitmap (Glance has no custom drawing): a flat track and the wavy arc with round caps. */
fun wavyRingBitmap(
    sizePx: Int,
    progress: Float,
    trackArgb: Int,
    arcArgb: Int,
    strokePx: Float,
    amplitudePx: Float
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx.coerceAtLeast(1), sizePx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = sizePx / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    paint.color = trackArgb
    canvas.drawCircle(c, c, c - strokePx / 2f - amplitudePx, paint)
    val points = wavyRingPoints(sizePx.toFloat(), strokePx, amplitudePx, progress)
    if (points.isNotEmpty()) {
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        paint.color = arcArgb
        canvas.drawPath(path, paint)
    }
    return bitmap
}
