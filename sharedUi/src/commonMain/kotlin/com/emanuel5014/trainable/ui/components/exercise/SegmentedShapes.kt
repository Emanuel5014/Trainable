package com.emanuel5014.trainable.ui.components

/** The corner radii, in dp, of the top and of the bottom of a row of a segmented list. */
data class SegmentCorners(val topDp: Float, val bottomDp: Float)

/** Where a row stands in its group: its [index] (from 0) among [count] rows. */
data class GroupPosition(val index: Int, val count: Int)

/**
 * The sets of a block as one segmented list: big corners at the ends of the group, small ones between its rows, and the
 * active row fully rounded so it stands out of the group. Plain math, so it can be tested without a screen.
 */
object SegmentedShapes {
    const val OUTER_DP = 20f
    const val INNER_DP = 4f

    fun corners(index: Int, count: Int, isActive: Boolean): SegmentCorners {
        if (isActive || count <= 1) return SegmentCorners(OUTER_DP, OUTER_DP)
        return SegmentCorners(
            topDp = if (index <= 0) OUTER_DP else INNER_DP,
            bottomDp = if (index >= count - 1) OUTER_DP else INNER_DP
        )
    }

    /** For each of [keys] (the block a set belongs to), its place in the run of neighbours with the same key. */
    fun <K> positions(keys: List<K>): List<GroupPosition> {
        val result = ArrayList<GroupPosition>(keys.size)
        var start = 0
        while (start < keys.size) {
            var end = start
            while (end + 1 < keys.size && keys[end + 1] == keys[start]) end++
            val count = end - start + 1
            for (i in 0 until count) result.add(GroupPosition(i, count))
            start = end + 1
        }
        return result
    }
}
