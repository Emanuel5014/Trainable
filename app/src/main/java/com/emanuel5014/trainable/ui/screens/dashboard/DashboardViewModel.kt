package com.emanuel5014.trainable.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.notification.GymMembershipWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

private data class PreferencesData(
    val goal: Int,
    val expiry: Long?,
    val dynamic: Boolean,
    val palette: Int,
    val swipe: Boolean,
    val floating: Boolean
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
    private val userPrefsRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
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
    private val _selectedSessionIds = MutableStateFlow<Set<Int>>(emptySet())
    
    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            workoutRepository.getActivePlans(),
            analyticsRepository.getTotalVolume()
        ) { plans, volume -> plans to volume },
        userRepository.currentUser,
        combine(
            combine(
                userPrefsRepository.weeklyGoal,
                userPrefsRepository.gymMembershipExpiryDate,
                userPrefsRepository.dynamicColor,
                userPrefsRepository.themePalette,
                userPrefsRepository.swipeActionsEnabled
            ) { goal, expiry, dynamic, palette, swipe -> 
                listOf(goal, expiry, dynamic, palette, swipe)
            },
            userPrefsRepository.floatingNavBar
        ) { prefsList, floating -> 
            PreferencesData(
                goal = prefsList[0] as Int,
                expiry = prefsList[1] as Long?,
                dynamic = prefsList[2] as Boolean,
                palette = prefsList[3] as Int,
                swipe = prefsList[4] as Boolean,
                floating = floating
            )
        },
        combine(
            workoutRepository.getAllSessions(),
            workoutRepository.getUnfinishedSessionsWithPlanName(),
            workoutRepository.getCardioSessionCountSince(weekStartMillis)
        ) { all, unf, cardioCount -> Triple(all, unf, cardioCount) },
        _selectedSessionIds
    ) { planVolumePair, user, prefs, sessionData, selectedIds ->
        val (plans, volume) = planVolumePair
        val (goal, membershipExpiry, dynamic, palette, swipe, floating) = prefs
        val (allSessions, unfinished, cardioCount) = sessionData
        val workoutsThisWeek = allSessions.filter { it.timestamp >= weekStartMillis }
        val numWorkoutsThisWeek = workoutsThisWeek.size
        
        // Suggested Plan Logic:
        // 1. If we haven't trained today and there's a plan assigned for today, prioritize it.
        // 2. Otherwise, find the first plan in the list that hasn't been performed yet this week.
        // 3. If all plans have been performed at least once this week, suggest the next one in sequence after the last session.
        
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val currentDayValue = when (today) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }.toString()

        val planForToday = plans.find { it.giorniSettimana?.split(",")?.contains(currentDayValue) == true }
        val lastSession = allSessions.firstOrNull()
        val trainedToday = lastSession != null && lastSession.timestamp >= todayStartMillis
        
        val idsPerformedThisWeek = workoutsThisWeek.map { it.planId }.toSet()
        val firstNotPerformedThisWeek = plans.find { it.id !in idsPerformedThisWeek }

        val todayPlan = if (!trainedToday) planForToday else null
        
        var suggestedPlan: WorkoutPlanEntity? = null
        if (plans.isNotEmpty()) {
            suggestedPlan = if (firstNotPerformedThisWeek != null) {
                firstNotPerformedThisWeek
            } else {
                val lastPlanIndex = plans.indexOfFirst { it.id == lastSession?.planId }
                if (lastPlanIndex != -1) {
                    plans[(lastPlanIndex + 1) % plans.size]
                } else {
                    plans.first()
                }
            }
        }

        // Avoid showing the same plan twice
        val finalSuggestedPlan = if (suggestedPlan?.id == todayPlan?.id) null else suggestedPlan

        DashboardUiState(
            username = user?.username ?: "Athlete",
            suggestedPlan = finalSuggestedPlan,
            todayPlan = todayPlan,
            weeklyVolumeTons = (volume ?: 0f) / 1000f,
            weeklyGoal = goal,
            workoutsThisWeek = numWorkoutsThisWeek,
            cardioWorkoutsThisWeek = cardioCount,
            unfinishedSessions = unfinished,
            isLoading = false,
            gymMembershipExpiryDate = membershipExpiry,
            dynamicColor = dynamic,
            themePalette = palette,
            floatingNavBar = floating,
            swipeActionsEnabled = swipe,
            selectedSessionIds = selectedIds,
            isSelectionMode = selectedIds.isNotEmpty(),
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
            if (timestampMillis != null) {
                GymMembershipWorker.enqueueImmediateCheck(context)
            }
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            workoutRepository.deleteSession(sessionId)
        }
    }

    fun toggleSelection(sessionId: Int) {
        val current = _selectedSessionIds.value.toMutableSet()
        if (current.contains(sessionId)) {
            current.remove(sessionId)
        } else {
            current.add(sessionId)
        }
        _selectedSessionIds.value = current
    }

    fun clearSelection() {
        _selectedSessionIds.value = emptySet()
    }

    fun deleteSelectedSessions() {
        viewModelScope.launch {
            _selectedSessionIds.value.forEach { sessionId ->
                workoutRepository.deleteSession(sessionId)
            }
            clearSelection()
        }
    }
}
