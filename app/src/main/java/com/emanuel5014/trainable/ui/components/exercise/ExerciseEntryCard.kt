package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

@Composable
fun ExerciseEntryCard(
    item: PlanExerciseWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    languageCode: String = "en",
    isSuperset: Boolean = false
) {
    val exerciseName = ExerciseTranslations.translate(item.exercise.nome, languageCode)
    com.emanuel5014.trainable.ui.components.GymCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.extraLarge)
            .clickable { onClick() },
        containerColor = if (isSuperset) com.emanuel5014.trainable.ui.theme.Primary.copy(alpha = 0.08f) else SurfaceContainer,
        border = if (isSuperset) androidx.compose.foundation.BorderStroke(1.5.dp, com.emanuel5014.trainable.ui.theme.Primary.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Reorder",
                tint = OnSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(Spacing.medium))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.planExercise.exerciseType == "cardio") {
                        Box(
                            modifier = Modifier
                                .background(com.emanuel5014.trainable.ui.theme.Tertiary.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = Spacing.small, vertical = 2.dp)
                        ) {
                            val cat = item.planExercise.cardioCategoria ?: item.exercise.categoria
                            val label = if (cat.equals("Cardio", ignoreCase = true)) "Cardio" else "Cardio ($cat)"
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = com.emanuel5014.trainable.ui.theme.Tertiary
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.small))
                        val durMin = item.planExercise.durataTargetSecondi?.let { it / 60 }
                        val distKm = item.planExercise.distanzaTargetKm
                        val targetText = buildString {
                            if (durMin != null && durMin > 0) append("${durMin} min")
                            if (distKm != null && distKm > 0) {
                                if (isNotEmpty()) append(" • ")
                                append("${distKm} km")
                            }
                            if (isEmpty()) append("Free-form")
                        }
                        Text(
                            text = targetText,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .background(SurfaceContainerHigh, CircleShape)
                                .padding(horizontal = Spacing.small, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${item.planExercise.serieTarget} × ${item.planExercise.repsTarget}",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text(
                            text = "Rest: ${item.planExercise.recuperoTarget}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
