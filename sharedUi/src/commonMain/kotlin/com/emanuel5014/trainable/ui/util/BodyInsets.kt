package com.emanuel5014.trainable.ui.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Whether a screen's scrolling body runs to the bottom edge, under the system's bottom area, instead of stopping above
 * it: iOS apps scroll under the home indicator, while the Android app keeps the padding it always had.
 */
internal expect val bodyRunsToBottomEdge: Boolean

/**
 * The padding a screen's body takes from its Scaffold: all of it on Android; on iOS everything but the bottom, which
 * the scrolling content adds at its end instead ([scrollEndInset]) so its last row can still be scrolled above the
 * home indicator.
 */
@Composable
fun PaddingValues.bodyPadding(): PaddingValues {
    if (!bodyRunsToBottomEdge) return this
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction),
        top = calculateTopPadding(),
        end = calculateEndPadding(direction),
        bottom = 0.dp
    )
}

/** The room the end of a scrolling body keeps for what [bodyPadding] left out: 0 on Android. */
fun PaddingValues.scrollEndInset(): Dp = if (bodyRunsToBottomEdge) calculateBottomPadding() else 0.dp

/** [this] with [extra] more at the bottom (a list's content padding plus [scrollEndInset]). */
@Composable
fun PaddingValues.plusBottom(extra: Dp): PaddingValues {
    if (extra == 0.dp) return this
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction),
        top = calculateTopPadding(),
        end = calculateEndPadding(direction),
        bottom = calculateBottomPadding() + extra
    )
}
