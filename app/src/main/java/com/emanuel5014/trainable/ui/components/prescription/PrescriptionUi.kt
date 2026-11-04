package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.data.local.entity.parts
import com.emanuel5014.trainable.data.local.entity.pauses
import com.emanuel5014.trainable.data.local.entity.performed
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.domain.prescription.TechniqueCodec
import com.emanuel5014.trainable.domain.rpe.RpeScale


fun prescriptionLabels(context: android.content.Context, weightUnit: String): PrescriptionLabels = PrescriptionLabels(
    max = context.getString(R.string.max_label),
    totalReps = { context.getString(R.string.total_reps_label, it) },
    bodyweight = context.getString(R.string.bodyweight_short),
    weight = { kg -> formatWeight(kg, weightUnit) },
    technique = { techniqueLabel(context, it) }
)

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

/** Prescription of a logged set as plain text for exports ("75% · MAX · Pause 2″ · 2+1+1"), without RPE. */
fun setLogPrescriptionText(context: android.content.Context, set: SetLogEntity): String? =
    (setLogPrescriptionBadges(context, set) + setLogPerformanceBadges(context, set))
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

/** Badges describing the prescription snapshot of a logged set ("75%", "MAX", "Pause 2″", "EXTRA"); its RPE is shown apart. */
fun setLogPrescriptionBadges(context: android.content.Context, set: SetLogEntity): List<String> = buildList {
    set.targetPercent?.let { add("${PrescriptionFormatter.number(it)}%") }
    set.targetRpe?.let { add("@" + PrescriptionFormatter.number(it)) }
    when (set.repMode) {
        "amrap" -> add(context.getString(R.string.max_label))
        "total" -> set.targetTotalReps?.let { add(context.getString(R.string.total_reps_label, it)) }
    }
    TechniqueCodec.decode(set.techniques).forEach { add(techniqueLabel(context, it)) }
    if (set.isExtra) add(context.getString(R.string.extra_badge))
}

/** The words of prescriptions and sets from the app's resources (the same functions as notifications and files). */
class AndroidPrescriptionText(private val context: android.content.Context) : PrescriptionText {
    override fun labels(weightUnit: String): PrescriptionLabels = prescriptionLabels(context, weightUnit)
    override fun technique(technique: Technique): String = techniqueLabel(context, technique)
    override fun setPrescriptionBadges(set: SetLogEntity): List<String> = setLogPrescriptionBadges(context, set)
    override fun setPerformanceBadges(set: SetLogEntity): List<String> = setLogPerformanceBadges(context, set)
    override fun rpeBadge(value: Float, scale: RpeScale): String = rpeBadgeText(context, value, scale)
}
