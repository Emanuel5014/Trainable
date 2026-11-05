package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun WorkoutExecutionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoutine: (Int) -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    CompositionLocalProvider(LocalWorkoutScreenPlatform provides AndroidWorkoutScreenPlatform) {
        WorkoutExecutionContent(onNavigateBack, onNavigateToRoutine, viewModel.model)
    }
}
