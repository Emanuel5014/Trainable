package com.example.gymtracking.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gymtracking.ui.theme.*

@Composable
fun RestTimerSection(
    remainingSeconds: Int,
    onAddTime: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = remainingSeconds > 0

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Tertiary)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Timer Info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = "Rest Timer",
                        tint = OnTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    val minutes = remainingSeconds / 60
                    val seconds = remainingSeconds % 60
                    val timeString = String.format("%d:%02d", minutes, seconds)
                    
                    Column {
                        Text(
                            text = "REST",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnTertiary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.titleLarge,
                            color = OnTertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add Time
                    FilledIconButton(
                        onClick = onAddTime,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = OnTertiary.copy(alpha = 0.1f),
                            contentColor = OnTertiary
                        )
                    ) {
                        Text("+30s", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    // Skip
                    FilledIconButton(
                        onClick = onSkip,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = OnTertiary,
                            contentColor = Tertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Skip Rest",
                        )
                    }
                }
            }
        }
    }
}
