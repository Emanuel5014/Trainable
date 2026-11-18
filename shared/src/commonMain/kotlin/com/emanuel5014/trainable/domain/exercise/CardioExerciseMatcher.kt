package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.ExerciseData
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvColumns

/**
 * Decides which exercise a cardio log belongs to, from the name written on it. It is the one place that decides, used by
 * the database migration, the startup repair and the CSV import, so the same history always lands on the same exercise.
 * Pure: no database, the caller passes the exercises it has and applies the plan.
 */
object CardioExerciseMatcher {
    data class Row(val logId: Int, val name: String)

    /** [newExercises] must be inserted by the caller; [links] maps every input log id to the exercise it belongs to. */
    data class LinkPlan(val newExercises: List<ExerciseEntity>, val links: Map<Int, Int>)

    private const val CARDIO = "Cardio"

    private fun isCardio(e: ExerciseEntity) = e.categoria.equals(CARDIO, ignoreCase = true)

    /** Aliases of a slot: from its row when it is there (it may be renamed), else from the shipped preset. */
    private fun slotAliases(slotId: Int, exercises: List<ExerciseEntity>): Set<String>? {
        val row = exercises.firstOrNull { it.id == slotId && isCardio(it) }
        val shipped = ExerciseData.initialExercises.firstOrNull { it.id == slotId } ?: return null
        return CardioPresets.aliasesOf(row ?: shipped)
    }

    /**
     * The exercise that answers to the aliases of a 164-166 slot: a custom cardio of the user carrying one of them
     * (so a custom `Running` keeps its history even when 164 is missing), else the preset row, else null.
     * A skipped slot has no owner at all.
     */
    fun slotOwner(presetId: Int, exercises: List<ExerciseEntity>, skipped: Set<Int>): Int? {
        if (presetId in skipped) return null
        val aliases = slotAliases(presetId, exercises) ?: return null
        val custom = exercises
            .filter { it.id >= ExerciseEntity.FIRST_CUSTOM_ID && isCardio(it) && HistoryCsvColumns.fold(it.nome) in aliases }
            .minWithOrNull(compareBy<ExerciseEntity> { it.isDeleted }.thenBy { it.id })
        if (custom != null) return custom.id
        return exercises.firstOrNull { it.id == presetId && isCardio(it) }?.id
    }

    fun plan(rows: List<Row>, exercises: List<ExerciseEntity>, skipped: Set<Int>, nextCustomId: Int): LinkPlan {
        // A blank name is still a log of some cardio: it goes to an exercise called "Cardio" like any other name.
        val groups = LinkedHashMap<String, MutableList<Row>>()
        val spellings = HashMap<String, MutableList<String>>()
        for (row in rows) {
            // A name with no letter or digit ("-", "?") says nothing: same as blank.
            val folded = HistoryCsvColumns.fold(row.name)
            val key = folded.ifEmpty { HistoryCsvColumns.fold(CARDIO) }
            val spelling = if (folded.isEmpty()) CARDIO else row.name.trim()
            groups.getOrPut(key) { mutableListOf() }.add(row)
            spellings.getOrPut(key) { mutableListOf() }.add(spelling)
        }

        val links = HashMap<Int, Int>()
        val created = mutableListOf<ExerciseEntity>()
        // Never trust the caller's counter: a clash with an existing id would corrupt a migration, so start above all.
        var nextId = maxOf(nextCustomId, ExerciseEntity.FIRST_CUSTOM_ID, (exercises.maxOfOrNull { it.id } ?: 0) + 1)
        for ((key, group) in groups) {
            var target = existingTarget(key, exercises, skipped)
            if (target == null) {
                // Most frequent spelling, ties to the smallest string, so the order of the rows never matters.
                val name = spellings.getValue(key).groupingBy { it }.eachCount()
                    .entries.sortedWith(compareBy({ -it.value }, { it.key })).first().key
                target = nextId++
                created += ExerciseEntity(id = target, nome = name, categoria = CARDIO)
            }
            group.forEach { links[it.logId] = target }
        }
        return LinkPlan(created, links)
    }

    private fun existingTarget(key: String, exercises: List<ExerciseEntity>, skipped: Set<Int>): Int? {
        // Rule 1: a custom cardio of the user with this name (visible first, then lowest id).
        exercises
            .filter { it.id >= ExerciseEntity.FIRST_CUSTOM_ID && isCardio(it) && HistoryCsvColumns.fold(it.nome) == key }
            .minWithOrNull(compareBy<ExerciseEntity> { it.isDeleted }.thenBy { it.id })
            ?.let { return it.id }

        // Rule 2: a cardio preset that answers to the name. A slot goes to whoever owns it, which may be a custom.
        for (slot in CardioPresets.SLOT_IDS.sorted()) {
            if (slot in skipped) continue
            if (key in (slotAliases(slot, exercises) ?: continue)) slotOwner(slot, exercises, skipped)?.let { return it }
        }
        return exercises
            .filter { it.id < ExerciseEntity.FIRST_CUSTOM_ID && it.id !in CardioPresets.SLOT_IDS && it.id !in skipped && isCardio(it) }
            .sortedBy { it.id }
            .firstOrNull { key in CardioPresets.aliasesOf(it) }
            ?.id
    }
}
