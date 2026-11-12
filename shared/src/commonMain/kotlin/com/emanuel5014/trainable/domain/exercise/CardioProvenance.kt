package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvColumns

/**
 * Links an old cardio log to the exercise its workout came from, before the name matcher: a workout from a routine wrote
 * the name of one of the routine's exercises (or of a swap replacement of the session), so when exactly one of those
 * exercises still has that name, the log is that exercise's, whatever its category. This keeps a workout left open
 * across the update on the exercise the routine holds, and keeps old and new logs of a routine exercise together.
 * Pure: the caller reads the tables and applies the links.
 */
object CardioProvenance {
    /** A cardio log not linked yet, with its session and the routine of that session (null when there is none). */
    data class Log(val logId: Int, val name: String, val sessionId: Int, val planId: Int?)

    /** One exercise a routine ([owner] = plan id) or a session ([owner] = session id) used. */
    data class Use(val owner: Int, val exerciseId: Int)

    /**
     * The logs whose name is, once cleaned like the matcher does, the current name of exactly one exercise among those of
     * their routine ([planExercises]) and the swap replacements of their session ([swaps]); log id to exercise id. A
     * skipped preset id is never a candidate; a blank name, none or several candidates leave the log to the matcher.
     */
    fun links(
        logs: List<Log>,
        planExercises: List<Use>,
        swaps: List<Use>,
        exercises: List<ExerciseEntity>,
        skipped: Set<Int>
    ): Map<Int, Int> {
        val byPlan = planExercises.groupBy({ it.owner }, { it.exerciseId })
        val bySession = swaps.groupBy({ it.owner }, { it.exerciseId })
        val names = exercises.associate { it.id to HistoryCsvColumns.fold(it.nome) }
        val links = LinkedHashMap<Int, Int>()
        for (log in logs.sortedBy { it.logId }) {
            val key = HistoryCsvColumns.fold(log.name)
            if (key.isEmpty()) continue
            val candidates = (log.planId?.let { byPlan[it] }.orEmpty() + bySession[log.sessionId].orEmpty())
                .toSet()
                .filter { it !in skipped && names[it] == key }
            if (candidates.size == 1) links[log.logId] = candidates.single()
        }
        return links
    }
}
