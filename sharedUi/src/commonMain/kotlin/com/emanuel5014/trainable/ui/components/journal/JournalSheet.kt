package com.emanuel5014.trainable.ui.components.journal

import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.journal_row_title
import com.emanuel5014.trainable.sharedui.resources.save
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.domain.journal.SessionJournal
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.SheetFormBody
import com.emanuel5014.trainable.ui.components.SheetFormFooter
import com.emanuel5014.trainable.ui.components.SheetFormLayout
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.Surface

/** Edits the journal of an existing workout. Saving hands back the cleaned journal; dismissing discards the edits. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalSheet(
    initial: SessionJournal,
    onSave: (SessionJournal) -> Unit,
    onDismiss: () -> Unit,
    hapticEnabled: Boolean
) {
    val data = LocalJournalData.current()
    val tags = data.tags
    val ratingEnabled = data.ratingEnabled
    var journal by rememberSaveable(stateSaver = SessionJournalSaver) { mutableStateOf(initial) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        SheetFormLayout(modifier = Modifier.imePadding()) {
            SheetFormBody(horizontalPadding = 24.dp, spacing = 20.dp) {
                Text(
                    text = stringResource(Res.string.journal_row_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                JournalEditor(
                    journal = journal,
                    onJournalChange = { journal = it },
                    tags = tags,
                    ratingEnabled = ratingEnabled,
                    onCreateTag = data::createTag,
                    hapticEnabled = hapticEnabled
                )
            }
            SheetFormFooter(horizontalPadding = 24.dp) {
                GymButton(onClick = { onSave(journal.cleaned()) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
