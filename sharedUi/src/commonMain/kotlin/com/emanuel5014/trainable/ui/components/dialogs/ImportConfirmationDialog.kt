package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.compare_cardio
import com.emanuel5014.trainable.sharedui.resources.exercise_type_advanced
import com.emanuel5014.trainable.sharedui.resources.exercise_type_time_and_reps
import com.emanuel5014.trainable.sharedui.resources.exercise_type_time_and_weight
import com.emanuel5014.trainable.sharedui.resources.import_confirm
import com.emanuel5014.trainable.sharedui.resources.import_includes_maximums
import com.emanuel5014.trainable.sharedui.resources.import_plan_summary
import com.emanuel5014.trainable.sharedui.resources.import_plans_message
import com.emanuel5014.trainable.sharedui.resources.import_plans_title
import com.emanuel5014.trainable.sharedui.resources.schedule_days
import com.emanuel5014.trainable.sharedui.resources.superset
import com.emanuel5014.trainable.sharedui.resources.week_of
import com.emanuel5014.trainable.data.remote.dto.WorkoutPlanExportDto
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

@Composable
fun ImportConfirmationDialog(
    plans: List<WorkoutPlanExportDto>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    contentModifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.import_plans_title)) },
        text = {
            Column(modifier = contentModifier, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.import_plans_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
                plans.forEach { plan ->
                    val cardioLabel = stringResource(Res.string.compare_cardio)
                    val timedLabel = stringResource(Res.string.exercise_type_time_and_weight)
                    val timedRepsLabel = stringResource(Res.string.exercise_type_time_and_reps)
                    val supersetLabel = stringResource(Res.string.superset).lowercase()
                    val advancedLabel = stringResource(Res.string.exercise_type_advanced)
                    val typeSummary = buildString {
                        val cardio = plan.exercises.count { it.exerciseType == "cardio" }
                        val timed = plan.exercises.count { it.exerciseType == "time_and_weight" }
                        val timedReps = plan.exercises.count { it.exerciseType == "time_and_reps" }
                        val supersets = plan.exercises.mapNotNull { it.supersetId }.distinct().size
                        val advanced = plan.exercises.count { it.blocks.isNotEmpty() }
                        if (cardio > 0) append(" • $cardio $cardioLabel")
                        if (timed > 0) append(" • $timed $timedLabel")
                        if (timedReps > 0) append(" • $timedReps $timedRepsLabel")
                        if (supersets > 0) append(" • $supersets $supersetLabel")
                        if (advanced > 0) append(" • $advanced $advancedLabel")
                    }
                    val weeksLabel = if (plan.weeksCount > 1) " • " + stringResource(Res.string.week_of, 1, plan.weeksCount) else ""
                    Text(
                        text = "• " + stringResource(
                            Res.string.import_plan_summary,
                            plan.nome,
                            plan.exercises.size
                        ) + typeSummary + weeksLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                    val maximums = plan.exercises.count { it.oneRepMaxKg != null }
                    if (maximums > 0) {
                        Text(
                            text = stringResource(Res.string.import_includes_maximums, maximums),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    if (plan.giorniSettimana != null) {
                        Text(
                            text = stringResource(Res.string.schedule_days) + ": " + plan.giorniSettimana,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            GymButton(
                onClick = onConfirm,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
            ) {
                Text(stringResource(Res.string.import_confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            GymButton(
                onClick = onDismiss,
                containerColor = Color.Transparent,
                contentColor = OnSurfaceVariant,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(Res.string.cancel).uppercase())
            }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}
