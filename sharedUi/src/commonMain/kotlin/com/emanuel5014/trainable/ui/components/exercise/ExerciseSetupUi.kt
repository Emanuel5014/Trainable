package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.domain.exercise.SetupNote
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.exercise_setup_add_cd
import com.emanuel5014.trainable.sharedui.resources.exercise_setup_clear
import com.emanuel5014.trainable.sharedui.resources.exercise_setup_edit_cd
import com.emanuel5014.trainable.sharedui.resources.exercise_setup_field
import com.emanuel5014.trainable.sharedui.resources.exercise_setup_hint
import com.emanuel5014.trainable.sharedui.resources.exercise_setup_preview
import com.emanuel5014.trainable.sharedui.resources.exercise_setup_subtitle
import com.emanuel5014.trainable.sharedui.resources.exercise_setup_title
import com.emanuel5014.trainable.sharedui.resources.save
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSecondaryContainer
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.SecondaryContainer
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Surface as SurfaceColor
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import org.jetbrains.compose.resources.stringResource

/*
 * The machine setup of an exercise (issue #67): how the lifter sets the seat, the backrest or the pins, typed once and
 * shown above the sets in every workout with the exercise.
 */

/** The setup as chips beside the tune icon; tapping it edits the setup (no [onClick]: a preview that does nothing). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseSetupCard(note: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val chips = SetupNote.chips(note)
    val description = stringResource(Res.string.exercise_setup_edit_cd, chips.joinToString(", "))
    Surface(
        shape = Shapes.medium,
        color = SurfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
            )
            .clearAndSetSemantics { contentDescription = description; if (onClick != null) role = Role.Button }
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(SecondaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = OnSecondaryContainer, modifier = Modifier.size(20.dp))
            }
            FlowRow(
                modifier = Modifier.weight(1f).padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chips.forEach { SetupChip(it) }
            }
        }
    }
}

@Composable
private fun SetupChip(text: String) {
    Box(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .background(SurfaceContainerHighest, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = OnSurface)
    }
}

/** The faint "+ Setup" chip of an exercise that has none yet. */
@Composable
fun ExerciseSetupAddChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(Res.string.exercise_setup_add_cd)
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, OnSurfaceVariant.copy(alpha = 0.45f)),
        modifier = modifier.semantics { contentDescription = description }
    ) {
        Row(
            modifier = Modifier.height(40.dp).padding(start = 12.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(Res.string.exercise_setup_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant
            )
        }
    }
}

/** The setup on one line, under the name of the collapsed workout header. */
@Composable
fun ExerciseSetupLine(note: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val line = SetupNote.oneLine(note)
    val description = stringResource(Res.string.exercise_setup_edit_cd, SetupNote.chips(note).joinToString(", "))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description; role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.Tune, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Edits the setup of [exerciseName]: a text field with a live preview of the chips. Saving hands back what was typed
 * (the repository cleans it), Remove hands back null; dismissing discards the edit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSetupSheet(
    exerciseName: String,
    initial: String?,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    // The cursor starts at the end, ready to add an item
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(initial.orEmpty().let { TextFieldValue(it, TextRange(it.length)) })
    }
    val text = field.text
    val focus = remember { FocusRequester() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceColor,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        SheetFormLayout(modifier = Modifier.imePadding()) {
            SheetFormBody(horizontalPadding = 24.dp, spacing = 20.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.exercise_setup_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = stringResource(Res.string.exercise_setup_subtitle, exerciseName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = field,
                    onValueChange = { field = if (it.text.length > SetupNote.MAX_LENGTH) it.copy(text = it.text.take(SetupNote.MAX_LENGTH)) else it },
                    label = { Text(stringResource(Res.string.exercise_setup_field)) },
                    supportingText = { Text(stringResource(Res.string.exercise_setup_hint)) },
                    minLines = 3,
                    maxLines = 8,
                    shape = Shapes.medium,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
                LaunchedEffect(Unit) { focus.requestFocus() }
                if (SetupNote.chips(text).isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(Res.string.exercise_setup_preview).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariant
                        )
                        ExerciseSetupCard(note = text, onClick = null)
                    }
                }
            }
            SheetFormFooter(horizontalPadding = 24.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!initial.isNullOrBlank()) {
                        TextButton(onClick = { onSave(null) }) {
                            Text(stringResource(Res.string.exercise_setup_clear).uppercase(), color = Error, fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        Spacer(Modifier.size(0.dp))
                    }
                    GymButton(onClick = { onSave(text) }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
