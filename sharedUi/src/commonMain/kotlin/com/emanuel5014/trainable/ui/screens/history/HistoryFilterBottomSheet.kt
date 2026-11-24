package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import com.emanuel5014.trainable.sharedui.resources.csv_export_title
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.local.entity.RoutineGroupEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.isArchived
import com.emanuel5014.trainable.data.local.relation.TagWithUsage
import com.emanuel5014.trainable.domain.journal.JournalFilters
import com.emanuel5014.trainable.domain.journal.RatingBucket
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.active_routines
import com.emanuel5014.trainable.sharedui.resources.archived_routines_header
import com.emanuel5014.trainable.sharedui.resources.clear_date
import com.emanuel5014.trainable.sharedui.resources.filters
import com.emanuel5014.trainable.sharedui.resources.history_filter_any_time
import com.emanuel5014.trainable.sharedui.resources.history_filter_count
import com.emanuel5014.trainable.sharedui.resources.history_filter_custom_range
import com.emanuel5014.trainable.sharedui.resources.history_filter_journal
import com.emanuel5014.trainable.sharedui.resources.history_filter_last_30_days
import com.emanuel5014.trainable.sharedui.resources.history_filter_next_year
import com.emanuel5014.trainable.sharedui.resources.history_filter_none_shown
import com.emanuel5014.trainable.sharedui.resources.history_filter_period
import com.emanuel5014.trainable.sharedui.resources.history_filter_previous_year
import com.emanuel5014.trainable.sharedui.resources.history_filter_reset
import com.emanuel5014.trainable.sharedui.resources.history_filter_routine
import com.emanuel5014.trainable.sharedui.resources.history_filter_show
import com.emanuel5014.trainable.sharedui.resources.history_filter_this_year
import com.emanuel5014.trainable.sharedui.resources.history_filter_whole_group
import com.emanuel5014.trainable.sharedui.resources.journal_bucket_high
import com.emanuel5014.trainable.sharedui.resources.journal_bucket_low
import com.emanuel5014.trainable.sharedui.resources.journal_bucket_mid
import com.emanuel5014.trainable.sharedui.resources.journal_filter_rating
import com.emanuel5014.trainable.sharedui.resources.journal_filter_with_note
import com.emanuel5014.trainable.sharedui.resources.journal_match_all
import com.emanuel5014.trainable.sharedui.resources.journal_match_any
import com.emanuel5014.trainable.sharedui.resources.journal_not_rated
import com.emanuel5014.trainable.sharedui.resources.journal_tags
import com.emanuel5014.trainable.sharedui.resources.no_results_filters
import com.emanuel5014.trainable.sharedui.resources.no_routine
import com.emanuel5014.trainable.sharedui.resources.routine_group_none
import com.emanuel5014.trainable.sharedui.resources.routine_group_routines
import com.emanuel5014.trainable.sharedui.resources.search_routines
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.SheetFormFooter
import com.emanuel5014.trainable.ui.components.journal.TagChip
import com.emanuel5014.trainable.ui.screens.routines.GroupSwatch
import com.emanuel5014.trainable.ui.screens.routines.formatMonthRange
import com.emanuel5014.trainable.ui.screens.routines.routineGroupColors
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.util.DateFormatter
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

/** From this many routines the picker gets a search field; below it the folded groups keep the list short. */
private const val SEARCH_MIN_ROUTINES = 20

/**
 * The history's filters on one page, in three parts: the routine (a whole group or one routine of it, or the workouts of
 * no routine), the period (a shortcut, a month of any year or a range from the calendar) and, with the journal on, tags,
 * rating and note. Every change applies at once; each choice shows how many workouts it leaves, and the button at the
 * bottom how many the list shows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterBottomSheet(
    filters: HistoryFilters,
    counts: HistoryFilterCounts,
    availablePlans: List<WorkoutPlanEntity>,
    routineGroups: List<RoutineGroupEntity>,
    hasUnassignedSessions: Boolean,
    onFiltersChanged: (HistoryFilters) -> Unit,
    onCustomRangeClick: () -> Unit,
    onDismiss: () -> Unit,
    /** The workout journal is on: the tag, rating and note filters show. */
    journalEnabled: Boolean = false,
    ratingEnabled: Boolean = true,
    availableTags: List<TagWithUsage> = emptyList(),
    /** Saves the workouts the filters show as a CSV; null leaves the button out. */
    onExportCsv: (() -> Unit)? = null,
    /** A CSV is being written: the button waits. */
    exportingCsv: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(OnSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp)
            ) {
                item(key = "header") {
                    FilterHeader(
                        shown = counts.shown,
                        total = counts.total,
                        canReset = filters.isActive(journalEnabled),
                        onReset = { onFiltersChanged(HistoryFilters()) }
                    )
                }
                item(key = "routines") {
                    RoutineSection(
                        filters = filters,
                        counts = counts,
                        availablePlans = availablePlans,
                        routineGroups = routineGroups,
                        hasUnassignedSessions = hasUnassignedSessions,
                        onFiltersChanged = onFiltersChanged
                    )
                }
                item(key = "period") {
                    PeriodSection(
                        filters = filters,
                        counts = counts,
                        onFiltersChanged = onFiltersChanged,
                        onCustomRangeClick = onCustomRangeClick
                    )
                }
                if (journalEnabled) {
                    item(key = "journal") {
                        JournalSection(
                            journalFilters = filters.journal,
                            ratingEnabled = ratingEnabled,
                            availableTags = availableTags,
                            onJournalFiltersChanged = { onFiltersChanged(filters.copy(journal = it)) }
                        )
                    }
                }
                item(key = "end") { Spacer(modifier = Modifier.height(16.dp)) }
            }

            SheetFormFooter(horizontalPadding = 20.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onExportCsv != null) {
                        // Saves what the list shows, so it waits while the filters leave nothing
                        val exportLabel = stringResource(Res.string.csv_export_title)
                        GymButton(
                            onClick = onExportCsv,
                            enabled = counts.shown > 0 && !exportingCsv,
                            containerColor = SurfaceContainerHigh,
                            contentColor = OnSurface,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .width(if (ResponsiveSize.isCompact) 56.dp else 64.dp)
                                .semantics { contentDescription = exportLabel }
                        ) {
                            if (exportingCsv) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
                            else Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = Primary)
                        }
                    }
                    GymButton(onClick = ::close, modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (counts.shown == 0) stringResource(Res.string.history_filter_none_shown).uppercase()
                            else pluralStringResource(Res.plurals.history_filter_show, counts.shown, counts.shown).uppercase(),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterHeader(shown: Int, total: Int, canReset: Boolean, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.filters),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = OnSurface
            )
            Text(
                text = pluralStringResource(Res.plurals.history_filter_count, total, shown, total),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
        }
        if (canReset) {
            TextButton(onClick = onReset) {
                Text(stringResource(Res.string.history_filter_reset), color = Error, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black,
        color = OnSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp)
    )
}

/** Corners of a row in a joined list: round at its ends, tight where rows touch. */
internal fun joinedShape(index: Int, count: Int): RoundedCornerShape {
    val top = if (index == 0) 24.dp else 8.dp
    val bottom = if (index == count - 1) 24.dp else 8.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

// --- Routine -----------------------------------------------------------------------------------------------------

/**
 * What the routine picker has picked and how a tap changes it: one routine, one group or "no routine" in the history's
 * filters, any mix of them in the CSV export.
 */
internal interface RoutinePickerChoice {
    fun groupPicked(groupId: Int): Boolean
    fun routinePicked(planId: Int): Boolean
    val noRoutinePicked: Boolean
    fun toggleGroup(groupId: Int)
    fun toggleRoutine(planId: Int)
    fun toggleNoRoutine()
}

/** The history filters' choice: one routine, one group or "no routine", and picking one drops the others. */
private class SingleRoutineChoice(
    private val filters: HistoryFilters,
    private val onFiltersChanged: (HistoryFilters) -> Unit
) : RoutinePickerChoice {
    override fun groupPicked(groupId: Int) = filters.groupId == groupId
    override fun routinePicked(planId: Int) = filters.planId == planId
    override val noRoutinePicked get() = filters.planId == NO_ROUTINE_PLAN_ID
    override fun toggleGroup(groupId: Int) =
        onFiltersChanged(filters.anyRoutine().copy(groupId = if (groupPicked(groupId)) null else groupId))
    override fun toggleRoutine(planId: Int) =
        onFiltersChanged(filters.anyRoutine().copy(planId = if (routinePicked(planId)) null else planId))
    override fun toggleNoRoutine() =
        onFiltersChanged(filters.anyRoutine().copy(planId = if (noRoutinePicked) null else NO_ROUTINE_PLAN_ID))
}

/** One row of the routine list: a block's head, its open content, or "no routine". */
private sealed interface RoutineRow {
    data class Head(val block: RoutinePickerBlock, val open: Boolean) : RoutineRow
    data class Content(val block: RoutinePickerBlock, val routines: List<RoutinePickerItem>) : RoutineRow
    data object NoRoutine : RoutineRow
}

@Composable
private fun RoutineSection(
    filters: HistoryFilters,
    counts: HistoryFilterCounts,
    availablePlans: List<WorkoutPlanEntity>,
    routineGroups: List<RoutineGroupEntity>,
    hasUnassignedSessions: Boolean,
    onFiltersChanged: (HistoryFilters) -> Unit
) {
    val blocks = remember(availablePlans, routineGroups, counts) { routinePickerBlocks(availablePlans, routineGroups, counts) }
    val choice = SingleRoutineChoice(filters, onFiltersChanged)
    RoutinePickerSection(
        title = stringResource(Res.string.history_filter_routine),
        blocks = blocks,
        noRoutineWorkouts = if (hasUnassignedSessions || choice.noRoutinePicked) counts.noRoutine else null,
        choice = choice
    )
}

/**
 * The routines in blocks (the groups and the routines in none, or the active and the archived ones), each block folded
 * or open, with a search field from [SEARCH_MIN_ROUTINES] routines; [noRoutineWorkouts] null leaves "no routine" out.
 * [lead] goes between the title and the blocks.
 */
@Composable
internal fun RoutinePickerSection(
    title: String,
    blocks: List<RoutinePickerBlock>,
    noRoutineWorkouts: Int?,
    choice: RoutinePickerChoice,
    lead: (@Composable () -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    // Blocks the lifter opened or closed; the others follow [RoutinePickerBlock.openByDefault] and the selection
    val toggled = remember { mutableStateMapOf<String, Boolean>() }
    val noRoutineLabel = stringResource(Res.string.no_routine)
    val routineCount = blocks.sumOf { it.routines.size }
    if (routineCount == 0 && noRoutineWorkouts == null) return
    val search = if (routineCount >= SEARCH_MIN_ROUTINES) query.trim() else ""

    val rows = buildList {
        blocks.forEach { block ->
            val routines = if (search.isEmpty()) block.routines else block.routines.filter { it.plan.nome.contains(search, ignoreCase = true) }
            if (routines.isEmpty()) return@forEach
            val holdsSelection = block.routines.any { choice.routinePicked(it.plan.id) } || (block.group != null && choice.groupPicked(block.group.id))
            val open = search.isNotEmpty() || (toggled[block.key] ?: (block.openByDefault || holdsSelection))
            add(RoutineRow.Head(block, open))
            if (open) add(RoutineRow.Content(block, routines))
        }
        if (noRoutineWorkouts != null && noRoutineLabel.contains(search, ignoreCase = true)) add(RoutineRow.NoRoutine)
    }

    Column {
        SectionLabel(title)
        lead?.invoke()
        if (routineCount >= SEARCH_MIN_ROUTINES) {
            GymInputField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(Res.string.search_routines),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp)) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                } else null,
                containerColor = SurfaceContainerHigh,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        if (rows.isEmpty()) {
            Text(
                text = stringResource(Res.string.no_results_filters),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            rows.forEachIndexed { index, row ->
                val shape = joinedShape(index, rows.size)
                when (row) {
                    is RoutineRow.Head -> key(row.block.key) {
                        RoutineBlockHead(
                            block = row.block,
                            open = row.open,
                            choice = choice,
                            shape = shape,
                            onToggle = { toggled[row.block.key] = !row.open }
                        )
                    }
                    is RoutineRow.Content -> key(row.block.key + "-content") {
                        RoutineBlockContent(
                            block = row.block,
                            routines = row.routines,
                            choice = choice,
                            shape = shape
                        )
                    }
                    RoutineRow.NoRoutine -> key("no-routine") {
                        NoRoutineRow(
                            label = noRoutineLabel,
                            workouts = noRoutineWorkouts ?: 0,
                            selected = choice.noRoutinePicked,
                            shape = shape,
                            onClick = choice::toggleNoRoutine
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineBlockHead(
    block: RoutinePickerBlock,
    open: Boolean,
    choice: RoutinePickerChoice,
    shape: RoundedCornerShape,
    onToggle: () -> Unit
) {
    val monthNames = remember { MonthCalendar.shortMonthNames() }
    val title = when (block.kind) {
        RoutineBlockKind.Group -> block.group?.name.orEmpty()
        RoutineBlockKind.Active -> stringResource(Res.string.active_routines)
        RoutineBlockKind.Archived -> stringResource(Res.string.archived_routines_header)
        RoutineBlockKind.Loose -> stringResource(Res.string.routine_group_none)
    }
    // What is picked inside the block, in the accent colour; otherwise how many routines and when they were used
    val picked = when {
        block.group != null && choice.groupPicked(block.group.id) -> stringResource(Res.string.history_filter_whole_group)
        else -> block.routines.filter { choice.routinePicked(it.plan.id) }.map { routineLabel(it, monthNames) }
            .takeIf { it.isNotEmpty() }?.joinToString(", ")
    }
    val summary = listOfNotNull(
        pluralStringResource(Res.plurals.routine_group_routines, block.routines.size, block.routines.size),
        block.period?.let { formatMonthRange(it.first, it.second, monthNames) }
    ).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onToggle)
            .heightIn(min = 64.dp)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (block.kind) {
            RoutineBlockKind.Group -> GroupSwatch(block.group?.let { routineGroupColors(it).first }, size = 18)
            RoutineBlockKind.Loose -> GroupSwatch(null, size = 18)
            RoutineBlockKind.Active -> Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
            RoutineBlockKind.Archived -> Icon(Icons.Rounded.Archive, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (block.kind == RoutineBlockKind.Loose) OnSurfaceVariant else OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (block.kind == RoutineBlockKind.Group && block.archived) {
                    Icon(Icons.Rounded.Archive, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = picked ?: summary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (picked != null) FontWeight.Bold else FontWeight.Normal,
                color = if (picked != null) Primary else OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = block.workouts.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant
        )
        Icon(
            if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = OnSurfaceVariant
        )
    }
}

/** The name of a routine, with its months when another routine of the block has the same name. */
private fun routineLabel(item: RoutinePickerItem, monthNames: List<String>): String {
    val period = item.period
    return if (item.showPeriod && period != null) "${item.plan.nome} · ${formatMonthRange(period.first, period.second, monthNames)}"
    else item.plan.nome
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineBlockContent(
    block: RoutinePickerBlock,
    routines: List<RoutinePickerItem>,
    choice: RoutinePickerChoice,
    shape: RoundedCornerShape
) {
    val monthNames = remember { MonthCalendar.shortMonthNames() }
    // No chip moves when one is picked: the counts are of the routines, not of the list
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val group = block.group
        if (group != null) {
            FilterOptionChip(
                label = stringResource(Res.string.history_filter_whole_group),
                count = block.workouts,
                selected = choice.groupPicked(group.id),
                onClick = { choice.toggleGroup(group.id) }
            )
        }
        routines.forEach { item ->
            key(item.plan.id) {
                FilterOptionChip(
                    label = routineLabel(item, monthNames),
                    count = item.workouts,
                    selected = choice.routinePicked(item.plan.id),
                    // A whole archived block needs no mark on each routine
                    leadingIcon = if (item.plan.isArchived && !block.archived) Icons.Rounded.Archive else null,
                    onClick = { choice.toggleRoutine(item.plan.id) }
                )
            }
        }
    }
}

@Composable
private fun NoRoutineRow(label: String, workouts: Int, selected: Boolean, shape: RoundedCornerShape, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Primary.copy(alpha = 0.14f) else SurfaceContainerHigh)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Rounded.LinkOff, contentDescription = null, tint = if (selected) Primary else OnSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Primary else OnSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        Text(text = workouts.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
    }
}

// --- Period ------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodSection(
    filters: HistoryFilters,
    counts: HistoryFilterCounts,
    onFiltersChanged: (HistoryFilters) -> Unit,
    onCustomRangeClick: () -> Unit
) {
    val zone = remember { TimeZone.currentSystemDefault() }
    val today = remember { Clock.System.todayIn(zone) }
    val currentYear = today.year
    val firstYear = minOf(counts.firstYear ?: currentYear, currentYear)
    val start = filters.startDate
    val end = filters.endDate
    val startDate = start?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(zone).date }

    val last30 = remember(today) { lastDaysRange(30, today, zone) }
    val thisYear = remember(currentYear) { yearRange(currentYear, zone) }
    val pickedMonth = if (start != null && end != null && startDate != null &&
        MonthCalendar.isWholeMonth(start, end, startDate.month.ordinal, startDate.year)
    ) startDate.year to startDate.month.ordinal else null
    val isLast30 = start == last30.first && end == last30.second
    val isThisYear = start == thisYear.first && end == thisYear.second
    val isCustom = start != null && pickedMonth == null && !isLast30 && !isThisYear

    var shownYear by remember { mutableStateOf((pickedMonth?.first ?: startDate?.year ?: currentYear).coerceIn(firstYear, currentYear)) }
    val monthNames = remember { MonthCalendar.shortMonthNames() }

    fun pick(range: Pair<Long, Long>?, selected: Boolean) {
        onFiltersChanged(if (range == null || selected) filters.anyDate() else filters.copy(startDate = range.first, endDate = range.second))
    }

    Column {
        SectionLabel(stringResource(Res.string.history_filter_period))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterOptionChip(stringResource(Res.string.history_filter_any_time), null, start == null) { pick(null, false) }
            FilterOptionChip(stringResource(Res.string.history_filter_last_30_days), null, isLast30) { pick(last30, isLast30) }
            FilterOptionChip(stringResource(Res.string.history_filter_this_year), null, isThisYear) { pick(thisYear, isThisYear) }
        }

        // The year whose months the grid shows
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { shownYear-- }, enabled = shownYear > firstYear) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(Res.string.history_filter_previous_year))
            }
            Text(
                text = shownYear.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = OnSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { shownYear++ }, enabled = shownYear < currentYear) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(Res.string.history_filter_next_year))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (rowStart in 0 until 12 step 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (month in rowStart until rowStart + 4) {
                        val selected = pickedMonth == (shownYear to month)
                        MonthCell(
                            name = monthNames.getOrElse(month) { (month + 1).toString() },
                            workouts = counts.byMonth[shownYear to month] ?: 0,
                            selected = selected,
                            modifier = Modifier.weight(1f),
                            onClick = { pick(MonthCalendar.monthRange(month, shownYear), selected) }
                        )
                    }
                }
            }
        }

        CustomRangeRow(
            range = if (isCustom && start != null && end != null) "${DateFormatter.formatShort(start)} – ${DateFormatter.formatShort(end)}" else null,
            onClick = onCustomRangeClick,
            onClear = { onFiltersChanged(filters.anyDate()) }
        )
    }
}

@Composable
private fun MonthCell(name: String, workouts: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    // A month without workouts cannot be picked: it would only empty the list
    val enabled = workouts > 0 || selected
    val content = when {
        selected -> OnPrimary
        enabled -> OnSurface
        else -> OnSurfaceVariant.copy(alpha = 0.45f)
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    selected -> Primary
                    enabled -> SurfaceContainerHigh
                    else -> SurfaceContainerHigh.copy(alpha = 0.45f)
                }
            )
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .heightIn(min = 56.dp)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = content, maxLines = 1)
        Text(
            text = if (workouts > 0) workouts.toString() else "–",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) OnPrimary.copy(alpha = 0.8f) else content.copy(alpha = if (enabled) 0.7f else 1f)
        )
    }
}

@Composable
internal fun CustomRangeRow(range: String?, onClick: () -> Unit, onClear: () -> Unit) {
    val active = range != null
    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (active) Primary.copy(alpha = 0.14f) else SurfaceContainerHigh)
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = if (active) Primary else OnSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(
            text = range ?: stringResource(Res.string.history_filter_custom_range),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (active) Primary else OnSurface,
            modifier = Modifier.weight(1f)
        )
        if (active) {
            val clearLabel = stringResource(Res.string.clear_date)
            IconButton(onClick = onClear, modifier = Modifier.semantics { contentDescription = clearLabel }) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            }
        } else {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
            }
        }
    }
}

// --- Journal -----------------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalSection(
    journalFilters: JournalFilters,
    ratingEnabled: Boolean,
    availableTags: List<TagWithUsage>,
    onJournalFiltersChanged: (JournalFilters) -> Unit
) {
    Column {
        SectionLabel(stringResource(Res.string.history_filter_journal))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (availableTags.isNotEmpty()) {
                ChipGroup(label = stringResource(Res.string.journal_tags)) {
                    availableTags.forEach { tag ->
                        key(tag.id) {
                            TagChip(
                                name = tag.name,
                                colorIndex = tag.colorIndex,
                                customColor = tag.customColor,
                                selected = tag.id in journalFilters.tagIds,
                                onClick = {
                                    val ids = journalFilters.tagIds
                                    onJournalFiltersChanged(journalFilters.copy(tagIds = if (tag.id in ids) ids - tag.id else ids + tag.id))
                                }
                            )
                        }
                    }
                }
                // Several tags: a workout needs at least one of them, or all
                if (journalFilters.tagIds.size > 1) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf(false to Res.string.journal_match_any, true to Res.string.journal_match_all)
                            .forEachIndexed { index, (matchAll, label) ->
                                SegmentedButton(
                                    selected = journalFilters.matchAllTags == matchAll,
                                    onClick = { onJournalFiltersChanged(journalFilters.copy(matchAllTags = matchAll)) },
                                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                                    icon = {},
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = Primary.copy(alpha = 0.15f),
                                        activeContentColor = Primary,
                                        activeBorderColor = Primary.copy(alpha = 0.3f),
                                        inactiveContainerColor = Color.Transparent,
                                        inactiveContentColor = OnSurfaceVariant,
                                        inactiveBorderColor = SurfaceContainerHighest
                                    ),
                                    label = { Text(stringResource(label)) }
                                )
                            }
                    }
                }
            }
            // Rating buckets and "with a note" in one row: both narrow by what was written about the workout
            ChipGroup(label = if (ratingEnabled) stringResource(Res.string.journal_filter_rating) else null) {
                if (ratingEnabled) {
                    RatingBucket.entries.forEach { bucket ->
                        val selected = bucket in journalFilters.ratingBuckets
                        FilterOptionChip(
                            label = stringResource(
                                when (bucket) {
                                    RatingBucket.LOW -> Res.string.journal_bucket_low
                                    RatingBucket.MID -> Res.string.journal_bucket_mid
                                    RatingBucket.HIGH -> Res.string.journal_bucket_high
                                    RatingBucket.UNRATED -> Res.string.journal_not_rated
                                }
                            ),
                            count = null,
                            selected = selected
                        ) {
                            val buckets = journalFilters.ratingBuckets
                            onJournalFiltersChanged(journalFilters.copy(ratingBuckets = if (selected) buckets - bucket else buckets + bucket))
                        }
                    }
                }
                FilterOptionChip(
                    label = stringResource(Res.string.journal_filter_with_note),
                    count = null,
                    selected = journalFilters.onlyWithNote
                ) { onJournalFiltersChanged(journalFilters.copy(onlyWithNote = !journalFilters.onlyWithNote)) }
            }
        }
    }
}

/** A wrapping row of chips with an optional small caption above it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipGroup(label: String?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        // No vertical spacing: each chip already reserves a 48dp touch target around its 32dp body
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

/** A filter chip; [count] (how many workouts it leaves) follows the label in a lighter weight. */
@Composable
internal fun FilterOptionChip(
    label: String,
    count: Int?,
    selected: Boolean,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = buildAnnotatedString {
                    append(label)
                    if (count != null) {
                        append("  ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = (if (selected) Primary else OnSurfaceVariant).copy(alpha = 0.7f))) {
                            append(count.toString())
                        }
                    }
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = when {
            selected -> { { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) } }
            leadingIcon != null -> { { Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) } }
            else -> null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SurfaceContainerHighest,
            selectedContainerColor = Primary.copy(alpha = 0.18f),
            selectedLabelColor = Primary,
            selectedLeadingIconColor = Primary,
            labelColor = OnSurface,
            iconColor = OnSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.Transparent,
            selectedBorderColor = Primary.copy(alpha = 0.35f)
        )
    )
}
