package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSecondaryContainer
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OnTertiary
import com.emanuel5014.trainable.ui.theme.OnTertiaryContainer
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SecondaryContainer
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.ui.theme.TertiaryContainer
import com.emanuel5014.trainable.util.WeightUnitConverter

@OptIn(ExperimentalFoundationApi::class)
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
    onLongClick: (() -> Unit)? = null,
    onEditValues: (() -> Unit)? = null,
    isActive: Boolean = false,
    weightUnit: String = "kg",
    previousNote: String? = null,
    timeSeconds: Int? = null,
    isExpanded: Boolean = false,
    onToggleExpanded: (() -> Unit)? = null,
    expandedContent: (@Composable () -> Unit)? = null
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
            .combinedClickable(
                onClick = { 
                    if (isActive && onEditValues != null) {
                        onEditValues()
                    } else {
                        onToggleComplete()
                    }
                },
                onLongClick = onLongClick
            )
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                fontWeight = FontWeight.ExtraBold,
                color = when {
                    isCompleted -> OnTertiary
                    isActive -> OnPrimary
                    else -> OnSurfaceVariant
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Set Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val repsOrTimeText = if (timeSeconds != null) "${timeSeconds}s" else "$reps"
                Text(
                    text = WeightUnitConverter.formatWithUnit(
                        WeightUnitConverter.convertDisplay(weight, weightUnit),
                        weightUnit
                    ) + " × $repsOrTimeText",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
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
                            fontWeight = FontWeight.ExtraBold,
                            color = OnSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Optional Expand/Collapse Button (next to Note icon)
        if (onToggleExpanded != null) {
            IconButton(onClick = onToggleExpanded) {
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse timer" else "Expand timer",
                    tint = if (isExpanded) Primary else OnSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        // Note Icon Button
        val hasPreviousHint = !isCompleted && note.isNullOrBlank() && !previousNote.isNullOrBlank()
        IconButton(onClick = { isNoteEditing = !isNoteEditing }) {
            Icon(
                imageVector = if (!note.isNullOrBlank()) Icons.AutoMirrored.Rounded.Notes else Icons.Rounded.EditNote,
                contentDescription = "Edit Note",
                tint = when {
                    isCompleted -> OnTertiaryContainer.copy(alpha = if (!note.isNullOrBlank()) 1f else 0.7f)
                    !note.isNullOrBlank() -> Primary
                    hasPreviousHint -> OnSurfaceVariant.copy(alpha = 0.4f)
                    else -> OnSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Checkbox area
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isCompleted) Tertiary else SurfaceContainerHigh)
                .clickable { onToggleComplete() },
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

        // Expanded Content (e.g. Timer for Time & Weight)
        if (isExpanded && expandedContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
            ) {
                expandedContent()
            }
        }

        // Note Input Area (Expandable)
        val showPreviousHint = !isCompleted && note.isNullOrBlank() && !previousNote.isNullOrBlank()
        if (isNoteEditing || !note.isNullOrBlank() || showPreviousHint) {
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
                } else if (!note.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isNoteEditing = true }
                        )
                    }
                } else if (showPreviousHint) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .clickable {
                                onNoteChange(previousNote)
                                isNoteEditing = true
                            }
                    ) {
                        Text(
                            text = previousNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}