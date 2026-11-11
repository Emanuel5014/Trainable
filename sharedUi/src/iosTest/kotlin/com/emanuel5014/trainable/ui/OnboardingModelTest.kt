package com.emanuel5014.trainable.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WeightLogDao
import com.emanuel5014.trainable.data.local.entity.UserEntity
import com.emanuel5014.trainable.data.local.entity.WeightLogEntity
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingChoices
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingModel
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingSettings
import com.emanuel5014.trainable.ui.screens.onboarding.ThemeChoice
import com.emanuel5014.trainable.ui.screens.onboarding.changeUnit
import com.emanuel5014.trainable.ui.screens.onboarding.pickMembershipDate
import com.emanuel5014.trainable.ui.screens.onboarding.toSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.test.*

private class SetupPreferences : DataStore<Preferences> {
    val values = MutableStateFlow<Preferences>(emptyPreferences())
    var fail = false
    override val data: Flow<Preferences> = values
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        if (fail) error("synthetic storage failure")
        return transform(values.value).also { values.value = it }
    }
}
private class SetupUser : UserDao, WeightLogDao {
    val user = MutableStateFlow<UserEntity?>(null)
    val weights = mutableListOf<WeightLogEntity>()
    override fun getUser(): Flow<UserEntity?> = user
    override suspend fun insertUser(user: UserEntity): Long { this.user.value = user; return user.id.toLong() }
    override suspend fun updateUser(user: UserEntity) { this.user.value = user }
    override suspend fun insertWeight(entry: WeightLogEntity) { weights += entry }
    override fun getWeightHistory(startDate: Long): Flow<List<WeightLogEntity>> = flowOf(weights.filter { it.timestamp >= startDate })
    override suspend fun deleteWeightLog(id: Int) { weights.removeAll { it.id == id } }
}

private fun setup(
    name: String, weight: String, goal: String, unit: String,
    theme: ThemeChoice = ThemeChoice(true, null, 0, 0, 0),
    edit: OnboardingChoices.() -> Unit = {}
): OnboardingSettings = OnboardingChoices().apply {
    username.value = name; weightInput.value = weight; weeklyGoalInput.value = goal; changeUnit(unit); edit()
}.toSettings(theme, "e2b")

class OnboardingModelTest {
    @Test fun newProfileSavesChoicesBeforeCompletionAndScheduler() = runBlocking {
        val store = SetupPreferences(); val prefs = UserPreferencesRepository(store); val user = SetupUser()
        var scheduled = false
        val model = OnboardingModel(UserRepository(user, user), prefs, this, nowMillis = { 12345L }, onCompleted = {
            assertTrue(prefs.hasCompletedOnboarding.first())
            assertEquals("Ada", user.user.value?.username)
            assertEquals(5, prefs.weeklyGoal.first())
            scheduled = true
        })
        model.completeOnboarding(setup("Ada", "62.5", "5", "kg", ThemeChoice(true, null, 3, 2, 2)) {
            hapticEnabled.value = false; timerNotificationsEnabled.value = false; advancedProgramming.value = true
        })
        assertTrue(scheduled)
        assertEquals(12345L, user.user.value?.dataIscrizione)
        assertEquals(62.5f, user.weights.single().pesoCorporeo)
        assertEquals(12345L, user.weights.single().timestamp)
        assertFalse(prefs.hapticEnabled.first())
        assertFalse(prefs.timerNotificationsEnabled.first())
        assertTrue(prefs.advancedProgrammingEnabled.first())
        assertEquals(3, prefs.themePalette.first())
        assertEquals(2, prefs.themeStyle.first())
        assertEquals(2, prefs.themeMode.first())
    }

    @Test fun existingProfilePreservesIdentityAndDoesNotAddZeroWeight() = runBlocking {
        val store = SetupPreferences(); val prefs = UserPreferencesRepository(store); val user = SetupUser()
        val original = UserEntity(id = 8, username = "Old", dataIscrizione = 777L)
        user.user.value = original
        val model = OnboardingModel(UserRepository(user, user), prefs, this, nowMillis = { 999L })
        model.completeOnboarding(setup(" ", "", "0", "lb"))
        assertEquals(original.copy(username = "Athlete"), user.user.value)
        assertTrue(user.weights.isEmpty())
        assertEquals(3, prefs.weeklyGoal.first())
        assertEquals("lb", prefs.weightUnit.first())
        assertTrue(prefs.hasCompletedOnboarding.first())
    }

    @Test fun failedPreferencesNeverMarkSetupCompletedOrScheduleBackup() = runBlocking {
        val store = SetupPreferences(); val prefs = UserPreferencesRepository(store); val user = SetupUser()
        var scheduled = false
        val model = OnboardingModel(UserRepository(user, user), prefs, this, onCompleted = { scheduled = true })
        store.fail = true
        assertFailsWith<IllegalStateException> { model.completeOnboarding(setup("Ada", "", "3", "kg")) }
        assertFalse(prefs.hasCompletedOnboarding.first())
        assertFalse(scheduled)
    }

    @Test fun disconnectedCloudSettingsAreNotOverwrittenBySetupDefaults() = runBlocking {
        val store = SetupPreferences(); val prefs = UserPreferencesRepository(store); val user = SetupUser()
        prefs.setNextcloudWifiOnly(true)
        prefs.setNextcloudAutoBackupFrequency(7)
        val model = OnboardingModel(UserRepository(user, user), prefs, this)
        model.completeOnboarding(setup("Ada", "", "3", "kg") { nextcloudWifiOnly.value = false; nextcloudAutoBackupFrequency.intValue = 1 })
        assertNull(prefs.nextcloudServerUrl.first())
        assertTrue(prefs.nextcloudWifiOnly.first())
        assertEquals(7, prefs.nextcloudAutoBackupFrequency.first())
    }

    @Test fun everyTweakIsWrittenToItsOwnPreference() = runBlocking {
        val store = SetupPreferences(); val prefs = UserPreferencesRepository(store); val user = SetupUser()
        val model = OnboardingModel(UserRepository(user, user), prefs, this)
        model.completeOnboarding(setup("Ada", "", "3", "lb") {
            rpeEnabled.value = true; rpeScale.value = RpeScale.ADVANCED; startingWeight.value = true
            setPerformance.value = true; inlineModifications.value = true; loadRounding.floatValue = 10f
            restTimerEnabled.value = false; warmupTimerEnabled.value = true; workoutTimerEnabled.value = true
            timerAddEnabled.value = false; timerAddSeconds.intValue = 60; timerSubtractEnabled.value = false
            timerSubtractSeconds.intValue = 5; keepScreenOnSetTimer.value = false; keepScreenOnCardioTimer.value = true
            autoStopCardioAtTarget.value = false; autoStopTimeWeightAtTarget.value = true
            exerciseMedia.value = true; exerciseMediaLarge.value = false; plateCalculator.value = true
            editablePresets.value = true; journalEnabled.value = true; journalPromptAtFinish.value = false
            journalRating.value = false; pickMembershipDate(1_800_000_000_000L)
        })
        assertTrue(prefs.rpeEnabled.first()); assertEquals(RpeScale.ADVANCED, prefs.rpeScale.first())
        assertTrue(prefs.startingWeightEnabled.first()); assertTrue(prefs.setPerformanceEnabled.first())
        assertTrue(prefs.inlineExerciseModificationsEnabled.first()); assertEquals(10f, prefs.loadRoundingIncrement.first())
        assertFalse(prefs.restTimerEnabled.first()); assertTrue(prefs.warmupTimerEnabled.first())
        assertTrue(prefs.workoutTimerEnabled.first()); assertFalse(prefs.timerAddEnabled.first())
        assertEquals(60, prefs.timerAddSeconds.first()); assertFalse(prefs.timerSubtractEnabled.first())
        assertEquals(5, prefs.timerSubtractSeconds.first()); assertFalse(prefs.keepScreenOnSetTimer.first())
        assertTrue(prefs.keepScreenOnCardioTimer.first()); assertFalse(prefs.autoStopCardioAtTarget.first())
        assertTrue(prefs.autoStopTimeWeightAtTarget.first())
        assertTrue(prefs.exerciseMediaEnabled.first()); assertFalse(prefs.exerciseMediaLarge.first())
        assertTrue(prefs.plateCalculatorEnabled.first()); assertTrue(prefs.editablePresetExercises.first())
        assertTrue(prefs.journalEnabled.first()); assertFalse(prefs.journalPromptAtFinish.first())
        assertFalse(prefs.journalRatingEnabled.first())
        assertEquals(1_800_000_000_000L, prefs.gymMembershipExpiryDate.first())
        assertTrue(prefs.gymMembershipExpiryNotificationsEnabled.first())
    }

    @Test fun noDateLeavesExpiryUnset() = runBlocking {
        val store = SetupPreferences(); val prefs = UserPreferencesRepository(store); val user = SetupUser()
        OnboardingModel(UserRepository(user, user), prefs, this).completeOnboarding(setup("Ada", "", "3", "kg"))
        assertNull(prefs.gymMembershipExpiryDate.first())
    }
}
