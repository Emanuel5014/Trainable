package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.runtime.*
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.util.TimerAdjustment

/**
 * In-progress choices survive page changes; no profile is saved until Finish.
 * Every default is the preference's own default, so an untouched choice writes what the app would use anyway.
 */
@Stable
class OnboardingChoices {
    val username = mutableStateOf("")
    val weightInput = mutableStateOf("")
    val weightUnit = mutableStateOf("kg")
    val weeklyGoalInput = mutableStateOf("3")
    val membershipExpiryDate = mutableStateOf<Long?>(null)
    val style = mutableStateOf(OnboardingStyle.Simple)

    // Program
    val rpeEnabled = mutableStateOf(false)
    val rpeScale = mutableStateOf(RpeScale.BASIC)
    val startingWeight = mutableStateOf(false)
    val setPerformance = mutableStateOf(false)
    val advancedProgramming = mutableStateOf(false)
    val inlineModifications = mutableStateOf(false)
    val loadRounding = mutableFloatStateOf(LoadCalculator.DEFAULT_INCREMENT_KG)
    /** True once the user picked a rounding step; until then it follows the unit's default. */
    val loadRoundingPicked = mutableStateOf(false)

    // Timers
    val restTimerEnabled = mutableStateOf(true)
    val warmupTimerEnabled = mutableStateOf(false)
    val workoutTimerEnabled = mutableStateOf(false)
    val timerAddEnabled = mutableStateOf(true)
    val timerAddSeconds = mutableIntStateOf(TimerAdjustment.DEFAULT_ADD_SECONDS)
    val timerSubtractEnabled = mutableStateOf(true)
    val timerSubtractSeconds = mutableIntStateOf(TimerAdjustment.DEFAULT_SUBTRACT_SECONDS)
    val keepScreenOnSetTimer = mutableStateOf(true)
    val keepScreenOnCardioTimer = mutableStateOf(true)
    val autoStopCardioAtTarget = mutableStateOf(true)
    val autoStopTimeWeightAtTarget = mutableStateOf(true)

    // Exercises and gestures
    val exerciseMedia = mutableStateOf(false)
    val exerciseMediaLarge = mutableStateOf(true)
    val plateCalculator = mutableStateOf(false)
    val editablePresets = mutableStateOf(false)
    val swipeActionsEnabled = mutableStateOf(true)
    val hapticEnabled = mutableStateOf(true)

    // Journal
    val journalEnabled = mutableStateOf(false)
    val journalPromptAtFinish = mutableStateOf(true)
    val journalRating = mutableStateOf(true)

    // Alerts
    val timerNotificationsEnabled = mutableStateOf(true)
    val gymMembershipExpiryNotificationsEnabled = mutableStateOf(false)
    val gymMembershipExpiryNotificationDaysBefore = mutableIntStateOf(3)
    val timerFinishedLockscreenVibrationDuration = mutableIntStateOf(30)

    // AI
    val aiScanEnabled = mutableStateOf(false)

    // Your data
    val autoBackupEnabled = mutableStateOf(false)
    val autoBackupFrequency = mutableIntStateOf(1)
    val autoBackupFolderUri = mutableStateOf<String?>(null)
    val autoBackupMaxCount = mutableIntStateOf(5)
    val autoBackupIncludeImages = mutableStateOf(false)
    val nextcloudAutoBackupEnabled = mutableStateOf(true)
    val nextcloudAutoBackupFrequency = mutableIntStateOf(1)
    val nextcloudAutoBackupMaxCount = mutableIntStateOf(5)
    val nextcloudAutoBackupIncludeImages = mutableStateOf(false)
    val nextcloudWifiOnly = mutableStateOf(true)
}
