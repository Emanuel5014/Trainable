package com.emanuel5014.trainable.ui.screens.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
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
fun WorkoutSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOneRepMaxes: () -> Unit = {},
    onNavigateToJournalTags: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.workout_settings),
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
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            SettingsSection(title = stringResource(R.string.workout_section_session)) {
                DuringWorkoutCard(viewModel)
            }
            SettingsSection(title = stringResource(R.string.workout_section_timers)) {
                TimersAndCardioCard(viewModel)
            }
            SettingsSection(title = stringResource(R.string.workout_section_programming)) {
                RoutinesAndExercisesCard(
                    viewModel = viewModel,
                    onNavigateToOneRepMaxes = onNavigateToOneRepMaxes
                )
            }
            SettingsSection(title = stringResource(R.string.workout_section_after)) {
                AfterWorkoutCard(viewModel, onNavigateToJournalTags)
            }
        }
    }
}

/** What the workout screen offers while you train: the clock, adding sets, set details, plates. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DuringWorkoutCard(viewModel: SettingsViewModel) {
    val weightUnit by viewModel.weightUnit.collectAsState()
    val workoutTimerEnabled by viewModel.workoutTimerEnabled.collectAsState()
    val inlineExerciseModificationsEnabled by viewModel.inlineExerciseModificationsEnabled.collectAsState()
    val setPerformanceEnabled by viewModel.setPerformanceEnabled.collectAsState()
    val plateCalculatorEnabled by viewModel.plateCalculatorEnabled.collectAsState()
    val plateCalculatorPlates by viewModel.plateCalculatorPlates.collectAsState()
    val rpeEnabled by viewModel.rpeEnabled.collectAsState()
    val rpeScale by viewModel.rpeScale.collectAsState()

    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingSwitchRow(
                icon = Icons.Rounded.Timer,
                title = stringResource(R.string.workout_timer),
                description = stringResource(R.string.workout_timer_desc),
                checked = workoutTimerEnabled,
                onCheckedChange = { viewModel.setWorkoutTimerEnabled(it) }
            )

            SettingDivider()

            SettingSwitchRow(
                icon = Icons.Rounded.AddCircleOutline,
                title = stringResource(R.string.inline_exercise_modifications),
                description = stringResource(R.string.inline_exercise_modifications_desc),
                checked = inlineExerciseModificationsEnabled,
                onCheckedChange = { viewModel.setInlineExerciseModificationsEnabled(it) }
            )

            SettingDivider()

            // Mini-sets, assisted and partial reps, technique as done
            SettingSwitchRow(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.set_performance_setting),
                description = stringResource(R.string.set_performance_setting_desc),
                checked = setPerformanceEnabled,
                onCheckedChange = { viewModel.setSetPerformanceEnabled(it) }
            )

            SettingDivider()

            // How hard each set felt, on the scale the user picks
            SettingSwitchRow(
                icon = Icons.Rounded.Speed,
                title = stringResource(R.string.rpe_setting),
                description = stringResource(R.string.rpe_setting_desc),
                checked = rpeEnabled,
                onCheckedChange = { viewModel.setRpeEnabled(it) }
            )

            AnimatedVisibility(visible = rpeEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingDivider()

                    RpeScaleOptions(selected = rpeScale, onSelect = { viewModel.setRpeScale(it) })
                }
            }

            SettingDivider()

            SettingSwitchRow(
                icon = Icons.Rounded.FitnessCenter,
                title = stringResource(R.string.plate_calculator),
                description = stringResource(R.string.plate_calculator_desc),
                checked = plateCalculatorEnabled,
                onCheckedChange = { viewModel.setPlateCalculatorEnabled(it) }
            )

            if (plateCalculatorEnabled) {
                SettingDivider()

                SettingChoice(
                    title = stringResource(R.string.plate_calculator_plates),
                    description = stringResource(R.string.plate_calculator_plates_desc)
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
                                    if (updated.isNotEmpty()) viewModel.setPlateCalculatorPlates(updated)
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

/** Rest and warmup timer buttons, then the cardio and timed-set switches, each type side by side. */
@Composable
private fun TimersAndCardioCard(viewModel: SettingsViewModel) {
    val timerAddSeconds by viewModel.timerAddSeconds.collectAsState()
    val timerSubtractSeconds by viewModel.timerSubtractSeconds.collectAsState()
    val timerAddEnabled by viewModel.timerAddEnabled.collectAsState()
    val timerSubtractEnabled by viewModel.timerSubtractEnabled.collectAsState()
    val autoStopCardioAtTarget by viewModel.autoStopCardioAtTarget.collectAsState()
    val keepScreenOnCardioTimer by viewModel.keepScreenOnCardioTimer.collectAsState()
    val autoStopTimeWeightAtTarget by viewModel.autoStopTimeWeightAtTarget.collectAsState()
    val keepScreenOnSetTimer by viewModel.keepScreenOnSetTimer.collectAsState()

    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // How far the + and - buttons of the rest and warmup timers move the countdown
            SettingHeader(
                icon = Icons.Rounded.Timer,
                title = stringResource(R.string.timer_buttons),
                description = stringResource(R.string.timer_buttons_desc)
            )

            // Each button has its own switch: only -, only +, both or neither
            TimerAdjustRow(
                title = stringResource(R.string.timer_add_time),
                enabled = timerAddEnabled,
                onEnabledChange = { viewModel.setTimerAddEnabled(it) },
                options = TimerAdjustment.ADD_OPTIONS.map { "+${it}s" },
                selectedIndex = TimerAdjustment.ADD_OPTIONS.indexOf(timerAddSeconds),
                onSelect = { viewModel.setTimerAddSeconds(TimerAdjustment.ADD_OPTIONS[it]) }
            )

            TimerAdjustRow(
                title = stringResource(R.string.timer_subtract_time),
                enabled = timerSubtractEnabled,
                onEnabledChange = { viewModel.setTimerSubtractEnabled(it) },
                options = TimerAdjustment.SUBTRACT_OPTIONS.map { "−${it}s" },
                selectedIndex = TimerAdjustment.SUBTRACT_OPTIONS.indexOf(timerSubtractSeconds),
                onSelect = { viewModel.setTimerSubtractSeconds(TimerAdjustment.SUBTRACT_OPTIONS[it]) }
            )

            SettingDivider()

            SettingSwitchRow(
                icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                title = stringResource(R.string.auto_stop_cardio_at_target),
                description = stringResource(R.string.auto_stop_cardio_at_target_desc),
                checked = autoStopCardioAtTarget,
                onCheckedChange = { viewModel.setAutoStopCardioAtTarget(it) }
            )

            SettingDivider()

            SettingSwitchRow(
                icon = Icons.Rounded.PhoneAndroid,
                title = stringResource(R.string.keep_screen_on_cardio_timer),
                description = stringResource(R.string.keep_screen_on_cardio_timer_desc),
                checked = keepScreenOnCardioTimer,
                onCheckedChange = { viewModel.setKeepScreenOnCardioTimer(it) }
            )

            SettingDivider()

            SettingSwitchRow(
                icon = Icons.Rounded.FitnessCenter,
                title = stringResource(R.string.auto_stop_time_weight_at_target),
                description = stringResource(R.string.auto_stop_time_weight_at_target_desc),
                checked = autoStopTimeWeightAtTarget,
                onCheckedChange = { viewModel.setAutoStopTimeWeightAtTarget(it) }
            )

            SettingDivider()

            SettingSwitchRow(
                icon = Icons.Rounded.PhoneAndroid,
                title = stringResource(R.string.keep_screen_on_set_timer),
                description = stringResource(R.string.keep_screen_on_set_timer_desc),
                checked = keepScreenOnSetTimer,
                onCheckedChange = { viewModel.setKeepScreenOnSetTimer(it) }
            )
        }
    }
}

/** How routines are written: a weight to start from and the %1RM programming tools. */
@Composable
private fun RoutinesAndExercisesCard(
    viewModel: SettingsViewModel,
    onNavigateToOneRepMaxes: () -> Unit
) {
    val weightUnit by viewModel.weightUnit.collectAsState()
    val startingWeightEnabled by viewModel.startingWeightEnabled.collectAsState()
    val advancedProgramming by viewModel.advancedProgrammingEnabled.collectAsState()
    val loadRoundingIncrement by viewModel.loadRoundingIncrement.collectAsState()

    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // A weight per exercise of a routine for its first workouts
            SettingSwitchRow(
                icon = Icons.Rounded.Scale,
                title = stringResource(R.string.starting_weight),
                description = stringResource(R.string.starting_weight_desc),
                checked = startingWeightEnabled,
                onCheckedChange = { viewModel.setStartingWeightEnabled(it) }
            )

            SettingDivider()

            // Powerlifting / %1RM prescriptions
            SettingSwitchRow(
                icon = Icons.Rounded.FitnessCenter,
                title = stringResource(R.string.advanced_programming),
                description = stringResource(R.string.advanced_programming_desc),
                checked = advancedProgramming,
                onCheckedChange = { viewModel.setAdvancedProgrammingEnabled(it) }
            )

            if (advancedProgramming) {
                SettingDivider()

                SettingNavRow(
                    icon = Icons.Rounded.EmojiEvents,
                    title = stringResource(R.string.one_rep_maxes),
                    description = stringResource(R.string.one_rep_maxes_empty_desc),
                    onClick = onNavigateToOneRepMaxes
                )

                SettingDivider()

                val increments = if (weightUnit == "lb") LoadCalculator.INCREMENTS_LB else LoadCalculator.INCREMENTS_KG
                SettingChoice(
                    title = stringResource(R.string.load_rounding),
                    description = stringResource(R.string.load_rounding_desc)
                ) {
                    ConnectedToggleRow(
                        options = increments.map { "${PrescriptionFormatter.number(it)} $weightUnit" },
                        selectedIndex = increments.indexOf(loadRoundingIncrement),
                        onSelect = { viewModel.setLoadRoundingIncrement(increments[it]) }
                    )
                }

            }
        }
    }
}

/** Workout journal: a rating, tags and a note on each workout. */
@Composable
private fun AfterWorkoutCard(viewModel: SettingsViewModel, onNavigateToJournalTags: () -> Unit) {
    val journalEnabled by viewModel.journalEnabled.collectAsState()
    val journalPromptAtFinish by viewModel.journalPromptAtFinish.collectAsState()
    val journalRatingEnabled by viewModel.journalRatingEnabled.collectAsState()

    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingSwitchRow(
                icon = Icons.Rounded.EditNote,
                title = stringResource(R.string.journal_title),
                description = stringResource(R.string.journal_desc),
                checked = journalEnabled,
                onCheckedChange = { viewModel.setJournalEnabled(it) }
            )

            AnimatedVisibility(visible = journalEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingDivider()

                    NestedSwitchRow(
                        title = stringResource(R.string.journal_prompt_finish),
                        description = stringResource(R.string.journal_prompt_finish_desc),
                        checked = journalPromptAtFinish,
                        onCheckedChange = { viewModel.setJournalPromptAtFinish(it) }
                    )
                    NestedSwitchRow(
                        title = stringResource(R.string.journal_rating_setting),
                        description = stringResource(R.string.journal_rating_setting_desc),
                        checked = journalRatingEnabled,
                        onCheckedChange = { viewModel.setJournalRatingEnabled(it) }
                    )

                    SettingDivider()

                    NestedNavRow(
                        title = stringResource(R.string.journal_manage_tags),
                        description = stringResource(R.string.journal_manage_tags_desc),
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
