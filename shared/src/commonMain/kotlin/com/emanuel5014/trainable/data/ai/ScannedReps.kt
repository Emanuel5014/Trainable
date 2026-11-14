package com.emanuel5014.trainable.data.ai

import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.RepRange
import com.emanuel5014.trainable.domain.prescription.RepsDraft
import com.emanuel5014.trainable.domain.prescription.RepsMode
import com.emanuel5014.trainable.domain.prescription.TimeAndReps

/** What the reps the AI read on a strength row turn into. */
sealed interface ScannedRepsResult {
    /** A number, a range or a per-set list; [reps] as stored for that mode, [repsMax] only for a range. */
    data class Strength(val mode: RepsMode, val reps: String, val repsMax: Int?) : ScannedRepsResult

    /** Nothing usable: the row is shown "da controllare" with [read] (null when the cell was blank). */
    data class Flagged(val read: String?) : ScannedRepsResult

    /** "MAX" with advanced programming on: [sets] sets of max reps, as an advanced block. */
    data class Amrap(val sets: Int) : ScannedRepsResult
}

object ScannedReps {
    private val maxWords = setOf("max", "amrap", "cedimento", "failure")
    private val numbers = Regex("""^\d{1,3}(-\d{1,3})*$""")

    /**
     * One number = fixed, two ascending = a range, two descending (or equal) or more = one number per set.
     * Anything else, a blank cell included, is flagged instead of guessed.
     */
    fun classify(raw: String, advanced: Boolean, sets: Int): ScannedRepsResult {
        val text = raw.trim()
        if (text.lowercase() in maxWords) return if (advanced) ScannedRepsResult.Amrap(sets) else ScannedRepsResult.Flagged(text)
        if (!numbers.matches(text)) return ScannedRepsResult.Flagged(text.ifBlank { null })
        val values = text.split("-").map { it.toInt() }
        if (values.any { it !in 1..RepRange.MAX_REPS }) return ScannedRepsResult.Flagged(text)
        return when {
            values.size == 1 -> ScannedRepsResult.Strength(RepsMode.FIXED, text, null)
            values.size == 2 && values[1] > values[0] -> ScannedRepsResult.Strength(RepsMode.RANGE, values[0].toString(), values[1])
            else -> ScannedRepsResult.Strength(RepsMode.PER_SET, text, null)
        }
    }
}

/** A row that is edited with the Fisso / Intervallo / Per serie reps editor. */
val ScannedExerciseEntry.isPlainStrength: Boolean
    get() = !isCardio && !isTimeAndWeight && !isTimeAndReps && !isAdvanced

/** The reps editor's state for this row, from what is stored on it. */
fun ScannedExerciseEntry.repsDraft(): RepsDraft = when (repsMode) {
    RepsMode.FIXED -> RepsDraft(RepsMode.FIXED, fixed = reps)
    RepsMode.RANGE -> RepsDraft(RepsMode.RANGE, min = reps, max = repsMax?.toString().orEmpty())
    RepsMode.PER_SET -> RepsDraft(RepsMode.PER_SET, list = reps)
    null -> RepsDraft.from(reps, repsMax)
}

/** Stores what the editor holds for its current mode, valid or not, so nothing typed is lost. */
fun ScannedExerciseEntry.withRepsDraft(draft: RepsDraft): ScannedExerciseEntry = copy(
    repsMode = draft.mode,
    reps = when (draft.mode) {
        RepsMode.FIXED -> draft.fixed
        RepsMode.RANGE -> draft.min
        RepsMode.PER_SET -> draft.list
    },
    repsMax = if (draft.mode == RepsMode.RANGE) draft.max.toIntOrNull() else null,
    needsReview = needsReview && !draft.isValid
)

/** False only for a plain strength row whose reps cannot be saved yet. */
val ScannedExerciseEntry.hasValidReps: Boolean
    get() = !isPlainStrength || repsDraft().isValid

/** This row as plain strength with [text] read as reps (no advanced blocks), flagged when it cannot be read. */
fun ScannedExerciseEntry.strengthFrom(text: String): ScannedExerciseEntry =
    when (val result = ScannedReps.classify(text, advanced = false, sets = sets)) {
        is ScannedRepsResult.Strength -> copy(
            repsMode = result.mode, reps = result.reps, repsMax = result.repsMax, readReps = null, needsReview = false
        )
        is ScannedRepsResult.Flagged -> copy(
            repsMode = RepsMode.FIXED, reps = "", repsMax = null, readReps = result.read, needsReview = true
        )
        is ScannedRepsResult.Amrap -> copy(repsMode = RepsMode.FIXED, reps = "", repsMax = null, needsReview = true)
    }

/** The type id the type selector shows for this row: "strength", "time_and_weight", "time_and_reps" or "advanced". */
val ScannedExerciseEntry.typeId: String
    get() = when {
        isAdvanced -> "advanced"
        isTimeAndWeight -> "time_and_weight"
        isTimeAndReps -> TIME_AND_REPS
        else -> "strength"
    }

/**
 * This row as the [type] picked in the type selector, carrying over what it can; null when nothing changes (the
 * selected type tapped again must not rewrite the reps).
 */
fun ScannedExerciseEntry.withType(type: String): ScannedExerciseEntry? {
    if (type == typeId) return null
    val currentTarget = timeSeconds ?: reps.filter { it.isDigit() }.toIntOrNull() ?: 45
    return when (type) {
        // A time ("45s") becomes that many reps, as before; anything else is read like the AI's reps
        "strength" -> copy(exerciseType = "strength", blocksByWeek = emptyMap())
            .strengthFrom(if (reps.endsWith("s")) "$currentTarget" else reps)
        TIME_AND_REPS -> copy(
            exerciseType = TIME_AND_REPS,
            // A hold's "45s" is its time; plain reps give their first number as the goal
            timeSeconds = timeSeconds ?: TimeAndReps.DEFAULT_SECONDS,
            reps = TimeAndReps.goalOf(reps.takeUnless { it.endsWith("s") }?.substringBefore("-")?.ifBlank { null }).toString(),
            blocksByWeek = emptyMap(),
            readReps = null,
            needsReview = false
        )
        "time_and_weight" -> copy(
            exerciseType = "time_and_weight",
            timeSeconds = currentTarget,
            reps = "${currentTarget}s",
            blocksByWeek = emptyMap(),
            readReps = null,
            needsReview = false
        )
        "advanced" -> {
            // Carry the plain sets × reps over as a first free block
            val blockReps = reps.takeIf { r -> r.split("-").all { it.trim().toIntOrNull() != null } } ?: "5"
            copy(
                exerciseType = "strength",
                timeSeconds = null,
                blocksByWeek = mapOf(1 to listOf(PrescriptionBlock(sets = sets, reps = blockReps))),
                readReps = null,
                needsReview = false
            )
        }
        else -> null
    }
}
