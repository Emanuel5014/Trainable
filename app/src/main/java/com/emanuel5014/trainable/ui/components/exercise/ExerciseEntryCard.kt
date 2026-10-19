package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.ResolvedPrescription
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSecondaryContainer
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.SecondaryContainer
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.R
import androidx.compose.ui.res.stringResource

/** Size of the number badge; the superset rail in the routine screen is centred on it. */
val ExerciseBadgeSize = 40.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExerciseEntryCard(
    item: PlanExerciseWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int,
    isDragging: Boolean = false,
    languageCode: String = "en",
    isSuperset: Boolean = false,
    week: Int = 1,
    weeksCount: Int = 1,
    labels: PrescriptionLabels? = null,
    oneRepMaxKg: Float? = null
) {
    val exerciseName = ExerciseTranslations.translate(item.exercise.nome, languageCode)
    // Prescription blocks are only shown when the caller passes labels (advanced programming is on)
    val resolved = if (labels != null) item.resolve(week) else ResolvedPrescription.Legacy

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "exercise_card_press"
    )
    val badgeShape = MaterialShapes.Cookie9Sided.toShape()
    val highlighted = isSuperset || isDragging

    com.emanuel5014.trainable.ui.components.GymCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .alpha(if (resolved is ResolvedPrescription.Excluded || item.exercise.isDeleted) 0.5f else 1f)
            .clip(Shapes.extraLarge)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        containerColor = if (isSuperset) Primary.copy(alpha = 0.10f) else SurfaceContainer,
        border = if (isSuperset) BorderStroke(1.5.dp, Primary.copy(alpha = 0.45f)) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(ExerciseBadgeSize)
                    .clip(badgeShape)
                    .background(if (highlighted) Primary else SecondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (highlighted) OnPrimary else OnSecondaryContainer,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.width(Spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Deleted from the exercise list: the routine keeps it, greyed out, until the user adds it back
                if (item.exercise.isDeleted) {
                    DeletedExercisePill()
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (resolved is ResolvedPrescription.Excluded) {
                    PrescriptionPill(text = stringResource(R.string.not_in_week, week))
                } else if (resolved is ResolvedPrescription.Blocks && labels != null) {
                    PrescriptionBlocksSummary(blocks = resolved.blocks, labels = labels)
                    val meta = buildList {
                        if (oneRepMaxKg != null && resolved.blocks.any { it.intensityType == com.emanuel5014.trainable.domain.prescription.IntensityType.PERCENT }) {
                            add(stringResource(R.string.one_rep_max_value, labels.weight(oneRepMaxKg)))
                        }
                        add(stringResource(R.string.rest_label, item.planExercise.recuperoTarget))
                        if (resolved.isFallback && weeksCount > 1) add(stringResource(R.string.week_repeats, resolved.sourceWeek))
                    }
                    Text(
                        text = meta.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.planExercise.exerciseType == "cardio") {
                        val cat = item.planExercise.cardioCategoria ?: item.exercise.categoria
                        val label = if (cat.equals("Cardio", ignoreCase = true)) stringResource(R.string.cardio_cat_label) else stringResource(R.string.cardio_cat_with_name, cat)
                        ExercisePill(text = label, containerColor = Tertiary.copy(alpha = 0.15f), contentColor = Tertiary)
                        Spacer(modifier = Modifier.width(Spacing.small))
                        val durMin = item.planExercise.durataTargetSecondi?.let { it / 60 }
                        val distKm = item.planExercise.distanzaTargetKm
                        val cardioSets = item.planExercise.serieTarget
                        val targetText = buildString {
                            if (cardioSets > 1) append("$cardioSets × ")
                            if (durMin != null && durMin > 0) append(stringResource(R.string.cardio_min_format, durMin))
                            if (distKm != null && distKm > 0) {
                                if (isNotEmpty()) append(" • ")
                                append(stringResource(R.string.cardio_dist_value, distKm.toString()))
                            }
                            if (isEmpty()) append(stringResource(R.string.cardio_free_form))
                        }
                        Text(
                            text = targetText,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    } else if (item.planExercise.exerciseType == "time_and_weight") {
                        ExercisePill(
                            text = stringResource(R.string.time_and_weight_badge),
                            containerColor = Primary.copy(alpha = 0.15f),
                            contentColor = Primary
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        val durSec = item.planExercise.durataTargetSecondi ?: item.planExercise.repsTarget.filter { it.isDigit() }.toIntOrNull() ?: 45
                        Text(
                            text = "${item.planExercise.serieTarget} × ${durSec}s • " + stringResource(R.string.rest_label, item.planExercise.recuperoTarget),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    } else if (item.planExercise.exerciseType == TIME_AND_REPS) {
                        ExercisePill(
                            text = stringResource(R.string.time_and_reps_badge),
                            containerColor = Primary.copy(alpha = 0.15f),
                            contentColor = Primary
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        val durSec = item.planExercise.durataTargetSecondi ?: TimeAndReps.DEFAULT_SECONDS
                        val goal = TimeAndReps.goalOf(item.planExercise.repsTarget)
                        Text(
                            text = "${item.planExercise.serieTarget} × " + stringResource(R.string.time_reps_value, durSec, goal) +
                                " • " + stringResource(R.string.rest_label, item.planExercise.recuperoTarget),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    } else {
                        ExercisePill(
                            text = "${item.planExercise.serieTarget} × ${item.planExercise.repsDisplay}",
                            containerColor = SurfaceContainerHigh,
                            contentColor = OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text(
                            text = stringResource(R.string.rest_label, item.planExercise.recuperoTarget),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(Spacing.small))
            Icon(
                imageVector = Icons.Rounded.DragIndicator,
                contentDescription = "Reorder",
                tint = OnSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ExercisePill(text: String, containerColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .background(containerColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}
