package com.example.gymtracking.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.gymtracking.data.local.relation.PlanExerciseWithDetails
import com.example.gymtracking.ui.theme.OnSurface
import com.example.gymtracking.ui.theme.OnSurfaceVariant
import com.example.gymtracking.ui.theme.Shapes
import com.example.gymtracking.ui.theme.Spacing
import com.example.gymtracking.ui.theme.SurfaceContainer
import com.example.gymtracking.data.ExerciseTranslations
import com.example.gymtracking.ui.theme.SurfaceContainerHigh

@Composable
fun ExerciseEntryCard(
    item: PlanExerciseWithDetails,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    languageCode: String = "en"
) {
    val exerciseName = ExerciseTranslations.translate(item.exercise.nome, languageCode)
    GymCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = SurfaceContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Reorder",
                tint = OnSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(Spacing.medium))
            
            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick stats pill
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
            
            // Menu
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Options",
                    tint = OnSurfaceVariant
                )
            }
        }
    }
}
