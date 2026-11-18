package com.emanuel5014.trainable.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** The settings file of the iOS app, in its Documents folder, named like the Android one. Open it once per process. */
@OptIn(ExperimentalForeignApi::class)
fun createSettingsDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    migrations = listOf(RpePreferenceMigration()),
    produceFile = {
        val documents = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        (requireNotNull(documents?.path) + "/settings.preferences_pb").toPath()
    }
)
