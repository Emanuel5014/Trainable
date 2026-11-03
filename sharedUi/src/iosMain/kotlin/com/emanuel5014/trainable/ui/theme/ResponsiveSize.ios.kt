package com.emanuel5014.trainable.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
actual fun rememberResponsiveSize() {
    val window = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    ResponsiveSize.update(
        widthDp = (window.width / density.density).toInt(),
        heightDp = (window.height / density.density).toInt(),
        fontScale = density.fontScale,
        // Android's dpi scale, where 160 dpi is 1 px per dp
        densityDpi = (density.density * 160).toInt()
    )
}
