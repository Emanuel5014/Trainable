package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import com.emanuel5014.trainable.ui.components.motion.RollingText
import com.emanuel5014.trainable.ui.components.HubChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.techniqueLabel
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.Tertiary

/**
 * Under the rest timer when the set just logged came out short of its reps: a tap turns the rest into the short
 * pause of a mini-set. Left alone, the rest simply goes on.
 */
@Composable
internal fun MiniSetOfferButton(missing: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HubChip(onClick = onClick, containerColor = Tertiary.copy(alpha = 0.15f), modifier = modifier) {
        Icon(Icons.Rounded.Replay, contentDescription = null, tint = Tertiary, modifier = Modifier.size(18.dp))
        Text(
            text = if (missing > 0) stringResource(R.string.mini_set_offer, missing) else stringResource(R.string.mini_set_offer_free),
            color = Tertiary,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** What the rest card says while it is the pause of a mini-set. */
@Composable
internal fun MiniSetPauseLabel(setNumber: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.mini_set_pause, setNumber).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Tertiary,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
    )
}

/**
 * The hub once the pause of a mini-set is over: how many reps it had (starting from the ones still missing), what the
 * set has so far, and the way out to the normal rest.
 */
@Composable
internal fun MiniSetEntry(
    setNumber: Int,
    soFar: String,
    initialReps: Int,
    hapticEnabled: Boolean,
    onLog: (Int) -> Unit,
    onEnd: () -> Unit
) {
    var reps by remember(setNumber, soFar) { mutableStateOf(initialReps.coerceAtLeast(1)) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.mini_set_title, setNumber),
            style = MaterialTheme.typography.labelLarge,
            color = Tertiary,
            fontWeight = FontWeight.ExtraBold
        )
        Text(text = stringResource(R.string.mini_set_so_far, soFar), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        CountStepper(value = reps, onValueChange = { reps = it }, min = 1, max = 99, hapticEnabled = hapticEnabled, big = true)
        GymButton(onClick = { onLog(reps) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.mini_set_log), fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
        TextButton(onClick = onEnd) {
            Text(stringResource(R.string.mini_set_end), fontWeight = FontWeight.ExtraBold, color = OnSurfaceVariant)
        }
    }
}

/**
 * − value +, as one connected group: the big version for the reps of a mini-set or of a Time & Reps set, the small one
 * for assisted and partial reps. The value rolls when it changes.
 */
@Composable
internal fun CountStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    hapticEnabled: Boolean,
    big: Boolean = false,
    label: String? = null
) {
    val haptic = LocalHapticFeedback.current
    fun change(to: Int) {
        if (to !in min..max) return
        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onValueChange(to)
    }
    val height = if (big) 48.dp else 36.dp
    val outer = height / 2
    val inner = 6.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        label?.let {
            Text(text = it.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontWeight = FontWeight.ExtraBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            FilledIconButton(
                onClick = { change(value - 1) },
                enabled = value > min,
                shape = RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = SurfaceContainerHighest, contentColor = OnSurface),
                modifier = Modifier.size(width = if (big) 56.dp else 44.dp, height = height)
            ) {
                Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.reps_decrease), modifier = Modifier.size(if (big) 24.dp else 18.dp))
            }
            Box(
                modifier = Modifier
                    .height(height)
                    .widthIn(min = if (big) 64.dp else 36.dp)
                    .clip(RoundedCornerShape(inner))
                    .background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                RollingText(
                    text = "$value",
                    style = if (big) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (value > 0 || big) Primary else OnSurfaceVariant
                )
            }
            FilledIconButton(
                onClick = { change(value + 1) },
                enabled = value < max,
                shape = RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (big) Primary else SurfaceContainerHighest, contentColor = if (big) OnPrimary else OnSurface),
                modifier = Modifier.size(width = if (big) 56.dp else 44.dp, height = height)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.reps_increase), modifier = Modifier.size(if (big) 24.dp else 18.dp))
            }
        }
    }
}

/**
 * Under the weight and reps of a set: the reps a spotter helped with, the partials, and, for a set with a prescribed
 * technique, the way to say it was done another way.
 */
@Composable
internal fun SetExtrasRow(
    assisted: Int,
    partial: Int,
    onAssistedChange: (Int) -> Unit,
    onPartialChange: (Int) -> Unit,
    hapticEnabled: Boolean,
    techniqueChanged: Boolean,
    onEditTechnique: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    // Folded away until needed (the panel is already full on a small phone); open when the set has something in it
    var expanded by remember { mutableStateOf(assisted > 0 || partial > 0 || techniqueChanged) }
    if (!expanded) {
        TextButton(onClick = { expanded = true }, modifier = modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(if (onEditTechnique != null) R.string.set_extras_more_technique else R.string.set_extras_more),
                color = OnSurfaceVariant,
                fontWeight = FontWeight.ExtraBold
            )
        }
        return
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CountStepper(assisted, onAssistedChange, 0, 99, hapticEnabled, label = stringResource(R.string.assisted_reps_label))
            CountStepper(partial, onPartialChange, 0, 99, hapticEnabled, label = stringResource(R.string.partial_reps_label))
        }
        if (onEditTechnique != null) {
            TextButton(onClick = onEditTechnique, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = if (techniqueChanged) Tertiary else OnSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.performed_technique_title),
                    color = if (techniqueChanged) Tertiary else OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * The techniques a set was really done with, starting from the prescribed ones (or what was already changed): a pause
 * or a tempo can be changed, any technique can be left out, a custom one can be rewritten ("Discesa 3, Fermo 1,
 * Salita 3"). Saving what the prescription says goes back to it.
 */
@Composable
internal fun PerformedTechniqueDialog(
    planned: List<Technique>,
    performed: List<Technique>?,
    onDismiss: () -> Unit,
    onSave: (techniques: List<Technique>?, applyToFollowing: Boolean) -> Unit,
    /** Off for a set of a past workout: there is no set still to do to carry it to. */
    showApplyToFollowing: Boolean = true
) {
    val context = LocalContext.current
    val start = performed ?: planned
    // One row per planned technique, with what was done instead (null: left out)
    val rows = remember {
        mutableStateListOf<Technique?>().apply {
            planned.forEach { p -> add(start.firstOrNull { sameKind(it, p) }) }
        }
    }
    var applyToFollowing by remember { mutableStateOf(true) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.performed_technique_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.performed_technique_hint), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                planned.forEachIndexed { index, plannedTechnique ->
                    val done = rows[index]
                    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainerHighest) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = done != null,
                                    onCheckedChange = { checked -> rows[index] = if (checked) plannedTechnique else null },
                                    colors = CheckboxDefaults.colors(checkedColor = Primary)
                                )
                                Text(
                                    text = techniqueLabel(context, plannedTechnique),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (done != null) OnSurface else OnSurfaceVariant
                                )
                            }
                            when (done) {
                                is Technique.Pause -> CountStepper(
                                    value = done.seconds,
                                    onValueChange = { rows[index] = Technique.Pause(it) },
                                    min = 1,
                                    max = 30,
                                    hapticEnabled = true,
                                    label = stringResource(R.string.seconds_label)
                                )
                                is Technique.Tempo -> OutlinedTextField(
                                    value = done.pattern,
                                    onValueChange = { text -> rows[index] = Technique.Tempo(text.filter { it.isDigit() || it == '-' || it == 'X' || it == 'x' }.take(11)) },
                                    label = { Text(stringResource(R.string.technique_edit)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                is Technique.Custom -> OutlinedTextField(
                                    value = done.text,
                                    onValueChange = { rows[index] = Technique.Custom(it.take(60)) },
                                    label = { Text(stringResource(R.string.technique_edit)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                else -> Unit
                            }
                            if (done == null) {
                                Text(stringResource(R.string.technique_not_done), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            }
                        }
                    }
                }
                if (showApplyToFollowing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = applyToFollowing, onCheckedChange = { applyToFollowing = it }, colors = CheckboxDefaults.colors(checkedColor = Primary))
                        Text(stringResource(R.string.performed_apply_following), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onSave(null, applyToFollowing) }) {
                    Text(stringResource(R.string.performed_restore), color = OnSurfaceVariant)
                }
                TextButton(onClick = {
                    // Empty texts are not a technique: as if left out
                    val done = rows.filterNotNull().filter {
                        when (it) {
                            is Technique.Custom -> it.text.isNotBlank()
                            is Technique.Tempo -> it.pattern.isNotBlank()
                            else -> true
                        }
                    }
                    onSave(done.takeIf { it != planned }, applyToFollowing)
                }) {
                    Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold, color = Primary)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel).uppercase(), color = OnSurfaceVariant) }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}

/** Two techniques of the same kind (a pause of any length, any tempo, any custom text). */
private fun sameKind(a: Technique, b: Technique): Boolean = when (a) {
    is Technique.Pause -> b is Technique.Pause
    is Technique.Tempo -> b is Technique.Tempo
    is Technique.Custom -> b is Technique.Custom
    else -> a == b
}
