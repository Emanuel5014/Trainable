package com.emanuel5014.trainable.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.HistoryCsvImporter
import com.emanuel5014.trainable.data.repository.ImportResult
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.domain.csvimport.CsvColumn
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.csvimport.CsvTextDecoder
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvTemplate
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvReader
import com.emanuel5014.trainable.domain.csvimport.HistoryImportPlanner
import com.emanuel5014.trainable.domain.csvimport.ImportOptions
import com.emanuel5014.trainable.domain.csvimport.ImportPlan
import com.emanuel5014.trainable.domain.csvimport.KnownData
import com.emanuel5014.trainable.domain.csvimport.ParsedHistory
import com.emanuel5014.trainable.domain.csvimport.ReadFailure
import com.emanuel5014.trainable.domain.csvimport.ReadResult
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import javax.inject.Inject

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

@HiltViewModel
class HistoryImportViewModel @Inject constructor(
    private val importer: HistoryCsvImporter,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<HistoryImportState>(HistoryImportState.Idle)
    val state: StateFlow<HistoryImportState> = _state.asStateFlow()

    /** The unit weights are shown in. */
    val appWeightUnit: StateFlow<String> = userPreferencesRepository.weightUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "kg")

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private var history: ParsedHistory? = null
    private var known: KnownData? = null
    private var planJob: Job? = null

    init {
        viewModelScope.launch {
            userPreferencesRepository.userLanguage.collect { _languageCode.value = localeManager.getResolvedLanguage() }
        }
    }

    fun load(uri: Uri) {
        _state.value = HistoryImportState.Reading
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) { readBytes(uri) }
            if (bytes == null) return@launch fail(HistoryImportFailure.Unreadable)
            if (bytes === TOO_LARGE) return@launch fail(HistoryImportFailure.TooLarge(MAX_FILE_MB))
            val name = withContext(Dispatchers.IO) { displayName(uri) }
            val result = withContext(Dispatchers.Default) { HistoryCsvReader.read(CsvTextDecoder.decode(bytes)) }
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
                    _state.value = HistoryImportState.Preview(name, parsed, options, plan, unitFromFile = parsed.weightUnit != null)
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
        planJob = viewModelScope.launch {
            val plan = withContext(Dispatchers.Default) { HistoryImportPlanner.plan(parsed, data, options) }
            _state.value = current.copy(options = options, plan = plan, importError = null)
        }
    }

    fun startImport() {
        val preview = _state.value as? HistoryImportState.Preview ?: return
        if (preview.plan.toImport.isEmpty()) return
        _state.value = HistoryImportState.Importing(preview.plan.toImport.size, preview.plan.setCount)
        viewModelScope.launch {
            try {
                val result = importer.apply(preview.plan)
                history = null
                known = null
                _state.value = HistoryImportState.Done(result)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = preview.copy(importError = e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun undo() {
        val done = _state.value as? HistoryImportState.Done ?: return
        if (done.undone) return
        viewModelScope.launch {
            try {
                importer.undo(done.result)
                _state.value = done.copy(undone = true, undoError = null)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = done.copy(undoError = e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /**
     * Writes the example CSV (header and names in the language of the app, weights in its unit) to [uri].
     * @param onResult null when it was saved, otherwise what went wrong.
     */
    fun saveExample(uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val language = localeManager.getResolvedLanguage()
            val unit = userPreferencesRepository.weightUnit.first()
            val notes = HistoryCsvTemplate.Notes(
                warmup = localeManager.getString(R.string.import_history_template_note_warmup),
                easy = localeManager.getString(R.string.import_history_template_note_easy)
            )
            val text = HistoryCsvTemplate.build(language, unit, LocalDate.now(), notes)
            val error = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        // The byte-order mark makes Excel read the accents as UTF-8
                        output.write(UTF8_BOM)
                        output.write(text.toByteArray(Charsets.UTF_8))
                    } ?: return@withContext "no output"
                    null
                } catch (e: Exception) {
                    e.message ?: e.javaClass.simpleName
                }
            }
            onResult(error)
        }
    }

    /** Back to the start, to choose a file again. */
    fun reset() {
        planJob?.cancel()
        history = null
        known = null
        _state.value = HistoryImportState.Idle
    }

    private fun readBytes(uri: Uri): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_FILE_MB * 1024L * 1024L) return TOO_LARGE
                out.write(buffer, 0, read)
            }
            out.toByteArray()
        }
    } catch (e: Exception) {
        null
    }

    private fun displayName(uri: Uri): String = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (e: Exception) {
        null
    } ?: uri.lastPathSegment.orEmpty()

    private companion object {
        const val MAX_FILE_MB = 20
        // Marks a file over the limit without reading it all
        val TOO_LARGE = ByteArray(0)
        val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}
