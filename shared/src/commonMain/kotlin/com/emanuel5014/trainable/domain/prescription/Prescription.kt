package com.emanuel5014.trainable.domain.prescription

/**
 * Powerlifting-style set prescriptions: a plan exercise can be described by an ordered list of
 * [PrescriptionBlock]s (e.g. `70% 5 · 75% 3×5 STOP 2" · 70% 6`), optionally varying week by week.
 * Exercises without blocks keep using the legacy `serieTarget` / `repsTarget` fields.
 */
enum class IntensityType(val code: String) {
    NONE("none"),
    PERCENT("percent"),
    RPE("rpe"),
    WEIGHT("weight"),
    /** Bodyweight movement ("BW"); weight is the added load, usually 0. */
    BODYWEIGHT("bodyweight");

    companion object {
        fun fromCode(code: String?): IntensityType = entries.firstOrNull { it.code == code } ?: NONE
    }
}

enum class RepMode(val code: String) {
    /** Fixed reps: `3x5` or a per-set list `3-4-2-1-5`. */
    FIXED("fixed"),
    /** As many reps as possible on every set: `3xMAX`. */
    AMRAP("amrap"),
    /** A total rep target reached in as few sets as possible: `20 REP ALSAP`. */
    TOTAL("total");

    companion object {
        fun fromCode(code: String?): RepMode = entries.firstOrNull { it.code == code } ?: FIXED
    }
}

sealed interface Technique {
    data class Pause(val seconds: Int) : Technique
    data object Chains : Technique
    data object Bands : Technique
    data object FeetUp : Technique
    data object Competition : Technique
    data class Tempo(val pattern: String) : Technique
    data object Deficit : Technique
    /** Every minute on the minute: rest is whatever is left of the minute. */
    data object Emom : Technique
    data class Custom(val text: String) : Technique
}

data class PrescriptionBlock(
    val sets: Int = 1,
    /** "5" or a per-set list "3-4-2-1-5". Empty for AMRAP / TOTAL. */
    val reps: String = "",
    val repMode: RepMode = RepMode.FIXED,
    val totalReps: Int? = null,
    val intensityType: IntensityType = IntensityType.NONE,
    /** Percent (0-100), RPE (1-10) or weight in kg depending on [intensityType]. */
    val intensityValue: Float? = null,
    val techniques: List<Technique> = emptyList(),
    val restSeconds: Int? = null,
    val note: String? = null
) {
    val repsList: List<Int>
        get() = reps.split("-").mapNotNull { it.trim().toIntOrNull() }

    /** Number of sets actually performed when the block is expanded (a rep list defines its own count). */
    val effectiveSets: Int
        get() = when (repMode) {
            RepMode.FIXED -> repsList.size.takeIf { it > 1 } ?: sets.coerceAtLeast(1)
            RepMode.AMRAP -> sets.coerceAtLeast(1)
            RepMode.TOTAL -> 1
        }
}

/** One concrete set produced by expanding a block. */
data class PlannedSet(
    val blockIndex: Int,
    val indexInBlock: Int,
    /** Planned reps, null for AMRAP. For TOTAL blocks this is the remaining reps. */
    val targetReps: Int?,
    val repMode: RepMode,
    val totalReps: Int? = null,
    val intensityType: IntensityType = IntensityType.NONE,
    val intensityValue: Float? = null,
    val techniques: List<Technique> = emptyList(),
    val restSeconds: Int? = null
) {
    val percent: Float? get() = intensityValue.takeIf { intensityType == IntensityType.PERCENT }
    val targetRpe: Float? get() = intensityValue.takeIf { intensityType == IntensityType.RPE }
    val fixedWeightKg: Float? get() = intensityValue.takeIf { intensityType == IntensityType.WEIGHT }
}
