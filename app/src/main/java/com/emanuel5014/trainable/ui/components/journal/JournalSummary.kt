package com.emanuel5014.trainable.ui.components.journal
import com.emanuel5014.trainable.ui.components.glass.glassPanelBackground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import com.emanuel5014.trainable.ui.components.glass.GlassIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.SessionTagEntity
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/** Tags shown on a collapsed card before they fold into a "+N". */
private const val VISIBLE_TAGS = 2

/** Whether a workout has anything in its journal to show. */
fun journalHasContent(rating: Int?, tags: List<SessionTagEntity>, note: String?): Boolean =
    rating != null || tags.isNotEmpty() || !note.isNullOrBlank()

/**
 * The journal of a workout on a collapsed history card: the rating, up to two tags (then "+N") and the first line of
 * the note. It draws nothing when there is nothing to show, so workouts without a journal look as they always did.
 * Tapping a tag hands its id to [onTagClick].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalSummaryLine(
    rating: Int?,
    tags: List<SessionTagEntity>,
    note: String?,
    onTagClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!journalHasContent(rating, tags, note)) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (rating != null || tags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (rating != null) RatingPill(rating)
                tags.take(VISIBLE_TAGS).forEach { tag ->
                    TagBadge(
                        name = tag.name,
                        colorIndex = tag.colorIndex,
                        customColor = tag.customColor,
                        modifier = Modifier.clip(CircleShape).clickable { onTagClick(tag.id) }
                    )
                }
                if (tags.size > VISIBLE_TAGS) TagBadge(name = "+${tags.size - VISIBLE_TAGS}", colorIndex = 4, customColor = null)
            }
        }
        val firstLine = note?.lineSequence()?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }
        if (firstLine != null) {
            Text(
                text = firstLine,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * The whole journal of a workout on an expanded history card: stars, every tag, the full note, and a pencil that opens
 * the journal sheet (with nothing in the journal yet, a prompt to start one).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalDetail(
    rating: Int?,
    tags: List<SessionTagEntity>,
    note: String?,
    onTagClick: (Int) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.medium)
            .glassPanelBackground(SurfaceContainerHigh, Shapes.medium)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!journalHasContent(rating, tags, note)) {
                Text(
                    text = stringResource(R.string.journal_row_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
            if (rating != null) RatingStars(rating = rating, onRatingChange = {}, readOnly = true)
            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        TagBadge(
                            name = tag.name,
                            colorIndex = tag.colorIndex,
                            customColor = tag.customColor,
                            modifier = Modifier.clip(CircleShape).clickable { onTagClick(tag.id) }
                        )
                    }
                }
            }
            if (!note.isNullOrBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = stringResource(R.string.journal_edit),
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
