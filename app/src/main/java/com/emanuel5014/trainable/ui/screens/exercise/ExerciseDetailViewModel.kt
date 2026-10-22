package com.emanuel5014.trainable.ui.screens.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.exercise.ExerciseHistory
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

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
    val languageCode: String = "en"
)

/** Behind the exercise detail screen: the exercise, its history and the unit and language to show them in. Reads only. */
@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager
) : ViewModel() {

    private val requestedId = MutableStateFlow<Int?>(null)

    private val weightUnit: Flow<String> = userPreferencesRepository.weightUnit
    private val languageCode: Flow<String> = userPreferencesRepository.userLanguage.map { localeManager.getResolvedLanguage() }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ExerciseDetailUiState> = requestedId
        .flatMapLatest { id ->
            if (id == null) flowOf(ExerciseDetailUiState())
            else flow {
                emit(ExerciseDetailUiState(exerciseId = id))
                val exercise = exerciseRepository.getExercise(id)
                if (exercise == null) {
                    emit(ExerciseDetailUiState(exerciseId = id, loading = false, notFound = true))
                } else {
                    val history = workoutRepository.getSetHistoryForExercise(id).map { ExerciseHistory.of(it) }.flowOn(Dispatchers.Default)
                    emitAll(
                        combine(history, weightUnit, languageCode) { history, unit, language ->
                            ExerciseDetailUiState(
                                exerciseId = id, loading = false, exercise = exercise, history = history,
                                weightUnit = unit, languageCode = language
                            )
                        }
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExerciseDetailUiState())

    fun load(exerciseId: Int) {
        requestedId.value = exerciseId
    }
}
