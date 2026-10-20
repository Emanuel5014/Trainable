package com.emanuel5014.trainable.ui.components

import kotlin.math.abs

/**
 * The numbers behind the bottom hub of the workout screen, which the lifter can drag between two steps.
 *
 * "Expansion" is 1 when the hub is open and 0 when it is a thin strip; everything in between is the hub
 * following the finger. All the functions are plain math so they can be tested without a screen.
 */
object HubMotion {
    const val OPEN = 1f
    const val STRIP = 0f

    /** How much a locked hub (one that needs its inputs) gives under the finger, as a share of the travel. */
    const val LOCKED_GIVE = 0.06f

    /** How much of the finger's movement a locked hub follows. */
    const val LOCKED_RESISTANCE = 0.3f

    /**
     * How present an element of the hub is: 0 while the hub is at [from] or lower, 1 from [to] up, a straight
     * line in between. Elements that matter less get a higher window so they leave first when the hub shrinks.
     */
    fun window(expansion: Float, from: Float, to: Float): Float {
        if (to <= from) return if (expansion >= to) 1f else 0f
        return ((expansion - from) / (to - from)).coerceIn(0f, 1f)
    }

    /** The expansion after the finger moved by [dragDeltaPx] (positive is down, which shrinks the hub). */
    fun dragExpansion(current: Float, dragDeltaPx: Float, travelPx: Float, locked: Boolean): Float {
        if (travelPx <= 0f) return current
        val moved = -dragDeltaPx / travelPx
        return if (locked) {
            (current + moved * LOCKED_RESISTANCE).coerceIn(OPEN - LOCKED_GIVE, OPEN)
        } else {
            (current + moved).coerceIn(STRIP, OPEN)
        }
    }

    /**
     * The step the hub settles on when the finger lifts. A fast fling decides by its direction, a slow release by
     * whichever step is closer. A locked hub always goes back open.
     * [velocityPxPerSec] is positive when the finger was moving down.
     */
    fun releaseTarget(expansion: Float, velocityPxPerSec: Float, flingVelocityPxPerSec: Float, locked: Boolean): Float = when {
        locked -> OPEN
        abs(velocityPxPerSec) >= flingVelocityPxPerSec -> if (velocityPxPerSec < 0f) OPEN else STRIP
        expansion >= 0.5f -> OPEN
        else -> STRIP
    }

    /** The step chosen by tapping the pill: the other one, unless the hub is locked. */
    fun tapTarget(detent: Float, locked: Boolean): Float = when {
        locked -> detent
        detent >= 0.5f -> STRIP
        else -> OPEN
    }

    /** Where the hub rests: open while locked, otherwise at the step the lifter chose. */
    fun restingTarget(detent: Float, locked: Boolean): Float = if (locked) OPEN else detent

    /** How far the finger travels between the two steps, never less than [minPx] so a drag stays controllable. */
    fun travelPx(openPx: Int, stripPx: Int, minPx: Float): Float = (openPx - stripPx).toFloat().coerceAtLeast(minPx)
}
