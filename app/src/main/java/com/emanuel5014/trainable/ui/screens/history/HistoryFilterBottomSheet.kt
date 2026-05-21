package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymInputField
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterBottomSheet(
    selectedPlanId: Int?,
    availablePlans: List<WorkoutPlanEntity>,
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
    var routineTab by remember { mutableStateOf(0) } // 0: Active, 1: Archived, 2: All

    val filteredPlans = remember(availablePlans, routineSearchQuery, routineTab) {
        availablePlans.filter { plan ->
            val matchesSearch = plan.nome.contains(routineSearchQuery, ignoreCase = true)
            val matchesTab = when (routineTab) {
                0 -> plan.isActive
                1 -> !plan.isActive
                else -> true
            }
            matchesSearch && matchesTab
        }
    }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
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
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.routines),
                            style = MaterialTheme.typography.labelMedium,
                            color = primaryColor,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        
                        val selectedPlanName = remember(selectedPlanId, availablePlans) {
                            availablePlans.find { it.id == selectedPlanId }?.nome
                        }
                        
                        if (selectedPlanName != null) {
                            Text(
                                text = "• $selectedPlanName",
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVariantColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    GymInputField(
                        value = routineSearchQuery,
                        onValueChange = { routineSearchQuery = it },
                        label = stringResource(R.string.search_routines),
                        placeholder = stringResource(R.string.search_routines),
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = onSurfaceVariantColor, modifier = Modifier.size(20.dp))
                        },
                        containerColor = surfaceContainerHighestColor
                    )

                    // Tabs (All, Active, Archived)
                    val tabLabels = listOf(
                        stringResource(R.string.active_routines),
                        stringResource(R.string.archived_routines_header),
                        stringResource(R.string.all)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(tabLabels) { index, label ->
                            FilterChip(
                                selected = routineTab == index,
                                onClick = { routineTab = index },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor.copy(alpha = 0.15f),
                                    selectedLabelColor = primaryColor,
                                    labelColor = onSurfaceVariantColor
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = routineTab == index,
                                    borderColor = surfaceContainerHighestColor,
                                    selectedBorderColor = primaryColor.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    // Routine Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "All" chip for current selection
                        if (routineTab == 2 && routineSearchQuery.isBlank()) {
                            item {
                                FilterChip(
                                    selected = selectedPlanId == null,
                                    onClick = { onPlanSelected(null) },
                                    label = { Text(stringResource(R.string.all)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryColor.copy(alpha = 0.15f),
                                        selectedLabelColor = primaryColor,
                                        labelColor = onSurfaceVariantColor
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedPlanId == null,
                                        borderColor = surfaceContainerHighestColor,
                                        selectedBorderColor = primaryColor.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }

                        items(filteredPlans) { plan ->
                            FilterChip(
                                selected = selectedPlanId == plan.id,
                                onClick = { onPlanSelected(plan.id) },
                                label = { Text(plan.nome) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor.copy(alpha = 0.15f),
                                    selectedLabelColor = primaryColor,
                                    labelColor = onSurfaceVariantColor
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedPlanId == plan.id,
                                    borderColor = surfaceContainerHighestColor,
                                    selectedBorderColor = primaryColor.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                    
                    if (filteredPlans.isEmpty() && (routineTab != 0 || routineSearchQuery.isNotBlank())) {
                        Text(
                            text = stringResource(R.string.no_results_filters),
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurfaceVariantColor
                        )
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
                            FilterChip(
                                selected = startDate == null,
                                onClick = { onDateRangeSelected(null, null) },
                                label = { Text(stringResource(R.string.all)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor.copy(alpha = 0.15f),
                                    selectedLabelColor = primaryColor,
                                    labelColor = onSurfaceVariantColor
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = startDate == null,
                                    borderColor = surfaceContainerHighestColor,
                                    selectedBorderColor = primaryColor.copy(alpha = 0.3f)
                                )
                            )
                        }

                        item {
                            val range = getMonthRange(currentMonth)
                            val isSelected = startDate == range.first && endDate == range.second
                            FilterChip(
                                selected = isSelected,
                                onClick = { onDateRangeSelected(range.first, range.second) },
                                label = { Text(stringResource(R.string.this_month)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor.copy(alpha = 0.15f),
                                    selectedLabelColor = primaryColor,
                                    labelColor = onSurfaceVariantColor
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = surfaceContainerHighestColor,
                                    selectedBorderColor = primaryColor.copy(alpha = 0.3f)
                                )
                            )
                        }

                        item {
                            val lastMonthIndex = if (currentMonth == 0) 11 else currentMonth - 1
                            val lastMonthYear = if (currentMonth == 0) currentYear - 1 else currentYear
                            val range = getMonthRange(lastMonthIndex, lastMonthYear)
                            val isSelected = startDate == range.first && endDate == range.second
                            FilterChip(
                                selected = isSelected,
                                onClick = { onDateRangeSelected(range.first, range.second) },
                                label = { Text(stringResource(R.string.last_month)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor.copy(alpha = 0.15f),
                                    selectedLabelColor = primaryColor,
                                    labelColor = onSurfaceVariantColor
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = surfaceContainerHighestColor,
                                    selectedBorderColor = primaryColor.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    // Month Selector
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(monthNames) { index, month ->
                            val isSelected = isMonthSelected(index)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val range = getMonthRange(index)
                                    onDateRangeSelected(range.first, range.second)
                                },
                                label = { Text(month) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor.copy(alpha = 0.15f),
                                    selectedLabelColor = primaryColor,
                                    labelColor = onSurfaceVariantColor
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = surfaceContainerHighestColor,
                                    selectedBorderColor = primaryColor.copy(alpha = 0.3f)
                                )
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear Date",
                                tint = onSurfaceVariantColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onClearDate() }
                            )
                        }
                    }
                }
            }
            
            item {
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
            
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
