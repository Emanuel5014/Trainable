package com.emanuel5014.trainable.ui.screens.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.dao.MergeOutcome
import com.emanuel5014.trainable.data.local.dao.MergePreview
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.repository.ExerciseMergeRepository
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.exercise.ExerciseHistory
import com.emanuel5014.trainable.domain.exercise.ExerciseMergeState
import com.emanuel5014.trainable.domain.exercise.ExerciseMerges
import com.emanuel5014.trainable.domain.exercise.MergeCheck
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the menu of the detail screen offers for an exercise. */
data class ExerciseDetailActions(
    val canMerge: Boolean = false,
    val canRemove: Boolean = false,
    val canRestore: Boolean = false
) {
    companion object {
        val NONE = ExerciseDetailActions()

        /** An exercise merged into another one can only have the merge undone: it is not hidden or restored by itself. */
        fun of(exercise: ExerciseEntity, merges: ExerciseMergeState): ExerciseDetailActions {
            val mergedAway = merges.asLoser != null
            return ExerciseDetailActions(
                canMerge = !mergedAway && !ExerciseMerges.isCardio(exercise),
                canRemove = !exercise.isDeleted && !mergedAway,
                canRestore = exercise.isDeleted && !mergedAway
            )
        }
    }
}

/** Tells the screen what just happened, to show a short confirmation. */
enum class ExerciseDetailMessage { MERGED, UNDONE, HIDDEN, DELETED, RESTORED, CANNOT_MERGE, CANNOT_MERGE_CARDIO, CANNOT_MERGE_WORKOUT }

/**
 * What the exercise detail screen shows. [exerciseId] says which exercise the state is about: the screen shows nothing
 * until it matches the one it asked for, so the history of the previous exercise never flashes.
 */
data class ExerciseDetailUiState(
    val exerciseId: Int? = null,
    val loading: Boolean = true,
    /** The exercise is gone (deleted for good while the screen was opening). */
    val notFound: Boolean = false,
    val exercise: ExerciseEntity? = null,
    val history: ExerciseHistory? = null,
    val weightUnit: String = "kg",
    val languageCode: String = "en",
    val merges: ExerciseMergeState = ExerciseMergeState(),
    val actions: ExerciseDetailActions = ExerciseDetailActions.NONE
)

/** Behind the exercise detail screen: the exercise, its history and merges, and the actions on it (merge, hide, restore). */
@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val mergeRepository: ExerciseMergeRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager
) : ViewModel() {

    private val requestedId = MutableStateFlow<Int?>(null)

    private val weightUnit: Flow<String> = userPreferencesRepository.weightUnit
    private val languageCode: Flow<String> = userPreferencesRepository.userLanguage.map { localeManager.getResolvedLanguage() }

    private val _messages = MutableSharedFlow<ExerciseDetailMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<ExerciseDetailMessage> = _messages.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ExerciseDetailUiState> = requestedId
        .flatMapLatest { id ->
            if (id == null) flowOf(ExerciseDetailUiState())
            else flow {
                emit(ExerciseDetailUiState(exerciseId = id))
                emitAll(
                    exerciseRepository.observeExercise(id).flatMapLatest { exercise ->
                        if (exercise == null) {
                            flowOf(ExerciseDetailUiState(exerciseId = id, loading = false, notFound = true))
                        } else {
                            val history = workoutRepository.getSetHistoryForExercise(id).map { ExerciseHistory.of(it) }.flowOn(Dispatchers.Default)
                            combine(history, weightUnit, languageCode, mergeRepository.observeState(id)) { history, unit, language, merges ->
                                ExerciseDetailUiState(
                                    exerciseId = id, loading = false, exercise = exercise, history = history,
                                    weightUnit = unit, languageCode = language,
                                    merges = merges, actions = ExerciseDetailActions.of(exercise, merges)
                                )
                            }
                        }
                    }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExerciseDetailUiState())

    /** The exercises this one can be merged into: in the library, and passing the merge rules. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val mergeCandidates: StateFlow<List<ExerciseEntity>> = requestedId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else combine(
                exerciseRepository.observeExercise(id),
                exerciseRepository.getAllExercises(),
                mergeRepository.observeMerges()
            ) { open, library, merges ->
                library.filter { ExerciseMerges.check(winner = it, loser = open, merges = merges) == MergeCheck.OK }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(exerciseId: Int) {
        requestedId.value = exerciseId
    }

    /** What merging the open exercise into [winnerId] would move. */
    suspend fun previewMerge(winnerId: Int): MergePreview =
        mergeRepository.preview(loserId = requireNotNull(requestedId.value) { "No exercise is open" }, winnerId = winnerId)

    /** Merges the open exercise into [winnerId]; [onMerged] gets the id to show next (the winner). */
    fun merge(winnerId: Int, onMerged: (Int) -> Unit) {
        val loserId = requestedId.value ?: return
        viewModelScope.launch {
            when (val outcome = mergeRepository.merge(winnerId, loserId)) {
                is MergeOutcome.Merged -> {
                    _messages.tryEmit(ExerciseDetailMessage.MERGED)
                    onMerged(winnerId)
                }
                is MergeOutcome.Rejected -> _messages.tryEmit(
                    when (outcome.check) {
                        MergeCheck.CARDIO_EXERCISE -> ExerciseDetailMessage.CANNOT_MERGE_CARDIO
                        MergeCheck.LOSER_IN_ACTIVE_WORKOUT -> ExerciseDetailMessage.CANNOT_MERGE_WORKOUT
                        else -> ExerciseDetailMessage.CANNOT_MERGE
                    }
                )
            }
        }
    }

    fun undoMerge(mergeId: Int) {
        viewModelScope.launch {
            if (mergeRepository.undo(mergeId)) _messages.tryEmit(ExerciseDetailMessage.UNDONE)
        }
    }

    /** Whether [remove] would delete the open exercise for good (a custom one nothing refers to) rather than hide it. */
    suspend fun willBeDeletedForGood(): Boolean {
        val exercise = state.value.exercise ?: return false
        return exercise.id >= ExerciseEntity.FIRST_CUSTOM_ID && !exerciseRepository.isExerciseInUse(exercise.id)
    }

    /** Takes the open exercise out of the library: hidden when it is a preset or anything refers to it, otherwise deleted. */
    fun remove() {
        val exercise = state.value.exercise ?: return
        viewModelScope.launch {
            exerciseRepository.deleteExercise(exercise)
            val stillThere = exerciseRepository.getExercise(exercise.id) != null
            _messages.tryEmit(if (stillThere) ExerciseDetailMessage.HIDDEN else ExerciseDetailMessage.DELETED)
        }
    }

    fun restore() {
        val exercise = state.value.exercise ?: return
        viewModelScope.launch {
            exerciseRepository.restoreExercise(exercise.id)
            _messages.tryEmit(ExerciseDetailMessage.RESTORED)
        }
    }
}
