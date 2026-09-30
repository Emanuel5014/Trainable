package com.emanuel5014.trainable.data.ai

import android.content.Context
import android.net.Uri
import com.emanuel5014.trainable.data.remote.dto.PrescriptionBlockExportDto
import com.emanuel5014.trainable.domain.prescription.IntensityType
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.RepMode
import com.emanuel5014.trainable.domain.prescription.TechniqueCodec
import com.emanuel5014.trainable.util.ImageStorageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** A routine scan that was read (and maybe edited) but not imported yet. */
data class ScanDraft(
    val entries: List<ScannedExerciseEntry>,
    /** The photo the entries were read from, kept so the inspector still works when the draft is reopened. */
    val imageUri: Uri?,
    val updatedAt: Long
)

/**
 * Keeps the result of a full-routine AI scan on disk, one draft per routine, so closing the review
 * (by mistake, or the app being killed) doesn't throw away a scan that took minutes to run.
 */
@Singleton
class ScanDraftStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val directory: File get() = File(context.filesDir, "scan_drafts").apply { mkdirs() }
    private fun jsonFile(planId: Int) = File(directory, "plan_$planId.json")
    private fun imageFile(planId: Int) = File(directory, "plan_$planId.jpg")

    /** Stores the photo (upright, compressed) and the entries; returns the Uri the review should show. */
    fun create(planId: Int, entries: List<ScannedExerciseEntry>, sourceImage: Uri?): Uri? {
        val target = imageFile(planId)
        target.delete()
        if (sourceImage != null) {
            ImageStorageUtils.readAndCompressImage(context, sourceImage)?.let { target.writeBytes(it) }
        }
        save(planId, entries)
        return target.takeIf { it.exists() }?.let { Uri.fromFile(it) }
    }

    /** Overwrites the entries of the existing draft (after the user edited them). */
    fun save(planId: Int, entries: List<ScannedExerciseEntry>) {
        val dto = DraftDto(updatedAt = System.currentTimeMillis(), entries = entries.map { it.toDto() })
        runCatching { jsonFile(planId).writeText(json.encodeToString(dto)) }
    }

    fun load(planId: Int): ScanDraft? {
        val file = jsonFile(planId)
        if (!file.exists()) return null
        return runCatching {
            val dto = json.decodeFromString<DraftDto>(file.readText())
            if (dto.entries.isEmpty()) return null
            ScanDraft(
                entries = dto.entries.map { it.toEntry() },
                imageUri = imageFile(planId).takeIf { it.exists() }?.let { Uri.fromFile(it) },
                updatedAt = dto.updatedAt
            )
        }.getOrNull()
    }

    fun clear(planId: Int) {
        jsonFile(planId).delete()
        imageFile(planId).delete()
    }

    // ---- serialisation --------------------------------------------------------------------------

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
        val isProgrammed: Boolean = false
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
        blocks = blocksByWeek.toSortedMap().flatMap { (week, blocks) ->
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
        isProgrammed = isProgrammed
    )

    private fun EntryDto.toEntry() = ScannedExerciseEntry(
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
        isProgrammed = isProgrammed
    )
}
