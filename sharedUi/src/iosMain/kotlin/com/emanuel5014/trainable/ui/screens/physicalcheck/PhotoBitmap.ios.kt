package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

@OptIn(ExperimentalResourceApi::class)
actual fun decodePhotoBitmap(bytes: ByteArray): ImageBitmap? =
    try { bytes.decodeToImageBitmap() } catch (e: Exception) { null }
