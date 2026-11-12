package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvColumns

/** What the library sync does with the presets the app ships. */
object ExercisePresets {

    /**
     * The presets to insert at launch. A preset the table does not have yet is left out when an exercise (a custom one, also
     * a hidden one) already carries its name: the user made it before the app learned it, and two with one name would
     * make every name check ambiguous. It comes in as soon as the name is free. A preset that is in the table is passed
     * on as it is; the insert ignores it. A [RetiredPresets] one comes in hidden.
     *
     * A [skipped] id (a cardio slot the user chose to leave out) is never returned. A cardio slot preset (164-166) that
     * is not in the table is also left out while any existing exercise, hidden or not, carries one of its aliases.
     */
    fun toInsert(
        presets: List<ExerciseEntity>,
        existing: List<ExerciseEntity>,
        skipped: Set<Int> = emptySet()
    ): List<ExerciseEntity> {
        val ids = existing.mapTo(hashSetOf()) { it.id }
        val names = existing.mapTo(hashSetOf()) { HistoryCsvColumns.fold(it.nome) }
        return presets
            .filter { it.id !in skipped }
            .filter { it.id in ids || HistoryCsvColumns.fold(it.nome) !in names }
            .filter { it.id in ids || it.id !in CardioPresets.SLOT_IDS || CardioPresets.aliasesOf(it).none { a -> a in names } }
            .map(RetiredPresets::forInsert)
    }

    /**
     * The presets "Reset preset names" gives their shipped name back to. A [skipped] id is left out: that slot holds a
     * row of the user's (an exercise that took the id before the preset shipped), never a preset, so it is not renamed.
     */
    fun toRename(presets: List<ExerciseEntity>, skipped: Set<Int>): List<ExerciseEntity> =
        presets.filter { it.id !in skipped }
}
