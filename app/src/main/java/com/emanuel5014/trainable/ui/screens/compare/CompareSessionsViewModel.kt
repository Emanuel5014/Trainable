package com.emanuel5014.trainable.ui.screens.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompareSessionsUiState(
    val session1: SessionWithDetails? = null,
    val session2: SessionWithDetails? = null,
    val isLoading: Boolean = true,
    val weightUnit: String = "kg",
    val languageCode: String = "en",
    val error: String? = null
)

@HiltViewModel
class CompareSessionsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompareSessionsUiState())
    val uiState: StateFlow<CompareSessionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.weightUnit.collect { unit ->
                _uiState.update { it.copy(weightUnit = unit) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.userLanguage.collect { userLang ->
                _uiState.update { it.copy(languageCode = localeManager.resolveLanguageForCompose(userLang)) }
            }
        }
    }

    fun loadSessions(sessionId1: Int, sessionId2: Int) {
        viewModelScope.launch {
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
