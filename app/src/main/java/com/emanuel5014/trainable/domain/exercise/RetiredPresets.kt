package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity

/**
 * Presets the library keeps as rows but no longer offers, because other presets took their place (issue #50): two of them
 * had a twin that only differed by the bench angle, two mixed a pair of exercises under one name. Their ids and names stay,
 * so a routine file or a history written by an older version still finds them: an import brings a hidden one back.
 */
object RetiredPresets {

    /** Raise it when presets are added to [replacedBy]: the clean-up of existing installs runs once more for the new list. */
    const val VERSION = 1

    /** Each retired preset with the presets that replace it. */
    val replacedBy: Map<Int, List<Int>> = mapOf(
        // 45-Degree Incline Dumbbell Press -> Incline Dumbbell Bench Press
        2 to listOf(118),
        // 45-Degree Incline Dumbbell Curl -> Incline Bench Dumbbell Curl
        36 to listOf(37),
        // Seated/Standing Lateral Raises -> Seated Lateral Raise, Standing Lateral Raise
        32 to listOf(158, 159),
        // Rear Delt Fly (Machine/Dumbbell) -> Rear Delt Fly (Machine), Rear Delt Fly (Dumbbell)
        73 to listOf(160, 161)
    )

    val ids: Set<Int> = replacedBy.keys

    fun isRetired(id: Int): Boolean = id in ids

    /** The row to insert for [preset]: a retired one comes in hidden, so a new install never lists it. */
    fun forInsert(preset: ExerciseEntity): ExerciseEntity = if (isRetired(preset.id)) preset.copy(isDeleted = true) else preset

    /**
     * The retired presets to hide on an install that already has them: the ones of [existing] that are still exactly as the
     * catalog [presets] ships them (not renamed, no media, not a favorite, ...) and that nothing refers to ([usedIds]: the
     * retired ids a workout, a routine or a max uses). What was used or changed stays as it is, and the user merges it
     * into its replacement when they want to.
     */
    fun toHide(existing: List<ExerciseEntity>, presets: List<ExerciseEntity>, usedIds: Set<Int>): List<Int> {
        val shipped = presets.associateBy { it.id }
        return existing.filter { it.id in ids && it.id !in usedIds && it == shipped[it.id] }.map { it.id }
    }
}
