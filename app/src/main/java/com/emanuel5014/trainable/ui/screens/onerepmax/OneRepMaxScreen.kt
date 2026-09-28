package com.emanuel5014.trainable.ui.screens.onerepmax

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.ui.components.EmptyState
import com.emanuel5014.trainable.ui.components.ExercisePickerBottomSheet
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.OneRepMaxDialog
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.components.formatWeight
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.ui.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneRepMaxScreen(
    onNavigateBack: () -> Unit,
    viewModel: OneRepMaxViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showPicker by remember { mutableStateOf(false) }
    var dialogExerciseId by remember { mutableStateOf<Int?>(null) }
    var detailExerciseId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = Surface,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPicker = true },
                containerColor = Primary,
                contentColor = OnPrimary,
                shape = CircleShape,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_one_rep_max).uppercase(), fontWeight = FontWeight.ExtraBold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ScreenHeader(
                    title = stringResource(R.string.one_rep_maxes),
                    subtitle = stringResource(R.string.one_rep_maxes_subtitle),
                    navigationIcon = {
                        GymIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = onNavigateBack,
                            containerColor = Color.Transparent,
                            contentColor = OnSurface,
                            description = "Back"
                        )
                    },
                    titleInRow = true,
                    titleStyle = MaterialTheme.typography.headlineLarge
                )
            }
            if (!state.isLoading && state.rows.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.EmojiEvents,
                        title = stringResource(R.string.one_rep_maxes_empty_title),
                        description = stringResource(R.string.one_rep_maxes_empty_desc),
                        modifier = Modifier.height(420.dp)
                    )
                }
            }
            items(state.rows, key = { it.exerciseId }) { row ->
                OneRepMaxRowCard(
                    row = row,
                    languageCode = languageCode,
                    weightUnit = state.weightUnit,
                    onClick = { detailExerciseId = row.exerciseId },
                    modifier = Modifier.padding(horizontal = ResponsiveSize.horizontalPadding)
                )
            }
        }
    }

    if (showPicker) {
        ExercisePickerBottomSheet(
            exercises = exercises.filterNot { it.categoria.equals("Cardio", ignoreCase = true) },
            categories = categories.filterNot { it.equals("Cardio", ignoreCase = true) },
            onExerciseSelected = { exercise ->
                showPicker = false
                dialogExerciseId = exercise.id
            },
            onAddCustomExercise = { nome, categoria, onCreated -> viewModel.addCustomExercise(nome, categoria, onCreated) },
            onDismiss = { showPicker = false },
            languageCode = languageCode
        )
    }

    dialogExerciseId?.let { exerciseId ->
        val current = state.rows.firstOrNull { it.exerciseId == exerciseId }?.current?.weightKg
        OneRepMaxDialog(
            title = exercises.firstOrNull { it.id == exerciseId }?.let { ExerciseTranslations.translate(it.nome, languageCode) }
                ?: stringResource(R.string.add_one_rep_max),
            initialKg = current,
            weightUnit = state.weightUnit,
            onDismiss = { dialogExerciseId = null },
            onConfirm = { kg, source ->
                viewModel.add(exerciseId, kg, source)
                dialogExerciseId = null
            }
        )
    }

    detailExerciseId?.let { exerciseId ->
        val row = state.rows.firstOrNull { it.exerciseId == exerciseId }
        if (row == null) {
            detailExerciseId = null
        } else {
            OneRepMaxDetailSheet(
                row = row,
                languageCode = languageCode,
                weightUnit = state.weightUnit,
                roundingIncrement = state.roundingIncrement,
                onDismiss = { detailExerciseId = null },
                onAdd = { dialogExerciseId = exerciseId },
                onDelete = { viewModel.delete(it) }
            )
        }
    }
}

@Composable
private fun OneRepMaxRowCard(
    row: OneRepMaxRow,
    languageCode: String,
    weightUnit: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GymCard(
        modifier = modifier.fillMaxWidth().clip(Shapes.extraLarge).clickable(onClick = onClick),
        containerColor = SurfaceContainer
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ExerciseTranslations.translate(row.exerciseName, languageCode),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface
                )
                Text(
                    text = formatWeight(row.current.weightKg, weightUnit),
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = listOfNotNull(
                        "${sourceLabel(row.current.source)} ${DateFormatter.format(row.current.date)}",
                        row.estimatedKg?.let { stringResource(R.string.estimated_one_rep_max_short, formatWeight(it, weightUnit)) }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                row.previousKg?.let { previous ->
                    val delta = row.current.weightKg - previous
                    if (delta != 0f) {
                        val sign = if (delta > 0) "+" else "−"
                        PrescriptionPill(
                            text = sign + formatWeight(kotlin.math.abs(delta), weightUnit),
                            containerColor = if (delta > 0) Tertiary.copy(alpha = 0.18f) else Error.copy(alpha = 0.12f),
                            contentColor = if (delta > 0) Tertiary else Error
                        )
                    }
                }
                if (row.history.size >= 2) {
                    TrendSparkline(values = row.history.map { it.weightKg }, modifier = Modifier.size(width = 72.dp, height = 32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OneRepMaxDetailSheet(
    row: OneRepMaxRow,
    languageCode: String,
    weightUnit: String,
    roundingIncrement: Float,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onDelete: (OneRepMaxEntity) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ResponsiveSize.cardPadding)
                .padding(bottom = ResponsiveSize.cardPadding)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.one_rep_max_current).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = ExerciseTranslations.translate(row.exerciseName, languageCode),
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = formatWeight(row.current.weightKg, weightUnit),
                    style = MaterialTheme.typography.displaySmall,
                    color = Primary,
                    fontWeight = FontWeight.Black
                )
            }

            if (row.history.size >= 2) {
                Surface(shape = Shapes.medium, color = SurfaceContainer) {
                    TrendSparkline(
                        values = row.history.map { it.weightKg },
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(16.dp),
                        strokeWidth = 4f
                    )
                }
            }

            // Percentage table, like the one at the bottom of a PL notebook page
            SectionTitle(stringResource(R.string.percent_table))
            val table = LoadCalculator.percentTable(row.current.weightKg, weightUnit, roundingIncrement, (60..100 step 5).toList().reversed())
            Surface(shape = Shapes.medium, color = SurfaceContainer) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    table.chunked(2).forEach { pair ->
                        Row {
                            pair.forEach { (percent, kg) ->
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$percent%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = OnSurfaceVariant,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.width(52.dp)
                                    )
                                    Text(
                                        text = formatWeight(kg, weightUnit),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = OnSurface,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.one_rep_max_history))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                row.history.reversed().forEach { entry ->
                    Surface(shape = Shapes.medium, color = SurfaceContainerHigh) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(formatWeight(entry.weightKg, weightUnit), style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "${sourceLabel(entry.source)} · ${DateFormatter.format(entry.date)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDelete(entry) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.one_rep_max_delete_entry), tint = Error)
                            }
                        }
                    }
                }
            }

            com.emanuel5014.trainable.ui.components.GymButton(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_one_rep_max).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = OnSurfaceVariant,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun sourceLabel(source: String): String = stringResource(
    when (source) {
        OneRepMaxEntity.SOURCE_TESTED -> R.string.source_tested
        OneRepMaxEntity.SOURCE_ESTIMATED -> R.string.source_estimated
        else -> R.string.source_manual
    }
)

/** Minimal line chart of 1RM values over time (oldest → newest). */
@Composable
private fun TrendSparkline(values: List<Float>, modifier: Modifier = Modifier, strokeWidth: Float = 3f) {
    val color = Primary
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (values.size < 2) return@Canvas
            val min = values.min()
            val max = values.max()
            val range = (max - min).takeIf { it > 0f } ?: 1f
            val stepX = size.width / (values.size - 1)
            val points = values.mapIndexed { i, v ->
                Offset(i * stepX, size.height - (v - min) / range * size.height)
            }
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, color, style = Stroke(width = strokeWidth * density, cap = StrokeCap.Round))
            drawCircle(color, radius = strokeWidth * density * 1.4f, center = points.last())
        }
    }
}

/** Compact entry point shown on the analytics tab: count of tracked lifts and the latest 1RMs. */
@Composable
fun OneRepMaxesEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OneRepMaxViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState()
    Surface(
        modifier = modifier.fillMaxWidth().clip(Shapes.medium).clickable(onClick = onClick),
        shape = Shapes.medium,
        color = Primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.one_rep_maxes).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (state.rows.isEmpty()) stringResource(R.string.one_rep_maxes_empty_title)
                    else state.rows.take(3).joinToString(" · ") {
                        "${ExerciseTranslations.translate(it.exerciseName, languageCode)} ${formatWeight(it.current.weightKg, state.weightUnit)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Icon(
                androidx.compose.material.icons.Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = OnSurfaceVariant
            )
        }
    }
}
