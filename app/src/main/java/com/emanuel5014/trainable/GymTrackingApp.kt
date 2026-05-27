package com.emanuel5014.trainable

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import com.emanuel5014.trainable.util.backup.AutoBackupWorker
import com.emanuel5014.trainable.util.notification.GymMembershipWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GymTrackingApp : Application(), Configuration.Provider {

    @Inject
    lateinit var localeManager: AppLocaleManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var userPrefsRepository: UserPreferencesRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            localeManager.applyStoredLanguage()
            GymMembershipWorker.schedule(this@GymTrackingApp)
            
            // Reschedule AutoBackupWorker on startup if it was enabled (e.g. after an app update)
            val isAutoBackupEnabled = userPrefsRepository.autoBackupEnabled.first()
            if (isAutoBackupEnabled) {
                val frequency = userPrefsRepository.autoBackupFrequency.first()
                AutoBackupWorker.schedule(
                    context = this@GymTrackingApp,
                    frequencyDays = frequency,
                    policy = ExistingPeriodicWorkPolicy.KEEP
                )
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
