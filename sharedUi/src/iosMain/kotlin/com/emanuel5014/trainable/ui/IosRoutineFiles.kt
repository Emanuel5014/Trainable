@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.emanuel5014.trainable.data.remote.dto.TrainablePlanParser
import com.emanuel5014.trainable.data.remote.dto.WorkoutPlanExportDto
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.import_failed
import com.emanuel5014.trainable.sharedui.resources.import_plans_title
import com.emanuel5014.trainable.sharedui.resources.import_successful
import com.emanuel5014.trainable.sharedui.resources.ok
import com.emanuel5014.trainable.ui.components.ImportConfirmationDialog
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSFileCoordinator
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSTemporaryDirectory

internal data class IosRoutinePayload(val json: String, val plans: List<WorkoutPlanExportDto>)

internal sealed interface IosRoutineImportState {
    data object Reading : IosRoutineImportState
    data class Preview(val payload: IosRoutinePayload) : IosRoutineImportState
    data object Importing : IosRoutineImportState
    data class Finished(val success: Boolean) : IosRoutineImportState
}

internal data class IosRoutineImportRequest(val id: Long, val state: IosRoutineImportState)

/** App-owned queue: neither navigation nor a language-root recreation loses a preview or cancels a write. */
internal class IosRoutineImportModel(
    private val scope: CoroutineScope,
    private val importPlans: suspend (String) -> Int
) {
    private val mutableRequests = MutableStateFlow<List<IosRoutineImportRequest>>(emptyList())
    val requests = mutableRequests.asStateFlow()
    private var nextId = 0L

    fun enqueue(read: () -> IosRoutinePayload) {
        val id = ++nextId
        mutableRequests.update { it + IosRoutineImportRequest(id, IosRoutineImportState.Reading) }
        scope.launch {
            val state = try {
                IosRoutineImportState.Preview(withContext(Dispatchers.IO) { read() })
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { IosRoutineImportState.Finished(false) }
            update(id, state)
        }
    }

    fun confirm(id: Long) {
        val request = mutableRequests.value.firstOrNull()?.takeIf { it.id == id } ?: return
        val preview = request.state as? IosRoutineImportState.Preview ?: return
        // Claim synchronously, before launching: rapid/repeated confirmation cannot import the same file twice.
        update(id, IosRoutineImportState.Importing)
        scope.launch {
            val success = try {
                withContext(Dispatchers.IO) { importPlans(preview.payload.json) } > 0
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { false }
            update(id, IosRoutineImportState.Finished(success))
        }
    }

    fun dismiss(id: Long) {
        val request = mutableRequests.value.firstOrNull()?.takeIf { it.id == id } ?: return
        if (request.state != IosRoutineImportState.Importing) mutableRequests.update { list -> list.filterNot { it.id == id } }
    }

    private fun update(id: Long, state: IosRoutineImportState) {
        mutableRequests.update { list -> list.map { if (it.id == id) it.copy(state = state) else it } }
    }
}

/** Called by SwiftUI onOpenURL, including cold launch. Reading/parsing never opens Room or DataStore. */
object IosRoutineFiles {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    internal val model = IosRoutineImportModel(scope) { json -> IosAppGraph.workouts.importPlans(json) }

    fun open(url: NSURL) {
        // Not a document: e.g. AltStore's "Open" launches the app with the altstore-<bundle id>:// scheme it adds
        if (!isIosRoutineFileUrl(url)) return
        val access = url.startAccessingSecurityScopedResource()
        scope.launch {
            model.enqueue {
                try { readIosRoutineFile(url) }
                finally { if (access) url.stopAccessingSecurityScopedResource() }
            }
        }
    }
}

/** Only documents handed over by Files/share sheet are routine imports; any other URL opening the app is ignored. */
internal fun isIosRoutineFileUrl(url: NSURL): Boolean = url.isFileURL()

/** Coordinate provider downloads and bound reads before decoding the shared Android-compatible format. */
internal fun readIosRoutineFile(url: NSURL, limit: Long = 50L * 1024 * 1024): IosRoutinePayload {
    require(url.isFileURL() && url.pathExtension?.lowercase() == "trainableplan")
    require(limit > 0)
    val path = requireNotNull(url.path)
    // App-owned Inbox/temp copies need no provider coordination (also valid for the native test runner).
    if (path.startsWith(NSTemporaryDirectory()) || path.startsWith(NSHomeDirectory() + "/")) {
        return readRoutinePayload(path, limit)
    }
    return memScoped {
        var result: Result<IosRoutinePayload>? = null
        val error = alloc<ObjCObjectVar<NSError?>>()
        error.value = null
        NSFileCoordinator(filePresenter = null).coordinateReadingItemAtURL(url, options = 0u, error = error.ptr) { coordinated ->
            result = runCatching { readRoutinePayload(requireNotNull(requireNotNull(coordinated).path), limit) }
        }
        checkNotNull(result) { error.value?.localizedDescription ?: "Could not coordinate routine file" }.getOrThrow()
    }
}

private fun readRoutinePayload(path: String, limit: Long): IosRoutinePayload {
    val bytes = Buffer()
    FileSystem.SYSTEM.source(path.toPath()).buffer().use { source ->
        while (!source.exhausted()) {
            source.read(bytes, minOf(8192L, limit - bytes.size + 1))
            require(bytes.size <= limit)
        }
    }
    val json = bytes.readByteArray().decodeToString(throwOnInvalidSequence = true)
    val plans = TrainablePlanParser.decode(json)
    require(plans.isNotEmpty())
    return IosRoutinePayload(json, plans)
}

/** Mounted only after backup startup, inside the app theme; pending restores block confirmation. */
@Composable
internal fun IosRoutineImportDialogs() {
    if (iosBackupRestartRequired) return
    val requests by IosRoutineFiles.model.requests.collectAsState()
    val request = requests.firstOrNull() ?: return
    when (val state = request.state) {
        IosRoutineImportState.Reading -> Unit
        is IosRoutineImportState.Preview -> ImportConfirmationDialog(
            plans = state.payload.plans,
            onConfirm = { if (!iosBackupRestartRequired) IosRoutineFiles.model.confirm(request.id) },
            onDismiss = { IosRoutineFiles.model.dismiss(request.id) },
            contentModifier = Modifier.verticalScroll(rememberScrollState())
        )
        IosRoutineImportState.Importing -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(Res.string.import_plans_title), color = OnSurface) },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) } },
            confirmButton = {}, containerColor = SurfaceContainerHigh
        )
        is IosRoutineImportState.Finished -> AlertDialog(
            onDismissRequest = { IosRoutineFiles.model.dismiss(request.id) },
            title = { Text(stringResource(if (state.success) Res.string.import_successful else Res.string.import_failed), color = OnSurface) },
            confirmButton = { TextButton(onClick = { IosRoutineFiles.model.dismiss(request.id) }) {
                Text(stringResource(Res.string.ok), color = Primary)
            } }, containerColor = SurfaceContainerHigh
        )
    }
}
