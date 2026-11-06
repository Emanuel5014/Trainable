package com.emanuel5014.trainable.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WeightLogDao
import com.emanuel5014.trainable.data.local.entity.UserEntity
import com.emanuel5014.trainable.data.local.entity.WeightLogEntity
import com.emanuel5014.trainable.data.repository.UserRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.ui.screens.onboarding.OnboardingModel
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
        model.completeOnboarding("Ada", 62.5f, 5, "kg", hapticEnabled = false,
            timerNotificationsEnabled = false, advancedProgrammingEnabled = true,
            themePalette = 3, themeStyle = 2, themeMode = 2)
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
        model.completeOnboarding(" ", 0f, 0, "lb")
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
        assertFailsWith<IllegalStateException> { model.completeOnboarding("Ada", 0f, 3, "kg") }
        assertFalse(prefs.hasCompletedOnboarding.first())
        assertFalse(scheduled)
    }

    @Test fun disconnectedCloudSettingsAreNotOverwrittenBySetupDefaults() = runBlocking {
        val store = SetupPreferences(); val prefs = UserPreferencesRepository(store); val user = SetupUser()
        prefs.setNextcloudWifiOnly(true)
        prefs.setNextcloudAutoBackupFrequency(7)
        val model = OnboardingModel(UserRepository(user, user), prefs, this)
        model.completeOnboarding("Ada", 0f, 3, "kg", nextcloudWifiOnly = false, nextcloudAutoBackupFrequency = 1)
        assertNull(prefs.nextcloudServerUrl.first())
        assertTrue(prefs.nextcloudWifiOnly.first())
        assertEquals(7, prefs.nextcloudAutoBackupFrequency.first())
    }
}
