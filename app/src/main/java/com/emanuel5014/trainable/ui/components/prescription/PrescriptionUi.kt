package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.data.local.entity.parts
import com.emanuel5014.trainable.data.local.entity.pauses
import com.emanuel5014.trainable.data.local.entity.performed
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
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.repsRange
import com.emanuel5014.trainable.domain.prescription.TechniqueCodec
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import com.emanuel5014.trainable.ui.theme.OnPrimary

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
            // Long ones (a custom technique as done) wrap instead of losing their end
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

/** Prescription of a logged set as plain text for exports ("75% · MAX · Pause 2″ · 2+1+1"), without RPE. */
fun setLogPrescriptionText(context: android.content.Context, set: SetLogEntity): String? =
    (setLogPrescriptionBadges(context, set.copy(rpe = null)) + setLogPerformanceBadges(context, set))
        .takeIf { it.isNotEmpty() }?.joinToString(" · ")

/** What an export says about a set: its prescription (with advanced programming on) and how it really went. */
fun setLogDetailsText(context: android.content.Context, set: SetLogEntity, withPrescription: Boolean): String? =
    if (withPrescription) setLogPrescriptionText(context, set)
    else setLogPerformanceBadges(context, set).takeIf { it.isNotEmpty() }?.joinToString(" · ")

/**
 * How a set really went, as badges: its parts after short pauses ("2+1+1 · pauses 60s, 50s"), the reps a spotter
 * helped with, the partials, and the technique it was done with when it was not the planned one.
 */
fun setPerformanceBadges(
    context: android.content.Context,
    parts: List<Int>,
    pauses: List<Int>,
    assisted: Int,
    partial: Int,
    performed: List<Technique>?
): List<String> = buildList {
    if (parts.size > 1) {
        val text = parts.joinToString("+")
        add(
            if (pauses.isEmpty()) text
            else context.resources.getQuantityString(R.plurals.parts_pause_badge, pauses.size, text, pauses.joinToString(", ") { "$it″" })
        )
    }
    if (assisted > 0) add(context.resources.getQuantityString(R.plurals.assisted_reps_badge, assisted, assisted))
    if (partial > 0) add(context.resources.getQuantityString(R.plurals.partial_reps_badge, partial, partial))
    performed?.let { list ->
        // "Done: Discesa 3, Fermo 1, Salita 3", or "No technique" for a set done without any of the planned ones
        add(
            if (list.isEmpty()) context.getString(R.string.performed_technique_none)
            else context.getString(R.string.performed_technique_badge, list.joinToString(" · ") { techniqueLabel(context, it) })
        )
    }
}

fun setLogPerformanceBadges(context: android.content.Context, set: SetLogEntity): List<String> =
    setPerformanceBadges(context, set.parts, set.pauses, set.assistedReps, set.partialReps, set.performed)

/** Badges describing the prescription snapshot and RPE of a logged set ("75%", "MAX", "Pause 2″", "RPE 8", "EXTRA"). */
fun setLogPrescriptionBadges(context: android.content.Context, set: SetLogEntity): List<String> = buildList {
    set.targetPercent?.let { add("${PrescriptionFormatter.number(it)}%") }
    set.targetRpe?.let { add("@" + PrescriptionFormatter.number(it)) }
    when (set.repMode) {
        "amrap" -> add(context.getString(R.string.max_label))
        "total" -> set.targetTotalReps?.let { add(context.getString(R.string.total_reps_label, it)) }
    }
    TechniqueCodec.decode(set.techniques).forEach { add(techniqueLabel(context, it)) }
    set.rpe?.let { add(context.getString(R.string.rpe_short, PrescriptionFormatter.number(it))) }
    if (set.isExtra) add(context.getString(R.string.extra_badge))
}

/**
 * Pills row for a logged set: the rep range it was done against ("10–15", for any set that has one) and, with
 * advanced programming on, its prescription; the %1RM badge (if any) is highlighted.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetLogBadges(set: SetLogEntity, modifier: Modifier = Modifier) {
    val advanced = LocalAdvancedProgramming.current
    val context = LocalContext.current
    val badges = remember(set, advanced) { if (advanced) setLogPrescriptionBadges(context, set) else emptyList() }
    // How the set went shows whether advanced programming is on or not
    val performance = remember(set) { setLogPerformanceBadges(context, set) }
    val range = set.repsRange?.text
    if (badges.isEmpty() && range == null && performance.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        range?.let {
            PrescriptionPill(text = it, containerColor = SurfaceContainerHigh, contentColor = OnSurfaceVariant)
        }
        badges.forEachIndexed { i, text ->
            val highlight = i == 0 && set.targetPercent != null
            PrescriptionPill(
                text = text,
                containerColor = if (highlight) Primary.copy(alpha = 0.12f) else SurfaceContainerHigh,
                contentColor = if (highlight) Primary else OnSurfaceVariant,
                emphasized = highlight
            )
        }
        performance.forEach { text ->
            PrescriptionPill(text = text, containerColor = Tertiary.copy(alpha = 0.15f), contentColor = Tertiary)
        }
    }
}
