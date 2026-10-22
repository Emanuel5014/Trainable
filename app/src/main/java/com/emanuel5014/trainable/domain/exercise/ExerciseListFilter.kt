package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity

/** What the exercises page lists. Every filter but [HIDDEN] leaves out the exercises hidden from the library. */
enum class ExerciseFilter { ALL, CUSTOM, WITH_MEDIA, WITHOUT_MEDIA, HIDDEN }

/** An exercise with its name as the app shows it (translated). */
data class ExerciseListEntry(val exercise: ExerciseEntity, val name: String)

/** The exercises of one category, [category] as the app shows it (translated). */
data class ExerciseGroup(val category: String, val entries: List<ExerciseListEntry>)

/** The rules behind the list of the exercises page, on plain data. */
object ExerciseListFilter {

    /**
     * The exercises that pass [filter] and match [query] (the stored or the translated name, any case), grouped by
     * translated category: groups in alphabetical order, entries by translated name, both ignoring case.
     */
    fun groups(exercises: List<ExerciseEntity>, query: String, filter: ExerciseFilter, languageCode: String): List<ExerciseGroup> {
        val needle = query.trim()
        return exercises.asSequence()
            .filter { if (filter == ExerciseFilter.HIDDEN) it.isDeleted else !it.isDeleted }
            .filter {
                when (filter) {
                    ExerciseFilter.ALL, ExerciseFilter.HIDDEN -> true
                    ExerciseFilter.CUSTOM -> it.id >= ExerciseEntity.FIRST_CUSTOM_ID
                    ExerciseFilter.WITH_MEDIA -> it.mediaPath != null
                    ExerciseFilter.WITHOUT_MEDIA -> it.mediaPath == null
                }
            }
            .map { ExerciseListEntry(it, ExerciseTranslations.translate(it.nome, languageCode)) }
            .filter { needle.isEmpty() || it.name.contains(needle, ignoreCase = true) || it.exercise.nome.contains(needle, ignoreCase = true) }
            .groupBy { ExerciseTranslations.translateCategory(it.exercise.categoria, languageCode) }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .map { (category, entries) -> ExerciseGroup(category, entries.sortedBy { it.name.lowercase() }) }
    }

    /** The filters to offer: Hidden only when something is hidden, and while it is selected so its chip does not vanish. */
    fun filtersToShow(hiddenCount: Int, selected: ExerciseFilter): List<ExerciseFilter> {
        val base = listOf(ExerciseFilter.ALL, ExerciseFilter.CUSTOM, ExerciseFilter.WITH_MEDIA, ExerciseFilter.WITHOUT_MEDIA)
        return if (hiddenCount > 0 || selected == ExerciseFilter.HIDDEN) base + ExerciseFilter.HIDDEN else base
    }
}
