package com.emanuel5014.trainable.data.report

import com.emanuel5014.trainable.data.local.entity.straightReps
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.groupedByExercise
import com.emanuel5014.trainable.data.local.entity.kind
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.SessionExerciseSwapEntity
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.TIME_AND_WEIGHT
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.PlanWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.RepMode
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

        // What was changed in the prescription of an exercise along the way, with the units and language of the report
        val editContext = localeManager.localizedContext()
        val editLabels = com.emanuel5014.trainable.ui.components.prescriptionLabels(editContext, weightUnit)
        val programEditsByExercise = mutableMapOf<Int, MutableList<ProgramEditEvent>>()
        sessions.forEach { session ->
            session.prescriptionEdits.forEach { edit ->
                programEditsByExercise.getOrPut(edit.exerciseId) { mutableListOf() }.add(
                    ProgramEditEvent(
                        sessionDate = session.session.timestamp,
                        createdAt = edit.createdAt,
                        week = edit.programWeek,
                        kind = edit.editKind,
                        scope = edit.editScope,
                        before = blockLines(edit.before, editLabels),
                        after = blockLines(edit.after, editLabels),
                        changes = com.emanuel5014.trainable.ui.components.programChangeLines(editContext, weightUnit, edit.before, edit.after)
                    )
                )
            }
        }

        val weekByDate = sessions.associate { it.session.timestamp to it.session.programWeek }
        val weeksCount = plan.weeksCount.coerceAtLeast(1)
        val planExerciseByExercise = planWithDetails.exercises.groupBy { it.exercise.id }.mapValues { it.value.first() }
        fun programOf(planExercise: PlanExerciseWithDetails) = ReportProgram.of(planExercise, weeksCount, advanced, editLabels)

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
                    // The prescription with advanced programming on (its RPE has a column of its own); mini-sets, help and partials always
                    planned = if (advanced) {
                        com.emanuel5014.trainable.ui.components.setLogPrescriptionBadges(editContext, setWithExercise.setLog.copy(rpe = null))
                    } else emptyList(),
                    performed = com.emanuel5014.trainable.ui.components.setLogPerformanceBadges(editContext, setWithExercise.setLog),
                    seconds = setWithExercise.setLog.durataSecondi,
                    straightReps = setWithExercise.setLog.straightReps,
                    // The reps it was done against: a plain set only has one when it had a range. The reps of a "total
                    // reps" block are what was left of the total, not a goal of the set
                    targetReps = setWithExercise.setLog.targetReps.takeIf {
                        setWithExercise.setLog.repMode != RepMode.TOTAL.code && (advanced || setWithExercise.setLog.blockIndex == null)
                    },
                    isExtra = setWithExercise.setLog.isExtra
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

            val planExercise = planExerciseByExercise[exerciseId]
            val sortedSessions = sessionMap.entries
                .sortedBy { it.key }
                .map { (date, sets) ->
                    val ordered = sets.sortedBy { it.setNumber }
                    ExerciseSessionEntry(
                        date = date,
                        sets = ordered,
                        week = weekByDate[date],
                        adherence = ReportProgram.adherence(planExercise, advanced, weekByDate[date], weeksCount, ordered)
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
                programEdits = programEditsByExercise[exerciseId].orEmpty().sortedWith(compareBy({ it.sessionDate }, { it.createdAt })),
                kind = when (kindRows[exerciseId].orEmpty().kind) {
                    SetLogKind.TIME_AND_REPS -> ReportExerciseKind.TIME_AND_REPS
                    SetLogKind.TIME_AND_WEIGHT -> ReportExerciseKind.TIME_AND_WEIGHT
                    SetLogKind.STRENGTH -> ReportExerciseKind.STRENGTH
                },
                program = planExercise?.let(::programOf)
            )
        } + cardioExercises(planWithDetails, sessions, languageCode, ::programOf)

        // What the sheet asks for and was never done at all is the most telling line of the report
        val done = exercises.map { it.exerciseId }.toSet()
        val neverDone = planWithDetails.exercises.filter { it.exercise.id !in done }.map { planExercise ->
            ExerciseReport(
                exerciseId = planExercise.exercise.id,
                exerciseName = ExerciseTranslations.translate(planExercise.exercise.nome, languageCode),
                muscleGroup = ExerciseTranslations.translateCategory(planExercise.exercise.categoria, languageCode),
                isCurrentlyInPlan = true,
                sessions = emptyList(),
                summary = calculateSummary(emptyList()),
                swapEvents = swapEventsByExercise[planExercise.exercise.id] ?: emptyList(),
                programEdits = programEditsByExercise[planExercise.exercise.id].orEmpty().sortedWith(compareBy({ it.sessionDate }, { it.createdAt })),
                kind = when {
                    planExercise.planExercise.exerciseType == "cardio" -> ReportExerciseKind.CARDIO
                    planExercise.planExercise.exerciseType == TIME_AND_REPS -> ReportExerciseKind.TIME_AND_REPS
                    planExercise.planExercise.exerciseType == TIME_AND_WEIGHT -> ReportExerciseKind.TIME_AND_WEIGHT
                    else -> ReportExerciseKind.STRENGTH
                },
                program = programOf(planExercise)
            )
        }

        val sortedExercises = exercises.sortedByDescending { it.summary.totalSets } + neverDone

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
            weightUnit = weightUnit,
            weeksCount = weeksCount,
            currentWeek = plan.currentWeek.coerceIn(1, weeksCount),
            weeks = ReportProgram.weekSummaries(weeksCount, sessions.map { it.session.programWeek to it.session.timestamp })
        )
    }

    /** The blocks of a prescription as lines of text, one each ("75% 4×5 Pause 2″"). */
    private fun blockLines(blocks: List<PrescriptionBlock>, labels: PrescriptionLabels): List<String> =
        blocks.map { PrescriptionFormatter.summary(listOf(it), labels) }

    /**
     * Cardio exercises have no set rows: their sets are the cardio logs of each session, one exercise per activity
     * (a session can have it twice, e.g. a warm-up and a cool-down, which both count).
     */
    private fun cardioExercises(
        planWithDetails: PlanWithDetails,
        sessions: List<SessionWithDetails>,
        languageCode: String,
        programOf: (PlanExerciseWithDetails) -> ExerciseProgram
    ): List<ExerciseReport> {
        val byActivity = mutableMapOf<String, MutableList<ExerciseSessionEntry>>()
        val names = mutableMapOf<String, String>()
        sessions.forEach { session ->
            session.cardio.filter { it.isCompleted }.groupedByExercise().forEach { cardio ->
                val key = cardio.categoria.trim().lowercase(java.util.Locale.ROOT)
                names.putIfAbsent(key, cardio.categoria.trim())
                byActivity.getOrPut(key) { mutableListOf() }.add(
                    ExerciseSessionEntry(
                        date = session.session.timestamp,
                        week = session.session.programWeek,
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
                kind = ReportExerciseKind.CARDIO,
                program = planExercise?.let(programOf)
            )
        }
    }

    /** The warm-ups are not shown in the report, so they stay out of its figures too (the sets, the best weight, the volume). */
    private fun calculateSummary(allSessions: List<ExerciseSessionEntry>): ExerciseSummary {
        val sessions = allSessions
            .map { it.copy(sets = it.sets.filter { set -> !set.isWarmup }) }
            .filter { it.sets.isNotEmpty() }
        if (sessions.isEmpty()) {
            return ExerciseSummary(0L, 0L, 0, 0f, 0L, 0f, 0L, null)
        }

        val working = sessions.flatMap { it.sets }
        val totalSets = working.size

        val maxWeightSet = working.maxByOrNull { it.weight }
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

        val bestOneRM = working
            .filter { it.weight > 0 && it.straightReps > 0 && it.seconds == null }
            .maxOfOrNull { set -> com.emanuel5014.trainable.domain.prescription.LoadCalculator.epley(set.weight, set.straightReps) }

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
