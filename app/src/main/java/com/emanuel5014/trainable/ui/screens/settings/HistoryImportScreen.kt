package com.emanuel5014.trainable.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R

private val PICKER_TYPES = arrayOf(
    "text/csv", "text/comma-separated-values", "text/plain", "application/csv",
    "application/vnd.ms-excel", "application/octet-stream", "*/*"
)

/** Settings -> Import CSV on Android: the shared [HistoryImportContent] with the system's document pickers. */
@Composable
fun HistoryImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryImportViewModel = hiltViewModel()
) {
    val language by viewModel.languageCode.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::load)
    }

    val context = LocalContext.current
    val exampleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            viewModel.saveExample(it) { error ->
                val message = if (error == null) context.getString(R.string.import_history_template_saved)
                else context.getString(R.string.import_history_template_error, error)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    HistoryImportContent(
        model = viewModel.model,
        language = language,
        onChoose = { picker.launch(PICKER_TYPES) },
        onDownloadExample = { exampleLauncher.launch("Trainable_import_example.csv") },
        onNavigateBack = onNavigateBack
    )
}
