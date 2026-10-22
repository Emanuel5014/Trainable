package com.emanuel5014.trainable.ui.components.motion

import kotlin.math.abs
import kotlin.math.min

/**
 * The numbers behind the motion of the workout screen. Plain math, so it can be tested without a screen.
 */

/** How a row of a wheel picker shrinks and fades the further it is from the centre line. */
object WheelLens {
    /** Distance, in rows, from which a row stays at its floor. */
    private const val FLOOR_DISTANCE = 1.5f
    private const val MIN_SCALE = 0.75f
    private const val MIN_ALPHA = 0.25f

    /** Scale of a row [distance] rows away from the centre (the side does not matter): 1 at the centre, 0.75 from 1.5 rows. */
    fun scale(distance: Float): Float = 1f - (1f - MIN_SCALE) * progress(distance)

    /** Opacity of a row [distance] rows away from the centre: 1 at the centre, 0.25 from 1.5 rows. */
    fun alpha(distance: Float): Float = 1f - (1f - MIN_ALPHA) * progress(distance)

    private fun progress(distance: Float): Float = min(abs(distance) / FLOOR_DISTANCE, 1f)
}

/** A pressed button of the workout tightens its corners and shrinks a little. */
object PressMorph {
    const val PRESSED_CORNER_DP = 10f
    const val PRESSED_SCALE = 0.96f

    /** Corner radius while [pressed] (0 at rest, 1 fully pressed) of a button whose corners are [restDp] at rest. */
    fun cornerDp(restDp: Float, pressed: Float): Float = restDp + (PRESSED_CORNER_DP - restDp) * pressed.coerceIn(0f, 1f)

    fun scale(pressed: Float): Float = 1f + (PRESSED_SCALE - 1f) * pressed.coerceIn(0f, 1f)
}

/** The wave of the rest ring is calm until the last ten seconds, then grows to its full height as the rest ends. */
object RestWave {
    private const val CALM = 0.6f
    private const val LAST_SECONDS = 10

    fun amplitude(remainingSeconds: Int): Float = when {
        remainingSeconds >= LAST_SECONDS -> CALM
        remainingSeconds <= 0 -> 1f
        else -> CALM + (1f - CALM) * (1f - remainingSeconds / LAST_SECONDS.toFloat())
    }
}
