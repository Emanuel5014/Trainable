package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.screens.settings.CustomColorPickerDialog
import com.emanuel5014.trainable.ui.screens.settings.LocalPaletteColors
import com.emanuel5014.trainable.ui.screens.settings.PalettePreviewCircle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val paletteNames = listOf(
    Res.string.palette_default, Res.string.palette_blue, Res.string.palette_green, Res.string.palette_red,
    Res.string.palette_purple, Res.string.palette_orange, Res.string.palette_pink, Res.string.palette_teal,
    Res.string.palette_monochrome
)
private val styleNames = listOf(
    Res.string.style_tonal_spot, Res.string.style_vibrant, Res.string.style_expressive, Res.string.style_neutral,
    Res.string.style_fruit_salad
)

/** "Il tuo stile": colour, theme mode, navigation bar and Liquid Glass, all written at once with a live preview. */
@Composable
fun LookStep(model: OnboardingModel, wallpaperSupported: Boolean, hero: @Composable () -> Unit) {
    val dynamicColor by model.dynamicColor.collectAsState(initial = true)
    val dynamicColorSeed by model.dynamicColorSeed.collectAsState(initial = null)
    val themePalette by model.themePalette.collectAsState(initial = 0)
    val themeStyle by model.themeStyle.collectAsState(initial = 0)
    val themeMode by model.themeMode.collectAsState(initial = 0)
    val navBarStyle by model.navBarStyle.collectAsState(initial = NavBarStyle.Floating)
    val glass by model.liquidGlassEnabled.collectAsState(initial = false)
    val paletteColors = LocalPaletteColors.current
    var showCustomColor by remember { mutableStateOf(false) }

    OnboardingPage(underTopRow = true) {
        hero()
        OnboardingTitle(stringResource(Res.string.onboarding_look_title))
        LookPreview(navBarStyle)

        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (wallpaperSupported) {
                SwitchRow(
                    position = JoinedPosition.Single,
                    title = stringResource(Res.string.dynamic_color),
                    description = stringResource(Res.string.dynamic_color_desc),
                    checked = dynamicColor,
                    onCheckedChange = { model.setDynamicColor(it) }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionLabel(stringResource(Res.string.app_palette))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (wallpaperSupported && dynamicColor) {
                        val seed = dynamicColorSeed ?: Color(0xFF1976D2).toArgb()
                        styleNames.forEachIndexed { index, name ->
                            ColourChoice(name, paletteColors.seed(seed, index), themeStyle == index) {
                                model.setDynamicColorSeed(null)
                                model.setThemeStyle(index)
                            }
                        }
                    } else {
                        paletteNames.forEachIndexed { index, name ->
                            ColourChoice(name, paletteColors.palette(index), themePalette == index) {
                                model.setThemePalette(index)
                            }
                        }
                    }
                }
            }

            JoinedRowSurface(JoinedPosition.Single) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { showCustomColor = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.custom_color), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (dynamicColorSeed != null) stringResource(styleNames[themeStyle.coerceIn(0, 4)]) else stringResource(Res.string.pick_theme_color),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (dynamicColorSeed != null) {
                        TextButton(onClick = { model.setDynamicColorSeed(null); model.setThemeStyle(0) }) {
                            Text(stringResource(Res.string.reset))
                        }
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionLabel(stringResource(Res.string.theme_mode))
                val light = stringResource(Res.string.theme_mode_light)
                val dark = stringResource(Res.string.theme_mode_dark)
                val system = stringResource(Res.string.theme_mode_system)
                // Mode ids as stored: 0 system, 1 light, 2 dark
                OptionGroup(listOf(1, 2, 0), themeMode, { when (it) { 1 -> light; 2 -> dark; else -> system } }, { model.setThemeMode(it) })
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionLabel(stringResource(Res.string.nav_bar_style))
                val names = mapOf(
                    NavBarStyle.Classic to stringResource(Res.string.nav_bar_style_classic),
                    NavBarStyle.Floating to stringResource(Res.string.nav_bar_style_floating),
                    NavBarStyle.Expressive to stringResource(Res.string.nav_bar_style_expressive)
                )
                OptionGroup(names.keys.toList(), navBarStyle, { names.getValue(it) }, { model.setNavBarStyle(it) })
            }

            SwitchRow(
                position = JoinedPosition.Single,
                title = stringResource(Res.string.liquid_glass),
                description = stringResource(Res.string.liquid_glass_desc),
                checked = glass,
                onCheckedChange = { model.setLiquidGlassEnabled(it) }
            )
        }
    }

    if (showCustomColor) {
        CustomColorPickerDialog(
            initialColor = dynamicColorSeed?.let { Color(it) },
            initialStyle = themeStyle,
            onDismiss = { showCustomColor = false },
            onApply = { seed, style ->
                model.setDynamicColorSeed(seed)
                model.setThemeStyle(style)
                showCustomColor = false
            }
        )
    }
}

@Composable
private fun ColourChoice(name: StringResource, colors: List<Color>, selected: Boolean, onClick: () -> Unit) {
    val label = stringResource(name)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PalettePreviewCircle(colors = colors, isSelected = selected, onClick = onClick, label = label)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** A tiny dashboard drawn with the live theme: two cards and the chosen navigation bar. */
@Composable
private fun LookPreview(style: NavBarStyle) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(scheme.surfaceContainerLow)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(16.dp)).background(scheme.primaryContainer))
            Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(16.dp)).background(scheme.secondaryContainer))
        }
        Box(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(14.dp)).background(scheme.surfaceContainerHigh))
        Spacer(Modifier.height(4.dp))
        val dots: @Composable () -> Unit = {
            repeat(4) { i ->
                Box(Modifier.size(if (i == 0) 18.dp else 12.dp).clip(CircleShape).background(if (i == 0) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.5f)))
            }
        }
        when (style) {
            NavBarStyle.Classic -> Row(
                modifier = Modifier.fillMaxWidth().height(34.dp).background(scheme.surfaceContainer),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) { dots() }
            NavBarStyle.Floating -> Row(
                modifier = Modifier.padding(horizontal = 28.dp).fillMaxWidth().height(34.dp).clip(CircleShape).background(scheme.surfaceContainerHighest),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) { dots() }
            else -> Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f).height(34.dp).clip(CircleShape).background(scheme.surfaceContainerHighest),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) { dots() }
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(scheme.primaryContainer))
            }
        }
        Spacer(Modifier.width(1.dp))
    }
}
