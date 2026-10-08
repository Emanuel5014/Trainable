package com.emanuel5014.trainable.domain.prescription

/** How the reps of a plain exercise are written: one number, a range, or one number per set. */
enum class RepsMode { FIXED, RANGE, PER_SET }

/** What is wrong with the reps typed in the exercise editor, so the field can say it. */
enum class RepsError { NOT_A_NUMBER, MAX_NOT_ABOVE_MIN, BAD_LIST }

/** The reps of an exercise as stored: `reps_target` and, for a range, its upper end. */
data class RepsTarget(val repsTarget: String, val repsMax: Int?)

/**
 * The reps fields of the exercise editor while they are being typed. Keeps what each mode needs apart so
 * switching mode back and forth never loses a value, and turns them into the stored [RepsTarget] once valid.
 */
data class RepsDraft(
    val mode: RepsMode = RepsMode.FIXED,
    val fixed: String = DEFAULT_REPS.toString(),
    val min: String = "",
    val max: String = "",
    val list: String = ""
) {
    private val fixedValue: Int? get() = fixed.trim().toIntOrNull()?.takeIf { it in 1..RepRange.MAX_REPS }
    private val minValue: Int? get() = min.trim().toIntOrNull()?.takeIf { it in 1..RepRange.MAX_REPS }
    private val maxValue: Int? get() = max.trim().toIntOrNull()?.takeIf { it in 1..RepRange.MAX_REPS }
    private val listValues: List<Int>? get() = parseList(list)

    /** The values to store, or null while the draft is not valid. */
    val target: RepsTarget?
        get() = when (mode) {
            RepsMode.FIXED -> fixedValue?.let { RepsTarget(it.toString(), null) }
            RepsMode.RANGE -> RepRange.of(minValue, maxValue)?.let { RepsTarget(it.min.toString(), it.max) }
            RepsMode.PER_SET -> listValues?.let { RepsTarget(it.joinToString("-"), null) }
        }

    val isValid: Boolean get() = target != null

    /** The problem to show under the fields: only for something typed, a blank field just keeps Save off. */
    val error: RepsError?
        get() = when (mode) {
            RepsMode.FIXED -> if (fixed.isNotBlank() && fixedValue == null) RepsError.NOT_A_NUMBER else null
            RepsMode.RANGE -> when {
                (min.isNotBlank() && minValue == null) || (max.isNotBlank() && maxValue == null) -> RepsError.NOT_A_NUMBER
                minValue != null && maxValue != null && maxValue!! <= minValue!! -> RepsError.MAX_NOT_ABOVE_MIN
                else -> null
            }
            // A trailing hyphen is just the next number being typed
            RepsMode.PER_SET -> if (list.isNotBlank() && listValues == null && !list.trimEnd().endsWith('-')) RepsError.BAD_LIST else null
        }

    /** Number of sets a per-set list spells out (null for a single number, which follows the sets field). */
    val perSetCount: Int? get() = if (mode == RepsMode.PER_SET) listValues?.size?.takeIf { it > 1 } else null

    /** The mode's first and last number, whatever has been typed, used to carry values across modes. */
    private fun firstAndLast(): Pair<Int?, Int?> = when (mode) {
        RepsMode.FIXED -> fixedValue.let { it to it }
        RepsMode.RANGE -> minValue to maxValue
        RepsMode.PER_SET -> {
            val numbers = list.split("-").mapNotNull { it.trim().toIntOrNull() }
            numbers.firstOrNull() to numbers.lastOrNull()
        }
    }

    fun withFixed(text: String) = copy(fixed = digits(text))
    fun withMin(text: String) = copy(min = digits(text))
    fun withMax(text: String) = copy(max = digits(text))
    fun withList(text: String) = copy(list = text.filter { it.isDigit() || it == '-' }.take(MAX_LIST_LENGTH))

    /** Switches mode, carrying over what was typed: 8 → 8–12 → 8 again, a list's ends become the range's ends. */
    fun withMode(newMode: RepsMode): RepsDraft {
        if (newMode == mode) return this
        val (first, last) = firstAndLast()
        val start = (first ?: DEFAULT_REPS).coerceIn(1, RepRange.MAX_REPS)
        return when (newMode) {
            RepsMode.FIXED -> copy(mode = newMode, fixed = start.toString())
            RepsMode.PER_SET -> copy(mode = newMode, list = start.toString())
            RepsMode.RANGE -> {
                // A list of 10-12-15 already says where the range ends; a single number needs a sensible top
                val top = last?.takeIf { mode == RepsMode.PER_SET && it > start && it <= RepRange.MAX_REPS } ?: defaultMax(start)
                // Coming back to a range typed earlier keeps it
                if (min.isNotBlank() && max.isNotBlank() && mode != RepsMode.PER_SET && start == minValue) {
                    copy(mode = newMode)
                } else {
                    copy(mode = newMode, min = start.toString(), max = top.toString())
                }
            }
        }
    }

    companion object {
        const val DEFAULT_REPS = 8
        private const val MAX_DIGITS = 3
        private const val MAX_LIST_LENGTH = 40

        /** A starting top for a range typed from a single number: 8 → 12, 10 → 15. */
        fun defaultMax(min: Int): Int = (min + if (min >= 10) 5 else 4).coerceAtMost(RepRange.MAX_REPS)

        private fun digits(text: String) = text.filter { it.isDigit() }.take(MAX_DIGITS)

        /** "8-6-4-2" → [8, 6, 4, 2]; null when any part is not a whole number from 1 to 100. */
        private fun parseList(text: String): List<Int>? {
            if (text.isBlank()) return null
            val numbers = text.split("-").map { it.trim().toIntOrNull() }
            if (numbers.any { it == null || it !in 1..RepRange.MAX_REPS }) return null
            return numbers.filterNotNull()
        }

        /** Rebuilds the editor state from a stored plan exercise. */
        fun from(repsTarget: String, repsMax: Int?): RepsDraft {
            val low = repsTarget.trim().toIntOrNull()
            val range = RepRange.of(low, repsMax)
            if (range != null) return RepsDraft(RepsMode.RANGE, min = range.min.toString(), max = range.max.toString())
            val numbers = repsTarget.split("-").mapNotNull { it.trim().toIntOrNull() }
            return if (numbers.size > 1) {
                RepsDraft(RepsMode.PER_SET, list = repsTarget.trim())
            } else {
                RepsDraft(RepsMode.FIXED, fixed = numbers.firstOrNull()?.toString() ?: repsTarget.trim())
            }
        }
    }
}
