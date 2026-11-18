package com.emanuel5014.trainable.ui.components.heart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** No health store on iOS yet (Apple Health comes later): saved heart data is still shown. */
@Composable
actual fun rememberHealthAccess(): HealthAccess = remember {
    HealthAccess(available = false, needsInstall = false, request = { it(false) }, isGranted = { false })
}
