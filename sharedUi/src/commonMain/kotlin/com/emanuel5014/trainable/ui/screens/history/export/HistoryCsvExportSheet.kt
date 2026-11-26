package com.emanuel5014.trainable.ui.screens.history.export

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.csvimport.CsvRowKinds
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.back
import com.emanuel5014.trainable.sharedui.resources.csv_export_add_exercise
import com.emanuel5014.trainable.sharedui.resources.csv_export_all
import com.emanuel5014.trainable.sharedui.resources.csv_export_all_exercises
import com.emanuel5014.trainable.sharedui.resources.csv_export_all_exercises_hint
import com.emanuel5014.trainable.sharedui.resources.csv_export_button
import com.emanuel5014.trainable.sharedui.resources.csv_export_deload
import com.emanuel5014.trainable.sharedui.resources.csv_export_deload_hint
import com.emanuel5014.trainable.sharedui.resources.csv_export_done_count
import com.emanuel5014.trainable.sharedui.resources.csv_export_empty
import com.emanuel5014.trainable.sharedui.resources.csv_export_exercise_workouts
import com.emanuel5014.trainable.sharedui.resources.csv_export_exercises
import com.emanuel5014.trainable.sharedui.resources.csv_export_include
import com.emanuel5014.trainable.sharedui.resources.csv_export_last_3_months
import com.emanuel5014.trainable.sharedui.resources.csv_export_last_year
import com.emanuel5014.trainable.sharedui.resources.csv_export_no_exercises
import com.emanuel5014.trainable.sharedui.resources.csv_export_pick_exercises
import com.emanuel5014.trainable.sharedui.resources.csv_export_pick_hint
import com.emanuel5014.trainable.sharedui.resources.csv_export_routines
import com.emanuel5014.trainable.sharedui.resources.csv_export_rows
import com.emanuel5014.trainable.sharedui.resources.csv_export_subtitle
import com.emanuel5014.trainable.sharedui.resources.csv_export_title
import com.emanuel5014.trainable.sharedui.resources.csv_export_type
import com.emanuel5014.trainable.sharedui.resources.csv_export_type_all
import com.emanuel5014.trainable.sharedui.resources.csv_export_type_cardio
import com.emanuel5014.trainable.sharedui.resources.csv_export_type_strength
import com.emanuel5014.trainable.sharedui.resources.csv_export_warmup
import com.emanuel5014.trainable.sharedui.resources.csv_export_warmup_hint
import com.emanuel5014.trainable.sharedui.resources.csv_export_workouts
import com.emanuel5014.trainable.sharedui.resources.done
import com.emanuel5014.trainable.sharedui.resources.history_filter_any_time
import com.emanuel5014.trainable.sharedui.resources.history_filter_last_30_days
import com.emanuel5014.trainable.sharedui.resources.history_filter_period
import com.emanuel5014.trainable.sharedui.resources.history_filter_reset
import com.emanuel5014.trainable.sharedui.resources.history_filter_this_year
import com.emanuel5014.trainable.sharedui.resources.no_routine
import com.emanuel5014.trainable.ui.components.ExerciseFilterBar
import com.emanuel5014.trainable.ui.components.ExerciseFilterOption
import com.emanuel5014.trainable.ui.components.ExerciseRow
import com.emanuel5014.trainable.ui.components.ExerciseSearchField
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.SheetFormFooter
import com.emanuel5014.trainable.ui.screens.history.ChipGroup
import com.emanuel5014.trainable.ui.screens.history.CustomRangeRow
import com.emanuel5014.trainable.ui.screens.history.FilterOptionChip
import com.emanuel5014.trainable.ui.screens.history.HistoryDateRangeDialog
import com.emanuel5014.trainable.ui.screens.history.RoutinePickerChoice
import com.emanuel5014.trainable.ui.screens.history.RoutinePickerSection
import com.emanuel5014.trainable.ui.screens.history.SectionLabel
import com.emanuel5014.trainable.ui.screens.history.joinedShape
import com.emanuel5014.trainable.ui.screens.settings.SettingsSwitch
import com.emanuel5014.trainable.ui.screens.settings.formatGroupedWhole
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.ui.util.PlatformBackHandler
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import com.emanuel5014.trainable.ui.util.uppercaseUi

/** Saves a CSV the user names the place of: Android's file picker, iOS's document picker. */
fun interface CsvFileSaver {
    /** Asks where to save [fileName], then writes what [csv] returns there; [onSaved] runs once it is written. */
    fun save(fileName: String, csv: suspend () -> String, onSaved: () -> Unit)
}

/** What the export reads the history from and writes it with (on iOS a repository that names the techniques). */
class HistoryCsvExportDeps(
    val repository: WorkoutRepository,
    val preferences: UserPreferencesRepository
)

/**
 * The CSV export sheet of Settings, starting from the whole history. The file has the format of the whole-history
 * export, so it can be imported back.
 */
@Composable
fun HistoryCsvExportHost(
    deps: HistoryCsvExportDeps,
    languageCode: String,
    saver: CsvFileSaver,
    onDismiss: () -> Unit,
    initial: HistoryCsvSelection = HistoryCsvSelection()
) {
    val scope = rememberCoroutineScope()
    val noRoutine = stringResource(Res.string.no_routine)
    val state = remember(deps, languageCode) {
        HistoryCsvExportState(deps.repository, deps.preferences, languageCode, initial, scope, noRoutine)
    }
    HistoryCsvExportSheet(state = state, saver = saver, onDismiss = onDismiss)
}

private enum class ExportPage { MAIN, EXERCISES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryCsvExportSheet(
    state: HistoryCsvExportState,
    saver: CsvFileSaver,
    onDismiss: () -> Unit
) {
    val ui by state.ui.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var page by rememberSaveable { mutableStateOf(ExportPage.MAIN) }
    var showDatePicker by remember { mutableStateOf(false) }
    // Kept here so coming back from the exercises finds the page where it was left
    val mainList = rememberLazyListState()
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }
    PlatformBackHandler(page == ExportPage.EXERCISES) { page = ExportPage.MAIN }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
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
        AnimatedContent(targetState = page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "csv_export_page") { shown ->
            when (shown) {
                ExportPage.MAIN -> MainPage(
                    ui = ui,
                    listState = mainList,
                    onChange = state::update,
                    onReset = state::reset,
                    onCustomRange = { showDatePicker = true },
                    onPickExercises = { page = ExportPage.EXERCISES },
                    onExport = { saver.save(ui.fileName, { state.csv() }, ::close) }
                )
                ExportPage.EXERCISES -> ExercisePage(
                    ui = ui,
                    onChange = state::update,
                    onBack = { page = ExportPage.MAIN }
                )
            }
        }
    }

    if (showDatePicker) {
        HistoryDateRangeDialog(
            onDismiss = { showDatePicker = false },
            onPicked = { from, to -> state.update { it.copy(startDate = from, endDate = to) } }
        )
    }
}

// --- The main page --------------------------------------------------------------------------------------------------

@Composable
private fun MainPage(
    ui: HistoryCsvExportUi,
    listState: LazyListState,
    onChange: ((HistoryCsvSelection) -> HistoryCsvSelection) -> Unit,
    onReset: () -> Unit,
    onCustomRange: () -> Unit,
    onPickExercises: () -> Unit,
    onExport: () -> Unit
) {
    val selection = ui.selection
    Column(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(horizontal = 20.dp)
        ) {
            item(key = "header") {
                Header(canReset = selection != HistoryCsvSelection(), onReset = onReset)
            }
            item(key = "routines") { RoutinesSection(ui, onChange) }
            item(key = "period") { PeriodSection(selection, onChange, onCustomRange) }
            item(key = "exercises") { ExercisesSection(ui, onChange, onPickExercises) }
            item(key = "type") { TypeSection(selection, onChange) }
            item(key = "include") { IncludeSection(ui, onChange) }
            item(key = "end") { Spacer(modifier = Modifier.height(16.dp)) }
        }
        SheetFormFooter(horizontalPadding = 20.dp) {
            ExportFooter(ui = ui, onExport = onExport)
        }
    }
}

@Composable
private fun Header(canReset: Boolean, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.csv_export_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = OnSurface
            )
            Text(
                text = stringResource(Res.string.csv_export_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
        }
        if (canReset) {
            TextButton(onClick = onReset) {
                Text(stringResource(Res.string.history_filter_reset), color = Error, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/** The export's choice of routines: any mix of routines, whole groups and "no routine"; none at all is every workout. */
private class MultiRoutineChoice(
    private val selection: HistoryCsvSelection,
    private val onChange: ((HistoryCsvSelection) -> HistoryCsvSelection) -> Unit
) : RoutinePickerChoice {
    override fun groupPicked(groupId: Int) = groupId in selection.groupIds
    override fun routinePicked(planId: Int) = planId in selection.planIds
    override val noRoutinePicked get() = selection.noRoutine
    override fun toggleGroup(groupId: Int) = onChange { it.copy(groupIds = it.groupIds.toggle(groupId)) }
    override fun toggleRoutine(planId: Int) = onChange { it.copy(planIds = it.planIds.toggle(planId)) }
    override fun toggleNoRoutine() = onChange { it.copy(noRoutine = !it.noRoutine) }
}

@Composable
private fun RoutinesSection(ui: HistoryCsvExportUi, onChange: ((HistoryCsvSelection) -> HistoryCsvSelection) -> Unit) {
    val selection = ui.selection
    RoutinePickerSection(
        title = stringResource(Res.string.csv_export_routines),
        blocks = ui.routineBlocks,
        noRoutineWorkouts = ui.noRoutineWorkouts,
        choice = MultiRoutineChoice(selection, onChange),
        lead = {
            // Nothing picked is every routine: this chip says so and takes the picks away
            ChipGroup(label = null) {
                FilterOptionChip(stringResource(Res.string.csv_export_all), ui.anyRoutineWorkouts, selection.anyRoutine) {
                    onChange { it.copy(planIds = emptySet(), groupIds = emptySet(), noRoutine = false) }
                }
            }
        }
    )
}

@Composable
private fun PeriodSection(
    selection: HistoryCsvSelection,
    onChange: ((HistoryCsvSelection) -> HistoryCsvSelection) -> Unit,
    onCustomRange: () -> Unit
) {
    val zone = remember { TimeZone.currentSystemDefault() }
    val today = remember { Clock.System.todayIn(zone) }
    val current = ExportPeriods.of(selection, today, zone)
    val shortcuts = listOf(
        ExportPeriod.ALL to Res.string.history_filter_any_time,
        ExportPeriod.LAST_30_DAYS to Res.string.history_filter_last_30_days,
        ExportPeriod.LAST_3_MONTHS to Res.string.csv_export_last_3_months,
        ExportPeriod.THIS_YEAR to Res.string.history_filter_this_year,
        ExportPeriod.LAST_YEAR to Res.string.csv_export_last_year
    )
    Column {
        SectionLabel(stringResource(Res.string.history_filter_period))
        ChipGroup(label = null) {
            shortcuts.forEach { (period, label) ->
                FilterOptionChip(stringResource(label), null, current == period) {
                    onChange { it.withPeriod(ExportPeriods.range(period, today, zone)) }
                }
            }
        }
        val start = selection.startDate
        val end = selection.endDate
        CustomRangeRow(
            range = if (current == ExportPeriod.CUSTOM && start != null && end != null) {
                "${DateFormatter.formatShort(start)} – ${DateFormatter.formatShort(end)}"
            } else null,
            onClick = onCustomRange,
            onClear = { onChange { it.withPeriod(null) } }
        )
    }
}

@Composable
private fun ExercisesSection(
    ui: HistoryCsvExportUi,
    onChange: ((HistoryCsvSelection) -> HistoryCsvSelection) -> Unit,
    onPickExercises: () -> Unit
) {
    Column {
        SectionLabel(stringResource(Res.string.csv_export_exercises))
        if (ui.pickedExercises.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerHigh)
                    .clickable(role = Role.Button, onClick = onPickExercises)
                    .heightIn(min = 64.dp)
                    .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(Res.string.csv_export_all_exercises),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = stringResource(Res.string.csv_export_all_exercises_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                }
            }
        } else {
            ChipGroup(label = null) {
                ui.pickedExercises.forEach { exercise ->
                    key("e${exercise.id}") {
                        FilterOptionChip(exercise.name, exercise.workouts, true) {
                            onChange { it.copy(rows = it.rows.copy(exerciseIds = it.rows.exerciseIds - exercise.id)) }
                        }
                    }
                }
                FilterOptionChip(stringResource(Res.string.csv_export_add_exercise), null, false, leadingIcon = Icons.Rounded.Add, onClick = onPickExercises)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSection(selection: HistoryCsvSelection, onChange: ((HistoryCsvSelection) -> HistoryCsvSelection) -> Unit) {
    val kinds = listOf(
        CsvRowKinds.ALL to Res.string.csv_export_type_all,
        CsvRowKinds.STRENGTH to Res.string.csv_export_type_strength,
        CsvRowKinds.CARDIO to Res.string.csv_export_type_cardio
    )
    Column {
        SectionLabel(stringResource(Res.string.csv_export_type))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            kinds.forEachIndexed { index, (kind, label) ->
                SegmentedButton(
                    selected = selection.rows.kinds == kind,
                    onClick = { onChange { it.copy(rows = it.rows.copy(kinds = kind)) } },
                    shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
                    icon = {},
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Primary.copy(alpha = 0.15f),
                        activeContentColor = Primary,
                        activeBorderColor = Primary.copy(alpha = 0.3f),
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = OnSurfaceVariant,
                        inactiveBorderColor = SurfaceContainerHighest
                    ),
                    label = { Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
    }
}

@Composable
private fun IncludeSection(ui: HistoryCsvExportUi, onChange: ((HistoryCsvSelection) -> HistoryCsvSelection) -> Unit) {
    val rows = ui.selection.rows
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        SectionLabel(stringResource(Res.string.csv_export_include))
        SwitchRow(
            title = stringResource(Res.string.csv_export_deload),
            hint = pluralStringResource(Res.plurals.csv_export_deload_hint, ui.deloadWorkouts, ui.deloadWorkouts),
            checked = rows.includeDeload,
            index = 0,
            onCheckedChange = { on -> onChange { it.copy(rows = it.rows.copy(includeDeload = on)) } }
        )
        SwitchRow(
            title = stringResource(Res.string.csv_export_warmup),
            hint = pluralStringResource(Res.plurals.csv_export_warmup_hint, ui.warmupSets, ui.warmupSets),
            checked = rows.includeWarmup,
            index = 1,
            onCheckedChange = { on -> onChange { it.copy(rows = it.rows.copy(includeWarmup = on)) } }
        )
    }
}

@Composable
private fun SwitchRow(title: String, hint: String, checked: Boolean, index: Int, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(joinedShape(index, 2))
            .background(SurfaceContainerHigh)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface)
            Text(text = hint, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        SettingsSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ExportFooter(ui: HistoryCsvExportUi, onExport: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralStringResource(Res.plurals.csv_export_workouts, ui.workouts, ui.workouts),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = OnSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = pluralStringResource(Res.plurals.csv_export_rows, ui.rows, formatGroupedWhole(ui.rows.toFloat())),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSurfaceVariant
            )
        }
        if (ui.workouts > 0) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Description, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(
                    text = ui.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis
                )
            }
        } else if (!ui.loading) {
            Text(
                text = stringResource(Res.string.csv_export_empty),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        GymButton(onClick = onExport, modifier = Modifier.fillMaxWidth(), enabled = ui.workouts > 0 && !ui.exporting && !ui.loading) {
            if (ui.exporting) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = OnPrimary)
            } else {
                Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(stringResource(Res.string.csv_export_button).uppercaseUi(), fontWeight = FontWeight.ExtraBold)
        }
    }
}

// --- Picking exercises ----------------------------------------------------------------------------------------------

@Composable
private fun ExercisePage(
    ui: HistoryCsvExportUi,
    onChange: ((HistoryCsvSelection) -> HistoryCsvSelection) -> Unit,
    onBack: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val picked = ui.selection.rows.exerciseIds
    // The categories of these exercises, the most used first
    val categories = remember(ui.exercises) {
        ui.exercises.groupingBy { it.category }.eachCount().entries.sortedByDescending { it.value }.map { it.key }.filter { it.isNotBlank() }
    }
    val shown = remember(ui.exercises, query, category) {
        val text = query.trim()
        ui.exercises.filter { (category == null || it.category == category) && (text.isEmpty() || it.name.contains(text, ignoreCase = true)) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back), tint = OnSurface)
                        }
                        Text(
                            text = stringResource(Res.string.csv_export_pick_exercises),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = OnSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (picked.isNotEmpty()) {
                            TextButton(onClick = { onChange { it.copy(rows = it.rows.copy(exerciseIds = emptySet())) } }) {
                                Text(stringResource(Res.string.history_filter_reset), color = Error, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    Text(
                        text = stringResource(Res.string.csv_export_pick_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    ExerciseSearchField(query = query, onQueryChange = { query = it })
                    if (categories.size > 1) {
                        val all = stringResource(Res.string.csv_export_all)
                        ExerciseFilterBar(
                            options = listOf(ExerciseFilterOption(all, category == null) { category = null }) +
                                categories.map { name -> ExerciseFilterOption(name, category == name) { category = if (category == name) null else name } }
                        )
                    }
                }
            }
            if (shown.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(Res.string.csv_export_no_exercises),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 24.dp)
                    )
                }
            }
            items(shown, key = { it.id }) { exercise ->
                val on = exercise.id in picked
                fun toggle() = onChange { it.copy(rows = it.rows.copy(exerciseIds = it.rows.exerciseIds.toggle(exercise.id))) }
                ExerciseRow(
                    name = exercise.name,
                    category = exercise.category + " · " + pluralStringResource(Res.plurals.csv_export_exercise_workouts, exercise.workouts, exercise.workouts),
                    onClick = ::toggle,
                    containerColor = if (on) Primary.copy(alpha = 0.14f) else SurfaceContainerHigh,
                    trailing = {
                        Checkbox(
                            checked = on,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = Primary, uncheckedColor = OnSurfaceVariant)
                        )
                    }
                )
            }
            item(key = "end") { Spacer(modifier = Modifier.height(16.dp)) }
        }
        SheetFormFooter(horizontalPadding = 20.dp) {
            GymButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (picked.isEmpty()) stringResource(Res.string.done).uppercaseUi()
                    else pluralStringResource(Res.plurals.csv_export_done_count, picked.size, picked.size).uppercaseUi(),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

private fun Set<Int>.toggle(id: Int): Set<Int> = if (id in this) this - id else this + id
