package com.emanuel5014.trainable.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.emanuel5014.trainable.domain.session.DeloadRules
import androidx.datastore.preferences.core.stringPreferencesKey
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.data.model.GlassSettings
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.data.model.ThemeSettings
import com.emanuel5014.trainable.data.model.ThemeSource
import com.emanuel5014.trainable.domain.exercise.ExerciseTypeMemory
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.util.PlateCalculator
import com.emanuel5014.trainable.util.RestSeconds
import com.emanuel5014.trainable.util.TimerAdjustment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The app's settings, stored in the "settings" preferences file that each platform opens. */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        const val DEFAULT_MAX_HEART_RATE = 190
        const val MIN_MAX_HEART_RATE = 120
        const val MAX_MAX_HEART_RATE = 230
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FREQUENCY = intPreferencesKey("auto_backup_frequency")
        val AUTO_BACKUP_FOLDER_URI = stringPreferencesKey("auto_backup_folder_uri")
        val AUTO_BACKUP_MAX_COUNT = intPreferencesKey("auto_backup_max_count")
        val AUTO_BACKUP_INCLUDE_IMAGES = booleanPreferencesKey("auto_backup_include_images")
        /** Last successful automatic backup (epoch millis): the iOS catch-up uses it. Device state, never in a backup. */
        val AUTO_BACKUP_LAST_RUN = androidx.datastore.preferences.core.longPreferencesKey("auto_backup_last_run")
        val NEXTCLOUD_AUTO_BACKUP_LAST_RUN = androidx.datastore.preferences.core.longPreferencesKey("nextcloud_auto_backup_last_run")
        val USER_LANGUAGE = stringPreferencesKey("user_language")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        /** "km" or "mi": only what the screens show and take; distances are always stored in km. */
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val FLOATING_NAV_BAR = booleanPreferencesKey("floating_nav_bar")
        val NAV_BAR_STYLE = intPreferencesKey("nav_bar_style")
        val LIQUID_GLASS_ENABLED = booleanPreferencesKey("liquid_glass_enabled")
        val GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        val GLASS_BLUR = floatPreferencesKey("glass_blur")
        val GLASS_STRETCH = booleanPreferencesKey("glass_stretch")
        val GLASS_JIGGLE = booleanPreferencesKey("glass_jiggle")
        val GLASS_NAV_BAR = booleanPreferencesKey("glass_nav_bar")
        val GLASS_HEADER_BUTTONS = booleanPreferencesKey("glass_header_buttons")
        val GLASS_ACTION_BUTTONS = booleanPreferencesKey("glass_action_buttons")
        val GLASS_WORKOUT_PANEL = booleanPreferencesKey("glass_workout_panel")
        val GLASS_OVERLAYS = booleanPreferencesKey("glass_overlays")
        val GLASS_SWITCHES = booleanPreferencesKey("glass_switches")
        val GLASS_TABS = booleanPreferencesKey("glass_tabs")
        val GLASS_SEARCH_BARS = booleanPreferencesKey("glass_search_bars")
        val GLASS_SLIDERS = booleanPreferencesKey("glass_sliders")
        val GLASS_WORKOUT_NAV = booleanPreferencesKey("glass_workout_nav")

        fun glassPartKey(part: GlassPart) = when (part) {
            GlassPart.NavBar -> GLASS_NAV_BAR
            GlassPart.HeaderButtons -> GLASS_HEADER_BUTTONS
            GlassPart.ActionButtons -> GLASS_ACTION_BUTTONS
            GlassPart.WorkoutPanel -> GLASS_WORKOUT_PANEL
            GlassPart.Overlays -> GLASS_OVERLAYS
            GlassPart.Switches -> GLASS_SWITCHES
            GlassPart.Tabs -> GLASS_TABS
            GlassPart.SearchBars -> GLASS_SEARCH_BARS
            GlassPart.Sliders -> GLASS_SLIDERS
            GlassPart.WorkoutNav -> GLASS_WORKOUT_NAV
        }
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DYNAMIC_COLOR_SEED = intPreferencesKey("dynamic_color_seed")
        val THEME_PALETTE = intPreferencesKey("theme_palette")
        val THEME_STYLE = intPreferencesKey("theme_style")
        // Themes section (2026-11): unset for a theme chosen before it, see ThemeSettings
        val THEME_SOURCE = intPreferencesKey("theme_source")
        val THEME_PHOTO_SEED = intPreferencesKey("theme_photo_seed")
        val THEME_PHOTO_COLORS = stringPreferencesKey("theme_photo_colors")
        val THEME_CUSTOM_SEED = intPreferencesKey("theme_custom_seed")
        val THEME_SECONDARY_HUE = intPreferencesKey("theme_secondary_hue")
        val THEME_TERTIARY_HUE = intPreferencesKey("theme_tertiary_hue")
        val THEME_NEUTRAL_TINT = intPreferencesKey("theme_neutral_tint")
        val THEME_CONTRAST = intPreferencesKey("theme_contrast")
        val THEME_PURE_BLACK = booleanPreferencesKey("theme_pure_black")
        val THEME_SAVED = stringPreferencesKey("theme_saved")

        /** Every theme preference read at once; the splash screens read the raw preferences with it. */
        fun themeSettings(preferences: Preferences): ThemeSettings = ThemeSettings(
            source = ThemeSource.fromId(preferences[THEME_SOURCE]),
            dynamicColor = preferences[DYNAMIC_COLOR] ?: true,
            dynamicColorSeed = preferences[DYNAMIC_COLOR_SEED],
            palette = preferences[THEME_PALETTE] ?: 0,
            style = preferences[THEME_STYLE] ?: 0,
            photoSeed = preferences[THEME_PHOTO_SEED],
            photoColors = ThemeSettings.decodeColors(preferences[THEME_PHOTO_COLORS]),
            customSeed = preferences[THEME_CUSTOM_SEED],
            secondaryHue = preferences[THEME_SECONDARY_HUE],
            tertiaryHue = preferences[THEME_TERTIARY_HUE],
            neutralTint = preferences[THEME_NEUTRAL_TINT],
            contrast = (preferences[THEME_CONTRAST] ?: 0).coerceIn(0, 2),
            pureBlack = preferences[THEME_PURE_BLACK] ?: false,
            saved = ThemeSettings.decodeSaved(preferences[THEME_SAVED])
        )
        val TIMER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("timer_notifications_enabled")
        val SWIPE_ACTIONS_ENABLED = booleanPreferencesKey("swipe_actions_enabled")
        /** Routines screen: the "your week" card folded into one line. */
        val ROUTINES_WEEK_COLLAPSED = booleanPreferencesKey("routines_week_collapsed")
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
    /** The remote AI profiles as one JSON list (see `AiProfileList`); never exported with the app's backup. */
    val AI_PROFILES = stringPreferencesKey("ai_profiles")
    /** "local" or the id of the profile the scan runs on. */
    val AI_ACTIVE_BACKEND = stringPreferencesKey("ai_active_backend")
    val AUTO_STOP_CARDIO_AT_TARGET = booleanPreferencesKey("auto_stop_cardio_at_target")
    val AUTO_STOP_TIME_WEIGHT_AT_TARGET = booleanPreferencesKey("auto_stop_time_weight_at_target")
    /** A timed set stopped at its target is logged, rests, and the next timed set starts by itself (issue #65). */
    val AUTO_ADVANCE_TIMED_SETS = booleanPreferencesKey("auto_advance_timed_sets")
    /** Master switch for %1RM / technique / weekly-program features. Off by default. */
    val ADVANCED_PROGRAMMING_ENABLED = booleanPreferencesKey("advanced_programming_enabled")
    /** Shows the user's own image/GIF of each exercise during a workout. Off by default. */
    val EXERCISE_MEDIA_ENABLED = booleanPreferencesKey("exercise_media_enabled")
    /** With exercise media on: also fill the free space under the sets with a large preview when there is room. */
    val EXERCISE_MEDIA_LARGE = booleanPreferencesKey("exercise_media_large")
    /** Shows the machine setup of each exercise above its sets, or a chip to add one (issue #67). On by default. */
    val EXERCISE_SETUP_VISIBLE = booleanPreferencesKey("exercise_setup_visible")
    /** Master switch for the plate calculator (which plates go on each side of the bar). Off by default. */
    val PLATE_CALCULATOR_ENABLED = booleanPreferencesKey("plate_calculator_enabled")
    /**
     * Starting weights: an exercise of a routine can carry a weight its sets start from, and an exercise with no
     * workout to copy from in the routine starts at the last weight used for it elsewhere. Off by default.
     */
    val STARTING_WEIGHT_ENABLED = booleanPreferencesKey("starting_weight_enabled")
    /**
     * Where the reps of a routine exercise start when a workout opens: false = the routine's reps, true = the reps done
     * in the last workout of the routine (when there is one). On by default: what was really done last time wins.
     */
    val REPS_FROM_LAST_WORKOUT = booleanPreferencesKey("reps_from_last_workout")
    /** Deload workouts: the share (40..100%) of the last normal workout's weight they propose. 60 by default. */
    val DELOAD_WEIGHT_PERCENT = intPreferencesKey("deload_weight_percent")
    /** Deload workouts count as days trained (weekly goal, widgets, training days). On by default. */
    val DELOAD_COUNTS_FOR_GOAL = booleanPreferencesKey("deload_counts_for_goal")
    /** The workout screen shows how long the workout has run (h:mm:ss) under its name. Off by default; the dashboard always does. */
    val SHOW_WORKOUT_ELAPSED = booleanPreferencesKey("show_workout_elapsed")
    /** The top of the routine screen (photos, note, muscle groups, big day pills) is open. Compact by default. */
    val ROUTINE_HEADER_EXPANDED = booleanPreferencesKey("routine_header_expanded")
    /**
     * Set details: mini-sets after a short pause, assisted and partial reps and the technique a set was really done
     * with. Off by default, sets then carry only their note.
     */
    val SET_PERFORMANCE_ENABLED = booleanPreferencesKey("set_performance_enabled")
    /**
     * Workout journal: a rating, tags and a note on each whole workout. Off by default; off hides it everywhere
     * (the data is kept).
     */
    val JOURNAL_ENABLED = booleanPreferencesKey("journal_enabled")
    /** With the journal on: the journal is offered when a workout is finished. On by default. */
    val JOURNAL_PROMPT_AT_FINISH = booleanPreferencesKey("journal_prompt_at_finish")
    /** With the journal on: workouts can be rated with 1 to 5 stars. On by default. */
    val JOURNAL_RATING_ENABLED = booleanPreferencesKey("journal_rating_enabled")
    /** Heart rate and calories of the watch or band (Health Connect) are read into each workout. Off by default. */
    val HEART_RATE_ENABLED = booleanPreferencesKey("heart_rate_enabled")
    /** The lifter's maximum heart rate, for the heart-rate zones. */
    val MAX_HEART_RATE = intPreferencesKey("max_heart_rate")
    /** The share card of a workout shows its heart rate (when it has one). On by default. */
    val SHARE_INCLUDE_HEART = booleanPreferencesKey("share_include_heart")
    /** Plates the gym has, comma separated and heaviest first, one list per weight unit. */
    val PLATE_CALCULATOR_PLATES_KG = stringPreferencesKey("plate_calculator_plates_kg")
    val PLATE_CALCULATOR_PLATES_LB = stringPreferencesKey("plate_calculator_plates_lb")
    /** Seconds the + and - buttons of the rest and warmup timers move the countdown by. */
    val TIMER_ADD_SECONDS = intPreferencesKey("timer_add_seconds")
    val TIMER_SUBTRACT_SECONDS = intPreferencesKey("timer_subtract_seconds")
    /** Whether the rest and warmup timers (and their notifications) show the - / + buttons and the skip button. */
    val TIMER_SHOW_TIME_BUTTONS = booleanPreferencesKey("timer_show_time_buttons")
    val TIMER_SHOW_SKIP_BUTTON = booleanPreferencesKey("timer_show_skip_button")
    /** The "Set done" button of the finished-rest notification / Live Activity. On by default. */
    val TIMER_SHOW_NEXT_SET_BUTTON = booleanPreferencesKey("timer_show_next_set_button")
    /** Workout mode of the dashboard: while a workout runs, a big panel with the rest and its buttons goes on top. */
    val DASHBOARD_WORKOUT_MODE = booleanPreferencesKey("dashboard_workout_mode")
    /** What the open workout last announced (current set, progress), so the dashboard can show it once the workout is closed. */
    val LIVE_WORKOUT_SNAPSHOT = stringPreferencesKey("live_workout_snapshot")
    /** Each of the two time buttons on its own: with both off the timers carry neither. On by default. */
    val TIMER_ADD_ENABLED = booleanPreferencesKey("timer_add_enabled")
    val TIMER_SUBTRACT_ENABLED = booleanPreferencesKey("timer_subtract_enabled")
    /** The rest countdown after each set of a workout. On by default; the warmup timer does not depend on it. */
    val REST_TIMER_ENABLED = booleanPreferencesKey("rest_timer_enabled")
    /**
     * Android updater: true = beta channel (pre-releases too), false = stable only, unset = follow the installed
     * version (a beta install stays on beta). Device state like the last backup run: never in a backup.
     */
    val UPDATE_CHANNEL_BETA = booleanPreferencesKey("update_channel_beta")
    /** Android updater: the release tag the user chose to skip; the start of the app stays quiet about it. Never in a backup. */
    val UPDATE_SKIPPED_TAG = stringPreferencesKey("update_skipped_tag")
    /** The last warmup the lifter started, in seconds: the warmup slider opens on it. */
    val LAST_WARMUP_SECONDS = intPreferencesKey("last_warmup_seconds")
    /** The last rest chosen when adding or swapping an exercise in a workout, in seconds: that slider opens on it. */
    val LAST_ADDED_EXERCISE_REST_SECONDS = intPreferencesKey("last_added_exercise_rest_seconds")
    /** The tracking type each exercise was last added with (see [ExerciseTypeMemory]): adding it again opens the selector on it. */
    val EXERCISE_TYPES = stringPreferencesKey("exercise_types")
    val LOAD_ROUNDING_KG = floatPreferencesKey("load_rounding_kg")
    val LOAD_ROUNDING_LB = floatPreferencesKey("load_rounding_lb")
    /** Old RPE setting (0 = advanced exercises only, 1 = every exercise, 2 = never): only [RpePreferenceMigration] and old backups read it. */
    val RPE_INPUT_MODE = intPreferencesKey("rpe_input_mode")
    /** Asks how hard each set felt (RPE), for every kind of exercise. Off by default. */
    val RPE_ENABLED = booleanPreferencesKey("rpe_enabled")
    /** The [com.emanuel5014.trainable.domain.rpe.RpeScale] RPE is asked in, by its id. */
    val RPE_SCALE = intPreferencesKey("rpe_scale")
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

    val autoBackupLastRun: Flow<Long?> = dataStore.data.map { preferences -> preferences[AUTO_BACKUP_LAST_RUN] }

    suspend fun setAutoBackupLastRun(millis: Long) {
        dataStore.edit { preferences -> preferences[AUTO_BACKUP_LAST_RUN] = millis }
    }

    /** The last successful automatic upload to Nextcloud (iOS decides what is due from it; never in a backup ZIP). */
    val nextcloudAutoBackupLastRun: Flow<Long?> = dataStore.data.map { preferences -> preferences[NEXTCLOUD_AUTO_BACKUP_LAST_RUN] }

    suspend fun setNextcloudAutoBackupLastRun(millis: Long) {
        dataStore.edit { preferences -> preferences[NEXTCLOUD_AUTO_BACKUP_LAST_RUN] = millis }
    }

    val userLanguage: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[USER_LANGUAGE]
        }

    val weightUnit: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[WEIGHT_UNIT] ?: "kg"
        }

    /** Never written for existing installs, so they keep km; onboarding picks miles with pounds. */
    val distanceUnit: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[DISTANCE_UNIT] ?: "km"
        }

    /** Falls back to the old floating/classic switch until the user picks one of the three styles. */
    val navBarStyle: Flow<NavBarStyle> = dataStore.data
        .map { preferences ->
            NavBarStyle.fromId(preferences[NAV_BAR_STYLE])
                ?: if (preferences[FLOATING_NAV_BAR] ?: true) NavBarStyle.Floating else NavBarStyle.Classic
        }

    val floatingNavBar: Flow<Boolean> = navBarStyle.map { it == NavBarStyle.Floating }

    /** Liquid Glass from Personalization: off by default, every part on once the user turns it on. */
    val glassSettings: Flow<GlassSettings> = dataStore.data
        .map { preferences ->
            GlassSettings(
                enabled = preferences[LIQUID_GLASS_ENABLED] ?: false,
                intensity = (preferences[GLASS_INTENSITY] ?: GlassSettings.DEFAULT_INTENSITY).coerceIn(0f, 1f),
                blur = (preferences[GLASS_BLUR] ?: GlassSettings.DEFAULT_BLUR).coerceIn(0f, 1f),
                stretch = preferences[GLASS_STRETCH] ?: true,
                jiggle = preferences[GLASS_JIGGLE] ?: true,
                parts = GlassPart.entries.filter { preferences[glassPartKey(it)] ?: true }.toSet()
            )
        }

    val themeSettings: Flow<ThemeSettings> = dataStore.data.map { themeSettings(it) }.distinctUntilChanged()

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

    val routinesWeekCollapsed: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[ROUTINES_WEEK_COLLAPSED] ?: false }

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

    val aiProfilesJson: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[AI_PROFILES] ?: ""
        }

    val aiActiveBackend: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[AI_ACTIVE_BACKEND] ?: "local"
        }

    val autoStopCardioAtTarget: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[AUTO_STOP_CARDIO_AT_TARGET] ?: true
        }

    val autoStopTimeWeightAtTarget: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[AUTO_STOP_TIME_WEIGHT_AT_TARGET] ?: true
        }

    val autoAdvanceTimedSets: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[AUTO_ADVANCE_TIMED_SETS] ?: false
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

    /** Both in one edit, so a deleted profile is never left as the active choice. */
    suspend fun setAiProfilesAndActive(profilesJson: String, activeId: String) {
        dataStore.edit { preferences ->
            preferences[AI_PROFILES] = profilesJson
            preferences[AI_ACTIVE_BACKEND] = activeId
        }
    }

    suspend fun setAiActiveBackend(id: String) {
        dataStore.edit { preferences ->
            preferences[AI_ACTIVE_BACKEND] = id
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

    val exerciseSetupVisible: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[EXERCISE_SETUP_VISIBLE] ?: true }

    suspend fun setExerciseSetupVisible(visible: Boolean) {
        dataStore.edit { preferences ->
            preferences[EXERCISE_SETUP_VISIBLE] = visible
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

    val restTimerEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[REST_TIMER_ENABLED] ?: true }

    suspend fun setRestTimerEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[REST_TIMER_ENABLED] = enabled
        }
    }

    /** The beta-updates choice, or null while the user never touched the switch (see [UPDATE_CHANNEL_BETA]). */
    val updateChannelBeta: Flow<Boolean?> = dataStore.data
        .map { preferences -> preferences[UPDATE_CHANNEL_BETA] }

    /** The release tag skipped with "Skip this version", or null (see [UPDATE_SKIPPED_TAG]). */
    val updateSkippedTag: Flow<String?> = dataStore.data
        .map { preferences -> preferences[UPDATE_SKIPPED_TAG] }

    suspend fun setUpdateSkippedTag(tag: String) {
        dataStore.edit { preferences ->
            preferences[UPDATE_SKIPPED_TAG] = tag
        }
    }

    suspend fun setUpdateChannelBeta(beta: Boolean) {
        dataStore.edit { preferences ->
            preferences[UPDATE_CHANNEL_BETA] = beta
        }
    }

    val lastWarmupSeconds: Flow<Int> = dataStore.data
        .map { preferences -> RestSeconds.snap(preferences[LAST_WARMUP_SECONDS], RestSeconds.WARMUP_STEPS) }

    suspend fun setLastWarmupSeconds(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[LAST_WARMUP_SECONDS] = RestSeconds.snap(seconds, RestSeconds.WARMUP_STEPS)
        }
    }

    val lastAddedExerciseRestSeconds: Flow<Int> = dataStore.data
        .map { preferences -> RestSeconds.snap(preferences[LAST_ADDED_EXERCISE_REST_SECONDS], RestSeconds.EXERCISE_STEPS) }

    suspend fun setLastAddedExerciseRestSeconds(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[LAST_ADDED_EXERCISE_REST_SECONDS] = RestSeconds.snap(seconds, RestSeconds.EXERCISE_STEPS)
        }
    }

    /** The type each exercise was last added with; the ones not listed are plain weight × reps. */
    val exerciseTypes: Flow<Map<Int, String>> = dataStore.data
        .map { preferences -> ExerciseTypeMemory.decode(preferences[EXERCISE_TYPES]) }

    /** Remembers [type] for [exerciseId]; read and written in one edit, so two quick saves lose neither. */
    suspend fun rememberExerciseType(exerciseId: Int, type: String) {
        dataStore.edit { preferences ->
            val current = ExerciseTypeMemory.decode(preferences[EXERCISE_TYPES])
            val next = ExerciseTypeMemory.remember(current, exerciseId, type)
            if (next != current) preferences[EXERCISE_TYPES] = ExerciseTypeMemory.encode(next)
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

    val timerShowNextSetButton: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[TIMER_SHOW_NEXT_SET_BUTTON] ?: true }

    suspend fun setTimerShowNextSetButton(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[TIMER_SHOW_NEXT_SET_BUTTON] = show
        }
    }

    val dashboardWorkoutMode: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[DASHBOARD_WORKOUT_MODE] ?: false }

    suspend fun setDashboardWorkoutMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DASHBOARD_WORKOUT_MODE] = enabled
        }
    }

    val liveWorkoutSnapshot: Flow<String?> = dataStore.data
        .map { preferences -> preferences[LIVE_WORKOUT_SNAPSHOT] }

    suspend fun setLiveWorkoutSnapshot(json: String?) {
        dataStore.edit { preferences ->
            if (json == null) preferences.remove(LIVE_WORKOUT_SNAPSHOT) else preferences[LIVE_WORKOUT_SNAPSHOT] = json
        }
    }

    val routineHeaderExpanded: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[ROUTINE_HEADER_EXPANDED] ?: false }

    suspend fun setRoutineHeaderExpanded(expanded: Boolean) {
        dataStore.edit { preferences ->
            preferences[ROUTINE_HEADER_EXPANDED] = expanded
        }
    }

    val startingWeightEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[STARTING_WEIGHT_ENABLED] ?: false }

    suspend fun setStartingWeightEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[STARTING_WEIGHT_ENABLED] = enabled
        }
    }

    val repsFromLastWorkout: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[REPS_FROM_LAST_WORKOUT] ?: true }

    suspend fun setRepsFromLastWorkout(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[REPS_FROM_LAST_WORKOUT] = enabled
        }
    }

    val deloadWeightPercent: Flow<Int> = dataStore.data
        .map { preferences -> DeloadRules.clampPercent(preferences[DELOAD_WEIGHT_PERCENT] ?: DeloadRules.DEFAULT_PERCENT) }

    suspend fun setDeloadWeightPercent(percent: Int) {
        dataStore.edit { preferences ->
            preferences[DELOAD_WEIGHT_PERCENT] = DeloadRules.clampPercent(percent)
        }
    }

    val deloadCountsForGoal: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[DELOAD_COUNTS_FOR_GOAL] ?: true }

    suspend fun setDeloadCountsForGoal(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DELOAD_COUNTS_FOR_GOAL] = enabled
        }
    }

    val showWorkoutElapsed: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[SHOW_WORKOUT_ELAPSED] ?: false }

    suspend fun setShowWorkoutElapsed(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_WORKOUT_ELAPSED] = enabled
        }
    }

    val setPerformanceEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[SET_PERFORMANCE_ENABLED] ?: false }

    suspend fun setSetPerformanceEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SET_PERFORMANCE_ENABLED] = enabled
        }
    }

    val journalEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[JOURNAL_ENABLED] ?: false }

    suspend fun setJournalEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[JOURNAL_ENABLED] = enabled
        }
    }

    val journalPromptAtFinish: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[JOURNAL_PROMPT_AT_FINISH] ?: true }

    suspend fun setJournalPromptAtFinish(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[JOURNAL_PROMPT_AT_FINISH] = enabled
        }
    }

    val journalRatingEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[JOURNAL_RATING_ENABLED] ?: true }

    suspend fun setJournalRatingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[JOURNAL_RATING_ENABLED] = enabled
        }
    }

    val heartRateEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[HEART_RATE_ENABLED] ?: false }

    suspend fun setHeartRateEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HEART_RATE_ENABLED] = enabled
        }
    }

    val maxHeartRate: Flow<Int> = dataStore.data
        .map { preferences -> preferences[MAX_HEART_RATE] ?: DEFAULT_MAX_HEART_RATE }

    suspend fun setMaxHeartRate(bpm: Int) {
        dataStore.edit { preferences ->
            preferences[MAX_HEART_RATE] = bpm.coerceIn(MIN_MAX_HEART_RATE, MAX_MAX_HEART_RATE)
        }
    }

    val shareIncludeHeart: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[SHARE_INCLUDE_HEART] ?: true }

    suspend fun setShareIncludeHeart(include: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHARE_INCLUDE_HEART] = include
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

    val rpeEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[RPE_ENABLED] ?: false }

    suspend fun setRpeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[RPE_ENABLED] = enabled
        }
    }

    val rpeScale: Flow<RpeScale> = dataStore.data
        .map { preferences -> RpeScale.fromId(preferences[RPE_SCALE]) }

    suspend fun setRpeScale(scale: RpeScale) {
        dataStore.edit { preferences ->
            preferences[RPE_SCALE] = scale.id
        }
    }

    suspend fun setAutoStopTimeWeightAtTarget(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_STOP_TIME_WEIGHT_AT_TARGET] = enabled
        }
    }

    suspend fun setAutoAdvanceTimedSets(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_ADVANCE_TIMED_SETS] = enabled
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

    suspend fun setDistanceUnit(unit: String) {
        dataStore.edit { preferences ->
            preferences[DISTANCE_UNIT] = unit
        }
    }

    suspend fun setNavBarStyle(style: NavBarStyle) {
        dataStore.edit { preferences ->
            preferences[NAV_BAR_STYLE] = style.id
            // Kept in sync for versions and backups that only know the floating/classic switch
            preferences[FLOATING_NAV_BAR] = style == NavBarStyle.Floating
        }
    }

    suspend fun setLiquidGlassEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LIQUID_GLASS_ENABLED] = enabled
        }
    }

    suspend fun setGlassStretch(on: Boolean) {
        dataStore.edit { preferences ->
            preferences[GLASS_STRETCH] = on
        }
    }

    suspend fun setGlassJiggle(on: Boolean) {
        dataStore.edit { preferences ->
            preferences[GLASS_JIGGLE] = on
        }
    }

    suspend fun setGlassBlur(blur: Float) {
        dataStore.edit { preferences ->
            preferences[GLASS_BLUR] = blur.coerceIn(0f, 1f)
        }
    }

    suspend fun setGlassIntensity(intensity: Float) {
        dataStore.edit { preferences ->
            preferences[GLASS_INTENSITY] = intensity.coerceIn(0f, 1f)
        }
    }

    suspend fun setGlassPart(part: GlassPart, on: Boolean) {
        dataStore.edit { preferences ->
            preferences[glassPartKey(part)] = on
        }
    }

    /**
     * Writes a theme from the Themes section, every key at once. The wallpaper switch follows the source, so a backup
     * opened by an older version still shows the wallpaper or palette part of it.
     */
    suspend fun setThemeSettings(theme: ThemeSettings) {
        dataStore.edit { p ->
            fun <T> put(key: Preferences.Key<T>, value: T?) { if (value != null) p[key] = value else p.remove(key) }
            put(THEME_SOURCE, theme.source?.id)
            p[DYNAMIC_COLOR] = theme.source?.let { it == ThemeSource.System } ?: theme.dynamicColor
            // Only ever the wallpaper colour picked under System: the photo and custom seeds have keys of their own
            put(DYNAMIC_COLOR_SEED, theme.dynamicColorSeed)
            p[THEME_PALETTE] = theme.palette
            p[THEME_STYLE] = theme.style
            put(THEME_PHOTO_SEED, theme.photoSeed)
            put(THEME_PHOTO_COLORS, theme.photoColors.takeIf { it.isNotEmpty() }?.let(ThemeSettings::encodeColors))
            put(THEME_CUSTOM_SEED, theme.customSeed)
            put(THEME_SECONDARY_HUE, theme.secondaryHue)
            put(THEME_TERTIARY_HUE, theme.tertiaryHue)
            put(THEME_NEUTRAL_TINT, theme.neutralTint)
            put(THEME_CONTRAST, theme.contrast.takeIf { it != 0 })
            put(THEME_PURE_BLACK, theme.pureBlack.takeIf { it })
            put(THEME_SAVED, theme.saved.takeIf { it.isNotEmpty() }?.let(ThemeSettings::encodeSaved))
        }
    }

    /** Onboarding writes the old keys; this makes them the ones that count again. */
    suspend fun clearThemeSource() {
        dataStore.edit { it.remove(THEME_SOURCE) }
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

    suspend fun setRoutinesWeekCollapsed(collapsed: Boolean) {
        dataStore.edit { preferences -> preferences[ROUTINES_WEEK_COLLAPSED] = collapsed }
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

    /**
     * The salt, validation block and device-wrapped password of the physical-check vault in one write, so a crash
     * never leaves a salt of one password next to the validation block of another. Null values are removed.
     */
    suspend fun setPhysicalCheckVaultKeys(
        salt: String?,
        validationBlock: String?,
        validationIv: String?,
        wrappedKey: String?,
        wrappedKeyIv: String?
    ) {
        dataStore.edit { preferences ->
            fun put(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String?) {
                if (value != null) preferences[key] = value else preferences.remove(key)
            }
            put(PHYSICAL_CHECK_ENCRYPTION_SALT, salt)
            put(PHYSICAL_CHECK_VALIDATION_BLOCK, validationBlock)
            put(PHYSICAL_CHECK_VALIDATION_IV, validationIv)
            put(PHYSICAL_CHECK_WRAPPED_KEY_KEYSTORE, wrappedKey)
            put(PHYSICAL_CHECK_WRAPPED_KEY_IV, wrappedKeyIv)
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
            preferences.remove(NEXTCLOUD_AUTO_BACKUP_LAST_RUN)
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
