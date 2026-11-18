package com.emanuel5014.trainable.ui.components.heart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.data.local.entity.SessionHeartDataEntity
import com.emanuel5014.trainable.domain.heart.HeartMath
import com.emanuel5014.trainable.domain.heart.HeartPoint
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.heart_active_kcal
import com.emanuel5014.trainable.sharedui.resources.heart_avg
import com.emanuel5014.trainable.sharedui.resources.heart_max
import com.emanuel5014.trainable.sharedui.resources.heart_title
import com.emanuel5014.trainable.sharedui.resources.heart_total_kcal
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** A workout's heart rate as the share card shows it. */
data class ShareHeart(
    val avgBpm: Int,
    val maxBpm: Int?,
    val kcal: Double?,
    /** [kcal] are the active ones (else the total). */
    val activeKcal: Boolean,
    val points: List<HeartPoint>,
    val durationSeconds: Int,
    /** Seconds in zones 1..5; null when the readings are too sparse for zones. */
    val zoneSeconds: List<Int>?,
    val source: String?
)

/** The saved heart rate of a workout, ready for the share card; null without heart rate. */
fun SessionHeartDataEntity.toShareHeart(maxHeartRate: Int): ShareHeart? {
    val avg = avgBpm ?: return null
    val points = HeartMath.decode(samples)
    val duration = ((windowEnd - windowStart) / 1000L).toInt().coerceAtLeast(1)
    val zones = HeartMath.zoneSeconds(points, duration, maxHeartRate).takeIf { it.sum() > 0 && it.sum() * 2 >= duration }
    return ShareHeart(
        avgBpm = avg,
        maxBpm = maxBpm,
        kcal = activeKcal ?: totalKcal,
        activeKcal = activeKcal != null,
        points = points,
        durationSeconds = duration,
        zoneSeconds = zones,
        source = source
    )
}

/**
 * The heart rate on the share card, in the glass style of its statistics bar: three numbers, a thin curve and the
 * zones as a bar (the colours speak for themselves in a picture).
 */
@Composable
fun ShareHeartSection(heart: ShareHeart, textPrimary: Color, textSecondary: Color, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = HeartRed, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(Res.string.heart_title).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = HeartRed,
                letterSpacing = 1.4.sp,
                modifier = Modifier.weight(1f)
            )
            heart.source?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            val labelColor = textSecondary.copy(alpha = 0.7f)
            ShareHeartValue(heart.avgBpm.toString(), stringResource(Res.string.heart_avg), textPrimary, labelColor, Modifier.weight(1f))
            heart.maxBpm?.let { ShareHeartValue(it.toString(), stringResource(Res.string.heart_max), textPrimary, labelColor, Modifier.weight(1f)) }
            heart.kcal?.let {
                ShareHeartValue(
                    it.roundToInt().toString(),
                    stringResource(if (heart.activeKcal) Res.string.heart_active_kcal else Res.string.heart_total_kcal),
                    textPrimary,
                    labelColor,
                    Modifier.weight(1f)
                )
            }
        }
        if (heart.points.size >= 2) {
            HeartCurve(
                points = heart.points,
                durationSeconds = heart.durationSeconds,
                average = heart.avgBpm,
                labelColor = null,
                strokeWidth = 1.6.dp,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )
        }
        heart.zoneSeconds?.let { ZoneBar(it, height = 6.dp, empty = Color.White.copy(alpha = 0.06f)) }
    }
}

@Composable
private fun ShareHeartValue(value: String, label: String, valueColor: Color, labelColor: Color, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = valueColor, maxLines = 1)
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The switch in the share preview that puts the heart rate on the card or takes it off. */
@Composable
fun ShareHeartToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        label = { Text(stringResource(Res.string.heart_title), fontWeight = FontWeight.Bold) },
        leadingIcon = {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                tint = if (checked) HeartRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        },
        modifier = modifier
    )
}
