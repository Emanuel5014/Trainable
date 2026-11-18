package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import com.emanuel5014.trainable.data.model.ColorSource
import com.emanuel5014.trainable.data.model.SavedTheme
import com.emanuel5014.trainable.data.model.ThemeSettings
import com.emanuel5014.trainable.data.model.ThemeSource
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.brightness
import com.emanuel5014.trainable.sharedui.resources.hue
import com.emanuel5014.trainable.sharedui.resources.palette_blue
import com.emanuel5014.trainable.sharedui.resources.palette_default
import com.emanuel5014.trainable.sharedui.resources.palette_green
import com.emanuel5014.trainable.sharedui.resources.palette_monochrome
import com.emanuel5014.trainable.sharedui.resources.palette_orange
import com.emanuel5014.trainable.sharedui.resources.palette_pink
import com.emanuel5014.trainable.sharedui.resources.palette_purple
import com.emanuel5014.trainable.sharedui.resources.palette_red
import com.emanuel5014.trainable.sharedui.resources.palette_teal
import com.emanuel5014.trainable.sharedui.resources.saturation
import com.emanuel5014.trainable.sharedui.resources.style_content
import com.emanuel5014.trainable.sharedui.resources.style_expressive
import com.emanuel5014.trainable.sharedui.resources.style_fidelity
import com.emanuel5014.trainable.sharedui.resources.style_fruit_salad
import com.emanuel5014.trainable.sharedui.resources.style_monochrome
import com.emanuel5014.trainable.sharedui.resources.style_neutral
import com.emanuel5014.trainable.sharedui.resources.style_rainbow
import com.emanuel5014.trainable.sharedui.resources.style_tonal_spot
import com.emanuel5014.trainable.sharedui.resources.style_vibrant
import com.emanuel5014.trainable.sharedui.resources.theme_accents
import com.emanuel5014.trainable.sharedui.resources.theme_accents_auto
import com.emanuel5014.trainable.sharedui.resources.theme_accents_manual
import com.emanuel5014.trainable.sharedui.resources.theme_advanced
import com.emanuel5014.trainable.sharedui.resources.theme_advanced_desc
import com.emanuel5014.trainable.sharedui.resources.theme_contrast_desc
import com.emanuel5014.trainable.sharedui.resources.theme_contrast_high
import com.emanuel5014.trainable.sharedui.resources.theme_contrast_medium
import com.emanuel5014.trainable.sharedui.resources.theme_contrast_standard
import com.emanuel5014.trainable.sharedui.resources.theme_hex
import com.emanuel5014.trainable.sharedui.resources.theme_mode_dark
import com.emanuel5014.trainable.sharedui.resources.theme_mode_light
import com.emanuel5014.trainable.sharedui.resources.theme_photo_colors
import com.emanuel5014.trainable.sharedui.resources.theme_photo_desc
import com.emanuel5014.trainable.sharedui.resources.theme_photo_none
import com.emanuel5014.trainable.sharedui.resources.theme_photo_pick
import com.emanuel5014.trainable.sharedui.resources.theme_pure_black
import com.emanuel5014.trainable.sharedui.resources.theme_pure_black_desc
import com.emanuel5014.trainable.sharedui.resources.theme_save
import com.emanuel5014.trainable.sharedui.resources.theme_saved
import com.emanuel5014.trainable.sharedui.resources.theme_saved_name
import com.emanuel5014.trainable.sharedui.resources.theme_saved_remove
import com.emanuel5014.trainable.sharedui.resources.theme_secondary
import com.emanuel5014.trainable.sharedui.resources.theme_section_color
import com.emanuel5014.trainable.sharedui.resources.theme_section_contrast
import com.emanuel5014.trainable.sharedui.resources.theme_section_mode
import com.emanuel5014.trainable.sharedui.resources.theme_section_style
import com.emanuel5014.trainable.sharedui.resources.theme_source_custom
import com.emanuel5014.trainable.sharedui.resources.theme_source_photo
import com.emanuel5014.trainable.sharedui.resources.theme_source_preset
import com.emanuel5014.trainable.sharedui.resources.theme_source_system
import com.emanuel5014.trainable.sharedui.resources.theme_style_fixed
import com.emanuel5014.trainable.sharedui.resources.theme_style_hint
import com.emanuel5014.trainable.sharedui.resources.theme_system_auto
import com.emanuel5014.trainable.sharedui.resources.theme_system_desc
import com.emanuel5014.trainable.sharedui.resources.theme_tertiary
import com.emanuel5014.trainable.sharedui.resources.theme_tint
import com.emanuel5014.trainable.sharedui.resources.theme_tint_auto
import com.emanuel5014.trainable.sharedui.resources.theme_tint_colorful
import com.emanuel5014.trainable.sharedui.resources.theme_tint_neutral
import com.emanuel5014.trainable.sharedui.resources.reset
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.Slider
import com.emanuel5014.trainable.ui.theme.AppColorSchemes
import com.emanuel5014.trainable.ui.theme.MonolithicPrimary
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.PhotoThemeColors
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.fromHSV
import com.emanuel5014.trainable.ui.theme.rememberThemePhotoPicker
import com.emanuel5014.trainable.ui.theme.toHSV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val RAINBOW = listOf(
    Color(0xFFFF0000), Color(0xFFFF8800), Color(0xFFFFFF00), Color(0xFF00FF00), Color(0xFF00CBFF),
    Color(0xFF0055FF), Color(0xFF8800FF), Color(0xFFFF00FF), Color(0xFFFF0000)
)

private val STYLE_NAMES = listOf(
    Res.string.style_tonal_spot, Res.string.style_vibrant, Res.string.style_expressive, Res.string.style_neutral,
    Res.string.style_fruit_salad, Res.string.style_content, Res.string.style_fidelity, Res.string.style_rainbow,
    Res.string.style_monochrome
)

internal val PALETTE_NAMES = listOf(
    Res.string.palette_default, Res.string.palette_blue, Res.string.palette_green, Res.string.palette_red,
    Res.string.palette_purple, Res.string.palette_orange, Res.string.palette_pink, Res.string.palette_teal,
    Res.string.palette_monochrome
)

/** Background tint the slider shows while the style's own one is in use (Tonal Spot's is 4). */
private const val DEFAULT_TINT = 4

/**
 * The Themes section: mode and pure black, where the colour comes from (wallpaper on Android 12+, a photo, a preset
 * or a colour of one's own, with accents, background tint and saved themes), the style and the contrast. Every
 * change is written at once and the whole app, this screen included, repaints with it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsSection(
    stored: ThemeSettings,
    themeMode: Int,
    wallpaperColors: List<Int>,
    systemAvailable: Boolean,
    onThemeModeChange: (Int) -> Unit,
    onThemeChange: (ThemeSettings) -> Unit
) {
    // A theme from before this section is shown (and first written) in its terms
    val theme = remember(stored, systemAvailable, wallpaperColors) { stored.migrated(systemAvailable, wallpaperColors) }
    val source = theme.source ?: ThemeSource.Preset
    val customStart = customColorStart()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(Res.string.theme_section_mode)
        GymCard(containerColor = SurfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PillSegments(
                    labels = listOf(Res.string.theme_source_system, Res.string.theme_mode_light, Res.string.theme_mode_dark),
                    selected = themeMode.coerceIn(0, 2),
                    onSelect = onThemeModeChange
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.theme_pure_black), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                        Text(stringResource(Res.string.theme_pure_black_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                    Box(Modifier.width(16.dp))
                    SettingsSwitch(
                        checked = theme.pureBlack,
                        onCheckedChange = { onThemeChange(theme.copy(pureBlack = it)) },
                        enabled = themeMode != 1
                    )
                }
            }
        }

        SectionLabel(Res.string.theme_section_color)
        GymCard(containerColor = SurfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val sources = themeSources(systemAvailable)
                PillSegments(
                    labels = sources.map { it.label() },
                    selected = sources.indexOf(source),
                    onSelect = { index -> onThemeChange(theme.withSource(sources[index], customStart)) }
                )
                ThemeSourceContent(theme, wallpaperColors, customStart, onThemeChange)
            }
        }

        SectionLabel(Res.string.theme_section_style)
        GymCard(containerColor = SurfaceContainerHigh) {
            ThemeStyleGrid(theme, systemAvailable, wallpaperColors, onThemeChange)
        }

        SectionLabel(Res.string.theme_section_contrast)
        GymCard(containerColor = SurfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PillSegments(
                    labels = listOf(Res.string.theme_contrast_standard, Res.string.theme_contrast_medium, Res.string.theme_contrast_high),
                    selected = theme.contrast,
                    onSelect = { onThemeChange(theme.copy(contrast = it)) }
                )
                Text(stringResource(Res.string.theme_contrast_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
    }
}

/** The sources this platform offers: the wallpaper only where the system shares it (Android 12+). */
internal fun themeSources(systemAvailable: Boolean): List<ThemeSource> = buildList {
    if (systemAvailable) add(ThemeSource.System)
    add(ThemeSource.Photo); add(ThemeSource.Preset); add(ThemeSource.Custom)
}

/** Switch source; a custom colour starts from [customStart] the first time, so nothing jumps. */
internal fun ThemeSettings.withSource(picked: ThemeSource, customStart: Int): ThemeSettings =
    copy(source = picked, customSeed = customSeed ?: customStart)

/** Where a custom colour starts: the colour on screen, or the brand blue when the screen is grey. */
@Composable
internal fun customColorStart(): Int {
    val currentPrimary = MaterialTheme.colorScheme.primary.toArgb()
    return if (Color(currentPrimary).toHSV()[1] < 0.15f) MonolithicPrimary.toArgb() else currentPrimary
}

/**
 * What a source offers: wallpaper colours, the photo button and its colours, the presets, or the custom colour.
 * [advanced] adds accents, background tint and saved themes to the custom colour (the setup leaves them out).
 */
@Composable
internal fun ThemeSourceContent(
    theme: ThemeSettings,
    wallpaperColors: List<Int>,
    customStart: Int,
    onThemeChange: (ThemeSettings) -> Unit,
    advanced: Boolean = true
) {
    when (theme.source ?: ThemeSource.Preset) {
        ThemeSource.System -> SystemColors(theme, wallpaperColors, onThemeChange)
        ThemeSource.Photo -> PhotoColors(theme, onThemeChange)
        ThemeSource.Preset -> PresetColors(theme, onThemeChange)
        ThemeSource.Custom -> CustomColor(theme, customStart, onThemeChange, advanced)
    }
}

/** The nine styles on the colour in use, greyed out for the fixed palettes, with a line about them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemeStyleGrid(
    theme: ThemeSettings,
    systemAvailable: Boolean,
    wallpaperColors: List<Int>,
    onThemeChange: (ThemeSettings) -> Unit
) {
    val currentPrimary = MaterialTheme.colorScheme.primary.toArgb()
    val styleSeed = when (val resolved = theme.colorSource(systemAvailable)) {
        is ColorSource.Seed -> resolved.seed
        is ColorSource.SystemDynamic -> theme.dynamicColorSeed ?: wallpaperColors.firstOrNull() ?: currentPrimary
        else -> null
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            STYLE_NAMES.forEachIndexed { index, name ->
                val label = stringResource(name)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(72.dp).alpha(if (styleSeed == null) 0.4f else 1f)
                ) {
                    PalettePreviewCircle(
                        colors = AppColorSchemes.previews(styleSeed ?: currentPrimary, index),
                        isSelected = styleSeed != null && theme.style == index,
                        onClick = { if (styleSeed != null) onThemeChange(theme.copy(style = index)) },
                        label = label
                    )
                    Text(
                        label, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = if (styleSeed != null && theme.style == index) Primary else OnSurfaceVariant
                    )
                }
            }
        }
        Text(
            stringResource(if (styleSeed == null) Res.string.theme_style_fixed else Res.string.theme_style_hint),
            style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
        )
    }
}

internal fun ThemeSource.label(): StringResource = when (this) {
    ThemeSource.System -> Res.string.theme_source_system
    ThemeSource.Photo -> Res.string.theme_source_photo
    ThemeSource.Preset -> Res.string.theme_source_preset
    ThemeSource.Custom -> Res.string.theme_source_custom
}

@Composable
private fun SectionLabel(text: StringResource) {
    Text(
        stringResource(text).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black,
        color = OnSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    )
}

/** Options in one pill; the chosen one is filled with the theme colour. */
@Composable
private fun PillSegments(labels: List<StringResource>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(CircleShape).background(SurfaceContainerHighest).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(CircleShape)
                    .background(if (on) Primary else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .semantics { this.selected = on }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                val style = if (labels.size > 3) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge
                // Long words ("Personalizado") shrink a little instead of being cut
                Text(
                    stringResource(label), style = style, fontWeight = FontWeight.ExtraBold,
                    color = if (on) OnPrimary else OnSurfaceVariant, maxLines = 1, softWrap = false,
                    textAlign = TextAlign.Center,
                    autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp)
                )
            }
        }
    }
}

@Composable
private fun Hint(text: StringResource) {
    Text(stringResource(text), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
}

@Composable
private fun SystemColors(theme: ThemeSettings, wallpaperColors: List<Int>, onThemeChange: (ThemeSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hint(Res.string.theme_system_desc)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val auto = stringResource(Res.string.theme_system_auto)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val on = theme.dynamicColorSeed == null
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (on) Primary.copy(alpha = 0.2f) else SurfaceContainerHighest)
                        .clickable { onThemeChange(theme.copy(dynamicColorSeed = null)) }
                        .semantics { contentDescription = auto; selected = on },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = if (on) Primary else OnSurfaceVariant)
                }
                Text(auto, style = MaterialTheme.typography.labelSmall, color = if (on) Primary else OnSurfaceVariant, maxLines = 1)
            }
            wallpaperColors.forEach { color ->
                PalettePreviewCircle(
                    colors = AppColorSchemes.previews(color, theme.style),
                    isSelected = theme.dynamicColorSeed == color,
                    onClick = { onThemeChange(theme.copy(dynamicColorSeed = color)) }
                )
            }
        }
    }
}

@Composable
private fun PhotoColors(theme: ThemeSettings, onThemeChange: (ThemeSettings) -> Unit) {
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(theme)
    val pick = rememberThemePhotoPicker { bitmap ->
        scope.launch {
            val colors = withContext(Dispatchers.Default) { PhotoThemeColors.extract(bitmap) }
            if (colors.isNotEmpty()) {
                onThemeChange(current.copy(source = ThemeSource.Photo, photoColors = colors, photoSeed = colors.first()))
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Hint(Res.string.theme_photo_desc)
        Button(
            onClick = pick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary)
        ) {
            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
            Box(Modifier.width(8.dp))
            Text(stringResource(Res.string.theme_photo_pick), fontWeight = FontWeight.ExtraBold)
        }
        if (theme.photoColors.isEmpty()) {
            Hint(Res.string.theme_photo_none)
        } else {
            SectionLabel(Res.string.theme_photo_colors)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                theme.photoColors.forEach { color ->
                    PalettePreviewCircle(
                        colors = AppColorSchemes.previews(color, theme.style),
                        isSelected = theme.photoSeed == color,
                        onClick = { onThemeChange(theme.copy(photoSeed = color)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetColors(theme: ThemeSettings, onThemeChange: (ThemeSettings) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PALETTE_NAMES.forEachIndexed { index, name ->
            val label = stringResource(name)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                PalettePreviewCircle(
                    colors = AppColorSchemes.palettePreview(index),
                    isSelected = theme.palette == index,
                    onClick = { onThemeChange(theme.copy(palette = index)) },
                    label = label
                )
                Text(
                    label, style = MaterialTheme.typography.labelSmall.copy(hyphens = Hyphens.Auto), maxLines = 2,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    color = if (theme.palette == index) Primary else OnSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomColor(theme: ThemeSettings, currentPrimary: Int, onThemeChange: (ThemeSettings) -> Unit, advanced: Boolean) {
    val current by rememberUpdatedState(theme)
    val seed = theme.customSeed ?: currentPrimary
    // The sliders move a local colour; the theme is written when a finger lifts, not at every frame
    var hue by remember { mutableFloatStateOf(Color(seed).toHSV()[0]) }
    var saturation by remember { mutableFloatStateOf(Color(seed).toHSV()[1]) }
    var brightness by remember { mutableFloatStateOf(Color(seed).toHSV()[2].coerceAtLeast(0.15f)) }
    val color = Color.fromHSV(hue, saturation, brightness)
    LaunchedEffect(seed) {
        if (color.toArgb() != seed) {
            val hsv = Color(seed).toHSV()
            hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2].coerceAtLeast(0.15f)
        }
    }
    fun commit() { onThemeChange(current.copy(customSeed = Color.fromHSV(hue, saturation, brightness).toArgb())) }

    var advancedOpen by remember { mutableStateOf(theme.secondaryHue != null || theme.neutralTint != null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(color))
            HexField(color.toArgb(), onHex = { argb -> onThemeChange(current.copy(customSeed = argb)) }, modifier = Modifier.weight(1f))
        }
        GradientSlider(
            label = stringResource(Res.string.hue), valueText = "${hue.toInt()}°", value = hue, range = 0f..360f,
            track = Brush.horizontalGradient(RAINBOW), thumb = color,
            onChange = { hue = it }, onFinished = ::commit
        )
        GradientSlider(
            label = stringResource(Res.string.saturation), valueText = "${(saturation * 100).toInt()}%", value = saturation, range = 0f..1f,
            track = Brush.horizontalGradient(listOf(Color.fromHSV(hue, 0f, brightness), Color.fromHSV(hue, 1f, brightness))), thumb = color,
            onChange = { saturation = it }, onFinished = ::commit
        )
        GradientSlider(
            label = stringResource(Res.string.brightness), valueText = "${(brightness * 100).toInt()}%", value = brightness, range = 0.15f..1f,
            track = Brush.horizontalGradient(listOf(Color(0xFF111111), Color.fromHSV(hue, saturation, 1f))), thumb = color,
            onChange = { brightness = it }, onFinished = ::commit
        )

        // The setup keeps to the colour itself; accents, tint and saved themes live in Settings
        if (advanced) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerHighest)
                    .clickable { advancedOpen = !advancedOpen }
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.theme_advanced), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = OnSurface)
                    Text(stringResource(Res.string.theme_advanced_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.rotate(if (advancedOpen) 180f else 0f))
            }
            AnimatedVisibility(advancedOpen) {
                AdvancedColors(theme, onThemeChange)
            }

            SavedThemes(theme, seed, onThemeChange)
        }
    }
}

@Composable
private fun AdvancedColors(theme: ThemeSettings, onThemeChange: (ThemeSettings) -> Unit) {
    val current by rememberUpdatedState(theme)
    val manual = theme.secondaryHue != null || theme.tertiaryHue != null
    val primaryHue = Color(theme.customSeed ?: 0).toHSV()[0].toInt()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(stringResource(Res.string.theme_accents), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = OnSurface)
        PillSegments(
            labels = listOf(Res.string.theme_accents_auto, Res.string.theme_accents_manual),
            selected = if (manual) 1 else 0,
            onSelect = { index ->
                onThemeChange(
                    if (index == 0) theme.copy(secondaryHue = null, tertiaryHue = null)
                    else theme.copy(secondaryHue = (primaryHue + 60) % 360, tertiaryHue = (primaryHue + 150) % 360)
                )
            }
        )
        if (manual) {
            var secondary by remember { mutableFloatStateOf((theme.secondaryHue ?: 0).toFloat()) }
            var tertiary by remember { mutableFloatStateOf((theme.tertiaryHue ?: 0).toFloat()) }
            GradientSlider(
                label = stringResource(Res.string.theme_secondary), valueText = "${secondary.toInt()}°", value = secondary, range = 0f..360f,
                track = Brush.horizontalGradient(RAINBOW), thumb = MaterialTheme.colorScheme.secondary,
                onChange = { secondary = it }, onFinished = { onThemeChange(current.copy(secondaryHue = secondary.toInt() % 360)) }
            )
            GradientSlider(
                label = stringResource(Res.string.theme_tertiary), valueText = "${tertiary.toInt()}°", value = tertiary, range = 0f..360f,
                track = Brush.horizontalGradient(RAINBOW), thumb = MaterialTheme.colorScheme.tertiary,
                onChange = { tertiary = it }, onFinished = { onThemeChange(current.copy(tertiaryHue = tertiary.toInt() % 360)) }
            )
        }

        var tint by remember(theme.neutralTint) { mutableFloatStateOf((theme.neutralTint ?: DEFAULT_TINT).toFloat()) }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.theme_tint), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = OnSurface, modifier = Modifier.weight(1f))
                if (theme.neutralTint == null) {
                    Text(stringResource(Res.string.theme_tint_auto), style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant)
                } else {
                    TextButton(onClick = { onThemeChange(theme.copy(neutralTint = null)) }) {
                        Text(stringResource(Res.string.reset), color = Primary)
                    }
                }
            }
            val hue = Color(theme.customSeed ?: 0).toHSV()[0]
            GradientSlider(
                label = null, valueText = null, value = tint, range = 0f..24f,
                track = Brush.horizontalGradient(listOf(Color.fromHSV(hue, 0f, 0.35f), Color.fromHSV(hue, 0.6f, 0.45f))),
                thumb = MaterialTheme.colorScheme.surfaceContainerHighest,
                onChange = { tint = it }, onFinished = { onThemeChange(current.copy(neutralTint = tint.toInt())) }
            )
            Row(Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.theme_tint_neutral), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, modifier = Modifier.weight(1f))
                Text(stringResource(Res.string.theme_tint_colorful), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavedThemes(theme: ThemeSettings, seed: Int, onThemeChange: (ThemeSettings) -> Unit) {
    val nextName = stringResource(Res.string.theme_saved_name, theme.saved.size + 1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(Res.string.theme_saved)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            theme.saved.forEachIndexed { index, saved ->
                val remove = stringResource(Res.string.theme_saved_remove, saved.name)
                InputChip(
                    selected = false,
                    onClick = { onThemeChange(theme.applying(saved)) },
                    label = { Text(saved.name, fontWeight = FontWeight.Bold) },
                    avatar = { Box(Modifier.size(InputChipDefaults.AvatarSize).clip(CircleShape).background(Color(saved.seed))) },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Close, contentDescription = remove,
                            modifier = Modifier.size(InputChipDefaults.IconSize).clickable {
                                onThemeChange(theme.copy(saved = theme.saved.filterIndexed { i, _ -> i != index }))
                            }
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(containerColor = SurfaceContainerHighest, labelColor = OnSurface)
                )
            }
            AssistChip(
                onClick = {
                    val saved = SavedTheme(nextName, seed, theme.style, theme.secondaryHue, theme.tertiaryHue, theme.neutralTint)
                    onThemeChange(theme.copy(saved = theme.saved + saved))
                },
                label = { Text(stringResource(Res.string.theme_save), fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                border = null
            )
        }
    }
}

/** A hex code to type; it applies as soon as it is a whole colour. */
@Composable
private fun HexField(argb: Int, onHex: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shown = "#" + (argb and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
    var text by remember(shown) { mutableStateOf(shown) }
    OutlinedTextField(
        value = text,
        onValueChange = { typed ->
            val clean = typed.uppercase().filter { it.isLetterOrDigit() || it == '#' }.take(7)
            text = if (clean.startsWith("#")) clean else "#$clean"
            parseHex(text)?.let(onHex)
        },
        label = { Text(stringResource(Res.string.theme_hex)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { text = shown }),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = OnSurfaceVariant.copy(alpha = 0.4f)),
        modifier = modifier
    )
}

private fun parseHex(text: String): Int? {
    val digits = text.removePrefix("#")
    if (digits.length != 6) return null
    return digits.toIntOrNull(16)?.let { it or 0xFF000000.toInt() }
}

/** A slider over a coloured track, written when the finger lifts. */
@Composable
private fun GradientSlider(
    label: String?,
    valueText: String?,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    track: Brush,
    thumb: Color,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    Column {
        if (label != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant)
                if (valueText != null) Text(valueText, style = MaterialTheme.typography.labelLarge, color = OnSurface)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(track).align(Alignment.Center)
            )
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                onValueChangeFinished = onFinished,
                modifier = Modifier.fillMaxSize(),
                colors = SliderDefaults.colors(
                    thumbColor = thumb,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}
