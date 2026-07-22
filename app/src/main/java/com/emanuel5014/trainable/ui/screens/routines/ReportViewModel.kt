package com.emanuel5014.trainable.ui.screens.routines

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.report.HtmlReportFormatter
import com.emanuel5014.trainable.data.report.ReportGenerator
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reportGenerator: ReportGenerator,
    private val htmlReportFormatter: HtmlReportFormatter,
    private val localeManager: AppLocaleManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val planIds: List<Int> = savedStateHandle.get<String>("planIdsString")
        ?.split(",")
        ?.mapNotNull { it.toIntOrNull() }
        ?: emptyList()

    private val _uiState = MutableStateFlow(ReportUiState(isLoading = true))
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        generateReport()
    }

    private fun generateReport() {
        viewModelScope.launch {
            try {
                val languageCode = localeManager.getResolvedLanguage()
                val reports = reportGenerator.generateReports(planIds, languageCode)
                val html = reports.joinToString("\n") { htmlReportFormatter.format(it, languageCode) }
                _uiState.update { it.copy(html = html, isLoading = false, reports = reports) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun shareReport() {
        val html = _uiState.value.html ?: return
        viewModelScope.launch {
            try {
                val cachePath = File(context.cacheDir, "reports")
                cachePath.mkdirs()
                val fileName = "trainable_report_${System.currentTimeMillis()}.html"
                val file = File(cachePath, fileName)
                file.writeText(html)

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "text/html"
                    putExtra(Intent.EXTRA_TITLE, "Trainable Report")
                }
                val chooser = Intent.createChooser(shareIntent, null)
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun saveReport(uri: Uri) {
        val html = _uiState.value.html ?: return
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(html.toByteArray())
                }
                _uiState.update { it.copy(saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}

data class ReportUiState(
    val isLoading: Boolean = true,
    val html: String? = null,
    val reports: List<com.emanuel5014.trainable.data.report.PlanReport> = emptyList(),
    val error: String? = null,
    val saveSuccess: Boolean = false
)
