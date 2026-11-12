package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.RecentCardio

/** A cardio the picker proposes on top; [lastUsed] is the time of its newest log, null when it was never done. */
data class SuggestedExercise(val exercise: ExerciseEntity, val lastUsed: Long?)

/** The "Suggested" section of the cardio picker: the three everyday slots first, then the cardio done most recently. */
object CardioSuggestions {
    private const val MAX_RECENT = 3
    private val SLOTS = listOf(CardioPresets.RUNNING, CardioPresets.CYCLING, CardioPresets.WALKING)

    /** [recent] must be newest first. Hidden exercises are never suggested. */
    fun of(exercises: List<ExerciseEntity>, recent: List<RecentCardio>, skipped: Set<Int>): List<SuggestedExercise> {
        val byId = exercises.associateBy { it.id }
        val lastById = recent.groupBy { it.exerciseId }.mapValues { (_, v) -> v.maxOf { it.lastTimestamp } }
        val result = mutableListOf<SuggestedExercise>()
        for (slot in SLOTS) {
            val owner = CardioExerciseMatcher.slotOwner(slot, exercises, skipped)?.let { byId[it] } ?: continue
            if (owner.isDeleted || result.any { it.exercise.id == owner.id }) continue
            result += SuggestedExercise(owner, lastById[owner.id])
        }
        var added = 0
        for (r in recent) {
            if (added >= MAX_RECENT) break
            val e = byId[r.exerciseId] ?: continue
            if (e.isDeleted || !e.categoria.equals("Cardio", ignoreCase = true) || result.any { it.exercise.id == e.id }) continue
            result += SuggestedExercise(e, r.lastTimestamp)
            added++
        }
        return result
    }
}
