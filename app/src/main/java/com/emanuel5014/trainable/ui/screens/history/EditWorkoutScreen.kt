package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/** Edit workout on Android: the shared [EditWorkoutContent] on the Hilt ViewModel's model. */
@Composable
fun EditWorkoutScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditWorkoutViewModel = hiltViewModel()
) {
    EditWorkoutContent(onNavigateBack = onNavigateBack, model = viewModel.model)
}
