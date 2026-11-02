package com.emanuel5014.trainable.domain.exercise

import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.TIME_AND_WEIGHT

/**
 * The tracking type (weight × reps, time & weight, time & reps) an exercise was last added with, so adding it again
 * opens on that type instead of making the lifter pick it every time. It only holds the exercises that are not plain
 * weight × reps: that is what the selector opens on for the others. Cardio (told by its category) and advanced (it
 * needs its blocks) are never remembered.
 *
 * Stored as one text, `id:type,id:type`, the most recent last.
 */
object ExerciseTypeMemory {
    const val STRENGTH = "strength"
    const val MAX_ENTRIES = 1000

    private val remembered = setOf(TIME_AND_WEIGHT, TIME_AND_REPS)

    /** The type to open the selector on for [exerciseId]. */
    fun typeFor(memory: Map<Int, String>, exerciseId: Int): String = memory[exerciseId] ?: STRENGTH

    /**
     * [memory] with the choice of [type] for [exerciseId] (the newest entry); plain strength forgets the exercise, and a
     * type that is not a tracking type changes nothing.
     */
    fun remember(memory: Map<Int, String>, exerciseId: Int, type: String): Map<Int, String> {
        if (type != STRENGTH && type !in remembered) return memory
        val next = LinkedHashMap(memory)
        next.remove(exerciseId)
        if (type != STRENGTH) next[exerciseId] = type
        while (next.size > MAX_ENTRIES) next.remove(next.keys.first())
        return next
    }

    fun encode(memory: Map<Int, String>): String = memory.entries.joinToString(",") { "${it.key}:${it.value}" }

    /** What [encode] wrote; an entry that cannot be read is left out, the others are kept. */
    fun decode(text: String?): Map<Int, String> {
        if (text.isNullOrBlank()) return emptyMap()
        val memory = LinkedHashMap<Int, String>()
        text.split(',').forEach { entry ->
            val id = entry.substringBefore(':', "").trim().toIntOrNull() ?: return@forEach
            val type = entry.substringAfter(':', "").trim()
            if (type in remembered) memory[id] = type
        }
        return memory
    }
}
