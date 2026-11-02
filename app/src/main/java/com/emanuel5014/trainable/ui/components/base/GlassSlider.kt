package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.ui.theme.LocalGlassSettings
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

private val ThumbWidth = 36.dp
private val ThumbHeight = 24.dp

/**
 * The Material 3 slider, with its thumb in Liquid Glass when the user has the sliders part on: a solid white
 * pill at rest that swells into a drop of glass, bending the track beneath, while it is held or dragged, as on
 * iOS 26. Same parameters as the Material slider, so it replaces it by import.
 *
 * A slider whose track is clear (colour pickers draw their own gradient under it) keeps the plain thumb.
 *
 * The drop follows the LiquidToggle example of Kyant0/AndroidLiquidGlass (Apache-2.0).
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val glass = LocalGlassSettings.current.isOn(GlassPart.Sliders) && colors.activeTrackColor != Color.Transparent
    if (!glass) {
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            colors = colors,
            interactionSource = interactionSource
        )
        return
    }

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
            // Recorded for the thumb to bend; the thumb itself is not part of it
            Box(modifier = Modifier.layerBackdrop(trackBackdrop), propagateMinConstraints = true) {
                SliderDefaults.Track(colors = colors, enabled = enabled, sliderState = sliderState)
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
                    lens(6.dp.toPx() * p, 12.dp.toPx() * p, chromaticAberration = true)
                },
                highlight = {
                    val p = press.value
                    Highlight.Ambient.copy(
                        width = Highlight.Ambient.width / 1.5f,
                        blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                        alpha = p
                    )
                },
                shadow = { Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.16f)) },
                innerShadow = {
                    val p = press.value
                    InnerShadow(radius = 4.dp * p, alpha = p)
                },
                layerBlock = {
                    val s = lerp(1f, 1.35f, press.value)
                    scaleX = s
                    scaleY = s
                },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 1f - press.value)) }
            )
            .size(ThumbWidth, ThumbHeight)
    )
}
