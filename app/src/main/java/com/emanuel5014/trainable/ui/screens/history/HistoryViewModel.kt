package com.emanuel5014.trainable.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<SessionWithDetails> = emptyList(),
    val filteredSessions: List<SessionWithDetails> = emptyList(),
    val availablePlans: List<WorkoutPlanEntity> = emptyList(),
    val selectedSession: SessionWithDetails? = null,
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedSessionIds: Set<Int> = emptySet(),
    val error: String? = null,
    val weightUnit: String = "kg",
    val selectedPlanId: Int? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Int>()
    val navigationEvent = _navigationEvent.asSharedFlow()

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
    }

    private fun loadPlans() {
        viewModelScope.launch {
            repository.getAllPlans().collect { plans ->
                _uiState.update { it.copy(availablePlans = plans) }
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

    fun setFilters(planId: Int?, startDate: Long?, endDate: Long?) {
        _uiState.update { it.copy(selectedPlanId = planId, startDate = startDate, endDate = endDate) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = state.sessions.filter { sessionDetails ->
            val matchesPlan = state.selectedPlanId == null || sessionDetails.session.planId == state.selectedPlanId
            val matchesDate = (state.startDate == null || sessionDetails.session.timestamp >= state.startDate) &&
                    (state.endDate == null || sessionDetails.session.timestamp <= (state.endDate + 86399999)) // Include the whole end day
            matchesPlan && matchesDate
        }
        _uiState.update { it.copy(filteredSessions = filtered) }
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

    fun addManualWorkout(planId: Int) {
        viewModelScope.launch {
            val sessionId = repository.createManualSessionFromPlan(planId, System.currentTimeMillis())
            if (sessionId != -1L) {
                _navigationEvent.emit(sessionId.toInt())
            }
        }
    }

    fun saveCardioWorkout(categoria: String, distanza: Float, durataSecondi: Int) {
        viewModelScope.launch {
            repository.saveCardioSession(categoria, distanza, durataSecondi, System.currentTimeMillis())
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
}
