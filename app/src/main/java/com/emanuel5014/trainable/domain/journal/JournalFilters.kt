package com.emanuel5014.trainable.domain.journal

/** The journal part of the history filters; the different kinds are ANDed, the values of one kind are not. */
data class JournalFilters(
    val tagIds: Set<Int> = emptySet(),
    /** With several tags: a workout needs all of them (true) or at least one (false). */
    val matchAllTags: Boolean = false,
    val ratingBuckets: Set<RatingBucket> = emptySet(),
    val onlyWithNote: Boolean = false
) {
    val isActive: Boolean get() = tagIds.isNotEmpty() || ratingBuckets.isNotEmpty() || onlyWithNote

    fun matches(rating: Int?, note: String?, sessionTagIds: Set<Int>): Boolean {
        if (tagIds.isNotEmpty()) {
            val hasTags = if (matchAllTags) sessionTagIds.containsAll(tagIds) else sessionTagIds.any { it in tagIds }
            if (!hasTags) return false
        }
        if (ratingBuckets.isNotEmpty() && ratingBuckets.none { it.matches(rating) }) return false
        if (onlyWithNote && note.isNullOrBlank()) return false
        return true
    }
}
