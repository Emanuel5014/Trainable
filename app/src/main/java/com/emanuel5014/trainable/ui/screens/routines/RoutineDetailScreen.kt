package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/** The shared routine screen (RoutineDetailContent) with Android's ViewModel, language and AI scan. */
@Composable
fun RoutineDetailScreen(
    onNavigateBack: () -> Unit,
    onStartWorkout: (Int, Int?) -> Unit,
    viewModel: RoutineDetailViewModel = hiltViewModel()
) {
    val languageCode by viewModel.languageCode.collectAsState(initial = "en")
    RoutineDetailContent(
        state = viewModel.state,
        languageCode = languageCode,
        scan = rememberAndroidRoutineScanUi(viewModel),
        onNavigateBack = onNavigateBack,
        onStartWorkout = onStartWorkout
    )
}
