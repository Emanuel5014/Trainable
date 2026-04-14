package com.example.gymtracking.ui.components.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.gymtracking.ui.components.GymCard
import com.example.gymtracking.ui.screens.analytics.AnalyticsChartPoint
import com.example.gymtracking.ui.screens.analytics.AnalyticsTimeRange
import com.example.gymtracking.ui.theme.OnSurface
import com.example.gymtracking.ui.theme.OnSurfaceVariant
import com.example.gymtracking.ui.theme.Primary
import com.example.gymtracking.ui.theme.Spacing
import com.example.gymtracking.ui.theme.SurfaceContainerLow

@Composable
fun BodyCompositionCard(
    selectedTimeRange: AnalyticsTimeRange,
    onTimeRangeSelected: (AnalyticsTimeRange) -> Unit,
    bodyWeightHistory: List<AnalyticsChartPoint>,
    bodyWeightInput: String,
    onBodyWeightInputChanged: (String) -> Unit,
    onSubmitWeight: () -> Unit
) {
    GymCard(containerColor = SurfaceContainerLow) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Text(
                text = "BODY COMPOSITION",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )

            Text(
                text = "WEIGHT HISTORY",
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                AnalyticsTimeRange.entries.forEach { timeRange ->
                    FilterChip(
                        selected = selectedTimeRange == timeRange,
                        onClick = { onTimeRangeSelected(timeRange) },
                        label = { Text(timeRange.label) }
                    )
                }
            }

            AnalyticsLineChart(
                points = bodyWeightHistory,
                modifier = Modifier.fillMaxWidth(),
                lineColor = Primary,
                fillColor = Primary.copy(alpha = 0.14f)
            )

            OutlinedTextField(
                value = bodyWeightInput,
                onValueChange = onBodyWeightInputChanged,
                label = { Text("Today's weight (kg)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onSubmitWeight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log weight")
            }
        }
    }
}