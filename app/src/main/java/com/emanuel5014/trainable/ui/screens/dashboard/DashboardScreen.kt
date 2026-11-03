package com.emanuel5014.trainable.ui.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.emanuel5014.trainable.util.BiometricHelper

/** The Android Dashboard: the shared [DashboardContent] on the state of [DashboardViewModel]. */
@Composable
fun DashboardScreen(
    isActive: Boolean = true,
    onNavigateToSettings: () -> Unit,
    onNavigateToWorkout: (planId: Int?, sessionId: Int?) -> Unit,
    onNavigateToQuickWorkout: (name: String?) -> Unit,
    onNavigateToPhysicalChecks: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val biometricEnabled by viewModel.physicalCheckBiometricEnabled.collectAsState(initial = false)

    val handleNavigateToPhysicalChecks = {
        if (biometricEnabled) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                BiometricHelper.checkAndShowBiometricPrompt(
                    activity = activity,
                    onSuccess = { onNavigateToPhysicalChecks() }
                )
            } else {
                onNavigateToPhysicalChecks()
            }
        } else {
            onNavigateToPhysicalChecks()
        }
    }
    val uiState by viewModel.uiState.collectAsState()
    // The app can stay in memory for days: "today" and the week must follow the calendar, not the launch
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshToday() }

    DashboardContent(
        uiState = uiState,
        actions = viewModel,
        isActive = isActive,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToWorkout = onNavigateToWorkout,
        onNavigateToQuickWorkout = onNavigateToQuickWorkout,
        onNavigateToPhysicalChecks = handleNavigateToPhysicalChecks
    )
}
