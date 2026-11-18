@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.CircularProgressIndicator
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
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject

/** Exports the shared history CSV with Android's filename and UTF-8 bytes (without a BOM). */
@Composable
internal fun IosHistoryExportButton() {
    val scope = rememberCoroutineScope()
    val presenter = LocalUIViewController.current
    val showMessage by rememberUpdatedState(LocalShowMessage.current)
    val prescription = LocalPrescriptionText.current
    val noTechnique = stringResource(Res.string.performed_technique_none)
    // The graph's general-purpose iOS platform still uses stored technique codes. CSV needs the same
    // localized labels as Android, including the explicit "no technique" value for performed sets.
    val repository = remember(prescription, noTechnique) {
        val db = IosAppGraph.database
        val platform = object : WorkoutPlatform by IosWorkoutPlatform() {
            override fun techniqueLabel(technique: Technique): String = prescription.technique(technique)
            override val noTechniqueLabel: String = noTechnique
        }
        WorkoutRepository(db.workoutDao(), db.userDao(), db.exerciseDao(), db.oneRepMaxDao(), IosAppGraph.preferences, platform)
    }
    var busy by remember { mutableStateOf(false) }
    // Each export owns its directory; neither cancellation nor cleanup touches the saved Files copy.
    var exportDirectory by remember { mutableStateOf<String?>(null) }

    fun cleanup() {
        exportDirectory?.let { NSFileManager.defaultManager.removeItemAtPath(it, null) }
        exportDirectory = null
        busy = false
    }

    // UIKit holds this weakly, so retain it throughout the save/cancel interaction.
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                cleanup()
                scope.launch { showMessage(getString(Res.string.export_ready)) }
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) { cleanup() }
        }
    }
    DisposableEffect(Unit) { onDispose { cleanup() } }

    GymButton(
        onClick = {
            busy = true
            scope.launch {
                var presented = false
                try {
                    val language = IosAppLocale.language()
                    val unit = IosAppGraph.preferences.weightUnit.first()
                    val directory = NSTemporaryDirectory() + "history-export-" + NSUUID().UUIDString
                    exportDirectory = directory
                    val url = withContext(Dispatchers.IO) {
                        val csv = repository.exportAllWorkoutsToCsv(unit, language)
                        check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null))
                        val timestamp = NSDateFormatter().apply {
                            locale = NSLocale("en_US_POSIX")
                            dateFormat = "yyyyMMdd_HHmmss"
                        }.stringFromDate(NSDate())
                        val output = NSURL.fileURLWithPath("$directory/Workouts_$timestamp.csv")
                        val bytes = csv.encodeToByteArray()
                        val data = bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) }
                        check(data.writeToURL(output, atomically = true))
                        output
                    }
                    val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)
                    picker.delegate = delegate
                    var top = presenter
                    while (top.presentedViewController != null) top = top.presentedViewController!!
                    top.presentViewController(picker, animated = true, completion = null)
                    presented = true
                } catch (error: CancellationException) { throw error }
                catch (_: Exception) { showMessage(getString(Res.string.export_failed)) }
                finally { if (!presented) cleanup() }
            }
        },
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
        containerColor = SurfaceContainerHigh,
        contentColor = OnSurface
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
        else Icon(Icons.Rounded.TableChart, contentDescription = null, tint = Primary)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(Res.string.export_csv), fontWeight = FontWeight.ExtraBold)
    }
}
