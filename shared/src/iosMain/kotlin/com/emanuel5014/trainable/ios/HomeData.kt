package com.emanuel5014.trainable.ios

import com.emanuel5014.trainable.data.local.GymDatabaseInstance
import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** A routine as the first iOS screen lists it. */
data class RoutineItem(val id: Int, val name: String, val exerciseCount: Int, val active: Boolean)

/** A finished workout as the first iOS screen lists it; [date] is already written out (day/month/year). */
data class WorkoutItem(val id: Int, val routine: String, val date: String, val workingSets: Int)

/**
 * What the first iOS screen shows, read from the shared database. Swift calls the functions with `try await`.
 */
class HomeData {
    private val database = GymDatabaseInstance.getDatabase()

    /** The user's routines, active ones first, without the system ones (Quick, Custom, Cardio). */
    suspend fun routines(): List<RoutineItem> = database.workoutDao().getAllPlansWithDetails().first()
        .filter { !it.plan.isSystemPlan }
        .map { RoutineItem(it.plan.id, it.plan.nome, it.exercises.size, it.plan.isActive) }

    /** The last [limit] finished workouts, newest first. */
    suspend fun recentWorkouts(limit: Int): List<WorkoutItem> = database.workoutDao().getAllSessionsWithDetails().first()
        .take(limit)
        .map { details ->
            val day = Instant.fromEpochMilliseconds(details.session.timestamp)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            WorkoutItem(
                id = details.session.id,
                routine = details.plan.nome,
                date = "${day.day.pad()}/${day.month.ordinal.plus(1).pad()}/${day.year}",
                workingSets = details.sets.count { !it.setLog.isWarmup && it.setLog.isCompleted }
            )
        }

    /** How many exercises the library holds, the built-in ones included. */
    suspend fun exerciseCount(): Int = database.exerciseDao().getAllExercisesIncludingDeleted().first().size

    private fun Int.pad() = toString().padStart(2, '0')
}
