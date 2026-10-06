package com.emanuel5014.trainable.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.util.PlateCalculator
import com.emanuel5014.trainable.util.TimerAdjustment
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
        val NAV_BAR_STYLE = intPreferencesKey("nav_bar_style")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DYNAMIC_COLOR_SEED = intPreferencesKey("dynamic_color_seed")
        val THEME_PALETTE = intPreferencesKey("theme_palette")
        val THEME_STYLE = intPreferencesKey("theme_style")
        val TIMER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("timer_notifications_enabled")
        val SWIPE_ACTIONS_ENABLED = booleanPreferencesKey("swipe_actions_enabled")
        val TIMER_FINISHED_LOCKSCREEN_VIBRATION_DURATION = intPreferencesKey("timer_finished_lockscreen_vibration_duration")
        val WARMUP_TIMER_ENABLED = booleanPreferencesKey("warmup_timer_enabled")
        val GYM_MEMBERSHIP_EXPIRY_DATE = androidx.datastore.preferences.core.longPreferencesKey("gym_membership_expiry_date")
        val GYM_MEMBERSHIP_EXPIRY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("gym_membership_expiry_notifications_enabled")
        val GYM_MEMBERSHIP_EXPIRY_NOTIFICATION_DAYS_BEFORE = intPreferencesKey("gym_membership_expiry_notification_days_before")
        val LAST_NOTIFIED_EXPIRY_DATE = androidx.datastore.preferences.core.longPreferencesKey("last_notified_expiry_date")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val PHYSICAL_CHECK_BIOMETRIC_ENABLED = booleanPreferencesKey("physical_check_biometric_enabled")
        val PHYSICAL_CHECK_ENCRYPTION_ENABLED = booleanPreferencesKey("physical_check_encryption_enabled")
        val PHYSICAL_CHECK_ENCRYPTION_SALT = stringPreferencesKey("physical_check_encryption_salt")
        val PHYSICAL_CHECK_WRAPPED_KEY_KEYSTORE = stringPreferencesKey("physical_check_wrapped_key_keystore")
        val PHYSICAL_CHECK_WRAPPED_KEY_IV = stringPreferencesKey("physical_check_wrapped_key_iv")
        val PHYSICAL_CHECK_VALIDATION_BLOCK = stringPreferencesKey("physical_check_validation_block")
        val PHYSICAL_CHECK_VALIDATION_IV = stringPreferencesKey("physical_check_validation_iv")
        val EDITABLE_PRESET_EXERCISES = booleanPreferencesKey("editable_preset_exercises")
    val WORKOUT_TIMER_ENABLED = booleanPreferencesKey("workout_timer_enabled")
    val INLINE_EXERCISE_MODIFICATIONS_ENABLED = booleanPreferencesKey("inline_exercise_modifications_enabled")
    val AI_SCAN_ENABLED = booleanPreferencesKey("ai_scan_enabled")
    val AI_RESOURCE_ANALYTICS_ENABLED = booleanPreferencesKey("ai_resource_analytics_enabled")
    val AI_MODEL_VARIANT = stringPreferencesKey("ai_model_variant")
    val AUTO_STOP_CARDIO_AT_TARGET = booleanPreferencesKey("auto_stop_cardio_at_target")
    val AUTO_STOP_TIME_WEIGHT_AT_TARGET = booleanPreferencesKey("auto_stop_time_weight_at_target")
    /** Master switch for %1RM / technique / weekly-program features. Off by default. */
    val ADVANCED_PROGRAMMING_ENABLED = booleanPreferencesKey("advanced_programming_enabled")
    /** Shows the user's own image/GIF of each exercise during a workout. Off by default. */
    val EXERCISE_MEDIA_ENABLED = booleanPreferencesKey("exercise_media_enabled")
    /** With exercise media on: also fill the free space under the sets with a large preview when there is room. */
    val EXERCISE_MEDIA_LARGE = booleanPreferencesKey("exercise_media_large")
    /** Master switch for the plate calculator (which plates go on each side of the bar). Off by default. */
    val PLATE_CALCULATOR_ENABLED = booleanPreferencesKey("plate_calculator_enabled")
    /** Plates the gym has, comma separated and heaviest first, one list per weight unit. */
    val PLATE_CALCULATOR_PLATES_KG = stringPreferencesKey("plate_calculator_plates_kg")
    val PLATE_CALCULATOR_PLATES_LB = stringPreferencesKey("plate_calculator_plates_lb")
    /** Seconds the + and - buttons of the rest and warmup timers move the countdown by. */
    val TIMER_ADD_SECONDS = intPreferencesKey("timer_add_seconds")
    val TIMER_SUBTRACT_SECONDS = intPreferencesKey("timer_subtract_seconds")
    /** Whether the rest and warmup timers (and their notifications) show the - / + buttons and the skip button. */
    val TIMER_SHOW_TIME_BUTTONS = booleanPreferencesKey("timer_show_time_buttons")
    val TIMER_SHOW_SKIP_BUTTON = booleanPreferencesKey("timer_show_skip_button")
    /** Each of the two time buttons on its own: with both off the timers carry neither. On by default. */
    val TIMER_ADD_ENABLED = booleanPreferencesKey("timer_add_enabled")
    val TIMER_SUBTRACT_ENABLED = booleanPreferencesKey("timer_subtract_enabled")
    val LOAD_ROUNDING_KG = floatPreferencesKey("load_rounding_kg")
    val LOAD_ROUNDING_LB = floatPreferencesKey("load_rounding_lb")
    /** 0 = RPE input only on advanced (%1RM) exercises, 1 = on every exercise, 2 = never. */
    val RPE_INPUT_MODE = intPreferencesKey("rpe_input_mode")
    val KEEP_SCREEN_ON_CARDIO_TIMER = booleanPreferencesKey("keep_screen_on_cardio_timer")
    val KEEP_SCREEN_ON_SET_TIMER = booleanPreferencesKey("keep_screen_on_set_timer")
    val NEXTCLOUD_BACKUP_ENABLED = booleanPreferencesKey("nextcloud_backup_enabled")
    val NEXTCLOUD_AUTO_BACKUP_ENABLED = booleanPreferencesKey("nextcloud_auto_backup_enabled")
    val NEXTCLOUD_SERVER_URL = stringPreferencesKey("nextcloud_server_url")
    val NEXTCLOUD_USERNAME = stringPreferencesKey("nextcloud_username")
    val NEXTCLOUD_ENCRYPTED_PASSWORD = stringPreferencesKey("nextcloud_encrypted_password")
    val NEXTCLOUD_PASSWORD_IV = stringPreferencesKey("nextcloud_password_iv")
    val NEXTCLOUD_REMOTE_FOLDER = stringPreferencesKey("nextcloud_remote_folder")
    val NEXTCLOUD_WIFI_ONLY = booleanPreferencesKey("nextcloud_wifi_only")
    val NEXTCLOUD_AUTO_BACKUP_FREQUENCY = intPreferencesKey("nextcloud_auto_backup_frequency")
    val NEXTCLOUD_AUTO_BACKUP_MAX_COUNT = intPreferencesKey("nextcloud_auto_backup_max_count")
    val NEXTCLOUD_AUTO_BACKUP_INCLUDE_IMAGES = booleanPreferencesKey("nextcloud_auto_backup_include_images")
    }

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] ?: false
        }

    val physicalCheckBiometricEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PHYSICAL_CHECK_BIOMETRIC_ENABLED] ?: false
        }

    val physicalCheckEncryptionEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PHYSICAL_CHECK_ENCRYPTION_ENABLED] ?: false
        }

    val physicalCheckEncryptionSalt: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[PHYSICAL_CHECK_ENCRYPTION_SALT]
        }

    val physicalCheckWrappedKeyKeystore: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[PHYSICAL_CHECK_WRAPPED_KEY_KEYSTORE]
        }

    val physicalCheckWrappedKeyIv: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[PHYSICAL_CHECK_WRAPPED_KEY_IV]
        }

    val physicalCheckValidationBlock: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[PHYSICAL_CHECK_VALIDATION_BLOCK]
        }

    val physicalCheckValidationIv: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[PHYSICAL_CHECK_VALIDATION_IV]
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

    /** Falls back to the old floating/classic switch until the user picks one of the three styles. */
    val navBarStyle: Flow<NavBarStyle> = dataStore.data
        .map { preferences ->
            NavBarStyle.fromId(preferences[NAV_BAR_STYLE])
                ?: if (preferences[FLOATING_NAV_BAR] ?: true) NavBarStyle.Floating else NavBarStyle.Classic
        }

    val floatingNavBar: Flow<Boolean> = navBarStyle.map { it == NavBarStyle.Floating }

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

    val timerFinishedLockscreenVibrationDuration: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[TIMER_FINISHED_LOCKSCREEN_VIBRATION_DURATION] ?: 0
        }

    val swipeActionsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SWIPE_ACTIONS_ENABLED] ?: true
        }

    val warmupTimerEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[WARMUP_TIMER_ENABLED] ?: false
        }

    val editablePresetExercises: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[EDITABLE_PRESET_EXERCISES] ?: false
        }

    val workoutTimerEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[WORKOUT_TIMER_ENABLED] ?: false
        }

    val inlineExerciseModificationsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[INLINE_EXERCISE_MODIFICATIONS_ENABLED] ?: false
        }

    val aiScanEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[AI_SCAN_ENABLED] ?: false
        }

    val aiResourceAnalyticsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[AI_RESOURCE_ANALYTICS_ENABLED] ?: false
        }

    val aiModelVariant: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[AI_MODEL_VARIANT] ?: "e2b"
        }

    val autoStopCardioAtTarget: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[AUTO_STOP_CARDIO_AT_TARGET] ?: true
        }

    val autoStopTimeWeightAtTarget: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[AUTO_STOP_TIME_WEIGHT_AT_TARGET] ?: true
        }

    val keepScreenOnCardioTimer: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEEP_SCREEN_ON_CARDIO_TIMER] ?: true
        }

    val keepScreenOnSetTimer: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[KEEP_SCREEN_ON_SET_TIMER] ?: true
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

    suspend fun setAiScanEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AI_SCAN_ENABLED] = enabled
        }
    }

    suspend fun setAiResourceAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AI_RESOURCE_ANALYTICS_ENABLED] = enabled
        }
    }

    suspend fun setAiModelVariant(variantId: String) {
        dataStore.edit { preferences ->
            preferences[AI_MODEL_VARIANT] = variantId
        }
    }

    suspend fun setAutoStopCardioAtTarget(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_STOP_CARDIO_AT_TARGET] = enabled
        }
    }

    val advancedProgrammingEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[ADVANCED_PROGRAMMING_ENABLED] ?: false }

    suspend fun setAdvancedProgrammingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ADVANCED_PROGRAMMING_ENABLED] = enabled
        }
    }

    val exerciseMediaEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[EXERCISE_MEDIA_ENABLED] ?: false }

    suspend fun setExerciseMediaEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EXERCISE_MEDIA_ENABLED] = enabled
        }
    }

    val exerciseMediaLarge: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[EXERCISE_MEDIA_LARGE] ?: true }

    suspend fun setExerciseMediaLarge(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EXERCISE_MEDIA_LARGE] = enabled
        }
    }

    val timerAddSeconds: Flow<Int> = dataStore.data
        .map { preferences -> TimerAdjustment.sanitizeAdd(preferences[TIMER_ADD_SECONDS]) }

    suspend fun setTimerAddSeconds(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[TIMER_ADD_SECONDS] = TimerAdjustment.sanitizeAdd(seconds)
        }
    }

    val timerSubtractSeconds: Flow<Int> = dataStore.data
        .map { preferences -> TimerAdjustment.sanitizeSubtract(preferences[TIMER_SUBTRACT_SECONDS]) }

    suspend fun setTimerSubtractSeconds(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[TIMER_SUBTRACT_SECONDS] = TimerAdjustment.sanitizeSubtract(seconds)
        }
    }

    val timerAddEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[TIMER_ADD_ENABLED] ?: true }

    suspend fun setTimerAddEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TIMER_ADD_ENABLED] = enabled
        }
    }

    val timerSubtractEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[TIMER_SUBTRACT_ENABLED] ?: true }

    suspend fun setTimerSubtractEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TIMER_SUBTRACT_ENABLED] = enabled
        }
    }

    val timerShowTimeButtons: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[TIMER_SHOW_TIME_BUTTONS] ?: true }

    suspend fun setTimerShowTimeButtons(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[TIMER_SHOW_TIME_BUTTONS] = show
        }
    }

    val timerShowSkipButton: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[TIMER_SHOW_SKIP_BUTTON] ?: true }

    suspend fun setTimerShowSkipButton(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[TIMER_SHOW_SKIP_BUTTON] = show
        }
    }

    val plateCalculatorEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PLATE_CALCULATOR_ENABLED] ?: false }

    suspend fun setPlateCalculatorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PLATE_CALCULATOR_ENABLED] = enabled
        }
    }

    /** Plates available in the current weight unit, heaviest first. */
    val plateCalculatorPlates: Flow<List<Float>> = dataStore.data
        .map { preferences ->
            val unit = preferences[WEIGHT_UNIT] ?: "kg"
            decodePlates(preferences[plateKey(unit)], unit)
        }

    suspend fun setPlateCalculatorPlates(unit: String, plates: List<Float>) {
        dataStore.edit { preferences ->
            preferences[plateKey(unit)] = encodePlates(plates)
        }
    }

    private fun plateKey(unit: String) =
        if (unit == "lb") PLATE_CALCULATOR_PLATES_LB else PLATE_CALCULATOR_PLATES_KG

    /** Plate lists are stored as text; anything unreadable or empty falls back to the usual plates. */
    private fun decodePlates(raw: String?, unit: String): List<Float> {
        val plates = raw.orEmpty().split(',').mapNotNull { it.trim().toFloatOrNull() }.filter { it > 0f }
        return plates.distinct().sortedDescending().ifEmpty { PlateCalculator.defaultPlates(unit) }
    }

    private fun encodePlates(plates: List<Float>): String =
        plates.distinct().sortedDescending().joinToString(",")

    /** Plate increment used to round %1RM loads, expressed in the current weight unit. */
    val loadRoundingIncrement: Flow<Float> = dataStore.data
        .map { preferences ->
            if ((preferences[WEIGHT_UNIT] ?: "kg") == "lb") {
                preferences[LOAD_ROUNDING_LB] ?: LoadCalculator.DEFAULT_INCREMENT_LB
            } else {
                preferences[LOAD_ROUNDING_KG] ?: LoadCalculator.DEFAULT_INCREMENT_KG
            }
        }

    suspend fun setLoadRoundingIncrement(unit: String, increment: Float) {
        dataStore.edit { preferences ->
            preferences[if (unit == "lb") LOAD_ROUNDING_LB else LOAD_ROUNDING_KG] = increment
        }
    }

    val rpeInputMode: Flow<Int> = dataStore.data
        .map { preferences -> preferences[RPE_INPUT_MODE] ?: 0 }

    suspend fun setRpeInputMode(mode: Int) {
        dataStore.edit { preferences ->
            preferences[RPE_INPUT_MODE] = mode
        }
    }

    suspend fun setAutoStopTimeWeightAtTarget(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_STOP_TIME_WEIGHT_AT_TARGET] = enabled
        }
    }

    suspend fun setKeepScreenOnCardioTimer(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEEP_SCREEN_ON_CARDIO_TIMER] = enabled
        }
    }

    suspend fun setKeepScreenOnSetTimer(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEEP_SCREEN_ON_SET_TIMER] = enabled
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

    suspend fun setNavBarStyle(style: NavBarStyle) {
        dataStore.edit { preferences ->
            preferences[NAV_BAR_STYLE] = style.id
            // Kept in sync for versions and backups that only know the floating/classic switch
            preferences[FLOATING_NAV_BAR] = style == NavBarStyle.Floating
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

    suspend fun setTimerFinishedLockscreenVibrationDuration(durationSeconds: Int) {
        dataStore.edit { preferences ->
            preferences[TIMER_FINISHED_LOCKSCREEN_VIBRATION_DURATION] = durationSeconds
        }
    }

    suspend fun setSwipeActionsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SWIPE_ACTIONS_ENABLED] = enabled
        }
    }

    suspend fun setWarmupTimerEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[WARMUP_TIMER_ENABLED] = enabled
        }
    }

    suspend fun setEditablePresetExercises(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EDITABLE_PRESET_EXERCISES] = enabled
        }
    }

    suspend fun setWorkoutTimerEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[WORKOUT_TIMER_ENABLED] = enabled
        }
    }

    suspend fun setInlineExerciseModificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[INLINE_EXERCISE_MODIFICATIONS_ENABLED] = enabled
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

    suspend fun setPhysicalCheckBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PHYSICAL_CHECK_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setPhysicalCheckEncryptionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PHYSICAL_CHECK_ENCRYPTION_ENABLED] = enabled
        }
    }

    suspend fun setPhysicalCheckEncryptionSalt(salt: String?) {
        dataStore.edit { preferences ->
            if (salt != null) {
                preferences[PHYSICAL_CHECK_ENCRYPTION_SALT] = salt
            } else {
                preferences.remove(PHYSICAL_CHECK_ENCRYPTION_SALT)
            }
        }
    }

    suspend fun setPhysicalCheckWrappedKeyKeystore(key: String?) {
        dataStore.edit { preferences ->
            if (key != null) {
                preferences[PHYSICAL_CHECK_WRAPPED_KEY_KEYSTORE] = key
            } else {
                preferences.remove(PHYSICAL_CHECK_WRAPPED_KEY_KEYSTORE)
            }
        }
    }

    suspend fun setPhysicalCheckWrappedKeyIv(iv: String?) {
        dataStore.edit { preferences ->
            if (iv != null) {
                preferences[PHYSICAL_CHECK_WRAPPED_KEY_IV] = iv
            } else {
                preferences.remove(PHYSICAL_CHECK_WRAPPED_KEY_IV)
            }
        }
    }

    suspend fun setPhysicalCheckValidationBlock(block: String?) {
        dataStore.edit { preferences ->
            if (block != null) {
                preferences[PHYSICAL_CHECK_VALIDATION_BLOCK] = block
            } else {
                preferences.remove(PHYSICAL_CHECK_VALIDATION_BLOCK)
            }
        }
    }

    suspend fun setPhysicalCheckValidationIv(iv: String?) {
        dataStore.edit { preferences ->
            if (iv != null) {
                preferences[PHYSICAL_CHECK_VALIDATION_IV] = iv
            } else {
                preferences.remove(PHYSICAL_CHECK_VALIDATION_IV)
            }
        }
    }

    val nextcloudBackupEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[NEXTCLOUD_BACKUP_ENABLED] ?: false }

    val nextcloudAutoBackupEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[NEXTCLOUD_AUTO_BACKUP_ENABLED] ?: false }

    val nextcloudServerUrl: Flow<String?> = dataStore.data
        .map { preferences -> preferences[NEXTCLOUD_SERVER_URL] }

    val nextcloudUsername: Flow<String?> = dataStore.data
        .map { preferences -> preferences[NEXTCLOUD_USERNAME] }

    val nextcloudEncryptedPassword: Flow<String?> = dataStore.data
        .map { preferences -> preferences[NEXTCLOUD_ENCRYPTED_PASSWORD] }

    val nextcloudPasswordIv: Flow<String?> = dataStore.data
        .map { preferences -> preferences[NEXTCLOUD_PASSWORD_IV] }

    val nextcloudRemoteFolder: Flow<String> = dataStore.data
        .map { preferences -> preferences[NEXTCLOUD_REMOTE_FOLDER] ?: "Trainable/Backups" }

    val nextcloudWifiOnly: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[NEXTCLOUD_WIFI_ONLY] ?: false }

    val nextcloudAutoBackupFrequency: Flow<Int> = dataStore.data
        .map { preferences -> preferences[NEXTCLOUD_AUTO_BACKUP_FREQUENCY] ?: 1 }

    val nextcloudAutoBackupMaxCount: Flow<Int> = dataStore.data
        .map { preferences -> preferences[NEXTCLOUD_AUTO_BACKUP_MAX_COUNT] ?: 5 }

    val nextcloudAutoBackupIncludeImages: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[NEXTCLOUD_AUTO_BACKUP_INCLUDE_IMAGES] ?: false }

    suspend fun setNextcloudConfig(
        serverUrl: String,
        username: String,
        encryptedPasswordHex: String,
        passwordIvHex: String,
        remoteFolder: String
    ) {
        dataStore.edit { preferences ->
            preferences[NEXTCLOUD_SERVER_URL] = serverUrl
            preferences[NEXTCLOUD_USERNAME] = username
            preferences[NEXTCLOUD_ENCRYPTED_PASSWORD] = encryptedPasswordHex
            preferences[NEXTCLOUD_PASSWORD_IV] = passwordIvHex
            preferences[NEXTCLOUD_REMOTE_FOLDER] = remoteFolder
            preferences[NEXTCLOUD_BACKUP_ENABLED] = true
        }
    }

    suspend fun clearNextcloudConfig() {
        dataStore.edit { preferences ->
            preferences.remove(NEXTCLOUD_SERVER_URL)
            preferences.remove(NEXTCLOUD_USERNAME)
            preferences.remove(NEXTCLOUD_ENCRYPTED_PASSWORD)
            preferences.remove(NEXTCLOUD_PASSWORD_IV)
            preferences.remove(NEXTCLOUD_REMOTE_FOLDER)
            preferences.remove(NEXTCLOUD_AUTO_BACKUP_FREQUENCY)
            preferences.remove(NEXTCLOUD_AUTO_BACKUP_MAX_COUNT)
            preferences.remove(NEXTCLOUD_AUTO_BACKUP_INCLUDE_IMAGES)
            preferences.remove(NEXTCLOUD_WIFI_ONLY)
            preferences[NEXTCLOUD_BACKUP_ENABLED] = false
            preferences[NEXTCLOUD_AUTO_BACKUP_ENABLED] = false
        }
    }

    suspend fun setNextcloudBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NEXTCLOUD_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun setNextcloudAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NEXTCLOUD_AUTO_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun setNextcloudWifiOnly(wifiOnly: Boolean) {
        dataStore.edit { preferences ->
            preferences[NEXTCLOUD_WIFI_ONLY] = wifiOnly
        }
    }

    suspend fun setNextcloudRemoteFolder(folder: String) {
        dataStore.edit { preferences ->
            preferences[NEXTCLOUD_REMOTE_FOLDER] = folder
        }
    }

    suspend fun setNextcloudAutoBackupFrequency(frequency: Int) {
        dataStore.edit { preferences ->
            preferences[NEXTCLOUD_AUTO_BACKUP_FREQUENCY] = frequency
        }
    }

    suspend fun setNextcloudAutoBackupMaxCount(maxCount: Int) {
        dataStore.edit { preferences ->
            preferences[NEXTCLOUD_AUTO_BACKUP_MAX_COUNT] = maxCount
        }
    }

    suspend fun setNextcloudAutoBackupIncludeImages(includeImages: Boolean) {
        dataStore.edit { preferences ->
            preferences[NEXTCLOUD_AUTO_BACKUP_INCLUDE_IMAGES] = includeImages
        }
    }
}
