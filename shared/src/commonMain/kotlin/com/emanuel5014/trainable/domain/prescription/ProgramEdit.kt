package com.emanuel5014.trainable.domain.prescription

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * How the prescription of an exercise was changed during a workout: [PROGRAM] when its blocks were edited, [LOAD]
 * when only the percentages of the load were moved.
 */
enum class ProgramEditKind(val code: String) {
    PROGRAM("program"),
    LOAD("load");

    companion object {
        fun fromCode(code: String?): ProgramEditKind = entries.firstOrNull { it.code == code } ?: PROGRAM
    }
}

/** Whether a change stays a variation of one workout or also replaces the week of the routine. */
enum class ProgramEditScope(val code: String) {
    /** The routine is left as it is: the change is only a variation of this workout. */
    WORKOUT("workout"),
    /** The week of the routine the workout follows now has the new prescription. */
    PLAN("plan");

    companion object {
        fun fromCode(code: String?): ProgramEditScope = entries.firstOrNull { it.code == code } ?: WORKOUT
    }
}

/** Blocks as text, to keep the prescription an exercise had before and after a change next to the workout. */
object BlocksCodec {
    @Serializable
    private data class BlockDto(
        val sets: Int = 1,
        val reps: String = "",
        val repMode: String = RepMode.FIXED.code,
        val totalReps: Int? = null,
        val intensityType: String = IntensityType.NONE.code,
        val intensityValue: Float? = null,
        val techniques: String? = null,
        val restSeconds: Int? = null,
        val note: String? = null
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(blocks: List<PrescriptionBlock>): String = json.encodeToString(
        kotlinx.serialization.builtins.ListSerializer(BlockDto.serializer()),
        blocks.map { block ->
            BlockDto(
                sets = block.sets,
                reps = block.reps,
                repMode = block.repMode.code,
                totalReps = block.totalReps,
                intensityType = block.intensityType.code,
                intensityValue = block.intensityValue,
                techniques = TechniqueCodec.encode(block.techniques),
                restSeconds = block.restSeconds,
                note = block.note
            )
        }
    )

    /** The blocks written by [encode]; a text that cannot be read gives no blocks. */
    fun decode(raw: String?): List<PrescriptionBlock> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(BlockDto.serializer()), raw).map { dto ->
                PrescriptionBlock(
                    sets = dto.sets,
                    reps = dto.reps,
                    repMode = RepMode.fromCode(dto.repMode),
                    totalReps = dto.totalReps,
                    intensityType = IntensityType.fromCode(dto.intensityType),
                    intensityValue = dto.intensityValue,
                    techniques = TechniqueCodec.decode(dto.techniques),
                    restSeconds = dto.restSeconds,
                    note = dto.note
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/** Moving the percentages of the blocks of an exercise, the way the quick load editor of a workout does. */
object LoadShift {
    const val STEP_PERCENT = 2.5f
    const val MIN_PERCENT = 1f
    const val MAX_PERCENT = 150f

    /** Blocks that carry a %1RM, the only ones whose load can be moved this way. */
    fun percentBlocks(blocks: List<PrescriptionBlock>): List<Int> =
        blocks.indices.filter { blocks[it].intensityType == IntensityType.PERCENT && blocks[it].intensityValue != null }

    /** [blocks] with the percentage of those at [blockIndices] moved by [delta] points (kept between 1% and 150%). */
    fun shift(blocks: List<PrescriptionBlock>, blockIndices: Set<Int>, delta: Float): List<PrescriptionBlock> =
        blocks.mapIndexed { index, block ->
            val percent = block.intensityValue
            if (index in blockIndices && block.intensityType == IntensityType.PERCENT && percent != null) {
                block.copy(intensityValue = clamp(percent + delta))
            } else block
        }

    fun clamp(percent: Float): Float {
        // Steps of a quarter keep a typed 77.3 as typed while 2.5 + 2.5 never drifts
        val rounded = kotlin.math.round(percent * 100f) / 100f
        return rounded.coerceIn(MIN_PERCENT, MAX_PERCENT)
    }
}

/**
 * Tells which block of a prescription became which after it was edited, so the sets already done stay with the block
 * they were done for even when a block was added or taken out before it.
 */
object BlockAlignment {

    /** What a set done for a block that no longer exists is moved to: no block of the prescription. */
    const val NO_BLOCK = -1

    /**
     * For each block of [old] the index of its block in [new]. Blocks that were not touched are matched with their
     * equal in order (the longest run of them), the ones changed between two such blocks pair up one by one in order,
     * and a block that is left without a counterpart (taken out, or one too many changed) is missing from the result.
     */
    fun align(old: List<PrescriptionBlock>, new: List<PrescriptionBlock>): Map<Int, Int> {
        val n = old.size
        val m = new.size
        val longest = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
            longest[i][j] = if (old[i] == new[j]) longest[i + 1][j + 1] + 1 else maxOf(longest[i + 1][j], longest[i][j + 1])
        }
        val anchors = mutableListOf<Pair<Int, Int>>()
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                old[i] == new[j] -> { anchors += i to j; i++; j++ }
                longest[i + 1][j] >= longest[i][j + 1] -> i++
                else -> j++
            }
        }
        val result = mutableMapOf<Int, Int>()
        var previousOld = -1
        var previousNew = -1
        (anchors + (n to m)).forEach { (anchorOld, anchorNew) ->
            val oldGap = (previousOld + 1 until anchorOld).toList()
            val newGap = (previousNew + 1 until anchorNew).toList()
            oldGap.zip(newGap).forEach { (o, nw) -> result[o] = nw }
            if (anchorOld < n) result[anchorOld] = anchorNew
            previousOld = anchorOld
            previousNew = anchorNew
        }
        return result
    }
}

/** One block that differs between two versions of a prescription. */
data class BlockChange(
    val kind: Kind,
    /** 1-based: the block's number in the new prescription, or in the old one for a block that was taken out. */
    val blockNumber: Int,
    val before: PrescriptionBlock?,
    val after: PrescriptionBlock?
) {
    enum class Kind { CHANGED, ADDED, REMOVED }
}

object ProgramDiff {
    /** What changed from [before] to [after], block by block, in block order. */
    fun changes(before: List<PrescriptionBlock>, after: List<PrescriptionBlock>): List<BlockChange> {
        val moved = BlockAlignment.align(before, after)
        val result = mutableListOf<BlockChange>()
        before.indices.filter { it !in moved }.forEach { i ->
            result += BlockChange(BlockChange.Kind.REMOVED, i + 1, before[i], null)
        }
        moved.forEach { (old, new) ->
            if (before[old] != after[new]) result += BlockChange(BlockChange.Kind.CHANGED, new + 1, before[old], after[new])
        }
        after.indices.filter { it !in moved.values }.forEach { i ->
            result += BlockChange(BlockChange.Kind.ADDED, i + 1, null, after[i])
        }
        return result.sortedWith(compareBy({ it.blockNumber }, { it.kind.ordinal }))
    }
}
