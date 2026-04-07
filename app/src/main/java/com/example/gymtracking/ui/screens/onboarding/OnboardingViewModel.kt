package com.example.gymtracking.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracking.data.local.entity.UserEntity
import com.example.gymtracking.data.repository.UserPreferencesRepository
import com.example.gymtracking.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModel() {

    suspend fun completeOnboarding(username: String, initialWeight: Float, weeklyGoal: Int) {
        // Update the default user that is created by Room callback (ID = 1)
        val existingUser = userRepository.currentUser.firstOrNull()

        val user = existingUser?.copy(
            username = username.ifBlank { "Athlete" }
        ) ?: UserEntity(
            id = 1,
            username = username.ifBlank { "Athlete" },
            dataIscrizione = System.currentTimeMillis()
        )

        userRepository.saveUser(user)

        if (initialWeight > 0f) {
            userRepository.addWeightLog(
                peso = initialWeight,
                data = System.currentTimeMillis(),
                userId = user.id
            )
        }

        userPrefsRepository.setWeeklyGoal(if (weeklyGoal > 0) weeklyGoal else 3)
        userPrefsRepository.setOnboardingCompleted(true)
    }
}
