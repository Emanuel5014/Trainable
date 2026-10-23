package com.emanuel5014.trainable.ui.screens.exercises

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.exercise.ExerciseFilter
import com.emanuel5014.trainable.domain.exercise.ExerciseListFilter
import com.emanuel5014.trainable.ui.components.ExerciseFormDialog
import com.emanuel5014.trainable.ui.components.ExerciseMediaMessageToasts
import com.emanuel5014.trainable.ui.components.ExerciseMediaViewer
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.rememberExerciseMediaPicker
import com.emanuel5014.trainable.ui.screens.exercise.ExerciseDetailDialog
import com.emanuel5014.trainable.ui.screens.settings.SettingsSwitch
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/**
 * Where the exercises are managed: the list with its filters, a way to create one, the media of each, and (further down)
 * the preset switch and the custom categories. Replaces the old media and customization screens.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun ExercisesScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExercisesViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val namesInUse by viewModel.namesInUse.collectAsState()
    val mediaEnabled by viewModel.mediaEnabled.collectAsState()
    val largePreview by viewModel.largePreview.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState()
    val editablePresets by viewModel.editablePresetExercises.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(ExerciseFilter.ALL) }
    var viewerExerciseId by rememberSaveable { mutableStateOf<Int?>(null) }
    var detailExerciseId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showRemoveAll by remember { mutableStateOf(false) }
    var showNewExercise by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    var showResetNames by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val resetDoneText = stringResource(R.string.reset_exercise_names)

    // A new exercise opens its detail, where the media is added
    LaunchedEffect(viewModel) { viewModel.createdExerciseId.collect { detailExerciseId = it } }

    val pickMedia = rememberExerciseMediaPicker { exerciseId, uri -> viewModel.attach(exerciseId, uri) }
    ExerciseMediaMessageToasts(viewModel.messages)

    val all = exercises
    val library = remember(all) { all.orEmpty().filter { !it.isDeleted } }
    val hiddenCount = all.orEmpty().size - library.size
    val withMediaCount = library.count { it.mediaPath != null }
    val groups = remember(all, query, filter, languageCode) {
        ExerciseListFilter.groups(all.orEmpty(), query, filter, languageCode)
    }
    val listState = rememberLazyListState()
    // The button shrinks to its icon as soon as the list scrolls, like the start button of a routine
    val fabExpanded by remember { derivedStateOf { !listState.canScrollBackward } }

    viewerExerciseId?.let { exerciseId ->
        val exercise = all.orEmpty().firstOrNull { it.id == exerciseId }
        val mediaFile = exercise?.mediaPath
        // The file goes away when it is removed from the viewer: close it then
        LaunchedEffect(mediaFile) { if (mediaFile == null) viewerExerciseId = null }
        if (exercise != null && mediaFile != null) {
            ExerciseMediaViewer(
                fileName = mediaFile,
                exerciseName = ExerciseTranslations.translate(exercise.nome, languageCode),
                onDismiss = { viewerExerciseId = null },
                onReplace = { pickMedia(exerciseId) },
                onRemove = { viewModel.remove(exerciseId) }
            )
        }
    }

    detailExerciseId?.let { exerciseId ->
        ExerciseDetailDialog(exerciseId = exerciseId, onDismiss = { detailExerciseId = null })
    }

    if (showNewExercise) {
        ExerciseFormDialog(
            title = stringResource(R.string.new_exercise),
            categories = categories,
            existingExercises = namesInUse,
            languageCode = languageCode,
            onConfirm = { form ->
                showNewExercise = false
                viewModel.createExercise(form.name, form.category, form.description)
            },
            onDismiss = { showNewExercise = false }
        )
    }

    if (showCategories) {
        CategoriesSheet(
            categories = customCategories,
            onAdd = viewModel::addCategory,
            onEdit = viewModel::updateCategory,
            onDelete = viewModel::deleteCategory,
            onDismiss = { showCategories = false }
        )
    }

    if (showResetNames) {
        ResetNamesDialog(
            onConfirm = {
                showResetNames = false
                viewModel.resetPresetExerciseNames {
                    Toast.makeText(context, resetDoneText, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showResetNames = false }
        )
    }

    if (showRemoveAll) {
        AlertDialog(
            onDismissRequest = { showRemoveAll = false },
            containerColor = SurfaceContainerHigh,
            shape = Shapes.extraLarge,
            title = {
                Text(
                    stringResource(R.string.exercise_media_remove_all_title),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = { Text(stringResource(R.string.exercise_media_remove_all_message), color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveAll = false
                    viewModel.removeAll()
                }) {
                    Text(stringResource(R.string.exercise_media_remove_all).uppercase(), color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveAll = false }) {
                    Text(stringResource(R.string.cancel).uppercase(), color = Primary)
                }
            }
        )
    }

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.exercises_title),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        GymIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onClick = onNavigateBack,
                            containerColor = Color.Transparent,
                            contentColor = OnSurface,
                            description = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = OnSurface
                )
            )
        },
        floatingActionButton = {
            // Not before the list is there: the name check of the form needs it
            if (all != null) ExtendedFloatingActionButton(
                onClick = { showNewExercise = true },
                expanded = fabExpanded,
                // Shrunk to its icon, the button still says what it does
                icon = {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = if (fabExpanded) null else stringResource(R.string.new_exercise)
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.new_exercise),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                containerColor = Primary,
                contentColor = OnPrimary,
                shape = RoundedCornerShape(20.dp)
            )
        }
    ) { paddingValues ->
        // On a wide screen the list keeps a readable width, centred
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxHeight()
                .imePadding(),
            // Room at the bottom so the button never covers the last row
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "display_settings") {
                DisplaySettingsCard(
                    mediaEnabled = mediaEnabled,
                    largePreview = largePreview,
                    onMediaEnabledChange = viewModel::setMediaEnabled,
                    onLargePreviewChange = viewModel::setLargePreview,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item(key = "personalization") {
                PersonalizationCard(
                    editablePresets = editablePresets,
                    onEditablePresetsChange = viewModel::setEditablePresetExercises,
                    onResetNames = { showResetNames = true },
                    onOpenCategories = { showCategories = true },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item(key = "summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.exercise_media_count, withMediaCount, library.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (withMediaCount > 0) {
                        TextButton(onClick = { showRemoveAll = true }) {
                            Text(
                                stringResource(R.string.exercise_media_remove_all).uppercase(),
                                color = Error,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            item(key = "search") {
                GymInputField(
                    value = query,
                    onValueChange = { query = it },
                    label = stringResource(R.string.search_exercises),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = OnSurfaceVariant) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close), tint = OnSurfaceVariant)
                            }
                        }
                    } else null
                )
            }

            item(key = "filters") {
                FilterBar(
                    filters = ExerciseListFilter.filtersToShow(hiddenCount, filter),
                    selected = filter,
                    onSelect = { filter = it }
                )
            }

            if (all == null) {
                item(key = "loading") {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        GymLoadingIndicator()
                    }
                }
            } else if (groups.isEmpty()) {
                item(key = "empty") { EmptyState() }
            }

            groups.forEach { group ->
                stickyHeader(key = "category_${group.category}") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface)
                            .padding(top = 12.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = group.category.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                items(group.entries, key = { it.exercise.id }) { entry ->
                    val exercise = entry.exercise
                    ExerciseListRow(
                        name = entry.name,
                        category = group.category,
                        mediaFile = exercise.mediaPath,
                        isCustom = exercise.id >= ExerciseEntity.FIRST_CUSTOM_ID,
                        isHidden = exercise.isDeleted,
                        onThumbnailClick = {
                            if (exercise.mediaPath != null) viewerExerciseId = exercise.id else pickMedia(exercise.id)
                        },
                        onClick = { detailExerciseId = exercise.id },
                        // A hidden exercise is not for picking media: its detail is where it is restored
                        mediaAction = filter != ExerciseFilter.HIDDEN,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
        }
    }
}

/** The filters as a connected group of toggle buttons that scrolls sideways when the labels do not fit. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FilterBar(
    filters: List<ExerciseFilter>,
    selected: ExerciseFilter,
    onSelect: (ExerciseFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        filters.forEachIndexed { index, option ->
            ToggleButton(
                checked = option == selected,
                onCheckedChange = { onSelect(option) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    filters.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(stringResource(labelOf(option)), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

private fun labelOf(filter: ExerciseFilter): Int = when (filter) {
    ExerciseFilter.ALL -> R.string.all
    ExerciseFilter.CUSTOM -> R.string.filter_custom
    ExerciseFilter.WITH_MEDIA -> R.string.exercise_media_filter_with
    ExerciseFilter.WITHOUT_MEDIA -> R.string.exercise_media_filter_without
    ExerciseFilter.HIDDEN -> R.string.exercise_media_filter_hidden
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(Primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = Primary, modifier = Modifier.size(32.dp))
        }
        Text(
            text = stringResource(R.string.no_exercises_found),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** The two media display switches, moved here from the old media screen. */
@Composable
private fun DisplaySettingsCard(
    mediaEnabled: Boolean,
    largePreview: Boolean,
    onMediaEnabledChange: (Boolean) -> Unit,
    onLargePreviewChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    GymCard(modifier = modifier, containerColor = SurfaceContainerHigh) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Rounded.Collections,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            stringResource(R.string.exercise_media_show),
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurface,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            stringResource(R.string.exercise_media_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                SettingsSwitch(checked = mediaEnabled, onCheckedChange = onMediaEnabledChange)
            }

            AnimatedVisibility(visible = mediaEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.exercise_media_large),
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                stringResource(R.string.exercise_media_large_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(checked = largePreview, onCheckedChange = onLargePreviewChange)
                    }
                }
            }
        }
    }
}
