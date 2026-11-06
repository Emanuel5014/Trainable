package com.emanuel5014.trainable.ui.theme

import androidx.compose.runtime.Composable

/** The shared Kyant glass plus Android's blur behind sheets and dialogs (OverlayGlass.kt), provided at the root. */
object AndroidPlatformGlass : KyantPlatformGlass() {
    @Composable
    override fun NoOverlayBlur() = com.emanuel5014.trainable.ui.theme.NoOverlayBlur()
}
