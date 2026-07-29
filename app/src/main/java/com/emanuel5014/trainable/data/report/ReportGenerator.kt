package com.emanuel5014.trainable.data.report

import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.SessionExerciseSwapEntity
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ReportGenerator @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend fun generateReport(planId: Int, languageCode: String = "en"): PlanReport {
        val planWithDetails = workoutRepository.getPlanWithDetails(planId).first()
            ?: return PlanReport(
                planId = planId,
                planName = "Unknown",
                planNote = null,
                startDate = 0L,
                endDate = null,
                totalSessions = 0,
                periodFirstSession = null,
                periodLastSession = null,
                exercises = emptyList(),
                weightUnit = userPreferencesRepository.weightUnit.first()
            )

        val sessions = workoutRepository.getFinishedSessionsWithDetailsForPlans(listOf(planId))
        val sessionIds = sessions.map { it.session.id }
        val swaps = if (sessionIds.isNotEmpty()) {
            workoutRepository.getSwapsForSessions(sessionIds)
        } else {
            emptyList()
        }

        val weightUnit = userPreferencesRepository.weightUnit.first()

        return buildReport(planWithDetails, sessions, swaps, languageCode, weightUnit)
    }

    suspend fun generateReports(planIds: List<Int>, languageCode: String = "en"): List<PlanReport> {
        val reports = mutableListOf<PlanReport>()
        for (planId in planIds) {
            reports.add(generateReport(planId, languageCode))
        }
        return reports
    }

    private fun buildReport(
        planWithDetails: PlanWithDetails,
        sessions: List<SessionWithDetails>,
        swaps: List<SessionExerciseSwapEntity>,
        languageCode: String,
        weightUnit: String
    ): PlanReport {
        val plan = planWithDetails.plan
        val currentExerciseIds = planWithDetails.exercises.map { it.exercise.id }.toSet()

        val sessionTimestamps = sessions.map { it.session.timestamp }
        val periodFirst = sessionTimestamps.minOrNull()
        val periodLast = sessionTimestamps.maxOrNull()

        val exerciseDataMap = mutableMapOf<Int, MutableList<Pair<Long, List<SetEntry>>>>()
        val swapEventsByExercise = mutableMapOf<Int, MutableList<SwapEvent>>()

        val planExerciseMap = planWithDetails.exercises.associateBy { it.planExercise.id }

        swaps.forEach { swap ->
            val originalPlanExercise = planExerciseMap[swap.originalExerciseId]
            if (originalPlanExercise != null) {
                val session = sessions.find { it.session.id == swap.sessionId }
                val sessionDate = session?.session?.timestamp ?: return@forEach

                val originalExerciseName = ExerciseTranslations.translate(
                    originalPlanExercise.exercise.nome, languageCode
                )
                val replacementExercise = sessions.flatMap { it.sets }
                    .map { it.exercise }
                    .distinctBy { it.id }
                    .find { it.id == swap.replacementExerciseId }

                val replacementName = replacementExercise?.let {
                    ExerciseTranslations.translate(it.nome, languageCode)
                } ?: "Unknown"

                val event = SwapEvent(
                    sessionDate = sessionDate,
                    originalPlanExerciseId = swap.originalExerciseId,
                    originalExerciseName = originalExerciseName,
                    replacementExerciseName = replacementName
                )

                swapEventsByExercise.getOrPut(originalPlanExercise.exercise.id) { mutableListOf() }.add(event)
            }
        }

        sessions.forEach { session ->
            val date = session.session.timestamp
            session.sets.forEach { setWithExercise ->
                val exerciseId = setWithExercise.exercise.id
                val convertedWeight = WeightUnitConverter.convertDisplay(setWithExercise.setLog.pesoSollevato, weightUnit)
                val setEntry = SetEntry(
                    setNumber = setWithExercise.setLog.numeroSerie,
                    weight = convertedWeight,
                    reps = setWithExercise.setLog.repsEffettive,
                    rpe = setWithExercise.setLog.rpe,
                    isWarmup = setWithExercise.setLog.isWarmup,
                    note = setWithExercise.setLog.note
                )

                exerciseDataMap.getOrPut(exerciseId) { mutableListOf() }
                    .add(date to listOf(setEntry))
            }
        }

        val mergedExerciseData = mutableMapOf<Int, MutableMap<Long, MutableList<SetEntry>>>()
        exerciseDataMap.forEach { (exerciseId, sessionPairs) ->
            val sessionMap = mergedExerciseData.getOrPut(exerciseId) { mutableMapOf() }
            sessionPairs.forEach { (date, sets) ->
                sessionMap.getOrPut(date) { mutableListOf() }.addAll(sets)
            }
        }

        val exercises = mergedExerciseData.map { (exerciseId, sessionMap) ->
            val exerciseName = sessions.flatMap { it.sets }
                .map { it.exercise }
                .distinctBy { it.id }
                .find { it.id == exerciseId }
                ?.let { ExerciseTranslations.translate(it.nome, languageCode) }
                ?: "Unknown"

            val muscleGroup = sessions.flatMap { it.sets }
                .map { it.exercise }
                .distinctBy { it.id }
                .find { it.id == exerciseId }
                ?.let { ExerciseTranslations.translateCategory(it.categoria, languageCode) }
                ?: ""

            val sortedSessions = sessionMap.entries
                .sortedBy { it.key }
                .map { (date, sets) ->
                    ExerciseSessionEntry(
                        date = date,
                        sets = sets.sortedBy { it.setNumber }
                    )
                }

            val summary = calculateSummary(sortedSessions)

            ExerciseReport(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                muscleGroup = muscleGroup,
                isCurrentlyInPlan = exerciseId in currentExerciseIds,
                sessions = sortedSessions,
                summary = summary,
                swapEvents = swapEventsByExercise[exerciseId] ?: emptyList()
            )
        }.sortedByDescending { it.summary.totalSets }

        return PlanReport(
            planId = plan.id,
            planName = plan.nome,
            planNote = plan.note,
            startDate = plan.dataInizio,
            endDate = plan.dataFine,
            totalSessions = sessions.size,
            periodFirstSession = periodFirst,
            periodLastSession = periodLast,
            exercises = exercises,
            weightUnit = weightUnit
        )
    }

    private fun calculateSummary(sessions: List<ExerciseSessionEntry>): ExerciseSummary {
        if (sessions.isEmpty()) {
            return ExerciseSummary(0L, 0L, 0, 0f, 0L, 0f, 0L, null)
        }

        val allSets = sessions.flatMap { it.sets }
        val totalSets = allSets.size

        val maxWeightSet = allSets.maxByOrNull { it.weight }
        val maxWeight = maxWeightSet?.weight ?: 0f
        val maxWeightDate = sessions.find { session ->
            session.sets.any { it.weight == maxWeight }
        }?.date ?: 0L

        val volumeBySession = sessions.map { session ->
            val volume = session.sets.sumOf { (it.weight * it.reps).toDouble() }.toFloat()
            session.date to volume
        }
        val maxVolumeEntry = volumeBySession.maxByOrNull { it.second }
        val maxVolume = maxVolumeEntry?.second ?: 0f
        val maxVolumeDate = maxVolumeEntry?.first ?: 0L

        val bestOneRM = allSets
            .filter { !it.isWarmup && it.weight > 0 && it.reps > 0 }
            .maxOfOrNull { set ->
                val epley = set.weight * (1 + set.reps / 30f)
                epley
            }

        return ExerciseSummary(
            firstSessionDate = sessions.first().date,
            lastSessionDate = sessions.last().date,
            totalSets = totalSets,
            maxWeight = maxWeight,
            maxWeightDate = maxWeightDate,
            maxVolume = maxVolume,
            maxVolumeDate = maxVolumeDate,
            bestEstimatedOneRM = bestOneRM
        )
    }
}
