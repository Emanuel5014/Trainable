package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.util.WeightUnitConverter

/** Localized labels for prescriptions; weights are shown in the user's [weightUnit]. */
@Composable
fun rememberPrescriptionLabels(weightUnit: String): PrescriptionLabels {
    val context = LocalContext.current
    return remember(weightUnit, context) { prescriptionLabels(context, weightUnit) }
}

fun prescriptionLabels(context: android.content.Context, weightUnit: String): PrescriptionLabels = PrescriptionLabels(
    max = context.getString(R.string.max_label),
    totalReps = { context.getString(R.string.total_reps_label, it) },
    bodyweight = context.getString(R.string.bodyweight_short),
    weight = { kg -> formatWeight(kg, weightUnit) },
    technique = { techniqueLabel(context, it) }
)

fun formatWeight(kg: Float, weightUnit: String): String =
    WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(kg, weightUnit), weightUnit)

fun techniqueLabel(context: android.content.Context, technique: Technique): String = when (technique) {
    is Technique.Pause -> context.getString(R.string.technique_pause_seconds, technique.seconds)
    Technique.Chains -> context.getString(R.string.technique_chains)
    Technique.Bands -> context.getString(R.string.technique_bands)
    Technique.FeetUp -> context.getString(R.string.technique_feet_up)
    Technique.Competition -> context.getString(R.string.technique_competition)
    is Technique.Tempo -> context.getString(R.string.technique_tempo_value, technique.pattern)
    Technique.Deficit -> context.getString(R.string.technique_deficit)
    Technique.Emom -> context.getString(R.string.technique_emom)
    is Technique.Custom -> technique.text
}

/** Small rounded pill, the shared visual unit for prescriptions (same family as the WARM UP chip). */
@Composable
fun PrescriptionPill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceContainerHigh,
    contentColor: Color = OnSurfaceVariant,
    emphasized: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (emphasized) FontWeight.Black else FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1
        )
    }
}

/** Compact overview of all blocks: one pill per block plus technique pills. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrescriptionBlocksSummary(
    blocks: List<PrescriptionBlock>,
    labels: PrescriptionLabels,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        blocks.forEach { block ->
            PrescriptionPill(
                text = PrescriptionFormatter.headline(block, labels),
                containerColor = Primary.copy(alpha = 0.12f),
                contentColor = Primary
            )
        }
        blocks.flatMap { it.techniques }.distinct().forEach { technique ->
            PrescriptionPill(
                text = techniqueLabel(context, technique),
                containerColor = Tertiary.copy(alpha = 0.15f),
                contentColor = Tertiary
            )
        }
    }
}

@Composable
fun weekLabel(week: Int): String = stringResource(R.string.week_short, week)
