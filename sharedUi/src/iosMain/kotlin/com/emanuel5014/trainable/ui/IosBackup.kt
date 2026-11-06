@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.util.backup.IosBackupManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeData
import platform.UniformTypeIdentifiers.UTTypeZIP
import platform.darwin.NSObject

private object IosBackups {
    val manager by lazy { IosBackupManager() }
    private val startupLock = Mutex()
    private var started = false
    suspend fun startup(): IosBackupManager.RestoreResult = startupLock.withLock {
        if (started) return@withLock IosBackupManager.RestoreResult.None
        manager.finishPendingRestore().also { started = true }
    }
    suspend fun prepareTimerAction(): Boolean = startupLock.withLock {
        if (manager.hasPendingRestoreOrRecovery()) return@withLock false
        if (!started) {
            if (manager.finishPendingRestore() != IosBackupManager.RestoreResult.None) return@withLock false
            started = true
        }
        true
    }
    var resetPending by mutableStateOf(false)
    var restartRequired by mutableStateOf(false)
    var startupResult by mutableStateOf(IosBackupManager.RestoreResult.None)
}

/** Other document imports must wait until a staged backup restore has been applied or cancelled. */
internal val iosBackupRestartRequired: Boolean get() = IosBackups.restartRequired

/** No Room/DataStore access from a cold App Intent until safe startup has run. */
internal suspend fun prepareIosTimerAction(): Boolean = withContext(Dispatchers.IO) { IosBackups.prepareTimerAction() }

/** Restores/recovery finish before a single repository, model, Room or DataStore instance is created. */
@Composable
internal fun IosBackupStartup(content: @Composable () -> Unit) {
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        failed = false
        try {
            val result = withContext(Dispatchers.IO) { IosBackups.startup() }
            if (result != IosBackupManager.RestoreResult.None) {
                if (result != IosBackupManager.RestoreResult.Reset) IosBackups.startupResult = result
                if (result == IosBackupManager.RestoreResult.Reset) {
                    platform.UserNotifications.UNUserNotificationCenter.currentNotificationCenter().apply {
                        removeAllPendingNotificationRequests()
                        removeAllDeliveredNotifications()
                    }
                }
                IosWorkoutLiveActivityBridge.service?.cancelAll()
            }
            ready = true
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { failed = true }
    }
    if (ready) content()
    else MaterialTheme(colorScheme = darkColorScheme()) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().padding(32.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (failed) {
                Text(stringResource(Res.string.import_failed), color = MaterialTheme.colorScheme.onBackground)
                TextButton(onClick = { attempt++ }) { Text(stringResource(Res.string.retry)) }
            } else CircularProgressIndicator()
        }
    }
}

/** The destructive action uses the same cold-start journal and cancellation path as backup restore. */
@Composable
internal fun IosResetButton() {
    val scope = rememberCoroutineScope()
    val show = LocalShowMessage.current
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    GymButton(onClick = { confirm = true }, enabled = !busy,
        modifier = Modifier.fillMaxWidth(), containerColor = Error.copy(alpha = 0.15f), contentColor = Error) {
        Icon(Icons.Rounded.RestartAlt, null, tint = Error)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(Res.string.reset_app_button), fontWeight = FontWeight.ExtraBold, color = Error)
    }
    if (confirm) AlertDialog(onDismissRequest = { if (!busy) confirm = false }, containerColor = SurfaceContainerHigh,
        title = { Text(stringResource(Res.string.reset_app_dialog), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
        text = { Text(stringResource(Res.string.reset_app_message), color = OnSurfaceVariant) },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { IosBackups.manager.stageReset() }
                    confirm = false
                    IosBackups.resetPending = true
                    IosBackups.restartRequired = true
                } catch (error: CancellationException) { throw error }
                catch (_: Exception) { show(getString(Res.string.reset_failed)) }
                finally { busy = false }
            }
        }) { Text(stringResource(Res.string.reset_app_button).uppercase(), color = Error) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { confirm = false }) {
            Text(stringResource(Res.string.cancel).uppercase(), color = Primary)
        } })
}

/** A confirmed restore remains cancellable until the next process launch; the live database stays open and intact. */
@Composable
internal fun IosBackupDialogs() {
    val scope = rememberCoroutineScope()
    val show = LocalShowMessage.current
    var cancelling by remember { mutableStateOf(false) }
    if (IosBackups.restartRequired) {
        AlertDialog(onDismissRequest = {}, containerColor = SurfaceContainerHigh,
            title = { Text(stringResource(if (IosBackups.resetPending) Res.string.reset_reopen_title else Res.string.backup_reopen_title), color = OnSurface, fontWeight = FontWeight.ExtraBold) },
            text = { Text(stringResource(if (IosBackups.resetPending) Res.string.reset_reopen_description else Res.string.backup_reopen_description), color = OnSurfaceVariant) },
            confirmButton = {}, dismissButton = {
                TextButton(enabled = !cancelling, onClick = {
                    cancelling = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { IosBackups.manager.cancelPendingRestore() }
                            IosBackups.restartRequired = false
                        } catch (error: CancellationException) { throw error }
                        catch (_: Exception) { show(getString(Res.string.import_failed)) }
                        finally { cancelling = false }
                    }
                }) { Text(stringResource(Res.string.cancel), color = Primary) }
            })
    }
    val result = IosBackups.startupResult
    if (result != IosBackupManager.RestoreResult.None) {
        AlertDialog(onDismissRequest = { IosBackups.startupResult = IosBackupManager.RestoreResult.None },
            containerColor = SurfaceContainerHigh,
            title = { Text(stringResource(if (result == IosBackupManager.RestoreResult.Restored)
                Res.string.import_successful else Res.string.import_failed), color = OnSurface) },
            confirmButton = { TextButton(onClick = { IosBackups.startupResult = IosBackupManager.RestoreResult.None }) {
                Text(stringResource(Res.string.ok), color = Primary)
            } })
    }
}

@Composable
internal fun IosBackupButtons(restoreOnly: Boolean = false, completeOnboarding: Boolean = false) {
    val scope = rememberCoroutineScope()
    val presenter = LocalUIViewController.current
    val show by rememberUpdatedState(LocalShowMessage.current)
    var busy by remember { mutableStateOf(false) }
    var includeImages by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<NSURL?>(null) }
    var exported by remember { mutableStateOf<String?>(null) }

    fun cleanupExport() {
        val path = exported ?: return
        exported = null
        scope.launch(Dispatchers.IO) { IosBackups.manager.removeExport(path) }
    }
    val openDelegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                picked = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            }
        }
    }
    val saveDelegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                cleanupExport()
                scope.launch { show(getString(Res.string.export_ready)) }
            }
            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) { cleanupExport() }
        }
    }
    fun export(images: Boolean) {
        includeImages = false
        busy = true
        scope.launch {
            try {
                val preferences = IosAppGraph.settingsStore.data.first()
                val path = withContext(Dispatchers.IO) { IosBackups.manager.exportBackup(images, preferences) }
                exported = path
                val picker = UIDocumentPickerViewController(forExportingURLs = listOf(NSURL.fileURLWithPath(path)), asCopy = true)
                picker.delegate = saveDelegate
                topBackupController(presenter).presentViewController(picker, true, null)
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { cleanupExport(); show(getString(Res.string.export_failed)) }
            finally { busy = false }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!restoreOnly) GymButton(onClick = { includeImages = true }, enabled = !busy, modifier = Modifier.weight(1f),
            containerColor = SurfaceContainerHigh, contentColor = OnSurface) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
            else Icon(Icons.Rounded.CloudUpload, null, tint = Primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.export_database), fontWeight = FontWeight.ExtraBold)
        }
        GymButton(onClick = {
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeZIP, UTTypeData), asCopy = true)
            picker.delegate = openDelegate
            picker.allowsMultipleSelection = false
            topBackupController(presenter).presentViewController(picker, true, null)
        }, enabled = !busy, modifier = Modifier.weight(1f), containerColor = SurfaceContainerHigh, contentColor = OnSurface) {
            Icon(Icons.Rounded.CloudDownload, null, tint = Primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.import_database), fontWeight = FontWeight.ExtraBold)
        }
    }
    if (includeImages) AlertDialog(onDismissRequest = { includeImages = false }, containerColor = SurfaceContainerHigh,
        title = { Text(stringResource(Res.string.include_images), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
        text = { Text(stringResource(Res.string.include_images_description), color = OnSurfaceVariant) },
        confirmButton = { TextButton(onClick = { export(true) }) { Text(stringResource(Res.string.yes), color = Primary) } },
        dismissButton = { TextButton(onClick = { export(false) }) { Text(stringResource(Res.string.no), color = OnSurfaceVariant) } })
    val selected = picked
    if (selected != null) AlertDialog(onDismissRequest = { if (!busy) { removePickedBackup(selected); picked = null } },
        containerColor = SurfaceContainerHigh,
        title = { Text(stringResource(Res.string.nextcloud_restore_confirm_title), color = OnSurface, fontWeight = FontWeight.ExtraBold) },
        text = { Text(stringResource(if (busy) Res.string.importing_database else Res.string.backup_restore_confirm_description), color = OnSurfaceVariant) },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                val access = selected.startAccessingSecurityScopedResource()
                try {
                    withContext(Dispatchers.IO) { IosBackups.manager.stageRestore(requireNotNull(selected.path), completeOnboarding) }
                    picked = null
                    IosBackups.restartRequired = true
                } catch (error: CancellationException) { throw error }
                catch (_: Exception) { picked = null; show(getString(Res.string.import_failed)) }
                finally {
                    if (access) selected.stopAccessingSecurityScopedResource()
                    removePickedBackup(selected)
                    busy = false
                }
            }
        }) { Text(stringResource(Res.string.import_database), color = Primary) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { removePickedBackup(selected); picked = null }) {
            Text(stringResource(Res.string.cancel), color = OnSurfaceVariant)
        } })
}

private fun topBackupController(root: UIViewController): UIViewController {
    var top = root
    while (top.presentedViewController != null) top = top.presentedViewController!!
    return top
}

/** The chooser is configured asCopy; only remove its app-owned Inbox/temp copy, never an external URL. */
private fun removePickedBackup(url: NSURL) {
    val path = url.path ?: return
    val documents = NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, false, null)?.path
    if (path.startsWith(NSTemporaryDirectory()) || (documents != null && path.startsWith("$documents/Inbox/")))
        NSFileManager.defaultManager.removeItemAtURL(url, null)
}
