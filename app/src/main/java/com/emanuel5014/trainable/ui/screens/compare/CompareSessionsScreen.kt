package com.emanuel5014.trainable.ui.screens.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlin.math.abs

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
                    androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                GymLoadingIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: stringResource(R.string.error),
                    color = Error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else if (uiState.session1 != null && uiState.session2 != null) {
            val session1 = uiState.session1!!
            val session2 = uiState.session2!!
            val weightUnit = uiState.weightUnit
            val languageCode = uiState.languageCode

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = ResponsiveSize.horizontalPadding,
                    vertical = Spacing.medium
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                item {
                    CompareHeaderSection(
                        session1 = session1,
                        session2 = session2,
                        weightUnit = weightUnit
                    )
                }

                item {
                    CompareMetricCard(
                        title = stringResource(R.string.compare_volume),
                        value1 = session1.sets.sumOf { (it.setLog.pesoSollevato * it.setLog.repsEffettive).toDouble() }.toFloat(),
                        value2 = session2.sets.sumOf { (it.setLog.pesoSollevato * it.setLog.repsEffettive).toDouble() }.toFloat(),
                        weightUnit = weightUnit,
                        isWeight = true
                    )
                }

                item {
                    CompareMetricCard(
                        title = stringResource(R.string.compare_sets),
                        value1 = session1.sets.size.toFloat(),
                        value2 = session2.sets.size.toFloat(),
                        weightUnit = null,
                        isWeight = false
                    )
                }

                item {
                    CompareMetricCard(
                        title = stringResource(R.string.compare_exercises),
                        value1 = session1.sets.map { it.exercise.id }.distinct().size.toFloat(),
                        value2 = session2.sets.map { it.exercise.id }.distinct().size.toFloat(),
                        weightUnit = null,
                        isWeight = false
                    )
                }

                item {
                    CompareMetricCard(
                        title = stringResource(R.string.compare_max_weight),
                        value1 = session1.sets.maxOfOrNull { it.setLog.pesoSollevato } ?: 0f,
                        value2 = session2.sets.maxOfOrNull { it.setLog.pesoSollevato } ?: 0f,
                        weightUnit = weightUnit,
                        isWeight = true
                    )
                }

                item {
                    CompareMetricCard(
                        title = stringResource(R.string.compare_avg_weight),
                        value1 = if (session1.sets.isNotEmpty()) session1.sets.map { it.setLog.pesoSollevato }.average().toFloat() else 0f,
                        value2 = if (session2.sets.isNotEmpty()) session2.sets.map { it.setLog.pesoSollevato }.average().toFloat() else 0f,
                        weightUnit = weightUnit,
                        isWeight = true
                    )
                }

                if (session1.cardio.isNotEmpty() || session2.cardio.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.compare_cardio),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSurface,
                            modifier = Modifier.padding(top = Spacing.small)
                        )
                    }
                    item {
                        CompareCardioSection(
                            session1 = session1,
                            session2 = session2
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.compare_exercises_detail),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface,
                        modifier = Modifier.padding(top = Spacing.small)
                    )
                }

                item {
                    CompareExercisesDetailSection(
                        session1 = session1,
                        session2 = session2,
                        weightUnit = weightUnit,
                        languageCode = languageCode
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareHeaderSection(
    session1: SessionWithDetails,
    session2: SessionWithDetails,
    weightUnit: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.compare_session_a),
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DateFormatter.format(session1.session.timestamp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Text(
                    text = session1.plan.nome,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                contentDescription = null,
                tint = OnSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(horizontal = Spacing.small)
                    .size(20.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.compare_session_b),
                    style = MaterialTheme.typography.labelSmall,
                    color = Tertiary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DateFormatter.format(session2.session.timestamp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Text(
                    text = session2.plan.nome,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompareMetricCard(
    title: String,
    value1: Float,
    value2: Float,
    weightUnit: String?,
    isWeight: Boolean
) {
    val diff = value1 - value2
    val diffPercent = if (value2 != 0f) (diff / abs(value2)) * 100f else 0f

    val isNeutral = abs(diffPercent) < 0.1f
    val isPositive = !isNeutral && diff > 0

    val badgeBgColor = when {
        isPositive -> Primary.copy(alpha = 0.12f)
        isNeutral -> OnSurfaceVariant.copy(alpha = 0.08f)
        else -> Error.copy(alpha = 0.12f)
    }
    val badgeContentColor = when {
        isPositive -> Primary
        isNeutral -> OnSurfaceVariant
        else -> Error
    }
    val icon = when {
        isPositive -> Icons.Rounded.ArrowUpward
        isNeutral -> Icons.Rounded.Remove
        else -> Icons.Rounded.ArrowDownward
    }

    val maxValue = maxOf(value1, value2, 0.01f)
    val progress1 = (value1 / maxValue).coerceIn(0f, 1f)
    val progress2 = (value2 / maxValue).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBgColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeContentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isNeutral) "0.0%" else String.format("%+.1f%%", diffPercent),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeContentColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatValue(value1, weightUnit, isWeight),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    text = stringResource(R.string.compare_vs),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatValue(value2, weightUnit, isWeight),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Tertiary
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.medium))
            
            // Visual comparison progress bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SurfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress1)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Primary.copy(alpha = 0.7f), Primary)
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SurfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress2)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Tertiary.copy(alpha = 0.7f), Tertiary)
                            )
                        )
                )
            }
        }
    }
}

private fun formatValue(value: Float, weightUnit: String?, isWeight: Boolean): String {
    return if (isWeight && weightUnit != null) {
        WeightUnitConverter.formatWithUnit(
            WeightUnitConverter.convertDisplay(value, weightUnit),
            weightUnit
        )
    } else {
        String.format("%.0f", value)
    }
}

@Composable
private fun CompareCardioSection(
    session1: SessionWithDetails,
    session2: SessionWithDetails
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.compare_session_a),
                style = MaterialTheme.typography.labelSmall,
                color = Primary,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = Spacing.small)
            )
            if (session1.cardio.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    session1.cardio.forEach { cardio ->
                        val icon = when (cardio.categoria.lowercase()) {
                            "run", "corsa" -> Icons.AutoMirrored.Rounded.DirectionsRun
                            "bike", "bici", "ciclismo" -> Icons.AutoMirrored.Rounded.DirectionsBike
                            else -> Icons.AutoMirrored.Rounded.DirectionsWalk
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(cardio.categoria, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = OnSurface)
                                Text("${cardio.distanza} km - ${formatDuration(cardio.durataSecondi)}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.compare_no_cardio_session_a),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.medium),
                color = SurfaceContainerHighest
            )

            Text(
                text = stringResource(R.string.compare_session_b),
                style = MaterialTheme.typography.labelSmall,
                color = Tertiary,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = Spacing.small)
            )
            if (session2.cardio.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    session2.cardio.forEach { cardio ->
                        val icon = when (cardio.categoria.lowercase()) {
                            "run", "corsa" -> Icons.AutoMirrored.Rounded.DirectionsRun
                            "bike", "bici", "ciclismo" -> Icons.AutoMirrored.Rounded.DirectionsBike
                            else -> Icons.AutoMirrored.Rounded.DirectionsWalk
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(icon, contentDescription = null, tint = Tertiary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(cardio.categoria, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = OnSurface)
                                Text("${cardio.distanza} km - ${formatDuration(cardio.durataSecondi)}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.compare_no_cardio_session_b),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun CompareExercisesDetailSection(
    session1: SessionWithDetails,
    session2: SessionWithDetails,
    weightUnit: String,
    languageCode: String
) {
    val exercises1 = session1.sets.groupBy { it.exercise.id }
        .mapValues { (_, sets) -> sets.sortedBy { it.setLog.numeroSerie } }
    val exercises2 = session2.sets.groupBy { it.exercise.id }
        .mapValues { (_, sets) -> sets.sortedBy { it.setLog.numeroSerie } }

    val allExerciseIds = (exercises1.keys + exercises2.keys).distinct()

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        allExerciseIds.forEach { exerciseId ->
            val ex1 = exercises1[exerciseId]
            val ex2 = exercises2[exerciseId]
            val exerciseName = ex1?.firstOrNull()?.exercise?.nome
                ?: ex2?.firstOrNull()?.exercise?.nome
                ?: "Unknown"

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ExerciseTranslations.translate(exerciseName, languageCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (ex1 != null && ex2 != null) {
                            fun calculate1RM(sets: List<com.emanuel5014.trainable.data.local.relation.SetWithExercise>): Float {
                                return sets.maxOfOrNull { setWithEx ->
                                    val set = setWithEx.setLog
                                    if (set.repsEffettive > 0) {
                                        if (set.repsEffettive == 1) set.pesoSollevato
                                        else set.pesoSollevato * (1f + set.repsEffettive / 30f)
                                    } else 0f
                                } ?: 0f
                            }
                            val value1 = calculate1RM(ex1)
                            val value2 = calculate1RM(ex2)
                            val diff = value1 - value2
                            val diffPercent = if (value2 != 0f) (diff / abs(value2)) * 100f else 0f

                            val isNeutral = abs(diffPercent) < 0.1f
                            val isPositive = !isNeutral && diff > 0

                            val badgeBgColor = when {
                                isPositive -> Primary.copy(alpha = 0.12f)
                                isNeutral -> OnSurfaceVariant.copy(alpha = 0.08f)
                                else -> Error.copy(alpha = 0.12f)
                            }
                            val badgeContentColor = when {
                                isPositive -> Primary
                                isNeutral -> OnSurfaceVariant
                                else -> Error
                            }
                            val icon = when {
                                isPositive -> Icons.Rounded.ArrowUpward
                                isNeutral -> Icons.Rounded.Remove
                                else -> Icons.Rounded.ArrowDownward
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeBgColor)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = badgeContentColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isNeutral) "0.0%" else String.format("%+.1f%%", diffPercent),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeContentColor
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.medium))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.compare_session_a),
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            if (ex1 != null) {
                                ex1.forEach { setWithEx ->
                                    val set = setWithEx.setLog
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${set.numeroSerie}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${WeightUnitConverter.format(WeightUnitConverter.convertDisplay(set.pesoSollevato, weightUnit))}${weightUnit} × ${set.repsEffettive}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = OnSurface
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.compare_not_performed),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(Spacing.medium))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.compare_session_b),
                                style = MaterialTheme.typography.labelSmall,
                                color = Tertiary,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            if (ex2 != null) {
                                ex2.forEach { setWithEx ->
                                    val set = setWithEx.setLog
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Tertiary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${set.numeroSerie}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Tertiary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${WeightUnitConverter.format(WeightUnitConverter.convertDisplay(set.pesoSollevato, weightUnit))}${weightUnit} × ${set.repsEffettive}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = OnSurface
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.compare_not_performed),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
}
