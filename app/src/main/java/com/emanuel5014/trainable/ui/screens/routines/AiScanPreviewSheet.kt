package com.emanuel5014.trainable.ui.screens.routines

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.ai.ScannedExerciseEntry
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.ui.components.ExercisePickerBottomSheet
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScanPreviewSheet(
    imageUri: Uri? = null,
    entries: List<ScannedExerciseEntry>,
    exercises: List<ExerciseEntity>,
    categories: List<String>,
    languageCode: String,
    editablePresetExercises: Boolean,
    onAddCustomExercise: (String, String, (ExerciseEntity) -> Unit) -> Unit,
    onConfirm: (List<ScannedExerciseEntry>, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editableEntries = remember { mutableStateListOf(*entries.toTypedArray()) }
    var pickingIndex by remember { mutableStateOf<Int?>(null) }
    var customEditIndex by remember { mutableStateOf<Int?>(null) }
    var isAddingNewExercise by remember { mutableStateOf(false) }
    var saveImageToPlan by remember { mutableStateOf(imageUri != null) }
    var showFullscreenPhoto by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // System BackHandler ensuring the back button always dismisses cleanly
    val isChildSheetOpen = pickingIndex != null || customEditIndex != null || showFullscreenPhoto || isAddingNewExercise || showConfirmDialog
    BackHandler(enabled = !isChildSheetOpen) {
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        contentColor = OnSurface,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(OnSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                // Adaptive 2-Column Split for tablets and foldables
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ResponsiveSize.cardPadding)
                        .padding(bottom = ResponsiveSize.cardPadding),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.large)
                ) {
                    // Left Column: Photo Inspector
                    if (imageUri != null) {
                        val tabletZoomState = rememberZoomState()

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(0.85f),
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
                                        text = stringResource(R.string.ai_scan_original_sheet),
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
                                        IconButton(
                                            onClick = { tabletZoomState.reset() },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.RestartAlt,
                                                contentDescription = "Reset Zoom",
                                                tint = OnSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { showFullscreenPhoto = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Fullscreen,
                                            contentDescription = stringResource(R.string.ai_scan_fullscreen_photo),
                                            tint = OnSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            ZoomablePhotoInspector(
                                imageUri = imageUri,
                                zoomState = tabletZoomState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }

                    // Right Column: Exercise list & actions
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        HeaderSection()

                        if (imageUri != null) {
                            SaveImageToggleCard(
                                checked = saveImageToPlan,
                                onCheckedChange = { saveImageToPlan = it }
                            )
                        }

                        ExerciseListSection(
                            entries = editableEntries,
                            languageCode = languageCode,
                            onRemove = { editableEntries.removeAt(it) },
                            onUpdate = { index, updated -> editableEntries[index] = updated },
                            onChangeExercise = { index ->
                                val entry = editableEntries.getOrNull(index)
                                if (entry?.exerciseId == null) {
                                    customEditIndex = index
                                } else {
                                    pickingIndex = index
                                }
                            },
                            onAddExerciseClick = { isAddingNewExercise = true }
                        )

                        ActionButtonsSection(
                            onDismiss = onDismiss,
                            onConfirm = { showConfirmDialog = true },
                            canConfirm = editableEntries.isNotEmpty()
                        )
                    }
                }
            } else {
                // Single Column (Compact Phone)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = ResponsiveSize.cardPadding)
                        .padding(bottom = ResponsiveSize.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    HeaderSection()

                    // Collapsible Photo Inspector for Phone
                    if (imageUri != null) {
                        CollapsiblePhotoInspector(
                            imageUri = imageUri,
                            onOpenFullscreen = { showFullscreenPhoto = true }
                        )

                        SaveImageToggleCard(
                            checked = saveImageToPlan,
                            onCheckedChange = { saveImageToPlan = it }
                        )
                    }

                    ExerciseListSection(
                        entries = editableEntries,
                        languageCode = languageCode,
                        onRemove = { editableEntries.removeAt(it) },
                        onUpdate = { index, updated -> editableEntries[index] = updated },
                        onChangeExercise = { index ->
                            val entry = editableEntries.getOrNull(index)
                            if (entry?.exerciseId == null) {
                                customEditIndex = index
                            } else {
                                pickingIndex = index
                            }
                        },
                        onAddExerciseClick = { isAddingNewExercise = true }
                    )

                    ActionButtonsSection(
                        onDismiss = onDismiss,
                        onConfirm = { showConfirmDialog = true },
                        canConfirm = editableEntries.isNotEmpty()
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.ai_scan_confirm_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.ai_scan_confirm_dialog_desc, editableEntries.size),
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
                        text = stringResource(R.string.ai_scan_confirm_dialog_confirm),
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
                        text = stringResource(R.string.ai_scan_confirm_dialog_review),
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
            onEditCustomExercise = { },
            onDeleteCustomExercise = { },
            onDismiss = { pickingIndex = null },
            languageCode = languageCode,
            editablePresetExercises = editablePresetExercises
        )
    }

    // Exercise Picker for adding a missing exercise
    if (isAddingNewExercise) {
        ExercisePickerBottomSheet(
            exercises = exercises,
            categories = categories,
            onExerciseSelected = { exercise ->
                val isCardio = exercise.categoria.equals("Cardio", ignoreCase = true)
                editableEntries.add(
                    ScannedExerciseEntry(
                        rawName = exercise.nome,
                        exerciseId = exercise.id,
                        matchedName = exercise.nome,
                        suggestedCategory = exercise.categoria,
                        sets = if (isCardio) 1 else 3,
                        reps = if (isCardio) "1" else "8-12",
                        restSeconds = 120,
                        cardioMinutes = if (isCardio) 20 else null
                    )
                )
                isAddingNewExercise = false
            },
            onAddCustomExercise = { nome, categoria, onCreated ->
                onAddCustomExercise(nome, categoria) { created ->
                    onCreated(created)
                }
            },
            onEditCustomExercise = { },
            onDeleteCustomExercise = { },
            onDismiss = { isAddingNewExercise = false },
            languageCode = languageCode,
            editablePresetExercises = editablePresetExercises
        )
    }
}

@Composable
private fun HeaderSection() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.ai_scan_preview_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineSmall.fontSize)
                ),
                color = OnSurface,
                fontWeight = FontWeight.Black
            )
            Text(
                text = stringResource(R.string.ai_scan_preview_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
    }
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
                    text = stringResource(R.string.ai_scan_save_image_to_routine),
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
private fun CollapsiblePhotoInspector(
    imageUri: Uri,
    onOpenFullscreen: () -> Unit
) {
    var isCollapsed by remember { mutableStateOf(false) }
    val zoomState = rememberZoomState()

    Card(
        shape = Shapes.large,
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OutlineVariant.copy(alpha = 0.4f), Shapes.large)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar with all actions in the top bar outside the photo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isCollapsed = !isCollapsed }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Photo,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.ai_scan_original_sheet),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AnimatedVisibility(
                        visible = !isCollapsed && zoomState.scale > 1.05f,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            onClick = { zoomState.reset() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.RestartAlt,
                                contentDescription = "Reset Zoom",
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenFullscreen,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Fullscreen,
                            contentDescription = stringResource(R.string.ai_scan_fullscreen_photo),
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { isCollapsed = !isCollapsed },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isCollapsed) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                            contentDescription = if (isCollapsed) stringResource(R.string.ai_scan_expand_photo) else stringResource(R.string.ai_scan_collapse_photo),
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = !isCollapsed) {
                ZoomablePhotoInspector(
                    imageUri = imageUri,
                    zoomState = zoomState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ZoomablePhotoInspector(
    imageUri: Uri,
    zoomState: ZoomState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    // Load full original image resolution so text is sharp when zoomed
    val imageRequest = remember(imageUri) {
        ImageRequest.Builder(context)
            .data(imageUri)
            .size(coil.size.Size.ORIGINAL)
            .crossfade(true)
            .build()
    }

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

                    if (newScale <= 1f) {
                        zoomState.offsetX = 0f
                        zoomState.offsetY = 0f
                    } else {
                        val maxX = ((imageSize.width * newScale) - imageSize.width) / 2f
                        val maxY = ((imageSize.height * newScale) - imageSize.height) / 2f

                        // 1:1 pan precision with finger movement
                        val newOffsetX = zoomState.offsetX + pan.x
                        val newOffsetY = zoomState.offsetY + pan.y

                        zoomState.offsetX = newOffsetX.coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f))
                        zoomState.offsetY = newOffsetY.coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))
                    }
                }
            }
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = stringResource(R.string.ai_scan_original_sheet),
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { imageSize = it }
                .graphicsLayer(
                    scaleX = zoomState.scale,
                    scaleY = zoomState.scale,
                    translationX = zoomState.offsetX,
                    translationY = zoomState.offsetY
                ),
            contentScale = ContentScale.Fit
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
                text = stringResource(R.string.ai_scan_zoom_hint),
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
    imageUri: Uri,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val fullscreenZoomState = rememberZoomState()
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    val imageRequest = remember(imageUri) {
        ImageRequest.Builder(context)
            .data(imageUri)
            .size(coil.size.Size.ORIGINAL)
            .crossfade(true)
            .build()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
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
            AsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.ai_scan_original_sheet),
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { imageSize = it }
                    .graphicsLayer(
                        scaleX = fullscreenZoomState.scale,
                        scaleY = fullscreenZoomState.scale,
                        translationX = fullscreenZoomState.offsetX,
                        translationY = fullscreenZoomState.offsetY
                    ),
                contentScale = ContentScale.Fit
            )

            // Top Bar Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                        text = stringResource(R.string.ai_scan_original_sheet),
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
                            contentDescription = stringResource(R.string.cancel),
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ResponsiveSize.cardPadding)
                .padding(bottom = ResponsiveSize.cardPadding)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
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
                        text = stringResource(R.string.ai_scan_custom_options_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                    Text(
                        text = stringResource(R.string.ai_scan_custom_options_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }

            // Name Input Field
            GymInputField(
                value = nameText,
                onValueChange = { nameText = it },
                label = stringResource(R.string.ai_scan_exercise_name_label),
                modifier = Modifier.fillMaxWidth()
            )

            // Category Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.ai_scan_category_label),
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
                        text = stringResource(R.string.ai_scan_save_to_library_checkbox),
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
                    Text(stringResource(R.string.ai_scan_save_changes).uppercase(), fontWeight = FontWeight.Black)
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
                        text = stringResource(R.string.ai_scan_select_from_catalog),
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseListSection(
    entries: List<ScannedExerciseEntry>,
    languageCode: String,
    onRemove: (Int) -> Unit,
    onUpdate: (Int, ScannedExerciseEntry) -> Unit,
    onChangeExercise: (Int) -> Unit,
    onAddExerciseClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(R.string.compare_exercises)} (${entries.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )

            Row(
                modifier = Modifier
                    .clip(Shapes.small)
                    .clickable { onAddExerciseClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.ai_scan_add_missing_exercise),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }

        entries.forEachIndexed { index, entry ->
            ScanEntryCard(
                entry = entry,
                index = index,
                languageCode = languageCode,
                onRemove = { onRemove(index) },
                onUpdate = { updated -> onUpdate(index, updated) },
                onChangeExercise = { onChangeExercise(index) }
            )
        }
    }
}

@Composable
private fun ScanEntryCard(
    entry: ScannedExerciseEntry,
    index: Int,
    languageCode: String,
    onRemove: () -> Unit,
    onUpdate: (ScannedExerciseEntry) -> Unit,
    onChangeExercise: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var setsText by remember(entry.rawName, index) { mutableStateOf(entry.sets.toString()) }
    var repsText by remember(entry.rawName, index) { mutableStateOf(entry.reps) }

    val displayTitle = if (entry.exerciseId != null) {
        ExerciseTranslations.translate(entry.matchedName ?: entry.rawName, languageCode)
    } else {
        ExerciseTranslations.translate(entry.rawName, languageCode)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.large)
            .background(SurfaceContainerHigh)
            .padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (entry.exerciseId != null) Primary else Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (entry.exerciseId != null) OnPrimary else Primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onChangeExercise() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (entry.exerciseId == null) {
                        Text(
                            text = stringResource(R.string.ai_scan_custom_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .background(Primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (entry.exerciseId == null) {
                        Text(
                            text = stringResource(R.string.ai_scan_custom_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary
                        )
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.ai_scan_tap_to_change),
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cancel),
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (entry.isCardio) {
            CardioDurationSlider(
                valueMinutes = entry.cardioMinutes ?: 20,
                onValueChange = { onUpdate(entry.copy(cardioMinutes = it)) },
                hapticEnabled = true,
                haptic = haptic,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                GymInputField(
                    value = setsText,
                    onValueChange = { value ->
                        setsText = value
                        value.toIntOrNull()?.let { onUpdate(entry.copy(sets = it)) }
                    },
                    label = stringResource(R.string.sets),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                GymInputField(
                    value = repsText,
                    onValueChange = { value ->
                        repsText = value
                        onUpdate(entry.copy(reps = value))
                    },
                    label = stringResource(R.string.reps),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        RestSlider(
            value = entry.restSeconds,
            onValueChange = { onUpdate(entry.copy(restSeconds = it)) },
            hapticEnabled = true,
            haptic = haptic,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RestSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    hapticEnabled: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    val steps = listOf(0, 30, 60, 90, 120, 180, 240, 300)
    val currentIndex = remember(value) {
        val exactIndex = steps.indexOf(value)
        if (exactIndex != -1) exactIndex
        else steps.indexOf(steps.minByOrNull { kotlin.math.abs(it - value) } ?: 120).coerceAtLeast(0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.rest_seconds),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${value}s",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = formatRestTime(value),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
        }

        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { rawValue ->
                val index = kotlin.math.round(rawValue).toInt()
                val clampedIndex = index.coerceIn(0, steps.size - 1)
                if (clampedIndex != currentIndex) {
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(steps[clampedIndex])
                }
            },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SurfaceContainerHighest
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CardioDurationSlider(
    valueMinutes: Int,
    onValueChange: (Int) -> Unit,
    hapticEnabled: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    val steps = listOf(5, 10, 15, 20, 25, 30, 45, 60, 90, 120)
    val closestIndex = remember(valueMinutes) {
        steps.indexOf(steps.minByOrNull { kotlin.math.abs(it - valueMinutes) } ?: 20).coerceAtLeast(0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.cardio_duration_slider),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${valueMinutes} min",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold
            )
            val displayMinutes = valueMinutes / 60
            val displaySecs = valueMinutes % 60
            if (displayMinutes > 0 || displaySecs > 0) {
                Text(
                    text = when {
                        displayMinutes == 0 -> "${displaySecs}s"
                        displaySecs == 0 -> "${displayMinutes}m"
                        else -> "${displayMinutes}m ${displaySecs}s"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }

        Slider(
            value = closestIndex.toFloat(),
            onValueChange = { rawValue ->
                val index = kotlin.math.round(rawValue).toInt()
                val clampedIndex = index.coerceIn(0, steps.size - 1)
                if (clampedIndex != closestIndex) {
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(steps[clampedIndex])
                }
            },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SurfaceContainerHighest
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun formatRestTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return when {
        minutes == 0 -> "${secs}s"
        secs == 0 -> "${minutes}m"
        else -> "${minutes}m ${secs}s"
    }
}

@Composable
private fun ActionButtonsSection(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    canConfirm: Boolean
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
            Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
        }

        GymButton(
            onClick = onConfirm,
            enabled = canConfirm,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.ai_scan_add_all).uppercase(), fontWeight = FontWeight.Black)
        }
    }
}


