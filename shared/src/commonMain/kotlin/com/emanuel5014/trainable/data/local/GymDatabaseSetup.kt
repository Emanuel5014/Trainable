package com.emanuel5014.trainable.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.entity.UserEntity
import com.emanuel5014.trainable.domain.exercise.ExercisePresets
import com.emanuel5014.trainable.domain.exercise.RetiredPresets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** Up to which [RetiredPresets.VERSION] the retired presets were already hidden in this database (0 = never). */
interface RetiredPresetsStore {
    fun applied(): Int
    fun markApplied(version: Int)
}

/**
 * What every platform does with the database: the migrations, a fresh start on a downgrade, and the built-in data
 * (user 1 and the exercise library) on a new database and at every open. [database] is the instance being built,
 * read when Room calls back.
 */
fun RoomDatabase.Builder<GymDatabase>.withAppSetup(
    retiredPresets: RetiredPresetsStore,
    database: () -> GymDatabase?
): RoomDatabase.Builder<GymDatabase> =
    addMigrations(*GymDatabase.ALL_MIGRATIONS)
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(connection: SQLiteConnection) {
                val dbInstance = database()
                if (dbInstance != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(dbInstance.exerciseDao(), dbInstance.userDao(), retiredPresets)
                    }
                }
            }

            override fun onOpen(connection: SQLiteConnection) {
                val dbInstance = database()
                if (dbInstance != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        syncExercises(dbInstance.exerciseDao(), retiredPresets)
                        // After the sync, so that a preset it just added can take its logs
                        repairCardioLinks(dbInstance.exerciseDao(), dbInstance.workoutDao())
                    }
                }
            }
        })

private suspend fun populateInitialData(exerciseDao: ExerciseDao, userDao: UserDao, retiredPresets: RetiredPresetsStore) {
    try {
        userDao.insertUser(
            UserEntity(
                id = 1,
                username = "Athlete",
                dataIscrizione = Clock.System.now().toEpochMilliseconds()
            )
        )
    } catch (e: Exception) {
        // User might already exist, ignore
    }

    syncExercises(exerciseDao, retiredPresets)
}

private suspend fun syncExercises(exerciseDao: ExerciseDao, retiredPresets: RetiredPresetsStore) {
    try {
        val existing = exerciseDao.getAllExercisesIncludingDeleted().first()
        val skipped = exerciseDao.getSkippedPresetIds().toSet()
        exerciseDao.insertExercises(ExercisePresets.toInsert(ExerciseData.initialExercises, existing, skipped))
    } catch (e: Exception) {
        // Exercises might already exist, ignore
    }
    hideRetiredPresets(exerciseDao, retiredPresets)
}

/** Links the cardio logs left without an exercise (normally none). A failure is ignored: the next launch tries again. */
private suspend fun repairCardioLinks(exerciseDao: ExerciseDao, workoutDao: WorkoutDao) {
    try {
        CardioLinking.repair(exerciseDao, workoutDao)
    } catch (e: Exception) {
        // Left unlinked for now: the rows keep their name, the next launch tries again
    }
}

/**
 * Once per [RetiredPresets.VERSION] and database, hides the retired presets that joined the list since the last run
 * and that this install still lists unused and as shipped. The ones a workout, a routine or a max uses are left alone.
 * It is marked done only when it went through.
 */
private suspend fun hideRetiredPresets(exerciseDao: ExerciseDao, retiredPresets: RetiredPresetsStore) {
    val applied = retiredPresets.applied()
    if (applied >= RetiredPresets.VERSION) return
    try {
        val existing = exerciseDao.getAllExercisesIncludingDeleted().first()
        val used = RetiredPresets.idsSince(applied).filterTo(hashSetOf()) { exerciseDao.isExerciseInUse(it) }
        RetiredPresets.toHide(existing, ExerciseData.initialExercises, used, applied)
            .forEach { exerciseDao.setExerciseDeleted(it, true) }
        retiredPresets.markApplied(RetiredPresets.VERSION)
    } catch (e: Exception) {
        // Not marked, so the next launch tries again
    }
}
