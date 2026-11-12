package com.emanuel5014.trainable.ui.screens.settings

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.emanuel5014.trainable.data.ai.AiModelStatus
import com.emanuel5014.trainable.data.ai.AiModelVariant
import com.emanuel5014.trainable.data.ai.DeviceCapabilityChecker
import com.emanuel5014.trainable.data.ai.LocalLlmEngine
import com.emanuel5014.trainable.data.ai.ModelDownloadManager
import com.emanuel5014.trainable.data.ai.ModelFileManager
import com.emanuel5014.trainable.data.ai.ModelImportError
import com.emanuel5014.trainable.data.local.GymDatabaseInstance
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.data.remote.GitHubRelease
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.data.model.GlassSettings
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.util.AppLocaleManager
import com.emanuel5014.trainable.util.PlateCalculator
import com.emanuel5014.trainable.util.TimerAdjustment
import com.emanuel5014.trainable.util.UpdateManager
import com.emanuel5014.trainable.util.UpdateChannel
import com.emanuel5014.trainable.BuildConfig
import kotlinx.coroutines.flow.map
import com.emanuel5014.trainable.util.backup.AutoBackupWorker
import com.emanuel5014.trainable.util.backup.BackupManager
import com.emanuel5014.trainable.util.notification.GymMembershipWorker
import com.emanuel5014.trainable.webserver.WebServerManager
import com.emanuel5014.trainable.webserver.WebServerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudBackupFile
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConfig
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudConnectionResult
import com.emanuel5014.trainable.data.remote.nextcloud.NextcloudWebDav
import com.emanuel5014.trainable.data.backup.NextcloudBackupService
import com.emanuel5014.trainable.util.backup.NextcloudBackupManager
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPrefsRepository: UserPreferencesRepository,
    private val workoutRepository: WorkoutRepository,
    private val localeManager: AppLocaleManager,
    private val backupManager: BackupManager,
    private val nextcloudBackupManager: NextcloudBackupManager,
    private val nextcloudWebDav: NextcloudWebDav,
    private val nextcloudBackupService: NextcloudBackupService,
    private val updateManager: UpdateManager,
    private val webServerManager: WebServerManager,
    private val deviceCapabilityChecker: DeviceCapabilityChecker,
    private val modelFileManager: ModelFileManager,
    private val modelDownloadManager: ModelDownloadManager,
    private val localLlmEngine: LocalLlmEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val state = SettingsState(userRepository, userPrefsRepository, viewModelScope)

    val currentUser = state.currentUser

    val weeklyGoal = state.weeklyGoal

    val hapticEnabled = state.hapticEnabled

    val autoBackupEnabled = state.autoBackupEnabled

    val autoBackupFrequency = state.autoBackupFrequency

    val autoBackupFolderUri = userPrefsRepository.autoBackupFolderUri.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val autoBackupMaxCount = state.autoBackupMaxCount

    val autoBackupIncludeImages = state.autoBackupIncludeImages

    val nextcloudBackupEnabled = state.nextcloudBackupEnabled

    val nextcloudAutoBackupEnabled = state.nextcloudAutoBackupEnabled

    val nextcloudServerUrl = state.nextcloudServerUrl

    val nextcloudUsername = state.nextcloudUsername

    val nextcloudRemoteFolder = state.nextcloudRemoteFolder

    val nextcloudWifiOnly = state.nextcloudWifiOnly

    val nextcloudAutoBackupFrequency = state.nextcloudAutoBackupFrequency

    val nextcloudAutoBackupMaxCount = state.nextcloudAutoBackupMaxCount

    val nextcloudAutoBackupIncludeImages = state.nextcloudAutoBackupIncludeImages

    private val _nextcloudTestState = MutableStateFlow<NextcloudConnectionResult?>(null)
    val nextcloudTestState: StateFlow<NextcloudConnectionResult?> = _nextcloudTestState.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _isNextcloudBackingUp = MutableStateFlow(false)
    val isNextcloudBackingUp: StateFlow<Boolean> = _isNextcloudBackingUp.asStateFlow()

    private val _isNextcloudRestoring = MutableStateFlow(false)
    val isNextcloudRestoring: StateFlow<Boolean> = _isNextcloudRestoring.asStateFlow()

    private val _nextcloudBackups = MutableStateFlow<List<NextcloudBackupFile>>(emptyList())
    val nextcloudBackups: StateFlow<List<NextcloudBackupFile>> = _nextcloudBackups.asStateFlow()

    private val _isLoadingNextcloudBackups = MutableStateFlow(false)
    val isLoadingNextcloudBackups: StateFlow<Boolean> = _isLoadingNextcloudBackups.asStateFlow()

    val userLanguage = localeManager.currentLanguage.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "system"
    )

    val resolvedLanguage = localeManager.userSelectedLanguage.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val navBarStyle = state.navBarStyle

    val dynamicColor = state.dynamicColor

    val dynamicColorSeed = state.dynamicColorSeed

    private val _wallpaperColors = MutableStateFlow<List<Int>>(emptyList())
    val wallpaperColors = _wallpaperColors.asStateFlow()

    init {
        extractWallpaperColors()
    }

    private fun extractWallpaperColors() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                val list = mutableListOf<Int>()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    colors?.primaryColor?.toArgb()?.let { list.add(it) }
                    colors?.secondaryColor?.toArgb()?.let { list.add(it) }
                    colors?.tertiaryColor?.toArgb()?.let { list.add(it) }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    // toArgb() is available from API 26+ for Color, but WallpaperColors.getPrimaryColor returns Color
                    @Suppress("NewApi")
                    colors?.primaryColor?.toArgb()?.let { list.add(it) }
                }
                _wallpaperColors.value = list.distinct()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val themePalette = state.themePalette

    val themeStyle = state.themeStyle

    val weightUnit = state.weightUnit

    val distanceUnit = state.distanceUnit

    val timerNotificationsEnabled = state.timerNotificationsEnabled

    val timerFinishedLockscreenVibrationDuration = state.timerFinishedLockscreenVibrationDuration

    val gymMembershipExpiryNotificationsEnabled = state.gymMembershipExpiryNotificationsEnabled

    val gymMembershipExpiryNotificationDaysBefore = state.gymMembershipExpiryNotificationDaysBefore

    val themeMode = state.themeMode

    val swipeActionsEnabled = state.swipeActionsEnabled

    val warmupTimerEnabled = state.warmupTimerEnabled

    val workoutTimerEnabled = state.workoutTimerEnabled

    val inlineExerciseModificationsEnabled = state.inlineExerciseModificationsEnabled

    val autoStopCardioAtTarget = state.autoStopCardioAtTarget

    val advancedProgrammingEnabled = state.advancedProgrammingEnabled

    fun setAdvancedProgrammingEnabled(enabled: Boolean) = state.setAdvancedProgrammingEnabled(enabled)

    val timerAddSeconds = state.timerAddSeconds

    fun setTimerAddSeconds(seconds: Int) = state.setTimerAddSeconds(seconds)

    val timerSubtractSeconds = state.timerSubtractSeconds

    fun setTimerSubtractSeconds(seconds: Int) = state.setTimerSubtractSeconds(seconds)

    val restTimerEnabled = state.restTimerEnabled

    fun setRestTimerEnabled(enabled: Boolean) = state.setRestTimerEnabled(enabled)

    val timerAddEnabled = state.timerAddEnabled

    fun setTimerAddEnabled(enabled: Boolean) = state.setTimerAddEnabled(enabled)

    val timerSubtractEnabled = state.timerSubtractEnabled

    fun setTimerSubtractEnabled(enabled: Boolean) = state.setTimerSubtractEnabled(enabled)

    val timerShowTimeButtons = state.timerShowTimeButtons

    fun setTimerShowTimeButtons(show: Boolean) = state.setTimerShowTimeButtons(show)

    val timerShowSkipButton = state.timerShowSkipButton

    fun setTimerShowSkipButton(show: Boolean) = state.setTimerShowSkipButton(show)

    val startingWeightEnabled = state.startingWeightEnabled

    fun setStartingWeightEnabled(enabled: Boolean) = state.setStartingWeightEnabled(enabled)

    val setPerformanceEnabled = state.setPerformanceEnabled

    fun setSetPerformanceEnabled(enabled: Boolean) = state.setSetPerformanceEnabled(enabled)

    val journalEnabled = state.journalEnabled

    fun setJournalEnabled(enabled: Boolean) = state.setJournalEnabled(enabled)

    val journalPromptAtFinish = state.journalPromptAtFinish

    fun setJournalPromptAtFinish(enabled: Boolean) = state.setJournalPromptAtFinish(enabled)

    val journalRatingEnabled = state.journalRatingEnabled

    fun setJournalRatingEnabled(enabled: Boolean) = state.setJournalRatingEnabled(enabled)

    val plateCalculatorEnabled = state.plateCalculatorEnabled

    fun setPlateCalculatorEnabled(enabled: Boolean) = state.setPlateCalculatorEnabled(enabled)

    /** Plates the gym has, in the current weight unit. */
    val plateCalculatorPlates = state.plateCalculatorPlates

    fun setPlateCalculatorPlates(plates: List<Float>) = state.setPlateCalculatorPlates(plates)

    val loadRoundingIncrement = state.loadRoundingIncrement

    fun setLoadRoundingIncrement(increment: Float) = state.setLoadRoundingIncrement(increment)

    val rpeEnabled = state.rpeEnabled

    val rpeScale = state.rpeScale

    fun setRpeEnabled(enabled: Boolean) = state.setRpeEnabled(enabled)

    fun setRpeScale(scale: RpeScale) = state.setRpeScale(scale)

    val autoStopTimeWeightAtTarget = state.autoStopTimeWeightAtTarget

    val keepScreenOnCardioTimer = state.keepScreenOnCardioTimer

    val keepScreenOnSetTimer = state.keepScreenOnSetTimer

    val webServerState = webServerManager.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WebServerState()
    )

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus

    private val _resetComplete = MutableStateFlow(false)
    val resetComplete: StateFlow<Boolean> = _resetComplete

    private val _latestRelease = MutableStateFlow<GitHubRelease?>(null)
    val latestRelease = _latestRelease.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress = _downloadProgress.asStateFlow()

    /** Beta updates on or off; until the user chooses, a beta install counts as on (see [UpdateChannel.isBeta]). */
    val betaUpdates = userPrefsRepository.updateChannelBeta
        .map { UpdateChannel.isBeta(it, BuildConfig.VERSION_NAME) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UpdateChannel.isBeta(null, BuildConfig.VERSION_NAME))

    /** Saves the channel, then looks for an update on it right away. */
    fun setBetaUpdates(beta: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setUpdateChannelBeta(beta)
            checkForUpdates()
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _backupStatus.value = "Checking for updates..."
            val release = updateManager.checkForUpdates()
            if (release != null) {
                _latestRelease.value = release
                _backupStatus.value = "New update available: ${release.tagName}"
            } else {
                _backupStatus.value = "No updates found"
            }
        }
    }

    fun downloadAndInstall(release: GitHubRelease) {
        viewModelScope.launch {
            _isDownloading.value = true
            updateManager.downloadAndInstall(release) { progress ->
                _downloadProgress.value = progress
            }.onSuccess {
                _isDownloading.value = false
                _latestRelease.value = null
            }.onFailure {
                _isDownloading.value = false
                _backupStatus.value = "Download failed: ${it.message}"
            }
        }
    }

    fun clearUpdate() {
        _latestRelease.value = null
    }

    fun setWeeklyGoal(goal: Int) = state.setWeeklyGoal(goal)

    fun setHapticEnabled(enabled: Boolean) = state.setHapticEnabled(enabled)

    private fun rescheduleAutoBackup() {
        viewModelScope.launch {
            AutoBackupWorker.scheduleFromPreferences(context, userPrefsRepository)
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setAutoBackupEnabled(enabled)
            rescheduleAutoBackup()
        }
    }

    fun setAutoBackupFrequency(frequency: Int) {
        viewModelScope.launch {
            userPrefsRepository.setAutoBackupFrequency(frequency)
            rescheduleAutoBackup()
        }
    }

    fun setNextcloudBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setNextcloudBackupEnabled(enabled)
            rescheduleAutoBackup()
        }
    }

    fun setNextcloudAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setNextcloudAutoBackupEnabled(enabled)
            rescheduleAutoBackup()
        }
    }

    fun setNextcloudWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setNextcloudWifiOnly(wifiOnly)
            rescheduleAutoBackup()
        }
    }

    fun setNextcloudAutoBackupFrequency(frequency: Int) {
        viewModelScope.launch {
            userPrefsRepository.setNextcloudAutoBackupFrequency(frequency)
            rescheduleAutoBackup()
        }
    }

    fun setNextcloudAutoBackupMaxCount(maxCount: Int) {
        viewModelScope.launch {
            userPrefsRepository.setNextcloudAutoBackupMaxCount(maxCount)
        }
    }

    fun setNextcloudAutoBackupIncludeImages(includeImages: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setNextcloudAutoBackupIncludeImages(includeImages)
        }
    }

    fun setNextcloudAutoBackupSettings(
        frequency: Int,
        maxCount: Int,
        includeImages: Boolean,
        wifiOnly: Boolean
    ) {
        viewModelScope.launch {
            userPrefsRepository.setNextcloudAutoBackupFrequency(frequency)
            userPrefsRepository.setNextcloudAutoBackupMaxCount(maxCount)
            userPrefsRepository.setNextcloudAutoBackupIncludeImages(includeImages)
            userPrefsRepository.setNextcloudWifiOnly(wifiOnly)
            userPrefsRepository.setNextcloudAutoBackupEnabled(true)
            rescheduleAutoBackup()
        }
    }

    fun clearNextcloudTestState() {
        _nextcloudTestState.value = null
    }

    fun testNextcloudConnection(serverUrl: String, username: String, passwordOrToken: String) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _nextcloudTestState.value = null
            val config = NextcloudConfig(
                serverUrl = serverUrl.trim(),
                username = username.trim(),
                passwordOrToken = passwordOrToken.trim()
            )
            val result = nextcloudWebDav.testConnection(config)
            _nextcloudTestState.value = result
            _isTestingConnection.value = false
        }
    }

    fun saveNextcloudConfig(
        serverUrl: String,
        username: String,
        passwordOrToken: String,
        remoteFolder: String
    ) {
        viewModelScope.launch {
            nextcloudBackupService.saveConfig(NextcloudConfig(serverUrl, username, passwordOrToken, remoteFolder))
            _backupStatus.value = "Nextcloud configured successfully"
            rescheduleAutoBackup()
        }
    }

    fun disconnectNextcloud() {
        viewModelScope.launch {
            userPrefsRepository.clearNextcloudConfig()
            _nextcloudBackups.value = emptyList()
            _backupStatus.value = "Nextcloud disconnected"
            rescheduleAutoBackup()
        }
    }

    fun backupToNextcloud(includeImages: Boolean = false) {
        viewModelScope.launch {
            _isNextcloudBackingUp.value = true
            _backupStatus.value = "Uploading backup to Nextcloud..."
            val result = nextcloudBackupManager.backupNow(includeImages)
            _isNextcloudBackingUp.value = false
            if (result.isSuccess) {
                _backupStatus.value = "Backup uploaded to Nextcloud successfully!"
                loadNextcloudBackups()
            } else {
                _backupStatus.value = "Nextcloud backup failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
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

    fun restoreFromNextcloud(backupFile: NextcloudBackupFile, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isNextcloudRestoring.value = true
            _backupStatus.value = "Restoring from Nextcloud..."
            val result = nextcloudBackupManager.restoreBackup(backupFile)
            _isNextcloudRestoring.value = false
            if (result.isSuccess) {
                _backupStatus.value = "Restore complete. Restarting..."
                onSuccess()
            } else {
                _backupStatus.value = "Restore failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun setAutoBackupFolder(uri: Uri) {
        viewModelScope.launch {
            if (uri != Uri.EMPTY) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    userPrefsRepository.setAutoBackupFolderUri(uri.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                    _backupStatus.value = "Failed to take folder permission"
                }
            } else {
                userPrefsRepository.setAutoBackupFolderUri(null)
            }
        }
    }

    fun setAutoBackupMaxCount(count: Int) = state.setAutoBackupMaxCount(count)


    fun setAutoBackupIncludeImages(include: Boolean) = state.setAutoBackupIncludeImages(include)

    fun runBackupNow() {
        viewModelScope.launch {
            _backupStatus.value = "Starting auto-backup test..."
            val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
                .build()
            WorkManager.getInstance(context).enqueue(request)
            _backupStatus.value = "Auto-backup worker enqueued"
        }
    }

    fun exportDatabase(uri: Uri, includeImages: Boolean = false) {
        viewModelScope.launch {
            _backupStatus.value = "Exporting..."
            val success = backupManager.exportDatabaseZip(uri, includeImages)
            _backupStatus.value = if (success) "Export successful" else "Export failed"
        }
    }

    fun importDatabase(uri: Uri, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _backupStatus.value = "Importing..."
            val success = backupManager.importDatabaseZip(uri)
            _backupStatus.value = if (success) "Import successful" else "Import failed"
            if (success) {
                onSuccess()
            }
        }
    }

    fun resetApp(onResetComplete: () -> Unit) {
        viewModelScope.launch {
            _backupStatus.value = "Resetting app..."
            try {
                GymDatabaseInstance.resetDatabase(context)
                userPrefsRepository.clearAllPreferences()
                // Clear analytics widgets preferences
                context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE).edit().clear().commit()
                
                _backupStatus.value = "Reset complete. Restarting..."
                _resetComplete.value = true
                onResetComplete()
            } catch (e: Exception) {
                _backupStatus.value = "Reset failed: ${e.message}"
            }
        }
    }

    fun clearStatus() {
        _backupStatus.value = null
    }

    private var _csvCache: String? = null

    fun getCsvContent(): String? {
        return _csvCache
    }

    fun exportWorkoutsToCsv() {
        viewModelScope.launch {
            _backupStatus.value = "Exporting CSV..."
            try {
                val languageCode = localeManager.getResolvedLanguage()
                _csvCache = workoutRepository.exportAllWorkoutsToCsv(weightUnit.value, languageCode)
                _backupStatus.value = "Export ready"
            } catch (e: Exception) {
                _backupStatus.value = "Export failed: ${e.message}"
            }
        }
    }

    fun setLanguage(languageCode: String, onLanguageChanged: () -> Unit) {
        viewModelScope.launch {
            localeManager.setUserLanguage(languageCode)
            onLanguageChanged()
        }
    }

    fun setNavBarStyle(style: NavBarStyle) = state.setNavBarStyle(style)

    val glassSettings = state.glassSettings

    fun setLiquidGlassEnabled(enabled: Boolean) = state.setLiquidGlassEnabled(enabled)

    fun setGlassStretch(on: Boolean) = state.setGlassStretch(on)

    fun setGlassBlur(blur: Float) = state.setGlassBlur(blur)

    fun setGlassIntensity(intensity: Float) = state.setGlassIntensity(intensity)

    fun setGlassPart(part: GlassPart, on: Boolean) = state.setGlassPart(part, on)

    fun setDynamicColor(enabled: Boolean) = state.setDynamicColor(enabled)

    fun setDynamicColorSeed(seed: Int?) = state.setDynamicColorSeed(seed)

    fun setThemePalette(index: Int) = state.setThemePalette(index)

    fun setThemeStyle(index: Int) = state.setThemeStyle(index)

    fun setTimerNotificationsEnabled(enabled: Boolean) = state.setTimerNotificationsEnabled(enabled)

    fun setTimerFinishedLockscreenVibrationDuration(durationSeconds: Int) = state.setTimerFinishedLockscreenVibrationDuration(durationSeconds)

    fun setGymMembershipExpiryNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setGymMembershipExpiryNotificationsEnabled(enabled)
            if (enabled) {
                GymMembershipWorker.enqueueImmediateCheck(context)
            }
        }
    }

    fun setGymMembershipExpiryNotificationDaysBefore(days: Int) {
        viewModelScope.launch {
            userPrefsRepository.setGymMembershipExpiryNotificationDaysBefore(days)
            GymMembershipWorker.enqueueImmediateCheck(context)
        }
    }

    fun setSwipeActionsEnabled(enabled: Boolean) = state.setSwipeActionsEnabled(enabled)

    fun setWarmupTimerEnabled(enabled: Boolean) = state.setWarmupTimerEnabled(enabled)

    fun setWorkoutTimerEnabled(enabled: Boolean) = state.setWorkoutTimerEnabled(enabled)

    fun setInlineExerciseModificationsEnabled(enabled: Boolean) = state.setInlineExerciseModificationsEnabled(enabled)

    fun setAutoStopCardioAtTarget(enabled: Boolean) = state.setAutoStopCardioAtTarget(enabled)

    fun setAutoStopTimeWeightAtTarget(enabled: Boolean) = state.setAutoStopTimeWeightAtTarget(enabled)

    fun setKeepScreenOnCardioTimer(enabled: Boolean) = state.setKeepScreenOnCardioTimer(enabled)

    fun setKeepScreenOnSetTimer(enabled: Boolean) = state.setKeepScreenOnSetTimer(enabled)

    fun setThemeMode(mode: Int) = state.setThemeMode(mode)

    fun setWeightUnit(unit: String) = state.setWeightUnit(unit)
    fun setDistanceUnit(unit: String) = state.setDistanceUnit(unit)

    fun getFolderDisplayPath(uriString: String?): String {
        if (uriString == null) return ""
        return try {
            val uri = Uri.parse(uriString)
            uri.lastPathSegment?.substringAfter("document/")?.replace(":", "/") ?: uriString
        } catch (e: Exception) {
            uriString
        }
    }

    fun updateUsername(newUsername: String) = state.updateUsername(newUsername)

    private var lastWebServerToggleMs = 0L

    fun toggleWebServer() {
        val now = System.currentTimeMillis()
        if (now - lastWebServerToggleMs < 600) return
        lastWebServerToggleMs = now
        if (webServerState.value.isRunning) {
            webServerManager.stopServer()
        } else {
            webServerManager.startServer()
        }
    }

    // region AI Scan

    val aiScanEnabled = userPrefsRepository.aiScanEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val aiResourceAnalyticsEnabled = userPrefsRepository.aiResourceAnalyticsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

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

    fun setAiScanEnabled(enabled: Boolean) = state.setAiScanEnabled(enabled)

    fun setAiResourceAnalyticsEnabled(enabled: Boolean) = state.setAiResourceAnalyticsEnabled(enabled)

    fun setAiModelVariant(variantId: String) = state.setAiModelVariant(variantId)

    fun downloadAiModel() {
        val variant = AiModelVariant.fromId(aiModelVariant.value)
        modelDownloadManager.startDownload(variant)
    }

    fun cancelAiModelDownload() {
        val variant = AiModelVariant.fromId(aiModelVariant.value)
        modelDownloadManager.cancelDownload(variant)
    }

    val aiModelImportError: StateFlow<ModelImportError?> = modelDownloadManager.importError

    /** Copies in a Gemma 4 model the user already has; the variant follows the file picked. */
    fun importAiModel(uri: Uri) {
        modelDownloadManager.importModel(uri)
    }

    fun clearAiModelImportError() {
        modelDownloadManager.clearImportError()
    }

    fun deleteAiModel() {
        viewModelScope.launch {
            val variant = AiModelVariant.fromId(aiModelVariant.value)
            modelDownloadManager.cancelDownload(variant)
            localLlmEngine.release()
            modelFileManager.delete(variant)
        }
    }

    // endregion
}
