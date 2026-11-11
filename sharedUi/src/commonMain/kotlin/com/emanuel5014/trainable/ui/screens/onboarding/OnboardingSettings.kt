package com.emanuel5014.trainable.ui.screens.onboarding

import com.emanuel5014.trainable.domain.WeeklyGoal
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.rpe.RpeScale

/** The theme the user ended on; colour, style and mode are written live, so this comes from the preferences. */
data class ThemeChoice(
    val dynamicColor: Boolean,
    val dynamicColorSeed: Int?,
    val palette: Int,
    val style: Int,
    val mode: Int
)

/** Everything the setup writes at the end, taken once from [OnboardingChoices] when the user finishes. */
data class OnboardingSettings(
    val username: String,
    val initialWeight: Float,
    val weightUnit: String,
    val weeklyGoal: Int,
    val membershipExpiryDate: Long?,
    val rpeEnabled: Boolean,
    val rpeScale: RpeScale,
    val startingWeight: Boolean,
    val setPerformance: Boolean,
    val advancedProgramming: Boolean,
    val inlineModifications: Boolean,
    val loadRounding: Float,
    val restTimerEnabled: Boolean,
    val warmupTimerEnabled: Boolean,
    val workoutTimerEnabled: Boolean,
    val timerAddEnabled: Boolean,
    val timerAddSeconds: Int,
    val timerSubtractEnabled: Boolean,
    val timerSubtractSeconds: Int,
    val keepScreenOnSetTimer: Boolean,
    val keepScreenOnCardioTimer: Boolean,
    val autoStopCardioAtTarget: Boolean,
    val autoStopTimeWeightAtTarget: Boolean,
    val exerciseMedia: Boolean,
    val exerciseMediaLarge: Boolean,
    val plateCalculator: Boolean,
    val editablePresets: Boolean,
    val swipeActionsEnabled: Boolean,
    val hapticEnabled: Boolean,
    val journalEnabled: Boolean,
    val journalPromptAtFinish: Boolean,
    val journalRating: Boolean,
    val timerNotificationsEnabled: Boolean,
    val gymMembershipExpiryNotificationsEnabled: Boolean,
    val gymMembershipExpiryNotificationDaysBefore: Int,
    val timerFinishedLockscreenVibrationDuration: Int,
    val aiScanEnabled: Boolean,
    val aiModelVariant: String,
    val autoBackupEnabled: Boolean,
    val autoBackupFrequency: Int,
    val autoBackupFolderUri: String?,
    val autoBackupMaxCount: Int,
    val autoBackupIncludeImages: Boolean,
    val nextcloudAutoBackupEnabled: Boolean,
    val nextcloudAutoBackupFrequency: Int,
    val nextcloudAutoBackupMaxCount: Int,
    val nextcloudAutoBackupIncludeImages: Boolean,
    val nextcloudWifiOnly: Boolean,
    val theme: ThemeChoice
)

fun OnboardingChoices.toSettings(theme: ThemeChoice, aiModelVariant: String) = OnboardingSettings(
    username = username.value,
    initialWeight = weightInput.value.replace(',', '.').toFloatOrNull() ?: 0f,
    weightUnit = weightUnit.value,
    weeklyGoal = WeeklyGoal.parse(weeklyGoalInput.value),
    membershipExpiryDate = membershipExpiryDate.value,
    rpeEnabled = rpeEnabled.value,
    rpeScale = rpeScale.value,
    startingWeight = startingWeight.value,
    setPerformance = setPerformance.value,
    advancedProgramming = advancedProgramming.value,
    inlineModifications = inlineModifications.value,
    loadRounding = loadRounding.floatValue,
    restTimerEnabled = restTimerEnabled.value,
    warmupTimerEnabled = warmupTimerEnabled.value,
    workoutTimerEnabled = workoutTimerEnabled.value,
    timerAddEnabled = timerAddEnabled.value,
    timerAddSeconds = timerAddSeconds.intValue,
    timerSubtractEnabled = timerSubtractEnabled.value,
    timerSubtractSeconds = timerSubtractSeconds.intValue,
    keepScreenOnSetTimer = keepScreenOnSetTimer.value,
    keepScreenOnCardioTimer = keepScreenOnCardioTimer.value,
    autoStopCardioAtTarget = autoStopCardioAtTarget.value,
    autoStopTimeWeightAtTarget = autoStopTimeWeightAtTarget.value,
    exerciseMedia = exerciseMedia.value,
    exerciseMediaLarge = exerciseMediaLarge.value,
    plateCalculator = plateCalculator.value,
    editablePresets = editablePresets.value,
    swipeActionsEnabled = swipeActionsEnabled.value,
    hapticEnabled = hapticEnabled.value,
    journalEnabled = journalEnabled.value,
    journalPromptAtFinish = journalPromptAtFinish.value,
    journalRating = journalRating.value,
    timerNotificationsEnabled = timerNotificationsEnabled.value,
    gymMembershipExpiryNotificationsEnabled = gymMembershipExpiryNotificationsEnabled.value,
    gymMembershipExpiryNotificationDaysBefore = gymMembershipExpiryNotificationDaysBefore.intValue,
    timerFinishedLockscreenVibrationDuration = timerFinishedLockscreenVibrationDuration.intValue,
    aiScanEnabled = aiScanEnabled.value,
    aiModelVariant = aiModelVariant,
    autoBackupEnabled = autoBackupEnabled.value,
    autoBackupFrequency = autoBackupFrequency.intValue,
    autoBackupFolderUri = autoBackupFolderUri.value,
    autoBackupMaxCount = autoBackupMaxCount.intValue,
    autoBackupIncludeImages = autoBackupIncludeImages.value,
    nextcloudAutoBackupEnabled = nextcloudAutoBackupEnabled.value,
    nextcloudAutoBackupFrequency = nextcloudAutoBackupFrequency.intValue,
    nextcloudAutoBackupMaxCount = nextcloudAutoBackupMaxCount.intValue,
    nextcloudAutoBackupIncludeImages = nextcloudAutoBackupIncludeImages.value,
    nextcloudWifiOnly = nextcloudWifiOnly.value,
    theme = theme
)

/** A date also turns the expiry alert on (the user can turn it off on Alerts); clearing it leaves the alert alone. */
fun OnboardingChoices.pickMembershipDate(millis: Long?) {
    membershipExpiryDate.value = millis
    if (millis != null) gymMembershipExpiryNotificationsEnabled.value = true
}

/**
 * Changes the unit. Until the user picks a rounding step it follows the unit's default (2.5 kg / 5 lb); a picked step
 * the new unit doesn't offer falls back to that unit's default too.
 */
fun OnboardingChoices.changeUnit(unit: String) {
    weightUnit.value = unit
    val offered = if (unit == "lb") LoadCalculator.INCREMENTS_LB else LoadCalculator.INCREMENTS_KG
    if (!loadRoundingPicked.value || loadRounding.floatValue !in offered) {
        loadRounding.floatValue = LoadCalculator.defaultIncrement(unit)
        loadRoundingPicked.value = false
    }
}

/** The user chose a rounding step on Tweaks. */
fun OnboardingChoices.pickRounding(step: Float) {
    loadRounding.floatValue = step
    loadRoundingPicked.value = true
}

/**
 * Nextcloud was connected from the Welcome restore sheet only to fetch a backup: automatic uploads stay off unless
 * the user turns them on in the Data step (an empty profile uploaded daily would rotate their real backups away).
 */
fun OnboardingChoices.connectedForRestoreOnly() {
    nextcloudAutoBackupEnabled.value = false
}

/** The four groups of the Tweaks step. */
enum class TweakGroup { Program, Timers, ExercisesAndGestures, Journal }

/** The main switch of each row in [group]; sub-options (scale, steps, size, prompt, stars) are not rows. */
private fun OnboardingChoices.rows(group: TweakGroup): List<Boolean> = when (group) {
    TweakGroup.Program -> listOf(
        rpeEnabled.value, startingWeight.value, setPerformance.value, advancedProgramming.value,
        inlineModifications.value
    )
    TweakGroup.Timers -> listOf(
        restTimerEnabled.value, warmupTimerEnabled.value, workoutTimerEnabled.value, timerAddEnabled.value,
        timerSubtractEnabled.value, keepScreenOnSetTimer.value, keepScreenOnCardioTimer.value,
        autoStopCardioAtTarget.value, autoStopTimeWeightAtTarget.value
    )
    TweakGroup.ExercisesAndGestures -> listOf(
        exerciseMedia.value, plateCalculator.value, editablePresets.value, swipeActionsEnabled.value,
        hapticEnabled.value
    )
    TweakGroup.Journal -> listOf(journalEnabled.value)
}

fun OnboardingChoices.activeCount(group: TweakGroup): Int = rows(group).count { it }

fun OnboardingChoices.activeFeatureCount(): Int = TweakGroup.entries.sumOf { activeCount(it) }
