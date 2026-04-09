package com.example.gymtracking.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gymtracking.data.local.GymDatabase
import com.example.gymtracking.data.repository.UserPreferencesRepository
import com.example.gymtracking.data.repository.UserRepository
import com.example.gymtracking.data.repository.WorkoutRepository
import com.example.gymtracking.util.AutoBackupWorker
import com.example.gymtracking.util.AppLocaleManager
import com.example.gymtracking.util.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus

    private val _resetComplete = MutableStateFlow(false)
    val resetComplete: StateFlow<Boolean> = _resetComplete

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
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            userPrefsRepository.setAutoBackupFolderUri(uri.toString())
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
}
