package com.emanuel5014.trainable.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import com.emanuel5014.trainable.data.ai.AiModelStatus
import com.emanuel5014.trainable.data.ai.AiModelVariant
import com.emanuel5014.trainable.data.ai.DeviceCapabilityChecker
import com.emanuel5014.trainable.data.ai.ModelDownloadManager
import com.emanuel5014.trainable.data.ai.ModelFileManager
import com.emanuel5014.trainable.data.local.entity.UserEntity
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.util.backup.AutoBackupWorker
import com.emanuel5014.trainable.util.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPrefsRepository: UserPreferencesRepository,
    private val backupManager: BackupManager,
    private val deviceCapabilityChecker: DeviceCapabilityChecker,
    private val modelFileManager: ModelFileManager,
    private val modelDownloadManager: ModelDownloadManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    val dynamicColor = userPrefsRepository.dynamicColor
    val dynamicColorSeed = userPrefsRepository.dynamicColorSeed
    val themePalette = userPrefsRepository.themePalette
    val themeStyle = userPrefsRepository.themeStyle
    val themeMode = userPrefsRepository.themeMode

    val aiScanEnabled = userPrefsRepository.aiScanEnabled
    val aiModelVariant = userPrefsRepository.aiModelVariant.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "e2b"
    )
    val aiDeviceSupported: Boolean = deviceCapabilityChecker.isSupported(context)

    val aiModelStatus: StateFlow<AiModelStatus> = combine(
        userPrefsRepository.aiModelVariant,
        modelDownloadManager.activeDownloads,
        modelFileManager.filesUpdatedTrigger
    ) { variantId, downloads, _ ->
        val variant = AiModelVariant.fromId(variantId)
        val ongoing = downloads[variant.id]
        when {
            ongoing != null -> ongoing
            modelFileManager.isDownloaded(variant) -> AiModelStatus.Ready
            else -> AiModelStatus.NotDownloaded
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiModelStatus.NotDownloaded
    )

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setDynamicColor(enabled)
        }
    }

    fun setDynamicColorSeed(seed: Int?) {
        viewModelScope.launch {
            userPrefsRepository.setDynamicColorSeed(seed)
        }
    }

    fun setThemePalette(index: Int) {
        viewModelScope.launch {
            userPrefsRepository.setThemePalette(index)
        }
    }

    fun setThemeStyle(index: Int) {
        viewModelScope.launch {
            userPrefsRepository.setThemeStyle(index)
        }
    }

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            userPrefsRepository.setThemeMode(mode)
        }
    }

    fun setAiScanEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setAiScanEnabled(enabled)
        }
    }

    fun setAiModelVariant(variantId: String) {
        viewModelScope.launch {
            userPrefsRepository.setAiModelVariant(variantId)
        }
    }

    fun downloadAiModel() {
        val variant = AiModelVariant.fromId(aiModelVariant.value)
        modelDownloadManager.startDownload(variant)
    }

    fun cancelAiModelDownload() {
        val variant = AiModelVariant.fromId(aiModelVariant.value)
        modelDownloadManager.cancelDownload(variant)
    }

    fun clearStatus() {
        _backupStatus.value = null
    }

    fun exportDatabase(uri: Uri, includeImages: Boolean) {
        viewModelScope.launch {
            val success = backupManager.exportDatabaseZip(uri, includeImages)
            _backupStatus.value = if (success) "Export successful" else "Export failed"
        }
    }

    fun importDatabase(uri: Uri, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = backupManager.importDatabaseZip(uri)
            if (success) {
                userPrefsRepository.setOnboardingCompleted(true)
                _backupStatus.value = "Import successful. Restarting..."
                onSuccess()
            } else {
                _backupStatus.value = "Import failed"
            }
        }
    }

    fun persistFolderUri(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
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
        autoStopCardioAtTarget: Boolean = false,
        autoStopTimeWeightAtTarget: Boolean = false,
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
        aiModelVariant: String = "e2b"
    ) {
        val existingUser = userRepository.currentUser.firstOrNull()

        val user = existingUser?.copy(
            username = username.ifBlank { "Athlete" }
        ) ?: UserEntity(
            id = 1,
            username = username.ifBlank { "Athlete" },
            dataIscrizione = System.currentTimeMillis()
        )

        userRepository.saveUser(user)

        if (initialWeight > 0f) {
            userRepository.addWeightLog(
                peso = initialWeight,
                data = System.currentTimeMillis(),
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
        userPrefsRepository.setOnboardingCompleted(true)

        if (autoBackupEnabled) {
            AutoBackupWorker.schedule(
                context = context,
                frequencyDays = autoBackupFrequency,
                policy = ExistingPeriodicWorkPolicy.UPDATE
            )
        }
    }
}
