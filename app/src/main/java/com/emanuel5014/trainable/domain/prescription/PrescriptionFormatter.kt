package com.emanuel5014.trainable.domain.prescription

import java.util.Locale

/** Localizable labels used when rendering blocks for humans. Defaults are compact English. */
data class PrescriptionLabels(
    val max: String = "MAX",
    val totalReps: (Int) -> String = { "$it reps" },
    val bodyweight: String = "BW",
    val weight: (Float) -> String = { "${PrescriptionFormatter.number(it)} kg" },
    val technique: (Technique) -> String = PrescriptionFormatter::defaultTechniqueLabel
)

object PrescriptionFormatter {

    fun number(value: Float): String {
        val rounded = kotlin.math.round(value * 100) / 100f
        return if (rounded % 1f == 0f) rounded.toInt().toString()
        else String.format(Locale.ROOT, "%.2f", rounded).trimEnd('0').trimEnd('.')
    }

    fun intensity(block: PrescriptionBlock, labels: PrescriptionLabels = PrescriptionLabels()): String? =
        intensity(block.intensityType, block.intensityValue, labels)

    fun intensity(type: IntensityType, value: Float?, labels: PrescriptionLabels = PrescriptionLabels()): String? =
        when (type) {
            IntensityType.NONE -> null
            IntensityType.PERCENT -> value?.let { "${number(it)}%" }
            IntensityType.RPE -> value?.let { "RPE ${number(it)}" }
            IntensityType.WEIGHT -> value?.let { labels.weight(it) }
            IntensityType.BODYWEIGHT -> if (value != null && value > 0f) "${labels.bodyweight} +${labels.weight(value)}" else labels.bodyweight
        }

    fun volume(block: PrescriptionBlock, labels: PrescriptionLabels = PrescriptionLabels()): String = when (block.repMode) {
        RepMode.TOTAL -> labels.totalReps(block.totalReps ?: 0)
        RepMode.AMRAP -> if (block.sets <= 1) labels.max else "${block.sets}×${labels.max}"
        RepMode.FIXED -> {
            val list = block.repsList
            when {
                list.size > 1 -> list.joinToString("-")
                list.isEmpty() -> if (block.sets > 1) "${block.sets}×?" else "?"
                block.sets <= 1 -> list[0].toString()
                else -> "${block.sets}×${list[0]}"
            }
        }
    }

    /** "75% · 3×5" — intensity and volume only, techniques are rendered as separate chips. */
    fun headline(block: PrescriptionBlock, labels: PrescriptionLabels = PrescriptionLabels()): String =
        listOfNotNull(intensity(block, labels), volume(block, labels)).joinToString(" · ")

    /** One-line summary with techniques, e.g. `75% 3×5 Pause 2" · 70% 6`. */
    fun summary(blocks: List<PrescriptionBlock>, labels: PrescriptionLabels = PrescriptionLabels()): String =
        blocks.joinToString(" · ") { block ->
            (listOfNotNull(intensity(block, labels), volume(block, labels)) + block.techniques.map(labels.technique))
                .joinToString(" ")
        }

    /** Re-parsable notation for the "write it like your notebook" field and exports. */
    fun notation(blocks: List<PrescriptionBlock>): String = blocks.joinToString("  ") { block ->
        val intensity = when (block.intensityType) {
            IntensityType.NONE -> null
            IntensityType.PERCENT -> block.intensityValue?.let { "${number(it)}%" }
            IntensityType.RPE -> block.intensityValue?.let { "@RPE${number(it)}" }
            IntensityType.WEIGHT -> block.intensityValue?.let { "${number(it)}kg" }
            IntensityType.BODYWEIGHT -> block.intensityValue?.takeIf { it > 0f }?.let { "BW ${number(it)}kg" } ?: "BW"
        }
        val volume = when (block.repMode) {
            RepMode.TOTAL -> "${block.totalReps ?: 0} REP ALSAP"
            RepMode.AMRAP -> "${block.sets.coerceAtLeast(1)}xMAX"
            RepMode.FIXED -> {
                val list = block.repsList
                when {
                    list.size > 1 -> list.joinToString("-")
                    list.isEmpty() -> "${block.sets.coerceAtLeast(1)}x0"
                    else -> "${block.sets.coerceAtLeast(1)}x${list[0]}"
                }
            }
        }
        (listOfNotNull(intensity, volume) + block.techniques.map(::notationTechnique)).joinToString(" ")
    }

    fun defaultTechniqueLabel(technique: Technique): String = when (technique) {
        is Technique.Pause -> "Pause ${technique.seconds}\""
        Technique.Chains -> "Chains"
        Technique.Bands -> "Bands"
        Technique.FeetUp -> "Feet up"
        Technique.Competition -> "Comp"
        is Technique.Tempo -> "Tempo ${technique.pattern}"
        Technique.Deficit -> "Deficit"
        Technique.Emom -> "EMOM"
        is Technique.Custom -> technique.text
    }

    private fun notationTechnique(technique: Technique): String = when (technique) {
        is Technique.Pause -> "STOP ${technique.seconds}\""
        Technique.Chains -> "CHAINS"
        Technique.Bands -> "BANDS"
        Technique.FeetUp -> "FEET UP"
        Technique.Competition -> "COMP"
        is Technique.Tempo -> "TEMPO ${technique.pattern}"
        Technique.Deficit -> "DEFICIT"
        Technique.Emom -> "EMOM"
        is Technique.Custom -> "(${technique.text})"
    }
}
