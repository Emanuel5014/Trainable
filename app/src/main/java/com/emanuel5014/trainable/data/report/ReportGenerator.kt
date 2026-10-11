package com.emanuel5014.trainable.data.report

import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.groupedByExercise
import com.emanuel5014.trainable.data.local.entity.kind
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
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: com.emanuel5014.trainable.util.AppLocaleManager
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
        val advanced = userPreferencesRepository.advancedProgrammingEnabled.first()

        return buildReport(planWithDetails, sessions, swaps, languageCode, weightUnit, advanced)
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
        weightUnit: String,
        advanced: Boolean
    ): PlanReport {
        val plan = planWithDetails.plan
        val currentExerciseIds = planWithDetails.exercises.map { it.exercise.id }.toSet()

        val sessionTimestamps = sessions.map { it.session.timestamp }
        val periodFirst = sessionTimestamps.minOrNull()
        val periodLast = sessionTimestamps.maxOrNull()

        val exerciseDataMap = mutableMapOf<Int, MutableList<Pair<Long, List<SetEntry>>>>()
        // The rows of each exercise, to tell how it is measured (weight × reps, timed, time & reps)
        val kindRows = mutableMapOf<Int, MutableList<com.emanuel5014.trainable.data.local.entity.SetLogEntity>>()
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
                    rpe = setWithExercise.setLog.rpe.takeIf { advanced },
                    isWarmup = setWithExercise.setLog.isWarmup,
                    note = setWithExercise.setLog.note,
                    prescription = if (advanced) {
                        com.emanuel5014.trainable.ui.components.setLogPrescriptionText(
                            localeManager.localizedContext(), setWithExercise.setLog
                        )
                    } else null,
                    seconds = setWithExercise.setLog.durataSecondi
                )
                kindRows.getOrPut(exerciseId) { mutableListOf() }.add(setWithExercise.setLog)

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
                swapEvents = swapEventsByExercise[exerciseId] ?: emptyList(),
                kind = when (kindRows[exerciseId].orEmpty().kind) {
                    SetLogKind.TIME_AND_REPS -> ReportExerciseKind.TIME_AND_REPS
                    SetLogKind.TIME_AND_WEIGHT -> ReportExerciseKind.TIME_AND_WEIGHT
                    SetLogKind.STRENGTH -> ReportExerciseKind.STRENGTH
                }
            )
        } + cardioExercises(planWithDetails, sessions, languageCode)

        val sortedExercises = exercises.sortedByDescending { it.summary.totalSets }

        return PlanReport(
            planId = plan.id,
            planName = plan.nome,
            planNote = plan.note,
            startDate = plan.dataInizio,
            endDate = plan.dataFine,
            totalSessions = sessions.size,
            periodFirstSession = periodFirst,
            periodLastSession = periodLast,
            exercises = sortedExercises,
            weightUnit = weightUnit
        )
    }

    /**
     * Cardio exercises have no set rows: their sets are the cardio logs of each session, one exercise per activity
     * (a session can have it twice, e.g. a warm-up and a cool-down, which both count).
     */
    private fun cardioExercises(planWithDetails: PlanWithDetails, sessions: List<SessionWithDetails>, languageCode: String): List<ExerciseReport> {
        val byActivity = mutableMapOf<String, MutableList<ExerciseSessionEntry>>()
        val names = mutableMapOf<String, String>()
        sessions.forEach { session ->
            session.cardio.filter { it.isCompleted }.groupedByExercise().forEach { cardio ->
                val key = cardio.categoria.trim().lowercase(java.util.Locale.ROOT)
                names.putIfAbsent(key, cardio.categoria.trim())
                byActivity.getOrPut(key) { mutableListOf() }.add(
                    ExerciseSessionEntry(
                        date = session.session.timestamp,
                        sets = cardio.sets.mapIndexed { index, log ->
                            SetEntry(
                                setNumber = index + 1,
                                weight = 0f,
                                reps = 0,
                                rpe = null,
                                isWarmup = false,
                                note = log.note,
                                seconds = log.durataSecondi,
                                distanceKm = log.distanza
                            )
                        }
                    )
                )
            }
        }
        return byActivity.map { (key, entries) ->
            val planExercise = planWithDetails.exercises.firstOrNull { it.exercise.nome.trim().lowercase(java.util.Locale.ROOT) == key }
            val sorted = entries.sortedBy { it.date }
            ExerciseReport(
                // Activities typed by hand have no exercise: a stable id that cannot clash with a real one
                exerciseId = planExercise?.exercise?.id ?: -(key.hashCode() and Int.MAX_VALUE) - 1,
                exerciseName = ExerciseTranslations.translate(names.getValue(key), languageCode),
                muscleGroup = ExerciseTranslations.translateCategory("Cardio", languageCode),
                isCurrentlyInPlan = planExercise != null,
                sessions = sorted,
                summary = calculateSummary(sorted),
                swapEvents = emptyList(),
                kind = ReportExerciseKind.CARDIO
            )
        }
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
            .filter { !it.isWarmup && it.weight > 0 && it.reps > 0 && it.seconds == null }
            .maxOfOrNull { set -> com.emanuel5014.trainable.domain.prescription.LoadCalculator.epley(set.weight, set.reps) }

        val working = allSets.filter { !it.isWarmup }
        return ExerciseSummary(
            firstSessionDate = sessions.first().date,
            lastSessionDate = sessions.last().date,
            totalSets = totalSets,
            maxWeight = maxWeight,
            maxWeightDate = maxWeightDate,
            maxVolume = maxVolume,
            maxVolumeDate = maxVolumeDate,
            bestEstimatedOneRM = bestOneRM,
            bestSeconds = working.maxOfOrNull { it.seconds ?: 0 } ?: 0,
            totalSeconds = working.sumOf { it.seconds ?: 0 },
            bestReps = working.maxOfOrNull { it.reps } ?: 0,
            totalReps = working.sumOf { it.reps },
            totalKm = working.sumOf { (it.distanceKm ?: 0f).toDouble() }.toFloat()
        )
    }
}
