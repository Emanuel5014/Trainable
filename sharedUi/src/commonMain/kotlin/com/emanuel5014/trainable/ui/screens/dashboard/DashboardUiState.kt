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
    /** The unfinished workout the live card follows (see [LiveWorkout.pick]); null when none. */
    val liveSession: SessionWithPlanName? = null,
    /** What the open workout last wrote about itself; may belong to another session or be missing. */
    val liveSnapshot: LiveWorkoutSnapshot? = null,
    /** The switch: while a workout runs, the dashboard puts the live panel and the exercise list on top. */
    val workoutMode: Boolean = false,
    val restButtons: RestButtons = RestButtons(),
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
    fun adjustRest(sessionId: Int, button: RestButton)
}
