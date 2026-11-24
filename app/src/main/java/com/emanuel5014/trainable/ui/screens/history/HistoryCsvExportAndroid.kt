package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.ui.screens.history.export.HistoryCsvExportDeps
import com.emanuel5014.trainable.ui.screens.history.export.HistoryCsvExportHost
import com.emanuel5014.trainable.ui.screens.history.export.rememberAndroidCsvFileSaver
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What Android's CSV export reads and writes with, and the app's language for the exercise names and the file. */
@HiltViewModel
class HistoryCsvExportViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    preferences: UserPreferencesRepository,
    localeManager: AppLocaleManager
) : ViewModel() {
    val deps = HistoryCsvExportDeps(workoutRepository, preferences)

    private val _languageCode = MutableStateFlow<String?>(null)
    val languageCode: StateFlow<String?> = _languageCode.asStateFlow()

    init {
        viewModelScope.launch { _languageCode.value = localeManager.getResolvedLanguage() }
    }
}

/** The shared CSV export sheet with Android's repositories and file picker. */
@Composable
fun AndroidHistoryCsvExport(
    onDismiss: () -> Unit,
    viewModel: HistoryCsvExportViewModel = hiltViewModel()
) {
    val languageCode by viewModel.languageCode.collectAsState()
    val saver = rememberAndroidCsvFileSaver()
    val language = languageCode ?: return
    HistoryCsvExportHost(
        deps = viewModel.deps,
        languageCode = language,
        saver = saver,
        onDismiss = onDismiss
    )
}
