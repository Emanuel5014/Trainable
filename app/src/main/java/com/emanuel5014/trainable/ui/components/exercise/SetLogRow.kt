package com.emanuel5014.trainable.ui.components

import androidx.compose.ui.platform.testTag
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateDpAsState
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.LocalLiquidGlassEnabled
import com.emanuel5014.trainable.ui.theme.liquidGlassSurface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.emanuel5014.trainable.ui.components.glass.GlassIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import com.emanuel5014.trainable.ui.components.glass.GlassSurface as Surface
import androidx.compose.material3.Text
import com.emanuel5014.trainable.ui.components.glass.GlassTextField as TextField
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
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.ui.theme.TertiaryContainer

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
    /** Opens a set that is not the active one for editing, done or still to do. Without it a tap on the row checks it. */
    onEdit: (() -> Unit)? = null,
    /** This set is the one being edited. */
    isEditing: Boolean = false,
    isActive: Boolean = false,
    weightUnit: String = "kg",
    previousNote: String? = null,
    timeSeconds: Int? = null,
    isExpanded: Boolean = false,
    onToggleExpanded: (() -> Unit)? = null,
    expandedContent: (@Composable () -> Unit)? = null,
    /** Replaces the reps number while the set is pending (e.g. "MAX" for AMRAP). */
    repsLabel: String? = null,
    /** Highlighted prescription badge, e.g. "75%". */
    intensityBadge: String? = null,
    /** Secondary badges: techniques, RPE, EXTRA. */
    badges: List<String> = emptyList(),
    /** A Time & Reps set: written "45s · 20 reps", without a weight. */
    isTimeAndReps: Boolean = false,
    /** Corner radii of the row, from its place in the segmented list of its block. */
    corners: SegmentCorners = SegmentCorners(SegmentedShapes.OUTER_DP, SegmentedShapes.OUTER_DP)
) {
    val glass = LocalLiquidGlassEnabled.current
    val topCorners by animateDpAsState(corners.topDp.dp, WorkoutMotion.spatial(), label = "set_row_top_corners")
    val bottomCorners by animateDpAsState(corners.bottomDp.dp, WorkoutMotion.spatial(), label = "set_row_bottom_corners")
    // The active set steps out of its group
    val activeMargin by animateDpAsState(if (isActive && !glass) 4.dp else 0.dp, WorkoutMotion.spatial(), label = "set_row_active_margin")
    // The springs overshoot: a size never goes below zero
    val top = topCorners.coerceAtLeast(0.dp)
    val bottom = bottomCorners.coerceAtLeast(0.dp)
    val rowShape = if (glass) RoundedCornerShape(20.dp) else RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom)
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
            .padding(vertical = activeMargin.coerceAtLeast(0.dp))
            .fillMaxWidth()
            .testTag("set_row")
            .clip(rowShape)
            .then(
                when {
                    isActive && !isCompleted -> if (glass) Modifier.border(1.dp, Primary.copy(alpha = 0.45f), rowShape)
                        else Modifier.border(2.dp, Primary.copy(alpha = 0.3f), rowShape)
                    isEditing -> Modifier.border(if (glass) 1.dp else 2.dp, Primary.copy(alpha = 0.7f), rowShape)
                    else -> Modifier
                }
            )
            .liquidGlassSurface(rowShape, backgroundColor, control = true, quiet = true)
            .background(if (glass) Color.Transparent else backgroundColor)
            .combinedClickable(
                onClick = {
                    when {
                        isActive && onEditValues != null -> onEditValues()
                        onEdit != null -> onEdit()
                        else -> onToggleComplete()
                    }
                },
                onLongClick = onLongClick
            )
            .animateContentSize(WorkoutMotion.spatial())
    ) {
        val hasPills = intensityBadge != null || badges.isNotEmpty()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = if (hasPills) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Set Number Badge
        SetNumberBadge(
            number = setNumber,
            shape = when {
                isCompleted -> BadgeShape.Done
                isActive -> BadgeShape.Active
                else -> BadgeShape.Pending
            },
            containerColor = numberContainerColor,
            contentColor = when {
                isCompleted -> OnTertiary
                isActive -> OnPrimary
                else -> OnSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Set Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = setValueText(
                        kind = when {
                            isTimeAndReps -> SetLogKind.TIME_AND_REPS
                            timeSeconds != null -> SetLogKind.TIME_AND_WEIGHT
                            else -> SetLogKind.STRENGTH
                        },
                        weightKg = weight,
                        reps = reps,
                        seconds = timeSeconds,
                        weightUnit = weightUnit,
                        repsText = repsLabel?.takeIf { !isCompleted }
                    ),
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
        val noteTint = when {
            isCompleted -> OnTertiaryContainer.copy(alpha = if (!note.isNullOrBlank()) 1f else 0.7f)
            !note.isNullOrBlank() -> Primary
            hasPreviousHint -> OnSurfaceVariant.copy(alpha = 0.4f)
            else -> OnSurfaceVariant.copy(alpha = 0.6f)
        }
        val noteIcon = if (!note.isNullOrBlank()) Icons.AutoMirrored.Rounded.Notes else Icons.Rounded.EditNote
        if (glass) {
            GymIconButton(
                onClick = { isNoteEditing = !isNoteEditing },
                icon = noteIcon, description = "Edit Note", shape = RoundedCornerShape(14.dp), quiet = true,
                contentColor = noteTint
            )
        } else {
            IconButton(onClick = { isNoteEditing = !isNoteEditing }) {
                Icon(imageVector = noteIcon, contentDescription = "Edit Note", tint = noteTint)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Checkbox area
        SetCheckTarget(isCompleted = isCompleted, isActive = isActive, onClick = onToggleComplete)
        }

        // What the set asks on top of its weight and reps, under the whole row
        if (hasPills) {
            FlowRow(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                intensityBadge?.let {
                    PrescriptionPill(
                        text = it,
                        containerColor = if (isCompleted) Tertiary.copy(alpha = 0.3f) else Primary.copy(alpha = 0.14f),
                        contentColor = if (isCompleted) OnTertiaryContainer else Primary,
                        emphasized = true
                    )
                }
                badges.forEach {
                    PrescriptionPill(
                        text = it,
                        containerColor = if (isCompleted) OnTertiaryContainer.copy(alpha = 0.1f) else SurfaceContainerHighest,
                        contentColor = if (isCompleted) OnTertiaryContainer else OnSurfaceVariant
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

/** The square on the right that checks and unchecks the set: an outline with a faint check to do, filled with a check popping in once done. */
@Composable
private fun SetCheckTarget(isCompleted: Boolean, isActive: Boolean, onClick: () -> Unit) {
    val glass = LocalLiquidGlassEnabled.current
    val shape = RoundedCornerShape(16.dp)
    val fill by animateColorAsState(if (isCompleted) Tertiary else Color.Transparent, label = "set_check_fill")
    val outline by animateColorAsState(
        when {
            isCompleted -> Color.Transparent
            isActive -> Primary.copy(alpha = 0.5f)
            else -> OnSurfaceVariant.copy(alpha = 0.25f)
        },
        label = "set_check_outline"
    )
    val tint by animateColorAsState(
        when {
            glass && isCompleted -> Tertiary
            isCompleted -> OnTertiary
            isActive -> Primary.copy(alpha = if (glass) 0.9f else 0.5f)
            else -> OnSurfaceVariant.copy(alpha = if (glass) 0.75f else 0.3f)
        },
        label = "set_check_tint"
    )
    val actionLabel = stringResource(R.string.log_set)
    // The check pops in when the set is logged, not when a done set first appears
    val pop = remember { Animatable(1f) }
    var firstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(isCompleted) {
        if (isCompleted && !firstComposition) {
            pop.snapTo(0.6f)
            pop.animateTo(1f, WorkoutMotion.spatial())
        }
        firstComposition = false
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .liquidGlassSurface(shape, if (isCompleted) Tertiary.copy(alpha = .25f) else SurfaceContainerHighest, control = true, quiet = true)
            .background(if (glass) Color.Transparent else fill)
            .border(1.5.dp, outline, shape)
            .testTag("set_check")
            .semantics { contentDescription = actionLabel }
            .toggleable(value = isCompleted, role = Role.Checkbox, onValueChange = { onClick() }),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = if (isCompleted) "Completed" else null,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
        )
    }
}
