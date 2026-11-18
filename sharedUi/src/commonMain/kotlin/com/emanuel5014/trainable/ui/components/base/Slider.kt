package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.ui.glass.LocalPlatformGlass
import com.emanuel5014.trainable.ui.theme.LocalGlassSettings

/**
 * The Material 3 slider, with its thumb in Liquid Glass when the user has the sliders part on and the platform draws
 * it (PlatformGlass). Same parameters as the Material slider, so it replaces it by import.
 *
 * A slider whose track is clear (colour pickers draw their own gradient under it) keeps the plain thumb.
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
    val platformGlass = LocalPlatformGlass.current
    val glass = LocalGlassSettings.current.isOn(GlassPart.Sliders) && colors.activeTrackColor != Color.Transparent &&
        platformGlass.hasGlassSlider
    if (glass) {
        platformGlass.GlassSlider(
            value, onValueChange, modifier, enabled, valueRange, steps, onValueChangeFinished, colors, interactionSource
        )
        return
    }
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
}
