package com.emanuel5014.trainable.ui.screens.compare

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/** Compare sessions on Android: the shared [CompareSessionsContent] on the Hilt ViewModel's model. */
@Composable
fun CompareSessionsScreen(
    sessionId1: Int,
    sessionId2: Int,
    onNavigateBack: () -> Unit,
    viewModel: CompareSessionsViewModel = hiltViewModel()
) {
    CompareSessionsContent(sessionId1, sessionId2, onNavigateBack, viewModel.model)
}
