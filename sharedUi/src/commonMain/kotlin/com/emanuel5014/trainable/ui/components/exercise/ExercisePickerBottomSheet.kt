package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.all_exercises_header
import com.emanuel5014.trainable.sharedui.resources.all_categories
import com.emanuel5014.trainable.sharedui.resources.choose_from_library
import com.emanuel5014.trainable.sharedui.resources.filter_custom
import com.emanuel5014.trainable.sharedui.resources.filter_favorites
import com.emanuel5014.trainable.sharedui.resources.new_exercise
import com.emanuel5014.trainable.sharedui.resources.select_exercise
import com.emanuel5014.trainable.sharedui.resources.all_cardio
import com.emanuel5014.trainable.sharedui.resources.create_cardio_exercise
import com.emanuel5014.trainable.sharedui.resources.days_ago
import com.emanuel5014.trainable.sharedui.resources.select_activity
import com.emanuel5014.trainable.sharedui.resources.suggested
import com.emanuel5014.trainable.sharedui.resources.today
import com.emanuel5014.trainable.sharedui.resources.which_cardio
import com.emanuel5014.trainable.domain.exercise.ExerciseForm
import com.emanuel5014.trainable.domain.exercise.SuggestedExercise
import org.jetbrains.compose.resources.pluralStringResource
import kotlin.time.Clock
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.exercise.ExercisePickerFilter
import com.emanuel5014.trainable.domain.exercise.TraitFilter
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.rememberResponsiveSize
import com.emanuel5014.trainable.ui.util.uppercaseUi

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
    hint: String? = null,
    /** Lists only this category (ignoring case), hides the category chips and fixes it in the create form. */
    lockedCategory: String? = null,
    /** Shown on top while the search is empty; only meant for the cardio entry. */
    suggested: List<SuggestedExercise> = emptyList(),
    /**
     * Asked before a new exercise is created from the form: a message refuses the name, and the form stays open with it
     * under the name until the name changes. Null creates at once.
     */
    refuseNewExercise: (suspend (name: String) -> String?)? = null
) {
    val platformUi = LocalExercisePlatformUi.current
    rememberResponsiveSize()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showOnlyCustom by remember { mutableStateOf(false) }
    var showOnlyFavorites by remember { mutableStateOf(false) }
    var traitFilter by remember { mutableStateOf(TraitFilter.NONE) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    // The name refused by [refuseNewExercise] and why, while the form is open; checking blocks a second confirm meanwhile
    var refusedName by remember { mutableStateOf<Pair<String, String>?>(null) }
    var checkingNewName by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var exerciseMediaToView by remember { mutableStateOf<ExerciseEntity?>(null) }
    var detailExerciseId by rememberSaveable { mutableStateOf<Int?>(null) }

    val locked = lockedCategory != null
    val filteredExercises = remember(exercises, searchQuery, selectedCategory, showOnlyCustom, showOnlyFavorites, languageCode, traitFilter, lockedCategory) {
        ExercisePickerFilter.apply(
            exercises, searchQuery, selectedCategory, showOnlyCustom, showOnlyFavorites, languageCode, traits = traitFilter
        ).filter { lockedCategory == null || it.categoria.equals(lockedCategory, ignoreCase = true) }
    }
    val shownSuggestions = if (searchQuery.isBlank()) suggested else emptyList()
    val nowMillis = remember { Clock.System.now().toEpochMilliseconds() }

    val showThumbnails = rememberShowExerciseThumbnails(exercises)

    val allLabel = stringResource(Res.string.all_categories)
    val favoritesLabel = stringResource(Res.string.filter_favorites)
    val customLabel = stringResource(Res.string.filter_custom)
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
        if (!locked) categories.forEach { category ->
            add(
                ExerciseFilterOption(ExerciseTranslations.translateCategory(category, languageCode), selectedCategory == category) {
                    selectedCategory = category
                }
            )
        }
    }

    val imeVisible = isKeyboardVisible

    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { !listState.canScrollBackward } }
    // Favorites in a group of their own while nothing narrows the list (the filter puts them first anyway)
    val narrowed = searchQuery.isNotBlank() || selectedCategory != null || showOnlyCustom || showOnlyFavorites || traitFilter.isActive
    val favorites = if (narrowed || locked) emptyList() else filteredExercises.filter { it.isFavorite }
    val others = if (favorites.isEmpty()) filteredExercises else filteredExercises.filterNot { it.isFavorite }
    // Suggestions can arrive just after the sheet opens, above the rows already shown: back to the top to show them
    val hasSuggestions = shownSuggestions.isNotEmpty()
    LaunchedEffect(hasSuggestions) { if (hasSuggestions) listState.scrollToItem(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        contentWindowInsets = PickerSheetInsets,
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
        val bottomInset = pickerBottomInset()
        Box {
        ExercisePickerSheetLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ResponsiveSize.cardPadding),
            header = {
                Column {
                    // The title makes way for the list while the keyboard is up
                    AnimatedVisibility(visible = !imeVisible) {
                        Column {
                            ExercisePickerHeader(
                                eyebrow = stringResource(if (locked) Res.string.select_activity else Res.string.select_exercise),
                                title = stringResource(if (locked) Res.string.which_cardio else Res.string.choose_from_library),
                                hint = hint
                            )
                            Spacer(modifier = Modifier.height(Spacing.medium))
                        }
                    }

                    ExerciseSearchWithTraitFilter(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        traitFilter = traitFilter,
                        onTraitFilterChange = { traitFilter = it },
                        resultCount = filteredExercises.size,
                        languageCode = languageCode
                    )

                    AnimatedVisibility(visible = !imeVisible) {
                        Column {
                            Spacer(modifier = Modifier.height(Spacing.small))
                            // Locked to one category there is nothing to filter by category: only favorites remain
                            if (!locked || filterOptions.size > 1) ExerciseFilterBar(options = filterOptions)
                        }
                    }
                }
            },
            list = {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(PickerRowGap),
                    contentPadding = PaddingValues(top = PickerListTop, bottom = bottomInset + if (pickOnly) ResponsiveSize.cardPadding else PickerFabClearance)
                ) {
                    @Composable
                    fun PickRow(exercise: ExerciseEntity, shape: Shape, note: String? = null) {
                        ExercisePickRow(
                            exercise = exercise,
                            languageCode = languageCode,
                            onClick = {
                                onExerciseSelected(exercise)
                                onDismiss()
                            },
                            showThumbnail = showThumbnails,
                            onThumbnailClick = { exerciseMediaToView = exercise },
                            onInfoClick = if (pickOnly || !platformUi.hasExerciseDetail) null else { { detailExerciseId = exercise.id } },
                            note = note,
                            shape = shape
                        )
                    }
                    if (shownSuggestions.isNotEmpty()) {
                        item(key = "suggested-header") { ExercisePickerSectionLabel(stringResource(Res.string.suggested), first = true) }
                        pickerGroup(shownSuggestions, "suggested", { it.exercise.id }) { s, shape ->
                            val days = s.lastUsed?.let { daysBetween(it, nowMillis) }
                            PickRow(
                                s.exercise,
                                shape,
                                note = days?.let { if (it <= 0) stringResource(Res.string.today) else pluralStringResource(Res.plurals.days_ago, it, it) }
                            )
                        }
                        if (locked) item(key = "all-header") { ExercisePickerSectionLabel(stringResource(Res.string.all_cardio)) }
                    }
                    if (favorites.isNotEmpty()) {
                        item(key = "favorites-header") {
                            ExercisePickerSectionLabel(stringResource(Res.string.filter_favorites), first = shownSuggestions.isEmpty())
                        }
                        pickerGroup(favorites, "favorite", { it.id }) { exercise, shape -> PickRow(exercise, shape) }
                        if (others.isNotEmpty()) {
                            item(key = "others-header") { ExercisePickerSectionLabel(stringResource(Res.string.all_exercises_header)) }
                        }
                    }
                    if (filteredExercises.isEmpty()) {
                        item(key = "empty") { ExerciseEmptyState(onReset = if (traitFilter.isActive) ({ traitFilter = TraitFilter.NONE }) else null) }
                    }
                    pickerGroup(others, "all", { it.id }) { exercise, shape -> PickRow(exercise, shape) }
                }
            },
            footer = {}
        )
        PickerCreateFab(
            label = stringResource(if (locked) Res.string.create_cardio_exercise else Res.string.new_exercise),
            visible = !imeVisible && !pickOnly,
            expanded = fabExpanded,
            onClick = { showAddCustomDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = ResponsiveSize.cardPadding, bottom = ResponsiveSize.cardPadding + bottomInset)
        )
        }
    }

    if (showAddCustomDialog) {
        ExerciseFormDialog(
            title = stringResource(Res.string.new_exercise),
            categories = if (lockedCategory != null) listOf(lockedCategory) else categories,
            initial = if (lockedCategory != null) ExerciseForm(category = lockedCategory) else ExerciseForm(),
            existingExercises = platformUi.namesInUse(),
            languageCode = languageCode,
            showDescription = false,
            nameRefusal = { typed -> refusedName?.takeIf { it.first == typed }?.second },
            onConfirm = { form ->
                fun create() {
                    onAddCustomExercise(form.name, form.category) { createdExercise ->
                        onExerciseSelected(createdExercise)
                        onDismiss()
                    }
                    refusedName = null
                    showAddCustomDialog = false
                }
                val check = refuseNewExercise
                if (check == null) create()
                else if (!checkingNewName) {
                    checkingNewName = true
                    scope.launch {
                        val refusal = try { check(form.name) } finally { checkingNewName = false }
                        if (refusal != null) refusedName = form.name to refusal else create()
                    }
                }
            },
            onDismiss = {
                refusedName = null
                showAddCustomDialog = false
            }
        )
    }

    platformUi.MediaPeek(exerciseMediaToView, languageCode) { exerciseMediaToView = null }
    detailExerciseId?.let { platformUi.ExerciseDetail(exerciseId = it, onDismiss = { detailExerciseId = null }) }

}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

private fun daysBetween(from: Long, to: Long): Int = ((to - from) / DAY_MILLIS).toInt()
