package com.emanuel5014.trainable.ui.screens.settings

import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.data.model.GlassSettings
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.util.PlateCalculator
import com.emanuel5014.trainable.util.TimerAdjustment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The settings stored on either device; platform backup, language and notification services stay in their app. */
class SettingsState(
    private val userRepository: UserRepository,
    private val userPrefsRepository: UserPreferencesRepository,
    private val scope: CoroutineScope
) {
    val currentUser = userRepository.currentUser.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val weeklyGoal = userPrefsRepository.weeklyGoal.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 3
    )

    val hapticEnabled = userPrefsRepository.hapticEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val autoBackupEnabled = userPrefsRepository.autoBackupEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val autoBackupFrequency = userPrefsRepository.autoBackupFrequency.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1
    )

    val autoBackupMaxCount = userPrefsRepository.autoBackupMaxCount.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 5
    )

    val autoBackupIncludeImages = userPrefsRepository.autoBackupIncludeImages.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val nextcloudBackupEnabled = userPrefsRepository.nextcloudBackupEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val nextcloudAutoBackupEnabled = userPrefsRepository.nextcloudAutoBackupEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val nextcloudServerUrl = userPrefsRepository.nextcloudServerUrl.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val nextcloudUsername = userPrefsRepository.nextcloudUsername.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val nextcloudRemoteFolder = userPrefsRepository.nextcloudRemoteFolder.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Trainable/Backups"
    )

    val nextcloudWifiOnly = userPrefsRepository.nextcloudWifiOnly.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val nextcloudAutoBackupFrequency = userPrefsRepository.nextcloudAutoBackupFrequency.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1
    )

    val nextcloudAutoBackupMaxCount = userPrefsRepository.nextcloudAutoBackupMaxCount.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 5
    )

    val nextcloudAutoBackupIncludeImages = userPrefsRepository.nextcloudAutoBackupIncludeImages.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val navBarStyle = userPrefsRepository.navBarStyle.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NavBarStyle.Floating
    )

    val dynamicColor = userPrefsRepository.dynamicColor.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val dynamicColorSeed = userPrefsRepository.dynamicColorSeed.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val themePalette = userPrefsRepository.themePalette.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val themeStyle = userPrefsRepository.themeStyle.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val weightUnit = userPrefsRepository.weightUnit.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "kg"
    )

    val timerNotificationsEnabled = userPrefsRepository.timerNotificationsEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val timerFinishedLockscreenVibrationDuration = userPrefsRepository.timerFinishedLockscreenVibrationDuration.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val gymMembershipExpiryNotificationsEnabled = userPrefsRepository.gymMembershipExpiryNotificationsEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val gymMembershipExpiryNotificationDaysBefore = userPrefsRepository.gymMembershipExpiryNotificationDaysBefore.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 3
    )

    val themeMode = userPrefsRepository.themeMode.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val swipeActionsEnabled = userPrefsRepository.swipeActionsEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val warmupTimerEnabled = userPrefsRepository.warmupTimerEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val workoutTimerEnabled = userPrefsRepository.workoutTimerEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val inlineExerciseModificationsEnabled = userPrefsRepository.inlineExerciseModificationsEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val autoStopCardioAtTarget = userPrefsRepository.autoStopCardioAtTarget.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val advancedProgrammingEnabled = userPrefsRepository.advancedProgrammingEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val timerAddSeconds = userPrefsRepository.timerAddSeconds.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerAdjustment.DEFAULT_ADD_SECONDS
    )

    val timerSubtractSeconds = userPrefsRepository.timerSubtractSeconds.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerAdjustment.DEFAULT_SUBTRACT_SECONDS
    )

    val restTimerEnabled = userPrefsRepository.restTimerEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val timerAddEnabled = userPrefsRepository.timerAddEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val timerSubtractEnabled = userPrefsRepository.timerSubtractEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val timerShowTimeButtons = userPrefsRepository.timerShowTimeButtons.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val timerShowSkipButton = userPrefsRepository.timerShowSkipButton.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val startingWeightEnabled = userPrefsRepository.startingWeightEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val setPerformanceEnabled = userPrefsRepository.setPerformanceEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val journalEnabled = userPrefsRepository.journalEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val journalPromptAtFinish = userPrefsRepository.journalPromptAtFinish.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val journalRatingEnabled = userPrefsRepository.journalRatingEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val plateCalculatorEnabled = userPrefsRepository.plateCalculatorEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val plateCalculatorPlates = userPrefsRepository.plateCalculatorPlates.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlateCalculator.DEFAULT_PLATES_KG
    )

    val loadRoundingIncrement = userPrefsRepository.loadRoundingIncrement.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.emanuel5014.trainable.domain.prescription.LoadCalculator.DEFAULT_INCREMENT_KG
    )

    val rpeEnabled = userPrefsRepository.rpeEnabled.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val rpeScale = userPrefsRepository.rpeScale.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RpeScale.BASIC
    )

    val autoStopTimeWeightAtTarget = userPrefsRepository.autoStopTimeWeightAtTarget.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val keepScreenOnCardioTimer = userPrefsRepository.keepScreenOnCardioTimer.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val keepScreenOnSetTimer = userPrefsRepository.keepScreenOnSetTimer.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val glassSettings = userPrefsRepository.glassSettings.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GlassSettings()
    )

    fun setAdvancedProgrammingEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setAdvancedProgrammingEnabled(enabled)
        }
    }

    fun setTimerAddSeconds(seconds: Int) {
        scope.launch {
            userPrefsRepository.setTimerAddSeconds(seconds)
        }
    }

    fun setTimerSubtractSeconds(seconds: Int) {
        scope.launch {
            userPrefsRepository.setTimerSubtractSeconds(seconds)
        }
    }

    fun setRestTimerEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setRestTimerEnabled(enabled)
        }
    }

    fun setTimerAddEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setTimerAddEnabled(enabled)
        }
    }

    fun setTimerSubtractEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setTimerSubtractEnabled(enabled)
        }
    }

    fun setTimerShowTimeButtons(show: Boolean) {
        scope.launch {
            userPrefsRepository.setTimerShowTimeButtons(show)
        }
    }

    fun setTimerShowSkipButton(show: Boolean) {
        scope.launch {
            userPrefsRepository.setTimerShowSkipButton(show)
        }
    }

    fun setStartingWeightEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setStartingWeightEnabled(enabled)
        }
    }

    fun setSetPerformanceEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setSetPerformanceEnabled(enabled)
        }
    }

    fun setJournalEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setJournalEnabled(enabled)
        }
    }

    fun setJournalPromptAtFinish(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setJournalPromptAtFinish(enabled)
        }
    }

    fun setJournalRatingEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setJournalRatingEnabled(enabled)
        }
    }

    fun setPlateCalculatorEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setPlateCalculatorEnabled(enabled)
        }
    }

    fun setPlateCalculatorPlates(plates: List<Float>) {
        scope.launch {
            userPrefsRepository.setPlateCalculatorPlates(weightUnit.value, plates)
        }
    }

    fun setLoadRoundingIncrement(increment: Float) {
        scope.launch {
            userPrefsRepository.setLoadRoundingIncrement(weightUnit.value, increment)
        }
    }

    fun setRpeEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setRpeEnabled(enabled)
        }
    }

    fun setRpeScale(scale: RpeScale) {
        scope.launch {
            userPrefsRepository.setRpeScale(scale)
        }
    }

    fun setWeeklyGoal(goal: Int) {
        scope.launch {
            userPrefsRepository.setWeeklyGoal(goal)
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setHapticEnabled(enabled)
        }
    }

    fun setAutoBackupMaxCount(count: Int) {
        scope.launch {
            userPrefsRepository.setAutoBackupMaxCount(count)
        }
    }

    fun setAutoBackupIncludeImages(include: Boolean) {
        scope.launch {
            userPrefsRepository.setAutoBackupIncludeImages(include)
        }
    }

    fun setNavBarStyle(style: NavBarStyle) {
        scope.launch {
            userPrefsRepository.setNavBarStyle(style)
        }
    }

    fun setLiquidGlassEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setLiquidGlassEnabled(enabled)
        }
    }

    fun setGlassStretch(on: Boolean) {
        scope.launch {
            userPrefsRepository.setGlassStretch(on)
        }
    }

    fun setGlassJiggle(on: Boolean) {
        scope.launch {
            userPrefsRepository.setGlassJiggle(on)
        }
    }

    fun setGlassBlur(blur: Float) {
        scope.launch {
            userPrefsRepository.setGlassBlur(blur)
        }
    }

    fun setGlassIntensity(intensity: Float) {
        scope.launch {
            userPrefsRepository.setGlassIntensity(intensity)
        }
    }

    fun setGlassPart(part: GlassPart, on: Boolean) {
        scope.launch {
            userPrefsRepository.setGlassPart(part, on)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setDynamicColor(enabled)
            if (!enabled) {
                userPrefsRepository.setDynamicColorSeed(null)
            }
        }
    }

    fun setDynamicColorSeed(seed: Int?) {
        scope.launch {
            userPrefsRepository.setDynamicColorSeed(seed)
        }
    }

    fun setThemePalette(index: Int) {
        scope.launch {
            userPrefsRepository.setThemePalette(index)
        }
    }

    fun setThemeStyle(index: Int) {
        scope.launch {
            userPrefsRepository.setThemeStyle(index)
        }
    }

    fun setTimerNotificationsEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setTimerNotificationsEnabled(enabled)
        }
    }

    fun setTimerFinishedLockscreenVibrationDuration(durationSeconds: Int) {
        scope.launch {
            userPrefsRepository.setTimerFinishedLockscreenVibrationDuration(durationSeconds)
        }
    }

    fun setSwipeActionsEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setSwipeActionsEnabled(enabled)
        }
    }

    fun setWarmupTimerEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setWarmupTimerEnabled(enabled)
        }
    }

    fun setWorkoutTimerEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setWorkoutTimerEnabled(enabled)
        }
    }

    fun setInlineExerciseModificationsEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setInlineExerciseModificationsEnabled(enabled)
        }
    }

    fun setAutoStopCardioAtTarget(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setAutoStopCardioAtTarget(enabled)
        }
    }

    fun setAutoStopTimeWeightAtTarget(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setAutoStopTimeWeightAtTarget(enabled)
        }
    }

    fun setKeepScreenOnCardioTimer(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setKeepScreenOnCardioTimer(enabled)
        }
    }

    fun setKeepScreenOnSetTimer(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setKeepScreenOnSetTimer(enabled)
        }
    }

    fun setThemeMode(mode: Int) {
        scope.launch {
            userPrefsRepository.setThemeMode(mode)
        }
    }

    fun setWeightUnit(unit: String) {
        scope.launch {
            userPrefsRepository.setWeightUnit(unit)
        }
    }

    fun setAiScanEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setAiScanEnabled(enabled)
        }
    }

    fun setAiResourceAnalyticsEnabled(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setAiResourceAnalyticsEnabled(enabled)
        }
    }

    fun setAiModelVariant(variantId: String) {
        scope.launch {
            userPrefsRepository.setAiModelVariant(variantId)
        }
    }

    fun updateUsername(newUsername: String) {
        scope.launch {
            currentUser.value?.let { user ->
                userRepository.updateUser(user.copy(username = newUsername))
            }
        }
    }
}
