package com.emanuel5014.trainable.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest

private data class ExerciseTypeOption(
    val type: String,
    @StringRes val label: Int,
    @StringRes val hint: Int,
    val icon: ImageVector
)

private val StrengthOption = ExerciseTypeOption(
    type = "strength",
    label = R.string.exercise_type_strength,
    hint = R.string.exercise_type_strength_hint,
    icon = Icons.Rounded.FitnessCenter
)
private val TimeAndWeightOption = ExerciseTypeOption(
    type = "time_and_weight",
    label = R.string.exercise_type_time_and_weight,
    hint = R.string.exercise_type_time_and_weight_hint,
    icon = Icons.Rounded.Timer
)
private val TimeAndRepsOption = ExerciseTypeOption(
    type = "time_and_reps",
    label = R.string.exercise_type_time_and_reps,
    hint = R.string.exercise_type_time_and_reps_hint,
    icon = Icons.Rounded.Timelapse
)
private val AdvancedOption = ExerciseTypeOption(
    type = "advanced",
    label = R.string.exercise_type_advanced,
    hint = R.string.advanced_prescription_hint,
    icon = Icons.Rounded.Percent
)

private val MIN_TILE_WIDTH = 116.dp

/**
 * Picks how an exercise is prescribed: weight & reps, time & weight, time & reps, or (with advanced programming on) an
 * advanced block prescription. [selectedType] and [onTypeSelected] use the `exerciseType` ids
 * (`strength`, `time_and_weight`, `time_and_reps`, `advanced`); the line under the tiles says what the selected one
 * means. Tiles too narrow for their names go two by two.
 */
@Composable
fun ExerciseTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    showAdvanced: Boolean = false
) {
    val options = remember(showAdvanced) {
        if (showAdvanced) listOf(StrengthOption, TimeAndWeightOption, TimeAndRepsOption, AdvancedOption)
        else listOf(StrengthOption, TimeAndWeightOption, TimeAndRepsOption)
    }
    val fontScale = LocalDensity.current.fontScale

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // One row while every tile keeps room for a long name ("Ripetizioni"), two by two otherwise
        val tileWidth = (maxWidth - 8.dp * (options.size - 1)) / options.size
        val rows = if (options.size > 2 && tileWidth < MIN_TILE_WIDTH * fontScale.coerceAtLeast(1f)) options.chunked(2) else listOf(options)
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { option ->
                        ExerciseTypeTile(
                            option = option,
                            selected = option.type == selectedType,
                            onClick = { onTypeSelected(option.type) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
        }

        AnimatedContent(
            targetState = options.firstOrNull { it.type == selectedType }?.hint,
            label = "exercise_type_hint"
        ) { hint ->
            if (hint != null) {
                Text(
                    text = stringResource(hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ExerciseTypeTile(
    option: ExerciseTypeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val container by animateColorAsState(
        targetValue = if (selected) Primary.copy(alpha = 0.12f) else SurfaceContainerHighest,
        label = "tile_container"
    )
    val border by animateColorAsState(
        targetValue = if (selected) Primary else Color.Transparent,
        label = "tile_border"
    )
    val iconContainer by animateColorAsState(
        targetValue = if (selected) Primary else OnSurfaceVariant.copy(alpha = 0.12f),
        label = "tile_icon_container"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) OnPrimary else OnSurfaceVariant,
        label = "tile_icon_tint"
    )

    Column(
        modifier = modifier
            .clip(shape)
            .background(container)
            .border(width = 1.5.dp, color = border, shape = shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = stringResource(option.label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) Primary else OnSurface,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
