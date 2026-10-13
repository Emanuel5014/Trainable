package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.domain.prescription.BlockAlignment
import com.emanuel5014.trainable.domain.prescription.LoadShift
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.RepMode

/**
 * Brings the sets of an advanced exercise in line with a prescription changed during the workout.
 *
 * What was done stays exactly as it was: a set that is completed keeps its weight, reps, RPE, notes and the
 * prescription it was done against. Only the sets still to do follow the new blocks, and one the change did not touch
 * keeps what the lifter typed on it (weight, notes).
 */
object ProgramEditing {

    /** The sets after the change and the ids of the saved rows that are no longer part of the exercise. */
    data class Result(val sets: List<WorkoutSetState>, val removedIds: List<Int>)

    /**
     * [current] are the sets of the exercise as they are now, [fresh] the sets the new blocks make when expanded from
     * scratch (planned weights included), both with a prescription on every set that belongs to a block; [oldBlocks]
     * and [newBlocks] tell which block became which (see [BlockAlignment]). With [loadsChanged] (the 1RM the
     * percentages refer to moved) the sets still to do take the planned weight again, even where their prescription
     * is the same.
     */
    fun apply(
        current: List<WorkoutSetState>,
        fresh: List<WorkoutSetState>,
        oldBlocks: List<PrescriptionBlock>,
        newBlocks: List<PrescriptionBlock>,
        loadsChanged: Boolean = false
    ): Result {
        val moved = BlockAlignment.align(oldBlocks, newBlocks)
        /** The set with its prescription pointing at the block it now belongs to; null when its block is gone. */
        fun WorkoutSetState.inNewBlocks(): WorkoutSetState? {
            val p = prescription ?: return null
            val newIndex = moved[p.blockIndex] ?: return null
            return if (newIndex == p.blockIndex) this else copy(prescription = p.copy(blockIndex = newIndex))
        }

        val inBlocks = current.filter { !it.isExtra && it.prescription != null }
        val done = inBlocks.filter { it.isCompleted }
        val pending = inBlocks.filter { !it.isCompleted }
        val freshByBlock = fresh.filter { it.prescription != null }.groupBy { it.prescription!!.blockIndex }.toSortedMap()

        val kept = mutableListOf<WorkoutSetState>()
        // Done sets that no longer fit: their block was taken out or got fewer sets than were already done
        val orphans = mutableListOf<WorkoutSetState>()

        freshByBlock.forEach { (blockIndex, planned) ->
            val template = planned.first().prescription!!
            val doneHere = done.mapNotNull { it.inNewBlocks() }.filter { it.prescription!!.blockIndex == blockIndex }
            val pendingHere = pending.mapNotNull { it.inNewBlocks() }.filter { it.prescription!!.blockIndex == blockIndex }
            if (template.repMode == RepMode.TOTAL) {
                kept += doneHere
                val remaining = (template.totalReps ?: 0) - doneHere.sumOf { it.reps }
                if (remaining > 0) {
                    val next = planned.first().let { f ->
                        f.copy(reps = remaining, prescription = f.prescription!!.copy(indexInBlock = doneHere.size, targetReps = remaining))
                    }
                    kept += carryOver(pendingHere.firstOrNull(), next, loadsChanged)
                }
            } else {
                val fits = doneHere.take(planned.size)
                orphans += doneHere.drop(planned.size)
                kept += fits
                planned.drop(fits.size).forEachIndexed { i, f -> kept += carryOver(pendingHere.getOrNull(i), f, loadsChanged) }
            }
        }
        // Done sets of a block that is gone: found by their original place, not by where blocks moved to
        orphans += done.filter { it.inNewBlocks() == null }

        val keptIds = kept.mapNotNull { it.id }.toSet()
        // Extras keep the order they had; the sets that lost their block follow them as extras too, in the order they
        // were done, still showing the load they were done at
        val tail = current.filter { it.isExtra || it.prescription == null } +
            orphans.map { it.copy(isExtra = true, prescription = it.prescription?.copy(blockIndex = BlockAlignment.NO_BLOCK)) }
        val removed = pending.mapNotNull { it.id }.filter { it !in keptIds }

        val sets = (kept + tail).mapIndexed { index, set -> set.copy(setNumber = index + 1) }
        return Result(sets, removed)
    }

    /**
     * A set still to do for the new plan [planned]: when there was one in the same place it keeps its row, its note
     * and, if its intensity did not change, the weight the lifter chose; one the change left alone is kept whole.
     */
    private fun carryOver(old: WorkoutSetState?, planned: WorkoutSetState, loadsChanged: Boolean): WorkoutSetState {
        if (old == null) return planned
        if (old.prescription == planned.prescription) {
            return if (loadsChanged && planned.prescription?.percent != null) old.copy(weight = planned.weight) else old
        }
        val before = old.prescription!!
        val after = planned.prescription!!
        val sameIntensity = before.intensityType == after.intensityType && before.intensityValue == after.intensityValue
        return planned.copy(
            id = old.id,
            note = old.note,
            weight = if (sameIntensity && !loadsChanged) old.weight else planned.weight,
            // A rep goal that did not change keeps the reps the lifter set
            reps = if (before.targetReps == after.targetReps && before.repMode == after.repMode) old.reps else planned.reps,
            rpe = old.rpe
        )
    }

    /** Blocks that still have a set to do: the ones a change of the load can still reach. */
    fun blocksWithPendingSets(sets: List<WorkoutSetState>): Set<Int> =
        sets.filter { !it.isCompleted && !it.isExtra }.mapNotNull { it.prescription?.blockIndex }.toSet()

    /** [blocksWithPendingSets] that carry a %1RM, in block order. */
    fun percentBlocksToDo(sets: List<WorkoutSetState>, blocks: List<PrescriptionBlock>): List<Int> {
        val percentBlocks = LoadShift.percentBlocks(blocks)
        return blocksWithPendingSets(sets).filter { it in blocks.indices && it in percentBlocks }.sorted()
    }
}
