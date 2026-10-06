package com.emanuel5014.trainable.util

import kotlin.math.abs
import kotlin.math.round

/**
 * Works out which plates to put on each side of a bar for a target weight. Everything here is in the user's
 * display unit (kg or lb) and never converts: the caller converts the stored kg weight first.
 */
object PlateCalculator {
    const val DEFAULT_BAR_KG = 20f
    const val DEFAULT_BAR_LB = 45f

    val BAR_PRESETS_KG = listOf(20f, 15f, 10f)
    val BAR_PRESETS_LB = listOf(45f, 35f, 25f)

    /** Plates the user can switch on or off in the settings, biggest first. */
    val PLATE_CHOICES_KG = listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f, 1f, 0.5f)
    val PLATE_CHOICES_LB = listOf(45f, 35f, 25f, 10f, 5f, 2.5f, 1.25f)

    val DEFAULT_PLATES_KG = listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f)
    val DEFAULT_PLATES_LB = listOf(45f, 35f, 25f, 10f, 5f, 2.5f)

    fun defaultBar(unit: String): Float = if (unit == "lb") DEFAULT_BAR_LB else DEFAULT_BAR_KG
    fun barPresets(unit: String): List<Float> = if (unit == "lb") BAR_PRESETS_LB else BAR_PRESETS_KG
    fun plateChoices(unit: String): List<Float> = if (unit == "lb") PLATE_CHOICES_LB else PLATE_CHOICES_KG
    fun defaultPlates(unit: String): List<Float> = if (unit == "lb") DEFAULT_PLATES_LB else DEFAULT_PLATES_KG

    /**
     * @property platesPerSide plates for one side, heaviest first (the other side mirrors it).
     * @property missing weight in total, over both sides, that the available plates cannot make up (0 when exact).
     * @property belowBar the target is lighter than the bar itself.
     */
    data class Result(
        val platesPerSide: List<Float>,
        val missing: Float,
        val belowBar: Boolean
    ) {
        val isExact: Boolean get() = !belowBar && missing == 0f
    }

    /** Weight of a bar with [platesPerSide] on both sides. */
    fun total(bar: Float, platesPerSide: List<Float>): Float =
        (hundredths(bar) + 2 * platesPerSide.sumOf { hundredths(it) }) / 100f

    /**
     * Loads [total] with the fewest plates per side. Between solutions with the same number of plates the one
     * with fewer different plates wins (20 + 20 rather than 25 + 15), then the heavier plates go first. When the
     * target cannot be reached exactly the closest weight below it is loaded and the rest is reported as missing.
     */
    fun solve(total: Float, bar: Float, available: List<Float>): Result {
        val target = hundredths(total) - hundredths(bar)
        if (target < 0) return Result(emptyList(), 0f, belowBar = true)

        // A plate counts twice, once per side
        val plates = available.map { hundredths(it) * 2 }.filter { it > 0 }.distinct().sortedDescending()
        if (plates.isEmpty() || target == 0) return Result(emptyList(), target / 100f, belowBar = false)

        val unit = plates.reduce(::gcd)
        val size = target / unit
        if (size > MAX_STATES) return Result(emptyList(), target / 100f, belowBar = false)

        val coins = plates.map { it / unit }
        val fewest = IntArray(size + 1) { UNREACHABLE }.also { it[0] = 0 }
        for (amount in 1..size) {
            for (coin in coins) {
                if (coin <= amount && fewest[amount - coin] != UNREACHABLE) {
                    fewest[amount] = minOf(fewest[amount], fewest[amount - coin] + 1)
                }
            }
        }

        var reached = size
        while (fewest[reached] == UNREACHABLE) reached--
        val loaded = bestCombination(reached, coins, fewest).map { it * unit / 2 }
        val missing = target - reached * unit
        return Result(loaded.map { it / 100f }, missing / 100f, belowBar = false)
    }

    /** Among the combinations that reach [amount] with the fewest plates, the one with the fewest distinct plates. */
    private fun bestCombination(amount: Int, coins: List<Int>, fewest: IntArray): List<Int> {
        var best: List<Int>? = null
        val current = ArrayList<Int>()

        fun better(candidate: List<Int>): Boolean {
            val known = best ?: return true
            val candidateKinds = candidate.distinct().size
            val knownKinds = known.distinct().size
            if (candidateKinds != knownKinds) return candidateKinds < knownKinds
            // Same number of kinds: heavier plates first (both lists are heaviest first)
            for (i in candidate.indices) {
                if (candidate[i] != known[i]) return candidate[i] > known[i]
            }
            return false
        }

        fun walk(remaining: Int, maxCoinIndex: Int) {
            if (remaining == 0) {
                if (better(current)) best = current.toList()
                return
            }
            // Plates are taken heaviest first, so a combination is only visited once
            for (index in maxCoinIndex until coins.size) {
                val coin = coins[index]
                if (coin <= remaining && fewest[remaining - coin] == fewest[remaining] - 1) {
                    current.add(coin)
                    walk(remaining - coin, index)
                    current.removeAt(current.lastIndex)
                }
            }
        }

        walk(amount, 0)
        return best.orEmpty()
    }

    /** Whole hundredths avoid the float noise of adding 1.25 and 2.5 a few times. */
    private fun hundredths(value: Float): Int = round(value * 100f).toInt()

    private fun gcd(a: Int, b: Int): Int = if (b == 0) abs(a) else gcd(b, a % b)

    private const val UNREACHABLE = Int.MAX_VALUE
    private const val MAX_STATES = 400_000
}
