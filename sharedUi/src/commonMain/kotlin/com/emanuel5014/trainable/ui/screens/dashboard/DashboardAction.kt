package com.emanuel5014.trainable.ui.screens.dashboard

import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName

/** Something the Dashboard offers to start or continue; the first one gets the big card above the navbar. */
sealed interface DashboardAction {
    data class Resume(val session: SessionWithPlanName) : DashboardAction
    data class Today(val plan: WorkoutPlanEntity) : DashboardAction
    data class Suggested(val plan: WorkoutPlanEntity) : DashboardAction
}

/** Unfinished workouts first (in their own order), then today's routine, then the suggested one. */
fun dashboardActions(uiState: DashboardUiState): List<DashboardAction> = buildList {
    uiState.unfinishedSessions.forEach { add(DashboardAction.Resume(it)) }
    uiState.todayPlan?.let { add(DashboardAction.Today(it)) }
    uiState.suggestedPlan?.let { add(DashboardAction.Suggested(it)) }
}

/** The unfinished workout of [planId], if any: starting that routine again asks to resume it instead. */
fun unfinishedSessionForPlan(uiState: DashboardUiState, planId: Int): SessionWithPlanName? =
    uiState.unfinishedSessions.find { it.session.planId == planId }
