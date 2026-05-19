package com.emanuel5014.trainable.ui.screens.analytics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.dao.CategoryVolumeRow
import com.emanuel5014.trainable.data.local.dao.ConsistencyRow
import com.emanuel5014.trainable.data.local.dao.PersonalBestRow
import com.emanuel5014.trainable.data.repository.AnalyticsRepository
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
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

    private val activePlanFlow = workoutRepository.getActivePlans()
        .map { plans -> plans.firstOrNull() }

    private val analyticsSnapshotFlow = combine(
        activePlanFlow,
        selectedTimeRange,
        selectedExerciseIds,
        widgetOrder,
        userPreferencesRepository.weightUnit,
        localeManager.currentLanguage
    ) { args ->
        val activePlan = args[0] as com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity?
        val timeRange = args[1] as AnalyticsTimeRange
        @Suppress("UNCHECKED_CAST")
        val selectedIds = args[2] as Set<Int>
        @Suppress("UNCHECKED_CAST")
        val order = args[3] as List<String>
        val weightUnit = args[4] as String
        val userLang = args[5] as String
        
        val languageCode = localeManager.resolveLanguageForCompose(userLang)
        AnalyticsQueryContext(
            activePlan = activePlan,
            timeRange = timeRange,
            startDate = timeRange.startDate(),
            selectedExerciseIds = selectedIds,
            widgetOrder = order,
            weightUnit = weightUnit,
            languageCode = languageCode
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
            analyticsRepository.getVolumeByCategory(context.startDate)
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

        coreFlow.flatMapLatest { core ->
            combine(
                strengthFlow,
                weightFlow,
                sessionsFlow,
                exerciseHistoriesFlow
            ) { strengthCategory, weightHistory, sessions, exerciseHistories ->
                buildAnalyticsState(
                    activePlanName = context.activePlan?.nome ?: "No Active Plan",
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
                    weightHistory = weightHistory,
                    sessions = sessions ?: emptyList(),
                    exerciseHistories = exerciseHistories,
                    weightUnit = context.weightUnit,
                    languageCode = context.languageCode
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
        widgetOrder.update { current ->
            val id = "exercise_$exerciseId"
            if (current.contains(id)) return@update current
            val newList = current + id
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
        weightHistory: List<com.emanuel5014.trainable.data.local.entity.WeightLogEntity>,
        sessions: List<WorkoutSessionEntity>,
        exerciseHistories: Map<Int, List<com.emanuel5014.trainable.data.local.dao.DailyExerciseMax>>,
        weightUnit: String,
        languageCode: String
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
                else -> null
            }
        }

        // No automatic selection - let user choose freely from the picker
        val finalSelectedIds = selectedExerciseIds

        return AnalyticsUiState(
            activePlanName = activePlanName,
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
        val activePlan: com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity?,
        val timeRange: AnalyticsTimeRange,
        val startDate: Long,
        val selectedExerciseIds: Set<Int>,
        val widgetOrder: List<String>,
        val weightUnit: String,
        val languageCode: String
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

    private companion object {
        val pushCategories = setOf("Chest", "Shoulders", "Arms")
    }
}
