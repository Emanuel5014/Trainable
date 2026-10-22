package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvColumns

/** Names of exercises: two exercises with the same name are what the merge of issue #50 has to clean up, so none is made. */
object ExerciseNames {

    /**
     * True when [name] is the name, stored or translated into [languageCode], of an exercise in the library (not hidden),
     * ignoring case, spaces and accents. [excludeId] is the exercise being edited, which may keep its own name.
     * A blank name is never taken (the form asks for one on its own).
     */
    fun isTaken(name: String, exercises: List<ExerciseEntity>, languageCode: String, excludeId: Int? = null): Boolean {
        val wanted = HistoryCsvColumns.fold(name.trim())
        if (wanted.isEmpty()) return false
        return exercises.any { exercise ->
            !exercise.isDeleted && exercise.id != excludeId &&
                (HistoryCsvColumns.fold(exercise.nome) == wanted ||
                    HistoryCsvColumns.fold(ExerciseTranslations.translate(exercise.nome, languageCode)) == wanted)
        }
    }
}
