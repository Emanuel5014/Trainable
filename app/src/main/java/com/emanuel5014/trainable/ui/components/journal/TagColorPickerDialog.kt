package com.emanuel5014.trainable.ui.components.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.emanuel5014.trainable.ui.components.glass.GlassAlertDialog as AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import com.emanuel5014.trainable.ui.components.glass.GlassTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.screens.settings.HueSlider
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.fromHSV
import com.emanuel5014.trainable.ui.theme.toHSV

/** How far saturation and brightness can go: a tag name is written in its colour, so it must not fade into the surface. */
private const val MIN_SATURATION = 0.25f
private const val MIN_BRIGHTNESS = 0.35f

/**
 * A colour for a tag, picked freely with hue, saturation and brightness sliders. [initialColor] is where the sliders
 * start (the colour the tag has now); [onApply] gets the opaque ARGB of the chosen colour.
 */
@Composable
fun TagColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onApply: (argb: Int) -> Unit
) {
    val initial = remember(initialColor) { initialColor.toHSV() }
    var hue by remember { mutableFloatStateOf(initial[0]) }
    var saturation by remember { mutableFloatStateOf(initial[1].coerceIn(MIN_SATURATION, 1f)) }
    var brightness by remember { mutableFloatStateOf(initial[2].coerceIn(MIN_BRIGHTNESS, 1f)) }

    val color = Color.fromHSV(hue, saturation, brightness)
    val argb = color.toArgb()
    val onColor = if (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f > 0.5f) Color.Black else Color.White

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = {
            Text(stringResource(R.string.custom_color), fontWeight = FontWeight.ExtraBold, color = OnSurface)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#%06X".format(argb and 0xFFFFFF),
                        color = onColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                HueSlider(hue = hue, onHueChange = { hue = it }, currentColor = color)

                ColorChannelSlider(
                    label = stringResource(R.string.saturation),
                    value = saturation,
                    valueRange = MIN_SATURATION..1f,
                    onValueChange = { saturation = it },
                    trackStart = Color.fromHSV(hue, MIN_SATURATION, brightness),
                    trackEnd = Color.fromHSV(hue, 1f, brightness),
                    thumbColor = color
                )

                ColorChannelSlider(
                    label = stringResource(R.string.brightness),
                    value = brightness,
                    valueRange = MIN_BRIGHTNESS..1f,
                    onValueChange = { brightness = it },
                    trackStart = Color.fromHSV(hue, saturation, MIN_BRIGHTNESS),
                    trackEnd = Color.fromHSV(hue, saturation, 1f),
                    thumbColor = color
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(argb) }) {
                Text(stringResource(R.string.apply).uppercase(), color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}

/** A slider laid over a gradient track from [trackStart] to [trackEnd], drawn like the hue slider. */
@Composable
private fun ColorChannelSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    trackStart: Color,
    trackEnd: Color,
    thumbColor: Color
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = OnSurface)
        }
        Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(trackStart, trackEnd)))
                    .align(Alignment.Center)
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxSize(),
                colors = SliderDefaults.colors(
                    thumbColor = thumbColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}
