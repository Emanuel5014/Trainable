package com.emanuel5014.trainable.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun NotificationSettingsScreen(onNavigateBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val timerNotificationsEnabled by viewModel.timerNotificationsEnabled.collectAsState()
    val context = LocalContext.current
    var pendingNotificationType by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            when (pendingNotificationType) {
                "timer" -> viewModel.setTimerNotificationsEnabled(true)
                "membership" -> viewModel.setGymMembershipExpiryNotificationsEnabled(true)
            }
        } else {
            Toast.makeText(context, "Permission denied for notifications", Toast.LENGTH_SHORT).show()
        }
        pendingNotificationType = null
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted && timerNotificationsEnabled) {
                viewModel.setTimerNotificationsEnabled(false)
            }
        }
    }

    NotificationSettingsContent(
        onNavigateBack = onNavigateBack,
        state = viewModel.state,
        onTimerNotificationsChange = { enabled ->
            if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pendingNotificationType = "timer"
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setTimerNotificationsEnabled(enabled)
            }
        },
        onMembershipNotificationsChange = { enabled ->
            if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pendingNotificationType = "membership"
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setGymMembershipExpiryNotificationsEnabled(enabled)
            }
        },
        onMembershipDaysBeforeChange = viewModel::setGymMembershipExpiryNotificationDaysBefore
    )
}
