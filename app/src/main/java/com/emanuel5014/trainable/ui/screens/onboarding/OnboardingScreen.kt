package com.emanuel5014.trainable.ui.screens.onboarding

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.fromHSV
import com.emanuel5014.trainable.ui.theme.getPalettePreviewColors
import com.emanuel5014.trainable.ui.theme.getSeedPreviewColors
import com.emanuel5014.trainable.ui.theme.toHSV
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 7 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var weightUnit by remember { mutableStateOf("kg") }
    var weeklyGoalInput by remember { mutableStateOf("3") }
    var hapticEnabled by remember { mutableStateOf(true) }
    var swipeActionsEnabled by remember { mutableStateOf(true) }
    var timerNotificationsEnabled by remember { mutableStateOf(true) }
    var gymMembershipExpiryNotificationsEnabled by remember { mutableStateOf(false) }
    var gymMembershipExpiryNotificationDaysBefore by remember { mutableIntStateOf(3) }
    var timerFinishedLockscreenVibrationDuration by remember { mutableIntStateOf(30) }
    var autoBackupEnabled by remember { mutableStateOf(false) }
    var autoBackupFrequency by remember { mutableIntStateOf(1) }
    var autoBackupFolderUri by remember { mutableStateOf<String?>(null) }
    var autoBackupMaxCount by remember { mutableIntStateOf(5) }
    var autoBackupIncludeImages by remember { mutableStateOf(false) }
    val dynamicColorEnabled by viewModel.dynamicColor.collectAsState(initial = true)
    val dynamicColorSeed by viewModel.dynamicColorSeed.collectAsState(initial = null)
    val themePalette by viewModel.themePalette.collectAsState(initial = 0)
    val themeStyle by viewModel.themeStyle.collectAsState(initial = 0)
    val themeMode by viewModel.themeMode.collectAsState(initial = 0)

    val backupStatus by viewModel.backupStatus.collectAsState()

    LaunchedEffect(backupStatus) {
        backupStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatus()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importDatabase(it) {
                Toast.makeText(context, "Import successful. Restarting app...", Toast.LENGTH_LONG).show()
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                exitProcess(0)
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.persistFolderUri(it)
            autoBackupFolderUri = it.toString()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.15f),
                            Surface,
                            Surface
                        )
                    )
                )
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (page) {
                    0 -> WelcomeSlide()
                    1 -> FeaturesSlide()
                    2 -> ConnectivitySlide()
                    3 -> ThemeSlide(
                        dynamicColor = dynamicColorEnabled,
                        onDynamicColorChange = { viewModel.setDynamicColor(it) },
                        dynamicColorSeed = dynamicColorSeed,
                        onDynamicColorSeedChange = { viewModel.setDynamicColorSeed(it) },
                        themePalette = themePalette,
                        onThemePaletteChange = { viewModel.setThemePalette(it) },
                        themeStyle = themeStyle,
                        onThemeStyleChange = { viewModel.setThemeStyle(it) },
                        themeMode = themeMode,
                        onThemeModeChange = { viewModel.setThemeMode(it) }
                    )
                    4 -> NotificationsSlide(
                        timerNotificationsEnabled = timerNotificationsEnabled,
                        onTimerNotificationsChange = { timerNotificationsEnabled = it },
                        gymMembershipExpiryNotificationsEnabled = gymMembershipExpiryNotificationsEnabled,
                        onGymMembershipExpiryNotificationsChange = { gymMembershipExpiryNotificationsEnabled = it },
                        gymMembershipExpiryNotificationDaysBefore = gymMembershipExpiryNotificationDaysBefore,
                        onGymMembershipExpiryNotificationDaysBeforeChange = { gymMembershipExpiryNotificationDaysBefore = it },
                        timerFinishedLockscreenVibrationDuration = timerFinishedLockscreenVibrationDuration,
                        onTimerFinishedLockscreenVibrationDurationChange = { timerFinishedLockscreenVibrationDuration = it },
                        hapticEnabled = hapticEnabled,
                        onHapticChange = { hapticEnabled = it },
                        swipeActionsEnabled = swipeActionsEnabled,
                        onSwipeActionsChange = { swipeActionsEnabled = it }
                    )
                    5 -> BackupSlide(
                        autoBackupEnabled = autoBackupEnabled,
                        onAutoBackupChange = { autoBackupEnabled = it },
                        autoBackupFrequency = autoBackupFrequency,
                        onAutoBackupFrequencyChange = { autoBackupFrequency = it },
                        autoBackupFolderUri = autoBackupFolderUri,
                        autoBackupMaxCount = autoBackupMaxCount,
                        onAutoBackupMaxCountChange = { autoBackupMaxCount = it },
                        autoBackupIncludeImages = autoBackupIncludeImages,
                        onAutoBackupIncludeImagesChange = { autoBackupIncludeImages = it },
                        onPickFolder = { folderPickerLauncher.launch(null) },
                        onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }
                    )
                    6 -> ProfileSetupSlide(
                        username = username,
                        onUsernameChange = { username = it },
                        weightInput = weightInput,
                        onWeightChange = { weightInput = it },
                        weightUnit = weightUnit,
                        onWeightUnitChange = { weightUnit = it },
                        weeklyGoalInput = weeklyGoalInput,
                        onWeeklyGoalChange = { weeklyGoalInput = it }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    val width by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "indicator_width")
                    val color by animateColorAsState(if (isSelected) Primary else OnSurfaceVariant.copy(alpha = 0.3f), label = "indicator_color")

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            val isLastPage = pagerState.currentPage == pagerState.pageCount - 1
            GymButton(
                onClick = {
                    coroutineScope.launch {
                        if (isLastPage) {
                            val weight = weightInput.replace(',', '.').toFloatOrNull() ?: 0f
                            val goal = weeklyGoalInput.toIntOrNull() ?: 3
                            viewModel.completeOnboarding(
                                username = username,
                                initialWeight = weight,
                                weeklyGoal = goal,
                                weightUnit = weightUnit,
                                hapticEnabled = hapticEnabled,
                                swipeActionsEnabled = swipeActionsEnabled,
                                timerNotificationsEnabled = timerNotificationsEnabled,
                                gymMembershipExpiryNotificationsEnabled = gymMembershipExpiryNotificationsEnabled,
                                gymMembershipExpiryNotificationDaysBefore = gymMembershipExpiryNotificationDaysBefore,
                                timerFinishedLockscreenVibrationDuration = timerFinishedLockscreenVibrationDuration,
                                autoBackupEnabled = autoBackupEnabled,
                                autoBackupFrequency = autoBackupFrequency,
                                autoBackupFolderUri = autoBackupFolderUri,
                                autoBackupMaxCount = autoBackupMaxCount,
                                autoBackupIncludeImages = autoBackupIncludeImages,
                                dynamicColor = dynamicColorEnabled,
                                dynamicColorSeed = dynamicColorSeed,
                                themePalette = themePalette,
                                themeStyle = themeStyle,
                                themeMode = themeMode
                            )
                            onFinished()
                        } else {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (isLastPage) stringResource(R.string.finish_setup) else stringResource(R.string.continue_text),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isLastPage) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun WelcomeSlide() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(56.dp))
                    .background(Primary.copy(alpha = 0.1f))
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(130.dp),
                tint = Primary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.displayMedium,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
    }
}

@Composable
private fun FeaturesSlide() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_features_title),
            style = MaterialTheme.typography.displaySmall,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            lineHeight = 44.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            FeatureItemExpressive(
                icon = Icons.Rounded.Bolt,
                title = stringResource(R.string.onboarding_feature_suggestions_title),
                desc = stringResource(R.string.onboarding_feature_suggestions_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.DragHandle,
                title = stringResource(R.string.onboarding_feature_control_title),
                desc = stringResource(R.string.onboarding_feature_control_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.Analytics,
                title = stringResource(R.string.onboarding_feature_insights_title),
                desc = stringResource(R.string.onboarding_feature_insights_desc)
            )
        }
    }
}

@Composable
private fun ConnectivitySlide() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_connectivity_title),
            style = MaterialTheme.typography.displaySmall,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            lineHeight = 44.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            FeatureItemExpressive(
                icon = Icons.Rounded.Backup,
                title = stringResource(R.string.onboarding_connectivity_backup_title),
                desc = stringResource(R.string.onboarding_connectivity_backup_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.Share,
                title = stringResource(R.string.onboarding_connectivity_share_plans_title),
                desc = stringResource(R.string.onboarding_connectivity_share_plans_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.IosShare,
                title = stringResource(R.string.onboarding_connectivity_share_workouts_title),
                desc = stringResource(R.string.onboarding_connectivity_share_workouts_desc)
            )
        }
    }
}

@Composable
private fun ThemeSlide(
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    dynamicColorSeed: Int?,
    onDynamicColorSeedChange: (Int?) -> Unit,
    themePalette: Int,
    onThemePaletteChange: (Int) -> Unit,
    themeStyle: Int,
    onThemeStyleChange: (Int) -> Unit,
    themeMode: Int,
    onThemeModeChange: (Int) -> Unit
) {
    var showCustomColorDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_theme_title),
            style = MaterialTheme.typography.displaySmall,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            lineHeight = 44.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            var showThemeModeDialog by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth().clickable { showThemeModeDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                        Text(
                            when (themeMode) {
                                0 -> stringResource(R.string.theme_mode_system)
                                1 -> stringResource(R.string.theme_mode_light)
                                else -> stringResource(R.string.theme_mode_dark)
                            },
                            style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant
                        )
                    }
                }
                if (showThemeModeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeModeDialog = false },
                        containerColor = SurfaceContainerHigh,
                        title = { Text(stringResource(R.string.theme_mode), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
                        text = {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onThemeModeChange(0)
                                            showThemeModeDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.theme_mode_system), style = MaterialTheme.typography.bodyLarge, color = OnSurface)
                                    if (themeMode == 0) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Primary)
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onThemeModeChange(1)
                                            showThemeModeDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.theme_mode_light), style = MaterialTheme.typography.bodyLarge, color = OnSurface)
                                    if (themeMode == 1) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Primary)
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onThemeModeChange(2)
                                            showThemeModeDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.theme_mode_dark), style = MaterialTheme.typography.bodyLarge, color = OnSurface)
                                    if (themeMode == 2) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Primary)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeModeDialog = false }) {
                                Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant)
                            }
                        }
                    )
                }

                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
            }

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            // Dynamic Color Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.dynamic_color), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                        Text(stringResource(R.string.dynamic_color_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = dynamicColor,
                    onCheckedChange = onDynamicColorChange,
                    thumbContent = if (dynamicColor) {
                        {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize), tint = Primary)
                        }
                    } else null
                )
            }

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            if (dynamicColor) {
                // Style selector (when dynamic color is on)
                val defaultSeed = Color(0xFF1976D2).toArgb()
                val seed = dynamicColorSeed ?: defaultSeed

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(SurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                        }
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
                        val styleNames = listOf("Tonal Spot", "Vibrant", "Expressive", "Neutral", "Fruit Salad")
                        (0..4).forEach { styleIndex ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PalettePreviewCircle(
                                    colors = getSeedPreviewColors(seed, styleIndex),
                                    isSelected = dynamicColorSeed == null && themeStyle == styleIndex || dynamicColorSeed != null && themeStyle == styleIndex,
                                    onClick = {
                                        onDynamicColorSeedChange(null)
                                        onThemeStyleChange(styleIndex)
                                    }
                                )
                                Text(styleNames[styleIndex], style = MaterialTheme.typography.labelSmall, color = if (themeStyle == styleIndex) Primary else OnSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            } else {
                // Palette selector (when dynamic color is off)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(SurfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                        }
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
                        val paletteNames = listOf("Default", "Blue", "Green", "Red", "Purple", "Orange", "Pink", "Teal", "Monochrome")
                        (0..8).forEach { index ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PalettePreviewCircle(
                                    colors = getPalettePreviewColors(index),
                                    isSelected = themePalette == index,
                                    onClick = { onThemePaletteChange(index) }
                                )
                                Text(paletteNames[index], style = MaterialTheme.typography.labelSmall, color = if (themePalette == index) Primary else OnSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            // Custom Color
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCustomColorDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Custom Color", style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                        if (dynamicColorSeed != null) {
                            val styleName = when (themeStyle) {
                                1 -> "Vibrant"
                                2 -> "Expressive"
                                3 -> "Neutral"
                                4 -> "Fruit Salad"
                                else -> "Tonal Spot"
                            }
                            Text("Custom \u2022 $styleName", style = MaterialTheme.typography.bodySmall, color = Primary)
                        } else {
                            Text("Pick your own theme color", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                if (dynamicColorSeed != null) {
                    val previewColors = getSeedPreviewColors(dynamicColorSeed, themeStyle)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        previewColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                onDynamicColorSeedChange(null)
                                onThemeStyleChange(0)
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Reset", color = OnSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                }
            }
        }

        if (showCustomColorDialog) {
            CustomColorPickerDialog(
                initialColor = dynamicColorSeed?.let { Color(it) },
                initialStyle = themeStyle,
                onDismiss = { showCustomColorDialog = false },
                onApply = { colorSeed, style ->
                    onDynamicColorSeedChange(colorSeed)
                    onThemeStyleChange(style)
                    showCustomColorDialog = false
                }
            )
        }
    }
}

@Composable
private fun NotificationsSlide(
    timerNotificationsEnabled: Boolean,
    onTimerNotificationsChange: (Boolean) -> Unit,
    gymMembershipExpiryNotificationsEnabled: Boolean,
    onGymMembershipExpiryNotificationsChange: (Boolean) -> Unit,
    gymMembershipExpiryNotificationDaysBefore: Int,
    onGymMembershipExpiryNotificationDaysBeforeChange: (Int) -> Unit,
    timerFinishedLockscreenVibrationDuration: Int,
    onTimerFinishedLockscreenVibrationDurationChange: (Int) -> Unit,
    hapticEnabled: Boolean,
    onHapticChange: (Boolean) -> Unit,
    swipeActionsEnabled: Boolean,
    onSwipeActionsChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = if (timerNotificationsEnabled) 140.dp else 0.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_notifications_title),
            style = MaterialTheme.typography.displaySmall,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            lineHeight = 44.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Timer Notifications + Lockscreen Vibration Duration
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CustomizeToggleItem(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.timer_notifications),
                    desc = stringResource(R.string.timer_notifications_desc),
                    checked = timerNotificationsEnabled,
                    onCheckedChange = onTimerNotificationsChange
                )

                if (timerNotificationsEnabled) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.lockscreen_vibration),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (timerFinishedLockscreenVibrationDuration == 0) {
                                    stringResource(R.string.vibration_default)
                                } else {
                                    stringResource(R.string.vibration_seconds, timerFinishedLockscreenVibrationDuration)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = Primary,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        var lastHapticSliderValue by remember { mutableIntStateOf(-1) }

                        Slider(
                            value = timerFinishedLockscreenVibrationDuration.toFloat(),
                            onValueChange = {
                                val newValue = it.toInt()
                                if (newValue != lastHapticSliderValue) {
                                    lastHapticSliderValue = newValue
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onTimerFinishedLockscreenVibrationDurationChange(newValue)
                            },
                            valueRange = 0f..30f,
                            steps = 29,
                            colors = SliderDefaults.colors(
                                thumbColor = Primary,
                                activeTrackColor = Primary,
                                inactiveTrackColor = SurfaceContainerHighest,
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            // Membership Notifications
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CustomizeToggleItem(
                    icon = Icons.Rounded.CreditCard,
                    title = stringResource(R.string.gym_membership_notifications),
                    desc = stringResource(R.string.gym_membership_notifications_desc),
                    checked = gymMembershipExpiryNotificationsEnabled,
                    onCheckedChange = onGymMembershipExpiryNotificationsChange
                )

                if (gymMembershipExpiryNotificationsEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 64.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.notify_days_before),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (gymMembershipExpiryNotificationDaysBefore > 1) onGymMembershipExpiryNotificationDaysBeforeChange(gymMembershipExpiryNotificationDaysBefore - 1) }) {
                                Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                            }
                            Text(
                                text = gymMembershipExpiryNotificationDaysBefore.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = Primary,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = { if (gymMembershipExpiryNotificationDaysBefore < 30) onGymMembershipExpiryNotificationDaysBeforeChange(gymMembershipExpiryNotificationDaysBefore + 1) }) {
                                Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            // Haptic Feedback
            CustomizeToggleItem(
                icon = Icons.Rounded.Vibration,
                title = stringResource(R.string.tactile_feedback),
                desc = stringResource(R.string.tactile_feedback_desc),
                checked = hapticEnabled,
                onCheckedChange = onHapticChange
            )

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            // Swipe Actions
            CustomizeToggleItem(
                icon = Icons.Rounded.RestartAlt,
                title = stringResource(R.string.swipe_actions),
                desc = stringResource(R.string.swipe_actions_desc),
                checked = swipeActionsEnabled,
                onCheckedChange = onSwipeActionsChange
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_setup_footer),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun BackupSlide(
    autoBackupEnabled: Boolean,
    onAutoBackupChange: (Boolean) -> Unit,
    autoBackupFrequency: Int,
    onAutoBackupFrequencyChange: (Int) -> Unit,
    autoBackupFolderUri: String?,
    autoBackupMaxCount: Int,
    onAutoBackupMaxCountChange: (Int) -> Unit,
    autoBackupIncludeImages: Boolean,
    onAutoBackupIncludeImagesChange: (Boolean) -> Unit,
    onPickFolder: () -> Unit,
    onImport: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_backup_title),
            style = MaterialTheme.typography.displaySmall,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            lineHeight = 44.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Auto Backup Toggle
            CustomizeToggleItem(
                icon = Icons.Rounded.Backup,
                title = stringResource(R.string.auto_backup),
                desc = if (autoBackupEnabled) stringResource(R.string.backup_enabled, autoBackupFrequency) else stringResource(R.string.backup_disabled),
                checked = autoBackupEnabled,
                onCheckedChange = onAutoBackupChange
            )

            if (autoBackupEnabled) {
                HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                // Frequency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.frequency), style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (autoBackupFrequency > 1) onAutoBackupFrequencyChange(autoBackupFrequency - 1) }) {
                            Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                        }
                        Text(stringResource(R.string.day_s, autoBackupFrequency), style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
                        IconButton(onClick = { if (autoBackupFrequency < 7) onAutoBackupFrequencyChange(autoBackupFrequency + 1) }) {
                            Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                // Max Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.keep_last), style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (autoBackupMaxCount > 1) onAutoBackupMaxCountChange(autoBackupMaxCount - 1) }) {
                            Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Decrease", tint = OnSurfaceVariant)
                        }
                        Text(stringResource(R.string.backup_s, autoBackupMaxCount), style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Black)
                        IconButton(onClick = { if (autoBackupMaxCount < 10) onAutoBackupMaxCountChange(autoBackupMaxCount + 1) }) {
                            Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Increase", tint = OnSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                // Include Images
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.include_images), style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    }
                    Switch(
                        checked = autoBackupIncludeImages,
                        onCheckedChange = onAutoBackupIncludeImagesChange,
                        thumbContent = if (autoBackupIncludeImages) {
                            {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize), tint = Primary)
                            }
                        } else null
                    )
                }

                HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                // Folder Picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.storage_location), style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    GymButton(
                        onClick = onPickFolder,
                        containerColor = if (autoBackupFolderUri == null) Primary.copy(alpha = 0.1f) else SurfaceContainerHighest,
                        contentColor = if (autoBackupFolderUri == null) Primary else OnSurface
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (autoBackupFolderUri != null) "Folder selected"
                            else stringResource(R.string.choose_folder)
                        )
                    }
                    if (autoBackupFolderUri == null) {
                        Text(
                            stringResource(R.string.folder_required_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Error
                        )
                    }
                }
            }

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            // Import Button
            GymButton(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceContainerHigh,
                contentColor = OnSurface
            ) {
                Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.import_database), fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_setup_footer),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun ProfileSetupSlide(
    username: String,
    onUsernameChange: (String) -> Unit,
    weightInput: String,
    onWeightChange: (String) -> Unit,
    weightUnit: String,
    onWeightUnitChange: (String) -> Unit,
    weeklyGoalInput: String,
    onWeeklyGoalChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(R.string.onboarding_setup_title),
            style = MaterialTheme.typography.displaySmall,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            lineHeight = 44.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            GymInputField(
                value = username,
                onValueChange = onUsernameChange,
                label = stringResource(R.string.onboarding_setup_username_label),
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_setup_weight_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GymInputField(
                        value = weightInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() || it == '.' || it == ',' }) {
                                onWeightChange(newValue)
                            }
                        },
                        label = "0.0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainerHigh)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("kg", "lb").forEach { unit ->
                            val isSelected = weightUnit == unit
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Primary else Color.Transparent)
                                    .clickable { onWeightUnitChange(unit) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unit,
                                    color = if (isSelected) OnPrimary else OnSurfaceVariant,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            GymInputField(
                value = weeklyGoalInput,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        onWeeklyGoalChange(newValue)
                    }
                },
                label = stringResource(R.string.onboarding_setup_goal_label),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Text(
                        stringResource(R.string.days_this_week),
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_setup_footer),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun FeatureItemExpressive(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun CustomizeToggleItem(
    icon: ImageVector,
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                        tint = Primary
                    )
                }
            } else null
        )
    }
}

@Composable
private fun PalettePreviewCircle(
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

                    drawArc(color = primary, startAngle = 180f, sweepAngle = 90f, useCenter = true)
                    drawArc(color = secondary, startAngle = 270f, sweepAngle = 90f, useCenter = true)
                    drawArc(color = tertiary, startAngle = 0f, sweepAngle = 90f, useCenter = true)
                    drawArc(color = neutral, startAngle = 90f, sweepAngle = 90f, useCenter = true)
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
                Icon(
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
private fun CustomColorPickerDialog(
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
            Text("Custom Color", fontWeight = FontWeight.ExtraBold, color = OnSurface)
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

                HueSlider(hue = hue, onHueChange = { hue = it }, currentColor = currentColor)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme Preview", style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val previewColors = getSeedPreviewColors(seedArgb, selectedStyle)
                        previewColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Style", style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
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
                Text("Apply".uppercase(), color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel".uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    currentColor: Color
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Hue", style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant)
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
