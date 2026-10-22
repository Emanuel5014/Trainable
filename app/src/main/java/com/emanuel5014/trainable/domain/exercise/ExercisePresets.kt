package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvColumns

/** What the library sync does with the presets the app ships. */
object ExercisePresets {

    /**
     * The presets to insert at launch. A preset the table does not have yet is left out when an exercise (a custom one, also
     * a hidden one) already carries its name: the user made it before the app learned it, and two with one name would
     * make every name check ambiguous. It comes in as soon as the name is free. A preset that is in the table is passed
     * on as it is; the insert ignores it.
     */
    fun toInsert(presets: List<ExerciseEntity>, existing: List<ExerciseEntity>): List<ExerciseEntity> {
        val ids = existing.mapTo(hashSetOf()) { it.id }
        val names = existing.mapTo(hashSetOf()) { HistoryCsvColumns.fold(it.nome) }
        return presets.filter { it.id in ids || HistoryCsvColumns.fold(it.nome) !in names }
    }
}
