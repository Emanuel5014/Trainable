package com.emanuel5014.trainable.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity

@Composable
actual fun rememberResponsiveSize() {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    ResponsiveSize.update(
        widthDp = config.screenWidthDp,
        heightDp = config.screenHeightDp,
        fontScale = density.fontScale,
        densityDpi = config.densityDpi
    )
}
