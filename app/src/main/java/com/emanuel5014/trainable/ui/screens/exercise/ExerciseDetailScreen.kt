package com.emanuel5014.trainable.ui.screens.exercise

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import com.emanuel5014.trainable.ui.components.FavoriteToggle
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.components.rememberExerciseMediaPicker
import com.emanuel5014.trainable.ui.components.ExerciseMediaMessageToasts
import com.emanuel5014.trainable.ui.components.ExerciseFormDialog
import com.emanuel5014.trainable.domain.exercise.ExerciseForm
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.emanuel5014.trainable.data.local.dao.MergePreview
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.exercise.ExerciseHistory
import com.emanuel5014.trainable.domain.exercise.ExerciseKind
import com.emanuel5014.trainable.domain.exercise.ExerciseNames
import com.emanuel5014.trainable.domain.exercise.ExercisePerformance
import com.emanuel5014.trainable.domain.exercise.ExerciseTraits
import com.emanuel5014.trainable.ui.components.DeletedExercisePill
import com.emanuel5014.trainable.ui.components.ExerciseMediaCard
import com.emanuel5014.trainable.ui.components.ExerciseMediaViewer
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.components.SetLogBadges
import com.emanuel5014.trainable.ui.components.ExercisePickerBottomSheet
import com.emanuel5014.trainable.ui.components.labelRes
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
    // After a merge the dialog goes on with the exercise the open one was merged into, whichever screen opened it
    var shownId by rememberSaveable(exerciseId) { mutableIntStateOf(exerciseId) }
    val state by viewModel.state.collectAsState()
    val candidates by viewModel.mergeCandidates.collectAsState()
    LaunchedEffect(shownId) { viewModel.load(shownId) }
    val isThisExercise = state.exerciseId == shownId
    // The exercise went away while the screen was opening: nothing to show
    LaunchedEffect(isThisExercise, state.notFound) { if (isThisExercise && state.notFound) onDismiss() }
    var mediaOpen by remember { mutableStateOf(false) }
    val exercise = state.exercise.takeIf { isThisExercise && !state.loading }
    val languageCode = state.languageCode
    val name = exercise?.let { ExerciseTranslations.translate(it.nome, languageCode) }.orEmpty()

    var pickerOpen by remember { mutableStateOf(false) }
    var mergeTarget by remember { mutableStateOf<ExerciseEntity?>(null) }
    var preview by remember { mutableStateOf<MergePreview?>(null) }
    var removeOpen by remember { mutableStateOf(false) }
    var willBeDeleted by remember { mutableStateOf<Boolean?>(null) }
    var lastMergeTarget by remember { mutableStateOf("") }
    var editOpen by remember { mutableStateOf(false) }
    val categories by viewModel.categories.collectAsState()
    val namesInUse by viewModel.namesInUse.collectAsState()
    // Add or replace the image or GIF of the open exercise
    val pickMedia = rememberExerciseMediaPicker { exerciseId, uri -> viewModel.attachMedia(exerciseId, uri) }
    ExerciseMediaMessageToasts(viewModel.mediaMessages)

    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    val mergedInto by rememberUpdatedState(lastMergeTarget)
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                ExerciseDetailMessage.MERGED -> context.getString(R.string.exercise_msg_merged, mergedInto)
                ExerciseDetailMessage.UNDONE -> context.getString(R.string.exercise_msg_undone)
                ExerciseDetailMessage.HIDDEN -> context.getString(R.string.exercise_msg_hidden)
                ExerciseDetailMessage.DELETED -> context.getString(R.string.exercise_msg_deleted)
                ExerciseDetailMessage.RESTORED -> context.getString(R.string.exercise_msg_restored)
                ExerciseDetailMessage.CANNOT_MERGE -> context.getString(R.string.exercise_msg_cannot_merge)
                ExerciseDetailMessage.CANNOT_MERGE_CARDIO -> context.getString(R.string.exercise_msg_cannot_merge_cardio)
                ExerciseDetailMessage.CANNOT_MERGE_WORKOUT -> context.getString(R.string.exercise_msg_cannot_merge_workout)
                ExerciseDetailMessage.CANNOT_RESTORE_NAME -> context.getString(R.string.exercise_msg_cannot_restore_name)
                ExerciseDetailMessage.SAVED -> context.getString(R.string.exercise_msg_saved)
            }
            // The screen closes with a deleted exercise, and a snackbar goes with it: a toast stays
            if (message == ExerciseDetailMessage.DELETED) Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            else snackbarHost.showSnackbar(text)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Surface) {
            ExerciseDetailScaffold(
                title = name,
                dimmed = exercise?.isDeleted == true,
                onBack = onDismiss,
                snackbarHost = snackbarHost,
                actions = {
                    if (exercise != null) {
                        if (state.actions.canFavorite) FavoriteToggle(exercise.isFavorite, viewModel::setFavorite)
                        ExerciseActionsMenu(
                            actions = state.actions,
                            isPreset = exercise.id < ExerciseEntity.FIRST_CUSTOM_ID,
                            onEdit = { editOpen = true },
                            onMerge = { pickerOpen = true },
                            onRemove = { removeOpen = true },
                            onRestore = viewModel::restore
                        )
                    }
                }
            ) {
                if (exercise != null) {
                    ExerciseDetailContent(
                        state = state,
                        onOpenMedia = { mediaOpen = true },
                        onAddMedia = { pickMedia(exercise.id) },
                        onUndoMerge = viewModel::undoMerge
                    )
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
            exerciseName = name,
            onDismiss = { mediaOpen = false },
            onReplace = { pickMedia(exercise.id) },
            onRemove = {
                viewModel.removeMedia()
                mediaOpen = false
            }
        )
    }

    if (editOpen && exercise != null) {
        // The form starts from what the exercise has now (a preset: its catalog values), and saves only what differs
        val traits = ExerciseTraits.of(exercise)
        ExerciseFormDialog(
            title = stringResource(R.string.edit_exercise_title),
            categories = categories,
            existingExercises = namesInUse,
            languageCode = languageCode,
            initial = ExerciseForm(
                ExerciseNames.shownName(exercise, languageCode), exercise.categoria, exercise.descrizione.orEmpty(),
                traits.muscles, traits.equipment
            ),
            editingId = exercise.id,
            showTraits = true,
            onConfirm = { form ->
                editOpen = false
                // The name in the form is the one the list shows: left alone, the stored name stays
                viewModel.save(form.copy(name = ExerciseNames.nameToStore(form.name, exercise, languageCode)))
            },
            onDismiss = { editOpen = false }
        )
    }

    if (pickerOpen && exercise != null) {
        ExercisePickerBottomSheet(
            exercises = candidates,
            categories = remember(candidates) { candidates.map { it.categoria }.distinct().sorted() },
            onExerciseSelected = { mergeTarget = it },
            onDismiss = { pickerOpen = false },
            languageCode = languageCode,
            pickOnly = true,
            hint = stringResource(R.string.exercise_merge_pick_hint, name)
        )
    }

    mergeTarget?.let { winner ->
        LaunchedEffect(winner.id) {
            preview = null
            preview = viewModel.previewMerge(winner.id)
        }
        MergePreviewDialog(
            loserName = name,
            winnerName = ExerciseTranslations.translate(winner.nome, languageCode),
            preview = preview,
            onConfirm = {
                lastMergeTarget = ExerciseTranslations.translate(winner.nome, languageCode)
                mergeTarget = null
                viewModel.merge(winner.id) { shownId = it }
            },
            onDismiss = { mergeTarget = null }
        )
    }

    if (removeOpen && exercise != null) {
        LaunchedEffect(exercise.id) { willBeDeleted = viewModel.willBeDeletedForGood() }
        RemoveExerciseDialog(
            name = name,
            willBeDeleted = willBeDeleted,
            onConfirm = {
                removeOpen = false
                willBeDeleted = null
                viewModel.remove()
            },
            onDismiss = {
                removeOpen = false
                willBeDeleted = null
            }
        )
    }
}

/** The ⋮ menu of the detail screen; nothing is drawn when the exercise offers no action. */
@Composable
private fun ExerciseActionsMenu(
    actions: ExerciseDetailActions,
    isPreset: Boolean,
    onEdit: () -> Unit,
    onMerge: () -> Unit,
    onRemove: () -> Unit,
    onRestore: () -> Unit
) {
    if (actions == ExerciseDetailActions.NONE) return
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.exercise_more_actions),
                tint = OnSurface
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (actions.canEdit) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.exercise_action_edit)) },
                    onClick = {
                        open = false
                        onEdit()
                    }
                )
            }
            if (actions.canMerge) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.exercise_action_merge)) },
                    onClick = {
                        open = false
                        onMerge()
                    }
                )
            }
            if (actions.canRemove) {
                DropdownMenuItem(
                    text = { Text(stringResource(if (isPreset) R.string.exercise_action_hide else R.string.exercise_action_delete)) },
                    onClick = {
                        open = false
                        onRemove()
                    }
                )
            }
            if (actions.canRestore) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.exercise_action_restore)) },
                    onClick = {
                        open = false
                        onRestore()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailScaffold(
    title: String,
    dimmed: Boolean,
    onBack: () -> Unit,
    snackbarHost: SnackbarHostState,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHost) },
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
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = if (dimmed) OnSurfaceVariant.copy(alpha = 0.7f) else OnSurface,
                    navigationIconContentColor = OnSurface,
                    actionIconContentColor = OnSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) { content() }
    }
}

/** The body of the detail screen; the dialog around it is [ExerciseDetailDialog]. */
@Composable
internal fun ExerciseDetailContent(
    state: ExerciseDetailUiState,
    onOpenMedia: () -> Unit,
    onAddMedia: () -> Unit,
    onUndoMerge: (Int) -> Unit
) {
    val exercise = state.exercise ?: return
    val history = state.history ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "media") {
            val mediaFile = exercise.mediaPath
            if (mediaFile != null) {
                ExerciseMediaCard(
                    fileName = mediaFile,
                    onOpen = onOpenMedia,
                    modifier = Modifier.fillMaxWidth().height(240.dp)
                )
            } else {
                AddMediaCard(onClick = onAddMedia, modifier = Modifier.fillMaxWidth())
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
                ExerciseTraits.of(exercise).takeUnless { it.isEmpty }?.let { ExerciseTraitChips(it) }
            }
        }
        if (state.merges.asWinner.isNotEmpty() || state.merges.asLoser != null) {
            item(key = "merges") { MergeRows(state = state.merges, languageCode = state.languageCode, onUndo = onUndoMerge) }
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

/** What the exercise targets, read-only: its equipment first, then its muscles in canonical order, wrapping onto more lines. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseTraitChips(traits: ExerciseTraits, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        traits.equipment?.let {
            PrescriptionPill(text = stringResource(it.labelRes()), containerColor = Primary.copy(alpha = 0.16f), contentColor = Primary)
        }
        traits.muscles.forEach { PrescriptionPill(text = stringResource(it.labelRes())) }
    }
}

/** Stands where the media goes while the exercise has none: one tap to pick an image or a GIF. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddMediaCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cookie = MaterialShapes.Cookie9Sided.toShape()
    val label = stringResource(R.string.exercise_media_add)
    Column(
        modifier = modifier
            .clip(Shapes.large)
            .background(SurfaceContainerHigh)
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(cookie)
                .background(Primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AddPhotoAlternate,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OnSurface
        )
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
        AnalyticsLineChart(
            points = shown,
            modifier = Modifier.padding(top = 8.dp),
            showValueLabels = shown.size <= MAX_LABELLED_POINTS
        )
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
            // An exercise done twice in the workout (warm-ups first, heavy sets last) lists each run on its own
            performance.groups.forEachIndexed { index, run ->
                if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Surface.copy(alpha = 0.5f))
                run.forEach { set -> PerformanceSetRow(set = set, weightUnit = weightUnit) }
            }
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
                if (set.isWarmup) PrescriptionPill(text = stringResource(R.string.compare_warmup_set))
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

/** Past this many sessions the value badges of the chart overlap, so it is drawn without them. */
private const val MAX_LABELLED_POINTS = 8

private fun weightText(kg: Float, weightUnit: String): String =
    WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(kg, weightUnit), weightUnit)
