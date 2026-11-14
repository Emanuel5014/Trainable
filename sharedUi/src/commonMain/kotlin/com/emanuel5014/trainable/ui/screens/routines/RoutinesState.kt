package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.data.local.entity.RoutineGroupEntity
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
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
    private val userPrefsRepository: UserPreferencesRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(RoutinesUiState(isLoading = true))
    val uiState: StateFlow<RoutinesUiState> = _uiState.asStateFlow()

    val swipeActionsEnabled: Flow<Boolean> = userPrefsRepository.swipeActionsEnabled

    /**
     * The "your week" card folded into one line; remembered across launches and in backups. Kept warm in [scope] and
     * null until DataStore answers, so the screen never draws the card open and then folds it.
     */
    val weekCollapsed: StateFlow<Boolean?> =
        userPrefsRepository.routinesWeekCollapsed.stateIn(scope, SharingStarted.Eagerly, null)

    fun setWeekCollapsed(collapsed: Boolean) {
        scope.launch { userPrefsRepository.setRoutinesWeekCollapsed(collapsed) }
    }

    init {
        scope.launch {
            combine(
                planRepository.getAllPlansWithDetails(),
                userPrefsRepository.floatingNavBar,
                planRepository.getRoutineGroups(),
                planRepository.getLastSessionTimes()
            ) { allPlans, floating, groups, lastSessions ->
                PlansSnapshot(allPlans, floating, groups, lastSessions.associate { it.planId to it.lastTimestamp })
            }.collect { (allPlans, floating, groups, lastSessions) ->
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
                    groups = groups,
                    lastSessionTimes = lastSessions,
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

    // Routine groups

    /** Puts the selected routines in a new group named [name] and leaves selection mode. */
    fun groupSelectedPlans(name: String, colorRole: Int, customColor: Int?) {
        val ids = _uiState.value.selectedPlanIds.toList()
        val cleanName = name.trim()
        if (ids.isEmpty() || cleanName.isEmpty()) return
        scope.launch {
            planRepository.createRoutineGroup(cleanName, colorRole, customColor, orderedSelection(ids))
            clearSelection()
        }
    }

    /** Moves the selected routines into the group [groupId], or out of their groups when it is null. */
    fun moveSelectedPlansToGroup(groupId: Int?) {
        val ids = _uiState.value.selectedPlanIds.toList()
        if (ids.isEmpty()) return
        scope.launch {
            planRepository.moveRoutinesToGroup(ids, groupId)
            clearSelection()
        }
    }

    fun renameGroup(group: RoutineGroupEntity, name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        scope.launch { planRepository.updateRoutineGroup(group.copy(name = cleanName)) }
    }

    /** A theme colour ([customColor] null) or a colour picked freely. */
    fun setGroupColor(group: RoutineGroupEntity, colorRole: Int, customColor: Int?) {
        scope.launch { planRepository.updateRoutineGroup(group.copy(colorRole = colorRole, customColor = customColor)) }
    }

    /** Swaps two groups in the list order (a group moving up or down next to its neighbour). */
    fun swapGroups(group: RoutineGroupEntity, other: RoutineGroupEntity) {
        val groups = _uiState.value.groups
        val swapped = swapInOrder(groups.map { it.id }, group.id, other.id)
        val byId = groups.associateBy { it.id }
        scope.launch { planRepository.reorderRoutineGroups(swapped.map { byId.getValue(it) }) }
    }

    /** Archives every routine of the group (they stay together in Archived), or brings them all back. */
    fun setGroupArchived(group: RoutineGroupEntity, archived: Boolean) {
        scope.launch { planRepository.setRoutineGroupActive(group.id, !archived) }
    }

    /** Removes the group; its routines stay, in no group. */
    fun dissolveGroup(group: RoutineGroupEntity) {
        scope.launch { planRepository.dissolveRoutineGroup(group.id) }
    }

    /** Swaps two routines in the order (a routine moving up or down next to its neighbour in a group). */
    fun swapPlans(planId: Int, otherPlanId: Int) {
        scope.launch {
            val ordered = orderedRoutines()
            val swapped = swapInOrder(ordered.map { it.id }, planId, otherPlanId)
            if (swapped == ordered.map { it.id }) return@launch
            val byId = ordered.associateBy { it.id }
            planRepository.savePlans(swapped.mapIndexed { index, id -> byId.getValue(id).copy(ordine = index) })
        }
    }

    /** Every routine (system plans aside), active ones first, each part in its order: the order [movePlan] saves. */
    private suspend fun orderedRoutines(): List<WorkoutPlanEntity> {
        val plans = planRepository.getAllPlansWithDetails().first().map { it.plan }.filter { it.note != "SYSTEM_PLAN" }
        val (active, archived) = plans.partition { it.isActive }
        return active + archived
    }

    private fun orderedSelection(ids: List<Int>): List<Int> {
        val all = _uiState.value.plans + _uiState.value.archivedPlans
        return all.map { it.plan.id }.filter { it in ids }
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
    /** The routine groups in their order. */
    val groups: List<RoutineGroupEntity> = emptyList(),
    /** When each routine was last trained (plan id → time of its last finished workout). */
    val lastSessionTimes: Map<Int, Long> = emptyMap(),
    val selectedPlanIds: Set<Int> = emptySet(),
    val isSelectionMode: Boolean = false,
    val floatingNavBar: Boolean = false,
    val error: String? = null
)

private data class PlansSnapshot(
    val plans: List<PlanWithDetails>,
    val floating: Boolean,
    val groups: List<RoutineGroupEntity>,
    val lastSessions: Map<Int, Long>
)
