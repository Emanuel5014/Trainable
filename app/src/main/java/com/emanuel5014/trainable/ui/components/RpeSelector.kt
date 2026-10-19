package com.emanuel5014.trainable.ui.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
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
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest

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
    scale: RpeScale,
    modifier: Modifier = Modifier,
    /** A tick when a level is picked on the simplified scale; the workout passes the app's vibration setting. */
    hapticEnabled: Boolean = false
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.rpe_optional).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            fontWeight = FontWeight.Black
        )
        if (scale == RpeScale.BASIC) {
            LevelTiles(value = value, onValueChange = onValueChange, hapticEnabled = hapticEnabled)
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

/**
 * The five levels as a connected expressive group: neutral tiles with the level's coloured icon; the chosen one fills
 * with that colour, takes more of the row with a little spring and pops its icon, and the caption under the row
 * slides in with its name and what it means.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LevelTiles(value: Float?, onValueChange: (Float?) -> Unit, hapticEnabled: Boolean) {
    val picked = RpeLevel.entries.firstOrNull { it.value == value }
    val haptic = LocalHapticFeedback.current
    // On a light theme the icons are darkened so the amber one still reads on a neutral tile
    val lightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        RpeLevel.entries.forEachIndexed { index, level ->
            val selected = level == picked
            val base = level.color()
            val idleIcon = if (lightTheme) lerp(base, Color.Black, 0.3f) else base
            val container by animateColorAsState(
                targetValue = if (selected) base else SurfaceContainerHighest,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "rpeTileContainer"
            )
            val content by animateColorAsState(
                targetValue = if (selected) base.onLevelColor() else idleIcon,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "rpeTileIcon"
            )
            // The chosen tile takes more of the row, with a little overshoot
            val share by animateFloatAsState(
                targetValue = if (selected) 1.9f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "rpeTileShare"
            )
            // The icon pops when its tile is chosen (not when the panel opens with a level already there)
            val pop = remember { Animatable(1f) }
            var firstComposition by remember { mutableStateOf(true) }
            LaunchedEffect(selected) {
                if (selected && !firstComposition) {
                    pop.snapTo(0.6f)
                    pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
                }
                firstComposition = false
            }
            val description = stringResource(level.nameRes()) + " " + level.range
            ToggleButton(
                checked = selected,
                onCheckedChange = {
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onValueChange(if (selected) null else level.value)
                },
                modifier = Modifier
                    .weight(share)
                    .height(52.dp)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = description
                    },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    RpeLevel.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = container,
                    contentColor = content,
                    checkedContainerColor = container,
                    checkedContentColor = content
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = level.icon(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                            rotationZ = (1f - pop.value) * -25f
                        }
                )
            }
        }
    }
    AnimatedContent(
        targetState = picked,
        transitionSpec = {
            (fadeIn(tween(220, delayMillis = 70)) + slideInVertically(tween(260)) { it / 3 }) togetherWith
                fadeOut(tween(90)) using SizeTransform(clip = false)
        },
        label = "rpeCaption"
    ) { level ->
        if (level != null) {
            Column {
                Text(
                    text = level.range + " · " + stringResource(level.nameRes()),
                    style = MaterialTheme.typography.titleSmall,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(level.descriptionRes()),
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
}

/** Icon colour on a tile filled with the level colour: dark on the light amber, white on the others. */
private fun Color.onLevelColor(): Color = if (luminance() > 0.35f) Color(0xFF1B1B1B) else Color.White
