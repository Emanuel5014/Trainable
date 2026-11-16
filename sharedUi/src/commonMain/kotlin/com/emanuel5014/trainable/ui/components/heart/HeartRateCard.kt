package com.emanuel5014.trainable.ui.components.heart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.local.entity.SessionHeartDataEntity
import com.emanuel5014.trainable.domain.heart.HeartMath
import com.emanuel5014.trainable.domain.heart.HeartPoint
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.heart_active_kcal
import com.emanuel5014.trainable.sharedui.resources.heart_allow
import com.emanuel5014.trainable.sharedui.resources.heart_avg
import com.emanuel5014.trainable.sharedui.resources.heart_max
import com.emanuel5014.trainable.sharedui.resources.heart_refresh
import com.emanuel5014.trainable.sharedui.resources.heart_source
import com.emanuel5014.trainable.sharedui.resources.heart_title
import com.emanuel5014.trainable.sharedui.resources.heart_total_kcal
import com.emanuel5014.trainable.sharedui.resources.heart_zones
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** The heart's own red, the same in every theme: the heart icon and the curve. */
private val HeartRed = Color(0xFFE5484D)

/** Zones 1..5, from easy to all-out. */
private val ZoneColors = listOf(
    Color(0xFF8E9AAF),
    Color(0xFF4C9AFF),
    Color(0xFF3FB950),
    Color(0xFFF0A030),
    Color(0xFFE5484D)
)

/** What the card says when there is nothing saved yet. */
data class HeartCardMessage(val text: String, val action: String?, val onAction: (() -> Unit)?)

/**
 * The heart rate of a workout: average and highest bpm, calories, the curve over the workout and the time in each
 * zone. With no [data] it shows [message] (or a loader while [syncing]).
 */
@Composable
fun HeartRateCard(
    data: SessionHeartDataEntity?,
    maxHeartRate: Int,
    syncing: Boolean,
    onRefresh: (() -> Unit)?,
    message: HeartCardMessage?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(44.dp)) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = HeartRed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(Res.string.heart_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            data?.source?.let { source ->
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(Res.string.heart_source, source),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Spacer(Modifier.weight(1f))
            when {
                syncing -> Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { GymLoadingIndicator(size = 20.dp) }
                onRefresh != null && data != null -> IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = stringResource(Res.string.heart_refresh),
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                else -> Spacer(Modifier.height(44.dp))
            }
        }

        Column(modifier = Modifier.padding(end = 10.dp)) {
            if (data != null) {
                HeartData(data, maxHeartRate)
            } else if (message != null) {
                Text(message.text, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                if (message.action != null && message.onAction != null && !syncing) {
                    TextButton(onClick = message.onAction, modifier = Modifier.padding(top = 2.dp)) {
                        Text(message.action, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeartData(data: SessionHeartDataEntity, maxHeartRate: Int) {
    val points = remember(data.samples) { HeartMath.decode(data.samples) }
    val durationSeconds = ((data.windowEnd - data.windowStart) / 1000L).toInt().coerceAtLeast(1)
    val kcal = data.activeKcal ?: data.totalKcal

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        HeartNumber(data.avgBpm?.toString() ?: "–", stringResource(Res.string.heart_avg), Modifier.weight(1f))
        HeartNumber(data.maxBpm?.toString() ?: "–", stringResource(Res.string.heart_max), Modifier.weight(1f))
        if (kcal != null) {
            HeartNumber(
                kcal.roundToInt().toString(),
                stringResource(if (data.activeKcal != null) Res.string.heart_active_kcal else Res.string.heart_total_kcal),
                Modifier.weight(1f)
            )
        }
    }

    if (points.size >= 2) {
        HeartCurve(points, durationSeconds, Modifier.padding(top = 12.dp).fillMaxWidth().height(72.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("0:00", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(clock(durationSeconds), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        }

        val zones = remember(points, maxHeartRate, durationSeconds) { HeartMath.zoneSeconds(points, durationSeconds, maxHeartRate) }
        val total = zones.sum()
        // A band without a workout started reads every few minutes: its zones would cover a few minutes of an hour
        if (total > 0 && total * 2 >= durationSeconds) {
            Text(
                stringResource(Res.string.heart_zones),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
            )
            Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(SurfaceContainerHighest)) {
                zones.forEachIndexed { index, seconds ->
                    if (seconds > 0) {
                        Box(Modifier.weight(seconds.toFloat()).height(10.dp).background(ZoneColors[index]))
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                zones.forEachIndexed { index, seconds ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(ZoneColors[index]))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Z${index + 1} ${minutes(seconds)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (seconds > 0) OnSurface else OnSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeartNumber(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurface, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The curve over the workout, with the area under it lightly filled. A gap much longer than the data's usual spacing
 * (and over 2 minutes) breaks the line, so a band that reads every few minutes still draws one line.
 */
@Composable
private fun HeartCurve(points: List<HeartPoint>, durationSeconds: Int, modifier: Modifier = Modifier) {
    val low = points.minOf { it.bpm } - 5
    val high = points.maxOf { it.bpm } + 5
    val breakGap = remember(points) { HeartMath.breakGapSeconds(points) }
    Canvas(modifier) {
        val range = (high - low).coerceAtLeast(1).toFloat()
        fun x(point: HeartPoint) = size.width * point.offsetSeconds.coerceIn(0, durationSeconds) / durationSeconds
        fun y(point: HeartPoint) = size.height * (1f - (point.bpm - low) / range)

        val runs = mutableListOf<MutableList<HeartPoint>>()
        points.forEach { point ->
            val last = runs.lastOrNull()?.lastOrNull()
            if (last == null || point.offsetSeconds - last.offsetSeconds > breakGap) runs += mutableListOf(point) else runs.last() += point
        }
        runs.forEach { run ->
            if (run.size == 1) {
                drawCircle(HeartRed, radius = 2.dp.toPx(), center = Offset(x(run[0]), y(run[0])))
                return@forEach
            }
            val line = Path().apply {
                moveTo(x(run[0]), y(run[0]))
                run.drop(1).forEach { lineTo(x(it), y(it)) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(x(run.last()), size.height)
                lineTo(x(run[0]), size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(HeartRed.copy(alpha = 0.28f), HeartRed.copy(alpha = 0.02f))))
            drawPath(line, HeartRed, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

private fun clock(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return "$h:${m.toString().padStart(2, '0')}"
}

private fun minutes(seconds: Int): String = "${(seconds + 30) / 60}'"
