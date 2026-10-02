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
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConfig
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConnectionResult
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudWebDavClient
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.util.NextcloudCryptoManager
import com.emanuel5014.trainable.util.backup.AutoBackupWorker
import com.emanuel5014.trainable.util.backup.BackupManager
import com.emanuel5014.trainable.util.backup.NextcloudBackupManager
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
    private val nextcloudWebDavClient: NextcloudWebDavClient,
    private val nextcloudCryptoManager: NextcloudCryptoManager,
    private val nextcloudBackupManager: NextcloudBackupManager,
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

    // ---- Nextcloud ---------------------------------------------------------------------------

    val nextcloudServerUrl: StateFlow<String?> = userPrefsRepository.nextcloudServerUrl.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null
    )
    val nextcloudUsername: StateFlow<String?> = userPrefsRepository.nextcloudUsername.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null
    )

    private val _isConnectingNextcloud = MutableStateFlow(false)
    val isConnectingNextcloud: StateFlow<Boolean> = _isConnectingNextcloud.asStateFlow()

    private val _nextcloudConnectState = MutableStateFlow<NextcloudConnectionResult?>(null)
    val nextcloudConnectState: StateFlow<NextcloudConnectionResult?> = _nextcloudConnectState.asStateFlow()

    private val _nextcloudBackups = MutableStateFlow<List<NextcloudBackupFile>>(emptyList())
    val nextcloudBackups: StateFlow<List<NextcloudBackupFile>> = _nextcloudBackups.asStateFlow()

    private val _isLoadingNextcloudBackups = MutableStateFlow(false)
    val isLoadingNextcloudBackups: StateFlow<Boolean> = _isLoadingNextcloudBackups.asStateFlow()

    private val _isNextcloudRestoring = MutableStateFlow(false)
    val isNextcloudRestoring: StateFlow<Boolean> = _isNextcloudRestoring.asStateFlow()

    /** Tests the server and, if it answers, keeps the (encrypted) credentials: one step instead of test + save. */
    fun connectNextcloud(serverUrl: String, username: String, passwordOrToken: String, remoteFolder: String) {
        viewModelScope.launch {
            _isConnectingNextcloud.value = true
            _nextcloudConnectState.value = null
            val result = nextcloudWebDavClient.testConnection(
                NextcloudConfig(serverUrl = serverUrl.trim(), username = username.trim(), passwordOrToken = passwordOrToken.trim())
            )
            if (result is NextcloudConnectionResult.Success) {
                val (encrypted, iv) = nextcloudCryptoManager.encryptPassword(passwordOrToken.trim())
                userPrefsRepository.setNextcloudConfig(
                    serverUrl = serverUrl.trim(),
                    username = username.trim(),
                    encryptedPasswordHex = encrypted.toHex(),
                    passwordIvHex = iv.toHex(),
                    remoteFolder = remoteFolder.trim().ifEmpty { DEFAULT_NEXTCLOUD_FOLDER }
                )
            }
            _nextcloudConnectState.value = result
            _isConnectingNextcloud.value = false
        }
    }

    fun disconnectNextcloud() {
        viewModelScope.launch {
            userPrefsRepository.clearNextcloudConfig()
            _nextcloudBackups.value = emptyList()
            _nextcloudConnectState.value = null
        }
    }

    fun loadNextcloudBackups() {
        viewModelScope.launch {
            _isLoadingNextcloudBackups.value = true
            val result = nextcloudBackupManager.listBackups()
            _isLoadingNextcloudBackups.value = false
            if (result.isSuccess) {
                _nextcloudBackups.value = result.getOrDefault(emptyList())
            } else {
                _backupStatus.value = "Failed to load backups: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    /** Replaces this device's data with a backup from the server; like a local import, the app restarts afterwards. */
    fun restoreFromNextcloud(backup: NextcloudBackupFile, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isNextcloudRestoring.value = true
            val result = nextcloudBackupManager.restoreBackup(backup)
            _isNextcloudRestoring.value = false
            if (result.isSuccess) {
                userPrefsRepository.setOnboardingCompleted(true)
                _backupStatus.value = "Restore complete. Restarting..."
                onSuccess()
            } else {
                _backupStatus.value = "Restore failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

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
        AutoBackupWorker.scheduleFromPreferences(context, userPrefsRepository)
    }

    private companion object {
        const val DEFAULT_NEXTCLOUD_FOLDER = "Trainable/Backups"
    }
}
