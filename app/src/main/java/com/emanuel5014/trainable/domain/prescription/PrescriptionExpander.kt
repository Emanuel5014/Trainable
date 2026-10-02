package com.emanuel5014.trainable.domain.prescription

object PrescriptionExpander {

    fun expand(blocks: List<PrescriptionBlock>): List<PlannedSet> = blocks.flatMapIndexed { blockIndex, block ->
        when (block.repMode) {
            RepMode.FIXED -> {
                val list = block.repsList
                if (list.size > 1) {
                    list.mapIndexed { i, reps -> block.toPlannedSet(blockIndex, i, reps) }
                } else {
                    List(block.sets.coerceAtLeast(1)) { i -> block.toPlannedSet(blockIndex, i, list.firstOrNull()) }
                }
            }
            RepMode.AMRAP -> List(block.sets.coerceAtLeast(1)) { i -> block.toPlannedSet(blockIndex, i, null) }
            RepMode.TOTAL -> listOf(block.toPlannedSet(blockIndex, 0, block.totalReps))
        }
    }

    /** Next set of a TOTAL block, or null when the target has been reached. */
    fun nextTotalRepsSet(template: PlannedSet, repsDone: Int): PlannedSet? {
        val total = template.totalReps ?: return null
        val remaining = total - repsDone
        if (remaining <= 0) return null
        return template.copy(indexInBlock = template.indexInBlock + 1, targetReps = remaining)
    }

    /** Legacy `serieTarget` / `repsTarget` derived from blocks, kept in sync for older consumers. */
    fun legacyTargets(blocks: List<PrescriptionBlock>): Pair<Int, String> {
        val sets = expand(blocks)
        if (sets.isEmpty()) return 1 to "1"
        val reps = sets.map { it.targetReps?.toString() ?: "MAX" }
        val repsTarget = if (reps.distinct().size == 1) reps[0] else reps.joinToString("-")
        return sets.size to repsTarget
    }

    private fun PrescriptionBlock.toPlannedSet(blockIndex: Int, indexInBlock: Int, reps: Int?) = PlannedSet(
        blockIndex = blockIndex,
        indexInBlock = indexInBlock,
        targetReps = reps,
        repMode = repMode,
        totalReps = totalReps,
        intensityType = intensityType,
        intensityValue = intensityValue,
        techniques = techniques,
        restSeconds = restSeconds
    )
}

/** Legacy reps parser ("10-8-6" → per set list), shared by workout, history and manual sessions. */
object LegacyReps {
    fun parse(repsTarget: String, targetSets: Int): List<Int> {
        val parts = repsTarget.split("-").mapNotNull { it.trim().toIntOrNull() }
        return when {
            parts.isEmpty() -> List(targetSets) { 8 }
            parts.size == 1 -> List(targetSets) { parts[0] }
            parts.size >= targetSets -> parts.take(targetSets)
            else -> parts + List(targetSets - parts.size) { parts.last() }
        }
    }
}
