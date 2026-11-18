package com.emanuel5014.trainable.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// The settings file of the Android app (datastore/settings.preferences_pb), read by UserPreferencesRepository
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { listOf(RpePreferenceMigration()) }
)
