package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.ui.components.ExerciseMediaSlot
import com.emanuel5014.trainable.ui.components.ExerciseInfoButton
import com.emanuel5014.trainable.ui.components.ProgramEditPill
import com.emanuel5014.trainable.ui.components.ProgressionSuggestionCard
import androidx.compose.ui.semantics.contentDescription
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.domain.prescription.IntensityType
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.ui.components.PrescriptionPill

/** What sits above the sets of a strength or timed exercise: prescription pills, tools, name, media thumbnail and the progression card. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ExerciseHeader(
    targetExState: WorkoutExerciseState,
    targetIndex: Int,
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    viewModel: WorkoutViewModel,
    languageCode: String,
    exerciseMedia: Map<Int, String>,
    largeMediaVisible: Boolean,
    pickMedia: (Int) -> Unit,
    prescriptionLabels: PrescriptionLabels,
    onOpenDetail: (Int) -> Unit
) {
    // Exercise Header
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // An advanced exercise reads what the current week prescribes: the plan's own
            // sets and reps targets are only kept in step with the first week
            val weekTargets = if (targetExState.isAdvanced) {
                PrescriptionExpander.legacyTargets(targetExState.blocks)
            } else null
            val setsCount = weekTargets?.first ?: targetExState.sets.size
            val repsCount = if (targetExState.isTimeAndReps) {
                val sec = targetExState.timeTargetSeconds ?: targetExState.sets.firstOrNull()?.timeSeconds ?: 45
                stringResource(R.string.time_reps_value, sec, targetExState.repsGoal ?: 0)
            } else if (targetExState.isTimeAndWeight) {
                val sec = targetExState.timeTargetSeconds ?: targetExState.sets.firstOrNull()?.timeSeconds ?: 45
                "${sec}s"
            } else {
                weekTargets?.second ?: targetExState.repsDisplay.takeIf { it.isNotBlank() } ?: run {
                    if (targetExState.sets.isEmpty()) "0"
                    else {
                        val allReps = targetExState.sets.map { it.reps }
                        if (allReps.distinct().size == 1) allReps.first().toString()
                        else allReps.joinToString("-")
                    }
                }
            }
            if (targetExState.isAdvanced) {
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    state.programWeek?.let { week ->
                        PrescriptionPill(
                            text = stringResource(R.string.week_short, week),
                            containerColor = Primary,
                            contentColor = OnPrimary,
                            emphasized = true
                        )
                    }
                    val usesPercent = targetExState.blocks.any { it.intensityType == IntensityType.PERCENT }
                    if (usesPercent) {
                        PrescriptionPill(
                            text = targetExState.oneRepMaxKg?.let { stringResource(R.string.one_rep_max_value, prescriptionLabels.weight(it)) }
                                ?: stringResource(R.string.one_rep_max_missing),
                            containerColor = if (targetExState.oneRepMaxKg != null) Primary.copy(alpha = 0.12f) else Error.copy(alpha = 0.12f),
                            contentColor = if (targetExState.oneRepMaxKg != null) Primary else Error
                        )
                    } else {
                        Text(
                            text = "$setsCount × $repsCount",
                            style = MaterialTheme.typography.titleMedium,
                            color = Primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    // The program of this exercise was changed during the workout: tap for the story
                    targetExState.lastProgramEdit?.let { edit ->
                        ProgramEditPill(edit, Modifier.clickable { ui.programHistoryFor = targetIndex })
                    }
                }
            } else Text(
                text = "$setsCount × $repsCount",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            if (targetIndex < state.exercises.lastIndex) {
                val nextEx = state.exercises[targetIndex + 1]
                val isLinked = targetExState.supersetId != null && targetExState.supersetId == nextEx.supersetId
                IconButton(
                    onClick = { viewModel.toggleSupersetWithNext(targetIndex) }
                ) {
                    Icon(
                        imageVector = if (isLinked) Icons.Rounded.LinkOff else Icons.Rounded.Link,
                        contentDescription = stringResource(R.string.toggle_superset_with_next),
                        tint = if (isLinked) Primary else OnSurfaceVariant
                    )
                }
            }
            if (targetExState.isAdvanced && !targetExState.isCardio && !targetExState.isTimed) {
                IconButton(onClick = { ui.programEditFor = targetIndex }) {
                    Icon(
                        imageVector = Icons.Rounded.DesignServices,
                        contentDescription = stringResource(R.string.edit_program),
                        tint = if (targetExState.lastProgramEdit != null) Primary else OnSurfaceVariant
                    )
                }
            }
            if (!targetExState.isCardio) {
                ExerciseInfoButton(onClick = { onOpenDetail(targetExState.exercise.id) })
            }
            IconButton(
                onClick = { ui.showSwapExerciseSheet = true }
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = stringResource(R.string.swap_exercise),
                    tint = OnSurfaceVariant
                )
            }
        }
        val exerciseDisplayName = ExerciseTranslations.translate(targetExState.exercise.nome, languageCode)
        if (state.exerciseMediaEnabled) {
            // The thumbnail takes width from the name, so the name steps down one size
            // to wrap onto the same number of lines it did at full width.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = exerciseDisplayName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnSurface,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                ExerciseMediaSlot(
                    fileName = exerciseMedia[targetExState.exercise.id],
                    onOpen = { ui.mediaViewer = targetExState.exercise.id to exerciseDisplayName },
                    onAdd = { pickMedia(targetExState.exercise.id) },
                    playing = !largeMediaVisible
                )
            }
        } else {
            Text(
                text = exerciseDisplayName,
                style = MaterialTheme.typography.displaySmall,
                color = OnSurface,
                fontWeight = FontWeight.Black
            )
        }

        if (targetExState.emomStartIndex != null && targetExState.exercise.id in ui.emomManualExercises) {
            Surface(
                onClick = { ui.emomManualExercises = ui.emomManualExercises - targetExState.exercise.id },
                color = Tertiary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = Tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.emom_use_timer),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = Tertiary,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        if (targetExState.supersetId != null) {
            Surface(
                color = Primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.superset),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = Primary,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
    
    targetExState.progressionToShow?.let { suggestion ->
        ProgressionSuggestionCard(
            suggestion = suggestion,
            weightUnit = state.weightUnit,
            onApply = { viewModel.applyProgression(targetIndex) },
            onDismiss = { viewModel.dismissProgression(targetIndex) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
        )
    }
}
