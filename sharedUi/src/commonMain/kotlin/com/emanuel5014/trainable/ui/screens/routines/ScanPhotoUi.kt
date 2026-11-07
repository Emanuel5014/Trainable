package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Draws the scanned sheet's photo in the review sheet at full resolution, fitted; each platform loads it its own way. */
interface ScanPhotoUi {
    @Composable
    fun SheetPhoto(uri: String, contentDescription: String, modifier: Modifier)
}
