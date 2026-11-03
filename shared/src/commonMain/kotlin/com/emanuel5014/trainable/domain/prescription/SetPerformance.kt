package com.emanuel5014.trainable.domain.prescription

/**
 * How a set really went, beyond weight × reps: the parts of a set finished after short pauses ("2+1+1", rest-pause or
 * cluster), the reps a spotter helped with and the partial reps. The reps of the set are the full reps done alone,
 * all parts together; the straight reps (the first part) are what an estimated 1RM or a progression can trust.
 */
object SetPerformance {
    const val DEFAULT_PAUSE_SECONDS = 40
    private const val MAX_PARTS = 10
    private const val MAX_PAUSE_SECONDS = 3600

    /** "2+1+1" for a set in parts, null for one done in one go. */
    fun encodeParts(parts: List<Int>): String? = parts.takeIf { it.size > 1 }?.joinToString("+")

    fun decodeParts(raw: String?): List<Int> =
        raw?.split("+")?.mapNotNull { it.trim().toIntOrNull()?.takeIf { n -> n >= 0 } }.orEmpty()

    /** The parts typed by hand ("2+1+1", "2 + 2"); null unless every part is a whole number and there are 1 to 10. */
    fun parseParts(text: String): List<Int>? {
        val tokens = text.split("+").map { it.trim() }
        if (tokens.any { it.isEmpty() } || tokens.size > MAX_PARTS) return null
        val parts = tokens.map { it.toIntOrNull()?.takeIf { n -> n in 0..999 } ?: return null }
        return parts.takeIf { it.isNotEmpty() }
    }

    fun encodePauses(seconds: List<Int>): String? = seconds.takeIf { it.isNotEmpty() }?.joinToString(",")

    fun decodePauses(raw: String?): List<Int> =
        raw?.split(",")?.mapNotNull { it.trim().toIntOrNull()?.takeIf { s -> s >= 0 } }.orEmpty()

    /**
     * The pauses typed by hand for the [gaps] between the parts of a set, one per field: a pause left empty after the
     * last one given is dropped (it was not recorded), one left empty before it counts as 0 so the rest stay in place.
     */
    fun pausesFromInput(inputs: List<String>, gaps: Int): List<Int> {
        val values = (0 until gaps.coerceAtLeast(0)).map { i -> inputs.getOrNull(i)?.trim()?.toIntOrNull()?.takeIf { it in 0..MAX_PAUSE_SECONDS } }
        val last = values.indexOfLast { it != null }
        return if (last < 0) emptyList() else values.take(last + 1).map { it ?: 0 }
    }

    /** The parts after one more of [reps], starting from a set of [currentReps] done in one go when there were none. */
    fun withPart(parts: List<Int>, currentReps: Int, reps: Int): List<Int> =
        (parts.ifEmpty { listOf(currentReps) }) + reps

    /**
     * The parts after the total of the set was changed to [total]: the last part takes the difference, a part that drops
     * to nothing hands what is left to the one before it, and a single part left means the set is in one go again
     * (empty).
     */
    fun adjustParts(parts: List<Int>, total: Int): List<Int> {
        if (parts.isEmpty()) return emptyList()
        val adjusted = parts.toMutableList()
        adjusted[adjusted.lastIndex] = adjusted.last() + (total - parts.sum())
        while (adjusted.size > 1 && adjusted.last() <= 0) {
            val carry = adjusted.removeAt(adjusted.lastIndex)
            adjusted[adjusted.lastIndex] = adjusted.last() + carry
        }
        return if (adjusted.size > 1) adjusted else emptyList()
    }

    /** Reps the set still misses to reach [target]; 0 when it got there or has no target. */
    fun missingReps(target: Int?, reps: Int): Int = if (target == null || target <= 0) 0 else (target - reps).coerceAtLeast(0)
}
