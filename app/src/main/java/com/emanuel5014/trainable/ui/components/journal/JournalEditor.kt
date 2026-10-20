package com.emanuel5014.trainable.ui.components.journal

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.relation.TagWithUsage
import com.emanuel5014.trainable.domain.journal.SessionJournal
import com.emanuel5014.trainable.domain.journal.TagName
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/** Keeps a journal being edited across rotation and process death. */
@Suppress("UNCHECKED_CAST")
val SessionJournalSaver = listSaver<SessionJournal, Any>(
    save = { listOf(it.rating ?: 0, it.note.orEmpty(), it.tagIds.toList()) },
    restore = {
        SessionJournal(
            rating = (it[0] as Int).takeIf { rating -> rating > 0 },
            note = (it[1] as String).ifEmpty { null },
            tagIds = (it[2] as List<Int>).toSet()
        )
    }
)

/**
 * The journal of a workout: stars, tags, note. It holds no state of its own beyond what is being typed: every change
 * goes out through [onJournalChange], so the finish sheet and the later edit sheet share it.
 * [onCreateTag] makes (or finds) a tag from the typed name and calls back with its id, which is then selected.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalEditor(
    journal: SessionJournal,
    onJournalChange: (SessionJournal) -> Unit,
    tags: List<TagWithUsage>,
    ratingEnabled: Boolean,
    onCreateTag: (name: String, onCreated: (Int) -> Unit) -> Unit,
    hapticEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    // A tag is created asynchronously: by the time it comes back the journal may have changed again
    val currentJournal by rememberUpdatedState(journal)
    val currentOnChange by rememberUpdatedState(onJournalChange)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (ratingEnabled) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RatingStars(
                    rating = journal.rating,
                    onRatingChange = { onJournalChange(journal.copy(rating = it)) },
                    hapticEnabled = hapticEnabled
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.journal_tags),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tags.forEach { tag ->
                    TagChip(
                        name = tag.name,
                        colorIndex = tag.colorIndex,
                        customColor = tag.customColor,
                        selected = tag.id in journal.tagIds,
                        onClick = {
                            val ids = journal.tagIds
                            onJournalChange(journal.copy(tagIds = if (tag.id in ids) ids - tag.id else ids + tag.id))
                        }
                    )
                }
                NewTagChip(
                    onCreate = { name ->
                        onCreateTag(name) { id ->
                            currentOnChange(currentJournal.copy(tagIds = currentJournal.tagIds + id))
                        }
                    }
                )
            }
        }

        NoteSection(
            note = journal.note.orEmpty(),
            onNoteChange = { onJournalChange(journal.copy(note = it.take(SessionJournal.MAX_NOTE))) }
        )
    }
}

/** "+ New tag": a chip that turns into a one-line field; Done (or leaving it empty) puts it away again. */
@Composable
private fun NewTagChip(onCreate: (String) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    if (!adding) {
        AssistChip(
            onClick = { adding = true },
            label = { Text(stringResource(R.string.journal_new_tag)) },
            leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        return
    }

    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    fun finish() {
        if (text.isNotBlank()) onCreate(text)
        text = ""
        adding = false
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(SurfaceContainerHigh)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it.replace("|", "").take(TagName.MAX_LENGTH) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
            cursorBrush = SolidColor(Primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { finish() }),
            modifier = Modifier.widthIn(min = 110.dp, max = 220.dp).focusRequester(focus),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.journal_tag_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    field()
                }
            }
        )
        IconButton(onClick = { text = ""; adding = false }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cancel), tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

/** The note: a quiet "Add a note…" row until it is wanted, then a multi-line field that grows in with a spring. */
@Composable
private fun NoteSection(note: String, onNoteChange: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(note.isNotEmpty()) }
    var focusOnOpen by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))) {
        if (!open) {
            Surface(
                onClick = { open = true; focusOnOpen = true },
                shape = Shapes.medium,
                color = SurfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, tint = Primary)
                    Text(
                        text = stringResource(R.string.journal_add_note),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariant
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.journal_note)) },
                minLines = 3,
                maxLines = 8,
                shape = Shapes.medium,
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
            if (focusOnOpen) {
                LaunchedEffect(Unit) {
                    focus.requestFocus()
                    focusOnOpen = false
                }
            }
        }
    }
}
