package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.apply_filters
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.select_date_range
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.util.DateFormatter
import org.jetbrains.compose.resources.stringResource

/**
 * The calendar that picks a range of days, for the history filters and the CSV export: [onPicked] gets the first and the
 * last millisecond of the range in the phone's time zone (see [pickedDaysRange]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDateRangeDialog(onDismiss: () -> Unit, onPicked: (from: Long, to: Long) -> Unit) {
    val dateRangePickerState = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            GymButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        val (from, to) = pickedDaysRange(start, end)
                        onPicked(from, to)
                    }
                    onDismiss()
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
            ) {
                Text(stringResource(Res.string.apply_filters).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            GymButton(
                onClick = onDismiss,
                containerColor = Color.Transparent,
                contentColor = OnSurfaceVariant
            ) {
                Text(stringResource(Res.string.cancel).uppercase())
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = SurfaceContainerHigh
        )
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp, max = 560.dp).fillMaxHeight(0.85f),
            title = {
                Text(
                    text = stringResource(Res.string.select_date_range),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            headline = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (dateRangePickerState.selectedStartDateMillis != null) {
                            DateFormatter.formatShort(dateRangePickerState.selectedStartDateMillis!!)
                        } else "Start",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text("-", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = if (dateRangePickerState.selectedEndDateMillis != null) {
                            DateFormatter.formatShort(dateRangePickerState.selectedEndDateMillis!!)
                        } else "End",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = SurfaceContainerHigh,
                selectedDayContainerColor = Primary,
                todayDateBorderColor = Primary,
                dayInSelectionRangeContainerColor = Primary.copy(alpha = 0.1f)
            )
        )
    }
}
