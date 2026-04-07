package com.example.gymtracking.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymtracking.ui.theme.*

@Composable
fun SetLogRow(
    setNumber: Int,
    weight: Float,
    reps: Int,
    note: String?,
    isWarmup: Boolean,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCompleted -> TertiaryContainer
            isActive -> Primary.copy(alpha = 0.08f)
            else -> SurfaceContainer
        },
        label = "set_row_bg"
    )
    val numberContainerColor by animateColorAsState(
        targetValue = when {
            isCompleted -> Tertiary
            isActive -> Primary
            else -> SurfaceContainerHighest
        },
        label = "set_row_num_bg"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isCompleted -> OnTertiaryContainer
            isActive -> Primary
            else -> OnSurface
        },
        label = "set_row_text_col"
    )

    var isNoteEditing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isActive && !isCompleted) {
                    Modifier.border(2.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                } else Modifier
            )
            .background(backgroundColor)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleComplete() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Set Number Badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(numberContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$setNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) OnTertiary else OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Set Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${weight}kg × $reps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (isWarmup) {
                    Surface(
                        color = SecondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "WARM UP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Note Icon Button
        IconButton(onClick = { isNoteEditing = !isNoteEditing }) {
            Icon(
                imageVector = if (!note.isNullOrBlank()) Icons.Rounded.Notes else Icons.Rounded.EditNote,
                contentDescription = "Edit Note",
                tint = if (!note.isNullOrBlank()) Primary else OnSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Checkbox area
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isCompleted) Tertiary else SurfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Completed",
                    tint = OnTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        }

        // Note Input Area (Expandable)
        if (isNoteEditing || !note.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            ) {
                if (isNoteEditing) {
                    TextField(
                        value = note ?: "",
                        onValueChange = onNoteChange,
                        placeholder = { Text("Add a note for this set...", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SurfaceContainer,
                            unfocusedContainerColor = SurfaceContainer,
                            focusedIndicatorColor = Primary,
                            unfocusedIndicatorColor = OnSurfaceVariant.copy(alpha = 0.5f),
                            cursorColor = Primary
                        ),
                        singleLine = false
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = note ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isNoteEditing = true }
                        )
                    }
                }
            }
        }
    }
}
