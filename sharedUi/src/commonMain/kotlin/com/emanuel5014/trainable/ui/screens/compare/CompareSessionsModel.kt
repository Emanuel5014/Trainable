package com.emanuel5014.trainable.ui.screens.compare

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompareSessionsUiState(
    val session1: SessionWithDetails? = null,
    val session2: SessionWithDetails? = null,
    val isLoading: Boolean = true,
    val weightUnit: String = "kg",
    val languageCode: String = "en",
    /** Every exercise by id, for the names of the cardio rows. */
    val exercises: Map<Int, ExerciseEntity> = emptyMap(),
    val error: String? = null
)

/**
 * Two workouts side by side, without the platform. [languageCodes] gives the language the exercise names are shown in
 * each time it may have changed. Android's CompareSessionsViewModel and the iOS screen hold one.
 */
class CompareSessionsModel(
    private val workoutRepository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val languageCodes: Flow<String>,
    private val scope: CoroutineScope
) {

    private val _uiState = MutableStateFlow(CompareSessionsUiState())
    val uiState: StateFlow<CompareSessionsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            userPreferencesRepository.weightUnit.collect { unit ->
                _uiState.update { it.copy(weightUnit = unit) }
            }
        }
        scope.launch {
            workoutRepository.getExercisesById().collect { exercises ->
                _uiState.update { it.copy(exercises = exercises) }
            }
        }
        scope.launch {
            languageCodes.collect { code ->
                _uiState.update { it.copy(languageCode = code) }
            }
        }
    }

    fun loadSessions(sessionId1: Int, sessionId2: Int) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                workoutRepository.getSessionWithDetails(sessionId1),
                workoutRepository.getSessionWithDetails(sessionId2)
            ) { session1, session2 ->
                session1 to session2
            }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { (session1, session2) ->
                    _uiState.update {
                        it.copy(
                            session1 = session1,
                            session2 = session2,
                            isLoading = false
                        )
                    }
                }
        }
    }
}
