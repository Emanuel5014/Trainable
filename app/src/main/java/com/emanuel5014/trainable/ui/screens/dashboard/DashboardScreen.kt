package com.emanuel5014.trainable.ui.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** The Android Dashboard: the shared [DashboardContent] on the state of [DashboardViewModel]. */
@Composable
fun DashboardScreen(
    isActive: Boolean = true,
    onNavigateToSettings: () -> Unit,
    onNavigateToWorkout: (planId: Int?, sessionId: Int?) -> Unit,
    onNavigateToQuickWorkout: (name: String?) -> Unit,
    onNavigateToPhysicalChecks: () -> Unit,
    onLogSet: (planId: Int, sessionId: Int, token: String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
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
        // The section asks for the fingerprint itself (PhysicalCheckGate), once
        onNavigateToPhysicalChecks = onNavigateToPhysicalChecks,
        onLogSet = onLogSet
    )
}
