package com.emanuel5014.trainable.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The radius of the screen's own rounded corners at the bottom, as the system reports it: Android 12+ from the
 * window's RoundedCorner, iOS from the screen. 0 for a square screen, null when the platform does not say.
 */
@Composable
expect fun rememberDisplayCornerRadius(): Dp?

/**
 * The corner radius of a surface set [inset] in from the screen's rounded corner, so both curves share one centre
 * (concentric corners): the screen's radius less the gap, never under [minimum]. Null when the screen's radius is not
 * known, so the caller keeps its own.
 */
fun concentricCornerRadius(displayRadius: Dp?, inset: Dp, minimum: Dp = 16.dp): Dp? =
    displayRadius?.let { (it - inset).coerceAtLeast(minimum) }
