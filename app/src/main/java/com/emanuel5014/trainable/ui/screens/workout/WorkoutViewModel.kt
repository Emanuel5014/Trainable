package com.emanuel5014.trainable.ui.screens.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.JournalRepository
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import com.emanuel5014.trainable.util.notification.AndroidWorkoutTimers
import com.emanuel5014.trainable.util.notification.TimerNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    workoutRepository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    exerciseRepository: ExerciseRepository,
    private val timerNotificationHelper: TimerNotificationHelper,
    localeManager: AppLocaleManager,
    oneRepMaxRepository: OneRepMaxRepository,
    journalRepository: JournalRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val model = WorkoutModel(
        workoutRepository = workoutRepository,
        userPreferencesRepository = userPreferencesRepository,
        exerciseRepository = exerciseRepository,
        workoutTimers = AndroidWorkoutTimers(timerNotificationHelper),
        workoutText = { AndroidWorkoutText(localeManager.localizedContext()) },
        miniSetStore = AndroidWorkoutMiniSetStore(localeManager.localizedContext()),
        oneRepMaxRepository = oneRepMaxRepository,
        journalRepository = journalRepository,
        languageCodes = userPreferencesRepository.userLanguage.map { localeManager.getResolvedLanguage() },
        modelScope = viewModelScope,
        planId = savedStateHandle.get<Int>("planId"),
        sessionId = savedStateHandle.get<Int>("sessionId"),
        quickStart = savedStateHandle.get<Boolean>("quickStart") ?: false,
        workoutName = savedStateHandle.get<String>("workoutName"),
        nowMillis = System::currentTimeMillis
    )

    init {
        // The notifications of this workout outlive the screen: tell their helper which session they belong to
        viewModelScope.launch {
            model.state.map { it.sessionId }.distinctUntilChanged().collect { id ->
                if (id != null) timerNotificationHelper.workoutSessionId = id
            }
        }
    }

    override fun onCleared() {
        model.close()
        super.onCleared()
    }

    val timerNotificationsEnabled = userPreferencesRepository.timerNotificationsEnabled

    fun turnOffTimerNotifications() {
        viewModelScope.launch { userPreferencesRepository.setTimerNotificationsEnabled(false) }
    }
}
