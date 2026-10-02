package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.util.WeightUnitConverter

/**
 * Tonal card showing the 1RM an exercise's percentages are computed from, with a quick
 * "use estimate" action when there's no 1RM yet but the logs allow an Epley estimate.
 */
@Composable
fun OneRepMaxCard(
    oneRepMaxKg: Float?,
    estimatedKg: Float?,
    weightUnit: String,
    onEdit: () -> Unit,
    onUseEstimate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = Shapes.medium,
        color = Primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.one_rep_max).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Primary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = oneRepMaxKg?.let { formatWeight(it, weightUnit) } ?: stringResource(R.string.set_one_rep_max),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (oneRepMaxKg != null) OnSurface else OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold
                )
                if (estimatedKg != null && estimatedKg > 0f) {
                    Text(
                        text = stringResource(R.string.one_rep_max_estimated, formatWeight(estimatedKg, weightUnit)),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
            }
            if (oneRepMaxKg == null && estimatedKg != null && estimatedKg > 0f) {
                TextButton(onClick = { onUseEstimate(estimatedKg) }) {
                    Text(stringResource(R.string.use_estimate).uppercase(), fontWeight = FontWeight.ExtraBold, color = Primary)
                }
            } else {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_one_rep_max), tint = Primary)
            }
        }
    }
}

/** Dialog to record a new 1RM value (entered in the display unit, stored in kg). */
@Composable
fun OneRepMaxDialog(
    title: String,
    initialKg: Float?,
    weightUnit: String,
    onDismiss: () -> Unit,
    onConfirm: (weightKg: Float, source: String) -> Unit
) {
    var text by remember {
        mutableStateOf(initialKg?.let { WeightUnitConverter.format(WeightUnitConverter.convertDisplay(it, weightUnit)) } ?: "")
    }
    var source by remember { mutableStateOf(OneRepMaxEntity.SOURCE_TESTED) }
    val parsed = text.replace(',', '.').toFloatOrNull()?.takeIf { it > 0f }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GymInputField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = "${stringResource(R.string.one_rep_max_weight)} ($weightUnit)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        OneRepMaxEntity.SOURCE_TESTED to R.string.source_tested,
                        OneRepMaxEntity.SOURCE_MANUAL to R.string.source_manual
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = source == value,
                            onClick = { source = value },
                            label = { Text(stringResource(label)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary.copy(alpha = 0.15f),
                                selectedLabelColor = Primary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            GymButton(
                onClick = { parsed?.let { onConfirm(WeightUnitConverter.convertStorage(it, weightUnit), source) } },
                enabled = parsed != null,
                containerColor = Primary,
                contentColor = OnPrimary,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            GymButton(
                onClick = onDismiss,
                containerColor = Color.Transparent,
                contentColor = OnSurfaceVariant,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.cancel).uppercase())
            }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}
