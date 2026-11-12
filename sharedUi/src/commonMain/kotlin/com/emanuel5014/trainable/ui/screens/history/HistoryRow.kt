package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.isArchived
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.platform.formatLocalDecimals
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.import_history_workouts_count
import com.emanuel5014.trainable.sharedui.resources.routine_sets_count
import com.emanuel5014.trainable.sharedui.resources.week_short
import com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.components.journal.JournalSummaryLine
import com.emanuel5014.trainable.ui.components.journal.journalHasContent
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.SurfaceContainerLow
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.domain.exercise.CardioNames
import com.emanuel5014.trainable.ui.util.fullDate
import com.emanuel5014.trainable.ui.util.monthName
import com.emanuel5014.trainable.ui.util.shortDayName
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/**
 * One workout of the history list, joined to its month's other workouts by [shape]: a date tile on the left, then
 * the name (with the cardio icon, week and archived pills), the numbers, and the journal or the muscles.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HistoryRow(
    sessionDetails: SessionWithDetails,
    planName: String,
    summary: HistoryRowSummary,
    shape: Shape,
    weightUnit: String,
    languageCode: String,
    exercises: Map<Int, ExerciseEntity>,
    showJournal: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTagClick: (Int) -> Unit
) {
    val session = sessionDetails.session
    val advancedProgramming = LocalAdvancedProgramming.current
    val date = remember(session.timestamp) {
        Instant.fromEpochMilliseconds(session.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    }
    val dayName = remember(date.dayOfWeek, languageCode) { shortDayName(date.dayOfWeek) }
    val dateDescription = remember(session.timestamp, languageCode) { fullDate(session.timestamp) }
    val cardioCategory = remember(sessionDetails.cardio, exercises) {
        // The name of the exercise the first cardio row is linked to (it only picks the icon)
        sessionDetails.cardio.minByOrNull { it.ordineEsercizio }?.let { CardioNames.of(it, exercises, "en") }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) Primary.copy(alpha = 0.1f).compositeOver(SurfaceContainerLow) else SurfaceContainerLow)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date tile: day number over the short weekday
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainerHighest)
                    .clearAndSetSemantics { contentDescription = dateDescription },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = date.day.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface,
                    maxLines = 1
                )
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    maxLines = 1
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Line 1: cardio icon, name, week pill, archived pill
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (summary.isCardioOnly && cardioCategory != null) {
                        Icon(
                            imageVector = cardioIcon(cardioCategory),
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = SessionTitle.of(planName).displayText(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val programWeek = session.programWeek
                    if (advancedProgramming && programWeek != null) {
                        PrescriptionPill(
                            text = stringResource(Res.string.week_short, programWeek),
                            containerColor = Primary.copy(alpha = 0.12f),
                            contentColor = Primary,
                            emphasized = true
                        )
                    }
                    if (sessionDetails.plan.isArchived) {
                        ArchivedRoutineBadge()
                    }
                }

                // Line 2: the numbers
                val stats = buildList {
                    summary.durationMs?.let { add(formatSessionDuration(it)) }
                    if (summary.isCardioOnly) {
                        if (summary.cardioKm > 0f) add(formatLocalDecimals(summary.cardioKm.toDouble(), 1) + " km")
                    } else {
                        if (summary.sets > 0) add(pluralStringResource(Res.plurals.routine_sets_count, summary.sets, summary.sets))
                        if (summary.volumeKg > 0f) add(formatVolume(summary.volumeKg, weightUnit))
                    }
                }
                if (stats.isNotEmpty()) {
                    Text(
                        text = stats.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Line 3: the journal, else the muscles
                if (showJournal && journalHasContent(session.rating, sessionDetails.tags, session.journalNote)) {
                    JournalSummaryLine(
                        rating = session.rating,
                        tags = sessionDetails.tags,
                        note = session.journalNote,
                        onTagClick = onTagClick,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else if (summary.muscles.isNotEmpty()) {
                    Text(
                        text = summary.muscles.joinToString(" · ") { ExerciseTranslations.translateCategory(it, languageCode) },
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Primary else Color.Transparent, CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) Primary else OnSurfaceVariant.copy(alpha = 0.6f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/** The head of a month: its name (with the year when not this year) and, on the right, its workouts and volume. */
@Composable
internal fun HistoryMonthHeader(month: HistoryMonth, weightUnit: String, currentYear: Int, languageCode: String) {
    val name = remember(month.month, languageCode) { monthName(month.month) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() }
            .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name + if (month.year != currentYear) " ${month.year}" else "",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        val count = month.sessions.size
        Text(
            text = pluralStringResource(Res.plurals.import_history_workouts_count, count, count) +
                if (month.volumeKg > 0f) " · " + formatVolume(month.volumeKg, weightUnit) else "",
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceVariant,
            maxLines = 1
        )
    }
}
