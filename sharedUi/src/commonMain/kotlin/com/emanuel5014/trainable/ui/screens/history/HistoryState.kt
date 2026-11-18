package com.emanuel5014.trainable.ui.screens.history

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.time.Clock
import com.emanuel5014.trainable.data.local.entity.RoutineGroupEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.local.relation.TagWithUsage
import com.emanuel5014.trainable.data.repository.JournalRepository
import com.emanuel5014.trainable.domain.exercise.SuggestedExercise
import com.emanuel5014.trainable.domain.journal.JournalFilters
import com.emanuel5014.trainable.domain.journal.SessionJournal
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.emanuel5014.trainable.data.local.entity.SessionHeartDataEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.repository.HeartRateRepository
import com.emanuel5014.trainable.data.repository.HeartSyncResult
import com.emanuel5014.trainable.domain.heart.HeartMath
import com.emanuel5014.trainable.ui.components.heart.ShareHeart
import com.emanuel5014.trainable.ui.components.heart.toShareHeart
import com.emanuel5014.trainable.domain.heart.isSparse


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
    /** How many workouts each choice of the filter sheet would show. */
    val filterCounts: HistoryFilterCounts = HistoryFilterCounts(),
    /** The groups of routines, for the routine filter. */
    val routineGroups: List<RoutineGroupEntity> = emptyList(),
    /** The workout journal is on: its filters apply and its tags, rating and notes are shown. */
    val journalEnabled: Boolean = false,
    /** With the journal on: workouts can be rated, so the rating filter shows. */
    val journalRatingEnabled: Boolean = true,
    /** Every tag, to pick from in the filter sheet and to name in the active-filters bar. */
    val availableTags: List<TagWithUsage> = emptyList(),
    /** Every exercise by id (hidden ones too), for the names of the cardio rows. */
    val exercises: Map<Int, ExerciseEntity> = emptyMap(),
    /** Heart rate and calories of the watch or band are read into each workout (Health Connect). */
    val heartRateEnabled: Boolean = false,
    /** For the heart-rate zones. */
    val maxHeartRate: Int = UserPreferencesRepository.DEFAULT_MAX_HEART_RATE,
    /** The share card shows the workout's heart rate (the switch in the share preview). */
    val shareIncludeHeart: Boolean = true
)

/** The history's state, the same on every platform; each one gives it its own lifetime ([scope]). */
class HistoryState(
    private val repository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val journalRepository: JournalRepository,
    private val scope: CoroutineScope,
    /** Heart rate of each workout; null leaves it out. */
    private val heartRateRepository: HeartRateRepository? = null
) {

    val swipeActionsEnabled = userPreferencesRepository.swipeActionsEnabled

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Int>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val _compareNavigationEvent = MutableSharedFlow<Pair<Int, Int>>()
    val compareNavigationEvent = _compareNavigationEvent.asSharedFlow()

    init {
        loadHistory()
        loadPlans()
        scope.launch {
            userPreferencesRepository.weightUnit.collect { unit ->
                _uiState.update { it.copy(weightUnit = unit) }
            }
        }
        scope.launch {
            userPreferencesRepository.journalEnabled.collect { enabled ->
                _uiState.update { it.copy(journalEnabled = enabled) }
                // Switching the journal off or on changes which filters count
                applyFilters()
            }
        }
        scope.launch {
            userPreferencesRepository.journalRatingEnabled.collect { enabled ->
                _uiState.update { it.copy(journalRatingEnabled = enabled) }
            }
        }
        scope.launch {
            userPreferencesRepository.heartRateEnabled.collect { enabled ->
                _uiState.update { it.copy(heartRateEnabled = enabled) }
            }
        }
        scope.launch {
            userPreferencesRepository.maxHeartRate.collect { bpm ->
                _uiState.update { it.copy(maxHeartRate = bpm) }
            }
        }
        scope.launch {
            userPreferencesRepository.shareIncludeHeart.collect { include ->
                _uiState.update { it.copy(shareIncludeHeart = include) }
            }
        }
        scope.launch {
            repository.getExercisesById().collect { exercises ->
                _uiState.update { it.copy(exercises = exercises) }
            }
        }
        scope.launch {
            journalRepository.tags.collect { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
        }
    }

    /** Whether workouts can read their heart rate on this platform (saved data shows either way). */
    val canSyncHeart: Boolean get() = heartRateRepository?.canSync == true

    fun heartData(sessionId: Int): Flow<SessionHeartDataEntity?> = heartRateRepository?.observe(sessionId) ?: flowOf(null)

    /**
     * Reads [session]'s heart rate from the health store and saves it. Without [force] nothing is read when data
     * for the workout's current span is already saved.
     */
    suspend fun syncHeart(session: WorkoutSessionEntity, force: Boolean): HeartSyncResult {
        val repository = heartRateRepository ?: return HeartSyncResult.UNAVAILABLE
        if (!force) {
            val saved = repository.saved(session.id)
            // A row without heart rate (an earlier version kept the phone's own calories) is read again
            // A sparse curve (a reading every few minutes) is read again too: the per-second series of the band's
            // workout may have reached Health Connect since
            if (saved != null && saved.avgBpm != null && !repository.isStale(saved, session) && !HeartMath.isSparse(saved)) {
                return HeartSyncResult.SAVED
            }
        }
        return runCatching { repository.sync(session, typicalDurationMs()) }.getOrDefault(HeartSyncResult.UNAVAILABLE)
    }

    /**
     * The heart rate the share card of [sessionId] may show: what the history card shows (Android follows the
     * heart-rate switch; a platform without a store shows restored data), null without heart rate.
     */
    suspend fun shareHeart(sessionId: Int): ShareHeart? {
        val state = _uiState.value
        if (!state.heartRateEnabled && canSyncHeart) return null
        return heartData(sessionId).first()?.toShareHeart(state.maxHeartRate)
    }

    fun setShareIncludeHeart(include: Boolean) {
        scope.launch { userPreferencesRepository.setShareIncludeHeart(include) }
    }

    /** How long the lifter's workouts with a recorded duration usually last (the median), for those without one. */
    private fun typicalDurationMs(): Long? {
        val durations = _uiState.value.sessions.mapNotNull { it.session.durationMs?.takeIf { ms -> ms in 10 * 60_000L..4 * 3_600_000L } }.sorted()
        return durations.getOrNull(durations.size / 2)
    }

    private fun loadPlans() {
        scope.launch {
            repository.getRoutineGroups().collect { groups ->
                _uiState.update { it.copy(routineGroups = groups) }
                // A group that was dissolved can no longer be filtered on
                val groupId = _uiState.value.filters.groupId
                if (groupId != null && groups.none { it.id == groupId }) setFilters(_uiState.value.filters.copy(groupId = null))
            }
        }
        scope.launch {
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
        scope.launch {
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
        _uiState.update {
            it.copy(
                filteredSessions = state.filters.apply(state.sessions, state.journalEnabled),
                filterCounts = historyFilterCounts(state.sessions, state.filters, state.journalEnabled)
            )
        }
    }

    /** The collection of the opened workout's details; only one runs, so an earlier workout cannot overwrite it. */
    private var detailsJob: Job? = null

    fun loadSessionDetails(sessionId: Int) {
        detailsJob?.cancel()
        detailsJob = scope.launch {
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
        scope.launch { exerciseRepository.restoreExercise(exerciseId) }
    }

    /** Replaces the rating, note and tags of a workout (the card follows through the history flow). */
    fun saveJournal(sessionId: Int, journal: SessionJournal) {
        scope.launch { journalRepository.saveJournal(sessionId, journal) }
    }

    fun deleteSession(sessionId: Int) {
        scope.launch {
            repository.deleteSession(sessionId)
        }
    }

    suspend fun loadPlanExercises(planId: Int): List<PlanExerciseWithDetails>? {
        return repository.getPlanWithDetails(planId).first()?.exercises
    }

    fun addManualWorkout(planId: Int) {
        scope.launch {
            val sessionId = repository.createManualSessionFromPlan(planId, Clock.System.now().toEpochMilliseconds())
            if (sessionId != -1L) {
                _navigationEvent.emit(sessionId.toInt())
            }
        }
    }

    fun addEmptyWorkout() {
        scope.launch {
            val sessionId = repository.createEmptyManualSession(Clock.System.now().toEpochMilliseconds())
            if (sessionId != -1L) {
                _navigationEvent.emit(sessionId.toInt())
            }
        }
    }

    /** A cardio workout on its own, of [exercise], done at [timestamp] (the moment chosen in the entry sheet). */
    fun saveCardioWorkout(exercise: ExerciseEntity, distanceKm: Float, seconds: Int, timestamp: Long, note: String? = null) {
        scope.launch {
            repository.saveCardioSession(exercise.id, exercise.nome, distanceKm, seconds, timestamp, note)
        }
    }

    /** The "Suggested" section of the cardio picker. */
    suspend fun cardioSuggestions(): List<SuggestedExercise> =
        exerciseRepository.cardioSuggestions(repository.recentCardioExercises())

    /** Whether the cardio picker refuses [name]: a hidden exercise that is not a cardio carries it. */
    suspend fun isNameOfHiddenNonCardio(name: String): Boolean = exerciseRepository.isNameOfHiddenNonCardio(name)

    /** A custom exercise made from the picker; [onCreated] gets it once it is in the library. */
    fun addCustomExercise(name: String, category: String, onCreated: (ExerciseEntity) -> Unit) {
        scope.launch {
            val id = exerciseRepository.addCustomExercise(name, category)
            // A hidden exercise of that name comes back as it was stored, so it is read again
            onCreated(exerciseRepository.getExercise(id) ?: ExerciseEntity(id = id, nome = name, categoria = category))
        }
    }

    fun updateSet(set: SetLogEntity) {
        scope.launch {
            repository.updateSet(set)
        }
    }

    fun deleteSet(set: SetLogEntity) {
        scope.launch {
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
        scope.launch {
            _uiState.value.selectedSessionIds.forEach { 
                repository.deleteSession(it)
            }
            clearSelection()
        }
    }

    fun compareSelectedSessions() {
        scope.launch {
            val selectedIds = _uiState.value.selectedSessionIds.toList()
            if (selectedIds.size == 2) {
                _compareNavigationEvent.emit(selectedIds[0] to selectedIds[1])
            }
        }
    }
}
