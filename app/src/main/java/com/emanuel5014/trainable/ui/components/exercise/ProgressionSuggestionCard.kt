package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import com.emanuel5014.trainable.ui.components.glass.GlassIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import com.emanuel5014.trainable.ui.components.glass.GlassSurface as Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.prescription.DoubleProgression
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.util.WeightUnitConverter

/**
 * Double progression: every set of the last workout reached the top of the rep range, so the load can go up.
 * Applying puts the heavier weight on the sets still to do and sends their reps back to the bottom of the range;
 * the cross keeps the weight for this workout.
 */
@Composable
fun ProgressionSuggestionCard(
    suggestion: DoubleProgression.Suggestion,
    weightUnit: String,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weightText = WeightUnitConverter.formatWithUnit(
        WeightUnitConverter.convertDisplay(suggestion.topWeightKg, weightUnit),
        weightUnit
    )
    Surface(
        modifier = modifier,
        color = Primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.12f))
                    .padding(6.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = stringResource(R.string.progression_hit_max, suggestion.reachedReps),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.progression_try_weight, weightText),
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            GymButton(
                onClick = onApply,
                height = 40,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)
            ) {
                Text(stringResource(R.string.apply).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.progression_dismiss),
                    tint = OnSurfaceVariant
                )
            }
        }
    }
}
