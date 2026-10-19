package com.emanuel5014.trainable.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.domain.suggestion.WorkoutSuggester
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.notification.GymMembershipWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
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

    /** The day the dashboard is computed for; the ViewModel outlives midnight, so the screen refreshes it on resume. */
    private val today = MutableStateFlow(LocalDate.now())

    private fun weekStartMillis(day: LocalDate): Long {
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        return day.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun refreshToday() {
        today.value = LocalDate.now()
    }

    private data class SessionData(
        val all: List<WorkoutSessionEntity>,
        val unfinished: List<SessionWithPlanName>,
        val cardioCount: Int,
        val day: LocalDate
    )

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    private val _selectedSessionIds = MutableStateFlow<Set<Int>>(emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
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
            today.flatMapLatest { workoutRepository.getCardioSessionCountSince(weekStartMillis(it)) },
            today
        ) { all, unf, cardioCount, day -> SessionData(all, unf, cardioCount, day) },
        _selectedSessionIds
    ) { planVolumePair, user, prefs, sessionData, selectedIds ->
        val (plans, volume) = planVolumePair
        val (goal, membershipExpiry, dynamic, palette, swipe, floating) = prefs
        val (allSessions, unfinished, cardioCount, day) = sessionData
        val weekStart = weekStartMillis(day)
        val numWorkoutsThisWeek = allSessions.count { it.timestamp >= weekStart }

        // Which routine to offer: see WorkoutSuggester (the sequence of routines, the days only shape the card)
        val (todayPlan, suggestedPlan) = WorkoutSuggester.suggest(plans, allSessions, day)

        DashboardUiState(
            username = user?.username ?: "Athlete",
            suggestedPlan = suggestedPlan,
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
    
    val physicalCheckBiometricEnabled = userPrefsRepository.physicalCheckBiometricEnabled


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
