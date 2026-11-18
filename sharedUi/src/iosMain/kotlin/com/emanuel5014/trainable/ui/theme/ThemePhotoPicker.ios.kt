package com.emanuel5014.trainable.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import com.emanuel5014.trainable.ui.rememberIosPhotoCapture
import com.emanuel5014.trainable.ui.screens.physicalcheck.decodePhotoBitmap

/** PHPicker, gallery only: no permission, and the photo is only read, never kept. */
@Composable
actual fun rememberThemePhotoPicker(onPicked: (ImageBitmap) -> Unit): () -> Unit {
    val picked by rememberUpdatedState(onPicked)
    val capture = rememberIosPhotoCapture(
        selectionLimit = 1,
        deniedText = "",
        onPicked = { photos -> photos.firstOrNull()?.let { decodePhotoBitmap(it) }?.let { picked(it) } },
        onCameraResult = { _, _ -> },
        onCameraDenied = {}
    )
    return remember(capture) { capture.pickPhotos }
}
