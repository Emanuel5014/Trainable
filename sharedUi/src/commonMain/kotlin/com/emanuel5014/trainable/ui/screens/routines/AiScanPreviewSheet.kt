package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.ui.util.PlatformBackHandler
import com.emanuel5014.trainable.ui.util.fullscreenDialogProperties
import com.emanuel5014.trainable.ui.util.fullscreenDialogTopInset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.pluralStringResource
import com.emanuel5014.trainable.sharedui.resources.ai_scan_exercises_read
import com.emanuel5014.trainable.sharedui.resources.ai_scan_removed
import com.emanuel5014.trainable.sharedui.resources.ai_scan_to_check
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.coerceIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.emanuel5014.trainable.data.ai.hasValidReps
import com.emanuel5014.trainable.domain.prescription.RepsMode
import com.emanuel5014.trainable.sharedui.resources.ai_scan_add_count
import com.emanuel5014.trainable.sharedui.resources.ai_scan_review_title
import com.emanuel5014.trainable.ui.components.isKeyboardVisible
import com.emanuel5014.trainable.ui.screens.onboarding.joinedPosition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.ai_scan_add_missing_exercise
import com.emanuel5014.trainable.sharedui.resources.ai_scan_category_label
import com.emanuel5014.trainable.sharedui.resources.ai_scan_confirm_dialog_confirm
import com.emanuel5014.trainable.sharedui.resources.ai_scan_confirm_dialog_desc
import com.emanuel5014.trainable.sharedui.resources.ai_scan_confirm_dialog_review
import com.emanuel5014.trainable.sharedui.resources.ai_scan_confirm_dialog_title
import com.emanuel5014.trainable.sharedui.resources.ai_scan_custom_options_desc
import com.emanuel5014.trainable.sharedui.resources.ai_scan_custom_options_title
import com.emanuel5014.trainable.sharedui.resources.ai_scan_exercise_name_label
import com.emanuel5014.trainable.sharedui.resources.ai_scan_fullscreen_photo
import com.emanuel5014.trainable.sharedui.resources.ai_scan_original_sheet
import com.emanuel5014.trainable.sharedui.resources.ai_scan_save_changes
import com.emanuel5014.trainable.sharedui.resources.ai_scan_save_image_to_routine
import com.emanuel5014.trainable.sharedui.resources.ai_scan_save_to_library_checkbox
import com.emanuel5014.trainable.sharedui.resources.ai_scan_select_from_catalog
import com.emanuel5014.trainable.sharedui.resources.ai_scan_zoom_hint
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.compare_exercises
import com.emanuel5014.trainable.sharedui.resources.reps
import com.emanuel5014.trainable.sharedui.resources.sets
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.ai.ScannedExerciseEntry
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.ui.components.ExercisePickerBottomSheet
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.SheetFormBody
import com.emanuel5014.trainable.ui.components.SheetFormFooter
import com.emanuel5014.trainable.ui.components.SheetFormLayout
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest

class ZoomState(
    initialScale: Float = 1f,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f
) {
    var scale by mutableFloatStateOf(initialScale)
    var offsetX by mutableFloatStateOf(initialOffsetX)
    var offsetY by mutableFloatStateOf(initialOffsetY)

    fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
}

@Composable
fun rememberZoomState(): ZoomState {
    return remember { ZoomState() }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun AiScanPreviewSheet(
    imageUri: String? = null,
    /** Draws the photo of the sheet (Android: coil at full resolution). */
    photo: ScanPhotoUi,
    entries: List<ScannedExerciseEntry>,
    exercises: List<ExerciseEntity>,
    categories: List<String>,
    languageCode: String,
    onAddCustomExercise: (String, String, (ExerciseEntity) -> Unit) -> Unit,
    onConfirm: (List<ScannedExerciseEntry>, Boolean) -> Unit,
    onDismiss: () -> Unit,
    /** Called (debounced) after every edit and when the sheet closes, so a draft can be kept. */
    onEntriesChanged: (List<ScannedExerciseEntry>) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editableEntries = remember { mutableStateListOf(*entries.toTypedArray()) }
    // Closing the sheet (swipe, back, cancel) keeps the scan as a draft instead of losing it
    val dismiss = {
        onEntriesChanged(editableEntries.toList())
        onDismiss()
    }
    LaunchedEffect(Unit) {
        snapshotFlow { editableEntries.toList() }
            .drop(1)
            .debounce(500)
            .collect { onEntriesChanged(it) }
    }
    var pickingIndex by remember { mutableStateOf<Int?>(null) }
    var customEditIndex by remember { mutableStateOf<Int?>(null) }
    var isAddingNewExercise by remember { mutableStateOf(false) }
    var saveImageToPlan by remember { mutableStateOf(imageUri != null) }
    var showFullscreenPhoto by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    // Rows carry a session-only id, so the open row, the checks and the swipe state follow their exercise when
    // rows are removed or put back. One row is open at a time.
    val rowIds = remember { mutableStateListOf<Long>().apply { addAll(editableEntries.indices.map { it.toLong() }) } }
    var nextRowId by remember { mutableLongStateOf(editableEntries.size.toLong()) }
    // Every row starts closed, flagged ones too: the "da controllare" chip leads to them
    var openId by remember { mutableStateOf<Long?>(null) }
    val seenIds = remember { mutableStateListOf<Long>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val panel = rememberScanPanelState()
    val snackbar = remember { SnackbarHostState() }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    // Typing needs the room: the panel goes up to Alto, a strip of the photo stays visible
    val keyboardOpen = isKeyboardVisible
    LaunchedEffect(keyboardOpen) { if (keyboardOpen) panel.expand() }
    /**
     * Only when a row is reached from elsewhere (the "da controllare" chip, Aggiungi, a new exercise): if it is off
     * screen, scroll just enough to show it. Opening a row by tapping it never moves the list.
     */
    fun revealRow(index: Int) {
        scrollJob?.cancel()
        scrollJob = scope.launch {
            // Let a row closing above finish first, then look where this one is (item 0 = header)
            delay(ROW_ANIMATION_SETTLE_MS)
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == index + 1 }
            val inView = item != null && item.offset >= info.viewportStartOffset && item.offset < info.viewportEndOffset - item.size.coerceAtMost(200)
            if (!inView) listState.animateScrollToItem(index + 1)
        }
    }
    fun closeOpenRow() {
        openId?.let { if (it !in seenIds) seenIds.add(it) }
        openId = null
    }
    fun openRow(index: Int, reveal: Boolean) {
        val id = rowIds.getOrNull(index) ?: return
        if (openId != id) {
            closeOpenRow()
            openId = id
        }
        if (reveal) {
            panel.raise()
            revealRow(index)
        }
    }
    fun toggleRow(index: Int) {
        if (openId == rowIds.getOrNull(index)) closeOpenRow() else openRow(index, reveal = false)
    }
    val removedText = stringResource(Res.string.ai_scan_removed)
    val undoText = stringResource(Res.string.cancel)
    fun removeRow(index: Int) {
        val id = rowIds.getOrNull(index) ?: return
        val entry = editableEntries[index]
        val wasSeen = id in seenIds
        editableEntries.removeAt(index)
        rowIds.removeAt(index)
        seenIds.remove(id)
        if (openId == id) openId = null
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val name = ExerciseTranslations.translate(entry.matchedName ?: entry.rawName, languageCode)
            val result = snackbar.showSnackbar(removedText.replace("%1\$s", name), actionLabel = undoText, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) {
                // Back where it was (or at the end if rows above were removed meanwhile)
                // A new id: under the old one the list would restore the swiped-away state and remove it again
                val at = index.coerceAtMost(editableEntries.size)
                val restoredId = nextRowId++
                editableEntries.add(at, entry)
                rowIds.add(at, restoredId)
                if (wasSeen) seenIds.add(restoredId)
            }
        }
    }
    fun addEntry(entry: ScannedExerciseEntry) {
        editableEntries.add(entry)
        rowIds.add(nextRowId++)
        openRow(editableEntries.lastIndex, reveal = true)
    }
    fun needsAttention(entry: ScannedExerciseEntry) = entry.needsReview || !entry.hasValidReps
    fun openNextToCheck() {
        if (editableEntries.isEmpty()) return
        val from = openId?.let { rowIds.indexOf(it) } ?: -1
        val next = (1..editableEntries.size).map { (from + it) % editableEntries.size }.firstOrNull { needsAttention(editableEntries[it]) }
        next?.let { openRow(it, reveal = true) }
    }
    fun confirm() {
        val invalid = editableEntries.indexOfFirst { !it.hasValidReps }
        if (invalid < 0) {
            showConfirmDialog = true
        } else {
            // Never save a row whose reps cannot be stored: show it instead
            openRow(invalid, reveal = true)
        }
    }

    // System BackHandler ensuring the back button always dismisses cleanly
    val isChildSheetOpen = pickingIndex != null || customEditIndex != null || showFullscreenPhoto || isAddingNewExercise || showConfirmDialog
    PlatformBackHandler(enabled = !isChildSheetOpen) {
        dismiss()
    }

    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = sheetState,
        containerColor = Surface,
        contentColor = OnSurface,
        tonalElevation = 0.dp,
        // The exercise panel inside is the thing to drag; the sheet itself does not move (no fighting over the finger,
        // no closing by accident). It closes with Annulla, back or a tap outside.
        sheetGesturesEnabled = false,
        dragHandle = null
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            val isWideScreen = maxWidth >= 600.dp
            val listContent: LazyListScope.() -> Unit = {
                scanReviewList(
                    entries = editableEntries,
                    rowIds = rowIds,
                    openId = openId,
                    seenIds = seenIds,
                    showHeader = isWideScreen || imageUri == null,
                    languageCode = languageCode,
                    onToggle = { toggleRow(it) },
                    onUpdate = { index, updated -> editableEntries[index] = updated },
                    onRemove = { removeRow(it) },
                    onChangeExercise = { index ->
                        val entry = editableEntries.getOrNull(index)
                        if (entry?.exerciseId == null) customEditIndex = index else pickingIndex = index
                    },
                    onAddExerciseClick = { isAddingNewExercise = true },
                    // A one-time choice: at the end of the list instead of taking room under the photo
                    footer = if (imageUri != null) {
                        { SaveImageToggleCard(checked = saveImageToPlan, onCheckedChange = { saveImageToPlan = it }) }
                    } else null
                )
            }
            val actions: @Composable () -> Unit = {
                ActionButtonsSection(
                    onDismiss = dismiss,
                    onConfirm = { confirm() },
                    count = editableEntries.size
                )
            }

            if (isWideScreen) {
                // Two columns on tablets and foldables: the sheet on the left, the exercises on the right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                        .padding(horizontal = ResponsiveSize.cardPadding)
                        .padding(bottom = ResponsiveSize.cardPadding),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.large)
                ) {
                    if (imageUri != null) {
                        val tabletZoomState = rememberZoomState()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Photo, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = stringResource(Res.string.ai_scan_original_sheet),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AnimatedVisibility(
                                        visible = tabletZoomState.scale > 1.05f,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        IconButton(onClick = { tabletZoomState.reset() }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset Zoom", tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(onClick = { showFullscreenPhoto = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            Icons.Rounded.Fullscreen,
                                            contentDescription = stringResource(Res.string.ai_scan_fullscreen_photo),
                                            tint = OnSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            ZoomablePhotoInspector(
                                imageUri = imageUri,
                                photo = photo,
                                zoomState = tabletZoomState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        HeaderSection()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            content = listContent
                        )
                        actions()
                    }
                }
            } else if (imageUri == null) {
                // No photo to compare with: a plain list
                Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    Box(modifier = Modifier.padding(horizontal = ResponsiveSize.cardPadding)) { HeaderSection() }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = ResponsiveSize.cardPadding, vertical = Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        content = listContent
                    )
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = ResponsiveSize.cardPadding).padding(top = 12.dp, bottom = 16.dp)) {
                        actions()
                    }
                }
            } else {
                // Phone: the photo of the sheet behind, the exercises on a panel dragged over it (Alto / Metà / Basso)
                val zoomState = rememberZoomState()
                ScanPhotoPanel(
                    state = panel,
                    photo = { area, coveredPx ->
                        Box(modifier = area) {
                            ZoomablePhotoInspector(
                                imageUri = imageUri,
                                photo = photo,
                                zoomState = zoomState,
                                modifier = Modifier.fillMaxSize(),
                                coveredPx = coveredPx
                            )
                            IconButton(
                                onClick = { showFullscreenPhoto = true },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(Surface.copy(alpha = 0.8f))
                            ) {
                                Icon(
                                    Icons.Rounded.Fullscreen,
                                    contentDescription = stringResource(Res.string.ai_scan_fullscreen_photo),
                                    tint = OnSurface
                                )
                            }
                        }
                    },
                    header = {
                        ReviewSummary(
                            count = editableEntries.size,
                            toCheck = editableEntries.count { needsAttention(it) },
                            onOpenNextToCheck = { openNextToCheck() }
                        )
                    },
                    list = {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = ResponsiveSize.cardPadding, vertical = Spacing.small),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            content = listContent
                        )
                    },
                    footer = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Always there, only shown while the list goes on below: the footer keeps its height,
                            // otherwise the panel's heights change under the finger
                            HorizontalDivider(color = OnSurfaceVariant.copy(alpha = if (listState.canScrollForward) 0.15f else 0f))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ResponsiveSize.cardPadding)
                                    .padding(top = 12.dp, bottom = 16.dp)
                            ) {
                                actions()
                            }
                        }
                    }
                )
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp)
            )
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(Res.string.ai_scan_confirm_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.ai_scan_confirm_dialog_desc, editableEntries.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onConfirm(editableEntries.toList(), saveImageToPlan)
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.ai_scan_confirm_dialog_confirm),
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDialog = false }
                ) {
                    Text(
                        text = stringResource(Res.string.ai_scan_confirm_dialog_review),
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = SurfaceContainerHigh,
            shape = Shapes.large
        )
    }

    // Fullscreen Image Dialog
    if (showFullscreenPhoto && imageUri != null) {
        FullscreenPhotoDialog(
            imageUri = imageUri,
            photo = photo,
            onDismiss = { showFullscreenPhoto = false }
        )
    }

    // Modal Edit Sheet for Custom / New Exercises
    customEditIndex?.let { index ->
        val entry = editableEntries.getOrNull(index)
        if (entry != null) {
            EditCustomExerciseSheet(
                entry = entry,
                categories = categories,
                languageCode = languageCode,
                onSave = { updatedEntry, saveToLibrary ->
                    if (saveToLibrary) {
                        onAddCustomExercise(updatedEntry.rawName, updatedEntry.suggestedCategory) { created ->
                            editableEntries[index] = updatedEntry.copy(
                                exerciseId = created.id,
                                matchedName = created.nome,
                                suggestedCategory = created.categoria
                            )
                        }
                    } else {
                        editableEntries[index] = updatedEntry
                    }
                    customEditIndex = null
                },
                onSelectFromCatalog = {
                    customEditIndex = null
                    pickingIndex = index
                },
                onDismiss = { customEditIndex = null }
            )
        }
    }

    // Exercise Picker for changing an existing exercise or linking from catalog
    if (pickingIndex != null) {
        ExercisePickerBottomSheet(
            exercises = exercises,
            categories = categories,
            onExerciseSelected = { exercise ->
                val i = pickingIndex ?: return@ExercisePickerBottomSheet
                val entry = editableEntries.getOrNull(i) ?: return@ExercisePickerBottomSheet
                editableEntries[i] = entry.copy(
                    exerciseId = exercise.id,
                    matchedName = exercise.nome,
                    suggestedCategory = exercise.categoria
                )
                pickingIndex = null
            },
            onAddCustomExercise = { nome, categoria, onCreated ->
                onAddCustomExercise(nome, categoria) { created ->
                    onCreated(created)
                }
            },
            onDismiss = { pickingIndex = null },
            languageCode = languageCode,
        )
    }

    // Exercise Picker for adding a missing exercise
    if (isAddingNewExercise) {
        ExercisePickerBottomSheet(
            exercises = exercises,
            categories = categories,
            onExerciseSelected = { exercise ->
                val isCardio = exercise.categoria.equals("Cardio", ignoreCase = true)
                addEntry(
                    ScannedExerciseEntry(
                        rawName = exercise.nome,
                        exerciseId = exercise.id,
                        matchedName = exercise.nome,
                        suggestedCategory = exercise.categoria,
                        sets = if (isCardio) 1 else 3,
                        reps = if (isCardio) "1" else "8",
                        restSeconds = 120,
                        cardioMinutes = if (isCardio) 20 else null,
                        // A new strength exercise starts as a range, 8–12
                        repsMode = if (isCardio) null else RepsMode.RANGE,
                        repsMax = if (isCardio) null else 12
                    )
                )
                isAddingNewExercise = false
            },
            onAddCustomExercise = { nome, categoria, onCreated ->
                onAddCustomExercise(nome, categoria) { created ->
                    onCreated(created)
                }
            },
            onDismiss = { isAddingNewExercise = false },
            languageCode = languageCode,
        )
    }
}

/** The panel's head: how many exercises were read and, when some need the user, a chip that opens the next one. */
@Composable
private fun ReviewSummary(count: Int, toCheck: Int, onOpenNextToCheck: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ResponsiveSize.cardPadding)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = pluralStringResource(Res.plurals.ai_scan_exercises_read, count, count),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            modifier = Modifier.weight(1f)
        )
        if (toCheck > 0) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable(onClick = onOpenNextToCheck)
                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pluralStringResource(Res.plurals.ai_scan_to_check, toCheck, toCheck),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Text(
        text = stringResource(Res.string.ai_scan_review_title),
        style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineSmall.fontSize)
        ),
        color = OnSurface,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SaveImageToggleCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Rounded.AddPhotoAlternate,
                    contentDescription = null,
                    tint = if (checked) Primary else OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(Res.string.ai_scan_save_image_to_routine),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                            tint = Primary
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
private fun ZoomablePhotoInspector(
    imageUri: String,
    photo: ScanPhotoUi,
    zoomState: ZoomState,
    modifier: Modifier = Modifier,
    /** Pixels of the photo's bottom hidden under the exercise panel: they can be dragged up into view. */
    coveredPx: () -> Float = { 0f }
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }


    Box(
        modifier = modifier
            .clip(Shapes.medium)
            .background(SurfaceContainerHighest)
            .pointerInput(zoomState) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (zoomState.scale > 1.1f) {
                            zoomState.reset()
                        } else {
                            val newScale = 2.5f
                            zoomState.scale = newScale
                            val diffX = (imageSize.width / 2f - tapOffset.x)
                            val diffY = (imageSize.height / 2f - tapOffset.y)
                            val maxX = ((imageSize.width * newScale) - imageSize.width) / 2f
                            val maxY = ((imageSize.height * newScale) - imageSize.height) / 2f
                            zoomState.offsetX = diffX.coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f))
                            zoomState.offsetY = diffY.coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
                        }
                    }
                )
            }
            .pointerInput(zoomState) {
                detectTransformGestures(panZoomLock = true) { _, pan, zoom, _ ->
                    val newScale = (zoomState.scale * zoom).coerceIn(1f, 5f)
                    zoomState.scale = newScale

                    val covered = coveredPx().coerceAtLeast(0f)
                    if (newScale <= 1f) {
                        // Not zoomed: only the part under the panel can be pulled up into view
                        zoomState.offsetX = 0f
                        zoomState.offsetY = (zoomState.offsetY + pan.y).coerceIn(-covered, 0f)
                    } else {
                        val maxX = ((imageSize.width * newScale) - imageSize.width) / 2f
                        val maxY = ((imageSize.height * newScale) - imageSize.height) / 2f

                        // 1:1 pan precision with finger movement
                        val newOffsetX = zoomState.offsetX + pan.x
                        val newOffsetY = zoomState.offsetY + pan.y

                        zoomState.offsetX = newOffsetX.coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f))
                        zoomState.offsetY = newOffsetY.coerceIn(-maxY.coerceAtLeast(0f) - covered, maxY.coerceAtLeast(0f))
                    }
                }
            }
    ) {
        photo.SheetPhoto(
            uri = imageUri,
            contentDescription = stringResource(Res.string.ai_scan_original_sheet),
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { imageSize = it }
                .graphicsLayer {
                    scaleX = zoomState.scale
                    scaleY = zoomState.scale
                    translationX = zoomState.offsetX
                    // The panel going back down uncovers the photo: never leave it pulled up past what is hidden
                    val maxY = ((imageSize.height * zoomState.scale) - imageSize.height) / 2f
                    translationY = zoomState.offsetY.coerceIn(-maxY.coerceAtLeast(0f) - coveredPx().coerceAtLeast(0f), maxY.coerceAtLeast(0f))
                }
        )

        // Subtle Zoom hint at bottom when not zoomed in
        AnimatedVisibility(
            visible = zoomState.scale <= 1.05f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        ) {
            Text(
                text = stringResource(Res.string.ai_scan_zoom_hint),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun FullscreenPhotoDialog(
    imageUri: String,
    photo: ScanPhotoUi,
    onDismiss: () -> Unit
) {
    val fullscreenZoomState = rememberZoomState()
    var imageSize by remember { mutableStateOf(IntSize.Zero) }


    Dialog(
        onDismissRequest = onDismiss,
        properties = fullscreenDialogProperties()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(fullscreenZoomState) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (fullscreenZoomState.scale > 1.1f) {
                                fullscreenZoomState.reset()
                            } else {
                                val newScale = 3f
                                fullscreenZoomState.scale = newScale
                                val diffX = (imageSize.width / 2f - tapOffset.x)
                                val diffY = (imageSize.height / 2f - tapOffset.y)
                                val maxX = ((imageSize.width * newScale) - imageSize.width) / 2f
                                val maxY = ((imageSize.height * newScale) - imageSize.height) / 2f
                                fullscreenZoomState.offsetX = diffX.coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f))
                                fullscreenZoomState.offsetY = diffY.coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
                            }
                        }
                    )
                }
                .pointerInput(fullscreenZoomState) {
                    detectTransformGestures(panZoomLock = true) { _, pan, zoom, _ ->
                        val newScale = (fullscreenZoomState.scale * zoom).coerceIn(1f, 6f)
                        fullscreenZoomState.scale = newScale

                        if (newScale <= 1f) {
                            fullscreenZoomState.offsetX = 0f
                            fullscreenZoomState.offsetY = 0f
                        } else {
                            val maxX = ((imageSize.width * newScale) - imageSize.width) / 2f
                            val maxY = ((imageSize.height * newScale) - imageSize.height) / 2f

                            val newOffsetX = fullscreenZoomState.offsetX + pan.x
                            val newOffsetY = fullscreenZoomState.offsetY + pan.y

                            fullscreenZoomState.offsetX = newOffsetX.coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f))
                            fullscreenZoomState.offsetY = newOffsetY.coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
                        }
                    }
                }
        ) {
            photo.SheetPhoto(
                uri = imageUri,
                contentDescription = stringResource(Res.string.ai_scan_original_sheet),
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { imageSize = it }
                    .graphicsLayer(
                        scaleX = fullscreenZoomState.scale,
                        scaleY = fullscreenZoomState.scale,
                        translationX = fullscreenZoomState.offsetX,
                        translationY = fullscreenZoomState.offsetY
                    )
            )

            // Top Bar Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fullscreenDialogTopInset()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.ai_scan_original_sheet),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    if (fullscreenZoomState.scale > 1.05f) {
                        Text(
                            text = "${(fullscreenZoomState.scale * 100).toInt()}%",
                            color = Primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (fullscreenZoomState.scale > 1.05f) {
                        IconButton(
                            onClick = { fullscreenZoomState.reset() },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = "Reset Zoom",
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(Res.string.cancel),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Sheet allowing the user to customize the exercise name and category
 * or link to an existing exercise before saving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCustomExerciseSheet(
    entry: ScannedExerciseEntry,
    categories: List<String>,
    languageCode: String,
    onSave: (ScannedExerciseEntry, Boolean) -> Unit,
    onSelectFromCatalog: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nameText by remember(entry.rawName) { mutableStateOf(entry.matchedName ?: entry.rawName) }
    var selectedCat by remember(entry.suggestedCategory) {
        mutableStateOf(entry.suggestedCategory.ifBlank { categories.firstOrNull() ?: "Chest" })
    }
    var saveToLibrary by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        contentColor = OnSurface,
        tonalElevation = 0.dp
    ) {
        SheetFormLayout {
            SheetFormBody(horizontalPadding = ResponsiveSize.cardPadding, spacing = Spacing.medium) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.ai_scan_custom_options_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSurface
                        )
                        Text(
                            text = stringResource(Res.string.ai_scan_custom_options_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Name Input Field
                GymInputField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = stringResource(Res.string.ai_scan_exercise_name_label),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.ai_scan_category_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )

                    val availableCategories = if (categories.isNotEmpty()) categories else listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Cardio")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableCategories.forEach { cat ->
                            val isSelected = cat.equals(selectedCat, ignoreCase = true)
                            val translatedCat = ExerciseTranslations.translateCategory(cat, languageCode)

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCat = cat },
                                label = { Text(translatedCat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = OnPrimary,
                                    selectedLeadingIconColor = OnPrimary,
                                    containerColor = SurfaceContainerHigh,
                                    labelColor = OnSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Save to reusable library switch
                Card(
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveToLibrary = !saveToLibrary }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.ai_scan_save_to_library_checkbox),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = saveToLibrary,
                            onCheckedChange = { saveToLibrary = it },
                            thumbContent = if (saveToLibrary) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                        tint = Primary
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
            SheetFormFooter(horizontalPadding = ResponsiveSize.cardPadding) {
                // Actions: Link to catalog OR Save edits
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GymButton(
                        onClick = {
                            val isCardio = selectedCat.equals("Cardio", ignoreCase = true)
                            onSave(
                                entry.copy(
                                    rawName = nameText.trim().ifBlank { entry.rawName },
                                    matchedName = nameText.trim().ifBlank { entry.rawName },
                                    suggestedCategory = selectedCat,
                                    cardioMinutes = if (isCardio) (entry.cardioMinutes ?: 20) else null
                                ),
                                saveToLibrary
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(Res.string.ai_scan_save_changes).uppercase(), fontWeight = FontWeight.Black)
                    }

                    OutlinedButton(
                        onClick = onSelectFromCatalog,
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium
                    ) {
                        Icon(
                            Icons.Rounded.Link,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.ai_scan_select_from_catalog),
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/** The exercises of the review: a header with the count, the joined rows, then "add a missing exercise". */
private fun LazyListScope.scanReviewList(
    entries: List<ScannedExerciseEntry>,
    rowIds: List<Long>,
    openId: Long?,
    seenIds: List<Long>,
    showHeader: Boolean,
    languageCode: String,
    onToggle: (Int) -> Unit,
    onUpdate: (Int, ScannedExerciseEntry) -> Unit,
    onRemove: (Int) -> Unit,
    onChangeExercise: (Int) -> Unit,
    onAddExerciseClick: () -> Unit,
    footer: (@Composable () -> Unit)?
) {
    if (showHeader) item(key = "header") {
        Text(
            text = "${stringResource(Res.string.compare_exercises)} · ${entries.size}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
    }
    if (!showHeader) item(key = "header") { Spacer(Modifier.height(0.dp)) }
    itemsIndexed(entries, key = { index, _ -> rowIds.getOrElse(index) { -1L - index } }) { index, entry ->
        val id = rowIds.getOrNull(index)
        ScanReviewRow(
            entry = entry,
            number = index + 1,
            position = joinedPosition(index, entries.size),
            expanded = id != null && openId == id,
            seen = id != null && id in seenIds,
            languageCode = languageCode,
            onToggle = { onToggle(index) },
            onUpdate = { onUpdate(index, it) },
            onRemove = { onRemove(index) },
            onChangeExercise = { onChangeExercise(index) },
            modifier = Modifier.animateItem()
        )
    }
    item(key = "add") {
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(Shapes.small)
                .clickable(onClick = onAddExerciseClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(Res.string.ai_scan_add_missing_exercise),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
    }
    footer?.let { content -> item(key = "footer") { Box(modifier = Modifier.padding(top = 8.dp)) { content() } } }
}

@Composable
private fun ActionButtonsSection(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        GymButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            containerColor = SurfaceContainerHigh,
            contentColor = OnSurfaceVariant
        ) {
            Text(stringResource(Res.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
        }

        GymButton(
            onClick = onConfirm,
            enabled = count > 0,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(Res.string.ai_scan_add_count, count).uppercase(), fontWeight = FontWeight.Black)
        }
    }
}

/** Longer than a row's expand/collapse animation. */
private const val ROW_ANIMATION_SETTLE_MS = 350L
