@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import com.emanuel5014.trainable.ui.components.Slider
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.isKeyboardVisible
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.fromHSV
import com.emanuel5014.trainable.ui.theme.toHSV
import kotlinx.coroutines.launch

import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.screens.settings.LocalPaletteColors
import com.emanuel5014.trainable.ui.components.motion.toComposePath
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.CalendarMonth

@Composable
fun OnboardingContent(
    model: OnboardingModel,
    onFinished: () -> Unit,
    choices: OnboardingChoices,
    pages: List<Int>,
    selectedAiModelVariant: String = "e2b",
    wallpaperSupported: Boolean = true,
    customVibrationSupported: Boolean = true,
    automaticBackupsSupported: Boolean = true,
    logo: @Composable (Modifier, Color) -> Unit = { modifier, tint ->
        Icon(painterResource(Res.drawable.ic_app_logo), null, modifier, tint = tint)
    },
    prepareCompletion: suspend (OnboardingChoices) -> Unit = {},
    platformPage: @Composable (Int, OnboardingChoices) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    var completing by remember { mutableStateOf(false) }
    val showMessage = com.emanuel5014.trainable.ui.components.LocalShowMessage.current
    var username by choices.username
    var weightInput by choices.weightInput
    var weightUnit by choices.weightUnit
    var weeklyGoalInput by choices.weeklyGoalInput
    var hapticEnabled by choices.hapticEnabled
    var swipeActionsEnabled by choices.swipeActionsEnabled
    var timerNotificationsEnabled by choices.timerNotificationsEnabled
    var gymMembershipExpiryNotificationsEnabled by choices.gymMembershipExpiryNotificationsEnabled
    var gymMembershipExpiryNotificationDaysBefore by choices.gymMembershipExpiryNotificationDaysBefore
    var timerFinishedLockscreenVibrationDuration by choices.timerFinishedLockscreenVibrationDuration
    var autoStopCardioAtTarget by choices.autoStopCardioAtTarget
    var autoStopTimeWeightAtTarget by choices.autoStopTimeWeightAtTarget
    var autoBackupEnabled by choices.autoBackupEnabled
    var autoBackupFrequency by choices.autoBackupFrequency
    var autoBackupFolderUri by choices.autoBackupFolderUri
    var autoBackupMaxCount by choices.autoBackupMaxCount
    var autoBackupIncludeImages by choices.autoBackupIncludeImages
    var aiScanEnabled by choices.aiScanEnabled
    var advancedProgramming by choices.advancedProgramming
    var nextcloudAutoBackupEnabled by choices.nextcloudAutoBackupEnabled
    var nextcloudAutoBackupFrequency by choices.nextcloudAutoBackupFrequency
    var nextcloudAutoBackupMaxCount by choices.nextcloudAutoBackupMaxCount
    var nextcloudAutoBackupIncludeImages by choices.nextcloudAutoBackupIncludeImages
    var nextcloudWifiOnly by choices.nextcloudWifiOnly
    val dynamicColorEnabled by model.dynamicColor.collectAsState(initial = true)
    val dynamicColorSeed by model.dynamicColorSeed.collectAsState(initial = null)
    val themePalette by model.themePalette.collectAsState(initial = 0)
    val themeStyle by model.themeStyle.collectAsState(initial = 0)
    val themeMode by model.themeMode.collectAsState(initial = 0)

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

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The pages end above the controls, so nothing ever scrolls underneath the indicator or the button
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        when (pages[page]) {
                            0 -> WelcomeSlide(logo)
                            1 -> FeaturesSlide()
                            2 -> ConnectivitySlide(automaticBackupsSupported)
                            3 -> ThemeSlide(
                                dynamicColor = dynamicColorEnabled,
                                onDynamicColorChange = { model.setDynamicColor(it) },
                                dynamicColorSeed = dynamicColorSeed,
                                onDynamicColorSeedChange = { model.setDynamicColorSeed(it) },
                                themePalette = themePalette,
                                onThemePaletteChange = { model.setThemePalette(it) },
                                themeStyle = themeStyle,
                                onThemeStyleChange = { model.setThemeStyle(it) },
                                themeMode = themeMode,
                                onThemeModeChange = { model.setThemeMode(it) },
                                wallpaperSupported = wallpaperSupported
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
                                customVibrationSupported = customVibrationSupported
                            )
                            5 -> WorkoutSlide(
                                swipeActionsEnabled = swipeActionsEnabled,
                                onSwipeActionsChange = { swipeActionsEnabled = it },
                                autoStopCardioAtTarget = autoStopCardioAtTarget,
                                onAutoStopCardioAtTargetChange = { autoStopCardioAtTarget = it },
                                autoStopTimeWeightAtTarget = autoStopTimeWeightAtTarget,
                                onAutoStopTimeWeightAtTargetChange = { autoStopTimeWeightAtTarget = it }
                            )
                            6 -> AdvancedSlide(
                                advancedProgramming = advancedProgramming,
                                onAdvancedProgrammingChange = { advancedProgramming = it },
                                aiSupported = 7 in pages
                            )
                            7 -> platformPage(7, choices)
                            8 -> platformPage(8, choices)
                            9 -> platformPage(9, choices)
                            10 -> ProfileSetupSlide(
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

                // Soft edge where the content is cut off, so it reads as "scroll for more"
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Surface)))
                )
            }

            // The keyboard covers the controls anyway: give the page the whole screen while typing
            AnimatedVisibility(visible = !isKeyboardVisible) {
                Column(
                    modifier = Modifier
                        .widthIn(max = OnboardingMetrics.maxContentWidth)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = OnboardingMetrics.horizontalPadding, vertical = 20.dp),
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
                                    if (completing) return@launch
                                    completing = true
                                    try {
                                        prepareCompletion(choices)
                                        val weight = weightInput.replace(',', '.').toFloatOrNull() ?: 0f
                                        val goal = weeklyGoalInput.toIntOrNull() ?: 3
                                        model.completeOnboarding(
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
                                            autoStopCardioAtTarget = autoStopCardioAtTarget,
                                            autoStopTimeWeightAtTarget = autoStopTimeWeightAtTarget,
                                            autoBackupEnabled = autoBackupEnabled,
                                            autoBackupFrequency = autoBackupFrequency,
                                            autoBackupFolderUri = autoBackupFolderUri,
                                            autoBackupMaxCount = autoBackupMaxCount,
                                            autoBackupIncludeImages = autoBackupIncludeImages,
                                            dynamicColor = dynamicColorEnabled,
                                            dynamicColorSeed = dynamicColorSeed,
                                            themePalette = themePalette,
                                            themeStyle = themeStyle,
                                            themeMode = themeMode,
                                            aiScanEnabled = aiScanEnabled,
                                            aiModelVariant = selectedAiModelVariant,
                                            advancedProgrammingEnabled = advancedProgramming,
                                            nextcloudAutoBackupEnabled = nextcloudAutoBackupEnabled,
                                            nextcloudAutoBackupFrequency = nextcloudAutoBackupFrequency,
                                            nextcloudAutoBackupMaxCount = nextcloudAutoBackupMaxCount,
                                            nextcloudAutoBackupIncludeImages = nextcloudAutoBackupIncludeImages,
                                            nextcloudWifiOnly = nextcloudWifiOnly
                                        )
                                        onFinished()
                                    } catch (error: kotlinx.coroutines.CancellationException) { throw error }
                                    catch (_: Exception) { showMessage(org.jetbrains.compose.resources.getString(Res.string.onboarding_save_failed)) }
                                    finally { completing = false }
                                } else {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = !completing,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = if (isLastPage) stringResource(Res.string.finish_setup) else stringResource(Res.string.continue_text),
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
    }
}

@Composable
private fun WelcomeSlide(logo: @Composable (Modifier, Color) -> Unit) {
    val cookieRotation = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            cookieRotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(
                    durationMillis = 20000,
                    easing = LinearEasing
                )
            )
            cookieRotation.snapTo(0f)
        }
    }
    val cookieShape = remember {
        object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val path = MaterialShapes.Cookie9Sided.toComposePath()
                val matrix = Matrix()
                matrix.scale(size.width, size.height)
                path.transform(matrix)
                path.translate(size.center - path.getBounds().center)
                return Outline.Generic(path)
            }
        }
    }

    val scale = OnboardingMetrics.artworkScale

    OnboardingPage(centered = true, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(190.dp * scale)
                    .graphicsLayer { rotationZ = cookieRotation.value }
                    .background(Primary.copy(alpha = 0.1f), cookieShape)
            )
            logo(Modifier.size(130.dp * scale), Primary)
        }

        Spacer(modifier = Modifier.height(48.dp * scale))

        Text(
            text = stringResource(Res.string.onboarding_welcome_title),
            style = MaterialTheme.typography.displayMedium,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            fontSize = OnboardingMetrics.welcomeTitleSize,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.onboarding_welcome_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
    }
}

@Composable
private fun FeaturesSlide() {
    OnboardingPage {
        OnboardingTitle(stringResource(Res.string.onboarding_features_title))

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            FeatureItemExpressive(
                icon = Icons.Rounded.Bolt,
                title = stringResource(Res.string.onboarding_feature_suggestions_title),
                desc = stringResource(Res.string.onboarding_feature_suggestions_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.DragHandle,
                title = stringResource(Res.string.onboarding_feature_control_title),
                desc = stringResource(Res.string.onboarding_feature_control_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.Analytics,
                title = stringResource(Res.string.onboarding_feature_insights_title),
                desc = stringResource(Res.string.onboarding_feature_insights_desc)
            )
        }
    }
}

@Composable
private fun ConnectivitySlide(automaticBackupsSupported: Boolean) {
    OnboardingPage {
        OnboardingTitle(stringResource(Res.string.onboarding_connectivity_title))

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            FeatureItemExpressive(
                icon = Icons.Rounded.Backup,
                title = stringResource(if (automaticBackupsSupported) Res.string.onboarding_connectivity_backup_title else Res.string.onboarding_manual_backup_title),
                desc = stringResource(if (automaticBackupsSupported) Res.string.onboarding_connectivity_backup_desc else Res.string.onboarding_manual_backup_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.Share,
                title = stringResource(Res.string.onboarding_connectivity_share_plans_title),
                desc = stringResource(Res.string.onboarding_connectivity_share_plans_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.IosShare,
                title = stringResource(Res.string.onboarding_connectivity_share_workouts_title),
                desc = stringResource(Res.string.onboarding_connectivity_share_workouts_desc)
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
    onThemeModeChange: (Int) -> Unit,
    wallpaperSupported: Boolean
) {
    var showCustomColorDialog by remember { mutableStateOf(false) }

    OnboardingPage {
        OnboardingTitle(stringResource(Res.string.onboarding_theme_title))

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
                if (showThemeModeDialog) {
                    AlertDialog(
                        onDismissRequest = { showThemeModeDialog = false },
                        containerColor = SurfaceContainerHigh,
                        title = { Text(stringResource(Res.string.theme_mode), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
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
                                    Text(stringResource(Res.string.theme_mode_system), style = MaterialTheme.typography.bodyLarge, color = OnSurface)
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
                                    Text(stringResource(Res.string.theme_mode_light), style = MaterialTheme.typography.bodyLarge, color = OnSurface)
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
                                    Text(stringResource(Res.string.theme_mode_dark), style = MaterialTheme.typography.bodyLarge, color = OnSurface)
                                    if (themeMode == 2) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Primary)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showThemeModeDialog = false }) {
                                Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
                            }
                        }
                    )
                }

                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
            }

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            if (wallpaperSupported) {
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
                            Text(stringResource(Res.string.dynamic_color), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                            Text(stringResource(Res.string.dynamic_color_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
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

            }

            if (wallpaperSupported && dynamicColor) {
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
                            Text(stringResource(Res.string.app_palette), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                            Text(stringResource(Res.string.app_palette_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
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
                                    colors = LocalPaletteColors.current.seed(seed, styleIndex),
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
                            Text(stringResource(Res.string.app_palette), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                            Text(stringResource(Res.string.app_palette_desc), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
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
                                    colors = LocalPaletteColors.current.palette(index),
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
                    val previewColors = LocalPaletteColors.current.seed(dynamicColorSeed, themeStyle)
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
    customVibrationSupported: Boolean
) {
    val haptic = LocalHapticFeedback.current

    OnboardingPage {
        OnboardingTitle(stringResource(Res.string.onboarding_notifications_title))

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Timer Notifications + Lockscreen Vibration Duration
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CustomizeToggleItem(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(Res.string.timer_notifications),
                    desc = stringResource(Res.string.timer_notifications_desc),
                    checked = timerNotificationsEnabled,
                    onCheckedChange = onTimerNotificationsChange
                )

                if (timerNotificationsEnabled && customVibrationSupported) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(Res.string.lockscreen_vibration),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (timerFinishedLockscreenVibrationDuration == 0) {
                                    stringResource(Res.string.vibration_default)
                                } else {
                                    stringResource(Res.string.vibration_seconds, timerFinishedLockscreenVibrationDuration)
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
                    title = stringResource(Res.string.gym_membership_notifications),
                    desc = stringResource(Res.string.gym_membership_notifications_desc),
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
                            stringResource(Res.string.notify_days_before),
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
                title = stringResource(Res.string.tactile_feedback),
                desc = stringResource(Res.string.tactile_feedback_desc),
                checked = hapticEnabled,
                onCheckedChange = onHapticChange
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.onboarding_setup_footer),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun WorkoutSlide(
    swipeActionsEnabled: Boolean,
    onSwipeActionsChange: (Boolean) -> Unit,
    autoStopCardioAtTarget: Boolean,
    onAutoStopCardioAtTargetChange: (Boolean) -> Unit,
    autoStopTimeWeightAtTarget: Boolean,
    onAutoStopTimeWeightAtTargetChange: (Boolean) -> Unit
) {
    OnboardingPage {
        OnboardingTitle(stringResource(Res.string.workout_settings))

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Swipe Actions
            CustomizeToggleItem(
                icon = Icons.Rounded.RestartAlt,
                title = stringResource(Res.string.swipe_actions),
                desc = stringResource(Res.string.swipe_actions_desc),
                checked = swipeActionsEnabled,
                onCheckedChange = onSwipeActionsChange
            )

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            // Auto-stop cardio at target
            CustomizeToggleItem(
                icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                title = stringResource(Res.string.auto_stop_cardio_at_target),
                desc = stringResource(Res.string.auto_stop_cardio_at_target_desc),
                checked = autoStopCardioAtTarget,
                onCheckedChange = onAutoStopCardioAtTargetChange
            )

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))

            // Auto-stop timed sets at target
            CustomizeToggleItem(
                icon = Icons.Rounded.FitnessCenter,
                title = stringResource(Res.string.auto_stop_time_weight_at_target),
                desc = stringResource(Res.string.auto_stop_time_weight_at_target_desc),
                checked = autoStopTimeWeightAtTarget,
                onCheckedChange = onAutoStopTimeWeightAtTargetChange
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.onboarding_setup_footer),
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

    OnboardingPage(verticalArrangement = Arrangement.Top) {
        OnboardingTitle(stringResource(Res.string.onboarding_setup_title))

        Spacer(modifier = Modifier.height(40.dp))

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            GymInputField(
                value = username,
                onValueChange = onUsernameChange,
                label = stringResource(Res.string.onboarding_setup_username_label),
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.onboarding_setup_weight_label),
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
                label = stringResource(Res.string.onboarding_setup_goal_label),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Text(
                        stringResource(Res.string.days_this_week),
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(Res.string.onboarding_setup_footer),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}

@Composable
fun FeatureItemExpressive(icon: ImageVector, title: String, desc: String) {
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
fun CustomizeToggleItem(
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
                        val previewColors = LocalPaletteColors.current.seed(seedArgb, selectedStyle)
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
                                colors = LocalPaletteColors.current.seed(seedArgb, styleIndex),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
/** What the advanced programming features are, with the switch that turns them on. */
@Composable
private fun AdvancedSlide(
    advancedProgramming: Boolean,
    onAdvancedProgrammingChange: (Boolean) -> Unit,
    aiSupported: Boolean
) {
    OnboardingPage {
        OnboardingTitle(stringResource(Res.string.onboarding_advanced_title))

        CustomizeToggleItem(
            icon = Icons.Rounded.Percent,
            title = stringResource(Res.string.advanced_programming),
            desc = stringResource(Res.string.advanced_programming_desc),
            checked = advancedProgramming,
            onCheckedChange = onAdvancedProgrammingChange
        )

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            FeatureItemExpressive(
                icon = Icons.Rounded.EmojiEvents,
                title = stringResource(Res.string.onboarding_advanced_percent_title),
                desc = stringResource(Res.string.onboarding_advanced_percent_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.Tune,
                title = stringResource(Res.string.onboarding_advanced_techniques_title),
                desc = stringResource(Res.string.onboarding_advanced_techniques_desc)
            )
            FeatureItemExpressive(
                icon = Icons.Rounded.CalendarMonth,
                title = stringResource(Res.string.onboarding_advanced_weeks_title),
                desc = stringResource(Res.string.onboarding_advanced_weeks_desc)
            )
            if (aiSupported) {
                FeatureItemExpressive(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(Res.string.onboarding_advanced_scan_title),
                    desc = stringResource(Res.string.onboarding_advanced_scan_desc)
                )
            }
        }

        Text(
            text = stringResource(Res.string.onboarding_setup_footer),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )

    }
}
