package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.components.isKeyboardVisible
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.util.PlatformBackHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The first-launch setup: Welcome, You, Style, Tweaks, Look, Alerts, AI (when the phone runs the model and advanced
 * programming is on), Your data, Ready. Theme, navigation bar and glass are written as the user picks them; every
 * other choice is kept in [choices] and written once by [OnboardingModel.completeOnboarding] at the end.
 */
@Composable
fun OnboardingContent(
    model: OnboardingModel,
    onFinished: () -> Unit,
    choices: OnboardingChoices,
    /** Where the scan runs: providers, plus the on-device model when this phone can run it. */
    ai: OnboardingAi,
    selectedAiModelVariant: String,
    languageLabel: String,
    onPickLanguage: () -> Unit,
    restore: OnboardingRestore,
    nextcloudConnected: Boolean,
    dataLocal: @Composable () -> Unit,
    dataNextcloud: @Composable () -> Unit,
    wallpaperSupported: Boolean = true,
    customVibrationSupported: Boolean = true,
    logo: @Composable (Modifier, Color) -> Unit = { modifier, tint ->
        Icon(painterResource(Res.drawable.ic_app_logo), null, modifier, tint = tint)
    },
    prepareCompletion: suspend (OnboardingChoices) -> Unit = {},
    /** Called when the user moves on from the Alerts step; the platform may ask for its permission here. */
    onNotificationsPageLeft: (timerEnabled: Boolean, membershipEnabled: Boolean) -> Unit = { _, _ -> }
) {
    val steps = onboardingSteps()
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    val showMessage = LocalShowMessage.current
    var completing by remember { mutableStateOf(false) }

    val dynamicColor by model.dynamicColor.collectAsState(initial = true)
    val dynamicColorSeed by model.dynamicColorSeed.collectAsState(initial = null)
    val themePalette by model.themePalette.collectAsState(initial = 0)
    val themeStyle by model.themeStyle.collectAsState(initial = 0)
    val themeMode by model.themeMode.collectAsState(initial = 0)

    val currentStep = steps.getOrElse(pagerState.currentPage) { OnboardingStep.Welcome }
    val goTo: (OnboardingStep) -> Unit = { step ->
        val index = steps.indexOf(step)
        if (index >= 0) scope.launch { pagerState.animateScrollToPage(index) }
    }
    val back: () -> Unit = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) } }

    // Back goes to the previous step instead of leaving the app
    PlatformBackHandler(enabled = pagerState.currentPage > 0) { back() }

    // Moving forward from Alerts, by the button or by a swipe: tracked by step, since the AI step can come and go
    val notificationsPageLeft by rememberUpdatedState(onNotificationsPageLeft)
    val latestSteps by rememberUpdatedState(steps)
    LaunchedEffect(pagerState) {
        var previous = latestSteps.getOrNull(pagerState.currentPage)
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val now = latestSteps.getOrNull(page)
            if (previous == OnboardingStep.Alerts && now != null && latestSteps.indexOf(now) > latestSteps.indexOf(OnboardingStep.Alerts)) {
                notificationsPageLeft(choices.timerNotificationsEnabled.value, choices.gymMembershipExpiryNotificationsEnabled.value)
            }
            previous = now
        }
    }

    val finish: () -> Unit = {
        scope.launch {
            if (completing) return@launch
            completing = true
            try {
                prepareCompletion(choices)
                model.completeOnboarding(
                    choices.toSettings(ThemeChoice(dynamicColor, dynamicColorSeed, themePalette, themeStyle, themeMode), selectedAiModelVariant)
                )
                onFinished()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showMessage(getString(Res.string.onboarding_save_failed))
            } finally {
                completing = false
            }
        }
    }

    val themeLabel = when (themeMode) {
        1 -> stringResource(Res.string.theme_mode_light)
        2 -> stringResource(Res.string.theme_mode_dark)
        else -> stringResource(Res.string.theme_mode_system)
    }

    Box(modifier = Modifier.fillMaxSize().background(Surface)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Primary.copy(alpha = 0.15f), Surface, Surface)))
        )

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Fixed above the pages: back and progress (not on Welcome)
            AnimatedVisibility(visible = currentStep != OnboardingStep.Welcome) {
                OnboardingTopRow(
                    progress = pagerState.currentPage / (steps.size - 1).coerceAtLeast(1).toFloat(),
                    onBack = back,
                    modifier = Modifier
                        .statusBarsPadding()
                        .widthIn(max = OnboardingMetrics.maxContentWidth)
                        .padding(horizontal = OnboardingMetrics.horizontalPadding - 8.dp, vertical = 8.dp)
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                    val step = steps[page]
                    val active = pagerState.currentPage == page
                    val hero: @Composable () -> Unit = {
                        OnboardingHeroShape(stepIndex = page, active = active, done = step == OnboardingStep.Ready) {
                            HeroNumber(page)
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        when (step) {
                            OnboardingStep.Welcome -> WelcomeStep(
                                logo, languageLabel, onPickLanguage, onStart = { goTo(OnboardingStep.You) }, restore = restore,
                                onConnectedForRestore = { choices.connectedForRestoreOnly() }
                            )
                            OnboardingStep.You -> ProfileStep(choices, hero)
                            OnboardingStep.Style -> StyleStep(choices, hero)
                            OnboardingStep.Tweaks -> TweaksStep(choices, hero)
                            OnboardingStep.Look -> LookStep(model, wallpaperSupported, hero)
                            OnboardingStep.Alerts -> AlertsStep(choices, customVibrationSupported, hero)
                            OnboardingStep.Ai -> AiStep(choices, ai, hero)
                            OnboardingStep.Data -> DataStep(choices.autoBackupEnabled.value, nextcloudConnected, dataLocal, dataNextcloud, hero)
                            OnboardingStep.Ready -> ReadyStep(choices, themeLabel, goTo, hero)
                        }
                    }
                }

                // Soft edge where the content is cut off, so it reads as "scroll for more"
                if (currentStep != OnboardingStep.Welcome) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Surface)))
                    )
                }
            }

            // The keyboard covers the button anyway: give the step the whole screen while typing
            AnimatedVisibility(visible = !isKeyboardVisible && currentStep != OnboardingStep.Welcome) {
                OnboardingNextButton(
                    isLast = currentStep == OnboardingStep.Ready,
                    enabled = !completing,
                    onClick = {
                        if (currentStep == OnboardingStep.Ready) finish()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier
                        .widthIn(max = OnboardingMetrics.maxContentWidth)
                        .navigationBarsPadding()
                        .padding(horizontal = OnboardingMetrics.horizontalPadding, vertical = 16.dp)
                )
            }
        }
    }
}
