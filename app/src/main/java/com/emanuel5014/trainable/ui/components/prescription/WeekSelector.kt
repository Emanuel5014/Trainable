package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/**
 * Connected toggle row W1…Wn, same visual language as the routine day picker.
 * [currentWeek] gets a dot marker; [dimmedWeeks] (e.g. weeks where an exercise is skipped) are faded.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeekSelector(
    weeksCount: Int,
    selectedWeek: Int,
    onWeekSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    currentWeek: Int? = null,
    dimmedWeeks: Set<Int> = emptySet(),
    hapticEnabled: Boolean = true,
    onAddWeek: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    LaunchedEffect(selectedWeek, weeksCount) {
        // Keep the selected week visible on long programs
        val fraction = if (weeksCount <= 1) 0f else (selectedWeek - 1f) / (weeksCount - 1f)
        scrollState.animateScrollTo((scrollState.maxValue * fraction).toInt())
    }

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..weeksCount).forEach { week ->
            val checked = week == selectedWeek
            ToggleButton(
                checked = checked,
                onCheckedChange = {
                    if (!checked) {
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onWeekSelected(week)
                    }
                },
                modifier = Modifier
                    .widthIn(min = 52.dp)
                    .alpha(if (week in dimmedWeeks && !checked) 0.45f else 1f),
                shapes = when {
                    weeksCount == 1 -> ToggleButtonDefaults.shapes()
                    week == 1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    week == weeksCount && onAddWeek == null -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = SurfaceContainerHigh,
                    contentColor = OnSurfaceVariant,
                    checkedContainerColor = Primary,
                    checkedContentColor = OnPrimary
                ),
                contentPadding = ToggleButtonDefaults.ContentPadding
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.week_short, week),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (week == currentWeek) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(4.dp)
                                .background(if (checked) OnPrimary else Primary, CircleShape)
                        )
                    }
                }
            }
        }
        if (onAddWeek != null) {
            FilledTonalIconButton(
                onClick = {
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAddWeek()
                },
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Primary.copy(alpha = 0.12f),
                    contentColor = Primary
                )
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_week))
            }
        }
    }
}
