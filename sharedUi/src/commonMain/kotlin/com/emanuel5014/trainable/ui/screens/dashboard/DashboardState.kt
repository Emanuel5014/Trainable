package com.emanuel5014.trainable.ui.screens.dashboard

import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.domain.suggestion.WorkoutSuggester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.emanuel5014.trainable.domain.weekStartMillis
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** The workouts and routines the Dashboard reads, and the one change to them it makes. */
interface DashboardData {
    fun activePlans(): Flow<List<WorkoutPlanEntity>>
    fun allSessions(): Flow<List<WorkoutSessionEntity>>
    fun unfinishedSessions(): Flow<List<SessionWithPlanName>>
    fun cardioSessionCountSince(sinceTimestamp: Long): Flow<Int>
    suspend fun deleteSession(sessionId: Int)
}

/** The day the user's week starts on, from the phone's region (Monday in Italy, Sunday in the US). */
expect fun firstDayOfWeek(): DayOfWeek

/**
 * The Dashboard's state, the same on every platform: each one gives it the data and its own lifetime ([scope]), plus
 * what it alone does when a membership expiry is set ([onMembershipSet], the reminder on Android).
 */
class DashboardState(
    private val data: DashboardData,
    analyticsRepository: AnalyticsRepository,
    userRepository: UserRepository,
    private val userPrefsRepository: UserPreferencesRepository,
    private val scope: CoroutineScope,
    private val onMembershipSet: () -> Unit = {}
) : DashboardActions {

    /** The day the dashboard is computed for; the state outlives midnight, so the screen refreshes it on resume. */
    private val today = MutableStateFlow(currentDay())

    private fun currentDay(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    /** Every change in the data asks for this, and reading the time zone is slow on iOS: kept until the next [refreshToday]. */
    private var weekStartOfDay: Pair<LocalDate, Long>? = null

    private fun weekStartMillis(day: LocalDate): Long {
        weekStartOfDay?.takeIf { it.first == day }?.let { return it.second }
        val millis = weekStartMillis(day, firstDayOfWeek(), TimeZone.currentSystemDefault())
        weekStartOfDay = day to millis
        return millis
    }

    fun refreshToday() {
        // Back in the app after a trip: the same day may start at another moment
        weekStartOfDay = null
        today.value = currentDay()
    }

    private data class PreferencesData(
        val goal: Int,
        val expiry: Long?,
        val dynamic: Boolean,
        val palette: Int,
        val swipe: Boolean,
        val floating: Boolean
    )

    private data class SessionData(
        val all: List<WorkoutSessionEntity>,
        val unfinished: List<SessionWithPlanName>,
        val cardioCount: Int,
        val day: LocalDate
    )

    private val _selectedSessionIds = MutableStateFlow<Set<Int>>(emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            data.activePlans(),
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
            data.allSessions(),
            data.unfinishedSessions(),
            today.flatMapLatest { data.cardioSessionCountSince(weekStartMillis(it)) },
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
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    override fun setGymMembershipExpiryDate(timestampMillis: Long?) {
        scope.launch {
            userPrefsRepository.setGymMembershipExpiryDate(timestampMillis)
            if (timestampMillis != null) {
                onMembershipSet()
            }
        }
    }

    override fun deleteSession(sessionId: Int) {
        scope.launch {
            data.deleteSession(sessionId)
        }
    }

    override fun toggleSelection(sessionId: Int) {
        val current = _selectedSessionIds.value.toMutableSet()
        if (current.contains(sessionId)) {
            current.remove(sessionId)
        } else {
            current.add(sessionId)
        }
        _selectedSessionIds.value = current
    }

    override fun clearSelection() {
        _selectedSessionIds.value = emptySet()
    }

    override fun deleteSelectedSessions() {
        scope.launch {
            _selectedSessionIds.value.forEach { sessionId ->
                data.deleteSession(sessionId)
            }
            clearSelection()
        }
    }
}
