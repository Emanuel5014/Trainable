package com.emanuel5014.trainable.ui.screens.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.first

@Composable
fun WorkoutExecutionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoutine: (Int) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    TimerNotificationPermission(viewModel)
    CompositionLocalProvider(LocalWorkoutScreenPlatform provides AndroidWorkoutScreenPlatform) {
        WorkoutExecutionContent(onNavigateBack, onNavigateToRoutine, viewModel.model)
    }
}

/**
 * Android 13+: timer notifications on but no permission (a restored backup, or onboarding skipped) would stay silent.
 * Ask once when a workout opens; a refusal turns the setting off, as Settings does, so it is not asked again.
 */
@Composable
private fun TimerNotificationPermission(viewModel: WorkoutViewModel) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) viewModel.turnOffTimerNotifications()
    }
    LaunchedEffect(Unit) {
        val granted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted && viewModel.timerNotificationsEnabled.first()) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
