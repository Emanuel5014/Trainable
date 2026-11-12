package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.ExerciseData
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvColumns

/** The cardio presets a cardio log is linked to by name, and the names each of them answers to. */
object CardioPresets {
    const val RUNNING = 164
    const val CYCLING = 165
    const val WALKING = 166

    /** The presets that were chip words before they were exercises (their ids may be blocked or skipped, see the spec). */
    val SLOT_IDS: Set<Int> = setOf(RUNNING, CYCLING, WALKING)

    /**
     * Every preset id that has ever shipped, frozen: a new preset must use an id above 163, so a log or a
     * backup that points at an old id never lands on a different exercise.
     */
    val EVER_USED_PRESET_IDS: Set<Int> =
        (1..62).toSet() + setOf(64, 65, 71) + (73..111) + (113..138) + (140..143) + (150..163)

    /** Words that were the old chip names, in every language they ever had, plus the English ones before version 23. */
    private val chipWords: Map<Int, List<String>> = mapOf(
        RUNNING to listOf("Running", "Run", "Corsa", "Laufen", "Correr", "Course", "Corrida"),
        CYCLING to listOf("Cycling", "Bike", "Bici", "Fahrrad", "Vélo", "Bicicleta"),
        WALKING to listOf("Walking", "Walk", "Camminata", "Gehen", "Caminar", "Marche", "Caminhada")
    )

    private val languages = listOf("it", "es", "fr", "de", "pt")

    /**
     * The folded names a cardio log may carry to belong to [preset]: its current stored name (also when the user
     * renamed it), its original English name, its five translations and, for the three slots, the old chip words.
     */
    fun aliasesOf(preset: ExerciseEntity): Set<String> {
        val names = mutableListOf(preset.nome)
        val original = ExerciseData.initialExercises.firstOrNull { it.id == preset.id }?.nome
        if (original != null) {
            names += original
            languages.forEach { names += ExerciseTranslations.translate(original, it) }
        }
        chipWords[preset.id]?.let { names += it }
        return names.map { HistoryCsvColumns.fold(it) }.filter { it.isNotEmpty() }.toSet()
    }
}
