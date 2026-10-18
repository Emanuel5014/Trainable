package com.emanuel5014.trainable.ui.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.rpe.RpeLevel
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.domain.rpe.RpeSettings
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary

/** Whether RPE is asked for and on which scale, provided once at the app root like [LocalAdvancedProgramming]. */
val LocalRpeSettings = staticCompositionLocalOf { RpeSettings() }

/**
 * The scale to edit an RPE with: the chosen one while RPE is on, and also for a value that is already there (so a set
 * that has an RPE can still be corrected with the switch off). Null when there is nothing to show.
 */
@Composable
fun editableRpeScale(hasValue: Boolean): RpeScale? {
    val settings = LocalRpeSettings.current
    return if (settings.enabled || hasValue) settings.scale else null
}

/** An RPE as a badge in the app: the level name on the simplified scale ("Hard"), "RPE 8" on the others. */
fun rpeBadgeText(context: Context, value: Float, scale: RpeScale): String =
    if (scale == RpeScale.BASIC) context.getString(RpeLevel.of(value).nameRes())
    else context.getString(R.string.rpe_short, PrescriptionFormatter.number(value))

@StringRes
fun RpeLevel.nameRes(): Int = when (this) {
    RpeLevel.WARM_UP -> R.string.rpe_level_warmup
    RpeLevel.EASY -> R.string.rpe_level_easy
    RpeLevel.MODERATE -> R.string.rpe_level_moderate
    RpeLevel.HARD -> R.string.rpe_level_hard
    RpeLevel.MAXIMUM -> R.string.rpe_level_maximum
}

@StringRes
fun RpeLevel.descriptionRes(): Int = when (this) {
    RpeLevel.WARM_UP -> R.string.rpe_level_warmup_desc
    RpeLevel.EASY -> R.string.rpe_level_easy_desc
    RpeLevel.MODERATE -> R.string.rpe_level_moderate_desc
    RpeLevel.HARD -> R.string.rpe_level_hard_desc
    RpeLevel.MAXIMUM -> R.string.rpe_level_maximum_desc
}

fun RpeLevel.icon(): ImageVector = when (this) {
    RpeLevel.WARM_UP -> Icons.Rounded.Spa
    RpeLevel.EASY -> Icons.Rounded.SentimentSatisfied
    RpeLevel.MODERATE -> Icons.Rounded.Speed
    RpeLevel.HARD -> Icons.Rounded.LocalFireDepartment
    RpeLevel.MAXIMUM -> Icons.Rounded.Bolt
}

/** Green, blue, yellow, orange, red: the effort heats up with the level. */
private fun RpeLevel.color(): Color = when (this) {
    RpeLevel.WARM_UP -> Color(0xFF2E9E6B)
    RpeLevel.EASY -> Color(0xFF3B7DDD)
    RpeLevel.MODERATE -> Color(0xFFE0A100)
    RpeLevel.HARD -> Color(0xFFE8672A)
    RpeLevel.MAXIMUM -> Color(0xFFD6453D)
}

/**
 * Asks how hard a set felt. The simplified [scale] is five icon tiles that name the level under them; the others are a
 * row of numbers. Tapping the chosen value clears it, and a value the scale does not offer (6.5 on the whole-number
 * scale) shows as nothing chosen until it is replaced.
 */
@Composable
fun RpeSelector(
    value: Float?,
    onValueChange: (Float?) -> Unit,
    scale: RpeScale = RpeScale.ADVANCED,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.rpe_optional).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            fontWeight = FontWeight.Black
        )
        if (scale == RpeScale.BASIC) {
            LevelTiles(value = value, onValueChange = onValueChange)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                scale.values.forEach { option ->
                    val selected = value == option
                    FilterChip(
                        selected = selected,
                        onClick = { onValueChange(if (selected) null else option) },
                        label = { Text(PrescriptionFormatter.number(option), fontWeight = FontWeight.ExtraBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = OnPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelTiles(value: Float?, onValueChange: (Float?) -> Unit) {
    val picked = RpeLevel.entries.firstOrNull { it.value == value }
    val shape = RoundedCornerShape(14.dp)
    // On a light theme the icons are darkened so the amber one still reads on its pale tint
    val lightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RpeLevel.entries.forEach { level ->
            val selected = level == picked
            val color = if (lightTheme) lerp(level.color(), Color.Black, 0.3f) else level.color()
            val description = stringResource(level.nameRes()) + " " + level.range
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .alpha(if (picked != null && !selected) 0.45f else 1f)
                    .clip(shape)
                    .background(color.copy(alpha = 0.16f))
                    .then(if (selected) Modifier.border(2.dp, color, shape) else Modifier)
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onValueChange(if (selected) null else level.value) }
                    )
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = level.icon(), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
    }
    if (picked != null) {
        Column {
            Text(
                text = picked.range + " · " + stringResource(picked.nameRes()),
                style = MaterialTheme.typography.titleSmall,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(picked.descriptionRes()),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
    } else {
        Text(
            text = stringResource(R.string.rpe_prompt),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}
