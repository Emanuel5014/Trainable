package com.emanuel5014.trainable.domain.csvimport

import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity

/** The exercise of this device (or the one to create) that an exercise name of the file stands for. */
sealed interface ResolvedExercise {
    /** Same for every spelling of the same exercise in the file. */
    val key: String

    val isCardio: Boolean

    data class Existing(
        override val key: String,
        val exercise: ExerciseEntity,
        /** Found through its name in another language ("Panca Piana" for "Flat Bench Press"). */
        val viaTranslation: Boolean
    ) : ResolvedExercise {
        override val isCardio: Boolean get() = exercise.categoria.equals(CARDIO, ignoreCase = true)
    }

    /** Not on this device: it is created as a custom exercise with the name and category of the file. */
    data class New(
        override val key: String,
        val name: String,
        val category: String,
        /** The category is not one of this device either, so it is created with the exercise. */
        val categoryIsNew: Boolean
    ) : ResolvedExercise {
        override val isCardio: Boolean get() = category.equals(CARDIO, ignoreCase = true)
    }

    companion object {
        const val CARDIO = "Cardio"
        const val DEFAULT_CATEGORY = "Custom"
    }
}

/**
 * Matches the exercise names of a file to the exercises of this device. The file is written with the names of the app
 * in the language it had at export time, so a name is looked up as stored, then through the translations of every
 * language; custom exercises are told by their name alone (their ids mean nothing on another device).
 */
class HistoryExerciseResolver(
    known: List<ExerciseEntity>,
    customCategories: List<String>,
    /** The exercises as they are, when [known] carries merged-away ones under their old names (see ExerciseMerges.aliasView). */
    realExercises: List<ExerciseEntity> = known
) {
    private val byName: Map<String, List<ExerciseEntity>> =
        known.sortedBy { it.id }.groupBy { HistoryCsvColumns.fold(it.nome) }

    /** Category as the device writes it, by folded spelling: built-ins, those of its exercises and its own. */
    private val categories: Map<String, String> = buildMap {
        BUILT_IN_CATEGORIES.forEach { put(HistoryCsvColumns.fold(it), it) }
        translatedCategories.forEach { (translated, canonical) -> putIfAbsent(translated, canonical) }
        known.forEach { putIfAbsent(HistoryCsvColumns.fold(it.categoria), it.categoria) }
        customCategories.forEach { putIfAbsent(HistoryCsvColumns.fold(it), it) }
    }

    private val realById: Map<Int, ExerciseEntity> = realExercises.associateBy { it.id }

    private val resolved = mutableMapOf<Triple<String, String, Boolean?>, ResolvedExercise>()

    /** A new exercise is created once, whatever category its rows give. */
    private val created = mutableMapOf<String, ResolvedExercise>()

    /**
     * @param cardio whether the row is cardio: true makes a new exercise belong to the Cardio category whatever the file
     * says, and with several candidates the matching kind is preferred; null when the row does not say.
     */
    fun resolve(name: String, category: String, cardio: Boolean?): ResolvedExercise {
        val folded = HistoryCsvColumns.fold(name)
        // The same name can mean two exercises (French calls "Machine Row" "Rowing Machine", which is also a cardio one)
        return resolved.getOrPut(Triple(folded, HistoryCsvColumns.fold(category), cardio)) {
            find(folded, category, cardio) ?: created.getOrPut(folded) { create(name, category, cardio, folded) }
        }
    }

    private fun find(folded: String, category: String, cardio: Boolean?): ResolvedExercise? {
        // Stored names first, then what the name translates from; the file's category and type pick among them
        val candidates = buildList {
            byName[folded]?.forEach { add(it to false) }
            translatedNames[folded].orEmpty().forEach { canonical ->
                byName[HistoryCsvColumns.fold(canonical)]?.forEach { add(it to true) }
            }
        }.distinctBy { it.first.id }
        if (candidates.isEmpty()) return null

        val wanted = categoryOf(category)?.let { HistoryCsvColumns.fold(it) } ?: category.takeIf { it.isNotBlank() }?.let { HistoryCsvColumns.fold(it) }
        fun cost(candidate: Pair<ExerciseEntity, Boolean>): Int {
            var cost = 0
            if (wanted == null || HistoryCsvColumns.fold(candidate.first.categoria) != wanted) cost += 4
            if (cardio != null && candidate.first.categoria.equals(ResolvedExercise.CARDIO, ignoreCase = true) != cardio) cost += 2
            if (candidate.second) cost += 1
            return cost
        }
        val best = candidates.minBy { cost(it) }
        return existing(best.first, viaTranslation = best.second)
    }

    private fun create(name: String, category: String, cardio: Boolean?, folded: String): ResolvedExercise {
        val known = categoryOf(category)
        val finalCategory = when {
            cardio == true -> ResolvedExercise.CARDIO
            known != null -> known
            category.isNotBlank() -> category.trim()
            else -> ResolvedExercise.DEFAULT_CATEGORY
        }
        val isNew = cardio != true && known == null && category.isNotBlank()
        return ResolvedExercise.New(key = "new:$folded", name = name.trim(), category = finalCategory, categoryIsNew = isNew)
    }

    /** The category of the device a category text of the file is, in any language; null when it is a new one. */
    fun categoryOf(text: String): String? = categories[HistoryCsvColumns.fold(text)]

    // Two spellings of the same exercise ("Pull-ups", "Trazioni") are one exercise of the file
    // The match may be a winner under the name of an exercise merged into it: what is reported is the exercise itself
    private fun existing(exercise: ExerciseEntity, viaTranslation: Boolean) =
        ResolvedExercise.Existing("exercise:${exercise.id}", realById[exercise.id] ?: exercise, viaTranslation)

    companion object {
        /** The stored (English) name an exercise name of any language stands for, the name itself when it is none. */
        fun canonicalName(name: String): String = translatedNames[HistoryCsvColumns.fold(name)]?.firstOrNull() ?: name

        private val BUILT_IN_CATEGORIES = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Cardio")

        private val allLanguages = listOf(
            ExerciseTranslations.it to ExerciseTranslations.itCategories,
            ExerciseTranslations.es to ExerciseTranslations.esCategories,
            ExerciseTranslations.fr to ExerciseTranslations.frCategories,
            ExerciseTranslations.de to ExerciseTranslations.deCategories,
            ExerciseTranslations.pt to ExerciseTranslations.ptCategories
        )

        /** Translated exercise name (folded) -> the stored names it translates, in the order the languages list them. */
        private val translatedNames: Map<String, List<String>> = buildMap<String, MutableList<String>> {
            allLanguages.forEach { (names, _) ->
                names.forEach { (canonical, translated) ->
                    val list = getOrPut(HistoryCsvColumns.fold(translated)) { mutableListOf() }
                    if (canonical !in list) list.add(canonical)
                }
            }
        }

        /** Translated category (folded) -> built-in category. */
        private val translatedCategories: Map<String, String> = buildMap {
            allLanguages.forEach { (_, categories) ->
                categories.forEach { (canonical, translated) -> putIfAbsent(HistoryCsvColumns.fold(translated), canonical) }
            }
        }
    }
}
