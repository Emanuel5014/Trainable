package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
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

private val ThumbWidth = 38.dp
private val ThumbHeight = 24.dp
private val TrackHeight = 6.dp

/**
 * The slider of the shared screens (Slider.kt) with its thumb in Liquid Glass: a solid white pill at rest that swells
 * into a drop of glass, bending the track beneath, while it is held or dragged, as on iOS 26.
 *
 * The drop follows the LiquidToggle example of Kyant0/AndroidLiquidGlass (Apache-2.0).
 */
@Composable
fun LiquidGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChangeFinished: (() -> Unit)?,
    colors: SliderColors,
    interactionSource: MutableInteractionSource
) {
    val trackBackdrop = rememberLayerBackdrop()
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        steps = steps,
        thumb = { GlassSliderThumb(interactionSource, enabled, trackBackdrop) },
        track = { sliderState ->
            // A thin iOS track with no gap and no stop dots, recorded for the drop to bend
            val active = colors.activeTrackColor
            val inactive = colors.inactiveTrackColor
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TrackHeight)
                    .layerBackdrop(trackBackdrop)
                    .alpha(if (enabled) 1f else 0.38f)
            ) {
                val range = sliderState.valueRange
                val span = range.endInclusive - range.start
                val fraction = if (span > 0f) ((sliderState.value - range.start) / span).coerceIn(0f, 1f) else 0f
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(inactive, cornerRadius = radius)
                drawRoundRect(active, size = Size(size.width * fraction, size.height), cornerRadius = radius)
            }
        },
        valueRange = valueRange
    )
}

@Composable
private fun GlassSliderThumb(
    interactionSource: InteractionSource,
    enabled: Boolean,
    trackBackdrop: Backdrop
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val press = animateFloatAsState(
        targetValue = if (pressed || dragged) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "glassSliderPress"
    )
    // What is around the track, for the part of the drop that reaches past it
    val around = rememberCanvasBackdrop { drawRect(Color.Transparent) }
    val backdrop = rememberCombinedBackdrop(around, trackBackdrop)

    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.38f)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    val p = press.value
                    blur(6.dp.toPx() * (1f - p))
                    lens(10.dp.toPx() * p, 20.dp.toPx() * p, chromaticAberration = true)
                },
                highlight = {
                    val p = press.value
                    Highlight.Ambient.copy(
                        width = Highlight.Ambient.width / 1.5f,
                        blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                        alpha = p
                    )
                },
                shadow = { Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.3f)) },
                innerShadow = {
                    // At rest a faint inner edge keeps the white pill apart from a white track (monochrome themes)
                    val p = press.value
                    InnerShadow(radius = lerp(1.5f, 4f, p).dp, alpha = lerp(0.35f, 1f, p))
                },
                layerBlock = {
                    val s = lerp(1f, 1.6f, press.value)
                    scaleX = s
                    scaleY = s
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 1f - press.value)) }
            )
            .size(ThumbWidth, ThumbHeight)
    )
}
