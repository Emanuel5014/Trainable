package com.emanuel5014.trainable.ui.screens.settings

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.emanuel5014.trainable.data.local.GymDatabase
import com.emanuel5014.trainable.data.remote.GitHubRelease
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import com.emanuel5014.trainable.util.AutoBackupWorker
import com.emanuel5014.trainable.util.BackupManager
import com.emanuel5014.trainable.util.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPrefsRepository: UserPreferencesRepository,
    private val workoutRepository: WorkoutRepository,
    private val localeManager: AppLocaleManager,
    private val backupManager: BackupManager,
    private val updateManager: UpdateManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val currentUser = userRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val weeklyGoal = userPrefsRepository.weeklyGoal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 3
    )

    val hapticEnabled = userPrefsRepository.hapticEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val autoBackupEnabled = userPrefsRepository.autoBackupEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val autoBackupFrequency = userPrefsRepository.autoBackupFrequency.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1
    )

    val autoBackupFolderUri = userPrefsRepository.autoBackupFolderUri.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val autoBackupMaxCount = userPrefsRepository.autoBackupMaxCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 5
    )

    val autoBackupIncludeImages = userPrefsRepository.autoBackupIncludeImages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

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

    val floatingNavBar = userPrefsRepository.floatingNavBar.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val dynamicColor = userPrefsRepository.dynamicColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val dynamicColorSeed = userPrefsRepository.dynamicColorSeed.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

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

    val themePalette = userPrefsRepository.themePalette.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val themeStyle = userPrefsRepository.themeStyle.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val weightUnit = userPrefsRepository.weightUnit.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "kg"
    )

    val timerNotificationsEnabled = userPrefsRepository.timerNotificationsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val gymMembershipExpiryNotificationsEnabled = userPrefsRepository.gymMembershipExpiryNotificationsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val gymMembershipExpiryNotificationDaysBefore = userPrefsRepository.gymMembershipExpiryNotificationDaysBefore.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 3
    )

    val swipeActionsEnabled = userPrefsRepository.swipeActionsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
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

    fun setWeeklyGoal(goal: Int) {
        viewModelScope.launch {
            userPrefsRepository.setWeeklyGoal(goal)
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setHapticEnabled(enabled)
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setAutoBackupEnabled(enabled)
            if (enabled) {
                AutoBackupWorker.schedule(context, autoBackupFrequency.value)
            } else {
                AutoBackupWorker.cancel(context)
            }
        }
    }

    fun setAutoBackupFrequency(frequency: Int) {
        viewModelScope.launch {
            userPrefsRepository.setAutoBackupFrequency(frequency)
            if (autoBackupEnabled.value) {
                AutoBackupWorker.schedule(context, frequency)
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

    fun setAutoBackupMaxCount(count: Int) {
        viewModelScope.launch {
            userPrefsRepository.setAutoBackupMaxCount(count)
        }
    }


    fun setAutoBackupIncludeImages(include: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setAutoBackupIncludeImages(include)
        }
    }

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
                GymDatabase.resetDatabase(context)
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
                _csvCache = workoutRepository.exportAllWorkoutsToCsv()
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

    fun setFloatingNavBar(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setFloatingNavBar(enabled)
        }
    }

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

    fun setTimerNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setTimerNotificationsEnabled(enabled)
        }
    }

    fun setGymMembershipExpiryNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setGymMembershipExpiryNotificationsEnabled(enabled)
        }
    }

    fun setGymMembershipExpiryNotificationDaysBefore(days: Int) {
        viewModelScope.launch {
            userPrefsRepository.setGymMembershipExpiryNotificationDaysBefore(days)
        }
    }

    fun setSwipeActionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.setSwipeActionsEnabled(enabled)
        }
    }

    fun setWeightUnit(unit: String) {
        viewModelScope.launch {
            userPrefsRepository.setWeightUnit(unit)
        }
    }

    fun getFolderDisplayPath(uriString: String?): String {
        if (uriString == null) return ""
        return try {
            val uri = Uri.parse(uriString)
            uri.lastPathSegment?.substringAfter("document/")?.replace(":", "/") ?: uriString
        } catch (e: Exception) {
            uriString
        }
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                userRepository.updateUser(user.copy(username = newUsername))
            }
        }
    }
}
