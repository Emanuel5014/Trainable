package com.emanuel5014.trainable.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.emanuel5014.trainable.data.local.GymDatabase
import com.emanuel5014.trainable.data.local.dao.ExerciseDao
import com.emanuel5014.trainable.data.local.dao.UserDao
import com.emanuel5014.trainable.data.local.dao.WorkoutDao
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.CustomCategoryEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.PlanExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SYSTEM_PLAN_NOTE
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.kind
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.TIME_AND_WEIGHT
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvColumns
import com.emanuel5014.trainable.domain.csvimport.HistoryImportPlanner
import com.emanuel5014.trainable.domain.csvimport.ImportPlan
import com.emanuel5014.trainable.domain.csvimport.KnownData
import com.emanuel5014.trainable.domain.csvimport.KnownSession
import com.emanuel5014.trainable.domain.csvimport.PlannedWorkout
import com.emanuel5014.trainable.domain.csvimport.ResolvedExercise
import com.emanuel5014.trainable.domain.csvimport.RoutineStatus
import com.emanuel5014.trainable.domain.csvimport.RoutineTarget
import com.emanuel5014.trainable.domain.csvimport.RowKind
import com.emanuel5014.trainable.domain.csvimport.SetSignature
import com.emanuel5014.trainable.domain.csvimport.SystemPlanKind
import com.emanuel5014.trainable.domain.prescription.SetPerformance
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** What an import added, and the ids to take it out again. */
data class ImportResult(
    val workouts: Int,
    val sets: Int,
    val newExercises: Int,
    val newRoutines: Int,
    val newCategories: Int,
    val skippedDuplicates: Int,
    val skippedRows: Int,
    internal val sessionIds: List<Int>,
    internal val planIds: List<Int>,
    internal val exerciseIds: List<Int>,
    internal val categoryIds: List<Int>,
    /** Exercises deleted from the exercise list that the file brought back, hidden again by [HistoryCsvImporter.undo]. */
    internal val revivedExerciseIds: List<Int> = emptyList()
)

/**
 * Applies an [ImportPlan] (the history of a CSV) to the database. Everything is written in one transaction, so an
 * import either lands whole or not at all, and [undo] takes exactly what it added out again.
 */
@Singleton
class HistoryCsvImporter @Inject constructor(
    private val database: GymDatabase,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val userDao: UserDao,
    @ApplicationContext private val context: Context
) {
    /** What the app holds now, to match a file against. */
    suspend fun loadKnown(zone: ZoneId = ZoneId.systemDefault()): KnownData {
        // Also the exercises deleted from the exercise list: their workouts are still there, so a file that repeats them
        // is recognised as duplicates, and one with new workouts of such an exercise adds them to it
        val exercises = exerciseDao.getAllExercisesIncludingDeleted().first()
        val categories = exerciseDao.getAllCustomCategories().first().map { it.name }
        val plans = workoutDao.getAllPlans().first()
        val sessions = workoutDao.getAllSessionsWithDetails().first()
        return withContext(Dispatchers.Default) {
            KnownData(
                exercises = exercises,
                customCategories = categories,
                plans = plans,
                sessions = sessions.map { details ->
                    KnownSession(
                        id = details.session.id,
                        planId = details.session.planId,
                        date = Instant.ofEpochMilli(details.session.timestamp).atZone(zone).toLocalDate(),
                        sets = details.sets.map {
                            SetSignature(
                                key = HistoryImportPlanner.exerciseKey(it.setLog.exerciseId),
                                weightKg = it.setLog.pesoSollevato,
                                // The reps of a timed hold mean nothing (older rows kept the duration there)
                                reps = if (it.setLog.kind == SetLogKind.TIME_AND_WEIGHT) 0 else it.setLog.repsEffettive,
                                seconds = it.setLog.durataSecondi ?: 0,
                                km = 0f
                            )
                        } + details.cardio.map {
                            SetSignature(HistoryImportPlanner.cardioKey(it.categoria), 0f, 0, it.durataSecondi, it.distanza)
                        }
                    )
                }
            )
        }
    }

    suspend fun apply(plan: ImportPlan, zone: ZoneId = ZoneId.systemDefault()): ImportResult {
        val result = database.withTransaction { write(plan, zone) }
        triggerWidgetUpdate()
        return result
    }

    private suspend fun write(plan: ImportPlan, zone: ZoneId): ImportResult {
        val user = userDao.getUser().first() ?: throw IllegalStateException("No user: finish the onboarding first")
        val workouts = plan.toImport

        // Categories the file brings; exercises show them without it, but this is where the user manages them
        val knownCategories = exerciseDao.getAllCustomCategories().first().map { HistoryCsvColumns.fold(it.name) }.toSet()
        val categoryIds = plan.newCategories.filter { HistoryCsvColumns.fold(it) !in knownCategories }.map {
            exerciseDao.insertCustomCategory(CustomCategoryEntity(name = it)).toInt()
        }

        // New exercises are custom ones: ids from 1000 up, as everywhere else
        var nextExerciseId = maxOf(exerciseDao.getMaxId() + 1, FIRST_CUSTOM_ID)
        val exerciseIds = mutableListOf<Int>()
        val created = mutableMapOf<String, ExerciseEntity>()
        plan.newExercises.forEach { use ->
            val exercise = use.exercise as ResolvedExercise.New
            val entity = ExerciseEntity(id = nextExerciseId++, nome = exercise.name, categoria = exercise.category)
            exerciseDao.insertExercise(entity)
            created[exercise.key] = entity
            exerciseIds.add(entity.id)
        }

        // Exercises the user deleted from the exercise list that the imported workouts use come back to it
        val revived = linkedSetOf<Int>()
        fun exerciseOf(exercise: ResolvedExercise): ExerciseEntity = when (exercise) {
            is ResolvedExercise.Existing -> exercise.exercise.also { if (it.isDeleted) revived.add(it.id) }
            is ResolvedExercise.New -> created.getValue(exercise.key)
        }

        val allPlans = workoutDao.getAllPlans().first()
        var nextOrder = (allPlans.maxOfOrNull { it.ordine } ?: -1) + 1
        val planIds = mutableListOf<Int>()
        val now = System.currentTimeMillis()

        // The app's own plans for workouts that belong to no routine, made when they are not there yet
        val systemPlanIds = mutableMapOf<SystemPlanKind, Int>()
        workouts.mapNotNull { (it.routine as? RoutineTarget.System)?.kind }.distinct().forEach { kind ->
            val existing = allPlans.firstOrNull { it.isSystemPlan && HistoryCsvColumns.fold(it.nome) in kind.aliases }
            systemPlanIds[kind] = existing?.id ?: workoutDao.insertPlan(
                WorkoutPlanEntity(
                    userId = user.id,
                    nome = kind.planName,
                    note = SYSTEM_PLAN_NOTE,
                    dataInizio = now,
                    isActive = false,
                    ordine = nextOrder++
                )
            ).toInt().also { planIds.add(it) }
        }

        // Routines named in the file that do not exist yet, with the exercises of their latest workout
        val routineIds = mutableMapOf<String, Int>()
        plan.routines.filter { it.status == RoutineStatus.NEW }.forEach { routine ->
            val first = workouts.filter { (it.routine as? RoutineTarget.Create)?.key == routine.key }.minOf { it.timestamp }
            val planId = workoutDao.insertPlan(
                WorkoutPlanEntity(
                    userId = user.id,
                    nome = routine.name,
                    dataInizio = first,
                    isActive = true,
                    ordine = nextOrder++
                )
            ).toInt()
            routineIds[routine.key] = planId
            planIds.add(planId)
            routine.template.distinctBy { exerciseOf(it.exercise).id }.forEachIndexed { index, item ->
                val isCardio = item.kind == RowKind.CARDIO
                workoutDao.insertPlanExercise(
                    PlanExerciseEntity(
                        planId = planId,
                        exerciseId = exerciseOf(item.exercise).id,
                        serieTarget = item.sets,
                        repsTarget = item.repsTarget,
                        recuperoTarget = if (isCardio) 0 else DEFAULT_REST_SECONDS,
                        ordine = index,
                        exerciseType = when (item.kind) {
                            RowKind.STRENGTH -> "strength"
                            RowKind.TIME_AND_WEIGHT -> TIME_AND_WEIGHT
                            RowKind.TIME_AND_REPS -> TIME_AND_REPS
                            RowKind.CARDIO -> "cardio"
                        },
                        durataTargetSecondi = item.durationSeconds,
                        cardioCategoria = if (isCardio) ResolvedExercise.CARDIO else null
                    )
                )
            }
        }

        var sets = 0
        val sessionIds = workouts.map { workout ->
            val planId = when (val target = workout.routine) {
                is RoutineTarget.Existing -> target.plan.id
                is RoutineTarget.Create -> routineIds.getValue(target.key)
                is RoutineTarget.System -> systemPlanIds.getValue(target.kind)
            }
            sets += workout.setCount
            writeWorkout(workout, planId, ::exerciseOf)
        }

        revived.forEach { exerciseDao.setExerciseDeleted(it, false) }

        return ImportResult(
            workouts = workouts.size,
            sets = sets,
            newExercises = exerciseIds.size,
            newRoutines = routineIds.size,
            newCategories = categoryIds.size,
            skippedDuplicates = if (plan.options.skipDuplicates) plan.duplicates.size else 0,
            skippedRows = plan.skippedRows,
            sessionIds = sessionIds,
            planIds = planIds,
            exerciseIds = exerciseIds,
            categoryIds = categoryIds,
            revivedExerciseIds = revived.toList()
        )
    }

    private suspend fun writeWorkout(workout: PlannedWorkout, planId: Int, exerciseOf: (ResolvedExercise) -> ExerciseEntity): Int {
        val sessionId = workoutDao.insertSession(
            WorkoutSessionEntity(
                planId = planId,
                timestamp = workout.timestamp,
                noteSessione = workout.sessionNote,
                isFinished = true,
                durationMs = workout.durationMs,
                programWeek = workout.programWeek
            )
        ).toInt()

        val setRows = mutableListOf<SetLogEntity>()
        val cardioRows = mutableListOf<CardioLogEntity>()
        workout.entries.forEach { entry ->
            val exercise = exerciseOf(entry.exercise)
            entry.sets.forEach { set ->
                if (entry.kind == RowKind.CARDIO) {
                    cardioRows.add(
                        CardioLogEntity(
                            sessionId = sessionId,
                            // Cardio rows are named after the activity
                            categoria = exercise.nome,
                            distanza = set.distanceKm,
                            durataSecondi = set.durationSeconds ?: 0,
                            timestamp = workout.timestamp,
                            ordineEsercizio = entry.order,
                            numeroSerie = set.number,
                            note = set.note
                        )
                    )
                } else {
                    setRows.add(
                        SetLogEntity(
                            sessionId = sessionId,
                            exerciseId = exercise.id,
                            pesoSollevato = set.weightKg,
                            repsEffettive = set.reps,
                            numeroSerie = set.number,
                            rpe = set.rpe,
                            isWarmup = set.warmup,
                            note = set.note,
                            ordineEsercizio = entry.order,
                            durataSecondi = set.durationSeconds,
                            exerciseType = if (entry.kind == RowKind.TIME_AND_REPS) TIME_AND_REPS else null,
                            repParts = SetPerformance.encodeParts(set.parts),
                            intraRests = SetPerformance.encodePauses(set.pauses).takeIf { set.parts.size > 1 },
                            assistedReps = set.assisted,
                            partialReps = set.partials
                        )
                    )
                }
            }
        }
        if (setRows.isNotEmpty()) workoutDao.insertSets(setRows)
        if (cardioRows.isNotEmpty()) workoutDao.insertCardioLogs(cardioRows)
        return sessionId
    }

    /** Takes out what [apply] added: its workouts, the routines and exercises it created, nothing else. */
    suspend fun undo(result: ImportResult) {
        database.withTransaction {
            result.sessionIds.chunked(CHUNK).forEach { workoutDao.deleteSessionsByIds(it) }
            // A plan of the app's own that the import made goes with it unless a workout has landed in it since
            val removable = result.planIds.filter { workoutDao.countSessionsForPlan(it) == 0 }
            removable.chunked(CHUNK).forEach { workoutDao.deletePlansByIds(it) }
            // An exercise the user has put to use since (in a routine, in a workout) stays, hidden, instead of taking that along
            result.exerciseIds.forEach { exerciseDao.removeFromLibrary(it) }
            result.revivedExerciseIds.forEach { exerciseDao.setExerciseDeleted(it, true) }
            result.categoryIds.chunked(CHUNK).forEach { exerciseDao.deleteCustomCategoriesByIds(it) }
        }
        triggerWidgetUpdate()
    }

    private fun triggerWidgetUpdate() {
        try {
            com.emanuel5014.trainable.widget.TrainableWidget.update(context)
            com.emanuel5014.trainable.widget.WeeklyGoalWidget.update(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private companion object {
        const val FIRST_CUSTOM_ID = 1000
        const val DEFAULT_REST_SECONDS = 120
        // Stays under SQLite's limit on variables in one statement
        const val CHUNK = 500
    }
}
