package com.emanuel5014.trainable.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/** A dialog that covers the whole screen, e.g. a photo viewer (Android: the old usePlatformDefaultWidth = false). */
expect fun fullscreenDialogProperties(): DialogProperties

/** Keeps controls at the top of a [fullscreenDialogProperties] dialog clear of the status bar where it draws under it. */
expect fun Modifier.fullscreenDialogTopInset(): Modifier
