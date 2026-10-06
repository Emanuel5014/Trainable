package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.ui.components.ExerciseMediaMessageToasts
import com.emanuel5014.trainable.ui.components.ExerciseMediaSlot
import com.emanuel5014.trainable.ui.components.ExerciseMediaViewer
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.rememberExerciseMediaPicker
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

private enum class MediaFilter { ALL, WITH_MEDIA, WITHOUT_MEDIA }

/** Side of the thumbnail at the start of each exercise row. */
private val RowThumbSize = 56.dp

/**
 * Settings -> Exercise Media: the display switches, plus the image/GIF of every exercise in one searchable,
 * category-grouped list. Tap a row to add a file (no media yet) or to view, replace or remove it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExerciseMediaScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExerciseMediaViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val mediaEnabled by viewModel.mediaEnabled.collectAsState()
    val largePreview by viewModel.largePreview.collectAsState()
    val languageCode by viewModel.languageCode.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(MediaFilter.ALL) }
    var viewerExerciseId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showRemoveAll by remember { mutableStateOf(false) }

    val pickMedia = rememberExerciseMediaPicker { exerciseId, uri -> viewModel.attach(exerciseId, uri) }
    ExerciseMediaMessageToasts(viewModel.messages)

    val withMediaCount = exercises.count { it.mediaPath != null }

    // Category -> (exercise, translated name), both ends sorted so the list reads like a glossary
    val groups = remember(exercises, query, filter, languageCode) {
        val needle = query.trim()
        exercises.asSequence()
            .filter {
                when (filter) {
                    MediaFilter.ALL -> true
                    MediaFilter.WITH_MEDIA -> it.mediaPath != null
                    MediaFilter.WITHOUT_MEDIA -> it.mediaPath == null
                }
            }
            .map { it to ExerciseTranslations.translate(it.nome, languageCode) }
            .filter { (exercise, name) ->
                needle.isEmpty() || name.contains(needle, ignoreCase = true) || exercise.nome.contains(needle, ignoreCase = true)
            }
            .groupBy { (exercise, _) -> ExerciseTranslations.translateCategory(exercise.categoria, languageCode) }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .mapValues { (_, items) -> items.sortedBy { it.second.lowercase() } }
    }

    viewerExerciseId?.let { exerciseId ->
        val exercise = exercises.firstOrNull { it.id == exerciseId }
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

    if (showRemoveAll) {
        AlertDialog(
            onDismissRequest = { showRemoveAll = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text(
                    stringResource(R.string.exercise_media_remove_all_title),
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
            },
            text = {
                Text(stringResource(R.string.exercise_media_remove_all_message), color = OnSurfaceVariant)
            },
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
                        stringResource(R.string.exercise_media),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleLarge
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = OnSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
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

            item(key = "summary") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.exercise_media_count, withMediaCount, exercises.size),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterOption(MediaFilter.ALL, filter, R.string.all) { filter = it }
                    FilterOption(MediaFilter.WITH_MEDIA, filter, R.string.exercise_media_filter_with) { filter = it }
                    FilterOption(MediaFilter.WITHOUT_MEDIA, filter, R.string.exercise_media_filter_without) { filter = it }
                }
            }

            if (groups.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.no_exercises_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            }

            groups.forEach { (category, entries) ->
                stickyHeader(key = "category_$category") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface)
                            .padding(top = 12.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = category.uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
                            color = Primary,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                items(entries, key = { (exercise, _) -> exercise.id }) { (exercise, name) ->
                    val mediaFile = exercise.mediaPath
                    ExerciseMediaRow(
                        name = name,
                        category = category,
                        fileName = mediaFile,
                        onClick = {
                            if (mediaFile != null) viewerExerciseId = exercise.id else pickMedia(exercise.id)
                        }
                    )
                }
            }
        }
    }
}

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
                    androidx.compose.material3.HorizontalDivider(color = Surface.copy(alpha = 0.5f))
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

@Composable
private fun FilterOption(
    option: MediaFilter,
    selected: MediaFilter,
    labelRes: Int,
    onSelect: (MediaFilter) -> Unit
) {
    FilterChip(
        selected = option == selected,
        onClick = { onSelect(option) },
        label = { Text(stringResource(labelRes)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary,
            selectedLabelColor = OnPrimary
        )
    )
}

@Composable
private fun ExerciseMediaRow(
    name: String,
    category: String,
    fileName: String?,
    onClick: () -> Unit
) {
    val clickLabel = stringResource(if (fileName != null) R.string.exercise_media_open else R.string.exercise_media_add)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerHigh)
            .clickable(onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // The whole row is the control: keep the thumbnail out of the accessibility tree so it is not read twice.
        // Still images only here, so a long list of GIFs does not animate all at once.
        Box(modifier = Modifier.clearAndSetSemantics { }) {
            ExerciseMediaSlot(
                fileName = fileName,
                onOpen = onClick,
                onAdd = onClick,
                size = RowThumbSize,
                playing = false
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = category,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
