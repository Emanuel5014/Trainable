package com.emanuel5014.trainable.domain.journal

/**
 * What the lifter says about a whole workout: how it went, which tags apply, a free note. Every part is optional.
 * (The name of a workout is something else: `WorkoutSessionEntity.noteSessione`.)
 */
data class SessionJournal(
    val rating: Int? = null,
    val note: String? = null,
    val tagIds: Set<Int> = emptySet()
) {
    val isEmpty: Boolean get() = rating == null && note.isNullOrBlank() && tagIds.isEmpty()

    /** The journal as it is stored: rating on the scale, note trimmed and capped (blank = none). */
    fun cleaned(): SessionJournal = SessionJournal(
        rating = JournalRating.valid(rating),
        note = note?.trim()?.takeWholeChars(MAX_NOTE)?.trimEnd()?.ifEmpty { null },
        tagIds = tagIds
    )

    companion object {
        const val MAX_NOTE = 2000
    }
}
