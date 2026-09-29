package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.withPrescription
import com.emanuel5014.trainable.data.local.entity.withoutPrescription
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.prescription.PlannedSet
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.domain.prescription.RepMode

/** Changes needed to bring the rows of a session exercise in line with a new prescription. */
data class SetSyncResult(
    val toInsert: List<SetLogEntity>,
    val toUpdate: List<SetLogEntity>,
    val toDelete: List<SetLogEntity>
)

/**
 * Applies advanced-prescription [PrescriptionBlock]s to the already logged rows of one exercise
 * (history editing). What was actually lifted (weight, reps, RPE, notes) is never overwritten; only
 * the prescription snapshot changes, rows are added when the new prescription has more sets and
 * removed when it has fewer.
 */
object PrescriptionSetSync {

    /** How many logged rows a prescription change would delete, so the UI can warn first. */
    fun removedCount(existing: List<SetLogEntity>, blocks: List<PrescriptionBlock>): Int =
        apply(existing, blocks, sessionId = 0, exerciseId = 0, order = 0, supersetId = null, restSeconds = null,
            oneRepMaxKg = null, unit = "kg", increment = 2.5f).toDelete.size

    fun apply(
        existing: List<SetLogEntity>,
        blocks: List<PrescriptionBlock>,
        sessionId: Int,
        exerciseId: Int,
        order: Int,
        supersetId: String?,
        restSeconds: Int?,
        oneRepMaxKg: Float?,
        unit: String,
        increment: Float
    ): SetSyncResult {
        val sorted = existing.sortedBy { it.numeroSerie }

        // Turning the prescription off: clear every snapshot, keep all rows.
        if (blocks.isEmpty()) {
            val cleared = sorted.map { it.withoutPrescription() }
            return SetSyncResult(
                toInsert = emptyList(),
                toUpdate = cleared.filterIndexed { i, row -> row != sorted[i] },
                toDelete = emptyList()
            )
        }

        val planned = PrescriptionExpander.expand(blocks)
        val extras = sorted.filter { it.isExtra }
        val regular = sorted.filterNot { it.isExtra }
        val hasSnapshot = regular.any { it.blockIndex != null }

        val ordered = mutableListOf<SetLogEntity>()
        val deleted = mutableListOf<SetLogEntity>()
        val keptWithoutBlock = mutableListOf<SetLogEntity>()

        fun target(p: PlannedSet): Float? = LoadCalculator.targetWeightKg(p, oneRepMaxKg, unit, increment)

        fun updated(row: SetLogEntity, p: PlannedSet): SetLogEntity {
            val weight = if (row.pesoSollevato <= 0f) target(p) ?: row.pesoSollevato else row.pesoSollevato
            return row.withPrescription(p).copy(pesoSollevato = weight)
        }

        fun created(p: PlannedSet, previous: SetLogEntity?): SetLogEntity {
            val reps = when (p.repMode) {
                RepMode.AMRAP -> previous?.repsEffettive ?: 5
                else -> p.targetReps ?: previous?.repsEffettive ?: 5
            }
            return SetLogEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                pesoSollevato = target(p) ?: previous?.pesoSollevato ?: 0f,
                repsEffettive = reps,
                numeroSerie = 0,
                ordineEsercizio = order,
                supersetId = supersetId,
                restTimerSeconds = restSeconds
            ).withPrescription(p)
        }

        if (!hasSnapshot) {
            // Plain → advanced: pair rows and planned sets by position
            planned.forEachIndexed { i, p ->
                val row = regular.getOrNull(i)
                ordered += if (row != null) updated(row, p) else created(p, ordered.lastOrNull())
            }
            deleted += regular.drop(planned.size)
        } else {
            val plannedByBlock = planned.groupBy { it.blockIndex }
            val validBlocks = plannedByBlock.keys
            regular.filter { it.blockIndex == null }.let { keptWithoutBlock += it }
            deleted += regular.filter { it.blockIndex != null && it.blockIndex !in validBlocks }

            plannedByBlock.toSortedMap().forEach { (blockIndex, plannedInBlock) ->
                val rows = regular.filter { it.blockIndex == blockIndex }
                if (plannedInBlock.first().repMode == RepMode.TOTAL) {
                    // ALSAP blocks own however many follow-up rows the athlete needed
                    val template = plannedInBlock.first()
                    if (rows.isEmpty()) ordered += created(template, ordered.lastOrNull())
                    rows.forEachIndexed { i, row ->
                        ordered += updated(row, template.copy(indexInBlock = i, targetReps = row.targetReps?.toIntOrNull() ?: template.targetReps))
                    }
                } else {
                    plannedInBlock.forEachIndexed { i, p ->
                        val row = rows.getOrNull(i)
                        ordered += if (row != null) updated(row, p) else created(p, ordered.lastOrNull())
                    }
                    deleted += rows.drop(plannedInBlock.size)
                }
            }
        }

        val finalRows = ordered + keptWithoutBlock + extras
        val numbered = finalRows.mapIndexed { i, row -> row.copy(numeroSerie = i + 1) }
        val originalsById = sorted.associateBy { it.id }

        return SetSyncResult(
            toInsert = numbered.filter { it.id == 0 },
            toUpdate = numbered.filter { it.id != 0 && originalsById[it.id] != it },
            toDelete = deleted
        )
    }
}
