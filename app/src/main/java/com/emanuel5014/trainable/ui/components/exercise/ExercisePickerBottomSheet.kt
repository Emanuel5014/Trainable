package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.exercise.ExercisePickerFilter
import com.emanuel5014.trainable.ui.screens.exercise.ExerciseDetailDialog
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.rememberResponsiveSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerBottomSheet(
    exercises: List<ExerciseEntity>,
    categories: List<String>,
    onExerciseSelected: (ExerciseEntity) -> Unit,
    onAddCustomExercise: (String, String, (ExerciseEntity) -> Unit) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    languageCode: String = "en",
    /** Only to choose one: no way to add, edit, delete or open the details of an exercise (e.g. picking what to merge into). */
    pickOnly: Boolean = false,
    /** Replaces the line under the title, e.g. to say what the choice is for. */
    hint: String? = null
) {
    rememberResponsiveSize()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showOnlyCustom by remember { mutableStateOf(false) }
    var showOnlyFavorites by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var exerciseMediaToView by remember { mutableStateOf<ExerciseEntity?>(null) }
    var detailExerciseId by rememberSaveable { mutableStateOf<Int?>(null) }

    val filteredExercises = remember(exercises, searchQuery, selectedCategory, showOnlyCustom, showOnlyFavorites, languageCode) {
        ExercisePickerFilter.apply(exercises, searchQuery, selectedCategory, showOnlyCustom, showOnlyFavorites, languageCode)
    }

    val showThumbnails = rememberShowExerciseThumbnails(exercises)

    val allLabel = stringResource(R.string.all_categories)
    val favoritesLabel = stringResource(R.string.filter_favorites)
    val customLabel = stringResource(R.string.filter_custom)
    // A category can combine with the favorites or the custom ones, so each button is on by itself
    val filterOptions = buildList {
        add(
            ExerciseFilterOption(allLabel, selectedCategory == null && !showOnlyCustom && !showOnlyFavorites) {
                selectedCategory = null
                showOnlyCustom = false
                showOnlyFavorites = false
            }
        )
        if (ExercisePickerFilter.showFavoritesChip(exercises, emptySet(), showOnlyFavorites)) {
            add(ExerciseFilterOption(favoritesLabel, showOnlyFavorites) { showOnlyFavorites = !showOnlyFavorites })
        }
        if (!pickOnly) add(ExerciseFilterOption(customLabel, showOnlyCustom) { showOnlyCustom = !showOnlyCustom })
        categories.forEach { category ->
            add(
                ExerciseFilterOption(ExerciseTranslations.translateCategory(category, languageCode), selectedCategory == category) {
                    selectedCategory = category
                }
            )
        }
    }

    val imeVisible = isKeyboardVisible

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(OnSurfaceVariant.copy(alpha = 0.4f))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ResponsiveSize.cardPadding)
                .padding(bottom = ResponsiveSize.cardPadding)
        ) {
            // Title and the custom-exercise button make way for the list while the keyboard is up
            AnimatedVisibility(visible = !imeVisible) {
                Column {
                Text(
                    text = stringResource(R.string.select_exercise).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = ResponsiveSize.labelLargeSize
                    ),
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(Spacing.xtraSmall))
                Text(
                    text = stringResource(R.string.choose_from_library),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = ResponsiveSize.headlineMediumSize
                    ),
                    color = OnSurface,
                    fontWeight = FontWeight.Black
                )

                if (hint != null) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.large))
                }
            }

            ExerciseSearchField(query = searchQuery, onQueryChange = { searchQuery = it })

            Spacer(modifier = Modifier.height(Spacing.medium))

            AnimatedVisibility(visible = !imeVisible) {
                Column {
                    ExerciseFilterBar(options = filterOptions)

                    Spacer(modifier = Modifier.height(Spacing.medium))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                if (filteredExercises.isEmpty()) item { ExerciseEmptyState() }
                items(filteredExercises) { exercise ->
                    ExercisePickRow(
                        exercise = exercise,
                        languageCode = languageCode,
                        onClick = {
                            onExerciseSelected(exercise)
                            onDismiss()
                        },
                        showThumbnail = showThumbnails,
                        onThumbnailClick = { exerciseMediaToView = exercise },
                        onInfoClick = if (pickOnly) null else { { detailExerciseId = exercise.id } }
                    )
                }
            }

            AnimatedVisibility(visible = !imeVisible && !pickOnly) {
                Column {
                    Spacer(modifier = Modifier.height(Spacing.medium))

                    GymButton(
                        onClick = { showAddCustomDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurface
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(Spacing.medium))
                        Text(stringResource(R.string.add_custom_exercise), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    if (showAddCustomDialog) {
        ExerciseFormDialog(
            title = stringResource(R.string.new_exercise),
            categories = categories,
            existingExercises = rememberNamesInUse(),
            languageCode = languageCode,
            showDescription = false,
            onConfirm = { form ->
                onAddCustomExercise(form.name, form.category) { createdExercise ->
                    onExerciseSelected(createdExercise)
                    onDismiss()
                }
                showAddCustomDialog = false
            },
            onDismiss = { showAddCustomDialog = false }
        )
    }

    ExerciseMediaPeek(exerciseMediaToView, languageCode) { exerciseMediaToView = null }
    detailExerciseId?.let { ExerciseDetailDialog(exerciseId = it, onDismiss = { detailExerciseId = null }) }

}
