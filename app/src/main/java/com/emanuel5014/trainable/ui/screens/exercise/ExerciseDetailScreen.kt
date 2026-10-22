package com.emanuel5014.trainable.ui.screens.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.domain.exercise.ExerciseHistory
import com.emanuel5014.trainable.domain.exercise.ExerciseKind
import com.emanuel5014.trainable.domain.exercise.ExercisePerformance
import com.emanuel5014.trainable.ui.components.DeletedExercisePill
import com.emanuel5014.trainable.ui.components.ExerciseMediaCard
import com.emanuel5014.trainable.ui.components.ExerciseMediaViewer
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.components.SetLogBadges
import com.emanuel5014.trainable.ui.components.setValueText
import com.emanuel5014.trainable.ui.components.analytics.AnalyticsLineChart
import com.emanuel5014.trainable.ui.screens.analytics.AnalyticsChartPoint
import com.emanuel5014.trainable.ui.screens.history.SessionTitle
import com.emanuel5014.trainable.ui.screens.history.displayText
import com.emanuel5014.trainable.ui.screens.history.sessionName
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.util.WeightUnitConverter

/**
 * Everything the app knows about one exercise: its media, records and every finished performance. A full-screen dialog
 * rather than a route, so opening it from a picker or from a workout in progress never loses what the user was doing.
 */
@Composable
fun ExerciseDetailDialog(
    exerciseId: Int,
    onDismiss: () -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(exerciseId) { viewModel.load(exerciseId) }
    val isThisExercise = state.exerciseId == exerciseId
    // The exercise went away while the screen was opening: nothing to show
    LaunchedEffect(isThisExercise, state.notFound) { if (isThisExercise && state.notFound) onDismiss() }
    var mediaOpen by remember { mutableStateOf(false) }
    val exercise = state.exercise.takeIf { isThisExercise && !state.loading }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Surface) {
            ExerciseDetailScaffold(
                title = exercise?.let { ExerciseTranslations.translate(it.nome, state.languageCode) }.orEmpty(),
                onBack = onDismiss
            ) {
                if (exercise != null) {
                    ExerciseDetailContent(state = state, onOpenMedia = { mediaOpen = true })
                } else if (!state.notFound) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }

    val mediaFile = exercise?.mediaPath
    if (mediaOpen && exercise != null && mediaFile != null) {
        ExerciseMediaViewer(
            fileName = mediaFile,
            exerciseName = ExerciseTranslations.translate(exercise.nome, state.languageCode),
            onDismiss = { mediaOpen = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = OnSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) { content() }
    }
}

/** The body of the detail screen; the dialog around it is [ExerciseDetailDialog]. */
@Composable
internal fun ExerciseDetailContent(state: ExerciseDetailUiState, onOpenMedia: () -> Unit) {
    val exercise = state.exercise ?: return
    val history = state.history ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        exercise.mediaPath?.let { mediaFile ->
            item(key = "media") {
                ExerciseMediaCard(
                    fileName = mediaFile,
                    onOpen = onOpenMedia,
                    modifier = Modifier.fillMaxWidth().height(240.dp)
                )
            }
        }
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = ExerciseTranslations.translateCategory(exercise.categoria, state.languageCode).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (exercise.isDeleted) OnSurfaceVariant.copy(alpha = 0.7f) else Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (exercise.isDeleted) DeletedExercisePill()
                }
                exercise.descrizione?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                }
            }
        }
        if (history.performances.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.exercise_detail_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            item(key = "records") { RecordsCard(history = history, weightUnit = state.weightUnit) }
            if (history.kind == ExerciseKind.STRENGTH && history.chartPoints.size >= 2) {
                item(key = "progress") { ProgressCard(points = history.chartPoints, weightUnit = state.weightUnit) }
            }
            item(key = "history_title") { SectionTitle(stringResource(R.string.exercise_detail_history)) }
            items(history.performances, key = { it.sessionId }) { performance ->
                PerformanceCard(performance = performance, kind = history.kind, weightUnit = state.weightUnit)
            }
        }
    }
}

@Composable
private fun ProgressCard(points: List<AnalyticsChartPoint>, weightUnit: String) {
    // The points carry kilograms: the chart and its figure are drawn in the unit the user chose
    val shown = remember(points, weightUnit) { points.map { it.copy(value = WeightUnitConverter.convertDisplay(it.value, weightUnit)) } }
    GymCard(containerColor = SurfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.exercise_detail_progress))
        Text(
            text = WeightUnitConverter.formatWithUnit(shown.last().value, weightUnit),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = OnSurface
        )
        AnalyticsLineChart(points = shown, modifier = Modifier.padding(top = 8.dp))
    }
}

/** One workout of the exercise: when and under what name, its best figure, and every set as the history writes it. */
@Composable
private fun PerformanceCard(performance: ExercisePerformance, kind: ExerciseKind, weightUnit: String) {
    val summary = when (kind) {
        ExerciseKind.STRENGTH -> performance.topWeightKg.takeIf { it > 0f }
            ?.let { stringResource(R.string.exercise_detail_top, weightText(it, weightUnit)) }
        ExerciseKind.BODYWEIGHT, ExerciseKind.TIME_AND_REPS -> performance.totalReps.takeIf { it > 0 }
            ?.let { stringResource(R.string.compare_reps_value, it) }
        ExerciseKind.TIMED -> performance.longestHoldSeconds.takeIf { it > 0 }?.let { "${it}s" }
    }
    GymCard(containerColor = SurfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DateFormatter.format(performance.timestamp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
                Text(
                    text = SessionTitle.of(sessionName(performance.sessionNote, performance.planName)).displayText(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            summary?.let { PrescriptionPill(text = it, containerColor = Primary.copy(alpha = 0.12f), contentColor = Primary, emphasized = true) }
        }
        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            performance.sets.forEach { set -> PerformanceSetRow(set = set, weightUnit = weightUnit) }
        }
    }
}

@Composable
private fun PerformanceSetRow(set: SetLogEntity, weightUnit: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.set_number, set.numeroSerie),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
                if (set.isWarmup) PrescriptionPill(text = stringResource(R.string.exercise_detail_warmup))
            }
            Text(
                text = setValueText(set, weightUnit),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
        }
        SetLogBadges(set = set, modifier = Modifier.padding(top = 2.dp))
        if (!set.note.isNullOrBlank()) {
            Row(modifier = Modifier.padding(top = 2.dp, start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Notes,
                    contentDescription = null,
                    tint = OnSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = set.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RecordsCard(history: ExerciseHistory, weightUnit: String) {
    val records = history.records
    GymCard(containerColor = SurfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.exercise_detail_records))
        when (history.kind) {
            ExerciseKind.STRENGTH -> {
                records.heaviest?.let { RecordRow(stringResource(R.string.exercise_record_heaviest), setValueText(it.set, weightUnit), it.timestamp) }
                records.bestE1rm?.let { RecordRow(stringResource(R.string.exercise_record_best_e1rm), weightText(it.value, weightUnit), it.timestamp) }
                records.bestVolume?.let { RecordRow(stringResource(R.string.exercise_record_best_volume), weightText(it.value, weightUnit), it.timestamp) }
            }
            ExerciseKind.BODYWEIGHT -> {
                records.mostRepsInSet?.let {
                    RecordRow(stringResource(R.string.exercise_record_most_reps_set), stringResource(R.string.compare_reps_value, it.set.repsEffettive), it.timestamp)
                }
                records.mostRepsInSession?.let {
                    RecordRow(stringResource(R.string.exercise_record_most_reps_session), stringResource(R.string.compare_reps_value, it.value.toInt()), it.timestamp)
                }
            }
            ExerciseKind.TIMED -> records.longestHold?.let {
                RecordRow(stringResource(R.string.exercise_record_longest_hold), setValueText(it.set, weightUnit), it.timestamp)
            }
            ExerciseKind.TIME_AND_REPS -> records.mostRepsInSet?.let {
                RecordRow(stringResource(R.string.exercise_record_most_reps_interval), setValueText(it.set, weightUnit), it.timestamp)
            }
        }
        val sessions = pluralStringResource(R.plurals.exercise_detail_sessions, records.sessionCount, records.sessionCount)
        val sets = pluralStringResource(R.plurals.exercise_detail_sets, records.workingSetCount, records.workingSetCount)
        val last = records.lastTimestamp?.let { stringResource(R.string.exercise_detail_last, DateFormatter.format(it)) }
        Text(
            text = listOfNotNull("$sessions · $sets", last).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = OnSurfaceVariant,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun RecordRow(label: String, value: String, timestamp: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = OnSurface)
            Text(text = DateFormatter.format(timestamp), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        }
    }
}

private fun weightText(kg: Float, weightUnit: String): String =
    WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(kg, weightUnit), weightUnit)
