package com.emanuel5014.trainable.ui.screens.settings

import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.style_tonal_spot
import com.emanuel5014.trainable.sharedui.resources.style_vibrant
import com.emanuel5014.trainable.sharedui.resources.style_expressive
import com.emanuel5014.trainable.sharedui.resources.style_neutral
import com.emanuel5014.trainable.sharedui.resources.style_fruit_salad
import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.sharedui.resources.apply
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.custom_color
import com.emanuel5014.trainable.sharedui.resources.style
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import com.emanuel5014.trainable.ui.components.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.fromHSV
import com.emanuel5014.trainable.ui.theme.toHSV

@Composable
fun PalettePreviewCircle(
    colors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit,
    label: String? = null
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (isSelected) Primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() }
            .semantics {
                if (label != null) contentDescription = label
                selected = isSelected
            }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (colors.size == 1) {
                    drawCircle(color = colors[0])
                } else {
                    val primary = colors.getOrElse(0) { Color.Gray }
                    val secondary = colors.getOrElse(1) { primary.copy(alpha = 0.7f) }
                    val tertiary = colors.getOrElse(2) { primary.copy(alpha = 0.5f) }
                    val neutral = colors.getOrElse(3) { primary.copy(alpha = 0.3f) }

                    drawArc(
                        color = primary,
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    drawArc(
                        color = secondary,
                        startAngle = 270f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    drawArc(
                        color = tertiary,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                    drawArc(
                        color = neutral,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }
            }
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun CustomColorPickerDialog(
    initialColor: Color?,
    initialStyle: Int,
    onDismiss: () -> Unit,
    onApply: (colorSeed: Int, style: Int) -> Unit
) {
    val paletteColors = LocalPaletteColors.current
    val defaultHue = initialColor?.toHSV()?.get(0) ?: 220f
    var hue by remember { mutableFloatStateOf(defaultHue) }
    var selectedStyle by remember { mutableIntStateOf(initialStyle) }

    val currentColor = remember(hue) { Color.fromHSV(hue, 0.8f, 0.9f) }
    val seedArgb = remember(currentColor) { currentColor.toArgb() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = {
            Text(stringResource(Res.string.custom_color), fontWeight = FontWeight.ExtraBold, color = OnSurface)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(currentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${seedArgb.toString(16).padStart(8, '0').substring(2).uppercase()}",
                        color = if (currentColor.let { c -> c.red * 0.299f + c.green * 0.587f + c.blue * 0.114f > 0.5f }) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                HueSlider(
                    hue = hue,
                    onHueChange = { hue = it },
                    currentColor = currentColor
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.style), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val styleNames = listOf(
                            stringResource(Res.string.style_tonal_spot), stringResource(Res.string.style_vibrant),
                            stringResource(Res.string.style_expressive), stringResource(Res.string.style_neutral),
                            stringResource(Res.string.style_fruit_salad)
                        )
                        (0..4).forEach { styleIndex ->
                            PalettePreviewCircle(
                                colors = paletteColors.seed(seedArgb, styleIndex),
                                isSelected = selectedStyle == styleIndex,
                                onClick = { selectedStyle = styleIndex },
                                label = styleNames[styleIndex]
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(seedArgb, selectedStyle) }) {
                Text(stringResource(Res.string.apply).uppercase(), color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}
