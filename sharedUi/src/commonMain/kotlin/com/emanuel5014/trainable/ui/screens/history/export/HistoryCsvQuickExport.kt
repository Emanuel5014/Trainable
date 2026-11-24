package com.emanuel5014.trainable.ui.screens.history.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import kotlinx.coroutines.flow.first

/**
 * Saves the workouts the history shows as a CSV in one go: the button of the history's filter sheet. The file is the
 * one the export sheet writes, so it can be imported back.
 */
class HistoryCsvQuickExport internal constructor(
    private val deps: HistoryCsvExportDeps,
    private val languageCode: String,
    private val saver: CsvFileSaver
) {
    /** The file is being written. */
    var exporting by mutableStateOf(false)
        private set

    fun export(sessions: List<SessionWithDetails>, fileName: String, onSaved: () -> Unit = {}) {
        saver.save(fileName, {
            exporting = true
            try {
                deps.repository.exportWorkoutsToCsv(sessions, deps.preferences.weightUnit.first(), languageCode)
            } finally {
                exporting = false
            }
        }, onSaved)
    }
}

@Composable
fun rememberHistoryCsvQuickExport(deps: HistoryCsvExportDeps, languageCode: String, saver: CsvFileSaver): HistoryCsvQuickExport =
    remember(deps, languageCode, saver) { HistoryCsvQuickExport(deps, languageCode, saver) }
