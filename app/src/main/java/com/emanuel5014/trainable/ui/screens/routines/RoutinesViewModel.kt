package com.emanuel5014.trainable.ui.screens.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.PlanRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The shared [RoutinesState] kept for the screen's lifetime, plus what only Android has: the export file and the language. */
@HiltViewModel
class RoutinesViewModel @Inject constructor(
    planRepository: PlanRepository,
    private val workoutRepository: WorkoutRepository,
    userPrefsRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager
) : ViewModel() {

    val state = RoutinesState(planRepository, userPrefsRepository, viewModelScope)

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    init {
        viewModelScope.launch {
            localeManager.userSelectedLanguage.collect { _ ->
                _languageCode.value = localeManager.getResolvedLanguage()
            }
        }
    }

    suspend fun exportPlans(planIds: List<Int>, includeImages: Boolean, includeMaximums: Boolean): String =
        workoutRepository.exportPlans(planIds, includeImages, includeMaximums)
}
