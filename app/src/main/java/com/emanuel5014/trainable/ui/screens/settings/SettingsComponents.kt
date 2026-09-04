package com.emanuel5014.trainable.ui.screens.settings

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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.fromHSV
import com.emanuel5014.trainable.ui.theme.getSeedPreviewColors
import com.emanuel5014.trainable.ui.theme.toHSV

@Composable
internal fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = Primary,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
internal fun LanguageOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurface
        )
        if (isSelected) {
            androidx.compose.material3.Icon(Icons.Rounded.Check, contentDescription = null, tint = Primary)
        }
    }
}

@Composable
internal fun SettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = if (checked) {
            {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                    tint = Primary
                )
            }
        } else {
            null
        }
    )
}

@Composable
internal fun PalettePreviewCircle(
    colors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (isSelected) Primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() }
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
internal fun CustomColorPickerDialog(
    initialColor: Color?,
    initialStyle: Int,
    onDismiss: () -> Unit,
    onApply: (colorSeed: Int, style: Int) -> Unit
) {
    val defaultHue = initialColor?.toHSV()?.get(0) ?: 220f
    var hue by remember { mutableFloatStateOf(defaultHue) }
    var selectedStyle by remember { mutableIntStateOf(initialStyle) }

    val currentColor = remember(hue) { Color.fromHSV(hue, 0.8f, 0.9f) }
    val seedArgb = remember(currentColor) { currentColor.toArgb() }

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
                    Text(stringResource(R.string.style), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        (0..4).forEach { styleIndex ->
                            PalettePreviewCircle(
                                colors = getSeedPreviewColors(seedArgb, styleIndex),
                                isSelected = selectedStyle == styleIndex,
                                onClick = { selectedStyle = styleIndex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(seedArgb, selectedStyle) }) {
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

@Composable
internal fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    currentColor: Color
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.hue), style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant)
            Text("${hue.toInt()}°", style = MaterialTheme.typography.labelLarge, color = OnSurface)
        }

        val rainbowColors = remember {
            listOf(
                Color(0xFFFF0000),
                Color(0xFFFF8800),
                Color(0xFFFFFF00),
                Color(0xFF00FF00),
                Color(0xFF00CBFF),
                Color(0xFF0055FF),
                Color(0xFF8800FF),
                Color(0xFFFF00FF),
                Color(0xFFFF0000),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = rainbowColors,
                            startX = 0f,
                            endX = Float.POSITIVE_INFINITY
                        )
                    )
                    .align(Alignment.Center)
            )

            Slider(
                value = hue,
                onValueChange = onHueChange,
                valueRange = 0f..360f,
                modifier = Modifier.fillMaxSize(),
                colors = SliderDefaults.colors(
                    thumbColor = currentColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                )
            )
        }
    }
}
