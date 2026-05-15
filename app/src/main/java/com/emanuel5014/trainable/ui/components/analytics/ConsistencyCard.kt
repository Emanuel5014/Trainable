package com.emanuel5014.trainable.ui.components.analytics

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
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.screens.analytics.ConsistencyUiModel
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary

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
                fontWeight = FontWeight.ExtraBold
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