package com.emanuel5014.trainable.ui.screens.routines

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/** The review sheet's photo through coil, exactly as AiScanPreviewSheet loaded it before it was shared. */
object AndroidScanPhoto : ScanPhotoUi {
    @Composable
    override fun SheetPhoto(uri: String, contentDescription: String, modifier: Modifier) {
        val context = LocalContext.current
        // Load full original image resolution so text is sharp when zoomed
        val imageRequest = remember(uri) {
            ImageRequest.Builder(context)
                .data(Uri.parse(uri))
                .size(coil.size.Size.ORIGINAL)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}
