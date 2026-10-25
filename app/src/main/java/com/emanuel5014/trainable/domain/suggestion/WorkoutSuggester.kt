package com.emanuel5014.trainable.domain.suggestion

import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** What the dashboard offers to train: the routine assigned to today, or just the one that comes next. */
data class WorkoutSuggestion(
    val todayPlan: WorkoutPlanEntity?,
    val suggestedPlan: WorkoutPlanEntity?
)

/**
 * Picks the routine to offer. The athlete goes through the routines in turn, so the next one is the one after the last
 * routine they did, in the order of the routines, and the sequence carries on across weeks (with 3 routines and 2
 * workouts a week it never lines up with the calendar). The days a routine is assigned to only decide how it is
 * presented (today's workout or a suggestion) and which routine starts the sequence when there is no history yet:
 * they never put a routine ahead of the one that is next, or the routine done two days ago would come back.
 */
object WorkoutSuggester {

    /**
     * [plans] are the active routines in the order of the user, [finishedSessions] any finished sessions in any order
     * (those of Quick / Cardio workouts and of archived routines are not part of the sequence).
     */
    fun suggest(
        plans: List<WorkoutPlanEntity>,
        finishedSessions: List<WorkoutSessionEntity>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): WorkoutSuggestion {
        if (plans.isEmpty()) return WorkoutSuggestion(todayPlan = null, suggestedPlan = null)

        val scheduledToday = plans.filter { today.dayOfWeek in scheduledDays(it) }
        val lastRoutineSession = finishedSessions
            .filter { session -> plans.any { it.id == session.planId } }
            .maxByOrNull { it.timestamp }
        val lastIndex = lastRoutineSession?.let { last -> plans.indexOfFirst { it.id == last.planId } } ?: -1

        val next = if (lastIndex != -1) {
            plans[(lastIndex + 1) % plans.size]
        } else {
            // Nothing to continue from yet: the schedule is all there is
            scheduledToday.firstOrNull() ?: plans.first()
        }

        // Any workout done today, a Quick or Cardio one too, takes today's workout off the screen: only the next
        // routine is suggested
        val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val trainedToday = finishedSessions.any { it.timestamp in startOfToday until startOfTomorrow }

        return if (!trainedToday && next in scheduledToday) {
            WorkoutSuggestion(todayPlan = next, suggestedPlan = null)
        } else {
            WorkoutSuggestion(todayPlan = null, suggestedPlan = next)
        }
    }

    /** The days a routine is assigned to, stored as "1,3,5" (1 = Monday ... 7 = Sunday). */
    private fun scheduledDays(plan: WorkoutPlanEntity): Set<DayOfWeek> =
        plan.giorniSettimana.orEmpty().split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .mapTo(mutableSetOf()) { DayOfWeek.of(it) }
}
