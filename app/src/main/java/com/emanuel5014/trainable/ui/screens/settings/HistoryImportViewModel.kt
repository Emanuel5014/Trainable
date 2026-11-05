package com.emanuel5014.trainable.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.repository.HistoryCsvImporter
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvTemplate
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/** Android's side of [HistoryImportModel]: the app's language, and reading/writing documents by their Uri. */
@HiltViewModel
class HistoryImportViewModel @Inject constructor(
    importer: HistoryCsvImporter,
    userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val model = HistoryImportModel(importer, userPreferencesRepository, viewModelScope)

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.userLanguage.collect { _languageCode.value = localeManager.getResolvedLanguage() }
        }
    }

    fun load(uri: Uri) = model.load {
        val bytes = withContext(Dispatchers.IO) { readBytes(uri) }
        when {
            bytes == null -> PickedCsv.Unreadable
            bytes === TOO_LARGE -> PickedCsv.TooLarge
            else -> PickedCsv.Read(withContext(Dispatchers.IO) { displayName(uri) }, bytes)
        }
    }

    /**
     * Writes the example CSV (header and names in the language of the app, weights in its unit) to [uri].
     * @param onResult null when it was saved, otherwise what went wrong.
     */
    fun saveExample(uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val language = localeManager.getResolvedLanguage()
            val notes = HistoryCsvTemplate.Notes(
                warmup = localeManager.getString(R.string.import_history_template_note_warmup),
                easy = localeManager.getString(R.string.import_history_template_note_easy)
            )
            val text = model.exampleText(language, notes)
            val error = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(HistoryImportModel.UTF8_BOM)
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

    private fun readBytes(uri: Uri): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > HistoryImportModel.MAX_FILE_MB * 1024L * 1024L) return TOO_LARGE
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
        // Marks a file over the limit without reading it all
        val TOO_LARGE = ByteArray(0)
    }
}
