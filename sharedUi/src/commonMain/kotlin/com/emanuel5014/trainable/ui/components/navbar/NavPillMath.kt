package com.emanuel5014.trainable.ui.components

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pill position maths for the floating nav bar. A position is measured in items:
 * 0f is the pill over the first item, 1.5f halfway between the second and the third.
 */
object NavPillMath {

    private fun lastIndex(count: Int): Float = (count - 1).coerceAtLeast(0).toFloat()

    /** Pill centred under the finger; [fingerX] is measured from the left edge of the items row. */
    fun positionForFinger(fingerX: Float, cellWidth: Float, count: Int): Float {
        if (cellWidth <= 0f) return 0f
        return (fingerX / cellWidth - 0.5f).coerceIn(0f, lastIndex(count))
    }

    fun positionForPager(page: Int, offsetFraction: Float, count: Int): Float =
        (page + offsetFraction).coerceIn(0f, lastIndex(count))

    fun nearestIndex(position: Float, count: Int): Int =
        position.roundToInt().coerceIn(0, lastIndex(count).toInt())

    /** Like [nearestIndex] but stays on [current] until the pill is clearly closer to another item. */
    fun closestIndexWithHysteresis(current: Int, position: Float, count: Int, margin: Float = 0.1f): Int {
        val held = current.coerceIn(0, lastIndex(count).toInt())
        return if (abs(position - held) <= 0.5f + margin) held else nearestIndex(position, count)
    }

    /** 1f with the pill right on the item, fading to 0f one item away. */
    fun proximity(position: Float, index: Int): Float =
        (1f - abs(position - index)).coerceIn(0f, 1f)
}
