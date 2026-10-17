package com.emanuel5014.trainable.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.local.relation.TagWithUsage
import com.emanuel5014.trainable.data.repository.JournalRepository
import com.emanuel5014.trainable.domain.journal.JournalFilters
import com.emanuel5014.trainable.domain.journal.SessionJournal
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Value of [HistoryFilters.planId] that keeps the workouts without a routine of their own: cardio,
 * custom and quick workouts, and those of routines that were deleted. Plan ids start at 1, so it never
 * collides with a real one.
 */
const val NO_ROUTINE_PLAN_ID = -1

data class HistoryUiState(
    val sessions: List<SessionWithDetails> = emptyList(),
    val filteredSessions: List<SessionWithDetails> = emptyList(),
    val availablePlans: List<WorkoutPlanEntity> = emptyList(),
    /** What each routine in [availablePlans] contains, by plan id. */
    val planSummaries: Map<Int, PlanSummary> = emptyMap(),
    val selectedSession: SessionWithDetails? = null,
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedSessionIds: Set<Int> = emptySet(),
    val error: String? = null,
    val weightUnit: String = "kg",
    /** What [filteredSessions] is narrowed to. */
    val filters: HistoryFilters = HistoryFilters(),
    /** The workout journal is on: its filters apply and its tags, rating and notes are shown. */
    val journalEnabled: Boolean = false,
    /** With the journal on: workouts can be rated, so the rating filter shows. */
    val journalRatingEnabled: Boolean = true,
    /** Every tag, to pick from in the filter sheet and to name in the active-filters bar. */
    val availableTags: List<TagWithUsage> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager,
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Int>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val _compareNavigationEvent = MutableSharedFlow<Pair<Int, Int>>()
    val compareNavigationEvent = _compareNavigationEvent.asSharedFlow()

    init {
        loadHistory()
        loadPlans()
        viewModelScope.launch {
            userPreferencesRepository.weightUnit.collect { unit ->
                _uiState.update { it.copy(weightUnit = unit) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.userLanguage.collect { userLang ->
                _languageCode.value = localeManager.resolveLanguageForCompose(userLang)
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.journalEnabled.collect { enabled ->
                _uiState.update { it.copy(journalEnabled = enabled) }
                // Switching the journal off or on changes which filters count
                applyFilters()
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.journalRatingEnabled.collect { enabled ->
                _uiState.update { it.copy(journalRatingEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            journalRepository.tags.collect { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
        }
    }

    private fun loadPlans() {
        viewModelScope.launch {
            repository.getAllPlansWithDetails().collect { plans ->
                // Routines only: the plans the app makes for cardio / quick / custom workouts are not listed
                val routines = plans.filter { !it.plan.isSystemPlan }
                val summaries = routines.associate { details ->
                    details.plan.id to PlanSummary(
                        exerciseCount = details.exercises.size,
                        muscleGroups = details.exercises
                            .sortedBy { it.planExercise.ordine }
                            .map { it.exercise.categoria }
                            .filter { it.isNotBlank() }
                            .distinct()
                    )
                }
                _uiState.update { it.copy(availablePlans = routines.map { details -> details.plan }, planSummaries = summaries) }
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllSessionsWithDetails()
                .onEach { sessions ->
                    _uiState.update { it.copy(sessions = sessions, isLoading = false) }
                    applyFilters()
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect()
        }
    }

    fun setFilters(filters: HistoryFilters) {
        _uiState.update { it.copy(filters = filters) }
        applyFilters()
    }

    /** Narrows the history to the workouts with this one tag (what tapping a tag on a card does); routine and dates stay. */
    fun filterByTag(tagId: Int) {
        setFilters(_uiState.value.filters.copy(journal = JournalFilters(tagIds = setOf(tagId))))
    }

    private fun applyFilters() {
        val state = _uiState.value
        _uiState.update { it.copy(filteredSessions = state.filters.apply(state.sessions, state.journalEnabled)) }
    }

    fun loadSessionDetails(sessionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getSessionWithDetails(sessionId)
                .onEach { session ->
                    _uiState.update { it.copy(selectedSession = session, isLoading = false) }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect()
        }
    }

    /** Adds an exercise the user deleted from the exercise list back, from a workout that still has it. */
    fun restoreExercise(exerciseId: Int) {
        viewModelScope.launch { exerciseRepository.restoreExercise(exerciseId) }
    }

    /** Replaces the rating, note and tags of a workout (the card follows through the history flow). */
    fun saveJournal(sessionId: Int, journal: SessionJournal) {
        viewModelScope.launch { journalRepository.saveJournal(sessionId, journal) }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    suspend fun loadPlanExercises(planId: Int): List<PlanExerciseWithDetails>? {
        return repository.getPlanWithDetails(planId).first()?.exercises
    }

    fun addManualWorkout(planId: Int) {
        viewModelScope.launch {
            val sessionId = repository.createManualSessionFromPlan(planId, System.currentTimeMillis())
            if (sessionId != -1L) {
                _navigationEvent.emit(sessionId.toInt())
            }
        }
    }

    fun addEmptyWorkout() {
        viewModelScope.launch {
            val sessionId = repository.createEmptyManualSession(System.currentTimeMillis())
            if (sessionId != -1L) {
                _navigationEvent.emit(sessionId.toInt())
            }
        }
    }

    fun saveCardioWorkout(categoria: String, distanza: Float, durataSecondi: Int, note: String? = null) {
        viewModelScope.launch {
            repository.saveCardioSession(categoria, distanza, durataSecondi, System.currentTimeMillis(), note)
        }
    }

    fun updateSet(set: SetLogEntity) {
        viewModelScope.launch {
            repository.updateSet(set)
        }
    }

    fun deleteSet(set: SetLogEntity) {
        viewModelScope.launch {
            repository.deleteSet(set)
        }
    }

    fun toggleSessionSelection(sessionId: Int) {
        _uiState.update { state ->
            val newSelection = if (state.selectedSessionIds.contains(sessionId)) {
                state.selectedSessionIds - sessionId
            } else {
                state.selectedSessionIds + sessionId
            }
            state.copy(
                selectedSessionIds = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedSessionIds = emptySet(), isSelectionMode = false) }
    }

    fun deleteSelectedSessions() {
        viewModelScope.launch {
            _uiState.value.selectedSessionIds.forEach { 
                repository.deleteSession(it)
            }
            clearSelection()
        }
    }

    fun compareSelectedSessions() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedSessionIds.toList()
            if (selectedIds.size == 2) {
                _compareNavigationEvent.emit(selectedIds[0] to selectedIds[1])
            }
        }
    }
}
