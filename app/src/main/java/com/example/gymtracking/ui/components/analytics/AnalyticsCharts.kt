package com.example.gymtracking.ui.components.analytics

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.gymtracking.ui.screens.analytics.AnalyticsChartPoint
import com.example.gymtracking.ui.theme.OnSurfaceVariant
import com.example.gymtracking.ui.theme.Primary
import com.example.gymtracking.ui.theme.SurfaceContainerHigh
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
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
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
                drawContext.canvas.nativeCanvas.apply {
                    val text = String.format("%.1f kg", point.value)
                    drawText(
                        text,
                        x,
                        y - 30f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#666666")
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
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