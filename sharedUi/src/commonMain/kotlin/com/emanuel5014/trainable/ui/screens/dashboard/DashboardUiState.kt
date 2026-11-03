package com.emanuel5014.trainable.ui.screens.dashboard

import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName

data class DashboardUiState(
    val username: String = "Athlete",
    val suggestedPlan: WorkoutPlanEntity? = null,
    val todayPlan: WorkoutPlanEntity? = null,
    val weeklyVolumeTons: Float = 0f,
    val weeklyGoal: Int = 3,
    val workoutsThisWeek: Int = 0,
    val cardioWorkoutsThisWeek: Int = 0,
    val unfinishedSessions: List<SessionWithPlanName> = emptyList(),
    val prSnapshots: List<PrSnapshot> = emptyList(),
    val gymMembershipExpiryDate: Long? = null,
    val dynamicColor: Boolean = true,
    val themePalette: Int = 0,
    val floatingNavBar: Boolean = false,
    val swipeActionsEnabled: Boolean = true,
    val selectedSessionIds: Set<Int> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = true
)

data class PrSnapshot(
    val exerciseName: String,
    val weightKg: Float,
    val isTrendingUp: Boolean
)

/** What the Dashboard changes; each platform's state holder carries it out. */
interface DashboardActions {
    fun setGymMembershipExpiryDate(timestampMillis: Long?)
    fun deleteSession(sessionId: Int)
    fun toggleSelection(sessionId: Int)
    fun clearSelection()
    fun deleteSelectedSessions()
}
