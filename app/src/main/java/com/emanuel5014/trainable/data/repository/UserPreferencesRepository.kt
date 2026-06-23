package com.emanuel5014.trainable.data.repository

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
        val AUTO_BACKUP_INCLUDE_IMAGES = booleanPreferencesKey("auto_backup_include_images")
        val USER_LANGUAGE = stringPreferencesKey("user_language")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val FLOATING_NAV_BAR = booleanPreferencesKey("floating_nav_bar")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DYNAMIC_COLOR_SEED = intPreferencesKey("dynamic_color_seed")
        val THEME_PALETTE = intPreferencesKey("theme_palette")
        val THEME_STYLE = intPreferencesKey("theme_style")
        val TIMER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("timer_notifications_enabled")
        val SWIPE_ACTIONS_ENABLED = booleanPreferencesKey("swipe_actions_enabled")
        val GYM_MEMBERSHIP_EXPIRY_DATE = androidx.datastore.preferences.core.longPreferencesKey("gym_membership_expiry_date")
        val GYM_MEMBERSHIP_EXPIRY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("gym_membership_expiry_notifications_enabled")
        val GYM_MEMBERSHIP_EXPIRY_NOTIFICATION_DAYS_BEFORE = intPreferencesKey("gym_membership_expiry_notification_days_before")
        val LAST_NOTIFIED_EXPIRY_DATE = androidx.datastore.preferences.core.longPreferencesKey("last_notified_expiry_date")
        val THEME_MODE = intPreferencesKey("theme_mode")
    }

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] ?: false
        }

    val gymMembershipExpiryDate: Flow<Long?> = dataStore.data
        .map { preferences ->
            preferences[GYM_MEMBERSHIP_EXPIRY_DATE]
        }

    val gymMembershipExpiryNotificationsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[GYM_MEMBERSHIP_EXPIRY_NOTIFICATIONS_ENABLED] ?: false
        }

    val gymMembershipExpiryNotificationDaysBefore: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[GYM_MEMBERSHIP_EXPIRY_NOTIFICATION_DAYS_BEFORE] ?: 3
        }

    val lastNotifiedExpiryDate: Flow<Long> = dataStore.data
        .map { preferences ->
            preferences[LAST_NOTIFIED_EXPIRY_DATE] ?: 0L
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

    val autoBackupIncludeImages: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[AUTO_BACKUP_INCLUDE_IMAGES] ?: false
        }

    val userLanguage: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[USER_LANGUAGE]
        }

    val weightUnit: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[WEIGHT_UNIT] ?: "kg"
        }

    val floatingNavBar: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[FLOATING_NAV_BAR] ?: true
        }

    val dynamicColor: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DYNAMIC_COLOR] ?: true
        }

    val dynamicColorSeed: Flow<Int?> = dataStore.data
        .map { preferences ->
            preferences[DYNAMIC_COLOR_SEED]
        }

    val themePalette: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[THEME_PALETTE] ?: 0
        }

    val themeStyle: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[THEME_STYLE] ?: 0
        }

    val timerNotificationsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[TIMER_NOTIFICATIONS_ENABLED] ?: true
        }

    val swipeActionsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SWIPE_ACTIONS_ENABLED] ?: true
        }

    val themeMode: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[THEME_MODE] ?: 0
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

    suspend fun setAutoBackupIncludeImages(include: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_BACKUP_INCLUDE_IMAGES] = include
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

    suspend fun setWeightUnit(unit: String) {
        dataStore.edit { preferences ->
            preferences[WEIGHT_UNIT] = unit
        }
    }

    suspend fun setFloatingNavBar(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[FLOATING_NAV_BAR] = enabled
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setDynamicColorSeed(seed: Int?) {
        dataStore.edit { preferences ->
            if (seed != null) {
                preferences[DYNAMIC_COLOR_SEED] = seed
            } else {
                preferences.remove(DYNAMIC_COLOR_SEED)
            }
        }
    }

    suspend fun setThemePalette(index: Int) {
        dataStore.edit { preferences ->
            preferences[THEME_PALETTE] = index
        }
    }

    suspend fun setThemeStyle(index: Int) {
        dataStore.edit { preferences ->
            preferences[THEME_STYLE] = index
        }
    }

    suspend fun setTimerNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TIMER_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setSwipeActionsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SWIPE_ACTIONS_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(mode: Int) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun setGymMembershipExpiryDate(timestampMillis: Long?) {
        dataStore.edit { preferences ->
            if (timestampMillis != null) {
                preferences[GYM_MEMBERSHIP_EXPIRY_DATE] = timestampMillis
            } else {
                preferences.remove(GYM_MEMBERSHIP_EXPIRY_DATE)
            }
        }
    }

    suspend fun setGymMembershipExpiryNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[GYM_MEMBERSHIP_EXPIRY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setGymMembershipExpiryNotificationDaysBefore(days: Int) {
        dataStore.edit { preferences ->
            preferences[GYM_MEMBERSHIP_EXPIRY_NOTIFICATION_DAYS_BEFORE] = days
        }
    }

    suspend fun setLastNotifiedExpiryDate(timestampMillis: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_NOTIFIED_EXPIRY_DATE] = timestampMillis
        }
    }
}
