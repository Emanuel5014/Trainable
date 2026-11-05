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
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Android's [ReportModel]: the routines from the route, sharing through a FileProvider and saving to a document. */
@HiltViewModel
class ReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    reportGenerator: ReportGenerator,
    htmlReportFormatter: HtmlReportFormatter,
    localeManager: AppLocaleManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val model = ReportModel(
        planIds = ReportModel.planIdsOf(savedStateHandle.get<String>("planIdsString")),
        reportGenerator = reportGenerator,
        htmlReportFormatter = htmlReportFormatter,
        languageCode = { localeManager.getResolvedLanguage() },
        scope = viewModelScope
    )

    fun shareReport() {
        val html = model.uiState.value.html ?: return
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
                model.failed(e.message)
            }
        }
    }

    fun saveReport(uri: Uri) {
        val html = model.uiState.value.html ?: return
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(html.toByteArray())
                }
                model.saved()
            } catch (e: Exception) {
                model.failed(e.message)
            }
        }
    }
}
