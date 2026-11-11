package com.emanuel5014.trainable.ui.screens.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.emanuel5014.trainable.ui.theme.Surface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.no_active_routines_message
import com.emanuel5014.trainable.sharedui.resources.quick_workout
import com.emanuel5014.trainable.sharedui.resources.resume_workout
import com.emanuel5014.trainable.sharedui.resources.select_routine
import com.emanuel5014.trainable.sharedui.resources.started
import com.emanuel5014.trainable.sharedui.resources.suggested_plan
import com.emanuel5014.trainable.sharedui.resources.today_workout
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.util.DateFormatter
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Colours, icon and label that tell the three kinds of action apart at a glance. */
private data class ActionStyle(
    val container: Color,
    val content: Color,
    val accent: Color,
    val onAccent: Color,
    val icon: ImageVector,
    val label: StringResource,
    /** Painted instead of [container] on the main card (resume only, like the membership card). */
    val brush: Brush? = null,
    /** Leading shape, its icon and the label colour on a neutral row. */
    val rowLead: Color = accent,
    val rowOnLead: Color = onAccent,
    val rowLabel: Color = accent
)

/**
 * Fixed warm colour of an unfinished workout. Like the red of an expired membership it does not follow the palette,
 * so "resume" stands out even with Monochrome or dynamic colours.
 */
private val ResumeAmber = Color(0xFFFFA94D)
private val ResumeAmberDark = Color(0xFF8A5300)
private val ResumeInk = Color(0xFF1A1A1A)

@Composable
private fun actionStyle(action: DashboardAction): ActionStyle {
    val scheme = MaterialTheme.colorScheme
    return when (action) {
        is DashboardAction.Resume -> {
            // Same light/dark rule and gradient strength as GymMembershipCard
            val isDark = with(scheme.surface) { red + green + blue < 1.5f }
            ActionStyle(
                container = ResumeAmber, content = ResumeInk, accent = ResumeInk, onAccent = ResumeAmber,
                icon = Icons.Rounded.PlayArrow, label = Res.string.resume_workout,
                brush = Brush.linearGradient(
                    if (isDark) listOf(ResumeAmber.copy(alpha = 0.9f), ResumeAmber.copy(alpha = 0.6f))
                    else listOf(ResumeAmber.copy(alpha = 0.45f), ResumeAmber.copy(alpha = 0.25f))
                ),
                rowLead = ResumeAmber, rowOnLead = ResumeInk,
                rowLabel = if (isDark) ResumeAmber else ResumeAmberDark
            )
        }
        is DashboardAction.Today -> ActionStyle(
            scheme.primary, scheme.onPrimary, scheme.onPrimary, scheme.primary,
            Icons.Rounded.Event, Res.string.today_workout,
            // On a neutral row Today's own accent (onPrimary, white) would vanish: there its kind colour is primary.
            rowLead = scheme.primary, rowOnLead = scheme.onPrimary, rowLabel = scheme.primary
        )
        is DashboardAction.Suggested -> ActionStyle(
            scheme.secondaryContainer, scheme.onSecondaryContainer, scheme.onSecondaryContainer, scheme.secondaryContainer,
            Icons.Rounded.FitnessCenter, Res.string.suggested_plan
        )
    }
}

private fun DashboardAction.title(): String = when (this) {
    is DashboardAction.Resume -> session.displayName
    is DashboardAction.Today -> plan.nome
    is DashboardAction.Suggested -> plan.nome
}

@Composable
private fun DashboardAction.subtitle(): String = when (this) {
    is DashboardAction.Resume -> stringResource(Res.string.started, DateFormatter.format(session.session.timestamp))
    is DashboardAction.Today -> plan.note ?: stringResource(Res.string.select_routine)
    is DashboardAction.Suggested -> plan.note ?: stringResource(Res.string.select_routine)
}

/** Selected cards get the same light primary wash the old resume card had. */
@Composable
private fun selectedContainer(base: Color, isSelected: Boolean): Color =
    if (isSelected) Primary.copy(alpha = 0.12f).compositeOver(base) else base

/** The big card under the info cards: the first thing to start or continue. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DashboardMainActionCard(
    action: DashboardAction,
    weekPill: String?,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    /** Null for routines: a long press there should not buzz and swallow the tap. */
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** False while the Dashboard is not on screen: the resume animations stop, like the info cards'. */
    isActive: Boolean = true
) {
    val style = actionStyle(action)
    val animated = style.brush != null && isActive && !isSelectionMode
    val wavePhase = remember { Animatable(0f) }
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(animated) {
        if (animated) {
            while (true) {
                wavePhase.animateTo(1f, tween(6000, easing = LinearEasing))
                wavePhase.snapTo(0f)
            }
        }
    }
    LaunchedEffect(animated) {
        if (animated) {
            while (true) {
                pulse.snapTo(0f)
                pulse.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
                delay(400)
            }
        } else {
            pulse.snapTo(0f)
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.extraLarge)
            .then(
                if (style.brush != null) {
                    Modifier
                        .background(Surface)
                        .background(style.brush)
                        .drawBehind { drawResumeWave(wavePhase.value) }
                        .then(if (isSelected) Modifier.background(Primary.copy(alpha = 0.12f)) else Modifier)
                } else {
                    Modifier.background(selectedContainer(style.container, isSelected))
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(style.icon, contentDescription = null, tint = style.accent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(style.label),
                    style = MaterialTheme.typography.labelMedium,
                    color = style.accent,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = action.title(),
                style = MaterialTheme.typography.headlineSmall,
                color = style.content,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = action.subtitle(),
                style = MaterialTheme.typography.bodyMedium,
                color = style.content.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (weekPill != null) {
                Spacer(modifier = Modifier.height(6.dp))
                PrescriptionPill(
                    text = weekPill,
                    containerColor = style.content.copy(alpha = 0.14f),
                    contentColor = style.content,
                    emphasized = true
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(56.dp)
                .then(
                    // "Live" wave leaving the play button of an unfinished workout
                    if (style.brush != null) Modifier.drawBehind {
                        val p = pulse.value
                        if (p > 0f) {
                            drawCircle(
                                color = style.accent.copy(alpha = 0.35f * (1f - p)),
                                radius = size.minDimension / 2f * (1f + 0.45f * p)
                            )
                        }
                    } else Modifier
                )
                .clip(CircleShape)
                .background(if (isSelectionMode && !isSelected) style.accent.copy(alpha = 0.3f) else style.accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = style.onAccent,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** A further action under the main card: same kind colours, smaller. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DashboardActionRow(
    action: DashboardAction,
    weekPill: String?,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    /** Null for routines: a long press there should not buzz and swallow the tap. */
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val style = actionStyle(action)
    val lead = style.rowLead
    val onLead = style.rowOnLead
    val labelColor = style.rowLabel
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.large)
            .background(selectedContainer(SurfaceContainerHigh, isSelected))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelectionMode && !isSelected) lead.copy(alpha = 0.4f) else lead),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.Check else style.icon,
                contentDescription = null,
                tint = onLead,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(style.label),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = action.title(),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Two unfinished workouts can share a name: the start date tells them apart, as on the old cards
            if (action is DashboardAction.Resume) {
                Text(
                    text = action.subtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (weekPill != null) {
            PrescriptionPill(
                text = weekPill,
                containerColor = Primary.copy(alpha = 0.12f),
                contentColor = Primary,
                emphasized = true
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
    }
}

/** Swipe left to ask for deleting an unfinished workout (the dialog does the deleting); the card springs back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionSwipeToDelete(
    enabled: Boolean,
    onDeleteRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onDeleteRequest()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            val progress = dismissState.progress
            val color by animateColorAsState(
                when {
                    progress > 0f && dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart -> Error.copy(alpha = 0.6f)
                    else -> Color.Transparent
                }, label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(Shapes.extraLarge)
                    .background(color)
                    .padding(horizontal = 28.dp)
            ) {
                if (progress > 0f && dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Rounded.DeleteSweep,
                        contentDescription = "Delete",
                        tint = Error,
                        modifier = Modifier.align(Alignment.CenterEnd).size(28.dp)
                    )
                }
            }
        }
    ) {
        content()
    }
}

/** Starts a workout without a routine; replaces the old floating button (hidden with the Expressive navbar). */
@Composable
internal fun QuickWorkoutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    GymButton(
        onClick = onClick,
        containerColor = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer,
        height = 56,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.Bolt, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.quick_workout),
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Shown in the action zone when there is nothing to start or continue. Not clickable. */
@Composable
internal fun NoRoutinesInvite(modifier: Modifier = Modifier) {
    val outline = OutlineVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val radius = 24.dp.toPx()
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))
                    )
                )
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.AutoMirrored.Rounded.ListAlt, contentDescription = null, tint = OnSurfaceVariant)
        Text(
            text = stringResource(Res.string.no_active_routines_message),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant
        )
    }
}

/**
 * One wave flowing across the whole resume card, the same family as the weekly goal's wave. [phase] runs 0..1, one
 * wavelength.
 */
private fun DrawScope.drawResumeWave(phase: Float) {
    val wavelength = size.width / 2f
    drawPath(
        wavePath(baseline = size.height * 0.58f, amplitude = 7.dp.toPx(), wavelength, shift = -phase * wavelength),
        color = Color.White.copy(alpha = 0.18f)
    )
}

/** A filled wave from [baseline] down to the bottom edge, moved sideways by [shift]. */
private fun DrawScope.wavePath(baseline: Float, amplitude: Float, wavelength: Float, shift: Float): Path = Path().apply {
    val half = wavelength / 2f
    var x = -wavelength + shift % wavelength - wavelength
    moveTo(x, size.height)
    lineTo(x, baseline)
    var up = true
    while (x < size.width + wavelength) {
        quadraticTo(x + half / 2f, baseline + if (up) -amplitude else amplitude, x + half, baseline)
        x += half
        up = !up
    }
    lineTo(x, size.height)
    close()
}
