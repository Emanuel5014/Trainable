package com.emanuel5014.trainable.ui.components.glass

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.LocalLiquidGlassEnabled
import com.emanuel5014.trainable.ui.theme.liquidGlassSurface
import kotlinx.coroutines.delay
import androidx.compose.material3.Switch as MaterialSwitch

/** A sliding glass lens with native toggle semantics, keyboard access and a 48 dp touch target. */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
) {
    if (!LocalLiquidGlassEnabled.current || onCheckedChange == null) {
        MaterialSwitch(checked, onCheckedChange, modifier, thumbContent, enabled, colors, interactionSource)
        return
    }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    var pending by remember { mutableStateOf<Boolean?>(null) }
    val shownChecked = pending ?: checked
    val currentChecked by rememberUpdatedState(shownChecked)
    val change by rememberUpdatedState(onCheckedChange)
    // Bridge asynchronous preference writes without snapping the thumb back after release.
    LaunchedEffect(checked, pending) {
        if (pending == checked) pending = null
        else if (pending != null) { delay(500); pending = null }
    }
    val fraction by animateFloatAsState(if (dragging) dragFraction else if (shownChecked) 1f else 0f,
        animationSpec = if (dragging) snap() else tween(180), label = "glass_switch_position")
    val thumbWidth by animateDpAsState(if (pressed || dragging) 36.dp else 28.dp,
        animationSpec = tween(120), label = "glass_switch_lens_width")
    val density = LocalDensity.current
    val travelPx = with(density) { 24.dp.toPx() }
    val scheme = MaterialTheme.colorScheme
    val activeTint = if (enabled) colors.checkedTrackColor else colors.disabledCheckedTrackColor
    val inactiveTint = if (enabled) colors.uncheckedTrackColor else colors.disabledUncheckedTrackColor
    fun commit(value: Boolean) {
        pending = value
        change(value)
    }
    Box(modifier.width(72.dp).height(48.dp)
        .toggleable(value = shownChecked, role = Role.Switch, enabled = enabled, interactionSource = source,
            indication = null, onValueChange = { commit(it) })
        .draggable(state = rememberDraggableState { delta -> dragFraction = (dragFraction + delta / travelPx).coerceIn(0f, 1f) },
            orientation = Orientation.Horizontal, enabled = enabled,
            reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
            interactionSource = source,
            onDragStarted = { dragFraction = if (currentChecked) 1f else 0f; dragging = true },
            onDragStopped = { velocity ->
                val target = when { velocity > 400f -> true; velocity < -400f -> false; else -> dragFraction >= .5f }
                if (target != currentChecked) commit(target)
                dragging = false
            }), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 64.dp, height = 36.dp)
            .liquidGlassSurface(CircleShape, lerp(inactiveTint, activeTint, fraction).copy(alpha = .45f),
                source, enabled = enabled, control = true, quiet = true)
            .border(.75.dp, (if (shownChecked) scheme.primary else scheme.outlineVariant).copy(alpha = if (enabled) .45f else .2f), CircleShape)
            .padding(4.dp)) {
            Box(Modifier.align(Alignment.CenterStart).offset(x = (56.dp - thumbWidth) * fraction)
                .width(thumbWidth).height(28.dp)
                .liquidGlassSurface(CircleShape, lerp(scheme.surfaceContainerHighest, scheme.primaryContainer, fraction),
                    source, enabled = enabled, control = true, quiet = true)
                .border(.75.dp, scheme.onSurface.copy(alpha = if (enabled) .24f else .12f), CircleShape),
                contentAlignment = Alignment.Center) {
                thumbContent?.invoke()
            }
        }
    }
}
