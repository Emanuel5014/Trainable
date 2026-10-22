package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvColumns
import com.emanuel5014.trainable.domain.csvimport.HistoryExerciseResolver

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

    /**
     * True when a visible exercise other than [exercise] carries its name, stored or as shown in [languageCode]: bringing
     * [exercise] back would leave two with one name.
     */
    fun takenByAnother(exercise: ExerciseEntity, exercises: List<ExerciseEntity>, languageCode: String): Boolean =
        isTaken(exercise.nome, exercises, languageCode, excludeId = exercise.id) ||
            isTaken(ExerciseTranslations.translate(exercise.nome, languageCode), exercises, languageCode, excludeId = exercise.id)

    /**
     * The hidden exercise [name] brings back: the one whose stored name, or whose name in any language of the app, is [name]
     * once case, accents, spaces and punctuation are set aside (the same rule as [isTaken]). Exercises merged into another
     * ([mergedAwayIds]) are not brought back: their name belongs to the exercise they were merged into.
     */
    fun findHidden(name: String, exercises: List<ExerciseEntity>, mergedAwayIds: Set<Int> = emptySet()): ExerciseEntity? {
        val typed = HistoryCsvColumns.fold(name.trim())
        if (typed.isEmpty()) return null
        // A name in any language stands for the stored (English) one; the spelling as typed counts too
        val wanted = setOf(typed, HistoryCsvColumns.fold(HistoryExerciseResolver.canonicalName(name.trim())))
        return exercises.firstOrNull { it.isDeleted && it.id !in mergedAwayIds && HistoryCsvColumns.fold(it.nome) in wanted }
    }
}
