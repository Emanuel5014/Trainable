package com.emanuel5014.trainable.widget.model

import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName

/** What the Routine and Quick start widgets show, and so what their main action does. */
sealed interface RoutineWidgetState {
    data class InProgress(val sessionId: Int, val planId: Int, val name: String, val startedAt: Long) : RoutineWidgetState
    data class Suggested(val planId: Int, val name: String, val today: Boolean, val exerciseCount: Int?) : RoutineWidgetState
    data object NoRoutine : RoutineWidgetState
}

/** The letter in the Cookie badge: the name's first character, uppercased ("?" for a blank name). */
val RoutineWidgetState.InProgress.initial: String get() = initialOf(name)
val RoutineWidgetState.Suggested.initial: String get() = initialOf(name)

private fun initialOf(name: String): String =
    name.trim().firstOrNull()?.toString()?.uppercase() ?: "?"

/** An open workout first, then today's routine, then the next one in the sequence (see WorkoutSuggester). */
fun routineWidgetState(
    unfinished: SessionWithPlanName?,
    todayPlan: WorkoutPlanEntity?,
    suggestedPlan: WorkoutPlanEntity?,
    exerciseCount: Int?
): RoutineWidgetState = when {
    unfinished != null -> RoutineWidgetState.InProgress(
        sessionId = unfinished.session.id,
        planId = unfinished.session.planId,
        name = unfinished.displayName,
        startedAt = unfinished.session.timestamp
    )
    todayPlan != null -> RoutineWidgetState.Suggested(todayPlan.id, todayPlan.nome, today = true, exerciseCount = exerciseCount)
    suggestedPlan != null -> RoutineWidgetState.Suggested(suggestedPlan.id, suggestedPlan.nome, today = false, exerciseCount = exerciseCount)
    else -> RoutineWidgetState.NoRoutine
}
