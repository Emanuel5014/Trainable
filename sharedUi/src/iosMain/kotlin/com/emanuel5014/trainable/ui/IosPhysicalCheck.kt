@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.*
import androidx.compose.ui.interop.LocalUIViewController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.emanuel5014.trainable.data.repository.IosPhysicalCheckFiles
import com.emanuel5014.trainable.data.repository.toByteArray
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.physical_check_auth_reason
import com.emanuel5014.trainable.sharedui.resources.physical_check_camera_denied
import com.emanuel5014.trainable.sharedui.resources.physical_check_camera_unavailable
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.screens.physicalcheck.LocalPhysicalCheckPlatform
import com.emanuel5014.trainable.ui.screens.physicalcheck.PhotoCapture
import com.emanuel5014.trainable.ui.screens.physicalcheck.PhysicalCheckModel
import com.emanuel5014.trainable.ui.screens.physicalcheck.PhysicalCheckPlatformUi
import org.jetbrains.compose.resources.stringResource
import platform.AVFoundation.*
import platform.Foundation.*
import platform.LocalAuthentication.*
import platform.PhotosUI.*
import platform.UIKit.*
import platform.darwin.*

/** One physical-check screen on iOS: its model is kept by the navigation entry, like Android's ViewModel. */
@Composable
internal fun IosPhysicalCheckScreen(entry: NavBackStackEntry, content: @Composable (PhysicalCheckModel) -> Unit) {
    val owner = viewModel<IosPhysicalCheckOwner>(viewModelStoreOwner = entry) { IosPhysicalCheckOwner() }
    CompositionLocalProvider(LocalPhysicalCheckPlatform provides IosPhysicalCheckPlatform) {
        content(owner.model)
    }
}

/** The model plus the app going to the background and back (Android: ProcessLifecycleOwner). */
private class IosPhysicalCheckOwner : ViewModel() {
    val model = PhysicalCheckModel(IosAppGraph.physicalChecks, IosAppGraph.preferences, viewModelScope)
    private val center = NSNotificationCenter.defaultCenter
    private val observers = listOf(
        center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) { _ ->
            model.onAppStop()
        },
        center.addObserverForName(UIApplicationWillEnterForegroundNotification, null, NSOperationQueue.mainQueue) { _ ->
            model.onAppStart()
        }
    )

    override fun onCleared() {
        observers.forEach { center.removeObserver(it) }
        super.onCleared()
    }
}

/** Face ID / Touch ID or the passcode; without any (no passcode set) the section opens, as on Android. */
@Composable
internal fun rememberIosBiometricPrompt(): (onSuccess: () -> Unit, onError: () -> Unit) -> Unit {
    val reason = stringResource(Res.string.physical_check_auth_reason)
    return remember(reason) {
        { onSuccess, onError ->
            val context = LAContext()
            if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)) {
                onSuccess()
            } else {
                context.evaluatePolicy(LAPolicyDeviceOwnerAuthentication, reason) { success, _ ->
                    dispatch_async(dispatch_get_main_queue()) { if (success) onSuccess() else onError() }
                }
            }
        }
    }
}

private object IosPhysicalCheckPlatform : PhysicalCheckPlatformUi {

    @Composable
    override fun rememberPhotoCapture(
        onPicked: (List<ByteArray>) -> Unit,
        onCameraResult: (success: Boolean, bytes: ByteArray?) -> Unit,
        onCameraDenied: () -> Unit
    ): PhotoCapture = rememberIosPhotoCapture(
        selectionLimit = 0,
        deniedText = stringResource(Res.string.physical_check_camera_denied),
        onPicked = onPicked,
        onCameraResult = onCameraResult,
        onCameraDenied = onCameraDenied
    )

    @Composable
    override fun rememberBiometricPrompt(): ((onSuccess: () -> Unit, onError: () -> Unit) -> Unit)? =
        rememberIosBiometricPrompt()

    /** No crop editor on iOS yet: the photo is kept as picked (already upright and made smaller). */
    @Composable
    override fun ImageEditor(imageBytes: ByteArray, onDismiss: () -> Unit, onConfirm: (ByteArray) -> Unit) {
        val confirm by rememberUpdatedState(onConfirm)
        LaunchedEffect(imageBytes) { confirm(imageBytes) }
    }
}

/**
 * Camera (asking for access first) and photo library. Photos arrive upright, JPEG, at most 1280 px.
 * [selectionLimit] 0 lets the user pick many photos, 1 one. [deniedText] is shown when camera access is refused.
 */
@Composable
internal fun rememberIosPhotoCapture(
    selectionLimit: Long,
    deniedText: String,
    onPicked: (List<ByteArray>) -> Unit,
    onCameraResult: (success: Boolean, bytes: ByteArray?) -> Unit,
    onCameraDenied: () -> Unit
): PhotoCapture {
    val presenter = LocalUIViewController.current
    val picked by rememberUpdatedState(onPicked)
    val cameraResult by rememberUpdatedState(onCameraResult)
    val denied by rememberUpdatedState(onCameraDenied)
    val show = LocalShowMessage.current
    val deniedMessage by rememberUpdatedState(deniedText)
    val unavailableText = stringResource(Res.string.physical_check_camera_unavailable)
    var alive by remember { mutableStateOf(true) }
    DisposableEffect(presenter) { onDispose { alive = false } }

    val galleryDelegate = remember {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                val results = didFinishPicking.filterIsInstance<PHPickerResult>()
                picker.dismissViewControllerAnimated(true) {
                    loadPhotos(results) { photos -> if (alive) picked(photos) }
                }
            }
        }
    }
    val cameraDelegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                picker.dismissViewControllerAnimated(true) {
                    val bytes = image?.let { UIImageJPEGRepresentation(it, 0.95) }?.toByteArray()
                        ?.let { IosPhysicalCheckFiles().compressImage(it) }
                    if (alive) cameraResult(true, bytes)
                }
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true) { if (alive) cameraResult(false, null) }
            }
        }
    }

    return remember(presenter, galleryDelegate, cameraDelegate) {
        fun presentCamera() {
            val picker = UIImagePickerController().apply {
                sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                delegate = cameraDelegate
            }
            topController(presenter).presentViewController(picker, animated = true, completion = null)
        }
        PhotoCapture(
            takePhoto = {
                if (!UIImagePickerController.isSourceTypeAvailable(
                        UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
                    cameraResult(false, null)
                    show(unavailableText)
                } else when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
                    AVAuthorizationStatusAuthorized -> presentCamera()
                    AVAuthorizationStatusNotDetermined ->
                        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                            dispatch_async(dispatch_get_main_queue()) {
                                if (granted) presentCamera() else { denied(); show(deniedMessage) }
                            }
                        }
                    else -> { denied(); show(deniedMessage) }
                }
            },
            pickPhotos = {
                val configuration = PHPickerConfiguration().apply {
                    filter = PHPickerFilter.imagesFilter
                    this.selectionLimit = selectionLimit
                    preferredAssetRepresentationMode = PHPickerConfigurationAssetRepresentationModeCurrent
                }
                val picker = PHPickerViewController(configuration).apply { delegate = galleryDelegate }
                topController(presenter).presentViewController(picker, animated = true, completion = null)
            }
        )
    }
}

/**
 * Each picked photo as an upright JPEG of at most 1280 px (HEIC and other formats included), in the picked order;
 * photos that cannot be read are left out. [done] runs on the main thread.
 */
private fun loadPhotos(results: List<PHPickerResult>, done: (List<ByteArray>) -> Unit) {
    if (results.isEmpty()) { done(emptyList()); return }
    val photos = arrayOfNulls<ByteArray>(results.size)
    var pending = results.size
    results.forEachIndexed { index, result ->
        result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
            val bytes = data?.let { d ->
                UIImage.imageWithData(d)?.let { IosPhysicalCheckFiles().compressImage(d.toByteArray()) }
            }
            dispatch_async(dispatch_get_main_queue()) {
                photos[index] = bytes
                pending--
                if (pending == 0) done(photos.filterNotNull())
            }
        }
    }
}

private fun topController(root: UIViewController): UIViewController {
    var top = root
    while (top.presentedViewController != null) top = top.presentedViewController!!
    return top
}
