package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.isArchived
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.SheetFormFooter
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.util.DateFormatter
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

/** Which routines the picker lists: every one, only the active ones or only the archived ones. */
private enum class RoutineStatus { All, Active, Archived }

/** Below this many routines the picker is short enough that a search field would only be noise. */
private const val SEARCH_MIN_ROUTINES = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterBottomSheet(
    selectedPlanId: Int?,
    availablePlans: List<WorkoutPlanEntity>,
    hasUnassignedSessions: Boolean,
    startDate: Long?,
    endDate: Long?,
    onPlanSelected: (Int?) -> Unit,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    onDateClick: () -> Unit,
    onClearDate: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Resolve colors
    val onSurfaceColor = OnSurface
    val onSurfaceVariantColor = OnSurfaceVariant
    val primaryColor = Primary
    val errorColor = Error
    val surfaceContainerHighColor = SurfaceContainerHigh
    val surfaceContainerHighestColor = SurfaceContainerHighest

    val calendar = remember { Calendar.getInstance() }
    val currentMonth = remember<Int>(calendar) { calendar.get(Calendar.MONTH) }
    val currentYear = remember<Int>(calendar) { calendar.get(Calendar.YEAR) }

    val monthNames = remember<List<String>> {
        val symbols = DateFormatSymbols.getInstance(Locale.getDefault())
        symbols.shortMonths.filter { it.isNotEmpty() }
    }

    var routineSearchQuery by remember { mutableStateOf("") }
    var routineStatus by remember { mutableStateOf(RoutineStatus.All) }

    val (archivedPlans, activePlans) = remember(availablePlans) { availablePlans.partition { it.isArchived } }
    val selectedPlan = remember(selectedPlanId, availablePlans) { availablePlans.find { it.id == selectedPlanId } }
    val showSearch = availablePlans.size > SEARCH_MIN_ROUTINES
    val query = if (showSearch) routineSearchQuery.trim() else ""

    val visibleActive = remember(activePlans, query, routineStatus) {
        if (routineStatus == RoutineStatus.Archived) emptyList()
        else activePlans.filter { it.nome.contains(query, ignoreCase = true) }
    }
    val visibleArchived = remember(archivedPlans, query, routineStatus) {
        if (routineStatus == RoutineStatus.Active) emptyList()
        else archivedPlans.filter { it.nome.contains(query, ignoreCase = true) }
    }

    // Workouts that belong to no routine (cardio, custom, quick, deleted routines) have no plan to pick
    val noRoutineLabel = stringResource(R.string.no_routine)
    val noRoutineSelected = selectedPlanId == NO_ROUTINE_PLAN_ID
    val showNoRoutine = (hasUnassignedSessions || noRoutineSelected) &&
        routineStatus == RoutineStatus.All &&
        noRoutineLabel.contains(query, ignoreCase = true)

    fun getMonthRange(monthIndex: Int, year: Int = currentYear): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthIndex)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return start to end
    }

    fun isMonthSelected(monthIndex: Int): Boolean {
        if (startDate == null || endDate == null) return false
        val cal = Calendar.getInstance()
        cal.timeInMillis = startDate
        val sMonth = cal.get(Calendar.MONTH)
        val sYear = cal.get(Calendar.YEAR)
        val sDay = cal.get(Calendar.DAY_OF_MONTH)

        cal.timeInMillis = endDate
        val eMonth = cal.get(Calendar.MONTH)
        val eYear = cal.get(Calendar.YEAR)
        val eDay = cal.get(Calendar.DAY_OF_MONTH)

        return sMonth == monthIndex && sYear == currentYear && sDay == 1 &&
               eMonth == monthIndex && eYear == currentYear && eDay == cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceContainerHighColor,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(onSurfaceVariantColor.copy(alpha = 0.4f))
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 600.dp)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.filters).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = onSurfaceColor,
                            letterSpacing = (-0.5).sp
                        )

                        if (selectedPlanId != null || startDate != null) {
                            Text(
                                text = stringResource(R.string.clear_all),
                                style = MaterialTheme.typography.labelLarge,
                                color = errorColor,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.clickable { onClearAll() }
                            )
                        }
                    }
                }

                // Routines Section
                if (availablePlans.isNotEmpty() || hasUnassignedSessions || noRoutineSelected) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 32.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.routines),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = primaryColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )

                                if (selectedPlan != null || noRoutineSelected) {
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                        SelectedRoutinePill(
                                            label = selectedPlan?.nome ?: noRoutineLabel,
                                            leadingIcon = when {
                                                selectedPlan?.isArchived == true -> Icons.Rounded.Archive
                                                selectedPlan == null -> Icons.Rounded.LinkOff
                                                else -> null
                                            },
                                            onClear = { onPlanSelected(null) }
                                        )
                                    }
                                }
                            }

                            if (availablePlans.isNotEmpty()) {
                                // Status: every routine, only the active ones or only the archived ones
                                val statusCounts = listOf(
                                    RoutineStatus.All to availablePlans.size,
                                    RoutineStatus.Active to activePlans.size,
                                    RoutineStatus.Archived to archivedPlans.size
                                )
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    statusCounts.forEachIndexed { index, (status, count) ->
                                        val selected = routineStatus == status
                                        val label = stringResource(
                                            when (status) {
                                                RoutineStatus.All -> R.string.all
                                                RoutineStatus.Active -> R.string.active_routines
                                                RoutineStatus.Archived -> R.string.archived_routines_header
                                            }
                                        )
                                        SegmentedButton(
                                            selected = selected,
                                            onClick = { routineStatus = status },
                                            enabled = count > 0 || selected,
                                            shape = SegmentedButtonDefaults.itemShape(index, statusCounts.size),
                                            icon = {},
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                            colors = SegmentedButtonDefaults.colors(
                                                activeContainerColor = primaryColor.copy(alpha = 0.15f),
                                                activeContentColor = primaryColor,
                                                activeBorderColor = primaryColor.copy(alpha = 0.3f),
                                                inactiveContainerColor = Color.Transparent,
                                                inactiveContentColor = onSurfaceVariantColor,
                                                inactiveBorderColor = surfaceContainerHighestColor,
                                                disabledInactiveContainerColor = Color.Transparent,
                                                disabledInactiveContentColor = onSurfaceVariantColor.copy(alpha = 0.38f),
                                                disabledInactiveBorderColor = surfaceContainerHighestColor
                                            ),
                                            label = {
                                                val countColor = LocalContentColor.current.copy(alpha = 0.6f)
                                                Text(
                                                    text = buildAnnotatedString {
                                                        append(label)
                                                        append("  ")
                                                        withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = countColor)) {
                                                            append(count.toString())
                                                        }
                                                    },
                                                    style = MaterialTheme.typography.labelLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        )
                                    }
                                }

                                if (showSearch) {
                                    GymInputField(
                                        value = routineSearchQuery,
                                        onValueChange = { routineSearchQuery = it },
                                        label = stringResource(R.string.search_routines),
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Search, contentDescription = null, tint = onSurfaceVariantColor, modifier = Modifier.size(20.dp))
                                        },
                                        trailingIcon = if (routineSearchQuery.isNotEmpty()) {
                                            {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .clickable { routineSearchQuery = "" },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Rounded.Close, contentDescription = null, tint = onSurfaceVariantColor, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        } else null,
                                        containerColor = surfaceContainerHighestColor
                                    )
                                }
                            }

                            if (visibleActive.isEmpty() && visibleArchived.isEmpty() && !showNoRoutine) {
                                Text(
                                    text = stringResource(
                                        if (query.isEmpty() && routineStatus == RoutineStatus.Archived) R.string.no_archived
                                        else R.string.no_results_filters
                                    ),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = onSurfaceVariantColor
                                )
                            } else {
                                // Labelled groups only when several kinds are on screen, otherwise the status already says it
                                val showGroupLabels = listOf(
                                    visibleActive.isNotEmpty(),
                                    visibleArchived.isNotEmpty(),
                                    showNoRoutine
                                ).count { it } > 1
                                if (visibleActive.isNotEmpty()) {
                                    RoutineChipGroup(
                                        label = if (showGroupLabels) stringResource(R.string.active_routines) else null,
                                        archived = false,
                                        plans = visibleActive,
                                        selectedPlanId = selectedPlanId,
                                        onPlanSelected = onPlanSelected
                                    )
                                }
                                if (visibleArchived.isNotEmpty()) {
                                    RoutineChipGroup(
                                        label = if (showGroupLabels) stringResource(R.string.archived_routines_header) else null,
                                        archived = true,
                                        plans = visibleArchived,
                                        selectedPlanId = selectedPlanId,
                                        onPlanSelected = onPlanSelected
                                    )
                                }
                                if (showNoRoutine) {
                                    ChipGroup(label = if (showGroupLabels) stringResource(R.string.routine_group_other) else null) {
                                        FilterOptionChip(
                                            label = noRoutineLabel,
                                            selected = noRoutineSelected,
                                            onClick = { onPlanSelected(if (noRoutineSelected) null else NO_ROUTINE_PLAN_ID) },
                                            leadingIcon = Icons.Rounded.LinkOff
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Date Filter Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.date_range),
                            style = MaterialTheme.typography.labelMedium,
                            color = primaryColor,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )

                        // Quick Presets
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterOptionChip(
                                    label = stringResource(R.string.all),
                                    selected = startDate == null,
                                    onClick = { onDateRangeSelected(null, null) }
                                )
                            }

                            item {
                                val range = getMonthRange(currentMonth)
                                FilterOptionChip(
                                    label = stringResource(R.string.this_month),
                                    selected = startDate == range.first && endDate == range.second,
                                    onClick = { onDateRangeSelected(range.first, range.second) }
                                )
                            }

                            item {
                                val lastMonthIndex = if (currentMonth == 0) 11 else currentMonth - 1
                                val lastMonthYear = if (currentMonth == 0) currentYear - 1 else currentYear
                                val range = getMonthRange(lastMonthIndex, lastMonthYear)
                                FilterOptionChip(
                                    label = stringResource(R.string.last_month),
                                    selected = startDate == range.first && endDate == range.second,
                                    onClick = { onDateRangeSelected(range.first, range.second) }
                                )
                            }
                        }

                        // Month Selector
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(monthNames) { index, month ->
                                FilterOptionChip(
                                    label = month,
                                    selected = isMonthSelected(index),
                                    onClick = {
                                        val range = getMonthRange(index)
                                        onDateRangeSelected(range.first, range.second)
                                    }
                                )
                            }
                        }

                        // Custom Range Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Shapes.medium)
                                .background(surfaceContainerHighestColor)
                                .clickable { onDateClick() }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (startDate != null && endDate != null) {
                                        "${DateFormatter.formatShort(startDate)} - ${DateFormatter.formatShort(endDate)}"
                                    } else {
                                        stringResource(R.string.select_date_range)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = onSurfaceColor
                                )
                            }
                            if (startDate != null) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable(onClickLabel = stringResource(R.string.clear_date)) { onClearDate() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = stringResource(R.string.clear_date),
                                        tint = onSurfaceVariantColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SheetFormFooter(horizontalPadding = 24.dp) {
                GymButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.done).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

/**
 * The routines of one kind as wrapping chips, so every name is readable without sideways scrolling.
 * Tapping the selected chip again drops the routine filter.
 */
@Composable
private fun RoutineChipGroup(
    label: String?,
    archived: Boolean,
    plans: List<WorkoutPlanEntity>,
    selectedPlanId: Int?,
    onPlanSelected: (Int?) -> Unit
) {
    ChipGroup(label = label, labelIcon = if (archived) Icons.Rounded.Archive else null) {
        plans.forEach { plan ->
            key(plan.id) {
                val selected = selectedPlanId == plan.id
                FilterOptionChip(
                    label = plan.nome,
                    selected = selected,
                    onClick = { onPlanSelected(if (selected) null else plan.id) },
                    leadingIcon = if (archived) Icons.Rounded.Archive else null
                )
            }
        }
    }
}

/** A wrapping row of chips with an optional small caption above it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGroup(
    label: String?,
    labelIcon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (labelIcon != null) {
                    Icon(
                        labelIcon,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
        // No vertical spacing: each chip already reserves a 48dp touch target around its 32dp body
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

/** The routine currently filtered on, tappable to remove the filter. */
@Composable
private fun SelectedRoutinePill(
    label: String,
    leadingIcon: ImageVector?,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(CircleShape)
            .background(Primary.copy(alpha = 0.12f))
            .clickable(onClickLabel = stringResource(R.string.clear_routine_filter), onClick = onClear)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Primary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
            Icons.Rounded.Close,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun FilterOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary.copy(alpha = 0.15f),
            selectedLabelColor = Primary,
            selectedLeadingIconColor = Primary,
            labelColor = OnSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = SurfaceContainerHighest,
            selectedBorderColor = Primary.copy(alpha = 0.3f)
        )
    )
}
