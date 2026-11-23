package com.emanuel5014.trainable.ui.screens.physicalcheck

import com.emanuel5014.trainable.data.local.entity.PhysicalCheckEntity
import com.emanuel5014.trainable.platform.formatLocalDecimals
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.round
import kotlin.time.Instant

/** The photo files of a check, in the order they were added (the entity keeps them comma separated). */
fun PhysicalCheckEntity.photoFilenames(): List<String> =
    if (fotoFilenames.isEmpty()) emptyList() else fotoFilenames.split(",").filter { it.isNotBlank() }

/**
 * The weight change (kg) of each weighed check against the closest older weighed check, by check id. A check
 * without weight, or the oldest weighed one, has no entry. [checks] may come in any order.
 */
fun weightChangesKg(checks: List<PhysicalCheckEntity>): Map<Int, Float> {
    val weighed = checks.filter { it.peso != null }.sortedWith(compareBy({ it.timestamp }, { it.id }))
    return weighed.zipWithNext { older, newer -> newer.id to (newer.peso!! - older.peso!!) }.toMap()
}

/** One month of the list: its checks, newest first, and the sum of their weight changes (null when none has one). */
data class PhysicalCheckMonth(val year: Int, val month: Int, val checks: List<PhysicalCheckEntity>, val weightChangeKg: Float?)

/**
 * Groups [checks] (newest first, as the database gives them) by local month, keeping the order. The month's change
 * is the sum of its checks' [changes], so it reads as the weight gained or lost from the check before the month.
 */
fun physicalCheckMonths(checks: List<PhysicalCheckEntity>, changes: Map<Int, Float>, timeZone: TimeZone): List<PhysicalCheckMonth> {
    val groups = mutableListOf<Pair<Pair<Int, Int>, MutableList<PhysicalCheckEntity>>>()
    for (c in checks) {
        val date = Instant.fromEpochMilliseconds(c.timestamp).toLocalDateTime(timeZone)
        val key = date.year to date.month.ordinal + 1
        if (groups.lastOrNull()?.first == key) groups.last().second.add(c) else groups.add(key to mutableListOf(c))
    }
    return groups.map { (key, list) ->
        val monthChanges = list.mapNotNull { changes[it.id] }
        PhysicalCheckMonth(key.first, key.second, list, if (monthChanges.isEmpty()) null else monthChanges.sum())
    }
}

/** "78,4 kg" in the user's unit, one decimal at most. */
fun formatBodyWeight(kg: Float, weightUnit: String): String =
    WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(kg, weightUnit), weightUnit)

/**
 * A weight change with its sign in the user's unit, one decimal: "−0,6 kg", "+1,2 lb", "±0 kg" when it rounds to
 * zero. The minus is the real minus sign, so it lines up with the plus.
 */
fun formatWeightChange(kg: Float, weightUnit: String): String {
    val value = WeightUnitConverter.convertDisplay(kg, weightUnit).toDouble()
    val rounded = round(value * 10) / 10
    val unit = if (weightUnit == "lb") "lb" else "kg"
    if (rounded == 0.0) return "±0 $unit"
    val sign = if (rounded < 0) "−" else "+"
    val digits = if (rounded == round(rounded)) 0 else 1
    return "$sign${formatLocalDecimals(abs(rounded), digits)} $unit"
}

/** Whole days between two timestamps, counted on the local calendar (never negative). */
fun daysBetween(fromMillis: Long, toMillis: Long, timeZone: TimeZone): Int {
    val a = Instant.fromEpochMilliseconds(minOf(fromMillis, toMillis)).toLocalDateTime(timeZone).date
    val b = Instant.fromEpochMilliseconds(maxOf(fromMillis, toMillis)).toLocalDateTime(timeZone).date
    return (b.toEpochDays() - a.toEpochDays()).toInt()
}
