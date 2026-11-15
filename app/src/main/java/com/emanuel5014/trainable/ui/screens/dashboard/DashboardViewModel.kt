package com.emanuel5014.trainable.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.notification.GymMembershipWorker
import com.emanuel5014.trainable.util.notification.TimerNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** The Android holder of the shared [DashboardState]: Hilt gives it the repositories, the ViewModel its lifetime. */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    analyticsRepository: AnalyticsRepository,
    userRepository: UserRepository,
    private val userPrefsRepository: UserPreferencesRepository,
    private val timerNotificationHelper: TimerNotificationHelper,
    @ApplicationContext private val context: Context
) : ViewModel(), DashboardActions {

    private val state = DashboardState(
        data = object : DashboardData {
            override fun activePlans(): Flow<List<WorkoutPlanEntity>> = workoutRepository.getActivePlans()
            override fun allSessions(): Flow<List<WorkoutSessionEntity>> = workoutRepository.getAllSessions()
            override fun unfinishedSessions(): Flow<List<SessionWithPlanName>> =
                workoutRepository.getUnfinishedSessionsWithPlanName()
            override fun cardioSessionCountSince(sinceTimestamp: Long): Flow<Int> =
                workoutRepository.getCardioSessionCountSince(sinceTimestamp)
            // Through the repository, which also refreshes the home screen widgets
            override suspend fun deleteSession(sessionId: Int) = workoutRepository.deleteSession(sessionId)
            // The workout screen is closed here: the rest saved on the session changes, then its notification follows
            override suspend fun adjustRest(sessionId: Int, button: RestButton) {
                SavedTimers.adjust(workoutRepository, userPrefsRepository, sessionId, button, System.currentTimeMillis())
                    ?.let { timerNotificationHelper.followSavedTimer(it) }
            }
        },
        analyticsRepository = analyticsRepository,
        userRepository = userRepository,
        userPrefsRepository = userPrefsRepository,
        scope = viewModelScope,
        onMembershipSet = { GymMembershipWorker.enqueueImmediateCheck(context) }
    )

    val uiState: StateFlow<DashboardUiState> = state.uiState

    val physicalCheckBiometricEnabled = userPrefsRepository.physicalCheckBiometricEnabled

    fun refreshToday() = state.refreshToday()

    override fun setGymMembershipExpiryDate(timestampMillis: Long?) = state.setGymMembershipExpiryDate(timestampMillis)

    override fun deleteSession(sessionId: Int) = state.deleteSession(sessionId)

    override fun toggleSelection(sessionId: Int) = state.toggleSelection(sessionId)

    override fun clearSelection() = state.clearSelection()

    override fun deleteSelectedSessions() = state.deleteSelectedSessions()

    override fun adjustRest(sessionId: Int, button: RestButton) = state.adjustRest(sessionId, button)
}
