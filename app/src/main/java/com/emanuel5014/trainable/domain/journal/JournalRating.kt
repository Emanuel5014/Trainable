package com.emanuel5014.trainable.domain.journal

/** The 1..5 star rating of a workout. */
object JournalRating {
    const val MIN = 1
    const val MAX = 5

    private val outOfFive = Regex("^(\\d+)\\s*/\\s*5$")

    /** The rating when it is on the scale, null otherwise. */
    fun valid(value: Int?): Int? = value?.takeIf { it in MIN..MAX }

    /** Reads `4` or `4/5` (what a hand-edited CSV holds); anything else is not a rating. */
    fun parse(text: String): Int? {
        val trimmed = text.trim()
        trimmed.toIntOrNull()?.let { return valid(it) }
        return outOfFive.matchEntire(trimmed)?.groupValues?.get(1)?.toIntOrNull()?.let(::valid)
    }
}

/** The groups the history can be filtered by: 1–2 stars, 3 stars, 4–5 stars, and workouts without a rating. */
enum class RatingBucket {
    LOW, MID, HIGH, UNRATED;

    fun matches(rating: Int?): Boolean = when (this) {
        LOW -> rating != null && rating in 1..2
        MID -> rating == 3
        HIGH -> rating != null && rating in 4..5
        UNRATED -> rating == null
    }
}
