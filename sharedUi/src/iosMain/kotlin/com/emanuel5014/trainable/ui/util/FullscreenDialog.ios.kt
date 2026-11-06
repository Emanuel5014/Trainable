package com.emanuel5014.trainable.ui.util

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

/** Edge to edge: without this the dialog stops at the safe area and the screen below shows at the bottom. */
actual fun fullscreenDialogProperties(): DialogProperties =
    DialogProperties(usePlatformDefaultWidth = false, usePlatformInsets = false)

actual fun Modifier.fullscreenDialogTopInset(): Modifier = statusBarsPadding()
