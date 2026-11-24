@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.platform.IosAppLocale
import com.emanuel5014.trainable.data.repository.IosWorkoutPlatform
import com.emanuel5014.trainable.data.repository.WorkoutPlatform
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.export_csv
import com.emanuel5014.trainable.sharedui.resources.export_failed
import com.emanuel5014.trainable.sharedui.resources.export_ready
import com.emanuel5014.trainable.sharedui.resources.performed_technique_none
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.components.LocalPrescriptionText
import com.emanuel5014.trainable.ui.screens.history.export.CsvFileSaver
import com.emanuel5014.trainable.ui.screens.history.export.HistoryCsvExportDeps
import com.emanuel5014.trainable.ui.screens.history.export.HistoryCsvExportHost
import com.emanuel5014.trainable.ui.screens.history.export.HistoryCsvQuickExport
import com.emanuel5014.trainable.ui.screens.history.export.rememberHistoryCsvQuickExport
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject

/**
 * The workouts repository the CSV export writes with: the graph's iOS platform keeps the stored technique codes, the
 * CSV needs the same localized labels as Android, including the explicit "no technique" value for performed sets.
 */
@Composable
internal fun rememberIosCsvRepository(): WorkoutRepository {
    val prescription = LocalPrescriptionText.current
    val noTechnique = stringResource(Res.string.performed_technique_none)
    return remember(prescription, noTechnique) {
        val db = IosAppGraph.database
        val platform = object : WorkoutPlatform by IosWorkoutPlatform() {
            override fun techniqueLabel(technique: Technique): String = prescription.technique(technique)
            override val noTechniqueLabel: String = noTechnique
        }
        WorkoutRepository(db.workoutDao(), db.userDao(), db.exerciseDao(), db.oneRepMaxDao(), IosAppGraph.preferences, platform)
    }
}

/**
 * iOS's [CsvFileSaver]: the CSV is written as UTF-8 (no BOM) into a directory of its own, then the document picker asks
 * where to keep a copy; neither cancelling nor the cleanup touches the saved copy.
 */
@Composable
internal fun rememberIosCsvFileSaver(): CsvFileSaver {
    val scope = rememberCoroutineScope()
    val presenter = LocalUIViewController.current
    val showMessage by rememberUpdatedState(LocalShowMessage.current)
    val holder = remember { IosCsvExportHolder() }

    // UIKit holds this weakly, so retain it throughout the save/cancel interaction.
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val saved = holder.onSaved
                holder.cleanup()
                scope.launch { showMessage(getString(Res.string.export_ready)) }
                saved?.invoke()
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) { holder.cleanup() }
        }
    }
    DisposableEffect(Unit) { onDispose { holder.cleanup() } }

    return remember(presenter) {
        CsvFileSaver { fileName, csv, onSaved ->
            scope.launch {
                var presented = false
                try {
                    holder.cleanup()
                    val directory = NSTemporaryDirectory() + "history-export-" + NSUUID().UUIDString
                    holder.directory = directory
                    val text = csv()
                    val url = withContext(Dispatchers.IO) {
                        check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null))
                        val output = NSURL.fileURLWithPath("$directory/$fileName")
                        val bytes = text.encodeToByteArray()
                        val data = bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) }
                        check(data.writeToURL(output, atomically = true))
                        output
                    }
                    holder.onSaved = onSaved
                    val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)
                    picker.delegate = delegate
                    var top = presenter
                    while (top.presentedViewController != null) top = top.presentedViewController!!
                    top.presentViewController(picker, animated = true, completion = null)
                    presented = true
                } catch (error: CancellationException) { throw error }
                catch (_: Exception) { showMessage(getString(Res.string.export_failed)) }
                finally { if (!presented) holder.cleanup() }
            }
        }
    }
}

/** The directory of the export the picker is showing and what to do once it is saved. */
private class IosCsvExportHolder {
    var directory: String? = null
    var onSaved: (() -> Unit)? = null

    fun cleanup() {
        directory?.let { NSFileManager.defaultManager.removeItemAtPath(it, null) }
        directory = null
        onSaved = null
    }
}

/** The shared CSV export sheet with iOS's repositories and document picker. */
@Composable
internal fun IosHistoryCsvExport(onDismiss: () -> Unit) {
    val repository = rememberIosCsvRepository()
    val saver = rememberIosCsvFileSaver()
    val deps = remember(repository) { HistoryCsvExportDeps(repository, IosAppGraph.preferences) }
    HistoryCsvExportHost(
        deps = deps,
        languageCode = IosAppLocale.language(),
        saver = saver,
        onDismiss = onDismiss
    )
}

/** The history filter sheet's CSV button, with iOS's repository and document picker. */
@Composable
internal fun rememberIosHistoryCsvQuickExport(): HistoryCsvQuickExport {
    val repository = rememberIosCsvRepository()
    val saver = rememberIosCsvFileSaver()
    val deps = remember(repository) { HistoryCsvExportDeps(repository, IosAppGraph.preferences) }
    return rememberHistoryCsvQuickExport(deps, IosAppLocale.language(), saver)
}

/** Settings' "Export CSV": opens the export sheet on the whole history. */
@Composable
internal fun IosHistoryExportButton() {
    var open by remember { mutableStateOf(false) }
    GymButton(
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth(),
        containerColor = SurfaceContainerHigh,
        contentColor = OnSurface
    ) {
        Icon(Icons.Rounded.TableChart, contentDescription = null, tint = Primary)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(Res.string.export_csv), fontWeight = FontWeight.ExtraBold)
    }
    if (open) IosHistoryCsvExport(onDismiss = { open = false })
}
