package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.repository.PlanRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Exports the routines [planIds] to a file named [fileName] and shares it: the share sheet on Android. A platform that cannot
 * share yet passes one that does nothing.
 */
typealias SharePlans = suspend (planIds: List<Int>, includeImages: Boolean, includeMaximums: Boolean, fileName: String) -> Unit

/** The routines list's state, the same on every platform; each one gives it its own lifetime ([scope]). */
class RoutinesState(
    private val planRepository: PlanRepository,
    userPrefsRepository: UserPreferencesRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(RoutinesUiState(isLoading = true))
    val uiState: StateFlow<RoutinesUiState> = _uiState.asStateFlow()

    val swipeActionsEnabled: Flow<Boolean> = userPrefsRepository.swipeActionsEnabled

    init {
        scope.launch {
            combine(
                planRepository.getAllPlansWithDetails(),
                userPrefsRepository.floatingNavBar
            ) { allPlans, floating ->
                allPlans to floating
            }.collect { (allPlans, floating) ->
                // Auto-tag legacy system plans if they exist without the note
                allPlans.forEach {
                    if (it.plan.note == null && !it.plan.isActive &&
                        (it.plan.nome == "Cardio" || it.plan.nome == "Custom Workout")) {
                        scope.launch {
                            planRepository.updatePlan(it.plan.copy(note = "SYSTEM_PLAN"))
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
        scope.launch {
            planRepository.deletePlan(plan)
        }
    }

    fun toggleArchive(plan: WorkoutPlanEntity) {
        scope.launch {
            planRepository.setPlanActive(plan.id, !plan.isActive)
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

    /** What the share dialog should offer for the current selection; null while nothing is being asked. */
    data class ExportPrompt(val hasImages: Boolean, val hasMaximums: Boolean)

    private val _exportPrompt = MutableStateFlow<ExportPrompt?>(null)
    val exportPrompt: StateFlow<ExportPrompt?> = _exportPrompt.asStateFlow()

    /** Shares the selection right away, or first asks about photos / maximums when it has any to include. */
    fun shareSelectedPlans(share: SharePlans) {
        scope.launch {
            val ids = _uiState.value.selectedPlanIds.toList()
            if (ids.isEmpty()) return@launch
            val options = planRepository.exportOptionsFor(ids)
            if (options.hasImages || options.hasMaximums) {
                _exportPrompt.value = ExportPrompt(hasImages = options.hasImages, hasMaximums = options.hasMaximums)
            } else {
                exportSelectedPlans(share, includeImages = false, includeMaximums = false)
            }
        }
    }

    fun dismissExportPrompt() {
        _exportPrompt.value = null
    }

    fun exportSelectedPlans(share: SharePlans, includeImages: Boolean = true, includeMaximums: Boolean = false) {
        _exportPrompt.value = null
        scope.launch {
            val ids = _uiState.value.selectedPlanIds.toList()
            if (ids.isNotEmpty()) {
                val allPlans = _uiState.value.plans + _uiState.value.archivedPlans
                val selected = allPlans.filter { it.plan.id in ids }
                share(ids, includeImages, includeMaximums, buildExportFileName(selected.map { it.plan.nome }))
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
        scope.launch {
            val ids = _uiState.value.selectedPlanIds
            val allPlans = _uiState.value.plans + _uiState.value.archivedPlans
            val selectedPlans = allPlans.filter { it.plan.id in ids }.map { it.plan }
            selectedPlans.forEach { planRepository.deletePlan(it) }
            clearSelection()
        }
    }

    fun archiveSelectedPlans() {
        scope.launch {
            _uiState.value.selectedPlanIds.forEach {
                planRepository.setPlanActive(it, false)
            }
            clearSelection()
        }
    }

    fun unarchiveSelectedPlans() {
        scope.launch {
            _uiState.value.selectedPlanIds.forEach {
                planRepository.setPlanActive(it, true)
            }
            clearSelection()
        }
    }

    fun createEmptyPlan(
        name: String,
        note: String? = null,
        giorniSettimana: String? = null,
        dataInizio: Long = Clock.System.now().toEpochMilliseconds(),
        dataFine: Long? = null
    ) {
        scope.launch {
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
            planRepository.savePlan(newPlan)
        }
    }

    fun movePlan(fromIndex: Int, toIndex: Int, isArchivedList: Boolean) {
        scope.launch {
            val allPlans: List<PlanWithDetails> = planRepository.getAllPlansWithDetails().first()
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
                planRepository.savePlans(updates)
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
