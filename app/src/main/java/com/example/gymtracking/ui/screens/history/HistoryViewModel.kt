package com.example.gymtracking.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracking.data.local.entity.SetLogEntity
import com.example.gymtracking.data.local.entity.WorkoutSessionEntity
import com.example.gymtracking.data.local.relation.SessionWithDetails
import com.example.gymtracking.data.repository.WorkoutRepository
import com.example.gymtracking.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<SessionWithDetails> = emptyList(),
    val selectedSession: SessionWithDetails? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val weightUnit: String = "kg"
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
        viewModelScope.launch {
            userPreferencesRepository.weightUnit.collect { unit ->
                _uiState.update { it.copy(weightUnit = unit) }
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllSessionsWithDetails()
                .onEach { sessions ->
                    _uiState.update { it.copy(sessions = sessions, isLoading = false) }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect()
        }
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

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
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
}
