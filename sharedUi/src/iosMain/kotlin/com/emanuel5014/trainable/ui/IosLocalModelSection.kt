package com.emanuel5014.trainable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ai.AiModelStatus
import com.emanuel5014.trainable.data.ai.IosLocalAiDevice
import com.emanuel5014.trainable.data.ai.IosModelStore
import com.emanuel5014.trainable.data.ai.ModelImportError
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.ai_download_cancel
import com.emanuel5014.trainable.sharedui.resources.ai_import_error_busy
import com.emanuel5014.trainable.sharedui.resources.ai_import_error_format
import com.emanuel5014.trainable.sharedui.resources.ai_import_error_read
import com.emanuel5014.trainable.sharedui.resources.ai_import_error_space
import com.emanuel5014.trainable.sharedui.resources.ai_import_error_unsupported_ios
import com.emanuel5014.trainable.sharedui.resources.ai_model_delete
import com.emanuel5014.trainable.sharedui.resources.ai_model_delete_confirm_desc
import com.emanuel5014.trainable.sharedui.resources.ai_model_delete_confirm_title
import com.emanuel5014.trainable.sharedui.resources.ai_model_download
import com.emanuel5014.trainable.sharedui.resources.ai_model_download_retry
import com.emanuel5014.trainable.sharedui.resources.ai_model_downloading
import com.emanuel5014.trainable.sharedui.resources.ai_model_import
import com.emanuel5014.trainable.sharedui.resources.ai_model_import_note_ios
import com.emanuel5014.trainable.sharedui.resources.ai_model_importing
import com.emanuel5014.trainable.sharedui.resources.ai_model_ready
import com.emanuel5014.trainable.sharedui.resources.ai_model_requirements
import com.emanuel5014.trainable.sharedui.resources.ai_model_variant
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.delete
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSByteCountFormatter
import platform.Foundation.NSByteCountFormatterCountStyleFile
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject

/**
 * The on-device model in the iOS AI settings (A5-4), the same controls as Android's LocalModelControls with one model
 * only: Gemma 4 E2B (E4B does not fit an iPhone's memory, checks/a5-local-gemma). Download in the background, import a
 * file from Files (opened in place, then copied and checked), delete. Shown only under "On this phone" on an iPhone
 * with 8 GB of RAM.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun IosLocalModelSection(activeIsLocal: Boolean, store: IosModelStore = IosAppGraph.localModels) {
    if (!activeIsLocal || !IosLocalAiDevice.isSupported) return
    val status by store.status.collectAsState()
    val importError by store.importError.collectAsState()
    var confirmDelete by remember { mutableStateOf(false) }
    val variant = store.variant

    // An error left by an earlier visit is no news now
    LaunchedEffect(Unit) { store.clearImportError() }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(stringResource(Res.string.ai_model_delete_confirm_title), fontWeight = FontWeight.ExtraBold, color = OnSurface)
            },
            text = { Text(stringResource(Res.string.ai_model_delete_confirm_desc), color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; store.delete() }) {
                    Text(stringResource(Res.string.delete).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = Primary)
                }
            }
        )
    }

    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.ai_model_variant), fontWeight = FontWeight.ExtraBold, color = OnSurface)
            Row(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceContainerHighest).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = variant.displayName,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Primary)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = OnPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                // An iPhone needs 8 GB for it (IosLocalAiDevice), not Android's 6
                text = stringResource(Res.string.ai_model_requirements, variant.sizeLabel, 8),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }

        when (val current = status) {
            is AiModelStatus.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(
                            if (current.importing) Res.string.ai_model_importing else Res.string.ai_model_downloading,
                            (current.progress * 100).toInt()
                        ).replace("%%", "%"),
                        style = MaterialTheme.typography.labelLarge,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    // An import is a local copy that finishes on its own; only a download can be stopped
                    if (!current.importing) {
                        TextButton(onClick = { store.cancelDownload() }) {
                            Text(stringResource(Res.string.ai_download_cancel).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                LinearWavyProgressIndicator(progress = { current.progress }, modifier = Modifier.fillMaxWidth())
            }
            AiModelStatus.Ready -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.ai_model_ready), style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                }
                TextButton(onClick = { confirmDelete = true }) {
                    Text(stringResource(Res.string.ai_model_delete).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold)
                }
            }
            is AiModelStatus.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(current.message, style = MaterialTheme.typography.bodySmall, color = Error)
                DownloadButton(stringResource(Res.string.ai_model_download_retry)) { store.startDownload() }
                ImportOption(importError, store)
            }
            AiModelStatus.NotDownloaded -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DownloadButton(stringResource(Res.string.ai_model_download, variant.displayName)) { store.startDownload() }
                ImportOption(importError, store)
            }
        }
    }
}

@Composable
private fun DownloadButton(label: String, onClick: () -> Unit) {
    GymButton(
        onClick = onClick,
        containerColor = Primary.copy(alpha = 0.1f),
        contentColor = Primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

/**
 * "Import a model you already have" from Files. The picker opens the file in place (not as a copy, which would take
 * 2.6 GB more in the app's inbox); [IosModelStore.import] reads it inside its security scope and copies it.
 */
@Composable
private fun ImportOption(error: ModelImportError?, store: IosModelStore) {
    val presenter = LocalUIViewController.current
    val delegate = remember(store) {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                (didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.absoluteString?.let(store::import)
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        TextButton(onClick = {
            store.clearImportError()
            // A .litertlm file has no type of its own, so every file is offered and checked afterwards
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeData), asCopy = false)
            picker.delegate = delegate
            picker.allowsMultipleSelection = false
            topController(presenter).presentViewController(picker, true, null)
        }) {
            Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.ai_model_import), color = Primary, fontWeight = FontWeight.ExtraBold)
        }
        Text(
            stringResource(Res.string.ai_model_import_note_ios),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(error.message(), style = MaterialTheme.typography.bodySmall, color = Error, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ModelImportError.message(): String = when (this) {
    ModelImportError.NotLiteRtLm -> stringResource(Res.string.ai_import_error_format)
    ModelImportError.UnsupportedModel -> stringResource(Res.string.ai_import_error_unsupported_ios)
    is ModelImportError.NotEnoughSpace -> stringResource(
        Res.string.ai_import_error_space,
        NSByteCountFormatter.stringFromByteCount(neededBytes, NSByteCountFormatterCountStyleFile)
    )
    ModelImportError.CannotRead -> stringResource(Res.string.ai_import_error_read)
    ModelImportError.Busy -> stringResource(Res.string.ai_import_error_busy)
}

private fun topController(root: UIViewController): UIViewController {
    var top = root
    while (top.presentedViewController != null) top = top.presentedViewController!!
    return top
}
