package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.domain.csvimport.ResolvedExercise

/**
 * Why the category of an exercise cannot change: its history would stop matching what it is. Decided by the data linked
 * to it, not by the category it has now (a routine exercise of another category used as cardio has cardio logs too).
 */
enum class CategoryChangeBlock {
    /** It has cardio logs: it stays in Cardio. */
    HAS_CARDIO_LOGS,

    /** It has strength sets: it cannot become cardio. */
    HAS_STRENGTH_SETS;

    companion object {
        private fun isCardio(category: String) = category.trim().equals(ResolvedExercise.CARDIO, ignoreCase = true)

        /**
         * Out of Cardio is blocked by cardio logs, into Cardio by strength sets; a change between two other categories
         * (or Cardio to Cardio) is always allowed, even for an exercise that has cardio logs.
         */
        fun of(from: String, to: String, hasCardioLogs: Boolean, hasStrengthSets: Boolean): CategoryChangeBlock? {
            val wasCardio = isCardio(from)
            val toCardio = isCardio(to)
            return when {
                wasCardio == toCardio -> null
                wasCardio && hasCardioLogs -> HAS_CARDIO_LOGS
                toCardio && hasStrengthSets -> HAS_STRENGTH_SETS
                else -> null
            }
        }
    }
}
