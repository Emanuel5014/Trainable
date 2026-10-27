package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity

/**
 * Presets the library keeps as rows but no longer offers, because other presets took their place (issue #50): two of them
 * had a twin that only differed by the bench angle, two mixed a pair of exercises under one name; (issue #51) the seated
 * calf raise on the machine was the same exercise as the seated calf raise, which is a machine one too. Their ids and names
 * stay, so a routine file or a history written by an older version still finds them: an import brings a hidden one back.
 */
object RetiredPresets {

    /**
     * Raise it when presets are added to [replacedBy], and record the new version in [addedIn]: the clean-up of existing
     * installs runs once more, for the presets that joined the list since the version it last ran for.
     */
    const val VERSION = 2

    /** Each retired preset with the presets that replace it. */
    val replacedBy: Map<Int, List<Int>> = mapOf(
        // 45-Degree Incline Dumbbell Press -> Incline Dumbbell Bench Press
        2 to listOf(118),
        // 45-Degree Incline Dumbbell Curl -> Incline Bench Dumbbell Curl
        36 to listOf(37),
        // Seated/Standing Lateral Raises -> Seated Lateral Raise, Standing Lateral Raise
        32 to listOf(158, 159),
        // Rear Delt Fly (Machine/Dumbbell) -> Rear Delt Fly (Machine), Rear Delt Fly (Dumbbell)
        73 to listOf(160, 161),
        // Seated Calf Raise Machine -> Seated Calf Raise (both were the machine one)
        79 to listOf(58)
    )

    /** The [VERSION] of the list in which a retired preset joined it; the ones that are not here were there from version 1. */
    private val addedIn: Map<Int, Int> = mapOf(79 to 2)

    val ids: Set<Int> = replacedBy.keys

    fun isRetired(id: Int): Boolean = id in ids

    /** The row to insert for [preset]: a retired one comes in hidden, so a new install never lists it. */
    fun forInsert(preset: ExerciseEntity): ExerciseEntity = if (isRetired(preset.id)) preset.copy(isDeleted = true) else preset

    /** The retired presets that joined the list after [appliedVersion], the version the clean-up last ran for (0: never). */
    fun idsSince(appliedVersion: Int): Set<Int> = ids.filterTo(hashSetOf()) { (addedIn[it] ?: 1) > appliedVersion }

    /**
     * The retired presets to hide on an install that already has them: the ones of [existing] that joined the list after
     * [appliedVersion], that are still exactly as the catalog [presets] ships them (not renamed, no media, not a favorite,
     * ...) and that nothing refers to ([usedIds]: the retired ids a workout, a routine or a max uses). What was used or
     * changed stays as it is, and the user merges it into its replacement when they want to. A preset from an earlier
     * version of the list is not looked at again: the user may have brought it back on purpose.
     */
    fun toHide(
        existing: List<ExerciseEntity>,
        presets: List<ExerciseEntity>,
        usedIds: Set<Int>,
        appliedVersion: Int = 0
    ): List<Int> {
        val shipped = presets.associateBy { it.id }
        val candidates = idsSince(appliedVersion)
        return existing.filter { it.id in candidates && it.id !in usedIds && it == shipped[it.id] }.map { it.id }
    }
}
