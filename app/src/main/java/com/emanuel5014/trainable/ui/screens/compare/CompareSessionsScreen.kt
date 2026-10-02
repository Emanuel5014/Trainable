package com.emanuel5014.trainable.ui.screens.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.domain.compare.CardioComparison
import com.emanuel5014.trainable.domain.compare.ComparedMetric
import com.emanuel5014.trainable.domain.compare.ComparedSet
import com.emanuel5014.trainable.domain.compare.ExerciseComparison
import com.emanuel5014.trainable.domain.compare.ExerciseKind
import com.emanuel5014.trainable.domain.compare.MetricKind
import com.emanuel5014.trainable.domain.compare.Outcome
import com.emanuel5014.trainable.domain.compare.SessionComparison
import com.emanuel5014.trainable.domain.compare.SetRow
import com.emanuel5014.trainable.domain.compare.Trend
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.components.SetLogBadges
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OnTertiary
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.util.WeightUnitConverter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private val ColorA: Color @Composable get() = Primary
private val ColorB: Color @Composable get() = Tertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareSessionsScreen(
    sessionId1: Int,
    sessionId2: Int,
    onNavigateBack: () -> Unit,
    viewModel: CompareSessionsViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId1, sessionId2) {
        viewModel.loadSessions(sessionId1, sessionId2)
    }

    val uiState by viewModel.uiState.collectAsState()
    var swapped by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.compare_sessions_title),
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = OnSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { swapped = !swapped }) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = stringResource(R.string.compare_swap_sessions),
                            tint = OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { paddingValues ->
        val session1 = uiState.session1
        val session2 = uiState.session2
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { GymLoadingIndicator() }

            uiState.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: stringResource(R.string.error),
                    color = Error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            session1 != null && session2 != null -> {
                val (a, b) = if (swapped) session2 to session1 else session1 to session2
                val comparison = remember(a, b) { SessionComparison.of(a, b) }
                CompareContent(
                    a = a,
                    b = b,
                    comparison = comparison,
                    weightUnit = uiState.weightUnit,
                    languageCode = uiState.languageCode,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun CompareContent(
    a: SessionWithDetails,
    b: SessionWithDetails,
    comparison: SessionComparison,
    weightUnit: String,
    languageCode: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ResponsiveSize.horizontalPadding,
            end = ResponsiveSize.horizontalPadding,
            top = Spacing.small,
            bottom = Spacing.extraLarge
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        item { SessionsHeader(a, b, comparison) }

        if (comparison.summary.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.compare_summary)) }
            item { SummaryCard(comparison.summary, weightUnit) }
        }

        if (comparison.cardio.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.compare_cardio)) }
            items(comparison.cardio) { CardioCard(it) }
        }

        if (comparison.exercises.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.compare_exercises_detail)) }
            items(comparison.exercises, key = { it.exerciseId }) {
                ExerciseCard(it, weightUnit = weightUnit, languageCode = languageCode)
            }
        }
    }
}

// ---- header ------------------------------------------------------------------------------------

@Composable
private fun SessionsHeader(a: SessionWithDetails, b: SessionWithDetails, comparison: SessionComparison) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), modifier = Modifier.fillMaxWidth()) {
            SessionPill("A", ColorA, OnPrimary, a, Modifier.weight(1f))
            SessionPill("B", ColorB, OnTertiary, b, Modifier.weight(1f))
        }

        val aAhead = comparison.summary.count { it.outcome == Outcome.BETTER }
        val bAhead = comparison.summary.count { it.outcome == Outcome.WORSE }
        if (aAhead + bAhead > 0) {
            Text(
                text = stringResource(R.string.compare_verdict, aAhead, bAhead),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionPill(
    letter: String,
    color: Color,
    onColor: Color,
    session: SessionWithDetails,
    modifier: Modifier = Modifier
) {
    val advanced = LocalAdvancedProgramming.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(letter, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = onColor)
        }
        Text(
            text = DateFormatter.format(session.session.timestamp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OnSurface
        )
        Text(
            text = session.plan.nome,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val week = session.session.programWeek.takeIf { advanced }
        val duration = session.session.durationMs?.takeIf { it > 0 }
        if (week != null || duration != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                week?.let { PrescriptionPill(stringResource(R.string.week_short, it), containerColor = color.copy(alpha = 0.18f), contentColor = color) }
                duration?.let { PrescriptionPill(formatDuration((it / 1000).toInt()), containerColor = SurfaceContainerHigh, contentColor = OnSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = OnSurface,
        modifier = Modifier.padding(top = Spacing.small)
    )
}

// ---- summary -----------------------------------------------------------------------------------

@Composable
private fun SummaryCard(metrics: List<ComparedMetric>, weightUnit: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small)) {
            metrics.forEachIndexed { index, metric ->
                if (index > 0) HorizontalDivider(color = SurfaceContainerHighest, modifier = Modifier.padding(vertical = 2.dp))
                MetricBars(metric, weightUnit)
            }
        }
    }
}

/** One metric: its name and change, then a bar per session with the value at the end. */
@Composable
private fun MetricBars(metric: ComparedMetric, weightUnit: String) {
    val top = max(metric.a ?: 0f, metric.b ?: 0f).coerceAtLeast(0.0001f)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = metricLabel(metric.kind),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            DeltaChip(metric)
        }
        BarLine(fraction = (metric.a ?: 0f) / top, color = ColorA, value = formatMetric(metric.kind, metric.a, weightUnit))
        BarLine(fraction = (metric.b ?: 0f) / top, color = ColorB, value = formatMetric(metric.kind, metric.b, weightUnit))
    }
}

@Composable
private fun BarLine(fraction: Float, color: Color, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceContainerHighest)
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 84.dp)
        )
    }
}

// ---- cardio ------------------------------------------------------------------------------------

@Composable
private fun CardioCard(cardio: CardioComparison) {
    val icon = when {
        cardio.label.containsAny("bike", "bici", "cicl", "spin") -> Icons.AutoMirrored.Rounded.DirectionsBike
        cardio.label.containsAny("run", "corsa", "treadmill", "tapis", "jog") -> Icons.AutoMirrored.Rounded.DirectionsRun
        else -> Icons.AutoMirrored.Rounded.DirectionsWalk
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                Text(
                    text = cardio.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ColumnHeader()
            val rows = listOf(
                Triple(MetricKind.CARDIO_DISTANCE, cardio.a?.distanceKm, cardio.b?.distanceKm),
                Triple(MetricKind.CARDIO_DURATION, cardio.a?.durationSeconds?.toFloat(), cardio.b?.durationSeconds?.toFloat()),
                Triple(MetricKind.CARDIO_PACE, cardio.a?.paceSecondsPerKm, cardio.b?.paceSecondsPerKm)
            ).filter { (_, a, b) -> a != null || b != null }
            rows.forEach { (kind, a, b) -> CompactMetricRow(ComparedMetric(kind, a, b), weightUnit = "kg") }
        }
    }
}

@Composable
private fun ColumnHeader() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.weight(1.1f))
        Text("A", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = ColorA, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text("B", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = ColorB, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.weight(0.9f))
    }
}

@Composable
private fun CompactMetricRow(metric: ComparedMetric, weightUnit: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = metricLabel(metric.kind),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = OnSurfaceVariant,
            modifier = Modifier.weight(1.1f)
        )
        Text(
            text = formatMetric(metric.kind, metric.a, weightUnit),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ColorA,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatMetric(metric.kind, metric.b, weightUnit),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ColorB,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.weight(0.9f), contentAlignment = Alignment.CenterEnd) { DeltaChip(metric) }
    }
}

// ---- exercises ---------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(comparison: ExerciseComparison, weightUnit: String, languageCode: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (comparison.kind == ExerciseKind.TIMED) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ExerciseTranslations.translate(comparison.exerciseName, languageCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                    Text(
                        text = ExerciseTranslations.translateCategory(comparison.category, languageCode),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
                comparison.headline?.let { DeltaChip(it) }
            }

            comparison.headline?.let { headline ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = metricLabel(headline.kind),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMetric(headline.kind, headline.a, weightUnit), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ColorA)
                        Text(stringResource(R.string.compare_vs), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(formatMetric(headline.kind, headline.b, weightUnit), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ColorB)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            SetsTableHeader()
            comparison.rows.forEach { row ->
                SetRowView(row, comparison, weightUnit)
            }
        }
    }
}

@Composable
private fun SetsTableHeader() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Spacer(modifier = Modifier.width(26.dp))
        Text("A", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = ColorA, modifier = Modifier.weight(1f).padding(start = 4.dp))
        Text("B", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = ColorB, modifier = Modifier.weight(1f).padding(start = 4.dp))
    }
}

@Composable
private fun SetRowView(row: SetRow, comparison: ExerciseComparison, weightUnit: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.width(26.dp).padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
            Text(
                text = if (row.isWarmup) "W" else row.index.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurfaceVariant
            )
        }
        val isFirstRow = row.index == 1 && !row.isWarmup
        SetCell(
            set = row.a,
            absent = if (comparison.a == null) isFirstRow else null,
            kind = comparison.kind,
            tint = ColorA,
            weightUnit = weightUnit,
            modifier = Modifier.weight(1f)
        )
        SetCell(
            set = row.b,
            absent = if (comparison.b == null) isFirstRow else null,
            kind = comparison.kind,
            tint = ColorB,
            weightUnit = weightUnit,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * @param absent null when the session did this exercise (an empty slot then just means it did fewer sets);
 * otherwise the session skipped the whole exercise, and only its first row says so.
 */
@Composable
private fun SetCell(
    set: ComparedSet?,
    absent: Boolean?,
    kind: ExerciseKind,
    tint: Color,
    weightUnit: String,
    modifier: Modifier = Modifier
) {
    val advanced = LocalAdvancedProgramming.current
    if (set == null && absent == false) {
        Spacer(modifier = modifier)
        return
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = if (set?.isWarmup == true) 0.05f else 0.1f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (set != null) {
            val rpe = set.log.rpe
            Text(
                text = setValueText(set, kind, weightUnit) + if (!advanced && rpe != null) " @${WeightUnitConverter.format(rpe)}" else "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (set.isWarmup) OnSurfaceVariant else OnSurface
            )
            SetLogBadges(set.log)
        } else if (absent == true) {
            Text(
                text = stringResource(R.string.compare_not_performed),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                fontStyle = FontStyle.Italic
            )
        } else {
            Text("—", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun setValueText(set: ComparedSet, kind: ExerciseKind, weightUnit: String): String {
    val load = WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(set.weightKg, weightUnit), weightUnit)
    return when {
        set.durationSeconds != null -> formatDuration(set.durationSeconds) + if (set.weightKg > 0f) " · $load" else ""
        kind == ExerciseKind.BODYWEIGHT || set.weightKg <= 0f -> stringResource(R.string.compare_reps_value, set.reps)
        else -> "$load × ${set.reps}"
    }
}

// ---- shared pieces -----------------------------------------------------------------------------

/** Arrow and percentage of a difference, coloured by whether it is good for that metric. */
@Composable
private fun DeltaChip(metric: ComparedMetric) {
    if (!metric.isComparable) return
    val color = when (metric.outcome) {
        Outcome.BETTER -> Primary
        Outcome.WORSE -> Error
        Outcome.NEUTRAL -> OnSurfaceVariant
    }
    val icon = when (metric.trend) {
        Trend.UP -> Icons.Rounded.ArrowUpward
        Trend.DOWN -> Icons.Rounded.ArrowDownward
        Trend.SAME -> Icons.Rounded.Remove
    }
    val text = when {
        metric.trend == Trend.SAME -> "0.0%"
        metric.percent != null -> String.format(Locale.getDefault(), "%+.1f%%", metric.percent)
        else -> ""
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        if (text.isNotEmpty()) {
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun metricLabel(kind: MetricKind): String = stringResource(
    when (kind) {
        MetricKind.DURATION -> R.string.compare_duration
        MetricKind.VOLUME -> R.string.compare_volume
        MetricKind.WORKING_SETS -> R.string.compare_working_sets
        MetricKind.TOTAL_REPS -> R.string.compare_total_reps
        MetricKind.EXERCISES -> R.string.compare_exercises
        MetricKind.MAX_WEIGHT -> R.string.compare_max_weight
        MetricKind.BEST_E1RM -> R.string.compare_best_1rm
        MetricKind.AVG_RPE -> R.string.compare_avg_rpe
        MetricKind.TIME_UNDER_TENSION -> R.string.compare_time_under_tension
        MetricKind.CARDIO_DISTANCE -> R.string.compare_distance
        MetricKind.CARDIO_DURATION -> R.string.compare_duration
        MetricKind.CARDIO_PACE -> R.string.compare_pace
    }
)

private fun formatMetric(kind: MetricKind, value: Float?, weightUnit: String): String {
    if (value == null) return "—"
    return when (kind) {
        MetricKind.VOLUME ->
            String.format(Locale.getDefault(), "%,.0f %s", WeightUnitConverter.convertDisplay(value, weightUnit), weightUnit)
        MetricKind.MAX_WEIGHT, MetricKind.BEST_E1RM ->
            WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(value, weightUnit), weightUnit)
        MetricKind.DURATION, MetricKind.CARDIO_DURATION, MetricKind.TIME_UNDER_TENSION -> formatDuration(value.roundToInt())
        MetricKind.WORKING_SETS, MetricKind.TOTAL_REPS, MetricKind.EXERCISES -> value.roundToInt().toString()
        MetricKind.AVG_RPE -> String.format(Locale.getDefault(), "%.1f", value)
        MetricKind.CARDIO_DISTANCE -> WeightUnitConverter.format(value) + " km"
        MetricKind.CARDIO_PACE -> {
            val total = value.roundToInt()
            String.format(Locale.getDefault(), "%d:%02d /km", total / 60, total % 60)
        }
    }
}

/** "1h 05m", "32m 10s", "45s". */
private fun formatDuration(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        h > 0 -> String.format(Locale.getDefault(), "%dh %02dm", h, m)
        m > 0 -> if (s > 0) String.format(Locale.getDefault(), "%dm %02ds", m, s) else "${m}m"
        else -> "${s}s"
    }
}

private fun String.containsAny(vararg parts: String): Boolean = parts.any { contains(it, ignoreCase = true) }
