package com.emanuel5014.trainable.data.repository

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity

/**
 * Finds the exercise of this device that an imported plan exercise refers to.
 *
 * Built-in exercises have the same id everywhere, but custom ones (id >= [FIRST_CUSTOM_ID]) are numbered
 * per device, so "id 1001" is a different exercise for every user. Matching those by id attached imported
 * plans to the wrong exercise (a bench variation turned into "Affondi Multipower"); the name is what
 * identifies a custom exercise, the id is only trusted for built-in ones.
 */
internal object ImportedExerciseMatcher {

    const val FIRST_CUSTOM_ID = 1000

    sealed interface Match {
        data class Existing(val exercise: ExerciseEntity) : Match

        /** Not on this device: create it as a custom exercise with the name and category from the file. */
        data object CreateCustom : Match

        /** Nothing to go on (old file without a name, unknown id): the exercise is skipped. */
        data object Unresolvable : Match
    }

    fun match(known: List<ExerciseEntity>, exerciseId: Int, exerciseName: String?): Match {
        val name = exerciseName?.trim().takeUnless { it.isNullOrEmpty() }

        if (name != null) {
            known.firstOrNull { it.nome.equals(name, ignoreCase = true) }?.let { return Match.Existing(it) }
            // A built-in exercise that was renamed between app versions keeps its id
            if (exerciseId < FIRST_CUSTOM_ID) {
                known.firstOrNull { it.id == exerciseId }?.let { return Match.Existing(it) }
            }
            return Match.CreateCustom
        }

        // Files from before exercise names were exported only carry the id
        return known.firstOrNull { it.id == exerciseId }?.let { Match.Existing(it) } ?: Match.Unresolvable
    }
}
