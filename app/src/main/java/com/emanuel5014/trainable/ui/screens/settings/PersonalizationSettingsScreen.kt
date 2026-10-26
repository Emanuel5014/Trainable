package com.emanuel5014.trainable.ui.screens.settings
import com.emanuel5014.trainable.ui.components.glass.glassPanelBackground

import com.emanuel5014.trainable.ui.theme.ScreenSurface

import androidx.compose.foundation.border
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.emanuel5014.trainable.ui.theme.liquidGlassSurface
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import com.emanuel5014.trainable.ui.components.glass.GlassAlertDialog as AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.emanuel5014.trainable.ui.components.glass.GlassIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import com.emanuel5014.trainable.ui.components.glass.GlassScaffold as Scaffold
import androidx.compose.material3.Text
import com.emanuel5014.trainable.ui.components.glass.GlassTextButton as TextButton
import com.emanuel5014.trainable.ui.components.glass.GlassTopAppBar as TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.getPalettePreviewColors
import com.emanuel5014.trainable.ui.theme.getSeedPreviewColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalizationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val dynamicColorSeed by viewModel.dynamicColorSeed.collectAsState()
    val wallpaperColors by viewModel.wallpaperColors.collectAsState()
    val themePalette by viewModel.themePalette.collectAsState()
    val themeStyle by viewModel.themeStyle.collectAsState()
    val savedGlassEnabled by viewModel.liquidGlassEnabled.collectAsState()
    val savedGlassIntensity by viewModel.glassIntensity.collectAsState()
    // Start with the already resolved app appearance, not a temporary 75%/enabled
    // value that replaces the saved look for one frame whenever this screen opens.
    val liquidGlassEnabled = savedGlassEnabled ?: com.emanuel5014.trainable.ui.theme.LocalLiquidGlassPreference.current
    val glassIntensity = savedGlassIntensity ?: com.emanuel5014.trainable.ui.theme.LocalGlassIntensity.current
    var glassPreview by remember(glassIntensity) { androidx.compose.runtime.mutableFloatStateOf(glassIntensity) }
    val themeMode by viewModel.themeMode.collectAsState()

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
                    viewModel.setDynamicColorSeed(savedSeedForRestore)
                    viewModel.setThemeStyle(savedStyleForRestore)
                }
                showCustomColorDialog = false
            },
            onApply = { colorSeed, style ->
                viewModel.setDynamicColorSeed(colorSeed)
                viewModel.setThemeStyle(style)
                showCustomColorDialog = false
            }
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.emanuel5014.trainable.ui.theme.LocalGlassIntensity provides glassPreview,
        com.emanuel5014.trainable.ui.theme.LocalLiquidGlassEnabled provides (liquidGlassEnabled && glassPreview > 0f)
    ) {
    Scaffold(
        containerColor = ScreenSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.personalization_title),
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
                    containerColor = ScreenSurface,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = OnSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GymCard(containerColor = SurfaceContainerHigh) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.liquid_glass_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OnSurface)
                        Text(stringResource(R.string.liquid_glass_description), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                    }
                    SettingsSwitch(checked = liquidGlassEnabled, onCheckedChange = viewModel::setLiquidGlassEnabled)
                }
                Spacer(Modifier.size(16.dp))
                val intensityLabel = stringResource(R.string.glass_intensity)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(intensityLabel, color = OnSurface, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.glass_intensity_percent, (glassPreview * 100).toInt()), color = Primary, fontWeight = FontWeight.Bold)
                }
                androidx.compose.material3.Slider(
                    value = glassPreview,
                    onValueChange = { glassPreview = it },
                    onValueChangeFinished = { viewModel.setGlassIntensity(glassPreview) },
                    enabled = liquidGlassEnabled,
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = intensityLabel },
                    thumb = {
                        Box(Modifier.size(32.dp).liquidGlassSurface(CircleShape, SurfaceContainerHigh)
                            .border(1.dp, Primary.copy(alpha = 0.65f), CircleShape))
                    }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.glass_solid), color = OnSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Text(stringResource(R.string.glass_liquid), color = OnSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.size(16.dp))
                GlassMotionSettingsRow(viewModel, liquidGlassEnabled && glassPreview > 0f)
            }
            BackgroundAppearanceCard(viewModel)
            GymCard(containerColor = SurfaceContainerHigh) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    var showThemeModeDialog by remember { mutableStateOf(false) }

                    if (showThemeModeDialog) {
                        AlertDialog(
                            onDismissRequest = { showThemeModeDialog = false },
                            containerColor = SurfaceContainerHigh,
                            title = { Text(stringResource(R.string.theme_mode), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
                            text = {
                                Column {
                                    LanguageOption(
                                        title = stringResource(R.string.theme_mode_system),
                                        isSelected = themeMode == 0,
                                        onClick = {
                                            viewModel.setThemeMode(0)
                                            showThemeModeDialog = false
                                        }
                                    )
                                    LanguageOption(
                                        title = stringResource(R.string.theme_mode_light),
                                        isSelected = themeMode == 1,
                                        onClick = {
                                            viewModel.setThemeMode(1)
                                            showThemeModeDialog = false
                                        }
                                    )
                                    LanguageOption(
                                        title = stringResource(R.string.theme_mode_dark),
                                        isSelected = themeMode == 2,
                                        onClick = {
                                            viewModel.setThemeMode(2)
                                            showThemeModeDialog = false
                                        }
                                    )
                                    LanguageOption(
                                        title = stringResource(R.string.theme_mode_oled),
                                        isSelected = themeMode == 3,
                                        onClick = {
                                            viewModel.setThemeMode(3)
                                            showThemeModeDialog = false
                                        }
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showThemeModeDialog = false }) {
                                    Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
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
                                Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    when (themeMode) {
                                        0 -> stringResource(R.string.theme_mode_system)
                                        1 -> stringResource(R.string.theme_mode_light)
                                        3 -> stringResource(R.string.theme_mode_oled)
                                        else -> stringResource(R.string.theme_mode_dark)
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
                                val isDynamicColorSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                                Text(stringResource(R.string.dynamic_color), style = MaterialTheme.typography.titleMedium, color = if (isDynamicColorSupported) OnSurface else OnSurfaceVariant, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    if (isDynamicColorSupported) stringResource(R.string.dynamic_color_desc) else stringResource(R.string.dynamic_color_not_supported),
                                    style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S,
                            onCheckedChange = { viewModel.setDynamicColor(it) },
                            enabled = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                        )
                    }

                    if (dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && wallpaperColors.isNotEmpty()) {
                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Palette, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.app_palette), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.app_palette_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
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
                                            .glassPanelBackground(SurfaceContainerHighest, CircleShape)
                                            .clickable {
                                                viewModel.setDynamicColorSeed(null)
                                                viewModel.setThemeStyle(0)
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
                                                    contentDescription = stringResource(R.string.system_default),
                                                    tint = Primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                Icons.Rounded.RestartAlt,
                                                contentDescription = stringResource(R.string.system_default),
                                                tint = OnSurfaceVariant,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Text(stringResource(R.string.palette_default), style = MaterialTheme.typography.labelSmall, color = if (dynamicColorSeed == null) Primary else OnSurfaceVariant, maxLines = 1)
                                }

                                val primarySeed = wallpaperColors.first()
                                val styleNames = listOf(
                                    stringResource(R.string.style_tonal_spot),
                                    stringResource(R.string.style_vibrant),
                                    stringResource(R.string.style_expressive),
                                    stringResource(R.string.style_neutral),
                                    stringResource(R.string.style_fruit_salad)
                                )
                                (0..4).forEach { styleIndex ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        PalettePreviewCircle(
                                            colors = getSeedPreviewColors(primarySeed, styleIndex),
                                            isSelected = dynamicColorSeed == primarySeed && themeStyle == styleIndex,
                                            onClick = {
                                                viewModel.setDynamicColorSeed(primarySeed)
                                                viewModel.setThemeStyle(styleIndex)
                                            }
                                        )
                                        Text(styleNames[styleIndex], style = MaterialTheme.typography.labelSmall, color = if (dynamicColorSeed == primarySeed && themeStyle == styleIndex) Primary else OnSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    if (!dynamicColor || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Palette, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(stringResource(R.string.app_palette), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                    Text(stringResource(R.string.app_palette_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val paletteNames = listOf(
                                    stringResource(R.string.palette_default),
                                    stringResource(R.string.palette_blue),
                                    stringResource(R.string.palette_green),
                                    stringResource(R.string.palette_red),
                                    stringResource(R.string.palette_purple),
                                    stringResource(R.string.palette_orange),
                                    stringResource(R.string.palette_pink),
                                    stringResource(R.string.palette_teal),
                                    stringResource(R.string.palette_monochrome)
                                )
                                (0..8).forEach { index ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        PalettePreviewCircle(
                                            colors = getPalettePreviewColors(index),
                                            isSelected = themePalette == index,
                                            onClick = { viewModel.setThemePalette(index) }
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
                                viewModel.setDynamicColorSeed(null)
                                viewModel.setThemeStyle(0)
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
                                Text(stringResource(R.string.custom_color), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                                if (hasCustomColor) {
                                    val styleName = when (themeStyle) {
                                        1 -> stringResource(R.string.style_vibrant)
                                        2 -> stringResource(R.string.style_expressive)
                                        3 -> stringResource(R.string.style_neutral)
                                        4 -> stringResource(R.string.style_fruit_salad)
                                        else -> stringResource(R.string.style_tonal_spot)
                                    }
                                    Text(styleName, style = MaterialTheme.typography.bodySmall, color = Primary, maxLines = 1)
                                } else {
                                    Text(stringResource(R.string.pick_theme_color), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, maxLines = 2)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        if (hasCustomColor) {
                            val previewColors = getSeedPreviewColors(dynamicColorSeed!!, themeStyle)
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
                                        viewModel.setDynamicColorSeed(null)
                                        viewModel.setThemeStyle(0)
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(stringResource(R.string.reset), color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    }
}
