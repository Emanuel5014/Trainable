package com.emanuel5014.trainable.ui.util

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Android 12+ reports each rounded corner of the display; older versions say nothing (null). */
@Composable
actual fun rememberDisplayCornerRadius(): Dp? {
    val view = LocalView.current
    val density = LocalDensity.current
    return remember(view, density) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@remember null
        val insets = view.rootWindowInsets ?: return@remember null
        val left = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
        val right = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
        val px = minOf(left, right)
        if (px <= 0) 0.dp else with(density) { px.toDp() }
    }
}
