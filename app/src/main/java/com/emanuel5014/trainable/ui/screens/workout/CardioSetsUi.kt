package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OnTertiaryContainer
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.TertiaryContainer

/** "12:30", or "1:05:00" past the hour. */
internal fun cardioTimeText(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

/**
 * The sets of a cardio exercise as chips: the done ones with their time (and distance), the one on the timer
 * outlined, the ones still to do, and a "+" for one more. A tap opens the set (its note, and its values once done).
 */
@Composable
internal fun CardioSetsStrip(
    sets: List<CardioSetState>,
    activeIndex: Int,
    onSetClick: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sets.forEachIndexed { index, set ->
            val active = index == activeIndex
            Surface(
                onClick = { onSetClick(index) },
                shape = CircleShape,
                color = when {
                    set.isCompleted -> TertiaryContainer
                    active -> Primary.copy(alpha = 0.15f)
                    else -> SurfaceContainerHigh
                },
                border = if (active) BorderStroke(1.5.dp, Primary) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = when {
                        set.isCompleted -> OnTertiaryContainer
                        active -> Primary
                        else -> OnSurfaceVariant
                    }
                    if (set.isCompleted) Icon(Icons.Rounded.Check, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (set.isCompleted) {
                            buildString {
                                append("${set.setNumber} · ${cardioTimeText(set.elapsedSeconds)}")
                                if (set.distanceKm > 0f) append(" · ").append(stringResource(R.string.cardio_dist_value, com.emanuel5014.trainable.ui.components.cardioKmText(set.distanceKm)))
                            }
                        } else stringResource(R.string.set_number, set.setNumber),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Bold,
                        color = color
                    )
                    if (!set.note.isNullOrBlank()) {
                        Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        Surface(onClick = onAdd, shape = CircleShape, color = SurfaceContainerHigh) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = stringResource(R.string.cardio_add_set),
                tint = Primary,
                modifier = Modifier.padding(8.dp).size(20.dp)
            )
        }
    }
}

/** The note of a set under its chips; a tap edits it. */
@Composable
internal fun CardioSetNoteLine(note: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = SurfaceContainerHigh.copy(alpha = 0.6f), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(14.dp))
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = OnSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * A cardio set: its note, and for a done set its time and distance too. [onDelete] is null when the set cannot go
 * (the only one, or the one on the timer).
 */
@Composable
internal fun CardioSetDialog(
    set: CardioSetState,
    onDismiss: () -> Unit,
    onSave: (seconds: Int, distanceKm: Float, note: String) -> Unit,
    onDelete: (() -> Unit)?
) {
    var minutes by remember { mutableStateOf((set.elapsedSeconds / 60).toString()) }
    var seconds by remember { mutableStateOf((set.elapsedSeconds % 60).toString()) }
    var distance by remember { mutableStateOf(if (set.distanceKm > 0f) set.distanceKm.toString() else "") }
    var note by remember { mutableStateOf(set.note.orEmpty()) }
    val totalSeconds = (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
    val valid = !set.isCompleted || totalSeconds > 0

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_number, set.setNumber)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (set.isCompleted) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = minutes,
                            onValueChange = { minutes = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text(stringResource(R.string.minutes)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = seconds,
                            onValueChange = { seconds = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text(stringResource(R.string.seconds_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = distance,
                        onValueChange = { distance = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text(stringResource(R.string.cardio_distance_label) + " " + stringResource(R.string.optional_suffix)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.routine_notes)) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.cardio_remove_set), tint = Error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel).uppercase()) }
                TextButton(
                    enabled = valid,
                    onClick = { onSave(totalSeconds, distance.replace(',', '.').toFloatOrNull() ?: 0f, note) }
                ) {
                    Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold, color = if (valid) Primary else OnSurface.copy(alpha = 0.4f))
                }
            }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}
