package com.emanuel5014.trainable.ui.components.heart

import androidx.compose.runtime.Composable

/** The platform's health store as the screens see it: Health Connect on Android, none on iOS yet. */
class HealthAccess(
    /** The store exists on this phone, or can be installed ([needsInstall]). */
    val available: Boolean,
    /** The store must be installed or updated first (Android 13 and older); [request] opens the Play Store. */
    val needsInstall: Boolean,
    /** Asks for the read permissions; [onResult] gets whether all of them are granted. */
    val request: (onResult: (Boolean) -> Unit) -> Unit,
    /** Whether every read permission is granted now. */
    val isGranted: suspend () -> Boolean
)

@Composable
expect fun rememberHealthAccess(): HealthAccess
