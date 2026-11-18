package com.emanuel5014.trainable.data.repository

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.emanuel5014.trainable.domain.rpe.RpePreference

/**
 * RPE used to be asked only with advanced programming on, through an input mode. It now has a switch and a scale of
 * its own: the first time the app runs with them, they are set from how the old ones were, and written down so that
 * turning advanced programming off later does not take RPE away.
 */
class RpePreferenceMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        UserPreferencesRepository.RPE_ENABLED !in currentData

    override suspend fun migrate(currentData: Preferences): Preferences {
        val settings = RpePreference.fromLegacy(
            advancedProgramming = currentData[UserPreferencesRepository.ADVANCED_PROGRAMMING_ENABLED] ?: false,
            legacyInputMode = currentData[UserPreferencesRepository.RPE_INPUT_MODE]
        )
        return currentData.toMutablePreferences().apply {
            this[UserPreferencesRepository.RPE_ENABLED] = settings.enabled
            this[UserPreferencesRepository.RPE_SCALE] = settings.scale.id
        }
    }

    override suspend fun cleanUp() = Unit
}
