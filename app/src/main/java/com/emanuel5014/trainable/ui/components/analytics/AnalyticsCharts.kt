package com.emanuel5014.trainable.ui.components.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.screens.analytics.AnalyticsChartPoint
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsLineChart(
    points: List<AnalyticsChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Primary,
    fillColor: Color = Primary.copy(alpha = 0.14f)
) {
    if (points.isEmpty()) {
        Text(
            text = "No data for this period",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            modifier = modifier.padding(vertical = 24.dp)
        )
        return
    }

    val gridColor = SurfaceContainerHigh

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val values = points.map { it.value }
            val minValue = values.minOrNull() ?: 0f
            val maxValue = values.maxOrNull() ?: 0f
            val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
            val horizontalPadding = size.width * 0.06f
            val verticalPadding = size.height * 0.12f
            val chartWidth = size.width - (horizontalPadding * 2f)
            val chartHeight = size.height - (verticalPadding * 2f)

            fun xFor(index: Int): Float {
                if (points.size == 1) return size.width / 2f
                return horizontalPadding + (index.toFloat() / (points.lastIndex.coerceAtLeast(1)).toFloat()) * chartWidth
            }

            fun yFor(value: Float): Float {
                val normalized = (value - minValue) / range
                return verticalPadding + chartHeight - (normalized * chartHeight)
            }

            repeat(3) { lineIndex ->
                val y = verticalPadding + (chartHeight / 2f) * lineIndex
                drawLine(
                    color = gridColor.copy(alpha = 0.65f),
                    start = Offset(horizontalPadding, y),
                    end = Offset(size.width - horizontalPadding, y),
                    strokeWidth = 1f
                )
            }

            val fillPath = Path()
            val linePath = Path()

            points.forEachIndexed { index, point ->
                val x = xFor(index)
                val y = yFor(point.value)

                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, size.height - verticalPadding)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = xFor(index - 1)
                    val prevY = yFor(points[index - 1].value)
                    
                    val controlX1 = prevX + (x - prevX) / 2f
                    val controlY1 = prevY
                    val controlX2 = prevX + (x - prevX) / 2f
                    val controlY2 = y
                    
                    linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                }
            }

            fillPath.lineTo(xFor(points.lastIndex), size.height - verticalPadding)
            fillPath.close()

            drawPath(path = fillPath, color = fillColor)
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            points.forEachIndexed { index, point ->
                val x = xFor(index)
                val y = yFor(point.value)
                drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))

                val text = String.format("%.1f", point.value)
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 32f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }
                val textWidth = textPaint.measureText(text)
                val badgeHPadding = 14f
                val badgeVPadding = 8f
                val badgeWidth = textWidth + badgeHPadding * 2
                val badgeTotalHeight = (textPaint.descent() - textPaint.ascent()) + badgeVPadding * 2
                val gapFromPoint = 8f
                val badgeLeft = (x - badgeWidth / 2f).coerceIn(
                    0f,
                    (size.width - badgeWidth).coerceAtLeast(0f)
                )
                val textCenterX = badgeLeft + badgeWidth / 2f

                val placeBelow = (y - 6f - gapFromPoint - badgeTotalHeight) < 0f
                val badgeTop: Float
                val textBaselineY: Float
                if (placeBelow) {
                    badgeTop = y + 6f + gapFromPoint
                    textBaselineY = badgeTop + badgeVPadding - textPaint.ascent()
                } else {
                    badgeTop = y - 6f - gapFromPoint - badgeTotalHeight
                    textBaselineY = badgeTop + badgeVPadding - textPaint.ascent()
                }

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.12f),
                    topLeft = Offset(badgeLeft + 1f, badgeTop + 1f),
                    size = Size(badgeWidth, badgeTotalHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = Color(0xFF2D2D2D).copy(alpha = 0.9f),
                    topLeft = Offset(badgeLeft, badgeTop),
                    size = Size(badgeWidth, badgeTotalHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    textCenterX,
                    textBaselineY,
                    textPaint
                )
            }
        }

        val labelIndices = rememberLabelIndices(points.size)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labelIndices.forEach { index ->
                Text(
                    text = formatChartLabel(points[index].timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

private fun rememberLabelIndices(size: Int): List<Int> {
    if (size <= 4) {
        return (0 until size).toList()
    }

    return listOf(0, size / 3, (size * 2) / 3, size - 1).distinct().sorted()
}

private fun formatChartLabel(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
    return formatter.format(Date(timestamp))
}