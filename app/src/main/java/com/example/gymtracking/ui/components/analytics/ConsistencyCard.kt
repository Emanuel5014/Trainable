package com.example.gymtracking.ui.components.analytics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtracking.ui.components.GymCard
import com.example.gymtracking.ui.screens.analytics.ConsistencyUiModel
import com.example.gymtracking.ui.theme.OnSurface
import com.example.gymtracking.ui.theme.OnSurfaceVariant
import com.example.gymtracking.ui.theme.Primary
import com.example.gymtracking.ui.theme.Spacing
import com.example.gymtracking.ui.theme.SurfaceContainerHigh
import com.example.gymtracking.ui.theme.Tertiary

@Composable
fun ConsistencyCard(consistency: ConsistencyUiModel) {
    GymCard(containerColor = SurfaceContainerHigh) {
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.medium)) {
            Text(
                text = "CONSISTENCY",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )

            Text(
                text = "${consistency.progress.times(100).toInt()}%",
                style = MaterialTheme.typography.displayMedium,
                color = Primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = consistency.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )

            LinearProgressIndicator(
                progress = { consistency.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = Tertiary,
                trackColor = SurfaceContainerHigh
            )

            Text(
                text = "${consistency.completedSessions}/${consistency.targetSessions} scheduled sessions completed.",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurface
            )
        }
    }
}