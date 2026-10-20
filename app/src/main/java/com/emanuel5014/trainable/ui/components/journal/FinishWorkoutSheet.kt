package com.emanuel5014.trainable.ui.components.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.journal.SessionJournal
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.SheetFormBody
import com.emanuel5014.trainable.ui.components.SheetFormFooter
import com.emanuel5014.trainable.ui.components.SheetFormLayout
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface

/**
 * Finishing a workout with the journal on: the question of the plain dialog and, under it, "How did it go?" with
 * stars, tags and a note. Everything in it is optional: FINISH is always enabled and finishing with nothing touched
 * is one tap, as it always was. What is typed lives in the caller ([journal], [onJournalChange]) so cancelling keeps it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishWorkoutSheet(
    title: String,
    message: String,
    journal: SessionJournal,
    onJournalChange: (SessionJournal) -> Unit,
    hapticEnabled: Boolean,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsState()
    val ratingEnabled by viewModel.ratingEnabled.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        SheetFormLayout(modifier = Modifier.imePadding()) {
            SheetFormBody(horizontalPadding = 24.dp, spacing = 20.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.journal_how_did_it_go),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
                JournalEditor(
                    journal = journal,
                    onJournalChange = onJournalChange,
                    tags = tags,
                    ratingEnabled = ratingEnabled,
                    onCreateTag = viewModel::createTag,
                    hapticEnabled = hapticEnabled
                )
            }
            SheetFormFooter(horizontalPadding = 24.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    GymButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.finish_confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancel).uppercase(), color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
