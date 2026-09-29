package com.emanuel5014.trainable.data.local.entity

import com.emanuel5014.trainable.domain.prescription.IntensityType
import com.emanuel5014.trainable.domain.prescription.PlannedSet
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.RepMode
import com.emanuel5014.trainable.domain.prescription.TechniqueCodec

/** True when the row carries an advanced-prescription snapshot. */
val SetLogEntity.hasPrescription: Boolean get() = blockIndex != null

/** Copies the prescription snapshot of [planned] onto this row (null clears it). */
fun SetLogEntity.withPrescription(planned: PlannedSet?): SetLogEntity = copy(
    targetPercent = planned?.percent,
    targetRpe = planned?.targetRpe,
    targetReps = planned?.let { it.targetReps?.toString() ?: if (it.repMode == RepMode.AMRAP) "MAX" else null },
    repMode = planned?.repMode?.code,
    blockIndex = planned?.blockIndex,
    techniques = planned?.let { TechniqueCodec.encode(it.techniques) },
    targetTotalReps = planned?.totalReps
)

fun SetLogEntity.withoutPrescription(): SetLogEntity = withPrescription(null).copy(isExtra = false)

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
