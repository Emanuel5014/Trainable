@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.emanuel5014.trainable.data.ai.LlmScanResult
import com.emanuel5014.trainable.data.ai.RoutineScanDraftStore
import com.emanuel5014.trainable.data.ai.RoutineScanPrompt
import com.emanuel5014.trainable.data.ai.RoutineScanResults
import com.emanuel5014.trainable.data.ai.ScanDraft
import com.emanuel5014.trainable.data.ai.ScanDraftCodec
import com.emanuel5014.trainable.data.ai.ScanPhase
import com.emanuel5014.trainable.data.ai.ScannedExerciseEntry
import com.emanuel5014.trainable.data.ai.backend.ConsentRequest
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import com.emanuel5014.trainable.data.ai.backend.ScanReadiness
import com.emanuel5014.trainable.data.ai.remote.ImageSizeFitter
import com.emanuel5014.trainable.data.ai.remote.RemoteScanPayload
import com.emanuel5014.trainable.data.ai.remote.RemoteScanSession
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.repository.encodeIosJpeg
import com.emanuel5014.trainable.data.repository.iosDocumentsPath
import com.emanuel5014.trainable.data.repository.saveIosRoutineImage
import com.emanuel5014.trainable.data.repository.toByteArray
import com.emanuel5014.trainable.data.repository.toNSData
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.ai_scan_failed
import com.emanuel5014.trainable.sharedui.resources.ai_scan_no_exercises
import com.emanuel5014.trainable.sharedui.resources.camera_permission_denied
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.screens.physicalcheck.decodePhotoBitmap
import com.emanuel5014.trainable.ui.screens.routines.RoutineDetailState
import com.emanuel5014.trainable.ui.screens.routines.RoutineScanBackend
import com.emanuel5014.trainable.ui.screens.routines.RoutineScanModel
import com.emanuel5014.trainable.ui.screens.routines.RoutineScanUi
import com.emanuel5014.trainable.ui.screens.routines.ScanPhotoUi
import com.emanuel5014.trainable.ui.screens.routines.rememberRoutineScanUi
import com.emanuel5014.trainable.ui.screens.settings.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import kotlin.io.encoding.Base64
import kotlin.time.Clock

@Composable
internal fun rememberIosRoutineScanUi(
    planId: Int,
    detail: RoutineDetailState,
    language: String
): RoutineScanUi {
    val scope = rememberCoroutineScope()
    val model = remember(planId, detail, language) {
        RoutineScanModel<Nothing>(
            planId, detail, IosAppGraph.workouts, IosAppGraph.exercises, IosAppGraph.oneRepMaxes,
            language = { language },
            backend = IosRoutineScanBackend,
            drafts = IosScanDraftStore,
            scope = scope,
            diskDispatcher = Dispatchers.IO
        )
    }
    val path = remember(model) { NSTemporaryDirectory() + "routine-scan-" + NSUUID().UUIDString + ".jpg" }
    DisposableEffect(model) {
        onDispose {
            model.close()
            NSFileManager.defaultManager.removeItemAtPath(path, null)
        }
    }

    val show = LocalShowMessage.current
    val scanFailed = stringResource(Res.string.ai_scan_failed)
    val noExercises = stringResource(Res.string.ai_scan_no_exercises)
    fun scanPhoto(bytes: ByteArray) {
        if (bytes.toNSData().writeToFile(path, atomically = true)) {
            model.scanRoutineSheet(path)
        } else {
            show(scanFailed)
        }
    }

    val capture = rememberIosPhotoCapture(
        selectionLimit = 1,
        deniedText = stringResource(Res.string.camera_permission_denied),
        onPicked = { photos -> photos.firstOrNull()?.let(::scanPhoto) },
        onCameraResult = { success, bytes -> if (success) bytes?.let(::scanPhoto) ?: show(scanFailed) },
        onCameraDenied = {},
        preparePhoto = { encodeIosJpeg(it, maxSide = ImageSizeFitter.START_SIDE, quality = 95) }
    )
    val scan = rememberRoutineScanUi(
        model = model,
        languageCode = language,
        photo = IosScanPhoto,
        takePhoto = capture.takePhoto,
        pickPhoto = capture.pickPhotos,
        savePhoto = { imagePath ->
            readScanPhoto(imagePath)?.let { encodeIosJpeg(it, maxSide = 1280, quality = 80) }
                ?.let(::saveIosRoutineImage)
        },
        showError = { state ->
            show(state.failure?.userMessage() ?: if (state.message == null) noExercises else scanFailed)
        }
    )
    if (scan.isScanning) {
        DisposableEffect(Unit) {
            val application = UIApplication.sharedApplication
            val previous = application.idleTimerDisabled
            application.idleTimerDisabled = true
            onDispose { application.idleTimerDisabled = previous }
        }
    }
    return scan
}

private object IosRoutineScanBackend : RoutineScanBackend {
    override val isReady get() = IosRemoteScan.isReady
    override suspend fun consentRequired() = IosRemoteScan.consentRequired()
    override suspend fun acceptConsent(request: ConsentRequest) = IosRemoteScan.acceptConsent(request)

    override suspend fun scan(
        imageUri: String,
        catalog: List<ExerciseEntity>,
        categories: List<String>,
        languageCode: String,
        onPhase: (ScanPhase) -> Unit,
        onStreamUpdate: (String, String) -> Unit
    ): List<ScannedExerciseEntry> {
        val advanced = IosAppGraph.preferences.advancedProgrammingEnabled.first()
        val result = IosRemoteScan.read(
            imageUri, RoutineScanPrompt.build(languageCode, categories, advanced),
            transcription = false, maxOutputTokens = null, onPhase, onStreamUpdate
        )
        onPhase(ScanPhase.PARSING)
        return RoutineScanResults.entries(result, catalog, categories, languageCode, advanced)
    }
}

internal object IosRemoteScan {
    private val profiles get() = IosAppGraph.aiProfiles

    val isReady = combine(
        IosAppGraph.preferences.aiScanEnabled, profiles.activeId, profiles.profiles
    ) { enabled, id, saved -> ScanReadiness.isReady(enabled, id, saved, localModelDownloaded = false) }

    private suspend fun activeProfile() = ScanReadiness.resolveActive(profiles.activeId.first(), profiles.profiles.first())

    suspend fun consentRequired() = ScanReadiness.consentRequired(activeProfile())

    suspend fun acceptConsent(request: ConsentRequest) {
        if (consentRequired() != request) throw ScanBackendException.NotConfigured()
        profiles.acceptConsent(request.profileId)
    }

    suspend fun read(
        imagePath: String,
        prompt: String,
        transcription: Boolean,
        maxOutputTokens: Int?,
        onPhase: (ScanPhase) -> Unit,
        onStreamUpdate: (String, String) -> Unit
    ): LlmScanResult {
        onPhase(ScanPhase.CONNECTING)
        val profile = activeProfile() ?: throw ScanBackendException.NotConfigured()
        // If the active profile changed during consent, fail before sending to a host that still needs approval.
        if (ScanReadiness.consentRequired(profile) != null) throw ScanBackendException.NotConfigured()
        val session = RemoteScanSession(profile, profiles.readKey(profile), profiles.keyState(profile), IosAppGraph.aiHttp)
        session.prepare()
        onPhase(ScanPhase.READING_SHEET)
        val image = withContext(Dispatchers.IO) {
            val bytes = readScanPhoto(imagePath) ?: throw ScanBackendException.BadResponse()
            val jpeg = ImageSizeFitter.fit { side, quality ->
                encodeIosJpeg(bytes, side, quality) ?: throw ScanBackendException.BadResponse()
            }
            Base64.encode(jpeg)
        }
        return session.run(RemoteScanPayload(prompt, image, transcription, maxOutputTokens), onStreamUpdate)
    }
}

private fun readScanPhoto(path: String) = NSData.dataWithContentsOfFile(path)?.toByteArray()

private object IosScanPhoto : ScanPhotoUi {
    @Composable
    override fun SheetPhoto(uri: String, contentDescription: String, modifier: Modifier) {
        val bitmap by produceState<ImageBitmap?>(null, uri) {
            value = withContext(Dispatchers.IO) { readScanPhoto(uri)?.let(::decodePhotoBitmap) }
        }
        val image = bitmap
        if (image == null) {
            Box(modifier)
        } else {
            Image(image, contentDescription, modifier, contentScale = ContentScale.Fit)
        }
    }
}

private object IosScanDraftStore : RoutineScanDraftStore {
    private val files get() = NSFileManager.defaultManager
    private fun directory() = iosDocumentsPath()?.let { "$it/scan_drafts" }
    private fun imagePath(directory: String, planId: Int) = "$directory/plan_${planId}.jpg"
    private fun jsonPath(directory: String, planId: Int) = "$directory/plan_${planId}.json"

    override fun create(
        planId: Int,
        entries: List<ScannedExerciseEntry>,
        sourceImage: String
    ): String? {
        val directory = directory() ?: return null
        val target = imagePath(directory, planId)
        val photo = NSData.dataWithContentsOfFile(sourceImage)
        val savedPhoto = photo != null && prepareDirectory(directory) && photo.writeToFile(target, atomically = true)
        if (!savedPhoto) files.removeItemAtPath(target, null)
        save(planId, entries)
        return target.takeIf { savedPhoto }
    }

    override fun save(planId: Int, entries: List<ScannedExerciseEntry>) {
        val directory = directory() ?: return
        if (prepareDirectory(directory)) {
            ScanDraftCodec.encode(entries, Clock.System.now().toEpochMilliseconds()).encodeToByteArray()
                .toNSData().writeToFile(jsonPath(directory, planId), atomically = true)
        }
    }

    override fun load(planId: Int): ScanDraft? {
        val directory = directory() ?: return null
        val content = NSData.dataWithContentsOfFile(jsonPath(directory, planId))?.toByteArray() ?: return null
        val image = imagePath(directory, planId).takeIf(files::fileExistsAtPath)
        return ScanDraftCodec.decode(content.decodeToString(), image)
    }

    override fun clear(planId: Int) {
        val directory = directory() ?: return
        files.removeItemAtPath(jsonPath(directory, planId), null)
        files.removeItemAtPath(imagePath(directory, planId), null)
    }

    private fun prepareDirectory(path: String) = files.createDirectoryAtPath(path, true, null, null)
}
