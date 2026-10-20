package com.emanuel5014.trainable.domain.journal

/** The colours a tag can have are a short fixed list; a tag stores the position of its colour. */
object TagPalette {
    const val SIZE = 6

    /** The slot for a new tag: the least used one so far, the lowest index on a tie. */
    fun nextSlot(usedSlots: List<Int>): Int {
        val counts = IntArray(SIZE)
        usedSlots.forEach { if (it in 0 until SIZE) counts[it]++ }
        return counts.indices.minByOrNull { counts[it] } ?: 0
    }
}
