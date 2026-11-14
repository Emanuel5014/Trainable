package com.emanuel5014.trainable.ui.screens.settings

import com.emanuel5014.trainable.ui.util.bodyPadding
import com.emanuel5014.trainable.ui.util.scrollEndInset
import com.emanuel5014.trainable.ui.util.plusBottom
import com.emanuel5014.trainable.sharedui.resources.Res
import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.sharedui.resources.advanced_programming
import com.emanuel5014.trainable.sharedui.resources.advanced_programming_desc
import com.emanuel5014.trainable.sharedui.resources.auto_stop_cardio_at_target
import com.emanuel5014.trainable.sharedui.resources.auto_stop_cardio_at_target_desc
import com.emanuel5014.trainable.sharedui.resources.auto_advance_timed_sets
import com.emanuel5014.trainable.sharedui.resources.auto_advance_timed_sets_desc
import com.emanuel5014.trainable.sharedui.resources.auto_stop_time_weight_at_target
import com.emanuel5014.trainable.sharedui.resources.auto_stop_time_weight_at_target_desc
import com.emanuel5014.trainable.sharedui.resources.inline_exercise_modifications
import com.emanuel5014.trainable.sharedui.resources.inline_exercise_modifications_desc
import com.emanuel5014.trainable.sharedui.resources.journal_desc
import com.emanuel5014.trainable.sharedui.resources.journal_manage_tags
import com.emanuel5014.trainable.sharedui.resources.journal_manage_tags_desc
import com.emanuel5014.trainable.sharedui.resources.journal_prompt_finish
import com.emanuel5014.trainable.sharedui.resources.journal_prompt_finish_desc
import com.emanuel5014.trainable.sharedui.resources.journal_rating_setting
import com.emanuel5014.trainable.sharedui.resources.journal_rating_setting_desc
import com.emanuel5014.trainable.sharedui.resources.journal_title
import com.emanuel5014.trainable.sharedui.resources.keep_screen_on_cardio_timer
import com.emanuel5014.trainable.sharedui.resources.keep_screen_on_cardio_timer_desc
import com.emanuel5014.trainable.sharedui.resources.keep_screen_on_set_timer
import com.emanuel5014.trainable.sharedui.resources.keep_screen_on_set_timer_desc
import com.emanuel5014.trainable.sharedui.resources.load_rounding
import com.emanuel5014.trainable.sharedui.resources.load_rounding_desc
import com.emanuel5014.trainable.sharedui.resources.one_rep_maxes
import com.emanuel5014.trainable.sharedui.resources.one_rep_maxes_empty_desc
import com.emanuel5014.trainable.sharedui.resources.plate_calculator
import com.emanuel5014.trainable.sharedui.resources.plate_calculator_desc
import com.emanuel5014.trainable.sharedui.resources.plate_calculator_plates
import com.emanuel5014.trainable.sharedui.resources.plate_calculator_plates_desc
import com.emanuel5014.trainable.sharedui.resources.rest_timer_setting
import com.emanuel5014.trainable.sharedui.resources.rest_timer_setting_desc
import com.emanuel5014.trainable.sharedui.resources.rpe_setting
import com.emanuel5014.trainable.sharedui.resources.rpe_setting_desc
import com.emanuel5014.trainable.sharedui.resources.set_performance_setting
import com.emanuel5014.trainable.sharedui.resources.set_performance_setting_desc
import com.emanuel5014.trainable.sharedui.resources.starting_weight
import com.emanuel5014.trainable.sharedui.resources.starting_weight_desc
import com.emanuel5014.trainable.sharedui.resources.timer_add_time
import com.emanuel5014.trainable.sharedui.resources.timer_buttons
import com.emanuel5014.trainable.sharedui.resources.timer_buttons_desc
import com.emanuel5014.trainable.sharedui.resources.timer_subtract_time
import com.emanuel5014.trainable.sharedui.resources.timer_next_set_button
import com.emanuel5014.trainable.sharedui.resources.timer_next_set_button_desc
import com.emanuel5014.trainable.sharedui.resources.workout_section_after
import com.emanuel5014.trainable.sharedui.resources.workout_section_programming
import com.emanuel5014.trainable.sharedui.resources.workout_section_session
import com.emanuel5014.trainable.sharedui.resources.workout_section_timers
import com.emanuel5014.trainable.sharedui.resources.workout_settings
import com.emanuel5014.trainable.sharedui.resources.workout_timer
import com.emanuel5014.trainable.sharedui.resources.workout_timer_desc
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.ui.components.ConnectedToggleRow
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.util.PlateCalculator
import com.emanuel5014.trainable.util.TimerAdjustment
import com.emanuel5014.trainable.util.WeightUnitConverter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSettingsContent(
    onNavigateBack: () -> Unit,
    onNavigateToOneRepMaxes: () -> Unit = {},
    onNavigateToJournalTags: () -> Unit = {},
    state: SettingsState
) {
    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.workout_settings),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues.bodyPadding())
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .padding(bottom = paddingValues.scrollEndInset()),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            SettingsSection(title = stringResource(Res.string.workout_section_session)) {
                DuringWorkoutCard(state)
            }
            SettingsSection(title = stringResource(Res.string.workout_section_timers)) {
                TimersAndCardioCard(state)
            }
            SettingsSection(title = stringResource(Res.string.workout_section_programming)) {
                RoutinesAndExercisesCard(
                    state = state,
                    onNavigateToOneRepMaxes = onNavigateToOneRepMaxes
                )
            }
            SettingsSection(title = stringResource(Res.string.workout_section_after)) {
                AfterWorkoutCard(state, onNavigateToJournalTags)
            }
        }
    }
}

/** What the workout screen offers while you train: the clock, adding sets, set details, plates. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuringWorkoutCard(state: SettingsState) {
    val weightUnit by state.weightUnit.collectAsState()
    val workoutTimerEnabled by state.workoutTimerEnabled.collectAsState()
    val inlineExerciseModificationsEnabled by state.inlineExerciseModificationsEnabled.collectAsState()
    val setPerformanceEnabled by state.setPerformanceEnabled.collectAsState()
    val plateCalculatorEnabled by state.plateCalculatorEnabled.collectAsState()
    val plateCalculatorPlates by state.plateCalculatorPlates.collectAsState()
    val rpeEnabled by state.rpeEnabled.collectAsState()
    val rpeScale by state.rpeScale.collectAsState()

    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingSwitchRow(
                icon = Icons.Rounded.Timer,
                title = stringResource(Res.string.workout_timer),
                description = stringResource(Res.string.workout_timer_desc),
                checked = workoutTimerEnabled,
                onCheckedChange = { state.setWorkoutTimerEnabled(it) }
            )

            SettingDivider()

            SettingSwitchRow(
                icon = Icons.Rounded.AddCircleOutline,
                title = stringResource(Res.string.inline_exercise_modifications),
                description = stringResource(Res.string.inline_exercise_modifications_desc),
                checked = inlineExerciseModificationsEnabled,
                onCheckedChange = { state.setInlineExerciseModificationsEnabled(it) }
            )

            SettingDivider()

            // Mini-sets, assisted and partial reps, technique as done
            SettingSwitchRow(
                icon = Icons.Rounded.Tune,
                title = stringResource(Res.string.set_performance_setting),
                description = stringResource(Res.string.set_performance_setting_desc),
                checked = setPerformanceEnabled,
                onCheckedChange = { state.setSetPerformanceEnabled(it) }
            )

            SettingDivider()

            // How hard each set felt, on the scale the user picks
            SettingSwitchRow(
                icon = Icons.Rounded.Speed,
                title = stringResource(Res.string.rpe_setting),
                description = stringResource(Res.string.rpe_setting_desc),
                checked = rpeEnabled,
                onCheckedChange = { state.setRpeEnabled(it) }
            )

            AnimatedVisibility(visible = rpeEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingDivider()

                    RpeScaleOptions(selected = rpeScale, onSelect = { state.setRpeScale(it) })
                }
            }

            SettingDivider()

            SettingSwitchRow(
                icon = Icons.Rounded.FitnessCenter,
                title = stringResource(Res.string.plate_calculator),
                description = stringResource(Res.string.plate_calculator_desc),
                checked = plateCalculatorEnabled,
                onCheckedChange = { state.setPlateCalculatorEnabled(it) }
            )

            if (plateCalculatorEnabled) {
                SettingDivider()

                SettingChoice(
                    title = stringResource(Res.string.plate_calculator_plates),
                    description = stringResource(Res.string.plate_calculator_plates_desc)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlateCalculator.plateChoices(weightUnit).forEach { plate ->
                            val selected = plate in plateCalculatorPlates
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    // The calculator needs at least one plate to work with
                                    val updated = if (selected) plateCalculatorPlates - plate else plateCalculatorPlates + plate
                                    if (updated.isNotEmpty()) state.setPlateCalculatorPlates(updated)
                                },
                                label = { Text("${WeightUnitConverter.format(plate)} $weightUnit", maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary.copy(alpha = 0.15f),
                                    selectedLabelColor = Primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The rest timer switch, the rest and warmup timer buttons, then the cardio and timed-set switches, each type side by side. */
@Composable
private fun TimersAndCardioCard(state: SettingsState) {
    val restTimerEnabled by state.restTimerEnabled.collectAsState()
    val timerAddSeconds by state.timerAddSeconds.collectAsState()
    val timerSubtractSeconds by state.timerSubtractSeconds.collectAsState()
    val timerAddEnabled by state.timerAddEnabled.collectAsState()
    val timerSubtractEnabled by state.timerSubtractEnabled.collectAsState()
    val timerShowNextSetButton by state.timerShowNextSetButton.collectAsState()
    val autoStopCardioAtTarget by state.autoStopCardioAtTarget.collectAsState()
    val keepScreenOnCardioTimer by state.keepScreenOnCardioTimer.collectAsState()
    val autoStopTimeWeightAtTarget by state.autoStopTimeWeightAtTarget.collectAsState()
    val autoAdvanceTimedSets by state.autoAdvanceTimedSets.collectAsState()
    val keepScreenOnSetTimer by state.keepScreenOnSetTimer.collectAsState()

    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // The countdown after each set; the warmup timer is its own thing
            SettingSwitchRow(
                icon = Icons.Rounded.Timer,
                title = stringResource(Res.string.rest_timer_setting),
                description = stringResource(Res.string.rest_timer_setting_desc),
                checked = restTimerEnabled,
                onCheckedChange = { state.setRestTimerEnabled(it) }
            )

            SettingDivider()

            // How far the + and - buttons of the rest and warmup timers move the countdown
            SettingHeader(
                icon = Icons.Rounded.Timer,
                title = stringResource(Res.string.timer_buttons),
                description = stringResource(Res.string.timer_buttons_desc)
            )

            // Each button has its own switch: only -, only +, both or neither
            TimerAdjustRow(
                title = stringResource(Res.string.timer_add_time),
                enabled = timerAddEnabled,
                onEnabledChange = { state.setTimerAddEnabled(it) },
                options = TimerAdjustment.ADD_OPTIONS.map { "+${it}s" },
                selectedIndex = TimerAdjustment.ADD_OPTIONS.indexOf(timerAddSeconds),
                onSelect = { state.setTimerAddSeconds(TimerAdjustment.ADD_OPTIONS[it]) }
            )

            TimerAdjustRow(
                title = stringResource(Res.string.timer_subtract_time),
                enabled = timerSubtractEnabled,
                onEnabledChange = { state.setTimerSubtractEnabled(it) },
                options = TimerAdjustment.SUBTRACT_OPTIONS.map { "−${it}s" },
                selectedIndex = TimerAdjustment.SUBTRACT_OPTIONS.indexOf(timerSubtractSeconds),
                onSelect = { state.setTimerSubtractSeconds(TimerAdjustment.SUBTRACT_OPTIONS[it]) }
            )

            SettingDivider()

            // "Set done" on the finished-rest notification / Live Activity
            SettingSwitchRow(
                icon = Icons.Rounded.Timer,
                title = stringResource(Res.string.timer_next_set_button),
                description = stringResource(Res.string.timer_next_set_button_desc),
                checked = timerShowNextSetButton,
                onCheckedChange = { state.setTimerShowNextSetButton(it) }
            )

            SettingDivider()

            SettingSwitchRow(
                icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                title = stringResource(Res.string.auto_stop_cardio_at_target),
                description = stringResource(Res.string.auto_stop_cardio_at_target_desc),
                checked = autoStopCardioAtTarget,
                onCheckedChange = { state.setAutoStopCardioAtTarget(it) }
            )

            SettingDivider()

            SettingSwitchRow(
                icon = Icons.Rounded.PhoneAndroid,
                title = stringResource(Res.string.keep_screen_on_cardio_timer),
                description = stringResource(Res.string.keep_screen_on_cardio_timer_desc),
                checked = keepScreenOnCardioTimer,
                onCheckedChange = { state.setKeepScreenOnCardioTimer(it) }
            )

            SettingDivider()

            SettingSwitchRow(
                icon = Icons.Rounded.FitnessCenter,
                title = stringResource(Res.string.auto_stop_time_weight_at_target),
                description = stringResource(Res.string.auto_stop_time_weight_at_target_desc),
                checked = autoStopTimeWeightAtTarget,
                onCheckedChange = { state.setAutoStopTimeWeightAtTarget(it) }
            )

            // Builds on the auto-stop: the set stopped at its target is logged and the chain goes on (issue #65)
            AnimatedVisibility(visible = autoStopTimeWeightAtTarget) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingDivider()

                    SettingSwitchRow(
                        icon = Icons.Rounded.FastForward,
                        title = stringResource(Res.string.auto_advance_timed_sets),
                        description = stringResource(Res.string.auto_advance_timed_sets_desc),
                        checked = autoAdvanceTimedSets,
                        onCheckedChange = { state.setAutoAdvanceTimedSets(it) }
                    )
                }
            }

            SettingDivider()

            SettingSwitchRow(
                icon = Icons.Rounded.PhoneAndroid,
                title = stringResource(Res.string.keep_screen_on_set_timer),
                description = stringResource(Res.string.keep_screen_on_set_timer_desc),
                checked = keepScreenOnSetTimer,
                onCheckedChange = { state.setKeepScreenOnSetTimer(it) }
            )
        }
    }
}

/** How routines are written: a weight to start from and the %1RM programming tools. */
@Composable
private fun RoutinesAndExercisesCard(
    state: SettingsState,
    onNavigateToOneRepMaxes: () -> Unit
) {
    val weightUnit by state.weightUnit.collectAsState()
    val startingWeightEnabled by state.startingWeightEnabled.collectAsState()
    val advancedProgramming by state.advancedProgrammingEnabled.collectAsState()
    val loadRoundingIncrement by state.loadRoundingIncrement.collectAsState()

    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // A weight per exercise of a routine for its first workouts
            SettingSwitchRow(
                icon = Icons.Rounded.Scale,
                title = stringResource(Res.string.starting_weight),
                description = stringResource(Res.string.starting_weight_desc),
                checked = startingWeightEnabled,
                onCheckedChange = { state.setStartingWeightEnabled(it) }
            )

            SettingDivider()

            // Powerlifting / %1RM prescriptions
            SettingSwitchRow(
                icon = Icons.Rounded.FitnessCenter,
                title = stringResource(Res.string.advanced_programming),
                description = stringResource(Res.string.advanced_programming_desc),
                checked = advancedProgramming,
                onCheckedChange = { state.setAdvancedProgrammingEnabled(it) }
            )

            if (advancedProgramming) {
                SettingDivider()

                SettingNavRow(
                    icon = Icons.Rounded.EmojiEvents,
                    title = stringResource(Res.string.one_rep_maxes),
                    description = stringResource(Res.string.one_rep_maxes_empty_desc),
                    onClick = onNavigateToOneRepMaxes
                )

                SettingDivider()

                val increments = if (weightUnit == "lb") LoadCalculator.INCREMENTS_LB else LoadCalculator.INCREMENTS_KG
                SettingChoice(
                    title = stringResource(Res.string.load_rounding),
                    description = stringResource(Res.string.load_rounding_desc)
                ) {
                    ConnectedToggleRow(
                        options = increments.map { "${PrescriptionFormatter.number(it)} $weightUnit" },
                        selectedIndex = increments.indexOf(loadRoundingIncrement),
                        onSelect = { state.setLoadRoundingIncrement(increments[it]) }
                    )
                }

            }
        }
    }
}

/** Workout journal: a rating, tags and a note on each workout. */
@Composable
private fun AfterWorkoutCard(state: SettingsState, onNavigateToJournalTags: () -> Unit) {
    val journalEnabled by state.journalEnabled.collectAsState()
    val journalPromptAtFinish by state.journalPromptAtFinish.collectAsState()
    val journalRatingEnabled by state.journalRatingEnabled.collectAsState()

    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingSwitchRow(
                icon = Icons.Rounded.EditNote,
                title = stringResource(Res.string.journal_title),
                description = stringResource(Res.string.journal_desc),
                checked = journalEnabled,
                onCheckedChange = { state.setJournalEnabled(it) }
            )

            AnimatedVisibility(visible = journalEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingDivider()

                    NestedSwitchRow(
                        title = stringResource(Res.string.journal_prompt_finish),
                        description = stringResource(Res.string.journal_prompt_finish_desc),
                        checked = journalPromptAtFinish,
                        onCheckedChange = { state.setJournalPromptAtFinish(it) }
                    )
                    NestedSwitchRow(
                        title = stringResource(Res.string.journal_rating_setting),
                        description = stringResource(Res.string.journal_rating_setting_desc),
                        checked = journalRatingEnabled,
                        onCheckedChange = { state.setJournalRatingEnabled(it) }
                    )

                    SettingDivider()

                    NestedNavRow(
                        title = stringResource(Res.string.journal_manage_tags),
                        description = stringResource(Res.string.journal_manage_tags_desc),
                        onClick = onNavigateToJournalTags
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingDivider() {
    HorizontalDivider(color = Surface.copy(alpha = 0.5f))
}

/** Title and one-line explanation of a setting. */
@Composable
private fun SettingText(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}

/** The icon and text a setting starts with, without any control on the right. */
@Composable
private fun SettingHeader(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        SettingText(title, description)
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingHeader(icon, title, description, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        SettingsSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** A row that opens another screen. */
@Composable
private fun SettingNavRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingHeader(icon, title, description, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
    }
}

/** A switch row nested under a main switch: indented to line up with the text above it. */
@Composable
private fun NestedSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingText(title, description, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        SettingsSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** A row that opens another screen, nested under a main switch like [NestedSwitchRow]. */
@Composable
private fun NestedNavRow(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingText(title, description, modifier = Modifier.weight(1f).padding(start = 40.dp))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
    }
}

/** A setting whose control sits under its text instead of beside it: chips or a toggle row. */
@Composable
private fun SettingChoice(
    title: String,
    description: String,
    control: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
        control()
    }
}

/** A timer button: a switch to show it, and the amounts to pick from while it is on. */
@Composable
private fun TimerAdjustRow(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            SettingsSwitch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        if (enabled) {
            ConnectedToggleRow(
                options = options,
                selectedIndex = selectedIndex,
                onSelect = onSelect
            )
        }
    }
}
