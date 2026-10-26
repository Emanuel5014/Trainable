package com.emanuel5014.trainable.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.model.AppBackground
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.glass.GlassFilterChip
import com.emanuel5014.trainable.ui.components.glass.GlassFilledTonalButton
import com.emanuel5014.trainable.ui.components.glass.GlassTextButton
import com.emanuel5014.trainable.ui.theme.*
import com.emanuel5014.trainable.util.WallpaperStorage
import kotlin.math.roundToInt

@Composable
internal fun GlassMotionSettingsRow(viewModel: SettingsViewModel, enabled: Boolean) {
    val saved by viewModel.glassMotionEnabled.collectAsState()
    val checked = saved ?: LocalGlassMotionPreference.current
    val context = LocalContext.current
    val available = remember(context) { hasGlassMotionSensor(context) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.glass_motion_title), color = OnSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(if (available) R.string.glass_motion_description else R.string.glass_motion_unavailable),
                color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        SettingsSwitch(checked = checked && available, onCheckedChange = viewModel::setGlassMotionEnabled, enabled = enabled && available)
    }
}

@Composable
internal fun BackgroundAppearanceCard(viewModel: SettingsViewModel) {
    val saved by viewModel.appBackground.collectAsState()
    val appearance = saved ?: LocalAppBackground.current
    val importing by viewModel.wallpaperImporting.collectAsState()
    val failed by viewModel.wallpaperImportFailed.collectAsState()
    var dimming by remember(appearance.dimming) { mutableFloatStateOf(appearance.dimming) }
    var showColor by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val wallpaper = remember(context, appearance.wallpaperName) { WallpaperStorage.file(context, appearance.wallpaperName) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::importWallpaper)
    }
    fun pickPhoto() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    if (showColor) {
        CustomColorPickerDialog(initialColor = Color(appearance.color), initialStyle = 0,
            onDismiss = { showColor = false }, onApply = { color, _ -> viewModel.setBackgroundColor(color); showColor = false }, showStyle = false)
    }
    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.background_title), color = OnSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.background_description), color = OnSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AppBackground.THEME to R.string.background_theme, AppBackground.COLOR to R.string.background_color,
                    AppBackground.WALLPAPER to R.string.background_photo).forEach { (mode, label) ->
                    GlassFilterChip(selected = if (mode == AppBackground.THEME) !appearance.isCustom else appearance.mode == mode && appearance.isCustom,
                        enabled = !importing,
                        onClick = {
                            if (mode == AppBackground.WALLPAPER && wallpaper == null) pickPhoto()
                            else viewModel.setBackgroundMode(mode)
                        }, label = { Text(stringResource(label)) })
                }
            }
            if (appearance.isCustom) {
                val previewColor = if (appearance.mode == AppBackground.COLOR) Color.Black.copy(alpha = dimming).compositeOver(Color(appearance.color)) else MaterialTheme.colorScheme.background
                Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(20.dp)).background(previewColor)) {
                    if (appearance.mode == AppBackground.WALLPAPER && wallpaper != null) {
                        AsyncImage(model = wallpaper, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimming)))
                    }
                    Text(stringResource(R.string.background_preview), color = Color.White, modifier = Modifier.align(Alignment.Center), fontWeight = FontWeight.Bold)
                }
            }
            if (appearance.mode == AppBackground.COLOR) {
                val colors = listOf(0xFF35516B, 0xFF42644C, 0xFF6A426B, 0xFF7A4F35, 0xFF38404B, 0xFF000000)
                val names = listOf(R.string.background_blue, R.string.background_green, R.string.background_purple,
                    R.string.background_amber, R.string.background_graphite, R.string.background_black)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    colors.forEachIndexed { index, argb ->
                        val label = stringResource(names[index])
                        val selected = appearance.color == argb.toInt()
                        Box(Modifier.size(48.dp).semantics { contentDescription = label; this.selected = selected; role = Role.RadioButton }
                            .clickable { viewModel.setBackgroundColor(argb.toInt()) }.padding(4.dp).clip(CircleShape)
                            .background(Color(argb)).border(if (selected) 2.dp else 1.dp, if (selected) Primary else OnSurfaceVariant.copy(alpha = .4f), CircleShape))
                    }
                }
                GlassTextButton(onClick = { showColor = true }) { Text(stringResource(R.string.custom_color)) }
            }
            if (appearance.mode == AppBackground.WALLPAPER || importing) {
                GlassFilledTonalButton(onClick = ::pickPhoto, enabled = !importing, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (importing) R.string.background_importing else R.string.background_choose_photo))
                }
                if (wallpaper != null && !importing) {
                    GlassTextButton(onClick = viewModel::removeWallpaper) { Text(stringResource(R.string.background_remove_photo)) }
                }
            }
            if (failed) Text(stringResource(R.string.background_import_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (appearance.isCustom) {
                val dimmingLabel = stringResource(R.string.background_dimming)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dimmingLabel, color = OnSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    Text("${(dimming * 100).roundToInt()}%", color = OnSurface, style = MaterialTheme.typography.labelLarge)
                }
                Slider(value = dimming, onValueChange = { dimming = it },
                    onValueChangeFinished = { viewModel.setBackgroundDimming(dimming) }, valueRange = .65f.. .90f,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = dimmingLabel })
            }
        }
    }
}
