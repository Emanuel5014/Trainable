package com.emanuel5014.trainable.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExerciseCustomization: () -> Unit,
    onNavigateToOneRepMaxes: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val weightUnit by viewModel.weightUnit.collectAsState()
    val advancedProgramming by viewModel.advancedProgrammingEnabled.collectAsState()
    val loadRoundingIncrement by viewModel.loadRoundingIncrement.collectAsState()
    val rpeInputMode by viewModel.rpeInputMode.collectAsState()
    val workoutTimerEnabled by viewModel.workoutTimerEnabled.collectAsState()
    val inlineExerciseModificationsEnabled by viewModel.inlineExerciseModificationsEnabled.collectAsState()
    val autoStopCardioAtTarget by viewModel.autoStopCardioAtTarget.collectAsState()
    val autoStopTimeWeightAtTarget by viewModel.autoStopTimeWeightAtTarget.collectAsState()
    val keepScreenOnCardioTimer by viewModel.keepScreenOnCardioTimer.collectAsState()
    val keepScreenOnSetTimer by viewModel.keepScreenOnSetTimer.collectAsState()

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GymCard(containerColor = SurfaceContainerHigh) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.workout_timer),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    stringResource(R.string.workout_timer_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = workoutTimerEnabled,
                            onCheckedChange = { viewModel.setWorkoutTimerEnabled(it) }
                        )
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.AddCircleOutline,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.inline_exercise_modifications),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    stringResource(R.string.inline_exercise_modifications_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = inlineExerciseModificationsEnabled,
                            onCheckedChange = { viewModel.setInlineExerciseModificationsEnabled(it) }
                        )
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.DirectionsRun,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.auto_stop_cardio_at_target),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    stringResource(R.string.auto_stop_cardio_at_target_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = autoStopCardioAtTarget,
                            onCheckedChange = { viewModel.setAutoStopCardioAtTarget(it) }
                        )
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.auto_stop_time_weight_at_target),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    stringResource(R.string.auto_stop_time_weight_at_target_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = autoStopTimeWeightAtTarget,
                            onCheckedChange = { viewModel.setAutoStopTimeWeightAtTarget(it) }
                        )
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneAndroid,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.keep_screen_on_cardio_timer),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    stringResource(R.string.keep_screen_on_cardio_timer_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = keepScreenOnCardioTimer,
                            onCheckedChange = { viewModel.setKeepScreenOnCardioTimer(it) }
                        )
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneAndroid,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.keep_screen_on_set_timer),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    stringResource(R.string.keep_screen_on_set_timer_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = keepScreenOnSetTimer,
                            onCheckedChange = { viewModel.setKeepScreenOnSetTimer(it) }
                        )
                    }

                    HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToExerciseCustomization() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.exercise_customization),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    stringResource(R.string.exercise_customization_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                    }
                }
            }

            // Powerlifting / %1RM prescriptions
            GymCard(containerColor = SurfaceContainerHigh) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.advanced_programming),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OnSurface,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    stringResource(R.string.advanced_programming_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        SettingsSwitch(
                            checked = advancedProgramming,
                            onCheckedChange = { viewModel.setAdvancedProgrammingEnabled(it) }
                        )
                    }

                    if (advancedProgramming) {
                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToOneRepMaxes() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.one_rep_maxes),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = OnSurface,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        stringResource(R.string.one_rep_maxes_empty_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                stringResource(R.string.load_rounding),
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                stringResource(R.string.load_rounding_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                            val increments = if (weightUnit == "lb") {
                                com.emanuel5014.trainable.domain.prescription.LoadCalculator.INCREMENTS_LB
                            } else {
                                com.emanuel5014.trainable.domain.prescription.LoadCalculator.INCREMENTS_KG
                            }
                            com.emanuel5014.trainable.ui.components.ConnectedToggleRow(
                                options = increments.map { "${com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter.number(it)} $weightUnit" },
                                selectedIndex = increments.indexOf(loadRoundingIncrement),
                                onSelect = { viewModel.setLoadRoundingIncrement(increments[it]) }
                            )
                        }

                        HorizontalDivider(color = Surface.copy(alpha = 0.5f))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                stringResource(R.string.rpe_input),
                                style = MaterialTheme.typography.titleMedium,
                                color = OnSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                stringResource(R.string.rpe_input_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                            com.emanuel5014.trainable.ui.components.ConnectedToggleRow(
                                options = listOf(
                                    stringResource(R.string.rpe_input_advanced),
                                    stringResource(R.string.rpe_input_all),
                                    stringResource(R.string.rpe_input_never)
                                ),
                                selectedIndex = rpeInputMode,
                                onSelect = { viewModel.setRpeInputMode(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
