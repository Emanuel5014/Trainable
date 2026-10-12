package com.emanuel5014.trainable.ui.components.exercise

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.TargetSecondsSlider
import com.emanuel5014.trainable.ui.theme.Error

/**
 * Sets, reps goal and interval of a Time & Reps exercise. The goal must be a whole number (see [TimeAndReps.parseGoal]);
 * what is wrong with it sits above the fields so the keyboard never covers it, like in [RepsEditor].
 */
@Composable
fun TimeAndRepsEditor(
    setsText: String,
    onSetsChange: (String) -> Unit,
    repsGoalText: String,
    onRepsGoalChange: (String) -> Unit,
    seconds: Int,
    onSecondsChange: (Int) -> Unit,
    hapticEnabled: Boolean,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    val invalid = TimeAndReps.parseGoal(repsGoalText) == null
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedVisibility(visible = invalid) {
            Text(
                text = stringResource(R.string.reps_goal_error),
                style = MaterialTheme.typography.bodySmall,
                color = Error,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GymInputField(
                value = setsText,
                onValueChange = onSetsChange,
                label = stringResource(R.string.sets),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            GymInputField(
                value = repsGoalText,
                onValueChange = { onRepsGoalChange(it.filter { c -> c.isDigit() }.take(3)) },
                label = stringResource(R.string.reps_goal_label),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = invalid,
                modifier = Modifier.weight(1f)
            )
        }
        TargetSecondsSlider(
            valueSeconds = seconds,
            onValueChange = onSecondsChange,
            hapticEnabled = hapticEnabled,
            haptic = haptic,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
