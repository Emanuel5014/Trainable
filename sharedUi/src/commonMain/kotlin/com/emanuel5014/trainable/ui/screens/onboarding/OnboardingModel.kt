package com.emanuel5014.trainable.ui.screens.onboarding

import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import com.emanuel5014.trainable.data.model.NavBarStyle
import kotlin.time.Clock

/** Profile/settings writes shared by both platforms. Android owns its backup scheduler. */
class OnboardingModel(
    private val userRepository: UserRepository,
    private val userPrefsRepository: UserPreferencesRepository,
    private val scope: CoroutineScope,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val onCompleted: suspend () -> Unit = {}
) {
    val dynamicColor = userPrefsRepository.dynamicColor
    val dynamicColorSeed = userPrefsRepository.dynamicColorSeed
    val themePalette = userPrefsRepository.themePalette
    val themeStyle = userPrefsRepository.themeStyle
    val themeMode = userPrefsRepository.themeMode

    fun setDynamicColor(enabled: Boolean) {
        scope.launch {
            userPrefsRepository.setDynamicColor(enabled)
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

    fun setThemeMode(mode: Int) {
        scope.launch {
            userPrefsRepository.setThemeMode(mode)
        }
    }

    val navBarStyle = userPrefsRepository.navBarStyle
    val liquidGlassEnabled = userPrefsRepository.glassSettings.map { it.enabled }

    /** Written at once, like the theme, so the Look preview and the setup itself follow the choice. */
    fun setNavBarStyle(style: NavBarStyle) {
        scope.launch { userPrefsRepository.setNavBarStyle(style) }
    }

    fun setLiquidGlassEnabled(enabled: Boolean) {
        scope.launch { userPrefsRepository.setLiquidGlassEnabled(enabled) }
    }

    suspend fun completeOnboarding(settings: OnboardingSettings) = with(settings) {
        val existingUser = userRepository.currentUser.firstOrNull()

        val user = existingUser?.copy(
            username = username.ifBlank { "Athlete" }
        ) ?: UserEntity(
            id = 1,
            username = username.ifBlank { "Athlete" },
            dataIscrizione = nowMillis()
        )

        userRepository.saveUser(user)

        if (initialWeight > 0f) {
            userRepository.addWeightLog(
                peso = initialWeight,
                data = nowMillis(),
                userId = user.id
            )
        }

        userPrefsRepository.setWeightUnit(weightUnit)
        // Pounds go with miles, kilograms with km (no control of its own: Settings changes it)
        userPrefsRepository.setDistanceUnit(if (weightUnit == "lb") "mi" else "km")
        userPrefsRepository.setWeeklyGoal(if (weeklyGoal > 0) weeklyGoal else 3)
        userPrefsRepository.setHapticEnabled(hapticEnabled)
        userPrefsRepository.setTimerFinishedLockscreenVibrationDuration(timerFinishedLockscreenVibrationDuration)
        userPrefsRepository.setAutoStopCardioAtTarget(autoStopCardioAtTarget)
        userPrefsRepository.setAutoStopTimeWeightAtTarget(autoStopTimeWeightAtTarget)
        userPrefsRepository.setSwipeActionsEnabled(swipeActionsEnabled)
        userPrefsRepository.setTimerNotificationsEnabled(timerNotificationsEnabled)
        userPrefsRepository.setGymMembershipExpiryNotificationsEnabled(gymMembershipExpiryNotificationsEnabled)
        userPrefsRepository.setGymMembershipExpiryNotificationDaysBefore(gymMembershipExpiryNotificationDaysBefore)
        userPrefsRepository.setAutoBackupEnabled(autoBackupEnabled)
        userPrefsRepository.setAutoBackupFrequency(autoBackupFrequency)
        if (autoBackupFolderUri != null) {
            userPrefsRepository.setAutoBackupFolderUri(autoBackupFolderUri)
        }
        userPrefsRepository.setAutoBackupMaxCount(autoBackupMaxCount)
        userPrefsRepository.setAutoBackupIncludeImages(autoBackupIncludeImages)
        userPrefsRepository.setDynamicColor(theme.dynamicColor)
        userPrefsRepository.setThemePalette(theme.palette)
        if (theme.dynamicColorSeed != null) {
            userPrefsRepository.setDynamicColorSeed(theme.dynamicColorSeed)
        }
        userPrefsRepository.setThemeStyle(theme.style)
        userPrefsRepository.setThemeMode(theme.mode)
        userPrefsRepository.setAiScanEnabled(aiScanEnabled)
        userPrefsRepository.setAiModelVariant(aiModelVariant)
        userPrefsRepository.setAdvancedProgrammingEnabled(advancedProgramming)

        // Program, timers, exercises and journal: one preference per Tweaks row
        userPrefsRepository.setRpeEnabled(rpeEnabled)
        userPrefsRepository.setRpeScale(rpeScale)
        userPrefsRepository.setStartingWeightEnabled(startingWeight)
        userPrefsRepository.setSetPerformanceEnabled(setPerformance)
        userPrefsRepository.setInlineExerciseModificationsEnabled(inlineModifications)
        userPrefsRepository.setLoadRoundingIncrement(weightUnit, loadRounding)
        userPrefsRepository.setRestTimerEnabled(restTimerEnabled)
        userPrefsRepository.setWarmupTimerEnabled(warmupTimerEnabled)
        userPrefsRepository.setWorkoutTimerEnabled(workoutTimerEnabled)
        userPrefsRepository.setTimerAddEnabled(timerAddEnabled)
        userPrefsRepository.setTimerAddSeconds(timerAddSeconds)
        userPrefsRepository.setTimerSubtractEnabled(timerSubtractEnabled)
        userPrefsRepository.setTimerSubtractSeconds(timerSubtractSeconds)
        userPrefsRepository.setKeepScreenOnSetTimer(keepScreenOnSetTimer)
        userPrefsRepository.setKeepScreenOnCardioTimer(keepScreenOnCardioTimer)
        userPrefsRepository.setExerciseMediaEnabled(exerciseMedia)
        userPrefsRepository.setExerciseMediaLarge(exerciseMediaLarge)
        userPrefsRepository.setPlateCalculatorEnabled(plateCalculator)
        userPrefsRepository.setEditablePresetExercises(editablePresets)
        userPrefsRepository.setJournalEnabled(journalEnabled)
        userPrefsRepository.setJournalPromptAtFinish(journalPromptAtFinish)
        userPrefsRepository.setJournalRatingEnabled(journalRating)
        if (membershipExpiryDate != null) {
            userPrefsRepository.setGymMembershipExpiryDate(membershipExpiryDate)
        }

        // The Nextcloud options only mean something once an account is connected
        val nextcloudConnected = userPrefsRepository.nextcloudServerUrl.firstOrNull() != null
        if (nextcloudConnected) {
            userPrefsRepository.setNextcloudAutoBackupFrequency(nextcloudAutoBackupFrequency)
            userPrefsRepository.setNextcloudAutoBackupMaxCount(nextcloudAutoBackupMaxCount)
            userPrefsRepository.setNextcloudAutoBackupIncludeImages(nextcloudAutoBackupIncludeImages)
            userPrefsRepository.setNextcloudWifiOnly(nextcloudWifiOnly)
            userPrefsRepository.setNextcloudAutoBackupEnabled(nextcloudAutoBackupEnabled)
        }
        userPrefsRepository.setOnboardingCompleted(true)

        // One job serves both destinations (local folder and Nextcloud)
        onCompleted()
    }

}
