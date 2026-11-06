@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.data_backup
import com.emanuel5014.trainable.sharedui.resources.import_csv
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.screens.settings.SettingsSection
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.text.intl.Locale
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvTemplate
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.import_history_template_error
import com.emanuel5014.trainable.sharedui.resources.import_history_template_note_easy
import com.emanuel5014.trainable.sharedui.resources.import_history_template_note_warmup
import com.emanuel5014.trainable.sharedui.resources.import_history_template_saved
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.screens.settings.HistoryImportContent
import com.emanuel5014.trainable.ui.screens.settings.HistoryImportModel
import com.emanuel5014.trainable.ui.screens.settings.PickedCsv
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithBytes
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeCommaSeparatedText
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.UniformTypeIdentifiers.UTTypeText
import platform.darwin.NSObject
import platform.posix.memcpy

/**
 * Settings -> Import CSV on iOS: the shared screen with the Files app's chooser for the history and its export sheet
 * for the example file. Uses the language chosen in the app's settings.
 */
@Composable
internal fun IosHistoryImportScreen(onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val model = remember { HistoryImportModel(IosAppGraph.historyImporter, IosAppGraph.preferences, scope) }
    val language = com.emanuel5014.trainable.platform.IosAppLocale.language()
    val presenter = LocalUIViewController.current
    val showMessage by rememberUpdatedState(LocalShowMessage.current)

    // The pickers hold their delegate weakly: these live as long as the screen
    val openDelegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
                model.load { withContext(Dispatchers.IO) { readPickedCsv(url) } }
            }
        }
    }
    val exportDelegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                scope.launch { showMessage(getString(Res.string.import_history_template_saved)) }
            }
        }
    }
    DisposableEffect(presenter) {
        onDispose { NSFileManager.defaultManager.removeItemAtPath(exampleFilePath(), null) }
    }

    HistoryImportContent(
        model = model,
        language = language,
        onChoose = {
            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(UTTypeCommaSeparatedText, UTTypePlainText, UTTypeText, UTTypeData),
                asCopy = true
            ).apply {
                delegate = openDelegate
                allowsMultipleSelection = false
            }
            topViewController(presenter).presentViewController(picker, animated = true, completion = null)
        },
        onDownloadExample = {
            scope.launch {
                val notes = HistoryCsvTemplate.Notes(
                    warmup = getString(Res.string.import_history_template_note_warmup),
                    easy = getString(Res.string.import_history_template_note_easy)
                )
                val text = model.exampleText(language, notes)
                val url = withContext(Dispatchers.IO) { writeExample(text) }
                if (url == null) {
                    showMessage(getString(Res.string.import_history_template_error, "no output"))
                    return@launch
                }
                val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true).apply {
                    delegate = exportDelegate
                }
                topViewController(presenter).presentViewController(picker, animated = true, completion = null)
            }
        },
        onNavigateBack = onNavigateBack
    )
}

private fun topViewController(root: UIViewController): UIViewController {
    var top = root
    while (top.presentedViewController != null) top = top.presentedViewController!!
    return top
}

/** The copy the chooser made in the app's own storage, read and then removed; over the limit it is not read at all. */
private fun readPickedCsv(url: NSURL): PickedCsv {
    val manager = NSFileManager.defaultManager
    try {
        val path = url.path ?: return PickedCsv.Unreadable
        val size = (manager.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? NSNumber)?.longLongValue
            ?: return PickedCsv.Unreadable
        if (size > HistoryImportModel.MAX_FILE_MB * 1024L * 1024L) return PickedCsv.TooLarge
        val data = NSData.dataWithContentsOfURL(url) ?: return PickedCsv.Unreadable
        return PickedCsv.Read(url.lastPathComponent.orEmpty(), data.toByteArray())
    } finally {
        manager.removeItemAtURL(url, null)
    }
}

private fun exampleFilePath(): String = NSTemporaryDirectory() + "Trainable_import_example.csv"

/** The example with the byte-order mark (Excel then reads the accents as UTF-8), as Android writes it. */
private fun writeExample(text: String): NSURL? {
    val bytes = HistoryImportModel.UTF8_BOM + text.encodeToByteArray()
    val url = NSURL.fileURLWithPath(exampleFilePath())
    val data = bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) }
    return if (data.writeToURL(url, atomically = true)) url else null
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}

/** Settings' data section: native backup export/restore and CSV export/import. */
@Composable
internal fun IosDataSection(onNavigateToImportHistory: () -> Unit) {
    SettingsSection(title = stringResource(Res.string.data_backup)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            IosBackupButtons()
            IosHistoryExportButton()
            GymButton(
                onClick = onNavigateToImportHistory,
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceContainerHigh,
                contentColor = OnSurface
            ) {
                Icon(Icons.Rounded.TableChart, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(Res.string.import_csv), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
