package com.emanuel5014.trainable.ui.screens.analytics

import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.dao.CategoryVolumeRow
import com.emanuel5014.trainable.data.local.dao.ConsistencyRow
import com.emanuel5014.trainable.data.local.dao.PeriodExerciseRow
import com.emanuel5014.trainable.data.local.dao.PeriodMetrics
import com.emanuel5014.trainable.data.local.dao.PersonalBestRow
import com.emanuel5014.trainable.data.local.entity.ExerciseMergeEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.ExerciseMergeRepository
import com.emanuel5014.trainable.data.repository.KeyValueStore
import com.emanuel5014.trainable.platform.formatLocalDecimals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.exercise.ExerciseMerges
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
/**
 * The analytics tab's state, the same on every platform: each one gives it its choices ([prefs]), its words ([text]),
 * the language exercise names are shown in ([languageCodes]) and its own lifetime ([scope]).
 */
class AnalyticsState(
    private val analyticsRepository: AnalyticsRepository,
    private val workoutRepository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val mergeRepository: ExerciseMergeRepository,
    private val prefs: KeyValueStore,
    private val text: AnalyticsText,
    languageCodes: Flow<String>,
    private val scope: CoroutineScope
) {

    
    private val selectedTimeRange = MutableStateFlow(AnalyticsTimeRange.OneMonth)
    private val bodyWeightInput = MutableStateFlow("")
    private val selectedExerciseIds = MutableStateFlow<Set<Int>>(loadSavedExerciseIds())
    private val widgetOrder = MutableStateFlow<List<String>>(loadWidgetOrder())
    private val categoryVolumeTimeRange = MutableStateFlow(loadCategoryVolumeTimeRange())
    private val periodComparisonRange = MutableStateFlow(AnalyticsTimeRange.OneMonth)
    private val bodyWeightTimeRange = MutableStateFlow(loadBodyWeightTimeRange())
    private val showProgressCards = MutableStateFlow(loadShowProgressCards())

    private fun loadShowProgressCards(): Boolean {
        return prefs.getBoolean("show_progress_cards", false)
    }

    private fun saveShowProgressCards(value: Boolean) {
        prefs.putBoolean("show_progress_cards", value)
    }

    private fun loadSavedExerciseIds(): Set<Int> {
        val saved = prefs.getStringSet("selected_exercise_ids", emptySet()) ?: emptySet()
        return saved.mapNotNull { it.toIntOrNull() }.toSet()
    }

    private fun saveExerciseIds(ids: Set<Int>) {
        prefs.putStringSet("selected_exercise_ids", ids.map { it.toString() }.toSet())
    }

    private fun loadWidgetOrder(): List<String> {
        val saved = prefs.getString("widget_order", null)
        return saved?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    private fun saveWidgetOrder(order: List<String>) {
        prefs.putString("widget_order", order.joinToString(","))
    }

    private fun loadCategoryVolumeTimeRange(): AnalyticsTimeRange {
        val name = prefs.getString("category_volume_time_range", AnalyticsTimeRange.OneWeek.name)
            ?: AnalyticsTimeRange.OneWeek.name
        return try { AnalyticsTimeRange.valueOf(name) } catch (_: IllegalArgumentException) { AnalyticsTimeRange.OneWeek }
    }

    private fun saveCategoryVolumeTimeRange(timeRange: AnalyticsTimeRange) {
        prefs.putString("category_volume_time_range", timeRange.name)
    }

    private fun loadBodyWeightTimeRange(): AnalyticsTimeRange {
        val name = prefs.getString("body_weight_time_range", AnalyticsTimeRange.OneMonth.name)
            ?: AnalyticsTimeRange.OneMonth.name
        return try { AnalyticsTimeRange.valueOf(name) } catch (_: IllegalArgumentException) { AnalyticsTimeRange.OneMonth }
    }

    private fun saveBodyWeightTimeRange(timeRange: AnalyticsTimeRange) {
        prefs.putString("body_weight_time_range", timeRange.name)
    }

    private val activePlanFlow = workoutRepository.getActivePlans()
        .map { plans -> plans.firstOrNull() }

    private val allPlansFlow = workoutRepository.getAllPlans()

    private val analyticsSnapshotFlow = combine(
        activePlanFlow,
        allPlansFlow,
        selectedTimeRange,
        selectedExerciseIds,
        widgetOrder,
        userPreferencesRepository.weightUnit,
        languageCodes,
        categoryVolumeTimeRange,
        periodComparisonRange,
        bodyWeightTimeRange,
        mergeRepository.observeMerges()
    ) { args ->
        val activePlan = args[0] as WorkoutPlanEntity?
        @Suppress("UNCHECKED_CAST")
        val allPlans = args[1] as List<WorkoutPlanEntity>
        val timeRange = args[2] as AnalyticsTimeRange
        @Suppress("UNCHECKED_CAST")
        val selectedIds = args[3] as Set<Int>
        @Suppress("UNCHECKED_CAST")
        val order = args[4] as List<String>
        val weightUnit = args[5] as String
        val userLang = args[6] as String
        val catVolumeTimeRange = args[7] as AnalyticsTimeRange
        val pRange = args[8] as AnalyticsTimeRange
        val bwTimeRange = args[9] as AnalyticsTimeRange
        @Suppress("UNCHECKED_CAST")
        val merges = args[10] as List<ExerciseMergeEntity>
        
        val languageCode = userLang
        
        val categoryVolumeStartDate = when (catVolumeTimeRange) {
            AnalyticsTimeRange.OneWeek -> AnalyticsCalendar.startOfWeek()
            AnalyticsTimeRange.OneMonth -> AnalyticsCalendar.startOfMonth()
            AnalyticsTimeRange.SixMonths -> AnalyticsCalendar.startOfSixMonths()
            AnalyticsTimeRange.All -> 0L
        }

        val bodyWeightStartDate = bwTimeRange.startDate()

        AnalyticsQueryContext(
            activePlan = activePlan,
            allPlans = allPlans,
            timeRange = timeRange,
            startDate = timeRange.startDate(),
            selectedExerciseIds = selectedIds,
            widgetOrder = order,
            weightUnit = weightUnit,
            languageCode = languageCode,
            categoryVolumeStartDate = categoryVolumeStartDate,
            categoryVolumeTimeRange = catVolumeTimeRange,
            bodyWeightTimeRange = bwTimeRange,
            bodyWeightStartDate = bodyWeightStartDate,
            period1Range = pRange,
            period2Range = pRange,
            merges = merges
        )
    }.flatMapLatest { context ->
        val consistencyFlow = context.activePlan?.let {
            analyticsRepository.getConsistency(it.id, context.startDate)
        } ?: flowOf(null)

        val volumeFlow = combine(
            analyticsRepository.getTotalVolumeSince(context.startDate),
            analyticsRepository.getVolumeHistory(context.startDate)
        ) { values: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            val totalVolume = values[0] as Float?
            @Suppress("UNCHECKED_CAST")
            val volumeHistory = values[1] as List<com.emanuel5014.trainable.data.local.dao.DailyVolume>
            VolumeAnalyticsSnapshot(
                totalVolume = totalVolume ?: 0f,
                volumeHistory = volumeHistory
            )
        }

        val bestsFlow = combine(
            analyticsRepository.getAllPersonalBests(),
            consistencyFlow
        ) { values: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            val personalBests = values[0] as List<PersonalBestRow>
            @Suppress("UNCHECKED_CAST")
            val consistency = values[1] as ConsistencyRow?
            BestsConsistencySnapshot(
                personalBests = personalBests,
                consistency = consistency
            )
        }

        val coreFlow = combine(volumeFlow, bestsFlow) { values: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            val volume = values[0] as VolumeAnalyticsSnapshot
            @Suppress("UNCHECKED_CAST")
            val bests = values[1] as BestsConsistencySnapshot
            CoreAnalyticsSnapshot(
                totalVolume = volume.totalVolume,
                volumeHistory = volume.volumeHistory,
                personalBests = bests.personalBests,
                consistency = bests.consistency
            )
        }

        val strengthFlow = combine(
            analyticsRepository.getStrengthIndex(context.startDate),
            analyticsRepository.getVolumeByCategory(context.categoryVolumeStartDate)
        ) { values: Array<Any?> ->
            @Suppress("UNCHECKED_CAST")
            val strengthIndex = values[0] as Float?
            @Suppress("UNCHECKED_CAST")
            val categoryVolumes = values[1] as List<CategoryVolumeRow>
            StrengthCategorySnapshot(
                strengthIndex = strengthIndex,
                categoryVolumes = categoryVolumes
            )
        }

        val weightFlow = analyticsRepository.getWeightHistory(context.bodyWeightStartDate)
        val sessionsFlow = workoutRepository.getAllSessions()
        
        val exerciseChartFlows = context.widgetOrder
            .filter { it.startsWith("exercise_") }
            .map { idStr ->
                val parts = idStr.split("_")
                val exerciseId = ExerciseMerges.widgetExerciseId(idStr, context.merges) ?: parts[1].toInt()
                val timeRange = if (parts.size >= 3) {
                    try { AnalyticsTimeRange.valueOf(parts[2]) } catch (_: Exception) { AnalyticsTimeRange.OneMonth }
                } else {
                    AnalyticsTimeRange.OneMonth
                }
                analyticsRepository.getExerciseProgressHistory(exerciseId, timeRange.startDate())
                    .map { history -> idStr to history }
            }

        val exerciseHistoriesFlow = if (exerciseChartFlows.isEmpty()) {
            flowOf(emptyMap<String, List<com.emanuel5014.trainable.data.local.dao.DailyExerciseMax>>())
        } else {
            combine(exerciseChartFlows) { pairs ->
                pairs.associate { it.first to it.second }
            }
        }

        val volumeChartFlows = context.widgetOrder
            .filter { it.startsWith("volume_") }
            .map { idStr ->
                val parts = idStr.split("_")
                val planId = parts[1].toInt()
                val timeRange = AnalyticsTimeRange.valueOf(parts[2])
                analyticsRepository.getVolumeHistoryForPlan(planId, timeRange.startDate())
                    .map { history -> idStr to history }
            }

        val volumeHistoriesFlow = if (volumeChartFlows.isEmpty()) {
            flowOf(emptyMap<String, List<com.emanuel5014.trainable.data.local.dao.DailyVolume>>())
        } else {
            combine(volumeChartFlows) { pairs ->
                pairs.associate { it.first to it.second }
            }
        }

        val period1StartDate = context.period1Range.startDate()
        val period1EndDate = Clock.System.now().toEpochMilliseconds()
        val period2StartDate = context.period2Range.startDate(period1StartDate)
        val period2EndDate = period1StartDate

        val period1MetricsFlow = analyticsRepository.getPeriodMetrics(period1StartDate, period1EndDate)
        val period2MetricsFlow = analyticsRepository.getPeriodMetrics(period2StartDate, period2EndDate)
        val period1ExercisesFlow = analyticsRepository.getPeriodExerciseBreakdown(period1StartDate, period1EndDate)
        val period2ExercisesFlow = analyticsRepository.getPeriodExerciseBreakdown(period2StartDate, period2EndDate)
        val period1TrainingDaysFlow = analyticsRepository.getTrainingDays(period1StartDate, period1EndDate)
        val period2TrainingDaysFlow = analyticsRepository.getTrainingDays(period2StartDate, period2EndDate)

        val timePeriodComparisonFlow = combine(
            combine(period1MetricsFlow, period2MetricsFlow) { p1, p2 -> p1 to p2 },
            combine(period1ExercisesFlow, period2ExercisesFlow) { p1, p2 -> p1 to p2 },
            combine(period1TrainingDaysFlow, period2TrainingDaysFlow) { p1, p2 -> p1 to p2 }
        ) { metricsPair, exercisesPair, daysPair ->
            val (p1Metrics, p2Metrics) = metricsPair
            val (p1Exercises, p2Exercises) = exercisesPair
            val (p1Days, p2Days) = daysPair
            TimePeriodComparisonSnapshot(
                period1Metrics = PeriodComparisonMetrics(
                    volume = p1Metrics.volume,
                    sessionCount = p1Metrics.sessionCount,
                    setCount = p1Metrics.setCount,
                    avgWeight = p1Metrics.avgWeight,
                    trainingDays = p1Days
                ),
                period2Metrics = PeriodComparisonMetrics(
                    volume = p2Metrics.volume,
                    sessionCount = p2Metrics.sessionCount,
                    setCount = p2Metrics.setCount,
                    avgWeight = p2Metrics.avgWeight,
                    trainingDays = p2Days
                ),
                period1Exercises = p1Exercises.map { PeriodExerciseComparison(ExerciseTranslations.translate(it.exerciseName, context.languageCode), it.volume, it.setCount, it.maxWeight, it.max1rm) },
                period2Exercises = p2Exercises.map { PeriodExerciseComparison(ExerciseTranslations.translate(it.exerciseName, context.languageCode), it.volume, it.setCount, it.maxWeight, it.max1rm) }
            )
        }

        coreFlow.flatMapLatest { core ->
            combine(
                combine(strengthFlow, weightFlow) { s, w -> s to w },
                combine(sessionsFlow, exerciseHistoriesFlow) { s, e -> s to e },
                combine(volumeHistoriesFlow, timePeriodComparisonFlow) { v, t -> v to t }
            ) { pair1, pair2, pair3 ->
                val (strengthCategory, weightHistory) = pair1
                val (sessions, exerciseHistories) = pair2
                val (volumeHistories, timePeriodComparison) = pair3
                buildAnalyticsState(
                    activePlanName = context.activePlan?.nome ?: "No Active Plan",
                    allPlans = context.allPlans,
                    timeRange = context.timeRange,
                    startDate = context.startDate,
                    totalVolume = core.totalVolume,
                    volumeHistory = core.volumeHistory,
                    personalBests = core.personalBests,
                    selectedExerciseIds = context.selectedExerciseIds,
                    widgetOrder = context.widgetOrder,
                    consistency = core.consistency,
                    strengthIndex = strengthCategory.strengthIndex,
                    categoryVolumes = strengthCategory.categoryVolumes,
                    categoryVolumeTimeRange = context.categoryVolumeTimeRange,
                    categoryVolumeStartDate = context.categoryVolumeStartDate,
                    weightHistory = weightHistory,
                    sessions = sessions,
                    exerciseHistories = exerciseHistories,
                    volumeHistories = volumeHistories,
                    weightUnit = context.weightUnit,
                    languageCode = context.languageCode,
                    timePeriodComparison = timePeriodComparison,
                    period1Range = context.period1Range,
                    period2Range = context.period2Range,
                    bodyWeightTimeRange = context.bodyWeightTimeRange,
                    merges = context.merges
                )
            }
        }
    }.catch { throwable ->
        emit(
            AnalyticsUiState(
                isLoading = false,
                error = throwable.message ?: "Errore sconosciuto"
            )
        )
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        analyticsSnapshotFlow,
        bodyWeightInput,
        showProgressCards
    ) { snapshot, input, showCards ->
        snapshot.copy(bodyWeightInput = input, showProgressCards = showCards)
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState()
    )

    fun selectTimeRange(timeRange: AnalyticsTimeRange) {
        selectedTimeRange.value = timeRange
    }

    fun onBodyWeightInputChanged(value: String) {
        bodyWeightInput.value = value
    }

    fun toggleExerciseSelection(exerciseId: Int) {
        selectedExerciseIds.update { current ->
            val newSet = if (current.contains(exerciseId)) {
                current - exerciseId
            } else {
                current + exerciseId
            }
            saveExerciseIds(newSet)
            newSet
        }
    }

    fun addExerciseChart(exerciseId: Int, timeRange: AnalyticsTimeRange = AnalyticsTimeRange.OneMonth) {
        addMultipleExerciseCharts(setOf(exerciseId), timeRange)
    }

    fun addMultipleExerciseCharts(exerciseIds: Set<Int>, timeRange: AnalyticsTimeRange = AnalyticsTimeRange.OneMonth) {
        widgetOrder.update { current ->
            var newList = current
            val timestamp = Clock.System.now().toEpochMilliseconds()
            exerciseIds.forEachIndexed { index, id ->
                val widgetId = "exercise_${id}_${timeRange.name}_${timestamp + index}"
                if (!newList.any { it == "exercise_$id" || it.startsWith("exercise_${id}_") }) {
                    newList = newList + widgetId
                }
            }
            saveWidgetOrder(newList)
            newList
        }
    }

    fun updateExerciseChart(widgetId: String, timeRange: AnalyticsTimeRange) {
        widgetOrder.update { current ->
            val index = current.indexOf(widgetId)
            if (index == -1) return@update current
            val parts = widgetId.split("_")
            val exerciseId = parts[1].toInt()
            val suffix = if (parts.size >= 4) parts[3] else "${Clock.System.now().toEpochMilliseconds()}"
            val newId = "exercise_${exerciseId}_${timeRange.name}_$suffix"
            val newList = current.toMutableList()
            newList[index] = newId
            saveWidgetOrder(newList)
            newList
        }
    }

    fun addBodyWeightChart() {
        saveBodyWeightTimeRange(bodyWeightTimeRange.value)
        widgetOrder.update { current ->
            if (current.contains("weight")) return@update current
            val newList = current + "weight"
            saveWidgetOrder(newList)
            newList
        }
    }

    fun updateBodyWeightChart(timeRange: AnalyticsTimeRange) {
        saveBodyWeightTimeRange(timeRange)
        bodyWeightTimeRange.value = timeRange
    }

    fun addCalendarChart() {
        widgetOrder.update { current ->
            if (current.contains("calendar")) return@update current
            val newList = current + "calendar"
            saveWidgetOrder(newList)
            newList
        }
    }

    fun addCategoryVolumeChart(timeRange: AnalyticsTimeRange) {
        saveCategoryVolumeTimeRange(timeRange)
        categoryVolumeTimeRange.value = timeRange
        widgetOrder.update { current ->
            if (current.contains("category_volume")) return@update current
            val newList = current + "category_volume"
            saveWidgetOrder(newList)
            newList
        }
    }

    fun updateCategoryVolumeChart(timeRange: AnalyticsTimeRange) {
        saveCategoryVolumeTimeRange(timeRange)
        categoryVolumeTimeRange.value = timeRange
    }

    fun addVolumeChart(planId: Int, timeRange: AnalyticsTimeRange) {
        widgetOrder.update { current ->
            val id = "volume_${planId}_${timeRange.name}_${Clock.System.now().toEpochMilliseconds()}"
            val newList = current + id
            saveWidgetOrder(newList)
            newList
        }
    }

    fun updateVolumeChart(widgetId: String, planId: Int, timeRange: AnalyticsTimeRange) {
        widgetOrder.update { current ->
            val index = current.indexOf(widgetId)
            if (index == -1) return@update current
            val newId = "volume_${planId}_${timeRange.name}_${widgetId.split("_").last()}"
            val newList = current.toMutableList()
            newList[index] = newId
            saveWidgetOrder(newList)
            newList
        }
    }

    fun removeAllExerciseCharts() {
        widgetOrder.update { current ->
            val newList = current.filter { !it.startsWith("exercise_") }
            saveWidgetOrder(newList)
            newList
        }
    }

    fun removeWidget(id: String) {
        widgetOrder.update { current ->
            val newList = current.filter { it != id }
            saveWidgetOrder(newList)
            newList
        }
    }

    /**
     * Applies the order the user dropped the shown widgets in. Ids that are saved but not shown
     * keep their place; the shown ones are laid back into the slots the shown ones used to hold.
     */
    fun reorderWidgets(orderedIds: List<String>) {
        widgetOrder.update { current ->
            val shown = orderedIds.filter { it in current }
            val slots = current.indices.filter { current[it] in shown }
            if (slots.size != shown.size) return@update current

            val newList = current.toMutableList()
            slots.forEachIndexed { i, slot -> newList[slot] = shown[i] }
            if (newList == current) return@update current
            saveWidgetOrder(newList)
            newList
        }
    }

    fun toggleProgressCards() {
        showProgressCards.update { current ->
            val new = !current
            saveShowProgressCards(new)
            new
        }
    }

    fun addTimePeriodComparison(range: AnalyticsTimeRange) {
        periodComparisonRange.value = range
        widgetOrder.update { current ->
            if (current.contains("time_period_comparison")) return@update current
            val newList = current + "time_period_comparison"
            saveWidgetOrder(newList)
            newList
        }
    }

    fun updateTimePeriodComparison(range: AnalyticsTimeRange) {
        periodComparisonRange.value = range
    }

    fun clearExerciseSelection() {
        selectedExerciseIds.value = emptySet()
        saveExerciseIds(emptySet())
    }

    fun submitWeight(timestamp: Long = Clock.System.now().toEpochMilliseconds()) {
        val parsedWeight = bodyWeightInput.value.replace(',', '.').toFloatOrNull() ?: return
        val weightUnit = uiState.value.weightUnit

        scope.launch {
            analyticsRepository.addWeightLog(
                userId = 1,
                peso = WeightUnitConverter.convertStorage(parsedWeight, weightUnit),
                timestamp = timestamp
            )
            bodyWeightInput.value = ""
        }
    }

    fun deleteWeightLog(id: Int) {
        scope.launch {
            analyticsRepository.deleteWeightLog(id)
        }
    }

    private fun buildAnalyticsState(
        activePlanName: String,
        allPlans: List<WorkoutPlanEntity>,
        timeRange: AnalyticsTimeRange,
        startDate: Long,
        totalVolume: Float,
        volumeHistory: List<com.emanuel5014.trainable.data.local.dao.DailyVolume>,
        personalBests: List<PersonalBestRow>,
        selectedExerciseIds: Set<Int>,
        widgetOrder: List<String>,
        consistency: ConsistencyRow?,
        strengthIndex: Float?,
        categoryVolumes: List<CategoryVolumeRow>,
        categoryVolumeTimeRange: AnalyticsTimeRange,
        categoryVolumeStartDate: Long,
        weightHistory: List<com.emanuel5014.trainable.data.local.entity.WeightLogEntity>,
        sessions: List<WorkoutSessionEntity>,
        exerciseHistories: Map<String, List<com.emanuel5014.trainable.data.local.dao.DailyExerciseMax>>,
        volumeHistories: Map<String, List<com.emanuel5014.trainable.data.local.dao.DailyVolume>>,
        weightUnit: String,
        languageCode: String,
        timePeriodComparison: TimePeriodComparisonSnapshot,
        period1Range: AnalyticsTimeRange,
        period2Range: AnalyticsTimeRange,
        bodyWeightTimeRange: AnalyticsTimeRange,
        merges: List<ExerciseMergeEntity>
    ): AnalyticsUiState {
        val completedSessions = consistency?.completedSessions ?: 0
        val targetSessionsPerWeek = consistency?.targetSessionsPerWeek ?: 0
        val weeksInRange = max(
            1f,
            (Clock.System.now().toEpochMilliseconds() - startDate).toFloat() / (7L * 24 * 60 * 60 * 1000).toFloat()
        )
        val expectedSessions = (targetSessionsPerWeek * weeksInRange).roundToInt().coerceAtLeast(1)
        val consistencyProgress = if (expectedSessions > 0) {
            (completedSessions.toFloat() / expectedSessions.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Map all personal bests
        val allBests = personalBests.map { row ->
            PersonalBestUiModel(
                exerciseId = row.exerciseId,
                exerciseName = ExerciseTranslations.translate(row.exerciseName, languageCode),
                category = ExerciseTranslations.translateCategory(row.category, languageCode),
                maxWeightKg = row.maxWeight,
                reps = row.reps
            )
        }

        // Extract finished session timestamps
        val finishedSessionDates = sessions
            .filter { it.isFinished }
            .map { it.timestamp }

        val widgets = widgetOrder.mapNotNull { id ->
            when {
                id == "weight" -> {
                    AnalyticsWidget.BodyWeight(
                        history = weightHistory.map { entry ->
                            AnalyticsChartPoint(
                                timestamp = entry.timestamp,
                                value = WeightUnitConverter.convertDisplay(entry.pesoCorporeo, weightUnit),
                                id = entry.id
                            )
                        },
                        timeRange = bodyWeightTimeRange
                    )
                }
                id == "calendar" -> {
                    AnalyticsWidget.Calendar(workoutDates = finishedSessionDates)
                }
                id.startsWith("exercise_") -> {
                    val parts = id.split("_")
                    val exerciseId = ExerciseMerges.widgetExerciseId(id, merges) ?: parts[1].toInt()
                    val timeRangeWidget = if (parts.size >= 3) {
                        try { AnalyticsTimeRange.valueOf(parts[2]) } catch (_: Exception) { AnalyticsTimeRange.OneMonth }
                    } else {
                        AnalyticsTimeRange.OneMonth
                    }
                    val exerciseName = allBests.find { it.exerciseId == exerciseId }?.exerciseName ?: "Unknown"
                    val history = exerciseHistories[id]?.map { point ->
                        AnalyticsChartPoint(
                            timestamp = point.timestamp,
                            value = WeightUnitConverter.convertDisplay(point.maxValue, weightUnit)
                        )
                    } ?: emptyList()
                    AnalyticsWidget.Exercise(
                        widgetId = id,
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
                        timeRange = timeRangeWidget,
                        history = history
                    )
                }
                id.startsWith("volume_") -> {
                    val parts = id.split("_")
                    val planId = parts[1].toInt()
                    val timeRangeWidget = AnalyticsTimeRange.valueOf(parts[2])
                    val planName = allPlans.find { it.id == planId }?.nome ?: "Unknown Plan"
                    val history = volumeHistories[id]?.map { point ->
                        AnalyticsChartPoint(
                            timestamp = point.timestamp,
                            value = WeightUnitConverter.convertDisplay(point.volume, weightUnit)
                        )
                    } ?: emptyList()
                    AnalyticsWidget.Volume(
                        widgetId = id,
                        planId = planId,
                        planName = planName,
                        timeRange = timeRangeWidget,
                        history = history
                    )
                }
                id == "category_volume" -> {
                    AnalyticsWidget.CategoryVolume(
                        history = categoryVolumes.map { row ->
                            CategoryVolumeRow(
                                category = ExerciseTranslations.translateCategory(row.category, languageCode),
                                volume = row.volume
                            )
                        },
                        timeRange = categoryVolumeTimeRange,
                        startDate = categoryVolumeStartDate
                    )
                }
                id == "time_period_comparison" -> {
                    val period1StartDate = period1Range.startDate()
                    val period1EndDate = Clock.System.now().toEpochMilliseconds()
                    val period2StartDate = period2Range.startDate(period1StartDate)
                    val period2EndDate = period1StartDate

                    val p1RangeLabel = "${text.shortDate(period1StartDate)} - ${text.shortDate(period1EndDate)}"
                    val p2RangeLabel = "${text.shortDate(period2StartDate)} - ${text.shortDate(period2EndDate)}"

                    val p1 = timePeriodComparison.period1Metrics
                    val p2 = timePeriodComparison.period2Metrics

                    fun deltaPercent(current: Float, previous: Float): Float =
                        if (previous != 0f) ((current - previous) / previous) * 100f else 0f

                    val volumeDelta = deltaPercent(p1.volume, p2.volume)
                    val sessionsDelta = deltaPercent(p1.sessionCount.toFloat(), p2.sessionCount.toFloat())
                    val setsDelta = deltaPercent(p1.setCount.toFloat(), p2.setCount.toFloat())
                    val trainingDaysDelta = deltaPercent(p1.trainingDays.toFloat(), p2.trainingDays.toFloat())
                    val avgWeightDelta = deltaPercent(p1.avgWeight, p2.avgWeight)

                    val summaryParts = listOf(
                        SummaryPart(text.compareVolume, volumeDelta, volumeDelta >= 0),
                        SummaryPart(text.compareSessions, sessionsDelta, sessionsDelta >= 0),
                        SummaryPart(text.trainingDays, trainingDaysDelta, trainingDaysDelta >= 0)
                    )

                    AnalyticsWidget.TimePeriodComparison(
                        period1Name = when (period1Range) {
                            AnalyticsTimeRange.OneWeek -> text.thisWeek
                            AnalyticsTimeRange.OneMonth -> text.thisMonth
                            else -> text.timeRange(period1Range)
                        },
                        period2Name = "${text.previous} (${text.timeRange(period2Range)})",
                        period1DateRange = p1RangeLabel,
                        period2DateRange = p2RangeLabel,
                        period1Metrics = p1,
                        period2Metrics = p2,
                        period1Exercises = timePeriodComparison.period1Exercises,
                        period2Exercises = timePeriodComparison.period2Exercises,
                        summaryParts = summaryParts,
                        timeRange = period1Range
                    )
                }
                else -> null
            }
        }

        // No automatic selection - let user choose freely from the picker
        val finalSelectedIds = selectedExerciseIds

        // Filter plans to show only active, non-system ones
        val filteredPlans = allPlans.filter { it.isActive && it.note != "SYSTEM_PLAN" }

        return AnalyticsUiState(
            activePlanName = activePlanName,
            allPlans = filteredPlans,
            selectedTimeRange = timeRange,
            totalVolumeKg = totalVolume,
            volumeHistory = volumeHistory.map { point ->
                AnalyticsChartPoint(
                    timestamp = point.timestamp,
                    value = WeightUnitConverter.convertDisplay(point.volume, weightUnit)
                )
            },
            consistency = ConsistencyUiModel(
                completedSessions = completedSessions,
                targetSessions = expectedSessions,
                progress = consistencyProgress,
                summary = buildConsistencySummary(completedSessions, expectedSessions)
            ),
            strengthIndex = StrengthIndexUiModel(
                percent = strengthIndex,
                summary = buildStrengthSummary(strengthIndex)
            ),
            personalBests = allBests,
            selectedExerciseIds = finalSelectedIds,
            widgets = widgets,
            bodyWeightHistory = weightHistory.map { entry ->
                AnalyticsChartPoint(
                    timestamp = entry.timestamp,
                    value = WeightUnitConverter.convertDisplay(entry.pesoCorporeo, weightUnit),
                    id = entry.id
                )
            },
            weightUnit = weightUnit,
            workoutDates = finishedSessionDates,
            isLoading = false,
            error = null
        )
    }

    private fun buildConsistencySummary(completed: Int, expected: Int): String {
        if (expected <= 0) return text.consistencyNoSessions
        return text.consistencySummary(completed, expected)
    }

    private fun buildStrengthSummary(strengthIndex: Float?): String {
        if (strengthIndex == null) return text.strengthNoData
        val formatted = formatLocalDecimals(abs(strengthIndex).toDouble(), 1)
        return if (strengthIndex >= 0f) {
            text.strengthPositive(formatted)
        } else {
            text.strengthNegative(formatted)
        }
    }

    private data class AnalyticsQueryContext(
        val activePlan: WorkoutPlanEntity?,
        val allPlans: List<WorkoutPlanEntity>,
        val timeRange: AnalyticsTimeRange,
        val startDate: Long,
        val selectedExerciseIds: Set<Int>,
        val widgetOrder: List<String>,
        val weightUnit: String,
        val languageCode: String,
        val categoryVolumeStartDate: Long,
        val categoryVolumeTimeRange: AnalyticsTimeRange,
        val bodyWeightTimeRange: AnalyticsTimeRange,
        val bodyWeightStartDate: Long,
        val period1Range: AnalyticsTimeRange,
        val period2Range: AnalyticsTimeRange,
        /** A chart pinned for an exercise merged into another shows the one it went into. */
        val merges: List<ExerciseMergeEntity>
    )

    private data class CoreAnalyticsSnapshot(
        val totalVolume: Float,
        val volumeHistory: List<com.emanuel5014.trainable.data.local.dao.DailyVolume>,
        val personalBests: List<PersonalBestRow>,
        val consistency: ConsistencyRow?
    )

    private data class VolumeAnalyticsSnapshot(
        val totalVolume: Float,
        val volumeHistory: List<com.emanuel5014.trainable.data.local.dao.DailyVolume>
    )

    private data class BestsConsistencySnapshot(
        val personalBests: List<PersonalBestRow>,
        val consistency: ConsistencyRow?
    )

    private data class StrengthCategorySnapshot(
        val strengthIndex: Float?,
        val categoryVolumes: List<CategoryVolumeRow>
    )

    private data class TimePeriodComparisonSnapshot(
        val period1Metrics: PeriodComparisonMetrics,
        val period2Metrics: PeriodComparisonMetrics,
        val period1Exercises: List<PeriodExerciseComparison>,
        val period2Exercises: List<PeriodExerciseComparison>
    )

    private companion object {
        val pushCategories = setOf("Chest", "Shoulders", "Arms")
    }
}
