package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity

/** What the exercise pickers list and in which order: the same rules for the picker and the swap sheet. */
object ExercisePickerFilter {

    /**
     * The exercises that pass every active filter: [query] (the stored or the translated name, any case), the stored
     * [category], [onlyCustom] (ids from [ExerciseEntity.FIRST_CUSTOM_ID]) and [onlyFavorites]; those in [excludedIds]
     * are left out. Favorites come first, each group in the order received.
     */
    fun apply(
        exercises: List<ExerciseEntity>,
        query: String,
        category: String?,
        onlyCustom: Boolean,
        onlyFavorites: Boolean,
        languageCode: String,
        excludedIds: Set<Int> = emptySet()
    ): List<ExerciseEntity> {
        val matching = exercises.filter { exercise ->
            exercise.id !in excludedIds &&
                (query.isBlank() ||
                    exercise.nome.contains(query, ignoreCase = true) ||
                    ExerciseTranslations.translate(exercise.nome, languageCode).contains(query, ignoreCase = true)) &&
                (category == null || exercise.categoria == category) &&
                (!onlyCustom || exercise.id >= ExerciseEntity.FIRST_CUSTOM_ID) &&
                (!onlyFavorites || exercise.isFavorite)
        }
        val (favorites, others) = matching.partition { it.isFavorite }
        return favorites + others
    }

    /** The "Favorites" chip shows while a listed exercise is a favorite, and stays while it is selected. */
    fun showFavoritesChip(exercises: List<ExerciseEntity>, excludedIds: Set<Int>, selected: Boolean): Boolean =
        selected || exercises.any { it.isFavorite && it.id !in excludedIds }
}
