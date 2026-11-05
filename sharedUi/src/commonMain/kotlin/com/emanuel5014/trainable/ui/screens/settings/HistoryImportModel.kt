package com.emanuel5014.trainable.ui.screens.settings

import com.emanuel5014.trainable.data.repository.HistoryCsvImporter
import com.emanuel5014.trainable.data.repository.ImportResult
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.domain.csvimport.CsvColumn
import com.emanuel5014.trainable.domain.csvimport.CsvTextDecoder
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvReader
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvTemplate
import com.emanuel5014.trainable.domain.csvimport.HistoryImportPlanner
import com.emanuel5014.trainable.domain.csvimport.ImportOptions
import com.emanuel5014.trainable.domain.csvimport.ImportPlan
import com.emanuel5014.trainable.domain.csvimport.KnownData
import com.emanuel5014.trainable.domain.csvimport.ParsedHistory
import com.emanuel5014.trainable.domain.csvimport.ReadFailure
import com.emanuel5014.trainable.domain.csvimport.ReadResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Why a file could not be turned into a preview. */
sealed interface HistoryImportFailure {
    data object Empty : HistoryImportFailure
    data object NoRows : HistoryImportFailure
    data class NotAHistory(val missing: List<CsvColumn>) : HistoryImportFailure
    data object Unreadable : HistoryImportFailure
    data class TooLarge(val limitMb: Int) : HistoryImportFailure
}

sealed interface HistoryImportState {
    /** Nothing chosen yet: explains the format. */
    data object Idle : HistoryImportState

    data object Reading : HistoryImportState

    data class Failed(val failure: HistoryImportFailure) : HistoryImportState

    /** Everything the import would do, waiting for a go. */
    data class Preview(
        val fileName: String,
        val history: ParsedHistory,
        val options: ImportOptions,
        val plan: ImportPlan,
        /** The weight header named the unit ([ImportOptions.weightUnit] started from it). */
        val unitFromFile: Boolean,
        /** The last attempt to import failed with this, and nothing was saved. */
        val importError: String? = null
    ) : HistoryImportState

    /** What is being written, for the progress overlay. */
    data class Importing(val workouts: Int, val sets: Int) : HistoryImportState

    data class Done(val result: ImportResult, val undone: Boolean = false, val undoError: String? = null) : HistoryImportState
}

/** A file the platform's chooser handed over, read by the platform (it knows where the file lives). */
sealed interface PickedCsv {
    data class Read(val name: String, val bytes: ByteArray) : PickedCsv

    /** Over [HistoryImportModel.MAX_FILE_MB]: the platform stopped reading it. */
    data object TooLarge : PickedCsv

    data object Unreadable : PickedCsv
}

/**
 * Settings -> Import CSV, without the platform: reads a history (bytes the platform read), plans it against what the
 * app holds, imports it and can take it out again. Android's HistoryImportViewModel and the iOS screen hold one.
 */
class HistoryImportModel(
    private val importer: HistoryCsvImporter,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<HistoryImportState>(HistoryImportState.Idle)
    val state: StateFlow<HistoryImportState> = _state.asStateFlow()

    /** The unit weights are shown in. */
    val appWeightUnit: StateFlow<String> = userPreferencesRepository.weightUnit
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), "kg")

    /** The app's theme choice (0 system, 1 light, 2 dark), for the waiting screen. */
    val themeMode: StateFlow<Int> = userPreferencesRepository.themeMode
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), 0)

    private var history: ParsedHistory? = null
    private var known: KnownData? = null
    private var planJob: Job? = null

    /** Reads the file [read] gives (called off the main thread by the platform's own code) and plans its import. */
    fun load(read: suspend () -> PickedCsv) {
        _state.value = HistoryImportState.Reading
        scope.launch {
            val file = when (val picked = read()) {
                PickedCsv.Unreadable -> return@launch fail(HistoryImportFailure.Unreadable)
                PickedCsv.TooLarge -> return@launch fail(HistoryImportFailure.TooLarge(MAX_FILE_MB))
                is PickedCsv.Read -> picked
            }
            val result = withContext(Dispatchers.Default) { HistoryCsvReader.read(CsvTextDecoder.decode(file.bytes)) }
            when (result) {
                is ReadResult.Failure -> fail(
                    when (result.reason) {
                        ReadFailure.EMPTY -> HistoryImportFailure.Empty
                        ReadFailure.NO_ROWS -> HistoryImportFailure.NoRows
                        ReadFailure.NOT_A_HISTORY -> HistoryImportFailure.NotAHistory(result.missing)
                    }
                )
                is ReadResult.Success -> {
                    val parsed = result.history
                    val appUnit = userPreferencesRepository.weightUnit.first()
                    val options = ImportOptions(weightUnit = parsed.weightUnit ?: appUnit)
                    val data = try {
                        importer.loadKnown()
                    } catch (e: Exception) {
                        return@launch fail(HistoryImportFailure.Unreadable)
                    }
                    history = parsed
                    known = data
                    val plan = withContext(Dispatchers.Default) { HistoryImportPlanner.plan(parsed, data, options) }
                    _state.value = HistoryImportState.Preview(file.name, parsed, options, plan, unitFromFile = parsed.weightUnit != null)
                }
            }
        }
    }

    private fun fail(failure: HistoryImportFailure) {
        _state.value = HistoryImportState.Failed(failure)
    }

    fun setWeightUnit(unit: String) = changeOptions { it.copy(weightUnit = unit) }

    fun setSkipDuplicates(skip: Boolean) = changeOptions { it.copy(skipDuplicates = skip) }

    fun setFillNewRoutines(fill: Boolean) = changeOptions { it.copy(fillNewRoutines = fill) }

    /** Creates, or stops creating, the routine the file names; its workouts become quick workouts instead. */
    fun setRoutineCreated(key: String, create: Boolean) =
        changeOptions { it.copy(declinedRoutines = if (create) it.declinedRoutines - key else it.declinedRoutines + key) }

    private fun changeOptions(change: (ImportOptions) -> ImportOptions) {
        val current = _state.value as? HistoryImportState.Preview ?: return
        val parsed = history ?: return
        val data = known ?: return
        val options = change(current.options)
        planJob?.cancel()
        planJob = scope.launch {
            val plan = withContext(Dispatchers.Default) { HistoryImportPlanner.plan(parsed, data, options) }
            _state.value = current.copy(options = options, plan = plan, importError = null)
        }
    }

    fun startImport() {
        val preview = _state.value as? HistoryImportState.Preview ?: return
        if (preview.plan.toImport.isEmpty()) return
        _state.value = HistoryImportState.Importing(preview.plan.toImport.size, preview.plan.setCount)
        scope.launch {
            try {
                val result = importer.apply(preview.plan)
                history = null
                known = null
                _state.value = HistoryImportState.Done(result)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = preview.copy(importError = e.message ?: e::class.simpleName.orEmpty())
            }
        }
    }

    fun undo() {
        val done = _state.value as? HistoryImportState.Done ?: return
        if (done.undone) return
        scope.launch {
            try {
                importer.undo(done.result)
                _state.value = done.copy(undone = true, undoError = null)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = done.copy(undoError = e.message ?: e::class.simpleName.orEmpty())
            }
        }
    }

    /** The example CSV: header and names in [language], weights in the app's unit, dated up to today. */
    suspend fun exampleText(language: String, notes: HistoryCsvTemplate.Notes): String {
        val unit = userPreferencesRepository.weightUnit.first()
        return HistoryCsvTemplate.build(language, unit, Clock.System.todayIn(TimeZone.currentSystemDefault()), notes)
    }

    /** Back to the start, to choose a file again. */
    fun reset() {
        planJob?.cancel()
        history = null
        known = null
        _state.value = HistoryImportState.Idle
    }

    companion object {
        const val MAX_FILE_MB = 20

        /** The byte-order mark that makes Excel read the accents of the example as UTF-8. */
        val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}
