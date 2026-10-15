package com.emanuel5014.trainable.data.local.entity

import com.emanuel5014.trainable.domain.prescription.IntensityType
import com.emanuel5014.trainable.domain.prescription.PlannedSet
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.RepMode
import com.emanuel5014.trainable.domain.prescription.RepRange
import com.emanuel5014.trainable.domain.prescription.TechniqueCodec

/** True when the row carries an advanced-prescription snapshot. */
val SetLogEntity.hasPrescription: Boolean get() = blockIndex != null

/** Copies the prescription snapshot of [planned] onto this row (null clears it). */
fun SetLogEntity.withPrescription(planned: PlannedSet?): SetLogEntity = copy(
    targetPercent = planned?.percent,
    targetRpe = planned?.targetRpe,
    targetReps = planned?.targetRepsText(),
    repMode = planned?.repMode?.code,
    blockIndex = planned?.blockIndex,
    techniques = planned?.let { TechniqueCodec.encode(it.techniques) },
    targetTotalReps = planned?.totalReps
)

/** The reps of a planned set as stored on its row: the number, "MAX" for an AMRAP set. */
private fun PlannedSet.targetRepsText(): String? = targetReps?.toString() ?: if (repMode == RepMode.AMRAP) "MAX" else null

/** Drops the prescription snapshot, and with it the technique the set was done with instead of the planned one. */
fun SetLogEntity.withoutPrescription(): SetLogEntity = withPrescription(null).copy(isExtra = false, performedTechniques = null)

/**
 * The rep range a plain set was done against ("10–15"), kept in `target_reps` which plain sets never used. It lets a
 * workout that is left and re-entered, a swapped or added exercise and the history all know the range without a plan.
 */
val SetLogEntity.repsRange: RepRange? get() = if (blockIndex == null) RepRange.parse(targetReps) else null

/** Stores [range] as the rep range snapshot of a plain set (null clears it); advanced sets keep their own snapshot. */
fun SetLogEntity.withRepsRange(range: RepRange?): SetLogEntity =
    if (blockIndex != null) this else copy(targetReps = range?.text)

/**
 * Rebuilds the blocks a session exercise was performed against from the snapshots on its set
 * rows (quick workouts, swaps and history have no plan exercise to read them from).
 * Fixed weights / bodyweight aren't part of the snapshot, so those blocks come back without intensity.
 */
fun List<SetLogEntity>.toPrescriptionBlocks(): List<PrescriptionBlock> =
    filter { it.blockIndex != null && !it.isExtra }
        .groupBy { it.blockIndex!! }
        .toSortedMap()
        .values
        .map { group ->
            val rows = group.sortedBy { it.numeroSerie }
            val first = rows.first()
            val mode = RepMode.fromCode(first.repMode)
            val plannedReps = rows.mapNotNull { it.targetReps?.toIntOrNull() }
            val (type, value) = when {
                first.targetPercent != null -> IntensityType.PERCENT to first.targetPercent
                first.targetRpe != null -> IntensityType.RPE to first.targetRpe
                else -> IntensityType.NONE to null
            }
            when (mode) {
                RepMode.TOTAL -> PrescriptionBlock(
                    sets = 1, repMode = mode, totalReps = first.targetTotalReps ?: rows.sumOf { it.repsEffettive },
                    intensityType = type, intensityValue = value,
                    techniques = TechniqueCodec.decode(first.techniques)
                )
                RepMode.AMRAP -> PrescriptionBlock(
                    sets = rows.size, repMode = mode,
                    intensityType = type, intensityValue = value,
                    techniques = TechniqueCodec.decode(first.techniques)
                )
                RepMode.FIXED -> PrescriptionBlock(
                    sets = rows.size,
                    reps = when {
                        plannedReps.isEmpty() -> ""
                        plannedReps.distinct().size == 1 -> plannedReps.first().toString()
                        else -> plannedReps.joinToString("-")
                    },
                    intensityType = type, intensityValue = value,
                    techniques = TechniqueCodec.decode(first.techniques)
                )
            }
        }

/**
 * True when this block of the previous session (its rows, in order) was prescribed exactly like [planned] (the sets the
 * block makes now): same kind of reps, reps, intensity, techniques and number of sets. It tells whether what was
 * written on a set last time still belongs to the set of today, when a periodized plan changes its blocks every week.
 * Fixed weights aren't part of the snapshot, so for those the weight lifted stands in for the planned one.
 */
fun List<SetLogEntity>.wasPrescribedLike(planned: List<PlannedSet>): Boolean {
    val template = planned.firstOrNull() ?: return false
    if (isEmpty() || any { it.repMode != template.repMode.code }) return false
    // A TOTAL block adds follow-up sets until the total is reached, so its sets are told by the total alone
    val setsMatch = if (template.repMode == RepMode.TOTAL) {
        first().targetTotalReps == template.totalReps
    } else {
        size == planned.size && zip(planned).all { (row, set) -> row.targetReps == set.targetRepsText() }
    }
    return setsMatch && zip(planned).all { (row, set) -> row.hasIntensityAndTechniquesOf(set) }
}

private fun SetLogEntity.hasIntensityAndTechniquesOf(set: PlannedSet): Boolean {
    val fixedWeight = when (set.intensityType) {
        IntensityType.WEIGHT -> set.intensityValue
        IntensityType.BODYWEIGHT -> set.intensityValue ?: 0f
        else -> null
    }
    return targetPercent == set.percent &&
        targetRpe == set.targetRpe &&
        techniques == TechniqueCodec.encode(set.techniques) &&
        (fixedWeight == null || pesoSollevato == fixedWeight)
}
