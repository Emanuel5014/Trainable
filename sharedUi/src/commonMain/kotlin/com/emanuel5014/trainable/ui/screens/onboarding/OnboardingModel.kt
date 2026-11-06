package com.emanuel5014.trainable.ui.screens.onboarding

import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
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

    suspend fun completeOnboarding(
        username: String,
        initialWeight: Float,
        weeklyGoal: Int,
        weightUnit: String,
        hapticEnabled: Boolean = true,
        swipeActionsEnabled: Boolean = true,
        timerNotificationsEnabled: Boolean = true,
        gymMembershipExpiryNotificationsEnabled: Boolean = false,
        gymMembershipExpiryNotificationDaysBefore: Int = 3,
        timerFinishedLockscreenVibrationDuration: Int = 30,
        autoStopCardioAtTarget: Boolean = true,
        autoStopTimeWeightAtTarget: Boolean = true,
        autoBackupEnabled: Boolean = false,
        autoBackupFrequency: Int = 1,
        autoBackupFolderUri: String? = null,
        autoBackupMaxCount: Int = 5,
        autoBackupIncludeImages: Boolean = false,
        dynamicColor: Boolean = true,
        dynamicColorSeed: Int? = null,
        themePalette: Int = 0,
        themeStyle: Int = 0,
        themeMode: Int = 0,
        aiScanEnabled: Boolean = false,
        aiModelVariant: String = "e2b",
        advancedProgrammingEnabled: Boolean = false,
        nextcloudAutoBackupEnabled: Boolean = false,
        nextcloudAutoBackupFrequency: Int = 1,
        nextcloudAutoBackupMaxCount: Int = 5,
        nextcloudAutoBackupIncludeImages: Boolean = false,
        nextcloudWifiOnly: Boolean = false
    ) {
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
        userPrefsRepository.setDynamicColor(dynamicColor)
        userPrefsRepository.setThemePalette(themePalette)
        if (dynamicColorSeed != null) {
            userPrefsRepository.setDynamicColorSeed(dynamicColorSeed)
        }
        userPrefsRepository.setThemeStyle(themeStyle)
        userPrefsRepository.setThemeMode(themeMode)
        userPrefsRepository.setAiScanEnabled(aiScanEnabled)
        userPrefsRepository.setAiModelVariant(aiModelVariant)
        userPrefsRepository.setAdvancedProgrammingEnabled(advancedProgrammingEnabled)

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
