package com.emanuel5014.trainable.data.ai

import com.emanuel5014.trainable.data.remote.dto.PrescriptionBlockExportDto
import com.emanuel5014.trainable.domain.prescription.IntensityType
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.RepMode
import com.emanuel5014.trainable.domain.prescription.RepsMode
import com.emanuel5014.trainable.domain.prescription.TechniqueCodec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A routine scan that was read (and maybe edited) but not imported yet. */
data class ScanDraft(
    val entries: List<ScannedExerciseEntry>,
    val imageUri: String?,
    val updatedAt: Long
)

/** A draft per routine, kept when the review or the app closes. Calls run on a disk dispatcher. */
interface RoutineScanDraftStore {
    fun create(planId: Int, entries: List<ScannedExerciseEntry>, sourceImage: String): String?
    fun save(planId: Int, entries: List<ScannedExerciseEntry>)
    fun load(planId: Int): ScanDraft?
    fun clear(planId: Int)
}

/** The existing Android draft format, also used by iOS. Photo paths are resolved on each platform. */
object ScanDraftCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(entries: List<ScannedExerciseEntry>, updatedAt: Long): String =
        json.encodeToString(DraftDto(updatedAt, entries.map { it.toDto() }))

    fun decode(content: String, imageUri: String?): ScanDraft? = runCatching {
        val dto = json.decodeFromString<DraftDto>(content)
        if (dto.entries.isEmpty()) null else ScanDraft(dto.entries.map { it.toEntry() }, imageUri, dto.updatedAt)
    }.getOrNull()

    @Serializable
    private data class DraftDto(val updatedAt: Long = 0L, val entries: List<EntryDto> = emptyList())

    @Serializable
    private data class EntryDto(
        val rawName: String,
        val exerciseId: Int? = null,
        val matchedName: String? = null,
        val suggestedCategory: String = "",
        val sets: Int = 3,
        val reps: String = "",
        val restSeconds: Int = 120,
        val cardioMinutes: Int? = null,
        val exerciseType: String = "strength",
        val timeSeconds: Int? = null,
        val blocks: List<PrescriptionBlockExportDto> = emptyList(),
        val oneRepMaxKg: Float? = null,
        val isProgrammed: Boolean = false,
        /** [RepsMode] name; missing in drafts saved before rep modes, which are then read again once. */
        val repsMode: String? = null,
        val repsMax: Int? = null,
        val readReps: String? = null,
        val needsReview: Boolean = false
    )

    private fun ScannedExerciseEntry.toDto() = EntryDto(
        rawName = rawName,
        exerciseId = exerciseId,
        matchedName = matchedName,
        suggestedCategory = suggestedCategory,
        sets = sets,
        reps = reps,
        restSeconds = restSeconds,
        cardioMinutes = cardioMinutes,
        exerciseType = exerciseType,
        timeSeconds = timeSeconds,
        blocks = blocksByWeek.entries.sortedBy { it.key }.flatMap { (week, blocks) ->
            blocks.mapIndexed { index, block ->
                PrescriptionBlockExportDto(
                    week = week,
                    ordine = index,
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
        },
        oneRepMaxKg = oneRepMaxKg,
        isProgrammed = isProgrammed,
        repsMode = repsMode?.name,
        repsMax = repsMax,
        readReps = readReps,
        needsReview = needsReview
    )

    private fun EntryDto.toEntry(): ScannedExerciseEntry {
        val entry = baseEntry()
        val mode = RepsMode.entries.firstOrNull { it.name == repsMode }
        // A row without a known mode comes from an older draft: its reps get the scan's rules once
        return if (mode == null && entry.isPlainStrength) entry.strengthFrom(entry.reps) else entry.copy(repsMode = mode)
    }

    private fun EntryDto.baseEntry() = ScannedExerciseEntry(
        rawName = rawName,
        exerciseId = exerciseId,
        matchedName = matchedName,
        suggestedCategory = suggestedCategory,
        sets = sets,
        reps = reps,
        restSeconds = restSeconds,
        cardioMinutes = cardioMinutes,
        exerciseType = exerciseType,
        timeSeconds = timeSeconds,
        blocksByWeek = blocks.groupBy { it.week }.mapValues { (_, list) ->
            list.sortedBy { it.ordine }.map { b ->
                PrescriptionBlock(
                    sets = b.sets,
                    reps = b.reps,
                    repMode = RepMode.fromCode(b.repMode),
                    totalReps = b.totalReps,
                    intensityType = IntensityType.fromCode(b.intensityType),
                    intensityValue = b.intensityValue,
                    techniques = TechniqueCodec.decode(b.techniques),
                    restSeconds = b.restSeconds,
                    note = b.note
                )
            }
        },
        oneRepMaxKg = oneRepMaxKg,
        isProgrammed = isProgrammed,
        repsMax = repsMax,
        readReps = readReps,
        needsReview = needsReview
    )
}
