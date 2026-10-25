package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.exercise.Equipment
import com.emanuel5014.trainable.domain.exercise.Muscle
import com.emanuel5014.trainable.domain.exercise.TraitFilter
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface

/**
 * The search field of an exercise list with the button of the muscle and equipment filter beside it; the sheet opens from the
 * button. [resultCount] is how many exercises the list shows with the current filter.
 */
@Composable
fun ExerciseSearchWithTraitFilter(
    query: String,
    onQueryChange: (String) -> Unit,
    traitFilter: TraitFilter,
    onTraitFilterChange: (TraitFilter) -> Unit,
    resultCount: Int,
    modifier: Modifier = Modifier
) {
    var sheetOpen by remember { mutableStateOf(false) }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ExerciseSearchField(query = query, onQueryChange = onQueryChange, modifier = Modifier.weight(1f))
        ExerciseTraitFilterButton(filter = traitFilter, onClick = { sheetOpen = true })
    }
    if (sheetOpen) {
        ExerciseTraitFilterSheet(
            filter = traitFilter,
            resultCount = resultCount,
            onFilterChange = onTraitFilterChange,
            onDismiss = { sheetOpen = false }
        )
    }
}

/** The filter button: a badge counts the choices that are on, and shows nothing while none is. */
@Composable
fun ExerciseTraitFilterButton(filter: TraitFilter, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(R.string.exercise_filters_title)
    val description =
        if (filter.isActive) "$title, ${stringResource(R.string.exercise_filters_active_count, filter.activeCount)}" else title
    IconButton(onClick = onClick, modifier = modifier) {
        BadgedBox(badge = { if (filter.isActive) Badge { Text(filter.activeCount.toString()) } }) {
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = description,
                tint = if (filter.isActive) Primary else OnSurfaceVariant
            )
        }
    }
}

/**
 * The sheet to narrow an exercise list down by muscle and equipment: any chosen muscle passes, any chosen equipment passes,
 * and both groups must be satisfied ([TraitFilter]). "Reset" clears it; [resultCount] follows the choices as they change.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseTraitFilterSheet(
    filter: TraitFilter,
    resultCount: Int,
    onFilterChange: (TraitFilter) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.large)
                .padding(bottom = Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.exercise_filters_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface
                )
                TextButton(onClick = { onFilterChange(TraitFilter.NONE) }, enabled = filter.isActive) {
                    Text(stringResource(R.string.exercise_filters_reset).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            }
            Text(
                text = stringResource(R.string.exercise_traits_muscles),
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)
            ) {
                Muscle.entries.forEach { muscle ->
                    FilterChip(
                        selected = muscle in filter.muscles,
                        onClick = { onFilterChange(filter.toggle(muscle)) },
                        label = { Text(stringResource(muscle.labelRes())) }
                    )
                }
            }
            Text(
                text = stringResource(R.string.exercise_traits_equipment),
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)
            ) {
                Equipment.entries.forEach { equipment ->
                    FilterChip(
                        selected = equipment in filter.equipment,
                        onClick = { onFilterChange(filter.toggle(equipment)) },
                        label = { Text(stringResource(equipment.labelRes())) }
                    )
                }
            }
            Text(
                text = stringResource(R.string.exercise_filters_results, resultCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Primary
            )
        }
    }
}
