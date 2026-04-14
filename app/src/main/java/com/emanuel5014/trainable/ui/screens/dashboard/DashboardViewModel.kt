package com.emanuel5014.trainable.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val username: String = "Athlete",
    val suggestedPlan: WorkoutPlanEntity? = null,
    val weeklyVolumeTons: Float = 0f,
    val weeklyGoal: Int = 3,
    val workoutsThisWeek: Int = 0,
    val unfinishedSessions: List<SessionWithPlanName> = emptyList(),
    val prSnapshots: List<PrSnapshot> = emptyList(),
    val gymMembershipExpiryDate: Long? = null,
    val isLoading: Boolean = true
)

data class PrSnapshot(
    val exerciseName: String,
    val weightKg: Float,
    val isTrendingUp: Boolean
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val userRepository: UserRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    private val weekStartMillis = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    private val todayStartMillis = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    
    val uiState: StateFlow<DashboardUiState> = combine(
        workoutRepository.getActivePlans(),
        analyticsRepository.getTotalVolume(),
        userRepository.currentUser,
        combine(
            userPrefsRepository.weeklyGoal,
            userPrefsRepository.gymMembershipExpiryDate
        ) { goal, expiry -> goal to expiry },
        combine(
            workoutRepository.getAllSessions(),
            workoutRepository.getUnfinishedSessionsWithPlanName()
        ) { all, unf -> all to unf }
    ) { plans, volume, user, prefs, sessionData ->
        val (goal, membershipExpiry) = prefs
        val (allSessions, unfinished) = sessionData
        val workoutsThisWeek = allSessions.count { it.timestamp >= weekStartMillis }
        
        // Suggested Plan Logic:
        // If we did a session today, suggest the next one in sequence.
        // Otherwise, suggest the one that corresponds to our current progress this week.
        val lastSession = allSessions.firstOrNull()
        val suggestedPlan = if (plans.isNotEmpty()) {
            if (lastSession != null && lastSession.timestamp >= todayStartMillis) {
                // We trained today, suggest the NEXT one
                val lastPlanIndex = plans.indexOfFirst { it.id == lastSession.planId }
                if (lastPlanIndex != -1) {
                    plans[(lastPlanIndex + 1) % plans.size]
                } else plans.first()
            } else {
                // Suggest based on how many workouts we've done this week
                plans[workoutsThisWeek % plans.size]
            }
        } else null

        DashboardUiState(
            username = user?.username ?: "Athlete",
            suggestedPlan = suggestedPlan,
            weeklyVolumeTons = (volume ?: 0f) / 1000f,
            weeklyGoal = goal,
            workoutsThisWeek = workoutsThisWeek,
            unfinishedSessions = unfinished,
            isLoading = false,
            gymMembershipExpiryDate = membershipExpiry,
            prSnapshots = listOf(
                PrSnapshot("BENCH PRESS", 120.0f, true),
                PrSnapshot("SQUAT", 160.5f, false)
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun setGymMembershipExpiryDate(timestampMillis: Long?) {
        viewModelScope.launch {
            userPrefsRepository.setGymMembershipExpiryDate(timestampMillis)
        }
    }
}
