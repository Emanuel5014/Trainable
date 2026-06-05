package com.emanuel5014.trainable.ui.screens.analytics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.dao.CategoryVolumeRow
import com.emanuel5014.trainable.data.local.dao.ConsistencyRow
import com.emanuel5014.trainable.data.local.dao.PeriodExerciseRow
import com.emanuel5014.trainable.data.local.dao.PeriodMetrics
import com.emanuel5014.trainable.data.local.dao.PersonalBestRow
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import com.emanuel5014.trainable.util.WeightUnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val workoutRepository: WorkoutRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)
    
    private val selectedTimeRange = MutableStateFlow(AnalyticsTimeRange.OneMonth)
    private val bodyWeightInput = MutableStateFlow("")
    private val selectedExerciseIds = MutableStateFlow<Set<Int>>(loadSavedExerciseIds())
    private val widgetOrder = MutableStateFlow<List<String>>(loadWidgetOrder())
    private val categoryVolumeTimeRange = MutableStateFlow(loadCategoryVolumeTimeRange())
    private val periodComparisonRange = MutableStateFlow(AnalyticsTimeRange.OneMonth)

    private fun loadSavedExerciseIds(): Set<Int> {
        val saved = prefs.getStringSet("selected_exercise_ids", emptySet()) ?: emptySet()
        return saved.mapNotNull { it.toIntOrNull() }.toSet()
    }

    private fun saveExerciseIds(ids: Set<Int>) {
        prefs.edit().putStringSet("selected_exercise_ids", ids.map { it.toString() }.toSet()).apply()
    }

    private fun loadWidgetOrder(): List<String> {
        val saved = prefs.getString("widget_order", null)
        return saved?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    private fun saveWidgetOrder(order: List<String>) {
        prefs.edit().putString("widget_order", order.joinToString(",")).apply()
    }

    private fun loadCategoryVolumeTimeRange(): AnalyticsTimeRange {
        val name = prefs.getString("category_volume_time_range", AnalyticsTimeRange.OneWeek.name)
            ?: AnalyticsTimeRange.OneWeek.name
        return try { AnalyticsTimeRange.valueOf(name) } catch (_: IllegalArgumentException) { AnalyticsTimeRange.OneWeek }
    }

    private fun saveCategoryVolumeTimeRange(timeRange: AnalyticsTimeRange) {
        prefs.edit().putString("category_volume_time_range", timeRange.name).apply()
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
        localeManager.currentLanguage,
        categoryVolumeTimeRange,
        periodComparisonRange
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
        
        val languageCode = localeManager.resolveLanguageForCompose(userLang)
        
        val categoryVolumeStartDate = when (catVolumeTimeRange) {
            AnalyticsTimeRange.OneWeek -> {
                val calendar = java.util.Calendar.getInstance()
                val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                val daysSinceMonday = if (dayOfWeek == java.util.Calendar.SUNDAY) 6 else dayOfWeek - java.util.Calendar.MONDAY
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysSinceMonday)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            AnalyticsTimeRange.OneMonth -> {
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            AnalyticsTimeRange.SixMonths -> {
                val calendar = java.util.Calendar.getInstance()
                calendar.add(java.util.Calendar.MONTH, -5)
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            AnalyticsTimeRange.All -> 0L
        }

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
            period1Range = pRange,
            period2Range = pRange
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

        val weightFlow = analyticsRepository.getWeightHistory(context.startDate)
        val sessionsFlow = workoutRepository.getAllSessions()
        
        val exerciseChartFlows = context.widgetOrder
            .filter { it.startsWith("exercise_") }
            .map { idStr ->
                val exerciseId = idStr.removePrefix("exercise_").toInt()
                analyticsRepository.getExerciseProgressHistory(exerciseId, context.startDate)
                    .map { history -> exerciseId to history }
            }

        val exerciseHistoriesFlow = if (exerciseChartFlows.isEmpty()) {
            flowOf(emptyMap<Int, List<com.emanuel5014.trainable.data.local.dao.DailyExerciseMax>>())
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
        val period1EndDate = System.currentTimeMillis()
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
                    period2Range = context.period2Range
                )
            }
        }
    }.catch { throwable ->
        emit(
            AnalyticsUiState(
                isLoading = false,
                error = throwable.localizedMessage ?: "Errore sconosciuto"
            )
        )
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        analyticsSnapshotFlow,
        bodyWeightInput
    ) { snapshot, input ->
        snapshot.copy(bodyWeightInput = input)
    }.stateIn(
        scope = viewModelScope,
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

    fun addExerciseChart(exerciseId: Int) {
        addMultipleExerciseCharts(setOf(exerciseId))
    }

    fun addMultipleExerciseCharts(exerciseIds: Set<Int>) {
        widgetOrder.update { current ->
            var newList = current
            exerciseIds.forEach { id ->
                val widgetId = "exercise_$id"
                if (!newList.contains(widgetId)) {
                    newList = newList + widgetId
                }
            }
            saveWidgetOrder(newList)
            newList
        }
    }

    fun addBodyWeightChart() {
        widgetOrder.update { current ->
            if (current.contains("weight")) return@update current
            val newList = current + "weight"
            saveWidgetOrder(newList)
            newList
        }
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
            val id = "volume_${planId}_${timeRange.name}_${System.currentTimeMillis()}"
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

    fun moveWidget(id: String, up: Boolean) {
        widgetOrder.update { current ->
            val index = current.indexOf(id)
            if (index == -1) return@update current
            val newIndex = if (up) index - 1 else index + 1
            if (newIndex !in current.indices) return@update current
            
            val newList = current.toMutableList()
            val item = newList.removeAt(index)
            newList.add(newIndex, item)
            saveWidgetOrder(newList)
            newList
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

    fun submitWeight() {
        val parsedWeight = bodyWeightInput.value.replace(',', '.').toFloatOrNull() ?: return
        val weightUnit = uiState.value.weightUnit

        viewModelScope.launch {
            analyticsRepository.addWeightLog(
                userId = 1,
                peso = WeightUnitConverter.convertStorage(parsedWeight, weightUnit),
                timestamp = System.currentTimeMillis()
            )
            bodyWeightInput.value = ""
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
        exerciseHistories: Map<Int, List<com.emanuel5014.trainable.data.local.dao.DailyExerciseMax>>,
        volumeHistories: Map<String, List<com.emanuel5014.trainable.data.local.dao.DailyVolume>>,
        weightUnit: String,
        languageCode: String,
        timePeriodComparison: TimePeriodComparisonSnapshot,
        period1Range: AnalyticsTimeRange,
        period2Range: AnalyticsTimeRange
    ): AnalyticsUiState {
        val completedSessions = consistency?.completedSessions ?: 0
        val targetSessionsPerWeek = consistency?.targetSessionsPerWeek ?: 0
        val weeksInRange = max(
            1f,
            (System.currentTimeMillis() - startDate).toFloat() / TimeUnit.DAYS.toMillis(7).toFloat()
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
                                value = WeightUnitConverter.convertDisplay(entry.pesoCorporeo, weightUnit)
                            )
                        }
                    )
                }
                id == "calendar" -> {
                    AnalyticsWidget.Calendar(workoutDates = finishedSessionDates)
                }
                id.startsWith("exercise_") -> {
                    val exerciseId = id.removePrefix("exercise_").toInt()
                    val exerciseName = allBests.find { it.exerciseId == exerciseId }?.exerciseName ?: "Unknown"
                    val history = exerciseHistories[exerciseId]?.map { point ->
                        AnalyticsChartPoint(
                            timestamp = point.timestamp,
                            value = WeightUnitConverter.convertDisplay(point.maxValue, weightUnit)
                        )
                    } ?: emptyList()
                    AnalyticsWidget.Exercise(
                        exerciseId = exerciseId,
                        exerciseName = exerciseName,
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
                    val period1EndDate = System.currentTimeMillis()
                    val period2StartDate = period2Range.startDate(period1StartDate)
                    val period2EndDate = period1StartDate

                    val dateFormat = java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault())
                    val p1RangeLabel = "${dateFormat.format(java.util.Date(period1StartDate))} - ${dateFormat.format(java.util.Date(period1EndDate))}"
                    val p2RangeLabel = "${dateFormat.format(java.util.Date(period2StartDate))} - ${dateFormat.format(java.util.Date(period2EndDate))}"

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
                        SummaryPart(context.getString(R.string.compare_volume), volumeDelta, volumeDelta >= 0),
                        SummaryPart(context.getString(R.string.compare_sessions), sessionsDelta, sessionsDelta >= 0),
                        SummaryPart(context.getString(R.string.analytics_training_days), trainingDaysDelta, trainingDaysDelta >= 0)
                    )

                    AnalyticsWidget.TimePeriodComparison(
                        period1Name = when (period1Range) {
                            AnalyticsTimeRange.OneWeek -> context.getString(R.string.this_week)
                            AnalyticsTimeRange.OneMonth -> context.getString(R.string.this_month)
                            else -> context.getString(period1Range.labelResId)
                        },
                        period2Name = "${context.getString(R.string.previous_exercise)} (${context.getString(period2Range.labelResId)})",
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
                summary = buildConsistencySummary(context, completedSessions, expectedSessions)
            ),
            strengthIndex = StrengthIndexUiModel(
                percent = strengthIndex,
                summary = buildStrengthSummary(context, strengthIndex)
            ),
            personalBests = allBests,
            selectedExerciseIds = finalSelectedIds,
            widgets = widgets,
            bodyWeightHistory = weightHistory.map { entry ->
                AnalyticsChartPoint(
                    timestamp = entry.timestamp,
                    value = WeightUnitConverter.convertDisplay(entry.pesoCorporeo, weightUnit)
                )
            },
            weightUnit = weightUnit,
            workoutDates = finishedSessionDates,
            isLoading = false,
            error = null
        )
    }

    private fun buildConsistencySummary(context: Context, completed: Int, expected: Int): String {
        if (expected <= 0) return context.getString(R.string.analytics_consistency_no_sessions)
        return context.getString(R.string.analytics_consistency_summary, completed, expected)
    }

    private fun buildStrengthSummary(context: Context, strengthIndex: Float?): String {
        if (strengthIndex == null) return context.getString(R.string.analytics_strength_no_data)
        val formatted = String.format(Locale.getDefault(), "%.1f", abs(strengthIndex))
        return if (strengthIndex >= 0f) {
            context.getString(R.string.analytics_strength_summary_positive, formatted)
        } else {
            context.getString(R.string.analytics_strength_summary_negative, formatted)
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
        val period1Range: AnalyticsTimeRange,
        val period2Range: AnalyticsTimeRange
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
