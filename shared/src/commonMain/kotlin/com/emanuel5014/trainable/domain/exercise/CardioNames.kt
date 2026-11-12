package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity

/** The name a cardio log is shown under: the one of the exercise it is linked to, so a rename is followed. */
object CardioNames {

    /** The linked exercise's name in [languageCode]; the name written on the log only when it is not linked (or the exercise is gone). */
    fun of(log: CardioLogEntity, exercises: Map<Int, ExerciseEntity>, languageCode: String): String =
        of(log.exerciseId, log.categoria, exercises, languageCode)

    /** Same for a row reduced to its [exerciseId] and the name written on it ([fallback]). */
    fun of(exerciseId: Int?, fallback: String, exercises: Map<Int, ExerciseEntity>, languageCode: String): String {
        val exercise = exerciseId?.let { exercises[it] } ?: return ExerciseTranslations.translate(fallback.trim(), languageCode)
        return ExerciseTranslations.translate(exercise.nome, languageCode)
    }
}
