package com.example.gymtracking.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FREQUENCY = intPreferencesKey("auto_backup_frequency")
        val AUTO_BACKUP_FOLDER_URI = stringPreferencesKey("auto_backup_folder_uri")
        val AUTO_BACKUP_MAX_COUNT = intPreferencesKey("auto_backup_max_count")
        val USER_LANGUAGE = stringPreferencesKey("user_language")
    }

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] ?: false
        }

    val weeklyGoal: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[WEEKLY_GOAL] ?: 3
        }

    val hapticEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[HAPTIC_ENABLED] ?: true
        }

    val autoBackupEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[AUTO_BACKUP_ENABLED] ?: false
        }

    val autoBackupFrequency: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[AUTO_BACKUP_FREQUENCY] ?: 1
        }

    val autoBackupFolderUri: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[AUTO_BACKUP_FOLDER_URI]
        }

    val autoBackupMaxCount: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[AUTO_BACKUP_MAX_COUNT] ?: 5
        }

    val userLanguage: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[USER_LANGUAGE]
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    suspend fun setWeeklyGoal(goal: Int) {
        dataStore.edit { preferences ->
            preferences[WEEKLY_GOAL] = goal
        }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun clearAllPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun setAutoBackupFrequency(frequency: Int) {
        dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_FREQUENCY] = frequency
        }
    }

    suspend fun setAutoBackupFolderUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri != null) {
                preferences[AUTO_BACKUP_FOLDER_URI] = uri
            } else {
                preferences.remove(AUTO_BACKUP_FOLDER_URI)
            }
        }
    }

    suspend fun setAutoBackupMaxCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_MAX_COUNT] = count
        }
    }

    suspend fun setUserLanguage(language: String?) {
        dataStore.edit { preferences ->
            if (language != null) {
                preferences[USER_LANGUAGE] = language
            } else {
                preferences.remove(USER_LANGUAGE)
            }
        }
    }
}
