package com.emanuel5014.trainable.ui.screens.routines

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.ShareUtils
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val userPrefsRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutinesUiState(isLoading = true))
    val uiState: StateFlow<RoutinesUiState> = _uiState.asStateFlow()

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    init {
        loadPlans()
        viewModelScope.launch {
            localeManager.userSelectedLanguage.collect { _ ->
                _languageCode.value = localeManager.getResolvedLanguage()
            }
        }
    }

    private fun loadPlans() {
        viewModelScope.launch {
            combine(
                workoutRepository.getAllPlansWithDetails(),
                userPrefsRepository.floatingNavBar
            ) { allPlans, floating ->
                allPlans to floating
            }.collect { (allPlans, floating) ->
                // Auto-tag legacy system plans if they exist without the note
                allPlans.forEach { 
                    if (it.plan.note == null && !it.plan.isActive && 
                        (it.plan.nome == "Cardio" || it.plan.nome == "Custom Workout")) {
                        viewModelScope.launch {
                            workoutRepository.updatePlan(it.plan.copy(note = "SYSTEM_PLAN"))
                        }
                    }
                }

                val filteredPlans = allPlans.filter { it.plan.note != "SYSTEM_PLAN" }
                val (active, archived) = filteredPlans.partition { it.plan.isActive }
                _uiState.update { it.copy(
                    plans = active,
                    archivedPlans = archived,
                    floatingNavBar = floating,
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
                val allPlans = _uiState.value.plans + _uiState.value.archivedPlans
                val selected = allPlans.filter { it.plan.id in ids }
                val json = workoutRepository.exportPlans(ids, includeImages)
                ShareUtils.shareWorkoutPlans(context, json, buildExportFileName(selected.map { it.plan.nome }))
                clearSelection()
            }
        }
    }

    private fun buildExportFileName(planNames: List<String>): String {
        if (planNames.isEmpty()) return "routines.trainableplan"
        val base = planNames.first()
            .lowercase()
            .replace(("[^a-z0-9]+").toRegex(), "-")
            .trim('-')
            .take(30)
            .ifBlank { "routines" }
        val suffix = if (planNames.size > 1) "-plus${planNames.size - 1}" else ""
        return "$base$suffix.trainableplan"
    }

    fun deleteSelectedPlans() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedPlanIds
            val allPlans = _uiState.value.plans + _uiState.value.archivedPlans
            val selectedPlans = allPlans.filter { it.plan.id in ids }.map { it.plan }
            selectedPlans.forEach { workoutRepository.deletePlan(it) }
            clearSelection()
        }
    }

    fun archiveSelectedPlans() {
        viewModelScope.launch {
            _uiState.value.selectedPlanIds.forEach { 
                workoutRepository.setPlanActive(it, false)
            }
            clearSelection()
        }
    }

    fun unarchiveSelectedPlans() {
        viewModelScope.launch {
            _uiState.value.selectedPlanIds.forEach { 
                workoutRepository.setPlanActive(it, true)
            }
            clearSelection()
        }
    }

    fun importPlans(jsonData: String) {
        viewModelScope.launch {
            try {
                val imported = workoutRepository.importPlans(jsonData)
                if (imported > 0) {
                    _uiState.update { it.copy(error = null) }
                } else {
                    _uiState.update { it.copy(error = "Import failed: empty or invalid file") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Import failed: ${e.localizedMessage}") }
            }
        }
    }
    
    fun createEmptyPlan(
        name: String,
        note: String? = null,
        giorniSettimana: String? = null,
        dataInizio: Long = System.currentTimeMillis(),
        dataFine: Long? = null
    ) {
        viewModelScope.launch {
            val currentPlans = _uiState.value.plans.map { it.plan } + _uiState.value.archivedPlans.map { it.plan }
            val nextOrder = (currentPlans.maxOfOrNull { it.ordine } ?: -1) + 1
            val newPlan = WorkoutPlanEntity(
                id = 0,
                userId = 1,
                nome = name,
                dataInizio = dataInizio,
                dataFine = dataFine,
                note = note,
                isActive = true,
                ordine = nextOrder,
                giorniSettimana = giorniSettimana
            )
            workoutRepository.savePlan(newPlan)
        }
    }

    fun updatePlan(
        plan: WorkoutPlanEntity,
        name: String,
        note: String?,
        giorniSettimana: String? = null,
        dataInizio: Long = plan.dataInizio,
        dataFine: Long? = plan.dataFine
    ) {
        viewModelScope.launch {
            workoutRepository.updatePlan(
                plan.copy(
                    nome = name,
                    note = note?.takeIf { it.isNotBlank() },
                    giorniSettimana = giorniSettimana,
                    dataInizio = dataInizio,
                    dataFine = dataFine
                )
            )
        }
    }

    fun movePlan(fromIndex: Int, toIndex: Int, isArchivedList: Boolean) {
        viewModelScope.launch {
            val allPlans: List<PlanWithDetails> = workoutRepository.getAllPlansWithDetails().first()
            val filteredPlans = allPlans.filter { it.plan.note != "SYSTEM_PLAN" }
            
            val active = mutableListOf<WorkoutPlanEntity>()
            val archived = mutableListOf<WorkoutPlanEntity>()
            
            for (pWithDetails in filteredPlans) {
                if (pWithDetails.plan.isActive) {
                    active.add(pWithDetails.plan)
                } else {
                    archived.add(pWithDetails.plan)
                }
            }
            
            val targetList = if (isArchivedList) archived else active
            
            if (fromIndex in targetList.indices && toIndex in targetList.indices) {
                val item = targetList.removeAt(fromIndex)
                targetList.add(toIndex, item)
                
                val combinedList = mutableListOf<WorkoutPlanEntity>()
                combinedList.addAll(active)
                combinedList.addAll(archived)
                
                val updates = mutableListOf<WorkoutPlanEntity>()
                for (i in combinedList.indices) {
                    val plan = combinedList[i]
                    updates.add(plan.copy(ordine = i))
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
    val floatingNavBar: Boolean = false,
    val error: String? = null
)
