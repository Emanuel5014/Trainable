package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/** Starts taking a photo with the camera or picking photos from the gallery (see [PhysicalCheckPlatformUi]). */
class PhotoCapture(val takePhoto: () -> Unit, val pickPhotos: () -> Unit)

/** What the physical-check screens need from the platform: camera and gallery, biometric prompt, photo editor. */
interface PhysicalCheckPlatformUi {
    /**
     * A camera and gallery pair for one screen. [onPicked] receives the picked photos as they are (possibly none);
     * [onCameraResult] whether a photo was taken and its bytes, already made smaller (null when unreadable);
     * [onCameraDenied] runs after the camera permission was refused, once the platform has told the user.
     */
    @Composable
    fun rememberPhotoCapture(
        onPicked: (List<ByteArray>) -> Unit,
        onCameraResult: (success: Boolean, bytes: ByteArray?) -> Unit,
        onCameraDenied: () -> Unit
    ): PhotoCapture

    /**
     * Asks for the fingerprint/face or device credential, with [title] and [subtitle] in the app's language where the
     * platform shows them; null when no prompt can be shown here.
     */
    @Composable
    fun rememberBiometricPrompt(title: String, subtitle: String): ((onSuccess: () -> Unit, onError: () -> Unit) -> Unit)?

    /**
     * Keeps what the section shows private while it is composed: Android blocks screenshots and screen recording and
     * blanks the recent-apps preview (its sheets and dialogs inherit that); iOS covers the app in the app switcher.
     */
    @Composable
    fun KeepPrivate()

    /** Crop and rotate one photo; [onConfirm] gets the edited JPEG. */
    @Composable
    fun ImageEditor(imageBytes: ByteArray, onDismiss: () -> Unit, onConfirm: (ByteArray) -> Unit)
}

val LocalPhysicalCheckPlatform = staticCompositionLocalOf<PhysicalCheckPlatformUi> {
    error("No PhysicalCheckPlatformUi provided")
}

/** The photo decoded for display, or null when the bytes are not an image. */
expect fun decodePhotoBitmap(bytes: ByteArray): ImageBitmap?
