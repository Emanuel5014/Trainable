package com.example.gymtracking.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.gymtracking.ui.theme.OnSurface
import com.example.gymtracking.ui.theme.OnSurfaceVariant
import com.example.gymtracking.ui.theme.Primary
import com.example.gymtracking.ui.theme.Spacing
import com.example.gymtracking.ui.theme.SurfaceContainer

@Composable
fun StatCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector = Icons.AutoMirrored.Filled.TrendingUp,
    iconTint: Color = Primary,
    modifier: Modifier = Modifier
) {
    GymCard(
        modifier = modifier,
        containerColor = SurfaceContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(Spacing.large)
            )
        }
        
        Spacer(modifier = Modifier.height(Spacing.medium))
        
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.small)
            )
        }
    }
}
