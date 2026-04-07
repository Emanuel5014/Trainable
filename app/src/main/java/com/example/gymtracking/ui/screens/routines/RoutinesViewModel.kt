package com.example.gymtracking.ui.screens.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracking.data.local.entity.WorkoutPlanEntity
import com.example.gymtracking.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutinesUiState(isLoading = true))
    val uiState: StateFlow<RoutinesUiState> = _uiState.asStateFlow()

    init {
        loadPlans()
    }

    private fun loadPlans() {
        viewModelScope.launch {
            workoutRepository.getActivePlans()
                .onEach { active ->
                    _uiState.update { it.copy(plans = active, isLoading = false) }
                }.collect()
        }
        viewModelScope.launch {
            workoutRepository.getExpiredPlans()
                .onEach { archived ->
                    _uiState.update { it.copy(archivedPlans = archived, isLoading = false) }
                }.collect()
        }
    }

    fun deletePlan(plan: WorkoutPlanEntity) {
        viewModelScope.launch {
            workoutRepository.deletePlan(plan)
        }
    }
    
    fun archivePlan(plan: WorkoutPlanEntity) {
        viewModelScope.launch {
            workoutRepository.setPlanActive(plan.id, false)
        }
    }

    fun unarchivePlan(plan: WorkoutPlanEntity) {
        viewModelScope.launch {
            workoutRepository.setPlanActive(plan.id, true)
        }
    }
    
    fun createEmptyPlan(name: String, note: String? = null) {
        viewModelScope.launch {
            val currentPlans = _uiState.value.plans + _uiState.value.archivedPlans
            val nextOrder = (currentPlans.maxOfOrNull { it.ordine } ?: -1) + 1
            val newPlan = WorkoutPlanEntity(
                id = 0,
                userId = 1,
                nome = name,
                dataInizio = System.currentTimeMillis(),
                note = note,
                isActive = true,
                ordine = nextOrder
            )
            workoutRepository.savePlan(newPlan)
        }
    }

    fun updatePlan(plan: WorkoutPlanEntity, name: String, note: String?) {
        viewModelScope.launch {
            workoutRepository.updatePlan(
                plan.copy(
                    nome = name,
                    note = note?.takeIf { it.isNotBlank() }
                )
            )
        }
    }

    fun movePlan(fromIndex: Int, toIndex: Int, isArchivedList: Boolean) {
        viewModelScope.launch {
            val list = (if (isArchivedList) _uiState.value.archivedPlans else _uiState.value.plans).toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                
                // Re-calculate order for the entire combined set to be safe
                // but for simplicity we update the current list's order relative to itself
                // To keep order consistent between active/archived, we might need a more global order
                // For now, let's just update the list being dragged.
                val updates = list.mapIndexed { index, it ->
                    it.copy(ordine = index)
                }
                workoutRepository.savePlans(updates)
            }
        }
    }
}

data class RoutinesUiState(
    val isLoading: Boolean = true,
    val plans: List<WorkoutPlanEntity> = emptyList(),
    val archivedPlans: List<WorkoutPlanEntity> = emptyList(),
    val error: String? = null
)
