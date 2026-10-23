package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.ui.graphics.Shape
import com.emanuel5014.trainable.ui.components.motion.RollingText
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import com.emanuel5014.trainable.ui.theme.OnTertiary
import com.emanuel5014.trainable.ui.theme.Tertiary

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RestTimerSection(
    remainingSeconds: Int,
    totalRestSeconds: Int = 90,
    onAddTime: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    onSubtractTime: () -> Unit = {},
    /** Which of the two time buttons to show (Workout settings). */
    showAdd: Boolean = true,
    showSubtract: Boolean = true,
    /** Seconds the two time buttons move the countdown by (Workout settings), only used for their labels. */
    addSeconds: Int = 30,
    subtractSeconds: Int = 10,
    /** How open the bottom hub is (1 open, 0 thin strip): the card shrinks and drops its label as it closes. */
    expansion: () -> Float = { 1f }
) {
    val isVisible = remainingSeconds > 0
    val progress = if (totalRestSeconds > 0) 1f - (remainingSeconds.toFloat() / totalRestSeconds.toFloat()) else 0f

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(WorkoutMotion.spatial(), initialScale = 0.92f) + fadeIn(WorkoutMotion.effects()) +
            expandVertically(WorkoutMotion.spatial(), expandFrom = Alignment.Bottom),
        exit = scaleOut(WorkoutMotion.spatial(), targetScale = 0.92f) + fadeOut(WorkoutMotion.effects()) +
            shrinkVertically(WorkoutMotion.spatial(), shrinkTowards = Alignment.Bottom),
        modifier = modifier
    ) {
        val e = expansion()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(lerp(18.dp, 24.dp, e)))
                .background(Tertiary)
                .padding(lerp(9.dp, 20.dp, e))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularWavyProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(lerp(32.dp, 56.dp, e)),
                            color = OnTertiary,
                            trackColor = OnTertiary.copy(alpha = 0.2f),
                            stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
                            trackStroke = WavyProgressIndicatorDefaults.circularTrackStroke
                        )
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Rest Timer",
                            tint = OnTertiary,
                            modifier = Modifier.size(lerp(16.dp, 24.dp, e))
                        )
                    }
                    Spacer(modifier = Modifier.width(lerp(10.dp, 16.dp, e)))
                    Column {
                        HubSlot(expansion = expansion, from = 0.3f, to = 0.55f) {
                            Text(
                                text = "REST",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnTertiary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        val minutes = remainingSeconds / 60
                        val seconds = remainingSeconds % 60
                        RollingText(
                            text = String.format("%d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.titleLarge.hubScaled(lerp(0.78f, 1f, e)),
                            color = OnTertiary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimerAdjustButtons(
                        showSubtract = showSubtract,
                        subtractLabel = "−${subtractSeconds}s",
                        onSubtract = onSubtractTime,
                        showAdd = showAdd,
                        addLabel = "+${addSeconds}s",
                        onAdd = onAddTime
                    )

                    FilledIconButton(
                        onClick = onSkip,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = OnTertiary,
                            contentColor = Tertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Skip Rest",
                        )
                    }
                }
            }
        }
    }
}

/** A small text button on a timer card that moves the countdown, e.g. "+30s" or "−10s". */
@Composable
fun TimerAdjustButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, shape: Shape = IconButtonDefaults.filledShape) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = OnTertiary.copy(alpha = 0.1f),
            contentColor = OnTertiary
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
    }
}

/** The two time buttons of a timer card; when both are there they are one connected pair. */
@Composable
fun TimerAdjustButtons(
    showSubtract: Boolean,
    subtractLabel: String,
    onSubtract: () -> Unit,
    showAdd: Boolean,
    addLabel: String,
    onAdd: () -> Unit
) {
    val pair = showSubtract && showAdd
    val start = if (pair) RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp) else IconButtonDefaults.filledShape
    val end = if (pair) RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp) else IconButtonDefaults.filledShape
    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        if (showSubtract) TimerAdjustButton(label = subtractLabel, onClick = onSubtract, shape = start)
        if (showAdd) TimerAdjustButton(label = addLabel, onClick = onAdd, shape = end)
    }
}
