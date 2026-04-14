package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.OnTertiary
import com.emanuel5014.trainable.ui.theme.Tertiary

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RestTimerSection(
    remainingSeconds: Int,
    totalRestSeconds: Int = 90,
    onAddTime: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = remainingSeconds > 0
    val progress = if (totalRestSeconds > 0) 1f - (remainingSeconds.toFloat() / totalRestSeconds.toFloat()) else 0f

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
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularWavyProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(56.dp),
                            color = OnTertiary,
                            trackColor = OnTertiary.copy(alpha = 0.2f),
                            stroke = WavyProgressIndicatorDefaults.circularIndicatorStroke,
                            trackStroke = WavyProgressIndicatorDefaults.circularTrackStroke
                        )
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Rest Timer",
                            tint = OnTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "REST",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnTertiary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        val minutes = remainingSeconds / 60
                        val seconds = remainingSeconds % 60
                        Text(
                            text = String.format("%d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.titleLarge,
                            color = OnTertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = onAddTime,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = OnTertiary.copy(alpha = 0.1f),
                            contentColor = OnTertiary
                        )
                    ) {
                        Text("+30s", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    
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
