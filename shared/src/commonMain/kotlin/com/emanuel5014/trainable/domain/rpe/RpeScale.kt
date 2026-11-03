package com.emanuel5014.trainable.domain.rpe

/**
 * The scale RPE (perceived effort) is asked in. What is stored is always the number, so a value logged on one scale
 * reads fine on another; [values] are the ones a scale offers.
 */
enum class RpeScale(val id: Int, val values: List<Float>) {
    /** Five named levels, stored as their upper bound (2, 4, 6, 8, 10): for people who do not know what RPE is. */
    BASIC(0, listOf(2f, 4f, 6f, 8f, 10f)),

    /** The classic 1 to 10, whole numbers. */
    STANDARD(1, (1..10).map { it.toFloat() }),

    /** 6 to 10 in half steps, the way coaches write it. */
    ADVANCED(2, (12..20).map { it / 2f });

    fun contains(value: Float): Boolean = value in values

    companion object {
        /** The scale stored under [id]; an unknown one is [BASIC]. */
        fun fromId(id: Int?): RpeScale = entries.firstOrNull { it.id == id } ?: BASIC
    }
}

/** A level of the [RpeScale.BASIC] scale: its stored [value] and the RPE numbers it covers ([range]). */
enum class RpeLevel(val value: Float, val range: String) {
    WARM_UP(2f, "1–2"),
    EASY(4f, "3–4"),
    MODERATE(6f, "5–6"),
    HARD(8f, "7–8"),
    MAXIMUM(10f, "9–10");

    companion object {
        /** The level that holds [value], whichever scale it was logged on (6.5 is Hard). */
        fun of(value: Float): RpeLevel = when {
            value <= 2f -> WARM_UP
            value <= 4f -> EASY
            value <= 6f -> MODERATE
            value <= 8f -> HARD
            else -> MAXIMUM
        }
    }
}

/** Whether the app asks for RPE, and on which scale. */
data class RpeSettings(
    val enabled: Boolean = false,
    val scale: RpeScale = RpeScale.BASIC
)

object RpePreference {
    /**
     * The settings that match how RPE worked before it had a switch of its own: it was asked with advanced programming
     * on (unless the old input mode said never), on the 6 to 10 scale.
     */
    fun fromLegacy(advancedProgramming: Boolean, legacyInputMode: Int?): RpeSettings = RpeSettings(
        enabled = advancedProgramming && (legacyInputMode ?: 0) != 2,
        scale = if (advancedProgramming) RpeScale.ADVANCED else RpeScale.BASIC
    )
}
