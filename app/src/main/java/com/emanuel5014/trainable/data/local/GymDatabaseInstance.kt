package com.emanuel5014.trainable.data.local

import android.content.Context
import androidx.room.Room

/**
 * The one database of the Android app. No SQLite driver is set, so Room keeps working on the system SQLite in its
 * compatibility mode, as it always did here; the schema, migrations and built-in data come from the shared module.
 */
object GymDatabaseInstance {
    @Volatile
    private var INSTANCE: GymDatabase? = null

    fun getDatabase(context: Context): GymDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                GymDatabase::class.java,
                GymDatabase.DATABASE_NAME
            )
                .withAppSetup(RetiredPresetsMarker.store(context.applicationContext)) { INSTANCE }
                .build()
            INSTANCE = instance
            instance
        }
    }

    fun closeDatabase() {
        INSTANCE?.close()
        INSTANCE = null
    }

    suspend fun resetDatabase(context: Context) {
        closeDatabase()
        RetiredPresetsMarker.forget(context)

        val dbFile = context.getDatabasePath(GymDatabase.DATABASE_NAME)
        val walFile = context.getDatabasePath("${GymDatabase.DATABASE_NAME}-wal")
        val shmFile = context.getDatabasePath("${GymDatabase.DATABASE_NAME}-shm")
        val journalFile = context.getDatabasePath("${GymDatabase.DATABASE_NAME}-journal")

        listOf(dbFile, walFile, shmFile, journalFile).forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
