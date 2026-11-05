package com.emanuel5014.trainable.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.parts
import com.emanuel5014.trainable.data.local.entity.pauses
import com.emanuel5014.trainable.data.local.entity.performed
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.domain.prescription.TechniqueCodec
import com.emanuel5014.trainable.domain.rpe.RpeLevel
import com.emanuel5014.trainable.domain.rpe.RpeScale
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.assisted_reps_badge
import com.emanuel5014.trainable.sharedui.resources.bodyweight_short
import com.emanuel5014.trainable.sharedui.resources.extra_badge
import com.emanuel5014.trainable.sharedui.resources.max_label
import com.emanuel5014.trainable.sharedui.resources.partial_reps_badge
import com.emanuel5014.trainable.sharedui.resources.parts_pause_badge
import com.emanuel5014.trainable.sharedui.resources.performed_technique_badge
import com.emanuel5014.trainable.sharedui.resources.performed_technique_none
import com.emanuel5014.trainable.sharedui.resources.rpe_short
import com.emanuel5014.trainable.sharedui.resources.technique_bands
import com.emanuel5014.trainable.sharedui.resources.technique_chains
import com.emanuel5014.trainable.sharedui.resources.technique_competition
import com.emanuel5014.trainable.sharedui.resources.technique_deficit
import com.emanuel5014.trainable.sharedui.resources.technique_emom
import com.emanuel5014.trainable.sharedui.resources.technique_feet_up
import com.emanuel5014.trainable.sharedui.resources.technique_pause_seconds
import com.emanuel5014.trainable.sharedui.resources.technique_tempo_value
import com.emanuel5014.trainable.sharedui.resources.total_reps_label
import com.emanuel5014.trainable.ui.components.PrescriptionText
import com.emanuel5014.trainable.ui.components.formatWeight
import com.emanuel5014.trainable.ui.components.nameText
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The words of the shared strings, read once in composition so they can be filled in outside of it. */
private class Templates(
    val max: String,
    val totalReps: String,
    val bodyweight: String,
    val pause: String,
    val chains: String,
    val bands: String,
    val feetUp: String,
    val competition: String,
    val tempo: String,
    val deficit: String,
    val emom: String,
    val extra: String,
    val performedNone: String,
    val performed: String,
    val rpeShort: String,
    val levels: Map<RpeLevel, String>,
    /** Singular and plural of each badge: these languages only have the two. */
    val parts: Pair<String, String>,
    val assisted: Pair<String, String>,
    val partial: Pair<String, String>
)

/** The iOS PrescriptionText: the same words and rules as Android's (PrescriptionUi.kt) from the shared strings. */
@Composable
internal fun rememberIosPrescriptionText(): PrescriptionText {
    val templates = Templates(
        max = stringResource(Res.string.max_label),
        totalReps = stringResource(Res.string.total_reps_label),
        bodyweight = stringResource(Res.string.bodyweight_short),
        pause = stringResource(Res.string.technique_pause_seconds),
        chains = stringResource(Res.string.technique_chains),
        bands = stringResource(Res.string.technique_bands),
        feetUp = stringResource(Res.string.technique_feet_up),
        competition = stringResource(Res.string.technique_competition),
        tempo = stringResource(Res.string.technique_tempo_value),
        deficit = stringResource(Res.string.technique_deficit),
        emom = stringResource(Res.string.technique_emom),
        extra = stringResource(Res.string.extra_badge),
        performedNone = stringResource(Res.string.performed_technique_none),
        performed = stringResource(Res.string.performed_technique_badge),
        rpeShort = stringResource(Res.string.rpe_short),
        levels = RpeLevel.entries.associateWith { stringResource(it.nameText()) },
        parts = pluralStringResource(Res.plurals.parts_pause_badge, 1) to pluralStringResource(Res.plurals.parts_pause_badge, 2),
        assisted = pluralStringResource(Res.plurals.assisted_reps_badge, 1) to pluralStringResource(Res.plurals.assisted_reps_badge, 2),
        partial = pluralStringResource(Res.plurals.partial_reps_badge, 1) to pluralStringResource(Res.plurals.partial_reps_badge, 2)
    )
    val language = Locale.current.language
    return remember(templates.max, templates.performed, language) { IosPrescriptionText(templates, language) }
}

private class IosPrescriptionText(private val t: Templates, private val language: String) : PrescriptionText {

    /** "%1$s" / "%1$d" style placeholders, as Android's getString fills them. */
    private fun fill(template: String, vararg args: Any): String =
        Regex("%(\\d+)\\$[sd]").replace(template) { m -> args.getOrNull(m.groupValues[1].toInt() - 1)?.toString() ?: m.value }

    /** French and Portuguese call 0 singular too; the other languages of the app only 1. */
    private fun plural(forms: Pair<String, String>, count: Int): String =
        if (count == 1 || (count == 0 && language in setOf("fr", "pt"))) forms.first else forms.second

    override fun labels(weightUnit: String): PrescriptionLabels = PrescriptionLabels(
        max = t.max,
        totalReps = { fill(t.totalReps, it) },
        bodyweight = t.bodyweight,
        weight = { kg -> formatWeight(kg, weightUnit) },
        technique = { technique(it) }
    )

    override fun technique(technique: Technique): String = when (technique) {
        is Technique.Pause -> fill(t.pause, technique.seconds)
        Technique.Chains -> t.chains
        Technique.Bands -> t.bands
        Technique.FeetUp -> t.feetUp
        Technique.Competition -> t.competition
        is Technique.Tempo -> fill(t.tempo, technique.pattern)
        Technique.Deficit -> t.deficit
        Technique.Emom -> t.emom
        is Technique.Custom -> technique.text
    }

    override fun setPrescriptionBadges(set: SetLogEntity): List<String> = buildList {
        set.targetPercent?.let { add("${PrescriptionFormatter.number(it)}%") }
        set.targetRpe?.let { add("@" + PrescriptionFormatter.number(it)) }
        when (set.repMode) {
            "amrap" -> add(t.max)
            "total" -> set.targetTotalReps?.let { add(fill(t.totalReps, it)) }
        }
        TechniqueCodec.decode(set.techniques).forEach { add(technique(it)) }
        if (set.isExtra) add(t.extra)
    }

    override fun setPerformanceBadges(set: SetLogEntity): List<String> =
        performanceBadges(set.parts, set.pauses, set.assistedReps, set.partialReps, set.performed)

    override fun performanceBadges(parts: List<Int>, pauses: List<Int>, assisted: Int, partial: Int, performed: List<Technique>?): List<String> = buildList {
        if (parts.size > 1) {
            val text = parts.joinToString("+")
            add(if (pauses.isEmpty()) text else fill(plural(t.parts, pauses.size), text, pauses.joinToString(", ") { "$it″" }))
        }
        if (assisted > 0) add(fill(plural(t.assisted, assisted), assisted))
        if (partial > 0) add(fill(plural(t.partial, partial), partial))
        performed?.let { list ->
            add(if (list.isEmpty()) t.performedNone else fill(t.performed, list.joinToString(" · ") { technique(it) }))
        }
    }

    override fun rpeBadge(value: Float, scale: RpeScale): String =
        if (scale == RpeScale.BASIC) t.levels.getValue(RpeLevel.of(value))
        else fill(t.rpeShort, PrescriptionFormatter.number(value))
}
