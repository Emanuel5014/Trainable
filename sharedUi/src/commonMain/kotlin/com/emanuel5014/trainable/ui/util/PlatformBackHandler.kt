package com.emanuel5014.trainable.ui.util

import androidx.compose.runtime.Composable

/** Runs [onBack] instead of going back while [enabled]: the system back button on Android, the back gesture on iOS. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)
