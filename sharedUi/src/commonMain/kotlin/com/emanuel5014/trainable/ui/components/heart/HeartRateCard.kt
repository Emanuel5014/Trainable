package com.emanuel5014.trainable.ui.components.heart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.data.local.entity.SessionHeartDataEntity
import com.emanuel5014.trainable.domain.heart.HeartMath
import com.emanuel5014.trainable.domain.heart.HeartPoint
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.heart_active_kcal
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
import com.emanuel5014.trainable.ui.theme.SurfaceContainerLow
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/** The heart's own red, the same in every theme: the heart icon and the curve. */
internal val HeartRed = Color(0xFFE5484D)

/** Zones 1..5, from easy to all-out. */
internal val ZoneColors = listOf(
    Color(0xFF8E9AAF),
    Color(0xFF4C9AFF),
    Color(0xFF3FB950),
    Color(0xFFF0A030),
    Color(0xFFE5484D)
)

/** What the card says when there is nothing saved yet. */
data class HeartCardMessage(val text: String, val action: String?, val onAction: (() -> Unit)?)

/**
 * The heart rate of a workout on one 16 dp grid: a header (heart, title, source, refresh), three centred tiles
 * (average, highest, calories), the curve with its average dashed and its range on the right, and the time in each
 * zone as a segmented bar with a centred legend. With no [data] it shows [message] (or a loader while [syncing]).
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HeartHeader(source = data?.source, syncing = syncing, onRefresh = onRefresh.takeIf { data != null })
        if (data != null) {
            HeartData(data, maxHeartRate)
        } else if (message != null) {
            Column {
                Text(message.text, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                if (message.action != null && message.onAction != null && !syncing) {
                    TextButton(onClick = message.onAction) {
                        Text(message.action, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeartHeader(source: String?, syncing: Boolean, onRefresh: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(HeartRed.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = HeartRed, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                stringResource(Res.string.heart_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurface
            )
            if (source != null) {
                Text(
                    stringResource(Res.string.heart_source, source),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        when {
            syncing -> Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) { GymLoadingIndicator(size = 20.dp) }
            onRefresh != null -> Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHighest)
                    .clickable(onClick = onRefresh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = stringResource(Res.string.heart_refresh),
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
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
        HeartTile(data.avgBpm?.toString() ?: "–", stringResource(Res.string.heart_avg), Modifier.weight(1f))
        HeartTile(data.maxBpm?.toString() ?: "–", stringResource(Res.string.heart_max), Modifier.weight(1f))
        if (kcal != null) {
            HeartTile(
                kcal.roundToInt().toString(),
                stringResource(if (data.activeKcal != null) Res.string.heart_active_kcal else Res.string.heart_total_kcal),
                Modifier.weight(1f)
            )
        }
    }

    if (points.size >= 2) {
        Column {
            HeartCurve(
                points = points,
                durationSeconds = durationSeconds,
                average = data.avgBpm,
                // The curve is averaged into buckets: the labels name the real highest and lowest reading
                highestLabel = data.maxBpm,
                lowestLabel = data.minBpm,
                labelColor = OnSurfaceVariant,
                modifier = Modifier.fillMaxWidth().height(84.dp)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = CURVE_LABEL_GUTTER)) {
                val style = MaterialTheme.typography.labelSmall
                Text("0:00", style = style, color = OnSurfaceVariant, modifier = Modifier.weight(1f))
                Text(clock(durationSeconds / 2), style = style, color = OnSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(clock(durationSeconds), style = style, color = OnSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }

        val zones = remember(points, maxHeartRate, durationSeconds) { HeartMath.zoneSeconds(points, durationSeconds, maxHeartRate) }
        val total = zones.sum()
        // A band without a workout started reads every few minutes: its zones would cover a few minutes of an hour
        if (total > 0 && total * 2 >= durationSeconds) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(Res.string.heart_zones).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = OnSurfaceVariant
                )
                ZoneBar(zones, height = 10.dp, empty = SurfaceContainerLow)
                Row(modifier = Modifier.fillMaxWidth()) {
                    zones.forEachIndexed { index, seconds ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                "Z${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = ZoneColors[index]
                            )
                            Text(
                                minutes(seconds),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (seconds > 0) OnSurface else OnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeartTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 6.dp, vertical = 10.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = OnSurface, maxLines = 1)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** The time in each zone as rounded segments with small gaps; [empty] fills the track when nothing is in a zone. */
@Composable
internal fun ZoneBar(zones: List<Int>, height: Dp, empty: Color, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(empty)
    ) {
        zones.forEachIndexed { index, seconds ->
            // Under half a minute the legend reads 0': no sliver either
            if (seconds >= 30) {
                Box(
                    Modifier
                        .weight(seconds.toFloat())
                        .height(height)
                        .clip(RoundedCornerShape(height / 2))
                        .background(ZoneColors[index])
                )
            }
        }
    }
}

/** Room on the right of the curve for its highest, average and lowest bpm. */
private val CURVE_LABEL_GUTTER = 28.dp

/**
 * The curve over the workout with the area under it lightly filled. With [labelColor] the average is dashed across
 * and the highest, average and lowest bpm are written in a gutter on the right. A gap much longer than the data's
 * usual spacing (and over 2 minutes) breaks the line.
 */
@Composable
internal fun HeartCurve(
    points: List<HeartPoint>,
    durationSeconds: Int,
    average: Int?,
    labelColor: Color?,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    highestLabel: Int? = null,
    lowestLabel: Int? = null
) {
    val highest = highestLabel ?: points.maxOf { it.bpm }
    val lowest = lowestLabel ?: points.minOf { it.bpm }
    val low = minOf(lowest, points.minOf { it.bpm }) - 5
    val high = maxOf(highest, points.maxOf { it.bpm }) + 5
    val breakGap = remember(points) { HeartMath.breakGapSeconds(points) }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor ?: Color.Transparent)

    Canvas(modifier) {
        val gutter = if (labelColor != null) CURVE_LABEL_GUTTER.toPx() else 0f
        val width = size.width - gutter
        val range = (high - low).coerceAtLeast(1).toFloat()
        fun x(point: HeartPoint) = width * point.offsetSeconds.coerceIn(0, durationSeconds) / durationSeconds
        fun y(bpm: Int) = size.height * (1f - (bpm - low) / range)

        if (labelColor != null && average != null) {
            drawLine(
                color = labelColor.copy(alpha = 0.55f),
                start = Offset(0f, y(average)),
                end = Offset(width, y(average)),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
            )
        }

        val runs = mutableListOf<MutableList<HeartPoint>>()
        points.forEach { point ->
            val last = runs.lastOrNull()?.lastOrNull()
            if (last == null || point.offsetSeconds - last.offsetSeconds > breakGap) runs += mutableListOf(point) else runs.last() += point
        }
        runs.forEach { run ->
            if (run.size == 1) {
                drawCircle(HeartRed, radius = strokeWidth.toPx(), center = Offset(x(run[0]), y(run[0].bpm)))
                return@forEach
            }
            val line = Path().apply {
                moveTo(x(run[0]), y(run[0].bpm))
                run.drop(1).forEach { lineTo(x(it), y(it.bpm)) }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(x(run.last()), size.height)
                lineTo(x(run[0]), size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(HeartRed.copy(alpha = 0.30f), HeartRed.copy(alpha = 0.02f))))
            drawPath(line, HeartRed, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        if (labelColor != null) {
            val minGap = 12.dp.toPx()
            val labels = buildList {
                add(highest to y(highest))
                if (average != null && abs(y(average) - y(highest)) > minGap && abs(y(average) - y(lowest)) > minGap) add(average to y(average))
                add(lowest to y(lowest))
            }
            labels.forEach { (bpm, centre) ->
                val text = measurer.measure(bpm.toString(), labelStyle)
                val top = (centre - text.size.height / 2f).coerceIn(0f, size.height - text.size.height)
                drawText(text, topLeft = Offset(size.width - text.size.width, top))
            }
        }
    }
}

private fun clock(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return "$h:${m.toString().padStart(2, '0')}"
}

private fun minutes(seconds: Int): String = "${(seconds + 30) / 60}'"
