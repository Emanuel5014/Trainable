package com.emanuel5014.trainable.ui.screens.settings

import com.emanuel5014.trainable.ui.util.bodyPadding
import com.emanuel5014.trainable.ui.util.scrollEndInset
import com.emanuel5014.trainable.ui.util.plusBottom
import com.emanuel5014.trainable.sharedui.resources.Res
import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.sharedui.resources.app_palette
import com.emanuel5014.trainable.sharedui.resources.app_palette_desc
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.custom_color
import com.emanuel5014.trainable.sharedui.resources.dynamic_color
import com.emanuel5014.trainable.sharedui.resources.dynamic_color_desc
import com.emanuel5014.trainable.sharedui.resources.dynamic_color_not_supported
import com.emanuel5014.trainable.sharedui.resources.glass_blur
import com.emanuel5014.trainable.sharedui.resources.glass_blur_desc
import com.emanuel5014.trainable.sharedui.resources.glass_intensity
import com.emanuel5014.trainable.sharedui.resources.glass_intensity_desc
import com.emanuel5014.trainable.sharedui.resources.glass_part_action_buttons
import com.emanuel5014.trainable.sharedui.resources.glass_part_action_buttons_desc
import com.emanuel5014.trainable.sharedui.resources.glass_part_header_buttons
import com.emanuel5014.trainable.sharedui.resources.glass_part_header_buttons_desc
import com.emanuel5014.trainable.sharedui.resources.glass_part_nav_bar
import com.emanuel5014.trainable.sharedui.resources.glass_part_nav_bar_desc
import com.emanuel5014.trainable.sharedui.resources.glass_part_nav_bar_needs_floating
import com.emanuel5014.trainable.sharedui.resources.glass_part_overlays
import com.emanuel5014.trainable.sharedui.resources.glass_part_overlays_desc
import com.emanuel5014.trainable.sharedui.resources.glass_part_search_bars
import com.emanuel5014.trainable.sharedui.resources.glass_part_search_bars_desc
import com.emanuel5014.trainable.sharedui.resources.glass_part_sliders
import com.emanuel5014.trainable.sharedui.resources.glass_part_sliders_desc
import com.emanuel5014.trainable.sharedui.resources.glass_part_switches
import com.emanuel5014.trainable.sharedui.resources.glass_part_switches_desc
import com.emanuel5014.trainable.sharedui.resources.glass_part_tabs
import com.emanuel5014.trainable.sharedui.resources.glass_part_tabs_desc
import com.emanuel5014.trainable.sharedui.resources.glass_part_workout_nav
import com.emanuel5014.trainable.sharedui.resources.glass_part_workout_nav_desc
import com.emanuel5014.trainable.sharedui.resources.glass_part_workout_panel
import com.emanuel5014.trainable.sharedui.resources.glass_part_workout_panel_desc
import com.emanuel5014.trainable.sharedui.resources.glass_parts
import com.emanuel5014.trainable.sharedui.resources.glass_jiggle
import com.emanuel5014.trainable.sharedui.resources.glass_jiggle_desc
import com.emanuel5014.trainable.sharedui.resources.glass_stretch
import com.emanuel5014.trainable.sharedui.resources.glass_stretch_desc
import com.emanuel5014.trainable.sharedui.resources.liquid_glass
import com.emanuel5014.trainable.sharedui.resources.liquid_glass_desc
import com.emanuel5014.trainable.sharedui.resources.liquid_glass_unavailable
import com.emanuel5014.trainable.sharedui.resources.palette_blue
import com.emanuel5014.trainable.sharedui.resources.palette_default
import com.emanuel5014.trainable.sharedui.resources.palette_green
import com.emanuel5014.trainable.sharedui.resources.palette_monochrome
import com.emanuel5014.trainable.sharedui.resources.palette_orange
import com.emanuel5014.trainable.sharedui.resources.palette_pink
import com.emanuel5014.trainable.sharedui.resources.palette_purple
import com.emanuel5014.trainable.sharedui.resources.palette_red
import com.emanuel5014.trainable.sharedui.resources.palette_teal
import com.emanuel5014.trainable.sharedui.resources.personalization_title
import com.emanuel5014.trainable.sharedui.resources.pick_theme_color
import com.emanuel5014.trainable.sharedui.resources.reset
import com.emanuel5014.trainable.sharedui.resources.style_expressive
import com.emanuel5014.trainable.sharedui.resources.style_fruit_salad
import com.emanuel5014.trainable.sharedui.resources.style_neutral
import com.emanuel5014.trainable.sharedui.resources.style_tonal_spot
import com.emanuel5014.trainable.sharedui.resources.style_vibrant
import com.emanuel5014.trainable.sharedui.resources.system_default
import com.emanuel5014.trainable.sharedui.resources.theme_mode
import com.emanuel5014.trainable.sharedui.resources.theme_mode_dark
import com.emanuel5014.trainable.sharedui.resources.theme_mode_light
import com.emanuel5014.trainable.sharedui.resources.theme_mode_system
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Settings
import com.emanuel5014.trainable.ui.components.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.data.model.GlassSettings
import com.emanuel5014.trainable.data.model.NavBarStyle
import kotlin.math.roundToInt
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationSettingsContent(
    onNavigateBack: () -> Unit,
    state: SettingsState,
    wallpaperColors: List<Int> = emptyList(),
    dynamicColorSupported: Boolean = false,
    glassSupported: Boolean = false,
    /** The glass parts this platform draws; the others get no switch. */
    glassParts: Set<GlassPart> = GlassPart.entries.toSet(),
    /** What the overlays switch says; Android names the version it needs, iOS has no such limit. */
    overlaysDescription: org.jetbrains.compose.resources.StringResource = Res.string.glass_part_overlays_desc,
    dynamicColorUnsupportedText: org.jetbrains.compose.resources.StringResource = Res.string.dynamic_color_not_supported
) {
    val dynamicColor by state.dynamicColor.collectAsState()
    val dynamicColorSeed by state.dynamicColorSeed.collectAsState()
    val themePalette by state.themePalette.collectAsState()
    val themeStyle by state.themeStyle.collectAsState()
    val themeMode by state.themeMode.collectAsState()
    val glassSettings by state.glassSettings.collectAsState()
    val navBarStyle by state.navBarStyle.collectAsState()

    val paletteColors = LocalPaletteColors.current
    val hasCustomColor = dynamicColorSeed != null && wallpaperColors.firstOrNull()?.let { dynamicColorSeed != it } ?: true

    var showCustomColorDialog by remember { mutableStateOf(false) }
    var savedSeedForRestore by remember { mutableStateOf<Int?>(null) }
    var savedStyleForRestore by remember { mutableIntStateOf(0) }

    if (showCustomColorDialog) {
        CustomColorPickerDialog(
            initialColor = savedSeedForRestore?.let { Color(it) },
            initialStyle = savedStyleForRestore,
            onDismiss = {
                if (savedSeedForRestore != null) {
                    state.setDynamicColorSeed(savedSeedForRestore)
                    state.setThemeStyle(savedStyleForRestore)
                }
                showCustomColorDialog = false
            },
            onApply = { colorSeed, style ->
                state.setDynamicColorSeed(colorSeed)
                state.setThemeStyle(style)
                showCustomColorDialog = false
            }
        )
    }

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.personalization_title),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = OnSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues.bodyPadding())
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .padding(bottom = paddingValues.scrollEndInset()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GymCard(containerColor = SurfaceContainerHigh) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    var showThemeModeDialog by remember { mutableStateOf(false) }

                    if (showThemeModeDialog) {
                        AlertDialog(
                            onDismissRequest = { showThemeModeDialog = false },
                            containerColor = SurfaceContainerHigh,
                            title = { Text(stringResource(Res.string.theme_mode), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
                            text = {
                                Column {
                                    LanguageOption(
                                        title = stringResource(Res.string.theme_mode_system),
                                        isSelected = themeMode == 0,
                                        onClick = {
                                            state.setThemeMode(0)
                                            showThemeModeDialog = false
                                        }
                                    )
                                    LanguageOption(
                                        title = stringResource(Res.string.theme_mode_light),
                                        isSelected = themeMode == 1,
                                        onClick = {
                                            state.setThemeMode(1)
                                            showThemeModeDialog = false
                                        }
                                    )
                                    LanguageOption(
                                        title = stringResource(Res.string.theme_mode_dark),
                                        isSelected = themeMode == 2,
                                        onClick = {
                                            state.setThemeMode(2)
                                            showThemeModeDialog = false
                                        }
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showThemeModeDialog = false }) {
                                    Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showThemeModeDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(Res.string.theme_mode), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    when (themeMode) {
                                        0 -> stringResource(Res.string.theme_mode_system)
                                        1 -> stringResource(Res.string.theme_mode_light)
                                        else -> stringResource(Res.string.theme_mode_dark)
                                    },
                                    style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                val isDynamicColorSupported = dynamicColorSupported
                                Text(stringResource(Res.string.dynamic_color), style = MaterialTheme.typography.titleMedium, color = if (isDynamicColorSupported) OnSurface else OnSurfaceVariant, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    if (isDynamicColorSupported) stringResource(Res.string.dynamic_color_desc) else stringResource(dynamicColorUnsupportedText),
                                    style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = dynamicColor && dynamicColorSupported,
                            onCheckedChange = { state.setDynamicColor(it) },
                            enabled = dynamicColorSupported
                        )
                    }

                    if (dynamicColor && dynamicColorSupported && wallpaperColors.isNotEmpty()) {
                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Palette, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.app_palette), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(Res.string.app_palette_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceContainerHighest)
                                            .clickable {
                                                state.setDynamicColorSeed(null)
                                                state.setThemeStyle(0)
                                            }
                                            .padding(if (dynamicColorSeed == null) 4.dp else 0.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dynamicColorSeed == null) {
                                            Box(
                                                modifier = Modifier.fillMaxSize().background(Primary.copy(alpha = 0.2f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Rounded.RestartAlt,
                                                    contentDescription = stringResource(Res.string.system_default),
                                                    tint = Primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                Icons.Rounded.RestartAlt,
                                                contentDescription = stringResource(Res.string.system_default),
                                                tint = OnSurfaceVariant,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Text(stringResource(Res.string.palette_default), style = MaterialTheme.typography.labelSmall, color = if (dynamicColorSeed == null) Primary else OnSurfaceVariant, maxLines = 1)
                                }

                                val primarySeed = wallpaperColors.first()
                                val styleNames = listOf(
                                    stringResource(Res.string.style_tonal_spot),
                                    stringResource(Res.string.style_vibrant),
                                    stringResource(Res.string.style_expressive),
                                    stringResource(Res.string.style_neutral),
                                    stringResource(Res.string.style_fruit_salad)
                                )
                                (0..4).forEach { styleIndex ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        PalettePreviewCircle(
                                            colors = paletteColors.seed(primarySeed, styleIndex),
                                            isSelected = dynamicColorSeed == primarySeed && themeStyle == styleIndex,
                                            onClick = {
                                                state.setDynamicColorSeed(primarySeed)
                                                state.setThemeStyle(styleIndex)
                                            },
                                            label = styleNames[styleIndex]
                                        )
                                        Text(styleNames[styleIndex], style = MaterialTheme.typography.labelSmall, color = if (dynamicColorSeed == primarySeed && themeStyle == styleIndex) Primary else OnSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    if (!dynamicColor || !dynamicColorSupported) {
                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Palette, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(Res.string.app_palette), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(Res.string.app_palette_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val paletteNames = listOf(
                                    stringResource(Res.string.palette_default),
                                    stringResource(Res.string.palette_blue),
                                    stringResource(Res.string.palette_green),
                                    stringResource(Res.string.palette_red),
                                    stringResource(Res.string.palette_purple),
                                    stringResource(Res.string.palette_orange),
                                    stringResource(Res.string.palette_pink),
                                    stringResource(Res.string.palette_teal),
                                    stringResource(Res.string.palette_monochrome)
                                )
                                (0..8).forEach { index ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        PalettePreviewCircle(
                                            colors = paletteColors.palette(index),
                                            isSelected = themePalette == index,
                                            onClick = { state.setThemePalette(index) },
                                            label = paletteNames[index]
                                        )
                                        Text(paletteNames[index], style = MaterialTheme.typography.labelSmall, color = if (themePalette == index) Primary else OnSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                savedSeedForRestore = dynamicColorSeed
                                savedStyleForRestore = themeStyle
                                state.setDynamicColorSeed(null)
                                state.setThemeStyle(0)
                                showCustomColorDialog = true
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(Res.string.custom_color), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                                if (hasCustomColor) {
                                    val styleName = when (themeStyle) {
                                        1 -> stringResource(Res.string.style_vibrant)
                                        2 -> stringResource(Res.string.style_expressive)
                                        3 -> stringResource(Res.string.style_neutral)
                                        4 -> stringResource(Res.string.style_fruit_salad)
                                        else -> stringResource(Res.string.style_tonal_spot)
                                    }
                                    Text(styleName, style = MaterialTheme.typography.bodySmall, color = Primary, maxLines = 1)
                                } else {
                                    Text(stringResource(Res.string.pick_theme_color), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, maxLines = 2)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        if (hasCustomColor) {
                            val previewColors = paletteColors.seed(dynamicColorSeed!!, themeStyle)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PalettePreviewCircle(
                                    colors = previewColors,
                                    isSelected = false,
                                    onClick = {}
                                )
                                TextButton(
                                    onClick = {
                                        state.setDynamicColorSeed(null)
                                        state.setThemeStyle(0)
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(stringResource(Res.string.reset), color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }
                    }
                }
            }

            LiquidGlassCard(
                settings = glassSettings,
                supported = glassSupported,
                parts = glassParts,
                overlaysDescription = overlaysDescription,
                floatingNavBar = navBarStyle == NavBarStyle.Floating,
                onEnabledChange = state::setLiquidGlassEnabled,
                onIntensityChange = state::setGlassIntensity,
                onBlurChange = state::setGlassBlur,
                onStretchChange = state::setGlassStretch,
                onJiggleChange = state::setGlassJiggle,
                onPartChange = state::setGlassPart
            )
        }
    }
}

/** Liquid Glass on/off, how much of it, and on which parts of the app. */
@Composable
private fun LiquidGlassCard(
    settings: GlassSettings,
    supported: Boolean,
    parts: Set<GlassPart>,
    overlaysDescription: org.jetbrains.compose.resources.StringResource,
    floatingNavBar: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onStretchChange: (Boolean) -> Unit,
    onJiggleChange: (Boolean) -> Unit,
    onPartChange: (GlassPart, Boolean) -> Unit
) {
    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassSwitchRow(
                icon = Icons.Rounded.AutoAwesome,
                title = stringResource(Res.string.liquid_glass),
                description = stringResource(if (supported) Res.string.liquid_glass_desc else Res.string.liquid_glass_unavailable),
                checked = settings.enabled && supported,
                enabled = supported,
                onCheckedChange = onEnabledChange,
                emphasized = true
            )

            if (settings.enabled && supported) {
                HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                GlassLevelSlider(
                    title = stringResource(Res.string.glass_intensity),
                    description = stringResource(Res.string.glass_intensity_desc),
                    value = settings.intensity,
                    onValueChange = onIntensityChange
                )
                GlassLevelSlider(
                    title = stringResource(Res.string.glass_blur),
                    description = stringResource(Res.string.glass_blur_desc),
                    value = settings.blur,
                    onValueChange = onBlurChange
                )
                GlassSwitchRow(
                    icon = Icons.Rounded.OpenInFull,
                    title = stringResource(Res.string.glass_stretch),
                    description = stringResource(Res.string.glass_stretch_desc),
                    checked = settings.stretch,
                    onCheckedChange = onStretchChange
                )
                GlassSwitchRow(
                    icon = Icons.Rounded.Waves,
                    title = stringResource(Res.string.glass_jiggle),
                    description = stringResource(Res.string.glass_jiggle_desc),
                    checked = settings.jiggle,
                    enabled = floatingNavBar && GlassPart.NavBar in settings.parts,
                    onCheckedChange = onJiggleChange
                )

                HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                Text(
                    stringResource(Res.string.glass_parts).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.Black
                )
                GlassSwitchRow(
                    icon = Icons.Rounded.Dashboard,
                    title = stringResource(Res.string.glass_part_nav_bar),
                    description = stringResource(
                        if (floatingNavBar) Res.string.glass_part_nav_bar_desc else Res.string.glass_part_nav_bar_needs_floating
                    ),
                    checked = GlassPart.NavBar in settings.parts,
                    onCheckedChange = { onPartChange(GlassPart.NavBar, it) }
                )
                GlassSwitchRow(
                    icon = Icons.Rounded.Settings,
                    title = stringResource(Res.string.glass_part_header_buttons),
                    description = stringResource(Res.string.glass_part_header_buttons_desc),
                    checked = GlassPart.HeaderButtons in settings.parts,
                    onCheckedChange = { onPartChange(GlassPart.HeaderButtons, it) }
                )
                GlassSwitchRow(
                    icon = Icons.Rounded.Add,
                    title = stringResource(Res.string.glass_part_action_buttons),
                    description = stringResource(Res.string.glass_part_action_buttons_desc),
                    checked = GlassPart.ActionButtons in settings.parts,
                    onCheckedChange = { onPartChange(GlassPart.ActionButtons, it) }
                )
                GlassSwitchRow(
                    icon = Icons.Rounded.FitnessCenter,
                    title = stringResource(Res.string.glass_part_workout_panel),
                    description = stringResource(Res.string.glass_part_workout_panel_desc),
                    checked = GlassPart.WorkoutPanel in settings.parts,
                    onCheckedChange = { onPartChange(GlassPart.WorkoutPanel, it) }
                )
                GlassSwitchRow(
                    icon = Icons.Rounded.SwapHoriz,
                    title = stringResource(Res.string.glass_part_workout_nav),
                    description = stringResource(Res.string.glass_part_workout_nav_desc),
                    checked = GlassPart.WorkoutNav in settings.parts,
                    onCheckedChange = { onPartChange(GlassPart.WorkoutNav, it) }
                )
                if (GlassPart.Overlays in parts) GlassSwitchRow(
                    icon = Icons.Rounded.Layers,
                    title = stringResource(Res.string.glass_part_overlays),
                    description = stringResource(overlaysDescription),
                    checked = GlassPart.Overlays in settings.parts,
                    onCheckedChange = { onPartChange(GlassPart.Overlays, it) }
                )
                GlassSwitchRow(
                    icon = Icons.Rounded.ToggleOn,
                    title = stringResource(Res.string.glass_part_switches),
                    description = stringResource(Res.string.glass_part_switches_desc),
                    checked = GlassPart.Switches in settings.parts,
                    onCheckedChange = { onPartChange(GlassPart.Switches, it) }
                )
                GlassSwitchRow(
                    icon = Icons.Rounded.Tab,
                    title = stringResource(Res.string.glass_part_tabs),
                    description = stringResource(Res.string.glass_part_tabs_desc),
                    checked = GlassPart.Tabs in settings.parts,
                    onCheckedChange = { onPartChange(GlassPart.Tabs, it) }
                )
                GlassSwitchRow(
                    icon = Icons.Rounded.Search,
                    title = stringResource(Res.string.glass_part_search_bars),
                    description = stringResource(Res.string.glass_part_search_bars_desc),
                    checked = GlassPart.SearchBars in settings.parts,
                    onCheckedChange = { onPartChange(GlassPart.SearchBars, it) }
                )
                GlassSwitchRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(Res.string.glass_part_sliders),
                    description = stringResource(Res.string.glass_part_sliders_desc),
                    checked = GlassPart.Sliders in settings.parts,
                    onCheckedChange = { onPartChange(GlassPart.Sliders, it) }
                )
            }
        }
    }
}

/** One 0-100 % level of the glass, saved on release only so dragging does not write to disk every frame. */
@Composable
private fun GlassLevelSlider(title: String, description: String, value: Float, onValueChange: (Float) -> Unit) {
    var draft by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            Text(
                "${(draft * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { onValueChange(draft) },
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SurfaceContainerHighest
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GlassSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    emphasized: Boolean = false,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    color = OnSurface,
                    fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.Bold
                )
                Text(description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        SettingsSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
