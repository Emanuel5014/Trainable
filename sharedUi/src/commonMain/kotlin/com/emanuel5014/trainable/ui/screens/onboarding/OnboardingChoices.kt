package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.runtime.*

/** In-progress choices survive page changes; no profile is saved until Finish. */
@Stable
class OnboardingChoices {
    val username = mutableStateOf("")
    val weightInput = mutableStateOf("")
    val weightUnit = mutableStateOf("kg")
    val weeklyGoalInput = mutableStateOf("3")
    val hapticEnabled = mutableStateOf(true)
    val swipeActionsEnabled = mutableStateOf(true)
    val timerNotificationsEnabled = mutableStateOf(true)
    val gymMembershipExpiryNotificationsEnabled = mutableStateOf(false)
    val gymMembershipExpiryNotificationDaysBefore = mutableIntStateOf(3)
    val timerFinishedLockscreenVibrationDuration = mutableIntStateOf(30)
    val autoStopCardioAtTarget = mutableStateOf(true)
    val autoStopTimeWeightAtTarget = mutableStateOf(true)
    val autoBackupEnabled = mutableStateOf(false)
    val autoBackupFrequency = mutableIntStateOf(1)
    val autoBackupFolderUri = mutableStateOf<String?>(null)
    val autoBackupMaxCount = mutableIntStateOf(5)
    val autoBackupIncludeImages = mutableStateOf(false)
    val aiScanEnabled = mutableStateOf(false)
    val advancedProgramming = mutableStateOf(false)
    val nextcloudAutoBackupEnabled = mutableStateOf(true)
    val nextcloudAutoBackupFrequency = mutableIntStateOf(1)
    val nextcloudAutoBackupMaxCount = mutableIntStateOf(5)
    val nextcloudAutoBackupIncludeImages = mutableStateOf(false)
    val nextcloudWifiOnly = mutableStateOf(true)
}
