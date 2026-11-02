package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.Primary
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch
import androidx.compose.ui.util.lerp as lerpFloat

// Same footprint as the Material 3 switch, so rows do not move when the glass is turned on
private val TrackWidth = 52.dp
private val TrackHeight = 32.dp
private val ThumbWidth = 34.dp
private val ThumbHeight = 26.dp
private val ThumbInset = 3.dp

/**
 * A switch in Liquid Glass, as on iOS 26: a solid pill thumb at rest that swells into a drop of glass,
 * bending the track beneath, while it is touched or moves. Tap it or drag the thumb.
 * [background] is the colour around the switch (the card it sits on).
 *
 * Modelled on the LiquidToggle example of Kyant0/AndroidLiquidGlass (Apache-2.0).
 */
@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scope = rememberCoroutineScope()
    val position = remember { Animatable(if (checked) 1f else 0f) }
    val press = remember { Animatable(0f) }
    val currentChecked = rememberUpdatedState(checked)
    val currentOnChange = rememberUpdatedState(onCheckedChange)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // A new value (tap, drag or from outside): the thumb travels as a drop and turns solid again on arrival
    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        if (position.value != target) {
            launch { press.animateTo(1f, spring(1f, 1000f)) }
            position.animateTo(target, spring(0.7f, 420f))
        }
        press.animateTo(0f, spring(1f, 600f))
    }

    val trackOff = MaterialTheme.colorScheme.surfaceContainerHighest
    val trackOn = Primary
    val trackBackdrop = rememberLayerBackdrop()
    val around = rememberCanvasBackdrop { drawRect(background) }
    val thumbBackdrop = rememberCombinedBackdrop(
        around,
        rememberBackdrop(trackBackdrop) { drawTrack ->
            // The drop magnifies the part of the track it covers
            val p = press.value
            scale(lerpFloat(2f / 3f, 0.75f, p), lerpFloat(0f, 0.75f, p)) { drawTrack() }
        }
    )

    Box(
        modifier = modifier
            .size(TrackWidth, TrackHeight)
            .alpha(if (enabled) 1f else 0.38f)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = null,
                indication = null,
                onValueChange = onCheckedChange
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val travel = (TrackWidth - ThumbWidth - ThumbInset * 2).toPx()
                detectHorizontalDragGestures(
                    onDragStart = { scope.launch { press.animateTo(1f, spring(1f, 1000f)) } },
                    onDragEnd = {
                        val on = position.value >= 0.5f
                        if (on != currentChecked.value) {
                            currentOnChange.value(on)
                        } else {
                            scope.launch {
                                position.animateTo(if (on) 1f else 0f, spring(0.7f, 420f))
                                press.animateTo(0f, spring(1f, 600f))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            position.animateTo(if (currentChecked.value) 1f else 0f, spring(0.7f, 420f))
                            press.animateTo(0f, spring(1f, 600f))
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val delta = (if (rtl) -dragAmount else dragAmount) / travel
                    scope.launch { position.snapTo((position.value + delta).coerceIn(0f, 1f)) }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .drawBehind { drawRect(lerp(trackOff, trackOn, position.value)) }
                .size(TrackWidth, TrackHeight)
        )
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val start = ThumbInset.toPx()
                    val end = (TrackWidth - ThumbWidth - ThumbInset).toPx()
                    val x = lerpFloat(start, end, position.value)
                    translationX = if (rtl) -x else x
                }
                .drawBackdrop(
                    backdrop = thumbBackdrop,
                    shape = { Capsule() },
                    effects = {
                        val p = press.value
                        blur(8.dp.toPx() * (1f - p))
                        lens(5.dp.toPx() * p, 10.dp.toPx() * p, chromaticAberration = true)
                    },
                    highlight = {
                        val p = press.value
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = p
                        )
                    },
                    shadow = { Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.12f)) },
                    innerShadow = {
                        val p = press.value
                        InnerShadow(radius = 4.dp * p, alpha = p)
                    },
                    layerBlock = {
                        val s = lerpFloat(1f, 1.45f, press.value)
                        scaleX = s
                        scaleY = s
                    },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 1f - press.value)) }
                )
                .size(ThumbWidth, ThumbHeight)
        )
    }
}
