package com.emanuel5014.trainable.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.suggestion.WorkoutSuggester
import com.emanuel5014.trainable.widget.model.RoutineWidgetState
import com.emanuel5014.trainable.widget.model.routineWidgetState
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.toKotlinLocalDate

/** What the Routine and Quick start widgets show, from the database (same choice as the dashboard). */
@Composable
fun collectRoutineWidgetState(workoutRepository: WorkoutRepository, withExerciseCount: Boolean): RoutineWidgetState {
    val plans by workoutRepository.getActivePlans().collectAsState(initial = emptyList())
    val allSessions by workoutRepository.getAllSessions().collectAsState(initial = emptyList())
    val unfinishedSessions by workoutRepository.getUnfinishedSessionsWithPlanName().collectAsState(initial = emptyList())

    // The sequence of routines decides, the days only shape the card (see WorkoutSuggester)
    val (todayPlan, suggestedPlan) = WorkoutSuggester.suggest(plans, allSessions, LocalDate.now().toKotlinLocalDate())
    val countedPlanId = if (withExerciseCount) (todayPlan ?: suggestedPlan)?.id else null
    val exerciseCount by remember(countedPlanId) {
        countedPlanId?.let { workoutRepository.countPlanExercises(it) } ?: flowOf(null)
    }.collectAsState(initial = null)

    return routineWidgetState(unfinishedSessions.firstOrNull(), todayPlan, suggestedPlan, exerciseCount)
}
