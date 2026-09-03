package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest

private val TARGET_SECONDS_STEPS = listOf(10, 15, 20, 30, 45, 60, 90, 120, 180, 240, 300, 600)

@Composable
fun TargetSecondsSlider(
    valueSeconds: Int,
    onValueChange: (Int) -> Unit,
    hapticEnabled: Boolean,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    val currentIndex = remember(valueSeconds) {
        TARGET_SECONDS_STEPS.indexOf(valueSeconds).takeIf { it != -1 }
            ?: TARGET_SECONDS_STEPS.indexOf(
                TARGET_SECONDS_STEPS.minByOrNull { kotlin.math.abs(it - valueSeconds) } ?: 45
            ).coerceAtLeast(0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.target_seconds),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${valueSeconds}s",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold
            )
            if (valueSeconds >= 60) {
                Text(
                    text = formatRestTime(valueSeconds),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }

        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { rawValue ->
                val index = kotlin.math.round(rawValue).toInt()
                val clampedIndex = index.coerceIn(0, TARGET_SECONDS_STEPS.size - 1)
                if (clampedIndex != currentIndex) {
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(TARGET_SECONDS_STEPS[clampedIndex])
                }
            },
            valueRange = 0f..(TARGET_SECONDS_STEPS.size - 1).toFloat(),
            steps = TARGET_SECONDS_STEPS.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SurfaceContainerHighest
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
