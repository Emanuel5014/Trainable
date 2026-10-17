package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.relation.TagWithUsage
import com.emanuel5014.trainable.domain.journal.RatingBucket
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.util.DateFormatter

private class ActiveFilter(val key: String, val label: String, val withoutIt: HistoryFilters)

/**
 * One removable chip per active filter, above the list, so what narrows the history is always in sight and a tap away
 * from being undone. Shows nothing when no filter is active. [planName] names the routine filter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveFiltersBar(
    filters: HistoryFilters,
    planName: String?,
    tags: List<TagWithUsage>,
    journalEnabled: Boolean,
    onFiltersChange: (HistoryFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!filters.isActive(journalEnabled)) return

    val bucketLabels = mapOf(
        RatingBucket.LOW to stringResource(R.string.journal_bucket_low),
        RatingBucket.MID to stringResource(R.string.journal_bucket_mid),
        RatingBucket.HIGH to stringResource(R.string.journal_bucket_high),
        RatingBucket.UNRATED to stringResource(R.string.journal_not_rated)
    )
    val withNoteLabel = stringResource(R.string.journal_filter_with_note)

    val active = buildList {
        if (filters.planId != null && planName != null) {
            add(ActiveFilter("plan", planName, filters.copy(planId = null)))
        }
        if (filters.startDate != null || filters.endDate != null) {
            val label = listOfNotNull(filters.startDate, filters.endDate).joinToString(" – ") { DateFormatter.formatShort(it) }
            add(ActiveFilter("dates", label, filters.copy(startDate = null, endDate = null)))
        }
        if (journalEnabled) {
            val journal = filters.journal
            tags.filter { it.id in journal.tagIds }.forEach { tag ->
                add(ActiveFilter("tag_${tag.id}", tag.name, filters.copy(journal = journal.copy(tagIds = journal.tagIds - tag.id))))
            }
            journal.ratingBuckets.sortedBy { it.ordinal }.forEach { bucket ->
                add(
                    ActiveFilter(
                        "bucket_${bucket.name}",
                        bucketLabels.getValue(bucket),
                        filters.copy(journal = journal.copy(ratingBuckets = journal.ratingBuckets - bucket))
                    )
                )
            }
            if (journal.onlyWithNote) {
                add(ActiveFilter("note", withNoteLabel, filters.copy(journal = journal.copy(onlyWithNote = false))))
            }
        }
    }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = ResponsiveSize.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(active, key = { it.key }) { filter ->
            InputChip(
                selected = true,
                onClick = { onFiltersChange(filter.withoutIt) },
                label = { Text(filter.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.journal_filter_remove_cd, filter.label),
                        modifier = Modifier.size(InputChipDefaults.IconSize)
                    )
                },
                colors = InputChipDefaults.inputChipColors(
                    selectedContainerColor = Primary.copy(alpha = 0.15f),
                    selectedLabelColor = Primary,
                    selectedTrailingIconColor = Primary,
                    labelColor = OnSurfaceVariant
                )
            )
        }
    }
}
