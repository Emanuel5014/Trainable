package com.example.gymtracking.ui.screens.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracking.data.local.entity.ExerciseEntity
import com.example.gymtracking.data.local.entity.PlanExerciseEntity
import com.example.gymtracking.data.local.relation.PlanWithDetails
import com.example.gymtracking.data.repository.ExerciseRepository
import com.example.gymtracking.data.repository.WorkoutRepository
import com.example.gymtracking.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutineDetailUiState(
    val isLoading: Boolean = true,
    val planDetails: PlanWithDetails? = null,
    val availableExercises: List<ExerciseEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val error: String? = null,
    val estimatedDurationMinutes: Int = 0,
    val estimatedVolumeKg: Float = 0f
)

@HiltViewModel
class RoutineDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val localeManager: AppLocaleManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val planId: Int = checkNotNull(savedStateHandle["planId"])

    private val _uiState = MutableStateFlow(RoutineDetailUiState(isLoading = true))
    val uiState: StateFlow<RoutineDetailUiState> = _uiState.asStateFlow()

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    init {
        viewModelScope.launch {
            localeManager.userSelectedLanguage.collect { lang ->
                _languageCode.value = lang ?: "en"
            }
        }
        loadPlanDetails()
        loadExercisesCatalog()
    }

    private fun loadPlanDetails() {
        viewModelScope.launch {
            try {
                workoutRepository.getPlanWithDetails(planId).collect { details ->
                    if (details != null) {
                        // Sort exercises by 'ordine'
                        val sortedExercises = details.exercises.sortedBy { it.planExercise.ordine }
                        val sortedDetails = details.copy(exercises = sortedExercises)
                        
                        val duration = calculateEstimatedDuration(sortedDetails)
                        val volume = calculateEstimatedVolume(sortedDetails)
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            planDetails = sortedDetails,
                            estimatedDurationMinutes = duration,
                            estimatedVolumeKg = volume,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Errore sconosciuto"
                )
            }
        }
    }

    private fun calculateEstimatedDuration(details: PlanWithDetails): Int {
        var totalSeconds = 0
        details.exercises.forEach { item ->
            val sets = item.planExercise.serieTarget
            val rest = item.planExercise.recuperoTarget
            // Average of 45s per set + rest interval between sets
            totalSeconds += (sets * 45) + (sets * rest)
        }
        return totalSeconds / 60
    }

    private fun calculateEstimatedVolume(details: PlanWithDetails): Float {
        // Here we mock a weight of 60kg if we don't have PR data yet
        // and take the min of the reps range (e.g. '8-12' -> 8)
        var totalVol = 0f
        details.exercises.forEach { item ->
            val sets = item.planExercise.serieTarget
            val reps = item.planExercise.repsTarget.takeWhile { it.isDigit() }.toIntOrNull() ?: 8
            totalVol += sets * reps * 60f 
        }
        return totalVol
    }

    private fun loadExercisesCatalog() {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _uiState.value = _uiState.value.copy(availableExercises = exercises)
            }
        }
        viewModelScope.launch {
            exerciseRepository.getCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
    }

    fun addCustomExercise(nome: String, categoria: String) {
        viewModelScope.launch {
            exerciseRepository.addCustomExercise(nome, categoria)
        }
    }

    fun updateCustomExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            exerciseRepository.updateExercise(exercise)
        }
    }

    fun deleteCustomExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            exerciseRepository.deleteExercise(exercise)
        }
    }

    fun addExercise(exerciseId: Int, serieTarget: Int, repsTarget: String, recuperoTarget: Int) {
        viewModelScope.launch {
            val current = _uiState.value.planDetails ?: return@launch
            val nextOrder = (current.exercises.maxOfOrNull { it.planExercise.ordine } ?: -1) + 1

            workoutRepository.savePlanExercise(
                PlanExerciseEntity(
                    planId = current.plan.id,
                    exerciseId = exerciseId,
                    serieTarget = serieTarget,
                    repsTarget = repsTarget,
                    recuperoTarget = recuperoTarget,
                    ordine = nextOrder
                )
            )
        }
    }

    fun updateExercise(
        original: PlanExerciseEntity,
        exerciseId: Int,
        serieTarget: Int,
        repsTarget: String,
        recuperoTarget: Int
    ) {
        viewModelScope.launch {
            workoutRepository.updatePlanExercise(
                original.copy(
                    exerciseId = exerciseId,
                    serieTarget = serieTarget,
                    repsTarget = repsTarget,
                    recuperoTarget = recuperoTarget
                )
            )
        }
    }

    fun removeExercise(planExercise: PlanExerciseEntity) {
        viewModelScope.launch {
            // Delete first
            workoutRepository.deletePlanExercise(planExercise)
            
            // Immediately re-calculate order from current state but excluding the deleted one
            // to avoid waiting for the Flow to emit a new state which might be too slow
            val currentDetails = _uiState.value.planDetails ?: return@launch
            val remaining = currentDetails.exercises
                .filter { it.planExercise.id != planExercise.id }
                .sortedBy { it.planExercise.ordine }
            
            val updates = remaining.mapIndexed { index, item ->
                item.planExercise.copy(ordine = index)
            }
            workoutRepository.savePlanExercises(updates)
        }
    }

    private fun renormalizeOrder() {
        viewModelScope.launch {
            val details = _uiState.value.planDetails ?: return@launch
            val sorted = details.exercises.sortedBy { it.planExercise.ordine }
            val updates = sorted.mapIndexed { index, item ->
                item.planExercise.copy(ordine = index)
            }
            workoutRepository.savePlanExercises(updates)
        }
    }

    fun moveExercise(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val details = _uiState.value.planDetails ?: return@launch
            val exercises = details.exercises.toMutableList()
            if (fromIndex in exercises.indices && toIndex in exercises.indices) {
                val item = exercises.removeAt(fromIndex)
                exercises.add(toIndex, item)
                
                val updates = exercises.mapIndexed { index, it ->
                    it.planExercise.copy(ordine = index)
                }
                workoutRepository.savePlanExercises(updates)
            }
        }
    }

    fun deletePlan(onSuccess: () -> Unit) {
        viewModelScope.launch {
            uiState.value.planDetails?.plan?.let { plan ->
                workoutRepository.deletePlan(plan)
                onSuccess()
            }
        }
    }

    fun updatePlanImage(imageUri: String?) {
        viewModelScope.launch {
            uiState.value.planDetails?.plan?.let { plan ->
                workoutRepository.updatePlan(plan.copy(imageUri = imageUri))
            }
        }
    }

    fun updatePlan(nome: String, note: String?) {
        viewModelScope.launch {
            uiState.value.planDetails?.plan?.let { plan ->
                workoutRepository.updatePlan(plan.copy(nome = nome, note = note))
            }
        }
    }
}
