package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.exercise_filters_heading
import com.emanuel5014.trainable.sharedui.resources.exercise_traits_muscles_count
import com.emanuel5014.trainable.sharedui.resources.filter_whole_zone
import com.emanuel5014.trainable.sharedui.resources.filter_show_n_exercises
import org.jetbrains.compose.resources.pluralStringResource
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.domain.exercise.MuscleZone
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.InputChip
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import com.emanuel5014.trainable.sharedui.resources.exercise_filters_active_count
import com.emanuel5014.trainable.sharedui.resources.exercise_filters_reset
import com.emanuel5014.trainable.sharedui.resources.exercise_filters_title
import com.emanuel5014.trainable.sharedui.resources.exercise_traits_equipment
import com.emanuel5014.trainable.sharedui.resources.exercise_traits_muscles
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
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
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.emanuel5014.trainable.domain.exercise.Equipment
import com.emanuel5014.trainable.domain.exercise.Muscle
import com.emanuel5014.trainable.domain.exercise.TraitFilter
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.util.uppercaseUi

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
    languageCode: String,
    modifier: Modifier = Modifier
) {
    var sheetOpen by remember { mutableStateOf(false) }
    // The filter sits inside the search capsule, on a disc of its own so it reads as a button
    ExerciseSearchField(
        query = query,
        onQueryChange = onQueryChange,
        modifier = modifier,
        trailing = {
            ExerciseTraitFilterButton(
                filter = traitFilter,
                onClick = { sheetOpen = true },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceContainerHighest)
            )
        }
    )
    if (sheetOpen) {
        ExerciseTraitFilterSheet(
            filter = traitFilter,
            resultCount = resultCount,
            languageCode = languageCode,
            onFilterChange = onTraitFilterChange,
            onDismiss = { sheetOpen = false }
        )
    }
}

/** The filter button: a badge counts the choices that are on, and shows nothing while none is. */
@Composable
fun ExerciseTraitFilterButton(filter: TraitFilter, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(Res.string.exercise_filters_title)
    val description =
        if (filter.isActive) "$title, ${stringResource(Res.string.exercise_filters_active_count, filter.activeCount)}" else title
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
 * and both groups must be satisfied ([TraitFilter]). The muscles come by area of the body ([MuscleZone]), each area a
 * card of the joined group with its category's shape and a button that takes the whole area; what is on shows on top
 * as chips that take it off. "Reset" clears it; the button at the bottom says how many exercises are left
 * ([resultCount] follows the choices as they change) and closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExerciseTraitFilterSheet(
    filter: TraitFilter,
    resultCount: Int,
    languageCode: String,
    onFilterChange: (TraitFilter) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        contentWindowInsets = PickerSheetInsets,
        dragHandle = { PickerDragHandle() }
    ) {
        val bottomInset = pickerBottomInset()
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ResponsiveSize.cardPadding)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.weight(1f)) {
                        ExercisePickerHeader(
                            eyebrow = stringResource(Res.string.exercise_filters_title),
                            title = stringResource(Res.string.exercise_filters_heading)
                        )
                    }
                    TextButton(onClick = { onFilterChange(TraitFilter.NONE) }, enabled = filter.isActive) {
                        Text(stringResource(Res.string.exercise_filters_reset).uppercaseUi(), fontWeight = FontWeight.ExtraBold)
                    }
                }

                // What is on, each one a tap away from going off
                if (filter.isActive) {
                    FlowRow(
                        modifier = Modifier.padding(top = Spacing.medium),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)
                    ) {
                        Muscle.entries.filter { it in filter.muscles }.forEach { muscle ->
                            ActiveTraitChip(stringResource(muscle.labelText())) { onFilterChange(filter.toggle(muscle)) }
                        }
                        Equipment.entries.filter { it in filter.equipment }.forEach { equipment ->
                            ActiveTraitChip(stringResource(equipment.labelText())) { onFilterChange(filter.toggle(equipment)) }
                        }
                    }
                }

                ExercisePickerSectionLabel(stringResource(Res.string.exercise_traits_muscles))
                Column(verticalArrangement = Arrangement.spacedBy(PickerRowGap)) {
                    val zones = MuscleZone.entries
                    zones.forEachIndexed { index, zone ->
                        MuscleZoneCard(
                            zone = zone,
                            filter = filter,
                            languageCode = languageCode,
                            shape = pickerRowShape(index, zones.size),
                            onFilterChange = onFilterChange
                        )
                    }
                }

                ExercisePickerSectionLabel(stringResource(Res.string.exercise_traits_equipment))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Equipment.entries.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEach { equipment ->
                                ToggleButton(
                                    checked = equipment in filter.equipment,
                                    onCheckedChange = { onFilterChange(filter.toggle(equipment)) },
                                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                                    colors = traitToggleColors()
                                ) {
                                    Text(
                                        stringResource(equipment.labelText()),
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.medium))
            }

            // Always in reach: how many exercises are left, and the way back to them
            GymButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ResponsiveSize.cardPadding)
                    .padding(top = Spacing.small, bottom = ResponsiveSize.cardPadding + bottomInset)
            ) {
                Text(
                    pluralStringResource(Res.plurals.filter_show_n_exercises, resultCount, resultCount).uppercaseUi(),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/** One area of the body: its shape, name and how many of its muscles are on, a button for all of them, then each one. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MuscleZoneCard(
    zone: MuscleZone,
    filter: TraitFilter,
    languageCode: String,
    shape: Shape,
    onFilterChange: (TraitFilter) -> Unit
) {
    val chosen = zone.muscles.count { it in filter.muscles }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceContainerHigh)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(categoryShape(zone.categoryKey))
                    .background(if (chosen > 0) Primary else Primary.copy(alpha = 0.22f))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ExerciseTranslations.translateCategory(zone.categoryKey, languageCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = OnSurface
                )
                if (chosen > 0) {
                    Text(
                        text = stringResource(Res.string.exercise_traits_muscles_count, chosen, zone.muscles.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
            ToggleButton(
                checked = filter.hasWhole(zone),
                onCheckedChange = { onFilterChange(filter.toggle(zone)) },
                colors = traitToggleColors(),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Text(stringResource(Res.string.filter_whole_zone), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            zone.muscles.forEach { muscle ->
                ToggleButton(
                    checked = muscle in filter.muscles,
                    onCheckedChange = { onFilterChange(filter.toggle(muscle)) },
                    colors = traitToggleColors()
                ) {
                    Text(stringResource(muscle.labelText()), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

/** Off: a step lighter than the card it sits on; on: the brand colour. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun traitToggleColors() = ToggleButtonDefaults.toggleButtonColors(
    containerColor = SurfaceContainerHighest,
    contentColor = OnSurface,
    checkedContainerColor = Primary,
    checkedContentColor = OnPrimary
)

/** A choice that is on, shown above the lists; a tap takes it off. */
@Composable
private fun ActiveTraitChip(label: String, onRemove: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label, fontWeight = FontWeight.ExtraBold) },
        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp)) }
    )
}
