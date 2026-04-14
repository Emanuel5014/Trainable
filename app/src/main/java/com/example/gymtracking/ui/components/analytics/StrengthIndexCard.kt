package com.example.gymtracking.ui.components.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.gymtracking.ui.components.GymCard
import com.example.gymtracking.ui.screens.analytics.StrengthIndexUiModel
import com.example.gymtracking.ui.theme.OnPrimary
import com.example.gymtracking.ui.theme.Primary
import com.example.gymtracking.ui.theme.Spacing

@Composable
fun StrengthIndexCard(strengthIndex: StrengthIndexUiModel) {
    GymCard(containerColor = Primary) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "STRENGTH INDEX",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnPrimary.copy(alpha = 0.85f)
                )
                Icon(
                    imageVector = if ((strengthIndex.percent ?: 0f) >= 0f) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                    contentDescription = null,
                    tint = OnPrimary
                )
            }

            Text(
                text = strengthIndex.percent?.let { formatSignedPercent(it) } ?: "--",
                style = MaterialTheme.typography.displayMedium,
                color = OnPrimary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = strengthIndex.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = OnPrimary.copy(alpha = 0.8f)
            )
        }
    }
}

private fun formatSignedPercent(value: Float): String {
    val formatted = String.format(java.util.Locale.getDefault(), "%.1f", kotlin.math.abs(value))
    return if (value >= 0f) "+$formatted%" else "-$formatted%"
}