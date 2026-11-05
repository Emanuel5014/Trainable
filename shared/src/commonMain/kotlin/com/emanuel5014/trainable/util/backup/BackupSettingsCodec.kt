package com.emanuel5014.trainable.util.backup

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.emanuel5014.trainable.data.model.GlassPart
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.domain.exercise.ExerciseTypeMemory
import com.emanuel5014.trainable.domain.rpe.RpePreference
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.util.TimerAdjustment

/**
 * The settings in Trainable's existing Android backups. Import merges only the keys in the archive;
 * device secrets (wrapped vault keys, AI keys and Nextcloud credentials) never enter this format.
 * The Android caller supplies its original org.json implementation, including its coercion rules.
 */
object BackupSettingsCodec {
    fun membership(expiry: Long): String = """{"gym_membership_expiry_date":$expiry}"""

    fun membershipExpiry(json: String): Long? =
        """"gym_membership_expiry_date"\s*:\s*(\d+)""".toRegex()
            .find(json)?.groupValues?.get(1)?.toLongOrNull()

    fun appPreferences(prefs: Preferences, json: BackupJsonObject = KotlinxBackupJsonObject()): String {
        json.put("weight_unit", prefs[UserPreferencesRepository.WEIGHT_UNIT] ?: "kg")
        prefs[UserPreferencesRepository.USER_LANGUAGE]?.let { json.put("user_language", it) }
        json.put("swipe_actions_enabled", prefs[UserPreferencesRepository.SWIPE_ACTIONS_ENABLED] ?: true)
        json.put("dynamic_color", prefs[UserPreferencesRepository.DYNAMIC_COLOR] ?: true)
        prefs[UserPreferencesRepository.DYNAMIC_COLOR_SEED]?.let { json.put("dynamic_color_seed", it) }
        json.put("theme_palette", prefs[UserPreferencesRepository.THEME_PALETTE] ?: 0)
        json.put("theme_style", prefs[UserPreferencesRepository.THEME_STYLE] ?: 0)
        json.put("haptic_enabled", prefs[UserPreferencesRepository.HAPTIC_ENABLED] ?: true)
        json.put("weekly_goal", prefs[UserPreferencesRepository.WEEKLY_GOAL] ?: 3)
        json.put("floating_nav_bar", prefs[UserPreferencesRepository.FLOATING_NAV_BAR] ?: true)
        prefs[UserPreferencesRepository.NAV_BAR_STYLE]?.let { json.put("nav_bar_style", it) }
        prefs[UserPreferencesRepository.LIQUID_GLASS_ENABLED]?.let { json.put("liquid_glass_enabled", it) }
        prefs[UserPreferencesRepository.GLASS_INTENSITY]?.let { json.put("glass_intensity", it.toDouble()) }
        prefs[UserPreferencesRepository.GLASS_BLUR]?.let { json.put("glass_blur", it.toDouble()) }
        prefs[UserPreferencesRepository.GLASS_STRETCH]?.let { json.put("glass_stretch", it) }
        GlassPart.entries.forEach { part ->
            val key = UserPreferencesRepository.glassPartKey(part)
            prefs[key]?.let { json.put(key.name, it) }
        }
        json.put("theme_mode", prefs[UserPreferencesRepository.THEME_MODE] ?: 0)
        json.put("timer_notifications_enabled", prefs[UserPreferencesRepository.TIMER_NOTIFICATIONS_ENABLED] ?: true)
        json.put("warmup_timer_enabled", prefs[UserPreferencesRepository.WARMUP_TIMER_ENABLED] ?: true)
        json.put("gym_membership_expiry_notifications_enabled", prefs[UserPreferencesRepository.GYM_MEMBERSHIP_EXPIRY_NOTIFICATIONS_ENABLED] ?: true)
        json.put("gym_membership_expiry_notification_days_before", prefs[UserPreferencesRepository.GYM_MEMBERSHIP_EXPIRY_NOTIFICATION_DAYS_BEFORE] ?: 14)

        json.put("physical_check_encryption_enabled", prefs[UserPreferencesRepository.PHYSICAL_CHECK_ENCRYPTION_ENABLED] ?: false)
        prefs[UserPreferencesRepository.PHYSICAL_CHECK_ENCRYPTION_SALT]?.let { json.put("physical_check_encryption_salt", it) }
        prefs[UserPreferencesRepository.PHYSICAL_CHECK_VALIDATION_BLOCK]?.let { json.put("physical_check_validation_block", it) }
        prefs[UserPreferencesRepository.PHYSICAL_CHECK_VALIDATION_IV]?.let { json.put("physical_check_validation_iv", it) }

        json.put("editable_preset_exercises", prefs[UserPreferencesRepository.EDITABLE_PRESET_EXERCISES] ?: false)
        json.put("workout_timer_enabled", prefs[UserPreferencesRepository.WORKOUT_TIMER_ENABLED] ?: true)
        json.put("inline_exercise_modifications_enabled", prefs[UserPreferencesRepository.INLINE_EXERCISE_MODIFICATIONS_ENABLED] ?: true)
        json.put("timer_finished_lockscreen_vibration_duration", prefs[UserPreferencesRepository.TIMER_FINISHED_LOCKSCREEN_VIBRATION_DURATION] ?: 30)
        json.put("auto_stop_cardio_at_target", prefs[UserPreferencesRepository.AUTO_STOP_CARDIO_AT_TARGET] ?: true)
        json.put("auto_stop_time_weight_at_target", prefs[UserPreferencesRepository.AUTO_STOP_TIME_WEIGHT_AT_TARGET] ?: true)
        json.put("keep_screen_on_cardio_timer", prefs[UserPreferencesRepository.KEEP_SCREEN_ON_CARDIO_TIMER] ?: true)
        json.put("keep_screen_on_set_timer", prefs[UserPreferencesRepository.KEEP_SCREEN_ON_SET_TIMER] ?: true)
        prefs[UserPreferencesRepository.LOAD_ROUNDING_KG]?.let { json.put("load_rounding_kg", it.toDouble()) }
        prefs[UserPreferencesRepository.LOAD_ROUNDING_LB]?.let { json.put("load_rounding_lb", it.toDouble()) }
        json.put("rpe_enabled", prefs[UserPreferencesRepository.RPE_ENABLED] ?: false)
        json.put("rpe_scale", prefs[UserPreferencesRepository.RPE_SCALE] ?: RpeScale.BASIC.id)
        json.put("advanced_programming_enabled", prefs[UserPreferencesRepository.ADVANCED_PROGRAMMING_ENABLED] ?: false)
        json.put("exercise_media_enabled", prefs[UserPreferencesRepository.EXERCISE_MEDIA_ENABLED] ?: false)
        json.put("exercise_media_large", prefs[UserPreferencesRepository.EXERCISE_MEDIA_LARGE] ?: true)
        json.put("timer_add_seconds", TimerAdjustment.sanitizeAdd(prefs[UserPreferencesRepository.TIMER_ADD_SECONDS]))
        json.put("timer_subtract_seconds", TimerAdjustment.sanitizeSubtract(prefs[UserPreferencesRepository.TIMER_SUBTRACT_SECONDS]))
        json.put("timer_add_enabled", prefs[UserPreferencesRepository.TIMER_ADD_ENABLED] ?: true)
        json.put("timer_subtract_enabled", prefs[UserPreferencesRepository.TIMER_SUBTRACT_ENABLED] ?: true)
        json.put("rest_timer_enabled", prefs[UserPreferencesRepository.REST_TIMER_ENABLED] ?: true)
        prefs[UserPreferencesRepository.LAST_WARMUP_SECONDS]?.let { json.put("last_warmup_seconds", it) }
        prefs[UserPreferencesRepository.LAST_ADDED_EXERCISE_REST_SECONDS]?.let { json.put("last_added_exercise_rest_seconds", it) }
        prefs[UserPreferencesRepository.EXERCISE_TYPES]?.let { json.put("exercise_types", it) }
        json.put("timer_show_time_buttons", prefs[UserPreferencesRepository.TIMER_SHOW_TIME_BUTTONS] ?: true)
        json.put("timer_show_skip_button", prefs[UserPreferencesRepository.TIMER_SHOW_SKIP_BUTTON] ?: true)
        json.put("starting_weight_enabled", prefs[UserPreferencesRepository.STARTING_WEIGHT_ENABLED] ?: false)
        json.put("set_performance_enabled", prefs[UserPreferencesRepository.SET_PERFORMANCE_ENABLED] ?: false)
        json.put("plate_calculator_enabled", prefs[UserPreferencesRepository.PLATE_CALCULATOR_ENABLED] ?: false)
        prefs[UserPreferencesRepository.PLATE_CALCULATOR_PLATES_KG]?.let { json.put("plate_calculator_plates_kg", it) }
        prefs[UserPreferencesRepository.PLATE_CALCULATOR_PLATES_LB]?.let { json.put("plate_calculator_plates_lb", it) }
        json.put("journal_enabled", prefs[UserPreferencesRepository.JOURNAL_ENABLED] ?: false)
        json.put("journal_prompt_at_finish", prefs[UserPreferencesRepository.JOURNAL_PROMPT_AT_FINISH] ?: true)
        json.put("journal_rating_enabled", prefs[UserPreferencesRepository.JOURNAL_RATING_ENABLED] ?: true)

        json.put("ai_scan_enabled", prefs[UserPreferencesRepository.AI_SCAN_ENABLED] ?: false)
        json.put("ai_model_variant", prefs[UserPreferencesRepository.AI_MODEL_VARIANT] ?: "e2b")
        json.put("ai_resource_analytics_enabled", prefs[UserPreferencesRepository.AI_RESOURCE_ANALYTICS_ENABLED] ?: false)

        return json.encode()
    }

    /** Called inside a single DataStore edit, so a malformed value rolls back the entire edit. */
    fun restoreAppPreferences(jsonObject: BackupJsonObject, prefs: MutablePreferences) {
        if (jsonObject.has("weight_unit"))
            prefs[UserPreferencesRepository.WEIGHT_UNIT] = jsonObject.getString("weight_unit")
        if (jsonObject.has("user_language") && !jsonObject.isNull("user_language"))
            prefs[UserPreferencesRepository.USER_LANGUAGE] = jsonObject.getString("user_language")
        if (jsonObject.has("swipe_actions_enabled"))
            prefs[UserPreferencesRepository.SWIPE_ACTIONS_ENABLED] = jsonObject.getBoolean("swipe_actions_enabled")
        if (jsonObject.has("dynamic_color"))
            prefs[UserPreferencesRepository.DYNAMIC_COLOR] = jsonObject.getBoolean("dynamic_color")
        if (jsonObject.has("dynamic_color_seed") && !jsonObject.isNull("dynamic_color_seed"))
            prefs[UserPreferencesRepository.DYNAMIC_COLOR_SEED] = jsonObject.getInt("dynamic_color_seed")
        if (jsonObject.has("theme_palette"))
            prefs[UserPreferencesRepository.THEME_PALETTE] = jsonObject.getInt("theme_palette")
        if (jsonObject.has("theme_style"))
            prefs[UserPreferencesRepository.THEME_STYLE] = jsonObject.getInt("theme_style")
        if (jsonObject.has("haptic_enabled"))
            prefs[UserPreferencesRepository.HAPTIC_ENABLED] = jsonObject.getBoolean("haptic_enabled")
        if (jsonObject.has("weekly_goal"))
            prefs[UserPreferencesRepository.WEEKLY_GOAL] = jsonObject.getInt("weekly_goal")
        if (jsonObject.has("floating_nav_bar"))
            prefs[UserPreferencesRepository.FLOATING_NAV_BAR] = jsonObject.getBoolean("floating_nav_bar")
        if (jsonObject.has("nav_bar_style"))
            prefs[UserPreferencesRepository.NAV_BAR_STYLE] = jsonObject.getInt("nav_bar_style")
        else if (jsonObject.has("floating_nav_bar"))
            // Backup from before the third style: let the restored switch decide
            prefs.remove(UserPreferencesRepository.NAV_BAR_STYLE)
        if (jsonObject.has("liquid_glass_enabled"))
            prefs[UserPreferencesRepository.LIQUID_GLASS_ENABLED] = jsonObject.getBoolean("liquid_glass_enabled")
        GlassPart.entries.forEach { part ->
            val key = UserPreferencesRepository.glassPartKey(part)
            if (jsonObject.has(key.name)) prefs[key] = jsonObject.getBoolean(key.name)
        }
        if (jsonObject.has("glass_intensity"))
            prefs[UserPreferencesRepository.GLASS_INTENSITY] =
                jsonObject.getDouble("glass_intensity").toFloat().coerceIn(0f, 1f)
        if (jsonObject.has("glass_stretch"))
            prefs[UserPreferencesRepository.GLASS_STRETCH] = jsonObject.getBoolean("glass_stretch")
        if (jsonObject.has("glass_blur"))
            prefs[UserPreferencesRepository.GLASS_BLUR] =
                jsonObject.getDouble("glass_blur").toFloat().coerceIn(0f, 1f)
        if (jsonObject.has("theme_mode"))
            prefs[UserPreferencesRepository.THEME_MODE] = jsonObject.getInt("theme_mode")
        if (jsonObject.has("timer_notifications_enabled"))
            prefs[UserPreferencesRepository.TIMER_NOTIFICATIONS_ENABLED] = jsonObject.getBoolean("timer_notifications_enabled")
        if (jsonObject.has("warmup_timer_enabled"))
            prefs[UserPreferencesRepository.WARMUP_TIMER_ENABLED] = jsonObject.getBoolean("warmup_timer_enabled")
        if (jsonObject.has("gym_membership_expiry_notifications_enabled"))
            prefs[UserPreferencesRepository.GYM_MEMBERSHIP_EXPIRY_NOTIFICATIONS_ENABLED] = jsonObject.getBoolean("gym_membership_expiry_notifications_enabled")
        if (jsonObject.has("gym_membership_expiry_notification_days_before"))
            prefs[UserPreferencesRepository.GYM_MEMBERSHIP_EXPIRY_NOTIFICATION_DAYS_BEFORE] = jsonObject.getInt("gym_membership_expiry_notification_days_before")
        if (jsonObject.has("physical_check_encryption_enabled"))
            prefs[UserPreferencesRepository.PHYSICAL_CHECK_ENCRYPTION_ENABLED] = jsonObject.getBoolean("physical_check_encryption_enabled")
        if (jsonObject.has("physical_check_encryption_salt") && !jsonObject.isNull("physical_check_encryption_salt"))
            prefs[UserPreferencesRepository.PHYSICAL_CHECK_ENCRYPTION_SALT] = jsonObject.getString("physical_check_encryption_salt")
        if (jsonObject.has("physical_check_validation_block") && !jsonObject.isNull("physical_check_validation_block"))
            prefs[UserPreferencesRepository.PHYSICAL_CHECK_VALIDATION_BLOCK] = jsonObject.getString("physical_check_validation_block")
        if (jsonObject.has("physical_check_validation_iv") && !jsonObject.isNull("physical_check_validation_iv"))
            prefs[UserPreferencesRepository.PHYSICAL_CHECK_VALIDATION_IV] = jsonObject.getString("physical_check_validation_iv")
        if (jsonObject.has("editable_preset_exercises"))
            prefs[UserPreferencesRepository.EDITABLE_PRESET_EXERCISES] = jsonObject.getBoolean("editable_preset_exercises")
        if (jsonObject.has("auto_stop_cardio_at_target"))
            prefs[UserPreferencesRepository.AUTO_STOP_CARDIO_AT_TARGET] = jsonObject.getBoolean("auto_stop_cardio_at_target")
        if (jsonObject.has("auto_stop_time_weight_at_target"))
            prefs[UserPreferencesRepository.AUTO_STOP_TIME_WEIGHT_AT_TARGET] = jsonObject.getBoolean("auto_stop_time_weight_at_target")
        if (jsonObject.has("keep_screen_on_cardio_timer"))
            prefs[UserPreferencesRepository.KEEP_SCREEN_ON_CARDIO_TIMER] = jsonObject.getBoolean("keep_screen_on_cardio_timer")
        if (jsonObject.has("keep_screen_on_set_timer"))
            prefs[UserPreferencesRepository.KEEP_SCREEN_ON_SET_TIMER] = jsonObject.getBoolean("keep_screen_on_set_timer")
        if (jsonObject.has("load_rounding_kg"))
            prefs[UserPreferencesRepository.LOAD_ROUNDING_KG] = jsonObject.getDouble("load_rounding_kg").toFloat()
        if (jsonObject.has("load_rounding_lb"))
            prefs[UserPreferencesRepository.LOAD_ROUNDING_LB] = jsonObject.getDouble("load_rounding_lb").toFloat()
        if (jsonObject.has("rpe_enabled")) {
            prefs[UserPreferencesRepository.RPE_ENABLED] = jsonObject.getBoolean("rpe_enabled")
            if (jsonObject.has("rpe_scale"))
                prefs[UserPreferencesRepository.RPE_SCALE] = jsonObject.getInt("rpe_scale")
        } else {
            // A backup from before RPE had a switch of its own
            val rpe = RpePreference.fromLegacy(
                advancedProgramming = jsonObject.optBoolean("advanced_programming_enabled", false),
                legacyInputMode = if (jsonObject.has("rpe_input_mode")) jsonObject.getInt("rpe_input_mode") else null
            )
            prefs[UserPreferencesRepository.RPE_ENABLED] = rpe.enabled
            prefs[UserPreferencesRepository.RPE_SCALE] = rpe.scale.id
        }
        if (jsonObject.has("advanced_programming_enabled"))
            prefs[UserPreferencesRepository.ADVANCED_PROGRAMMING_ENABLED] = jsonObject.getBoolean("advanced_programming_enabled")
        if (jsonObject.has("exercise_media_enabled"))
            prefs[UserPreferencesRepository.EXERCISE_MEDIA_ENABLED] = jsonObject.getBoolean("exercise_media_enabled")
        if (jsonObject.has("exercise_media_large"))
            prefs[UserPreferencesRepository.EXERCISE_MEDIA_LARGE] = jsonObject.getBoolean("exercise_media_large")
        if (jsonObject.has("timer_add_seconds"))
            prefs[UserPreferencesRepository.TIMER_ADD_SECONDS] = TimerAdjustment.sanitizeAdd(jsonObject.getInt("timer_add_seconds"))
        if (jsonObject.has("timer_subtract_seconds"))
            prefs[UserPreferencesRepository.TIMER_SUBTRACT_SECONDS] = TimerAdjustment.sanitizeSubtract(jsonObject.getInt("timer_subtract_seconds"))
        if (jsonObject.has("timer_add_enabled"))
            prefs[UserPreferencesRepository.TIMER_ADD_ENABLED] = jsonObject.getBoolean("timer_add_enabled")
        if (jsonObject.has("timer_subtract_enabled"))
            prefs[UserPreferencesRepository.TIMER_SUBTRACT_ENABLED] = jsonObject.getBoolean("timer_subtract_enabled")
        if (jsonObject.has("rest_timer_enabled"))
            prefs[UserPreferencesRepository.REST_TIMER_ENABLED] = jsonObject.getBoolean("rest_timer_enabled")
        if (jsonObject.has("last_warmup_seconds"))
            prefs[UserPreferencesRepository.LAST_WARMUP_SECONDS] = jsonObject.getInt("last_warmup_seconds")
        if (jsonObject.has("last_added_exercise_rest_seconds"))
            prefs[UserPreferencesRepository.LAST_ADDED_EXERCISE_REST_SECONDS] = jsonObject.getInt("last_added_exercise_rest_seconds")
        if (jsonObject.has("exercise_types")) {
            // Read and written back, so a damaged value comes in as what could be read of it
            val types = ExerciseTypeMemory.decode(jsonObject.optString("exercise_types"))
            prefs[UserPreferencesRepository.EXERCISE_TYPES] = ExerciseTypeMemory.encode(types)
        }
        if (jsonObject.has("timer_show_time_buttons"))
            prefs[UserPreferencesRepository.TIMER_SHOW_TIME_BUTTONS] = jsonObject.getBoolean("timer_show_time_buttons")
        if (jsonObject.has("timer_show_skip_button"))
            prefs[UserPreferencesRepository.TIMER_SHOW_SKIP_BUTTON] = jsonObject.getBoolean("timer_show_skip_button")
        if (jsonObject.has("starting_weight_enabled"))
            prefs[UserPreferencesRepository.STARTING_WEIGHT_ENABLED] = jsonObject.getBoolean("starting_weight_enabled")
        if (jsonObject.has("set_performance_enabled"))
            prefs[UserPreferencesRepository.SET_PERFORMANCE_ENABLED] = jsonObject.getBoolean("set_performance_enabled")
        if (jsonObject.has("plate_calculator_enabled"))
            prefs[UserPreferencesRepository.PLATE_CALCULATOR_ENABLED] = jsonObject.getBoolean("plate_calculator_enabled")
        if (jsonObject.has("plate_calculator_plates_kg"))
            prefs[UserPreferencesRepository.PLATE_CALCULATOR_PLATES_KG] = jsonObject.getString("plate_calculator_plates_kg")
        if (jsonObject.has("plate_calculator_plates_lb"))
            prefs[UserPreferencesRepository.PLATE_CALCULATOR_PLATES_LB] = jsonObject.getString("plate_calculator_plates_lb")
        if (jsonObject.has("journal_enabled"))
            prefs[UserPreferencesRepository.JOURNAL_ENABLED] = jsonObject.getBoolean("journal_enabled")
        if (jsonObject.has("journal_prompt_at_finish"))
            prefs[UserPreferencesRepository.JOURNAL_PROMPT_AT_FINISH] = jsonObject.getBoolean("journal_prompt_at_finish")
        if (jsonObject.has("journal_rating_enabled"))
            prefs[UserPreferencesRepository.JOURNAL_RATING_ENABLED] = jsonObject.getBoolean("journal_rating_enabled")
    }

    fun analytics(values: BackupAnalyticsSettings, json: BackupJsonObject = KotlinxBackupJsonObject()): String? {
        if (values == BackupAnalyticsSettings()) return null
        values.selectedExerciseIds?.let { json.putStrings("selected_exercise_ids", it) }
        values.widgetOrder?.let { json.put("widget_order", it) }
        values.showProgressCards?.let { json.put("show_progress_cards", it) }
        values.categoryVolumeTimeRange?.let { json.put("category_volume_time_range", it) }
        return json.encode()
    }

    fun readAnalytics(json: BackupJsonObject): BackupAnalyticsSettings = BackupAnalyticsSettings(
        selectedExerciseIds = if (json.has("selected_exercise_ids")) json.getStrings("selected_exercise_ids") else null,
        widgetOrder = if (json.has("widget_order")) json.getString("widget_order") else null,
        showProgressCards = if (json.has("show_progress_cards")) json.getBoolean("show_progress_cards") else null,
        categoryVolumeTimeRange = if (json.has("category_volume_time_range")) json.getString("category_volume_time_range") else null
    )
}

/** Null means absent, while false and the empty set are present values in an archive. */
data class BackupAnalyticsSettings(
    val selectedExerciseIds: Set<String>? = null,
    val widgetOrder: String? = null,
    val showProgressCards: Boolean? = null,
    val categoryVolumeTimeRange: String? = null
)
