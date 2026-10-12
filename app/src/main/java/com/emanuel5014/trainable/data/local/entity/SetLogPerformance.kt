package com.emanuel5014.trainable.data.local.entity

import com.emanuel5014.trainable.domain.prescription.SetPerformance
import com.emanuel5014.trainable.domain.prescription.Technique
import com.emanuel5014.trainable.domain.prescription.TechniqueCodec

/** The parts of a set done after short pauses; empty for a set done in one go. */
val SetLogEntity.parts: List<Int> get() = SetPerformance.decodeParts(repParts).takeIf { it.size > 1 }.orEmpty()

val SetLogEntity.pauses: List<Int> get() = SetPerformance.decodePauses(intraRests)

/** Reps done in one go: the first part of a set in parts, all of them otherwise. */
val SetLogEntity.straightReps: Int get() = parts.firstOrNull() ?: repsEffettive

val SetLogEntity.isInParts: Boolean get() = parts.isNotEmpty()

/** Stored for a set done without any of its planned techniques (an empty list would read as "as planned"). */
private const val NO_TECHNIQUE = "none"

/** The techniques the set was really done with, null when they were the planned ones. */
val SetLogEntity.performed: List<Technique>? get() = decodePerformed(performedTechniques)

fun encodePerformed(techniques: List<Technique>?): String? = when {
    techniques == null -> null
    techniques.isEmpty() -> NO_TECHNIQUE
    else -> TechniqueCodec.encode(techniques)
}

fun decodePerformed(raw: String?): List<Technique>? = when (raw) {
    null -> null
    NO_TECHNIQUE -> emptyList()
    else -> TechniqueCodec.decode(raw)
}
