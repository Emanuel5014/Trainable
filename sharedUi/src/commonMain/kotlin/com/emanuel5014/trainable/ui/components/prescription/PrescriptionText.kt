package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.repsRange
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.week_short
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.Tertiary
import com.emanuel5014.trainable.util.WeightUnitConverter
import org.jetbrains.compose.resources.stringResource

/**
 * The words of prescriptions and logged sets outside of composition (labels for the formatter, badges kept in
 * remember). Each app provides it at its root: Android with its resources, which also serve notifications and files.
 */
interface PrescriptionText {
    fun labels(weightUnit: String): PrescriptionLabels
    fun technique(technique: Technique): String
    /** Badges describing the prescription snapshot of a logged set ("75%", "MAX", "Pause 2″", "EXTRA"). */
    fun setPrescriptionBadges(set: SetLogEntity): List<String>
    /** How a logged set really went: its parts and pauses, help, partials, the technique as done. */
    fun setPerformanceBadges(set: SetLogEntity): List<String>
    /** An RPE as a badge: the level name on the simplified scale ("Hard"), "RPE 8" on the others. */
    fun rpeBadge(value: Float, scale: RpeScale): String
}

val LocalPrescriptionText = staticCompositionLocalOf<PrescriptionText> { error("No PrescriptionText provided") }

/** Localized labels for prescriptions; weights are shown in the user's [weightUnit]. */
@Composable
fun rememberPrescriptionLabels(weightUnit: String): PrescriptionLabels {
    val text = LocalPrescriptionText.current
    return remember(weightUnit, text) { text.labels(weightUnit) }
}

fun formatWeight(kg: Float, weightUnit: String): String =
    WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(kg, weightUnit), weightUnit)

/** Compact overview of all blocks: one pill per block plus technique pills. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrescriptionBlocksSummary(
    blocks: List<PrescriptionBlock>,
    labels: PrescriptionLabels,
    modifier: Modifier = Modifier
) {
    val text = LocalPrescriptionText.current
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
                text = text.technique(technique),
                containerColor = Tertiary.copy(alpha = 0.15f),
                contentColor = Tertiary
            )
        }
    }
}

@Composable
fun weekLabel(week: Int): String = stringResource(Res.string.week_short, week)

/**
 * Pills row for a logged set: the rep range it was done against ("10–15", for any set that has one) and, with
 * advanced programming on, its prescription; the %1RM badge (if any) is highlighted.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetLogBadges(set: SetLogEntity, modifier: Modifier = Modifier) {
    val advanced = LocalAdvancedProgramming.current
    val text = LocalPrescriptionText.current
    val badges = remember(set, advanced) { if (advanced) text.setPrescriptionBadges(set) else emptyList() }
    // How the set went shows whether advanced programming is on or not
    val performance = remember(set) { text.setPerformanceBadges(set) }
    val range = set.repsRange?.text
    // The RPE of a set shows wherever it was recorded, with advanced programming on or off
    val rpeScale = LocalRpeSettings.current.scale
    val rpeText = set.rpe?.let { text.rpeBadge(it, rpeScale) }
    if (badges.isEmpty() && range == null && performance.isEmpty() && rpeText == null) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        range?.let {
            PrescriptionPill(text = it, containerColor = SurfaceContainerHigh, contentColor = OnSurfaceVariant)
        }
        badges.forEachIndexed { i, badge ->
            val highlight = i == 0 && set.targetPercent != null
            PrescriptionPill(
                text = badge,
                containerColor = if (highlight) Primary.copy(alpha = 0.12f) else SurfaceContainerHigh,
                contentColor = if (highlight) Primary else OnSurfaceVariant,
                emphasized = highlight
            )
        }
        rpeText?.let { PrescriptionPill(text = it, containerColor = SurfaceContainerHigh, contentColor = OnSurfaceVariant) }
        performance.forEach { badge ->
            PrescriptionPill(text = badge, containerColor = Tertiary.copy(alpha = 0.15f), contentColor = Tertiary)
        }
    }
}
