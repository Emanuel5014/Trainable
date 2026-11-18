package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.util.ShareUtils

/** The shared routines list (RoutineListContent) with Android's ViewModel and share sheet. */
@Composable
fun RoutineListScreen(
    onNavigateToDetail: (Int) -> Unit,
    onGenerateReport: (List<Int>) -> Unit = {},
    onSwipingItemChange: ((Boolean) -> Unit)? = null,
    isVisible: Boolean = true,
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val languageCode by viewModel.languageCode.collectAsState(initial = "en")
    val context = LocalContext.current
    RoutineListContent(
        state = viewModel.state,
        languageCode = languageCode,
        sharePlans = { ids, includeImages, includeMaximums, fileName ->
            ShareUtils.shareWorkoutPlans(context, viewModel.exportPlans(ids, includeImages, includeMaximums), fileName)
        },
        onNavigateToDetail = onNavigateToDetail,
        onGenerateReport = onGenerateReport,
        onSwipingItemChange = onSwipingItemChange,
        isVisible = isVisible
    )
}
