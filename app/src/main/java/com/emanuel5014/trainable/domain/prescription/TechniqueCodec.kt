package com.emanuel5014.trainable.domain.prescription

/**
 * Stable string encoding for techniques, used by Room columns and the .trainableplan export.
 * Example: `pause:2|chains|feet_up|tempo:3-1-0|custom:TENS 2"`.
 */
object TechniqueCodec {
    private const val SEPARATOR = "|"

    fun encode(techniques: List<Technique>): String? {
        if (techniques.isEmpty()) return null
        return techniques.joinToString(SEPARATOR) { encodeOne(it) }
    }

    fun decode(raw: String?): List<Technique> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(SEPARATOR).mapNotNull { decodeOne(it.trim()) }
    }

    private fun encodeOne(technique: Technique): String = when (technique) {
        is Technique.Pause -> "pause:${technique.seconds}"
        Technique.Chains -> "chains"
        Technique.Bands -> "bands"
        Technique.FeetUp -> "feet_up"
        Technique.Competition -> "competition"
        is Technique.Tempo -> "tempo:${technique.pattern.replace(SEPARATOR, "/")}"
        Technique.Deficit -> "deficit"
        Technique.Emom -> "emom"
        is Technique.Custom -> "custom:${technique.text.replace(SEPARATOR, "/")}"
    }

    private fun decodeOne(token: String): Technique? {
        if (token.isEmpty()) return null
        val key = token.substringBefore(':')
        val value = token.substringAfter(':', "")
        return when (key) {
            "pause" -> Technique.Pause(value.toIntOrNull()?.coerceAtLeast(1) ?: 1)
            "chains" -> Technique.Chains
            "bands" -> Technique.Bands
            "feet_up" -> Technique.FeetUp
            "competition" -> Technique.Competition
            "tempo" -> value.takeIf { it.isNotBlank() }?.let { Technique.Tempo(it) }
            "deficit" -> Technique.Deficit
            "emom" -> Technique.Emom
            "custom" -> value.takeIf { it.isNotBlank() }?.let { Technique.Custom(it) }
            else -> Technique.Custom(token)
        }
    }
}

/** CSV codec for `plan_exercises.excluded_weeks` ("4,8"). */
object WeekSetCodec {
    fun encode(weeks: Set<Int>): String? =
        weeks.filter { it > 0 }.sorted().takeIf { it.isNotEmpty() }?.joinToString(",")

    fun decode(raw: String?): Set<Int> =
        raw?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.filter { it > 0 }?.toSet() ?: emptySet()
}
