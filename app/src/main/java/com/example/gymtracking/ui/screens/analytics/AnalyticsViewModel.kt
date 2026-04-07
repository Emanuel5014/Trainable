package com.example.gymtracking.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracking.data.local.dao.CategoryVolumeRow
import com.example.gymtracking.data.local.dao.ConsistencyRow
import com.example.gymtracking.data.local.dao.PersonalBestRow
import com.example.gymtracking.data.repository.AnalyticsRepository
import com.example.gymtracking.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val selectedTimeRange = MutableStateFlow(AnalyticsTimeRange.OneMonth)
    private val bodyWeightInput = MutableStateFlow("")

    private val activePlanFlow = workoutRepository.getActivePlans()
        .map { plans -> plans.firstOrNull() }

    private val analyticsSnapshotFlow = combine(
        activePlanFlow,
        selectedTimeRange
    ) { activePlan, timeRange ->
        AnalyticsQueryContext(
            activePlan = activePlan,
            timeRange = timeRange,
            startDate = timeRange.startDate()
        )
    }.flatMapLatest { context ->
        val consistencyFlow = context.activePlan?.let {
            analyticsRepository.getConsistency(it.id, context.startDate)
        } ?: flowOf(null)

        val volumeFlow = combine(
            analyticsRepository.getTotalVolumeSince(context.startDate),
            analyticsRepository.getVolumeHistory(context.startDate)
        ) { values: Array<Any?> ->
            val totalVolume = values[0] as Float?
            val volumeHistory = values[1] as List<com.example.gymtracking.data.local.dao.DailyVolume>
            VolumeAnalyticsSnapshot(
                totalVolume = totalVolume ?: 0f,
                volumeHistory = volumeHistory
            )
        }

        val bestsFlow = combine(
            analyticsRepository.getAllPersonalBests(),
            consistencyFlow
        ) { values: Array<Any?> ->
            val personalBests = values[0] as List<PersonalBestRow>
            val consistency = values[1] as ConsistencyRow?
            BestsConsistencySnapshot(
                personalBests = personalBests,
                consistency = consistency
            )
        }

        val coreFlow = combine(volumeFlow, bestsFlow) { values: Array<Any?> ->
            val volume = values[0] as VolumeAnalyticsSnapshot
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
            val strengthIndex = values[0] as Float?
            val categoryVolumes = values[1] as List<CategoryVolumeRow>
            StrengthCategorySnapshot(
                strengthIndex = strengthIndex,
                categoryVolumes = categoryVolumes
            )
        }

        val supplementalFlow = combine(
            strengthFlow,
            analyticsRepository.getWeightHistory(context.startDate)
        ) { values: Array<Any?> ->
            val strengthCategory = values[0] as StrengthCategorySnapshot
            val weightHistory = values[1] as List<com.example.gymtracking.data.local.entity.WeightLogEntity>
            SupplementalAnalyticsSnapshot(
                strengthIndex = strengthCategory.strengthIndex,
                categoryVolumes = strengthCategory.categoryVolumes,
                weightHistory = weightHistory
            )
        }

        coreFlow.flatMapLatest { core ->
            supplementalFlow.map { supplemental ->
                buildAnalyticsState(
                    activePlanName = context.activePlan?.nome ?: "No Active Plan",
                    timeRange = context.timeRange,
                    startDate = context.startDate,
                    totalVolume = core.totalVolume,
                    volumeHistory = core.volumeHistory,
                    personalBests = core.personalBests,
                    consistency = core.consistency,
                    strengthIndex = supplemental.strengthIndex,
                    categoryVolumes = supplemental.categoryVolumes,
                    weightHistory = supplemental.weightHistory
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

    fun submitWeight() {
        val parsedWeight = bodyWeightInput.value.replace(',', '.').toFloatOrNull() ?: return

        viewModelScope.launch {
            analyticsRepository.addWeightLog(
                userId = 1,
                peso = parsedWeight,
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
        volumeHistory: List<com.example.gymtracking.data.local.dao.DailyVolume>,
        personalBests: List<PersonalBestRow>,
        consistency: com.example.gymtracking.data.local.dao.ConsistencyRow?,
        strengthIndex: Float?,
        categoryVolumes: List<CategoryVolumeRow>,
        weightHistory: List<com.example.gymtracking.data.local.entity.WeightLogEntity>
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

        return AnalyticsUiState(
            activePlanName = activePlanName,
            selectedTimeRange = timeRange,
            totalVolumeKg = totalVolume,
            volumeHistory = volumeHistory.map { point ->
                AnalyticsChartPoint(timestamp = point.timestamp, value = point.volume)
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
            personalBests = personalBests.map { row ->
                PersonalBestUiModel(
                    exerciseId = row.exerciseId,
                    exerciseName = row.exerciseName,
                    category = row.category,
                    maxWeightKg = row.maxWeight
                )
            },
            bodyWeightHistory = weightHistory.map { entry ->
                AnalyticsChartPoint(timestamp = entry.timestamp, value = entry.pesoCorporeo)
            },
            isLoading = false,
            error = null
        )
    }

    private fun buildConsistencySummary(completed: Int, expected: Int): String {
        if (expected <= 0) return "No scheduled sessions yet."
        return "$completed/$expected scheduled sessions completed."
    }

    private fun buildStrengthSummary(strengthIndex: Float?): String {
        if (strengthIndex == null) return "Not enough PR history yet."
        val formatted = String.format(Locale.getDefault(), "%.1f", abs(strengthIndex))
        return if (strengthIndex >= 0f) {
            "+$formatted% vs previous 30 days"
        } else {
            "-$formatted% vs previous 30 days"
        }
    }

    private fun formatPercent(value: Float): String {
        return String.format(Locale.getDefault(), "%.0f", value)
    }

    private data class AnalyticsQueryContext(
        val activePlan: com.example.gymtracking.data.local.entity.WorkoutPlanEntity?,
        val timeRange: AnalyticsTimeRange,
        val startDate: Long
    )

    private data class CoreAnalyticsSnapshot(
        val totalVolume: Float,
        val volumeHistory: List<com.example.gymtracking.data.local.dao.DailyVolume>,
        val personalBests: List<PersonalBestRow>,
        val consistency: ConsistencyRow?
    )

    private data class VolumeAnalyticsSnapshot(
        val totalVolume: Float,
        val volumeHistory: List<com.example.gymtracking.data.local.dao.DailyVolume>
    )

    private data class BestsConsistencySnapshot(
        val personalBests: List<PersonalBestRow>,
        val consistency: ConsistencyRow?
    )

    private data class StrengthCategorySnapshot(
        val strengthIndex: Float?,
        val categoryVolumes: List<CategoryVolumeRow>
    )

    private data class SupplementalAnalyticsSnapshot(
        val strengthIndex: Float?,
        val categoryVolumes: List<CategoryVolumeRow>,
        val weightHistory: List<com.example.gymtracking.data.local.entity.WeightLogEntity>
    )

    private companion object {
        val pushCategories = setOf("Chest", "Shoulders", "Arms")
    }
}