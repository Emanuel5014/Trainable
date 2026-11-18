package com.emanuel5014.trainable.data.report

import com.emanuel5014.trainable.data.local.entity.straightReps
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.CardioExerciseKey
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.belongsTo
import com.emanuel5014.trainable.data.local.entity.exerciseKey
import com.emanuel5014.trainable.data.local.entity.groupedByExercise
import com.emanuel5014.trainable.domain.exercise.CardioNames
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

class ReportGenerator(
    private val workoutRepository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    /** The words of prescriptions in the report's language, asked for each routine. */
    private val reportText: () -> ReportText
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

        val exercisesById = workoutRepository.getExercisesById().first()
        val report = buildReport(planWithDetails, sessions, swaps, languageCode, weightUnit, advanced, exercisesById)
            .copy(distanceUnit = userPreferencesRepository.distanceUnit.first())
        // The workout journal is part of the report only while it is on
        return if (userPreferencesRepository.journalEnabled.first()) report.copy(journal = journalEntries(sessions)) else report
    }

    /** The workouts that have a rating, tags or a note, newest first. */
    private fun journalEntries(sessions: List<SessionWithDetails>): List<JournalEntry> =
        sessions
            .filter { it.session.rating != null || it.tags.isNotEmpty() || !it.session.journalNote.isNullOrBlank() }
            .sortedByDescending { it.session.timestamp }
            .map { details ->
                JournalEntry(
                    date = details.session.timestamp,
                    rating = details.session.rating,
                    tags = details.tags.map { it.name }.sortedBy { it.lowercase() },
                    note = details.session.journalNote?.takeIf { it.isNotBlank() }
                )
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
        advanced: Boolean,
        exercisesById: Map<Int, ExerciseEntity>
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
        val text = reportText()
        val editLabels = text.labels(weightUnit)
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
                        changes = text.programChangeLines(weightUnit, edit.before, edit.after)
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
                    rpe = setWithExercise.setLog.rpe,
                    isWarmup = setWithExercise.setLog.isWarmup,
                    note = setWithExercise.setLog.note,
                    // The prescription with advanced programming on (its RPE has a column of its own); mini-sets, help and partials always
                    planned = if (advanced) {
                        text.setPrescriptionBadges(setWithExercise.setLog)
                    } else emptyList(),
                    performed = text.setPerformanceBadges(setWithExercise.setLog),
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
        } + cardioExercises(planWithDetails, sessions, languageCode, exercisesById, ::programOf)

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
     * (a session can have it twice, e.g. a warm-up and a cool-down, which both count). The activity is the exercise the
     * logs are linked to, named as that exercise; only a row not linked yet falls back to the name written on it.
     */
    private fun cardioExercises(
        planWithDetails: PlanWithDetails,
        sessions: List<SessionWithDetails>,
        languageCode: String,
        exercisesById: Map<Int, ExerciseEntity>,
        programOf: (PlanExerciseWithDetails) -> ExerciseProgram
    ): List<ExerciseReport> {
        val byActivity = linkedMapOf<CardioExerciseKey, MutableList<ExerciseSessionEntry>>()
        val firstRows = mutableMapOf<CardioExerciseKey, CardioLogEntity>()
        sessions.forEach { session ->
            session.cardio.filter { it.isCompleted }.groupedByExercise().forEach { cardio ->
                val key = cardio.first.exerciseKey()
                firstRows.getOrPut(key) { cardio.first }
                byActivity.getOrPut(key) { mutableListOf() }.add(
                    ExerciseSessionEntry(
                        date = session.session.timestamp,
                        week = session.session.programWeek,
                        sets = cardio.sets.mapIndexed { index, log ->
                            SetEntry(
                                setNumber = index + 1,
                                weight = 0f,
                                reps = 0,
                                rpe = log.rpe,
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
            val row = firstRows.getValue(key)
            val planExercise = planWithDetails.exercises.firstOrNull { row.belongsTo(it.exercise) }
            val sorted = entries.sortedBy { it.date }
            ExerciseReport(
                // A row not linked yet has no exercise: a stable id that cannot clash with a real one
                exerciseId = row.exerciseId ?: planExercise?.exercise?.id
                    ?: -((key as CardioExerciseKey.Unlinked).foldedName.hashCode() and Int.MAX_VALUE) - 1,
                exerciseName = CardioNames.of(row, exercisesById, languageCode),
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
