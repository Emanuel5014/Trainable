package com.emanuel5014.trainable.ui.components.exercise

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.prescription.RepsDraft
import com.emanuel5014.trainable.domain.prescription.RepsError
import com.emanuel5014.trainable.domain.prescription.RepsMode
import com.emanuel5014.trainable.domain.prescription.StartingWeight
import com.emanuel5014.trainable.ui.components.ConnectedToggleRow
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant

/**
 * Sets and reps of a plain exercise: a switch between one number for every set, a range to progress through
 * ("10–15") and one number per set ("8-6-4-2"), and the fields that mode needs next to the sets field.
 * Switching mode carries what was typed over (see [RepsDraft.withMode]).
 */
@Composable
fun RepsEditor(
    draft: RepsDraft,
    onDraftChange: (RepsDraft) -> Unit,
    setsText: String,
    onSetsChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = RepsMode.entries
    val error = draft.error?.let { repsErrorText(it) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ConnectedToggleRow(
            options = listOf(
                stringResource(R.string.reps_mode_fixed),
                stringResource(R.string.reps_mode_range),
                stringResource(R.string.reps_mode_per_set)
            ),
            selectedIndex = modes.indexOf(draft.mode),
            onSelect = { onDraftChange(draft.withMode(modes[it])) },
            stackWhenTextIsLarge = true
        )

        // Above the fields so the keyboard never covers what the mode means or what is wrong
        AnimatedContent(
            targetState = error to draft.mode,
            label = "reps_editor_hint"
        ) { (message, mode) ->
            val hint = when (mode) {
                RepsMode.FIXED -> null
                RepsMode.RANGE -> stringResource(R.string.reps_range_hint)
                RepsMode.PER_SET -> stringResource(R.string.reps_hint)
            }
            val text = message ?: hint
            if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message != null) Error else OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
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
            when (draft.mode) {
                RepsMode.FIXED -> GymInputField(
                    value = draft.fixed,
                    onValueChange = { onDraftChange(draft.withFixed(it)) },
                    label = stringResource(R.string.reps),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    modifier = Modifier.weight(1f)
                )
                RepsMode.RANGE -> {
                    GymInputField(
                        value = draft.min,
                        onValueChange = { onDraftChange(draft.withMin(it)) },
                        label = stringResource(R.string.reps_min),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = error != null,
                        modifier = Modifier.weight(1f)
                    )
                    GymInputField(
                        value = draft.max,
                        onValueChange = { onDraftChange(draft.withMax(it)) },
                        label = stringResource(R.string.reps_max),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = error != null,
                        modifier = Modifier.weight(1f)
                    )
                }
                RepsMode.PER_SET -> GymInputField(
                    value = draft.list,
                    onValueChange = { text ->
                        val updated = draft.withList(text)
                        onDraftChange(updated)
                        // A list spells out how many sets there are. Set it every time rather than comparing with
                        // setsText: fast typing can arrive before that value was refreshed and the sets would stay behind.
                        updated.perSetCount?.let { count -> onSetsChange(count.toString()) }
                    },
                    label = stringResource(R.string.reps),
                    isError = error != null,
                    modifier = Modifier.weight(2f)
                )
            }
        }

    }
}

@Composable
private fun repsErrorText(error: RepsError): String = when (error) {
    RepsError.NOT_A_NUMBER -> stringResource(R.string.reps_number_error)
    RepsError.MAX_NOT_ABOVE_MIN -> stringResource(R.string.reps_range_error)
    RepsError.BAD_LIST -> stringResource(R.string.reps_list_error)
}

/**
 * The weight the sets of this exercise start from (shown when starting weights are on). Blank means none; the
 * value is typed in [weightUnit] and the caller converts it for storage.
 */
@Composable
fun StartingWeightField(
    text: String,
    onTextChange: (String) -> Unit,
    weightUnit: String,
    modifier: Modifier = Modifier
) {
    val invalid = text.isNotBlank() && StartingWeight.parse(text) == null
    GymInputField(
        value = text,
        onValueChange = { onTextChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(MAX_LENGTH)) },
        label = stringResource(R.string.starting_weight_field, weightUnit),
        supportingText = stringResource(R.string.starting_weight_hint),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = invalid,
        modifier = modifier.fillMaxWidth()
    )
}

private const val MAX_LENGTH = 7
