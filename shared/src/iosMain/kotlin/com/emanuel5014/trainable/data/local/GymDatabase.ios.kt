package com.emanuel5014.trainable.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask

/** The database of the iOS app, in its Documents folder, on the SQLite that ships inside the app. */
object GymDatabaseInstance {
    private var instance: GymDatabase? = null

    fun getDatabase(): GymDatabase = instance ?: Room.databaseBuilder<GymDatabase>(name = databasePath())
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .withAppSetup(UserDefaultsRetiredPresets) { instance }
        .build()
        .also { instance = it }

    fun closeDatabase() {
        instance?.close()
        instance = null
    }

    @OptIn(ExperimentalForeignApi::class)
    fun databasePath(): String {
        val documents = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null
        )
        return requireNotNull(documents?.path) + "/" + GymDatabase.DATABASE_NAME
    }
}

private object UserDefaultsRetiredPresets : RetiredPresetsStore {
    private const val KEY = "retired_presets_version"

    override fun applied(): Int = NSUserDefaults.standardUserDefaults.integerForKey(KEY).toInt()

    override fun markApplied(version: Int) = NSUserDefaults.standardUserDefaults.setInteger(version.toLong(), KEY)
}
