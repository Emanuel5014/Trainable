package com.emanuel5014.trainable.ui.screens.physicalcheck

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.back
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.close
import com.emanuel5014.trainable.sharedui.resources.delete
import com.emanuel5014.trainable.sharedui.resources.edit
import com.emanuel5014.trainable.sharedui.resources.physical_check_add_check_cd
import com.emanuel5014.trainable.sharedui.resources.physical_check_compare_cd
import com.emanuel5014.trainable.sharedui.resources.physical_check_compare_count
import com.emanuel5014.trainable.sharedui.resources.physical_check_delete_message
import com.emanuel5014.trainable.sharedui.resources.physical_check_delete_photo_message
import com.emanuel5014.trainable.sharedui.resources.physical_check_delete_photo_title
import com.emanuel5014.trainable.sharedui.resources.physical_check_delete_title
import com.emanuel5014.trainable.sharedui.resources.physical_check_max_2
import com.emanuel5014.trainable.sharedui.resources.physical_check_new_button
import com.emanuel5014.trainable.sharedui.resources.physical_check_no_checks
import com.emanuel5014.trainable.sharedui.resources.physical_check_no_checks_desc
import com.emanuel5014.trainable.sharedui.resources.physical_check_pick_two
import com.emanuel5014.trainable.sharedui.resources.physical_check_settings_cd
import com.emanuel5014.trainable.sharedui.resources.physical_check_title
import com.emanuel5014.trainable.ui.components.EmptyState
import com.emanuel5014.trainable.ui.components.LocalHapticsSetting
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.components.QuickSwipeBox
import com.emanuel5014.trainable.ui.components.quickSwipeDanger
import com.emanuel5014.trainable.ui.components.quickSwipePrimary
import com.emanuel5014.trainable.ui.glass.actionGlass
import com.emanuel5014.trainable.ui.glass.actionGlassContainer
import com.emanuel5014.trainable.ui.glass.actionGlassElevation
import com.emanuel5014.trainable.ui.glass.actionGlassSource
import com.emanuel5014.trainable.ui.glass.rememberActionGlassSource
import com.emanuel5014.trainable.ui.screens.routines.routineRowCorners
import com.emanuel5014.trainable.ui.screens.routines.toShape
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.util.PlatformBackHandler
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/**
 * The physical checks: the biometric or password lock when one is set, else the checks grouped by month (newest
 * first) with the first photo as cover, opened in a bottom sheet. Two checks are picked in selection mode to compare.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhysicalCheckContent(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCompare: (Int, Int) -> Unit,
    viewModel: PhysicalCheckModel
) {
    val checks by viewModel.checks.collectAsState()
    val weightUnit by viewModel.weightUnit.collectAsState()
    val swipeActionsEnabled by viewModel.swipeActionsEnabled.collectAsState(initial = true)

    var showAddSheet by remember { mutableStateOf(false) }

    var selectedForCompare by remember { mutableStateOf(setOf<Int>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var checkToDelete by remember { mutableStateOf<PhysicalCheckEntity?>(null) }
    var checkToEdit by remember { mutableStateOf<PhysicalCheckEntity?>(null) }
    var openCheckId by remember { mutableStateOf<Int?>(null) }

    val haptic = LocalHapticFeedback.current
    val hapticEnabled by LocalHapticsSetting.current.collectAsState(initial = true)

    data class FullscreenState(val checkId: Int, val filenames: List<String>, val initialIndex: Int)
    var fullscreenState by remember { mutableStateOf<FullscreenState?>(null) }

    val platform = LocalPhysicalCheckPlatform.current
    val showMessage = LocalShowMessage.current
    val maxTwoText = stringResource(Res.string.physical_check_max_2)
    val languageCode = androidx.compose.ui.text.intl.Locale.current.language

    fun leaveSelection() {
        isSelectionMode = false
        selectedForCompare = emptySet()
    }

    // The fingerprint/PIN and the password come first; the screen and all its sheets and dialogs only behind them
    PhysicalCheckGate(viewModel, onNavigateBack) {
        val actionGlass = rememberActionGlassSource()
        val listState = rememberLazyListState()
        val changes = remember(checks) { weightChangesKg(checks) }
        val months = remember(checks, changes) { physicalCheckMonths(checks, changes, TimeZone.currentSystemDefault()) }
        val currentYear = remember(months) { Clock.System.todayIn(TimeZone.currentSystemDefault()).year }
        val addFabExpanded by remember { derivedStateOf { !listState.canScrollBackward } }

        PlatformBackHandler(isSelectionMode) { leaveSelection() }
        LaunchedEffect(isSelectionMode) { if (isSelectionMode) openCheckId = null }

        Scaffold(
            containerColor = Surface,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(if (isSelectionMode) Res.string.physical_check_pick_two else Res.string.physical_check_title),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (isSelectionMode) {
                            IconButton(onClick = { leaveSelection() }) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.close), tint = OnSurface)
                            }
                        } else {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back), tint = OnSurface)
                            }
                        }
                    },
                    actions = {
                        if (!isSelectionMode) {
                            if (checks.size >= 2) {
                                IconButton(onClick = { isSelectionMode = true }) {
                                    Icon(Icons.Rounded.Compare, contentDescription = stringResource(Res.string.physical_check_compare_cd), tint = OnSurface)
                                }
                            }
                            IconButton(onClick = {
                                viewModel.touch()
                                onNavigateToSettings()
                            }) {
                                Icon(Icons.Rounded.Security, contentDescription = stringResource(Res.string.physical_check_settings_cd), tint = OnSurface)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Surface,
                        scrolledContainerColor = Surface,
                        titleContentColor = OnSurface
                    )
                )
            },
            floatingActionButton = {
                if (isSelectionMode) {
                    if (selectedForCompare.size == 2) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                val list = checks.filter { it.id in selectedForCompare }.map { it.id }
                                leaveSelection()
                                onNavigateToCompare(list[0], list[1])
                            },
                            containerColor = actionGlassContainer(actionGlass, Primary),
                            contentColor = OnPrimary,
                            shape = Shapes.large,
                            elevation = actionGlassElevation(actionGlass),
                            modifier = Modifier.actionGlass(actionGlass, Primary)
                        ) {
                            Icon(Icons.Rounded.Compare, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.physical_check_compare_count), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    ExtendedFloatingActionButton(
                        onClick = {
                            viewModel.touch()
                            showAddSheet = true
                        },
                        expanded = addFabExpanded || checks.isEmpty(),
                        icon = { Icon(Icons.Rounded.AddAPhoto, contentDescription = stringResource(Res.string.physical_check_add_check_cd)) },
                        text = { Text(stringResource(Res.string.physical_check_new_button), fontWeight = FontWeight.ExtraBold) },
                        containerColor = actionGlassContainer(actionGlass, Primary),
                        contentColor = OnPrimary,
                        shape = Shapes.large,
                        elevation = actionGlassElevation(actionGlass),
                        modifier = Modifier.actionGlass(actionGlass, Primary)
                    )
                }
            }
        ) { paddingValues ->
            if (checks.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.PhotoLibrary,
                    title = stringResource(Res.string.physical_check_no_checks),
                    description = stringResource(Res.string.physical_check_no_checks_desc),
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .actionGlassSource(actionGlass)
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        start = ResponsiveSize.horizontalPadding,
                        end = ResponsiveSize.horizontalPadding,
                        top = 4.dp,
                        // Room for the button, so the last row can scroll above it
                        bottom = 104.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    months.forEachIndexed { monthIndex, month ->
                        item(key = "m-${month.year}-${month.month}", contentType = "month") {
                            Column {
                                if (monthIndex > 0) Spacer(modifier = Modifier.height(10.dp))
                                PhysicalCheckMonthHeader(month, weightUnit, currentYear, languageCode)
                            }
                        }
                        itemsIndexed(month.checks, key = { _, c -> c.id }, contentType = { _, _ -> "row" }) { index, check ->
                            val corners = routineRowCorners(index, month.checks.size)
                            val swipeOn = swipeActionsEnabled && !isSelectionMode
                            QuickSwipeBox(
                                startToEnd = if (swipeOn) quickSwipePrimary(Icons.Rounded.Edit, stringResource(Res.string.edit)) else null,
                                endToStart = if (swipeOn) quickSwipeDanger(Icons.Rounded.DeleteSweep, stringResource(Res.string.delete)) else null,
                                onSwiped = { direction ->
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    when (direction) {
                                        SwipeToDismissBoxValue.EndToStart -> checkToDelete = check
                                        SwipeToDismissBoxValue.StartToEnd -> checkToEdit = check
                                        SwipeToDismissBoxValue.Settled -> {}
                                    }
                                },
                                shape = corners.toShape()
                            ) {
                                PhysicalCheckRow(
                                    check = check,
                                    weightChangeKg = changes[check.id],
                                    corners = corners,
                                    weightUnit = weightUnit,
                                    languageCode = languageCode,
                                    model = viewModel,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = check.id in selectedForCompare,
                                    onClick = {
                                        viewModel.touch()
                                        if (isSelectionMode) {
                                            selectedForCompare = when {
                                                check.id in selectedForCompare -> selectedForCompare - check.id
                                                selectedForCompare.size < 2 -> selectedForCompare + check.id
                                                else -> {
                                                    showMessage(maxTwoText)
                                                    selectedForCompare
                                                }
                                            }
                                        } else {
                                            openCheckId = check.id
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode && checks.size >= 2) {
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isSelectionMode = true
                                            selectedForCompare = setOf(check.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // The opened check; it closes by itself when the check is deleted
        val openCheck = openCheckId?.let { id -> checks.firstOrNull { it.id == id } }
        LaunchedEffect(openCheckId, openCheck != null) {
            if (openCheckId != null && openCheck == null) openCheckId = null
        }
        if (openCheck != null) {
            PhysicalCheckDetailSheet(
                check = openCheck,
                weightChangeKg = changes[openCheck.id],
                weightUnit = weightUnit,
                model = viewModel,
                showEditDelete = !swipeActionsEnabled,
                canCompare = checks.size >= 2,
                onEdit = { checkToEdit = openCheck },
                onDelete = { checkToDelete = openCheck },
                onCompare = {
                    openCheckId = null
                    isSelectionMode = true
                    selectedForCompare = setOf(openCheck.id)
                },
                onAddPhotos = { checkToEdit = openCheck },
                onOpenPhoto = { filenames, index ->
                    viewModel.touch()
                    fullscreenState = FullscreenState(openCheck.id, filenames, index)
                },
                onDismiss = { openCheckId = null }
            )
        }

        if (showAddSheet) {
            PhysicalCheckEditorSheet(
                existing = null,
                model = viewModel,
                onDismiss = { showAddSheet = false },
                onSave = { timestamp, peso, note, photos ->
                    viewModel.addCheck(timestamp, peso, note, photos) {
                        showAddSheet = false
                    }
                }
            )
        }

        checkToDelete?.let { check ->
            AlertDialog(
                onDismissRequest = { checkToDelete = null },
                title = { Text(stringResource(Res.string.physical_check_delete_title)) },
                text = { Text(stringResource(Res.string.physical_check_delete_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteCheck(check)
                        checkToDelete = null
                    }) {
                        Text(stringResource(Res.string.delete), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { checkToDelete = null }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }

        checkToEdit?.let { check ->
            PhysicalCheckEditorSheet(
                existing = check,
                model = viewModel,
                onDismiss = { checkToEdit = null },
                onSave = { timestamp, peso, note, _ ->
                    viewModel.updateCheck(check.id, timestamp, peso, note) {
                        checkToEdit = null
                    }
                }
            )
        }

        fullscreenState?.let { state ->
            Dialog(
                onDismissRequest = { fullscreenState = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                PhysicalCheckFullscreenViewer(
                    filenames = state.filenames,
                    initialIndex = state.initialIndex,
                    viewModel = viewModel,
                    onDismiss = { fullscreenState = null },
                    onDeletePhoto = { index ->
                        val filename = state.filenames[index]
                        viewModel.deletePhotoFromCheck(state.checkId, filename)
                        val remaining = state.filenames.toMutableList().apply { removeAt(index) }
                        if (remaining.isEmpty()) {
                            fullscreenState = null
                        } else {
                            fullscreenState = state.copy(filenames = remaining, initialIndex = index.coerceAtMost(remaining.size - 1))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhysicalCheckFullscreenViewer(
    filenames: List<String>,
    initialIndex: Int,
    viewModel: PhysicalCheckModel,
    onDismiss: () -> Unit,
    onDeletePhoto: ((Int) -> Unit)? = null
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { filenames.size }
    var isZoomed by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed,
            beyondViewportPageCount = 1,
            // No stretch past the first and last photo, as in the sheet
            overscrollEffect = null
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ZoomableDecryptedImage(
                    filename = filenames[page],
                    viewModel = viewModel,
                    onZoomChanged = { zoomed ->
                        if (pagerState.currentPage == page) {
                            isZoomed = zoomed
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (filenames.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${filenames.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            } else {
                Spacer(modifier = Modifier)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDeletePhoto != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.close),
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.physical_check_delete_photo_title)) },
            text = { Text(stringResource(Res.string.physical_check_delete_photo_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val currentIndex = pagerState.currentPage
                    showDeleteConfirm = false
                    onDeletePhoto?.invoke(currentIndex)
                }) {
                    Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ZoomableDecryptedImage(
    filename: String,
    viewModel: PhysicalCheckModel,
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    val transformableState = rememberTransformableState { _, zoomChange, panChange, _ ->
        val oldScale = scale
        val newScale = (oldScale * zoomChange).coerceIn(1f, 5f)
        scale = newScale

        if (newScale <= 1f) {
            offsetX = 0f
            offsetY = 0f
            return@rememberTransformableState
        }

        val maxX = (imageSize.width * (newScale - 1f)) / 2f
        val maxY = (imageSize.height * (newScale - 1f)) / 2f

        val newOffsetX = offsetX + (panChange.x * newScale)
        val newOffsetY = offsetY + (panChange.y * newScale)

        offsetX = newOffsetX.coerceIn(-maxX, maxX)
        offsetY = newOffsetY.coerceIn(-maxY, maxY)
    }

    LaunchedEffect(scale) {
        onZoomChanged(scale > 1f)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        DecryptedImage(
            filename = filename,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .padding(16.dp)
                .onSizeChanged { imageSize = it }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .transformable(
                    state = transformableState,
                    canPan = { scale > 1f }
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun DecryptedImage(
    filename: String,
    viewModel: PhysicalCheckModel,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var imageBytes by remember(filename) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(filename) {
        imageBytes = viewModel.getPhotoBytes(filename)
    }

    if (imageBytes != null) {
        val bitmap = remember(imageBytes) {
            val bytes = imageBytes!!
            decodePhotoBitmap(bytes)
        }
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = modifier,
                contentScale = contentScale
            )
        } else {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}
