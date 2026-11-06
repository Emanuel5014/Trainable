@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.components.BottomBarManager
import com.emanuel5014.trainable.ui.components.LocalPrescriptionText
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.components.WorkoutShareCardContent
import com.emanuel5014.trainable.ui.screens.history.HistoryShareUi
import com.emanuel5014.trainable.ui.util.PlatformBackHandler
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.EncodedImageFormat
import platform.CoreGraphics.CGRectMake
import platform.Foundation.*
import platform.Photos.*
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController
import kotlin.coroutines.resume

private data class HistoryImageRequest(
    val session: SessionWithDetails,
    val planName: String,
    val exercises: List<PlanExerciseWithDetails>?
)

private data class HistoryImage(val bitmap: ImageBitmap, val png: ByteArray)

/** iOS owns capture/preview/native sharing; the exported card itself is the same Compose content as Android. */
@Composable
internal fun rememberIosHistoryShareUi(): HistoryShareUi {
    var request by remember { mutableStateOf<HistoryImageRequest?>(null) }
    var captured by remember { mutableStateOf<HistoryImage?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val presenter = LocalUIViewController.current
    val show by rememberUpdatedState(LocalShowMessage.current)
    val shareSession = remember(presenter) { HistoryImageShareSession() }

    DisposableEffect(shareSession) {
        onDispose {
            shareSession.controller?.dismissViewControllerAnimated(false, null)
            shareSession.close()
        }
    }

    return remember(scope, presenter) {
        object : HistoryShareUi {
            override val available = true
            override fun share(session: SessionWithDetails, planName: String, planExercises: List<PlanExerciseWithDetails>?) {
                if (request == null && captured == null) request = HistoryImageRequest(session, planName, planExercises)
            }

            @Composable
            override fun Overlay(languageCode: String, weightUnit: String, showPrescription: Boolean) {
                val active = request != null || captured != null
                DisposableEffect(active) {
                    val visible = BottomBarManager.isVisibleOverride
                    val locked = BottomBarManager.swipeLocked
                    if (active) {
                        BottomBarManager.isVisibleOverride = false
                        BottomBarManager.swipeLocked = true
                    }
                    onDispose {
                        if (active) {
                            BottomBarManager.isVisibleOverride = visible
                            BottomBarManager.swipeLocked = locked
                        }
                    }
                }
                fun dismiss() { if (!saving) { request = null; captured = null } }
                PlatformBackHandler(active) { dismiss() }
                val failed = stringResource(Res.string.export_failed)
                val saved = stringResource(Res.string.image_saved_gallery)
                val failedSave = stringResource(Res.string.failed_save_image)
                request?.let { pending ->
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.76f)).clickable {}.zIndex(100f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(Res.string.preparing_share), color = Color.White)
                        }
                        key(pending) {
                            RenderHistoryImage(pending, languageCode, weightUnit, showPrescription,
                                onReady = { captured = it; request = null },
                                onFailed = { request = null; show(failed) })
                        }
                    }
                }
                captured?.let { result ->
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.76f)).clickable { dismiss() }.zIndex(101f), contentAlignment = Alignment.Center) {
                        Card(Modifier.padding(24.dp).fillMaxWidth().clickable {}, shape = MaterialTheme.shapes.extraLarge) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(Res.string.share_preview), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                    IconButton(onClick = { dismiss() }, enabled = !saving) {
                                        Icon(Icons.Rounded.Close, stringResource(Res.string.close))
                                    }
                                }
                                Box(Modifier.weight(1f, fill = false).heightIn(max = 460.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
                                    Image(result.bitmap, null, Modifier.fillMaxWidth().aspectRatio(result.bitmap.width.toFloat() / result.bitmap.height), contentScale = ContentScale.FillWidth)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(onClick = {
                                        if (!saving) {
                                            saving = true
                                            scope.launch {
                                                try { show(if (saveHistoryImageToPhotos(result.png)) saved else failedSave) }
                                                catch (error: CancellationException) { throw error }
                                                catch (_: Exception) { show(failedSave) }
                                                finally { saving = false }
                                            }
                                        }
                                    }, enabled = !saving, modifier = Modifier.weight(1f)) {
                                        if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                        else Icon(Icons.Rounded.Download, null)
                                        Spacer(Modifier.width(8.dp)); Text(stringResource(Res.string.save))
                                    }
                                    Button(onClick = {
                                        if (shareSession.directory == null) scope.launch {
                                            try { shareSession.present(result.png, presenter) { if (it) show(failed) } }
                                            catch (error: CancellationException) { throw error }
                                            catch (_: Exception) { show(failed) }
                                        }
                                    }, enabled = !saving, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Rounded.Share, null)
                                        Spacer(Modifier.width(8.dp)); Text(stringResource(Res.string.share))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A full-height unbounded card is recorded without drawing it onto the app's visible canvas. */
@Composable
private fun RenderHistoryImage(
    request: HistoryImageRequest, language: String, unit: String, prescription: Boolean,
    onReady: (HistoryImage) -> Unit, onFailed: () -> Unit
) {
    val layer = rememberGraphicsLayer()
    val drawn = remember { CompletableDeferred<Unit>() }
    val text = LocalPrescriptionText.current
    val logo = painterResource(Res.drawable.ic_app_logo)
    val name = stringResource(Res.string.app_name)
    // Predictable export resolution and legible text, independent of accessibility text scaling.
    CompositionLocalProvider(LocalDensity provides Density(2f, 1f)) {
        val density = LocalDensity.current
        Layout(modifier = Modifier.clearAndSetSemantics {}, content = {
            WorkoutShareCardContent(request.session, request.planName, language, unit, request.exercises, prescription,
                modifier = Modifier.drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawn.complete(Unit)
                }, logo = logo, appName = name,
                setDetailsText = { set, includePrescription ->
                    ((if (includePrescription) text.setPrescriptionBadges(set) else emptyList()) + text.setPerformanceBadges(set))
                        .takeIf { it.isNotEmpty() }?.joinToString(" · ")
                })
        }) { children, _ ->
            val width = with(density) { 402.dp.roundToPx() }
            val card = children.single().measure(Constraints(minWidth = width, maxWidth = width))
            if (card.height > 16384 || card.width.toLong() * card.height > 24_000_000) {
                drawn.completeExceptionally(IllegalArgumentException("Share image too large"))
                layout(1, 1) {}
            } else layout(1, 1) { card.place(0, 0) }
        }
    }
    LaunchedEffect(layer) {
        try {
            drawn.await()
            withFrameNanos { }; withFrameNanos { }
            val bitmap = layer.toImageBitmap()
            val png = withContext(Dispatchers.Default) { encodeHistoryPng(bitmap) }
            onReady(HistoryImage(bitmap, png))
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { onFailed() }
    }
}

internal fun encodeHistoryPng(bitmap: ImageBitmap): ByteArray {
    val image = org.jetbrains.skia.Image.makeFromBitmap(bitmap.asSkiaBitmap())
    try {
        val data = checkNotNull(image.encodeToData(EncodedImageFormat.PNG))
        try { return data.bytes } finally { data.close() }
    } finally { image.close() }
}

private fun ByteArray.historyData(): NSData = usePinned { NSData.dataWithBytes(it.addressOf(0), size.toULong()) }

/** Add-only access: saving an export never needs permission to read the user's photo library. */
internal suspend fun saveHistoryImageToPhotos(png: ByteArray): Boolean {
    val status = suspendCancellableCoroutine<PHAuthorizationStatus> { continuation ->
        PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { value ->
            if (continuation.isActive) continuation.resume(value)
        }
    }
    if (status != PHAuthorizationStatusAuthorized && status != PHAuthorizationStatusLimited) return false
    val data = png.historyData()
    return suspendCancellableCoroutine { continuation ->
        PHPhotoLibrary.sharedPhotoLibrary().performChanges({
            PHAssetCreationRequest.creationRequestForAsset().addResourceWithType(PHAssetResourceTypePhoto, data, null)
        }, completionHandler = { success, _ -> if (continuation.isActive) continuation.resume(success) })
    }
}

private class HistoryImageShareSession {
    var directory: String? = null
    var controller: UIActivityViewController? = null
    private var disposed = false

    suspend fun present(png: ByteArray, presenter: platform.UIKit.UIViewController, onFinished: (Boolean) -> Unit) {
        if (disposed || directory != null) return
        val folder = NSTemporaryDirectory() + "history-image-" + NSUUID().UUIDString
        directory = folder
        var presented = false
        try {
            val url = withContext(Dispatchers.IO) {
                check(NSFileManager.defaultManager.createDirectoryAtPath(folder, true, null, null))
                NSURL.fileURLWithPath("$folder/workout_share.png").also { check(png.historyData().writeToURL(it, true)) }
            }
            if (disposed) return
            val sheet = UIActivityViewController(listOf(url), null)
            sheet.completionWithItemsHandler = { _, _, _, error -> cleanup(); onFinished(error != null) }
            var top = presenter
            while (top.presentedViewController != null) top = top.presentedViewController!!
            sheet.popoverPresentationController?.apply {
                sourceView = top.view
                sourceRect = CGRectMake(top.view.bounds.useContents { size.width / 2 }, top.view.bounds.useContents { size.height / 2 }, 0.0, 0.0)
            }
            controller = sheet
            top.presentViewController(sheet, true, null)
            presented = true
        } finally {
            if (!presented) { cleanup(); NSFileManager.defaultManager.removeItemAtPath(folder, null) }
        }
    }

    fun close() { disposed = true; cleanup() }
    private fun cleanup() {
        controller?.completionWithItemsHandler = null
        controller = null
        directory?.let { NSFileManager.defaultManager.removeItemAtPath(it, null) }
        directory = null
    }
}
