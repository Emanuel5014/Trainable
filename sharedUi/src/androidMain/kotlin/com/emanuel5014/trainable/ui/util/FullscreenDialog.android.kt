package com.emanuel5014.trainable.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

actual fun fullscreenDialogProperties(): DialogProperties = DialogProperties(usePlatformDefaultWidth = false)

/** The Android dialog window already sits below the status bar. */
actual fun Modifier.fullscreenDialogTopInset(): Modifier = this
