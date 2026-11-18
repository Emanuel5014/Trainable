package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.data.report.HtmlReportFormatter
import com.emanuel5014.trainable.data.report.PlanReport
import com.emanuel5014.trainable.data.report.ReportGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val isLoading: Boolean = true,
    val html: String? = null,
    val reports: List<PlanReport> = emptyList(),
    val error: String? = null,
    val saveSuccess: Boolean = false
)

/**
 * The report of [planIds] as one HTML page, without the platform: Android's ReportViewModel and the iOS screen hold one
 * and write or share the page their own way. [languageCode] is the language the report is written in.
 */
class ReportModel(
    private val planIds: List<Int>,
    private val reportGenerator: ReportGenerator,
    private val htmlReportFormatter: HtmlReportFormatter,
    private val languageCode: suspend () -> String,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(ReportUiState(isLoading = true))
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        generateReport()
    }

    private fun generateReport() {
        scope.launch {
            try {
                val language = languageCode()
                val reports = reportGenerator.generateReports(planIds, language)
                // One page for all the routines: glued pages would be several documents in one file
                val html = htmlReportFormatter.format(reports, language)
                _uiState.update { it.copy(html = html, isLoading = false, reports = reports) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    /** The page was written where the user chose. */
    fun saved() = _uiState.update { it.copy(saveSuccess = true) }

    /** Saving or sharing failed with [message]. */
    fun failed(message: String?) = _uiState.update { it.copy(error = message) }

    fun clearSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }

    companion object {
        /** The routes carry the routines as "3,7,12". */
        fun planIdsOf(text: String?): List<Int> = text?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()

        /** "trainable_Push_A_<millis>.html": the first routine's name with only letters, digits, _ and -. */
        fun fileName(state: ReportUiState, millis: Long): String {
            val planName = state.reports.firstOrNull()?.planName ?: "report"
            val safeName = planName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            return "trainable_${safeName}_$millis.html"
        }
    }
}
