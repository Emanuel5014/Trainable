package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.domain.csvimport.CsvColumn
import com.emanuel5014.trainable.domain.csvimport.DateOrder
import com.emanuel5014.trainable.domain.csvimport.ExerciseUse
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvColumns
import com.emanuel5014.trainable.domain.csvimport.HistoryImportPlanner
import com.emanuel5014.trainable.domain.csvimport.ImportIssue
import com.emanuel5014.trainable.domain.csvimport.IssueCode
import com.emanuel5014.trainable.domain.csvimport.IssueSeverity
import com.emanuel5014.trainable.domain.csvimport.PlannedEntry
import com.emanuel5014.trainable.domain.csvimport.PlannedRoutine
import com.emanuel5014.trainable.domain.csvimport.PlannedSet
import com.emanuel5014.trainable.domain.csvimport.PlannedWorkout
import com.emanuel5014.trainable.domain.csvimport.ResolvedExercise
import com.emanuel5014.trainable.domain.csvimport.RoutineStatus
import com.emanuel5014.trainable.domain.csvimport.RoutineTarget
import com.emanuel5014.trainable.domain.csvimport.RowKind
import com.emanuel5014.trainable.domain.csvimport.SystemPlanKind
import com.emanuel5014.trainable.ui.components.GlowProgressOverlay
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.delay
import com.emanuel5014.trainable.sharedui.resources.back
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.custom_workout
import com.emanuel5014.trainable.sharedui.resources.done
import com.emanuel5014.trainable.sharedui.resources.import_history_already_in_app
import com.emanuel5014.trainable.sharedui.resources.import_history_backup_tip
import com.emanuel5014.trainable.sharedui.resources.import_history_button
import com.emanuel5014.trainable.sharedui.resources.import_history_cardio_workouts
import com.emanuel5014.trainable.sharedui.resources.import_history_choose_another
import com.emanuel5014.trainable.sharedui.resources.import_history_choose_file
import com.emanuel5014.trainable.sharedui.resources.import_history_col_distance
import com.emanuel5014.trainable.sharedui.resources.import_history_col_duration
import com.emanuel5014.trainable.sharedui.resources.import_history_col_parts
import com.emanuel5014.trainable.sharedui.resources.import_history_col_reps
import com.emanuel5014.trainable.sharedui.resources.import_history_col_rpe
import com.emanuel5014.trainable.sharedui.resources.import_history_col_session_duration
import com.emanuel5014.trainable.sharedui.resources.import_history_col_set
import com.emanuel5014.trainable.sharedui.resources.import_history_col_superset
import com.emanuel5014.trainable.sharedui.resources.import_history_col_type
import com.emanuel5014.trainable.sharedui.resources.import_history_col_weight
import com.emanuel5014.trainable.sharedui.resources.import_history_column_date
import com.emanuel5014.trainable.sharedui.resources.import_history_column_exercise
import com.emanuel5014.trainable.sharedui.resources.import_history_column_measure
import com.emanuel5014.trainable.sharedui.resources.import_history_columns_optional
import com.emanuel5014.trainable.sharedui.resources.import_history_columns_required
import com.emanuel5014.trainable.sharedui.resources.import_history_columns_title
import com.emanuel5014.trainable.sharedui.resources.import_history_custom_workouts
import com.emanuel5014.trainable.sharedui.resources.import_history_dates_day_first
import com.emanuel5014.trainable.sharedui.resources.import_history_dates_guessed
import com.emanuel5014.trainable.sharedui.resources.import_history_dates_month_first
import com.emanuel5014.trainable.sharedui.resources.import_history_done_new_exercises
import com.emanuel5014.trainable.sharedui.resources.import_history_done_new_routines
import com.emanuel5014.trainable.sharedui.resources.import_history_done_skipped_duplicates
import com.emanuel5014.trainable.sharedui.resources.import_history_done_title
import com.emanuel5014.trainable.sharedui.resources.import_history_error_empty
import com.emanuel5014.trainable.sharedui.resources.import_history_error_import
import com.emanuel5014.trainable.sharedui.resources.import_history_error_no_rows
import com.emanuel5014.trainable.sharedui.resources.import_history_error_not_history
import com.emanuel5014.trainable.sharedui.resources.import_history_error_too_large
import com.emanuel5014.trainable.sharedui.resources.import_history_error_unreadable
import com.emanuel5014.trainable.sharedui.resources.import_history_exercise_matched
import com.emanuel5014.trainable.sharedui.resources.import_history_exercises_count
import com.emanuel5014.trainable.sharedui.resources.import_history_exercises_existing
import com.emanuel5014.trainable.sharedui.resources.import_history_exercises_new
import com.emanuel5014.trainable.sharedui.resources.import_history_fill_routines
import com.emanuel5014.trainable.sharedui.resources.import_history_fill_routines_desc
import com.emanuel5014.trainable.sharedui.resources.import_history_importing
import com.emanuel5014.trainable.sharedui.resources.import_history_intro
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_future
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_ignored
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_invalid_date
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_invalid_number
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_line
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_missing_cardio
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_missing_date
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_missing_duration
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_missing_exercise
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_missing_reps
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_no_values
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_not_cardio
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_too_large
import com.emanuel5014.trainable.sharedui.resources.import_history_issue_unknown_type
import com.emanuel5014.trainable.sharedui.resources.import_history_issues_note
import com.emanuel5014.trainable.sharedui.resources.import_history_new_badge
import com.emanuel5014.trainable.sharedui.resources.import_history_new_categories
import com.emanuel5014.trainable.sharedui.resources.import_history_not_imported_columns
import com.emanuel5014.trainable.sharedui.resources.import_history_nothing
import com.emanuel5014.trainable.sharedui.resources.import_history_quick_workouts
import com.emanuel5014.trainable.sharedui.resources.import_history_reading
import com.emanuel5014.trainable.sharedui.resources.import_history_routine_empty
import com.emanuel5014.trainable.sharedui.resources.import_history_routine_with_exercises
import com.emanuel5014.trainable.sharedui.resources.import_history_routines_count
import com.emanuel5014.trainable.sharedui.resources.import_history_routines_declined
import com.emanuel5014.trainable.sharedui.resources.import_history_routines_existing
import com.emanuel5014.trainable.sharedui.resources.import_history_routines_new
import com.emanuel5014.trainable.sharedui.resources.import_history_routines_none
import com.emanuel5014.trainable.sharedui.resources.import_history_rows_skipped
import com.emanuel5014.trainable.sharedui.resources.import_history_rule_duplicates
import com.emanuel5014.trainable.sharedui.resources.import_history_rule_exercises
import com.emanuel5014.trainable.sharedui.resources.import_history_rule_routines
import com.emanuel5014.trainable.sharedui.resources.import_history_rules_title
import com.emanuel5014.trainable.sharedui.resources.import_history_section_exercises
import com.emanuel5014.trainable.sharedui.resources.import_history_section_issues
import com.emanuel5014.trainable.sharedui.resources.import_history_section_options
import com.emanuel5014.trainable.sharedui.resources.import_history_section_routines
import com.emanuel5014.trainable.sharedui.resources.import_history_section_workouts
import com.emanuel5014.trainable.sharedui.resources.import_history_sets_count
import com.emanuel5014.trainable.sharedui.resources.import_history_show_more
import com.emanuel5014.trainable.sharedui.resources.import_history_skip_duplicates
import com.emanuel5014.trainable.sharedui.resources.import_history_skip_duplicates_found
import com.emanuel5014.trainable.sharedui.resources.import_history_stat_exercises
import com.emanuel5014.trainable.sharedui.resources.import_history_stat_sets
import com.emanuel5014.trainable.sharedui.resources.import_history_stat_workouts
import com.emanuel5014.trainable.sharedui.resources.import_history_supersets_count
import com.emanuel5014.trainable.sharedui.resources.import_history_template_button
import com.emanuel5014.trainable.sharedui.resources.import_history_template_hint
import com.emanuel5014.trainable.sharedui.resources.import_history_template_title
import com.emanuel5014.trainable.sharedui.resources.import_history_title
import com.emanuel5014.trainable.sharedui.resources.import_history_undo
import com.emanuel5014.trainable.sharedui.resources.import_history_undo_confirm
import com.emanuel5014.trainable.sharedui.resources.import_history_undo_failed
import com.emanuel5014.trainable.sharedui.resources.import_history_undo_message
import com.emanuel5014.trainable.sharedui.resources.import_history_undo_title
import com.emanuel5014.trainable.sharedui.resources.import_history_undone
import com.emanuel5014.trainable.sharedui.resources.import_history_unknown_columns
import com.emanuel5014.trainable.sharedui.resources.import_history_volume
import com.emanuel5014.trainable.sharedui.resources.import_history_warnings
import com.emanuel5014.trainable.sharedui.resources.import_history_weights_assumed
import com.emanuel5014.trainable.sharedui.resources.import_history_weights_from_file
import com.emanuel5014.trainable.sharedui.resources.import_history_weights_in
import com.emanuel5014.trainable.sharedui.resources.import_history_workouts_count
import com.emanuel5014.trainable.sharedui.resources.quick_workout
import com.emanuel5014.trainable.sharedui.resources.Res
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.ui.util.PlatformBackHandler

private const val PAGE = 30

/**
 * Settings -> Import CSV: reads a workout history in the shape the app exports, shows everything the import would do
 * (routines, exercises, workouts and their sets, what is skipped and why) and only then writes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryImportContent(
    model: HistoryImportModel,
    language: String,
    onChoose: () -> Unit,
    onDownloadExample: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by model.state.collectAsState()
    val appUnit by model.appWeightUnit.collectAsState()
    val choose = onChoose

    // Back from the preview (or an error) returns to the start instead of leaving; nothing can be left while it writes
    PlatformBackHandler(enabled = state is HistoryImportState.Preview || state is HistoryImportState.Failed) { model.reset() }
    PlatformBackHandler(enabled = state is HistoryImportState.Importing) { }

    // The same waiting screen as the AI scan; it also covers the top bar while it writes
    val themeMode by model.themeMode.collectAsState()
    val isDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Surface,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(Res.string.import_history_title),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (state is HistoryImportState.Preview || state is HistoryImportState.Failed) model.reset() else onNavigateBack()
                            },
                            enabled = state !is HistoryImportState.Importing
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back), tint = OnSurface)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Surface,
                        titleContentColor = OnSurface,
                        navigationIconContentColor = OnSurface
                    )
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (val current = state) {
                    HistoryImportState.Idle -> IntroContent(onChoose = choose, onDownloadExample = onDownloadExample)
                    // Waiting is shown by the overlay below
                    HistoryImportState.Reading, is HistoryImportState.Importing -> Unit
                    is HistoryImportState.Failed -> FailureContent(current.failure, onChoose = choose)
                    is HistoryImportState.Preview -> PreviewContent(
                        preview = current,
                        appUnit = appUnit,
                        language = language,
                        model = model
                    )
                    is HistoryImportState.Done -> DoneContent(
                        done = current,
                        onUndo = model::undo,
                        onFinish = onNavigateBack
                    )
                }
            }
        }

        when (val current = state) {
            HistoryImportState.Reading -> ImportProgressOverlay(
                title = stringResource(Res.string.import_history_reading),
                detail = null,
                isDark = isDark
            )
            is HistoryImportState.Importing -> ImportProgressOverlay(
                title = stringResource(Res.string.import_history_importing),
                detail = pluralStringResource(Res.plurals.import_history_workouts_count, current.workouts, current.workouts) +
                    " · " + pluralStringResource(Res.plurals.import_history_sets_count, current.sets, current.sets),
                isDark = isDark
            )
            else -> Unit
        }
    }
}

// --- Start, progress, errors ---

@Composable
private fun IntroContent(onChoose: () -> Unit, onDownloadExample: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(Res.string.import_history_intro), style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)

        GymCard(containerColor = SurfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Label(stringResource(Res.string.import_history_columns_title))
                Text(stringResource(Res.string.import_history_columns_required), style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                Text(stringResource(Res.string.import_history_columns_optional), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
        }

        GymCard(containerColor = SurfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Label(stringResource(Res.string.import_history_template_title))
                Text(stringResource(Res.string.import_history_template_hint), style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                GymButton(
                    onClick = onDownloadExample,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Primary.copy(alpha = 0.15f),
                    contentColor = Primary
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(Res.string.import_history_template_button), fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        GymCard(containerColor = SurfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Label(stringResource(Res.string.import_history_rules_title))
                Bullet(stringResource(Res.string.import_history_rule_exercises))
                Bullet(stringResource(Res.string.import_history_rule_routines))
                Bullet(stringResource(Res.string.import_history_rule_duplicates))
            }
        }

        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Text(stringResource(Res.string.import_history_backup_tip), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }

        GymButton(onClick = onChoose, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.FileOpen, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(Res.string.import_history_choose_file), fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * The waiting screen of the AI scan (glowing background, loading indicator, title, seconds elapsed, progress bar) so
 * reading and writing a history waits the same way.
 */
@Composable
private fun ImportProgressOverlay(title: String, detail: String?, isDark: Boolean) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }
    GlowProgressOverlay(isDark = isDark, hazeState = null) {
        GymLoadingIndicator()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.titleMedium.fontSize)
                ),
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = listOfNotNull(detail, "${elapsedSeconds}s").joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = Primary,
            trackColor = SurfaceContainerHighest
        )
    }
}

@Composable
private fun FailureContent(failure: HistoryImportFailure, onChoose: () -> Unit) {
    val message = when (failure) {
        HistoryImportFailure.Empty -> stringResource(Res.string.import_history_error_empty)
        HistoryImportFailure.NoRows -> stringResource(Res.string.import_history_error_no_rows)
        HistoryImportFailure.Unreadable -> stringResource(Res.string.import_history_error_unreadable)
        is HistoryImportFailure.TooLarge -> stringResource(Res.string.import_history_error_too_large, failure.limitMb)
        is HistoryImportFailure.NotAHistory -> stringResource(
            Res.string.import_history_error_not_history,
            failure.missing.map { missingColumnName(it) }.distinct().joinToString(", ")
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Error, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = OnSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        GymButton(onClick = onChoose) {
            Text(stringResource(Res.string.import_history_choose_another), fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun missingColumnName(column: CsvColumn): String = when (column) {
    CsvColumn.DATE -> stringResource(Res.string.import_history_column_date)
    CsvColumn.EXERCISE -> stringResource(Res.string.import_history_column_exercise)
    else -> stringResource(Res.string.import_history_column_measure)
}

// --- Preview ---

@Composable
private fun PreviewContent(
    preview: HistoryImportState.Preview,
    appUnit: String,
    language: String,
    model: HistoryImportModel
) {
    val plan = preview.plan
    val options = preview.options
    var issuesOpen by rememberSaveable { mutableStateOf(plan.errorCount > 0) }
    var routinesOpen by rememberSaveable { mutableStateOf(true) }
    var exercisesOpen by rememberSaveable { mutableStateOf(false) }
    var workoutsOpen by rememberSaveable { mutableStateOf(true) }
    var visibleIssues by rememberSaveable { mutableStateOf(PAGE) }
    var visibleExercises by rememberSaveable { mutableStateOf(PAGE) }
    var visibleWorkouts by rememberSaveable { mutableStateOf(PAGE) }
    val openWorkouts = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "summary") { SummaryCard(preview, appUnit) }

            preview.importError?.let { error ->
                item(key = "import_error") {
                    GymCard(containerColor = Error.copy(alpha = 0.15f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Error)
                            Text(stringResource(Res.string.import_history_error_import, error), color = Error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item(key = "options") { OptionsCard(preview, appUnit, model) }

            if (plan.issues.isNotEmpty()) {
                item(key = "issues_header") {
                    SectionHeader(
                        title = stringResource(Res.string.import_history_section_issues),
                        summary = issuesSummary(plan.skippedRows, plan.warningCount),
                        expanded = issuesOpen,
                        onToggle = { issuesOpen = !issuesOpen },
                        warn = plan.errorCount > 0
                    )
                }
                if (issuesOpen) {
                    item(key = "issues_note") {
                        Text(stringResource(Res.string.import_history_issues_note), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                    items(plan.issues.take(visibleIssues), key = { "issue_${it.line}_${it.code}_${it.column}_${it.value}" }) { issue ->
                        IssueRow(issue)
                    }
                    if (plan.issues.size > visibleIssues) {
                        item(key = "issues_more") { ShowMore(plan.issues.size - visibleIssues) { visibleIssues += PAGE } }
                    }
                }
            }

            routinesSection(preview, language, routinesOpen, { routinesOpen = !routinesOpen }, model)

            if (plan.exercises.isNotEmpty()) {
                item(key = "exercises_header") {
                    SectionHeader(
                        title = stringResource(Res.string.import_history_section_exercises),
                        summary = pluralStringResource(Res.plurals.import_history_exercises_count, plan.exercises.size, plan.exercises.size),
                        expanded = exercisesOpen,
                        onToggle = { exercisesOpen = !exercisesOpen }
                    )
                }
                if (exercisesOpen) {
                    if (plan.newCategories.isNotEmpty()) {
                        item(key = "new_categories") {
                            Text(
                                stringResource(Res.string.import_history_new_categories, plan.newCategories.joinToString(", ")),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    val shown = plan.exercises.take(visibleExercises)
                    var lastNew: Boolean? = null
                    shown.forEach { use ->
                        val isNew = use.exercise is ResolvedExercise.New
                        if (isNew != lastNew) {
                            lastNew = isNew
                            item(key = "exercises_group_$isNew") {
                                GroupLabel(
                                    if (isNew) stringResource(Res.string.import_history_exercises_new)
                                    else stringResource(Res.string.import_history_exercises_existing),
                                    count = if (isNew) plan.newExercises.size else plan.existingExercises.size
                                )
                            }
                        }
                        item(key = "exercise_${use.exercise.key}") { ExerciseRow(use, language) }
                    }
                    if (plan.exercises.size > visibleExercises) {
                        item(key = "exercises_more") { ShowMore(plan.exercises.size - visibleExercises) { visibleExercises += PAGE } }
                    }
                }
            }

            if (plan.workouts.isNotEmpty()) {
                item(key = "workouts_header") {
                    SectionHeader(
                        title = stringResource(Res.string.import_history_section_workouts),
                        summary = pluralStringResource(Res.plurals.import_history_workouts_count, plan.workouts.size, plan.workouts.size),
                        expanded = workoutsOpen,
                        onToggle = { workoutsOpen = !workoutsOpen }
                    )
                }
                if (workoutsOpen) {
                    val ordered = plan.workouts.asReversed()
                    items(ordered.take(visibleWorkouts), key = { "workout_${it.key}" }) { workout ->
                        WorkoutRow(
                            workout = workout,
                            skipped = workout.isDuplicate && options.skipDuplicates,
                            expanded = workout.key in openWorkouts,
                            onToggle = { if (workout.key in openWorkouts) openWorkouts.remove(workout.key) else openWorkouts.add(workout.key) },
                            unit = appUnit,
                            language = language
                        )
                    }
                    if (ordered.size > visibleWorkouts) {
                        item(key = "workouts_more") { ShowMore(ordered.size - visibleWorkouts) { visibleWorkouts += PAGE } }
                    }
                }
            }
        }

        BottomBar(
            count = plan.toImport.size,
            onImport = model::startImport
        )
    }
}

@Composable
private fun SummaryCard(preview: HistoryImportState.Preview, appUnit: String) {
    val plan = preview.plan
    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                preview.fileName,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(plan.toImport.size.toString(), stringResource(Res.string.import_history_stat_workouts), Modifier.weight(1f))
                Stat(plan.setCount.toString(), stringResource(Res.string.import_history_stat_sets), Modifier.weight(1f))
                Stat(plan.exercises.size.toString(), stringResource(Res.string.import_history_stat_exercises), Modifier.weight(1f))
            }
            val first = plan.firstDate
            val last = plan.lastDate
            if (first != null && last != null) {
                Text(
                    "${formatMediumDate(first)} – ${formatMediumDate(last)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
            if (plan.volumeKg > 0f) {
                val volume = WeightUnitConverter.convertDisplay(plan.volumeKg, appUnit)
                Text(
                    stringResource(Res.string.import_history_volume, "${formatGroupedWhole(volume)} $appUnit"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
            val supersets = plan.toImport.sumOf { it.supersetCount }
            if (supersets > 0) {
                Text(
                    pluralStringResource(Res.plurals.import_history_supersets_count, supersets, supersets),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OptionsCard(preview: HistoryImportState.Preview, appUnit: String, model: HistoryImportModel) {
    val plan = preview.plan
    val options = preview.options
    val history = preview.history
    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Label(stringResource(Res.string.import_history_section_options))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(Res.string.import_history_weights_in),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    listOf("kg", "lb").forEach { unit ->
                        FilterChip(
                            selected = options.weightUnit == unit,
                            onClick = { model.setWeightUnit(unit) },
                            label = { Text(unit, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                Text(
                    if (preview.unitFromFile) stringResource(Res.string.import_history_weights_from_file)
                    else stringResource(Res.string.import_history_weights_assumed),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }

            HorizontalDivider(color = Surface.copy(alpha = 0.5f))
            OptionSwitch(
                title = stringResource(Res.string.import_history_skip_duplicates),
                description = stringResource(Res.string.import_history_skip_duplicates_found, plan.duplicates.size),
                checked = options.skipDuplicates,
                onChange = model::setSkipDuplicates
            )
            HorizontalDivider(color = Surface.copy(alpha = 0.5f))
            OptionSwitch(
                title = stringResource(Res.string.import_history_fill_routines),
                description = stringResource(Res.string.import_history_fill_routines_desc),
                checked = options.fillNewRoutines,
                onChange = model::setFillNewRoutines
            )

            if (history.dateOrderApplied) {
                HorizontalDivider(color = Surface.copy(alpha = 0.5f))
                val note = if (history.dateOrder == DateOrder.DAY_FIRST) Res.string.import_history_dates_day_first else Res.string.import_history_dates_month_first
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(note), style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                    if (history.dateOrderGuessed) {
                        Text(
                            stringResource(Res.string.import_history_dates_guessed),
                            style = MaterialTheme.typography.bodySmall,
                            color = Tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (history.ignoredColumns.isNotEmpty()) {
                Text(
                    stringResource(Res.string.import_history_not_imported_columns, history.ignoredColumns.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
            if (history.unknownColumns.isNotEmpty()) {
                Text(
                    stringResource(Res.string.import_history_unknown_columns, history.unknownColumns.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OptionSwitch(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = OnSurface, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        SettingsSwitch(checked = checked, onCheckedChange = onChange)
    }
}

private fun LazyListScope.routinesSection(
    preview: HistoryImportState.Preview,
    language: String,
    open: Boolean,
    onToggle: () -> Unit,
    model: HistoryImportModel
) {
    val plan = preview.plan
    val groupCount = plan.routines.size + plan.systemWorkouts.size
    if (groupCount == 0) return

    item(key = "routines_header") {
        SectionHeader(
            title = stringResource(Res.string.import_history_section_routines),
            summary = pluralStringResource(Res.plurals.import_history_routines_count, plan.routines.size, plan.routines.size),
            expanded = open,
            onToggle = onToggle
        )
    }
    if (!open) return

    listOf(
        RoutineStatus.EXISTING to Res.string.import_history_routines_existing,
        RoutineStatus.NEW to Res.string.import_history_routines_new,
        RoutineStatus.DECLINED to Res.string.import_history_routines_declined
    ).forEach { (status, label) ->
        val routines = plan.routines.filter { it.status == status }
        if (routines.isEmpty()) return@forEach
        item(key = "routines_group_$status") { GroupLabelRes(label, routines.size) }
        items(routines, key = { "routine_${it.status}_${it.key}" }) { routine ->
            RoutineRow(routine, onCreateChange = { create -> model.setRoutineCreated(routine.key, create) })
        }
    }

    if (plan.systemWorkouts.isNotEmpty()) {
        item(key = "routines_group_system") { GroupLabelRes(Res.string.import_history_routines_none, null) }
        items(plan.systemWorkouts.entries.toList(), key = { "system_${it.key}" }) { (kind, count) ->
            val name = when (kind) {
                SystemPlanKind.QUICK -> stringResource(Res.string.import_history_quick_workouts)
                SystemPlanKind.CUSTOM -> stringResource(Res.string.import_history_custom_workouts)
                SystemPlanKind.CARDIO -> stringResource(Res.string.import_history_cardio_workouts)
            }
            SimpleRow(title = name, subtitle = pluralStringResource(Res.plurals.import_history_workouts_count, count, count))
        }
    }
}

@Composable
private fun RoutineRow(routine: PlannedRoutine, onCreateChange: (Boolean) -> Unit) {
    val range = if (routine.firstDate == routine.lastDate) formatMediumDate(routine.firstDate)
    else "${formatMediumDate(routine.firstDate)} – ${formatMediumDate(routine.lastDate)}"
    val workouts = pluralStringResource(Res.plurals.import_history_workouts_count, routine.workouts, routine.workouts)
    val detail = when (routine.status) {
        RoutineStatus.NEW ->
            if (routine.template.isEmpty()) stringResource(Res.string.import_history_routine_empty)
            else pluralStringResource(Res.plurals.import_history_routine_with_exercises, routine.template.size, routine.template.size)
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(routine.name, style = MaterialTheme.typography.bodyLarge, color = OnSurface, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("$workouts · $range", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = Primary, fontWeight = FontWeight.Bold)
        }
        if (routine.status != RoutineStatus.EXISTING) {
            SettingsSwitch(checked = routine.status == RoutineStatus.NEW, onCheckedChange = onCreateChange)
        }
    }
}

@Composable
private fun ExerciseRow(use: ExerciseUse, language: String) {
    val exercise = use.exercise
    val name = when (exercise) {
        is ResolvedExercise.Existing -> ExerciseTranslations.translate(exercise.exercise.nome, language)
        is ResolvedExercise.New -> exercise.name
    }
    val category = when (exercise) {
        is ResolvedExercise.Existing -> ExerciseTranslations.translateCategory(exercise.exercise.categoria, language)
        is ResolvedExercise.New -> ExerciseTranslations.translateCategory(exercise.category, language)
    }
    val sets = pluralStringResource(Res.plurals.import_history_sets_count, use.sets, use.sets)
    // Show how the file wrote it when that is not what the exercise is called here
    val writtenAs = use.fileName.takeIf { exercise is ResolvedExercise.Existing && HistoryCsvColumns.fold(it) != HistoryCsvColumns.fold(name) }
    SimpleRow(
        title = name,
        subtitle = "$category · $sets",
        extra = writtenAs?.let { stringResource(Res.string.import_history_exercise_matched, it) }
    )
}

@Composable
private fun WorkoutRow(
    workout: PlannedWorkout,
    skipped: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    unit: String,
    language: String
) {
    val routineName = when (val target = workout.routine) {
        is RoutineTarget.Existing -> target.plan.nome
        is RoutineTarget.Create -> target.name
        is RoutineTarget.System -> workout.sessionNote ?: when (target.kind) {
            SystemPlanKind.QUICK -> stringResource(Res.string.quick_workout).lowercase().replaceFirstChar { it.uppercase() }
            SystemPlanKind.CUSTOM -> stringResource(Res.string.custom_workout)
            SystemPlanKind.CARDIO -> stringResource(Res.string.import_history_cardio_workouts)
        }
    }
    val alpha = if (skipped) 0.55f else 1f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatMediumDate(workout.date),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurface.copy(alpha = alpha),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    routineName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary.copy(alpha = alpha),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${pluralStringResource(Res.plurals.import_history_exercises_count, workout.entries.size, workout.entries.size)} · " +
                        pluralStringResource(Res.plurals.import_history_sets_count, workout.setCount, workout.setCount) +
                        (workout.durationMs?.let { " · " + formatSeconds((it / 1000).toInt()) } ?: "") +
                        (workout.supersetCount.takeIf { it > 0 }?.let { " · " + pluralStringResource(Res.plurals.import_history_supersets_count, it, it) } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant.copy(alpha = alpha)
                )
            }
            if (workout.isDuplicate) Badge(stringResource(Res.string.import_history_already_in_app), if (skipped) OnSurfaceVariant else Tertiary)
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = OnSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = Surface.copy(alpha = 0.5f))
                workout.entries.forEach { entry -> EntryDetail(entry, unit, language) }
            }
        }
    }
}

@Composable
private fun EntryDetail(entry: PlannedEntry, unit: String, language: String) {
    val name = when (val exercise = entry.exercise) {
        is ResolvedExercise.Existing -> ExerciseTranslations.translate(exercise.exercise.nome, language)
        is ResolvedExercise.New -> exercise.name
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
            if (entry.exercise is ResolvedExercise.New) Badge(stringResource(Res.string.import_history_new_badge), Tertiary)
        }
        entry.sets.forEach { set ->
            Text(
                "${set.number}.  ${setText(entry.kind, set, unit)}" + (set.note?.let { "  ·  $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
    }
}

private fun setText(kind: RowKind, set: PlannedSet, unit: String): String {
    fun weight() = WeightUnitConverter.format(WeightUnitConverter.convertDisplay(set.weightKg, unit)) + " " + unit
    val warm = if (set.warmup) " (W)" else ""
    return when (kind) {
        RowKind.STRENGTH -> "${weight()} × ${set.reps}$warm" + if (set.parts.size > 1) "  [${set.parts.joinToString("+")}]" else ""
        RowKind.TIME_AND_WEIGHT -> (if (set.weightKg > 0f) "${weight()} · " else "") + formatSeconds(set.durationSeconds ?: 0) + warm
        RowKind.TIME_AND_REPS -> "${formatSeconds(set.durationSeconds ?: 0)} × ${set.reps}"
        RowKind.CARDIO -> formatSeconds(set.durationSeconds ?: 0) + if (set.distanceKm > 0f) " · ${WeightUnitConverter.format(set.distanceKm)} km" else ""
    }
}

private fun formatSeconds(seconds: Int): String = when {
    seconds >= 3600 -> "${seconds / 3600} h ${(seconds % 3600) / 60} min"
    seconds >= 60 -> if (seconds % 60 == 0) "${seconds / 60} min" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')} min"
    else -> "$seconds s"
}

@Composable
private fun IssueRow(issue: ImportIssue) {
    val isError = issue.severity == IssueSeverity.ERROR
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.Warning,
            contentDescription = null,
            tint = if (isError) Error else Tertiary,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(stringResource(Res.string.import_history_issue_line, issue.line), style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(issueText(issue), style = MaterialTheme.typography.bodySmall, color = OnSurface)
        }
    }
}

@Composable
private fun issueText(issue: ImportIssue): String = when (issue.code) {
    IssueCode.MISSING_DATE -> stringResource(Res.string.import_history_issue_missing_date)
    IssueCode.INVALID_DATE -> stringResource(Res.string.import_history_issue_invalid_date, issue.value)
    IssueCode.MISSING_EXERCISE -> stringResource(Res.string.import_history_issue_missing_exercise)
    IssueCode.INVALID_NUMBER -> stringResource(Res.string.import_history_issue_invalid_number, columnName(issue.column), issue.value)
    IssueCode.VALUE_TOO_LARGE -> stringResource(Res.string.import_history_issue_too_large, columnName(issue.column), issue.value)
    IssueCode.NO_VALUES -> stringResource(Res.string.import_history_issue_no_values)
    IssueCode.MISSING_REPS -> stringResource(Res.string.import_history_issue_missing_reps)
    IssueCode.MISSING_DURATION -> stringResource(Res.string.import_history_issue_missing_duration)
    IssueCode.MISSING_CARDIO_VALUES -> stringResource(Res.string.import_history_issue_missing_cardio)
    IssueCode.CARDIO_NOT_CARDIO_EXERCISE -> stringResource(Res.string.import_history_issue_not_cardio, issue.value)
    IssueCode.UNKNOWN_TYPE -> stringResource(Res.string.import_history_issue_unknown_type, issue.value)
    IssueCode.IGNORED_VALUE -> stringResource(Res.string.import_history_issue_ignored, columnName(issue.column), issue.value)
    IssueCode.FUTURE_DATE -> stringResource(Res.string.import_history_issue_future, issue.value)
}

@Composable
private fun columnName(column: CsvColumn?): String = when (column) {
    CsvColumn.WEIGHT -> stringResource(Res.string.import_history_col_weight)
    CsvColumn.REPS -> stringResource(Res.string.import_history_col_reps)
    CsvColumn.DURATION -> stringResource(Res.string.import_history_col_duration)
    CsvColumn.DISTANCE -> stringResource(Res.string.import_history_col_distance)
    CsvColumn.SET -> stringResource(Res.string.import_history_col_set)
    CsvColumn.RPE -> stringResource(Res.string.import_history_col_rpe)
    CsvColumn.PARTS -> stringResource(Res.string.import_history_col_parts)
    CsvColumn.TYPE -> stringResource(Res.string.import_history_col_type)
    CsvColumn.SESSION_DURATION -> stringResource(Res.string.import_history_col_session_duration)
    CsvColumn.SUPERSET -> stringResource(Res.string.import_history_col_superset)
    CsvColumn.DATE -> stringResource(Res.string.import_history_column_date)
    CsvColumn.EXERCISE -> stringResource(Res.string.import_history_column_exercise)
    else -> column?.name.orEmpty()
}

@Composable
private fun issuesSummary(skipped: Int, warnings: Int): String = listOfNotNull(
    pluralStringResource(Res.plurals.import_history_rows_skipped, skipped, skipped).takeIf { skipped > 0 },
    pluralStringResource(Res.plurals.import_history_warnings, warnings, warnings).takeIf { warnings > 0 }
).joinToString(" · ")

@Composable
private fun BottomBar(count: Int, onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        GymButton(onClick = onImport, enabled = count > 0, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (count > 0) pluralStringResource(Res.plurals.import_history_button, count, count) else stringResource(Res.string.import_history_nothing),
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// --- Done ---

@Composable
private fun DoneContent(done: HistoryImportState.Done, onUndo: () -> Unit, onFinish: () -> Unit) {
    var confirmUndo by remember { mutableStateOf(false) }
    val result = done.result

    if (confirmUndo) {
        AlertDialog(
            onDismissRequest = { confirmUndo = false },
            containerColor = SurfaceContainerHigh,
            title = { Text(stringResource(Res.string.import_history_undo_title), fontWeight = FontWeight.ExtraBold, color = OnSurface) },
            text = { Text(stringResource(Res.string.import_history_undo_message), color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { confirmUndo = false; onUndo() }) {
                    Text(stringResource(Res.string.import_history_undo_confirm), color = Error, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmUndo = false }) {
                    Text(stringResource(Res.string.cancel).uppercase(), color = Primary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (done.undone) Icons.Rounded.Undo else Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        if (done.undone) {
            Text(
                stringResource(Res.string.import_history_undone),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                stringResource(Res.string.import_history_done_title),
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${pluralStringResource(Res.plurals.import_history_workouts_count, result.workouts, result.workouts)} · " +
                        pluralStringResource(Res.plurals.import_history_sets_count, result.sets, result.sets),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold
                )
                if (result.newExercises > 0) {
                    Text(pluralStringResource(Res.plurals.import_history_done_new_exercises, result.newExercises, result.newExercises), color = OnSurfaceVariant)
                }
                if (result.newRoutines > 0) {
                    Text(pluralStringResource(Res.plurals.import_history_done_new_routines, result.newRoutines, result.newRoutines), color = OnSurfaceVariant)
                }
                if (result.skippedDuplicates > 0) {
                    Text(pluralStringResource(Res.plurals.import_history_done_skipped_duplicates, result.skippedDuplicates, result.skippedDuplicates), color = OnSurfaceVariant)
                }
                if (result.skippedRows > 0) {
                    Text(pluralStringResource(Res.plurals.import_history_rows_skipped, result.skippedRows, result.skippedRows), color = OnSurfaceVariant)
                }
            }
        }
        done.undoError?.let {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(Res.string.import_history_undo_failed, it), color = Error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(32.dp))
        GymButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.done).uppercase(), fontWeight = FontWeight.ExtraBold)
        }
        if (!done.undone) {
            Spacer(Modifier.height(8.dp))
            GymButton(
                onClick = { confirmUndo = true },
                modifier = Modifier.fillMaxWidth(),
                containerColor = Error.copy(alpha = 0.15f),
                contentColor = Error
            ) {
                Text(stringResource(Res.string.import_history_undo), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// --- Small pieces ---

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = Primary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("•", color = Primary, fontWeight = FontWeight.Black)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
    }
}

@Composable
private fun SectionHeader(title: String, summary: String, expanded: Boolean, onToggle: () -> Unit, warn: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
            color = if (warn) Error else Primary,
            fontWeight = FontWeight.Black
        )
        Text(
            summary,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = OnSurfaceVariant)
    }
}

@Composable
private fun GroupLabel(text: String, count: Int?) {
    Text(
        if (count != null) "$text ($count)" else text,
        style = MaterialTheme.typography.labelMedium,
        color = OnSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun GroupLabelRes(resource: StringResource, count: Int?) = GroupLabel(stringResource(resource), count)

@Composable
private fun SimpleRow(title: String, subtitle: String, extra: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = OnSurface, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        if (extra != null) Text(extra, style = MaterialTheme.typography.bodySmall, color = Primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerHighest)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Primary, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun ShowMore(remaining: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.import_history_show_more, minOf(remaining, PAGE)), color = Primary, fontWeight = FontWeight.ExtraBold)
    }
}
