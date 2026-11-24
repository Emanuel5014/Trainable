package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.emanuel5014.trainable.ui.navigation.CompareSessions
import com.emanuel5014.trainable.ui.navigation.EditWorkoutSession
import com.emanuel5014.trainable.ui.screens.history.export.rememberAndroidCsvFileSaver
import com.emanuel5014.trainable.ui.screens.history.export.rememberHistoryCsvQuickExport

/** The shared history (HistoryContent) with Android's ViewModel, language, share card and routes. */
@Composable
fun HistoryScreen(
    navController: NavController? = null,
    isVisible: Boolean = true,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val languageCode by viewModel.languageCode.collectAsState()
    val exportDeps = hiltViewModel<HistoryCsvExportViewModel>().deps
    val csvExport = rememberHistoryCsvQuickExport(exportDeps, languageCode, rememberAndroidCsvFileSaver())
    HistoryContent(
        state = viewModel.state,
        languageCode = languageCode,
        share = rememberAndroidHistoryShareUi(),
        onEditSession = { sessionId -> navController?.navigate(EditWorkoutSession(sessionId)) },
        onCompareSessions = { first, second -> navController?.navigate(CompareSessions(first, second)) },
        isVisible = isVisible,
        csvExport = csvExport
    )
}
