package com.emanuel5014.trainable.ui.screens.routines

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.ShareUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
            workoutRepository.getAllPlansWithDetails()
                .collect { allPlans ->
                    val (active, archived) = allPlans.partition { it.plan.isActive }
                    _uiState.update { it.copy(
                        plans = active,
                        archivedPlans = archived,
                        isLoading = false
                    )}
                }
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

    fun toggleArchive(plan: WorkoutPlanEntity) {
        viewModelScope.launch {
            workoutRepository.setPlanActive(plan.id, !plan.isActive)
        }
    }
    
    fun togglePlanSelection(planId: Int) {
        _uiState.update { state ->
            val newSelection = if (state.selectedPlanIds.contains(planId)) {
                state.selectedPlanIds - planId
            } else {
                state.selectedPlanIds + planId
            }
            state.copy(
                selectedPlanIds = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPlanIds = emptySet(), isSelectionMode = false) }
    }

    fun hasImagesInSelection(): Boolean {
        val selectedIds = _uiState.value.selectedPlanIds
        val allPlans = _uiState.value.plans + _uiState.value.archivedPlans
        return allPlans.filter { it.plan.id in selectedIds }.any { 
            it.plan.imageUri != null || it.images.isNotEmpty() 
        }
    }

    fun exportSelectedPlans(context: Context, includeImages: Boolean = true) {
        viewModelScope.launch {
            val ids = _uiState.value.selectedPlanIds.toList()
            if (ids.isNotEmpty()) {
                val json = workoutRepository.exportPlans(ids, includeImages)
                ShareUtils.shareWorkoutPlans(context, json)
                clearSelection()
            }
        }
    }

    fun importPlans(jsonData: String) {
        viewModelScope.launch {
            try {
                workoutRepository.importPlans(jsonData)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Import failed: ${e.localizedMessage}") }
            }
        }
    }
    
    fun createEmptyPlan(name: String, note: String? = null) {
        viewModelScope.launch {
            val currentPlans = _uiState.value.plans.map { it.plan } + _uiState.value.archivedPlans.map { it.plan }
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
                
                val updates = list.mapIndexed { index, it ->
                    it.plan.copy(ordine = index)
                }
                workoutRepository.savePlans(updates)
            }
        }
    }
}

data class RoutinesUiState(
    val isLoading: Boolean = true,
    val plans: List<PlanWithDetails> = emptyList(),
    val archivedPlans: List<PlanWithDetails> = emptyList(),
    val selectedPlanIds: Set<Int> = emptySet(),
    val isSelectionMode: Boolean = false,
    val error: String? = null
)
