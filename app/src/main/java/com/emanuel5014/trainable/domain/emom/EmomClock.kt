package com.emanuel5014.trainable.domain.emom

enum class EmomPhase {
    /** No run on the clock yet. */
    Idle,
    /** Get-ready countdown before the first round. */
    LeadIn,
    /** A round is running and its set is not logged yet. */
    Work,
    /** The round's set is logged: the rest of the minute is rest. */
    Rest
}

data class EmomSnapshot(
    val phase: EmomPhase,
    /** 1-based round of the minute on the clock (the first round during the lead-in). */
    val round: Int,
    val totalRounds: Int,
    /** Seconds until the next round starts (until GO during the lead-in). */
    val secondsLeft: Int,
    /** Fraction of the current minute (or of the lead-in) already elapsed, 0..1. */
    val progress: Float
)

/**
 * The minute clock behind an EMOM ("every minute on the minute") run.
 *
 * The run is driven by one elapsed-seconds counter, the same one timed sets use. It starts
 * [LEAD_IN_SECONDS] below zero so the lifter gets a get-ready countdown, and a new round begins
 * every [MINUTE_SECONDS] from zero.
 */
object EmomClock {
    const val MINUTE_SECONDS = 60
    const val LEAD_IN_SECONDS = 10
    const val START_ELAPSED = -LEAD_IN_SECONDS

    /** Seconds before every round that tick to get the lifter ready. */
    const val COUNTDOWN_CUE_SECONDS = 3

    /** Seconds after a round starts during which the screen shouts GO. */
    const val GO_FLASH_SECONDS = 3

    /** Zero-based minute on the clock; -1 during the lead-in. */
    fun minuteIndex(elapsed: Int): Int = Math.floorDiv(elapsed, MINUTE_SECONDS)

    fun secondsIntoMinute(elapsed: Int): Int = Math.floorMod(elapsed, MINUTE_SECONDS)

    /** Seconds until the next round starts, counting the lead-in as a short minute of its own. */
    fun secondsLeft(elapsed: Int): Int = MINUTE_SECONDS - secondsIntoMinute(elapsed)

    /** True while [elapsed] sits in the last [COUNTDOWN_CUE_SECONDS] seconds before a round. */
    fun isCountdownTick(elapsed: Int): Boolean = secondsLeft(elapsed) in 1..COUNTDOWN_CUE_SECONDS

    /** True right after a round starts, while the screen flashes GO. */
    fun isGoFlash(elapsed: Int): Boolean = elapsed >= 0 && secondsIntoMinute(elapsed) < GO_FLASH_SECONDS

    /** Length of the run that starts at [startIndex]: consecutive EMOM sets, [isEmom] one flag per set. */
    fun runLength(isEmom: List<Boolean>, startIndex: Int): Int {
        if (startIndex !in isEmom.indices) return 0
        var length = 0
        while (startIndex + length < isEmom.size && isEmom[startIndex + length]) length++
        return length
    }

    fun snapshot(elapsed: Int, started: Boolean, roundsLogged: Int, totalRounds: Int): EmomSnapshot {
        if (!started) {
            return EmomSnapshot(EmomPhase.Idle, round = 1, totalRounds = totalRounds, secondsLeft = MINUTE_SECONDS, progress = 0f)
        }
        if (elapsed < 0) {
            val left = (-elapsed).coerceAtMost(LEAD_IN_SECONDS)
            return EmomSnapshot(
                phase = EmomPhase.LeadIn,
                round = 1,
                totalRounds = totalRounds,
                secondsLeft = left,
                progress = 1f - left / LEAD_IN_SECONDS.toFloat()
            )
        }
        val minute = minuteIndex(elapsed)
        return EmomSnapshot(
            phase = if (roundsLogged > minute) EmomPhase.Rest else EmomPhase.Work,
            round = (minute + 1).coerceIn(1, totalRounds.coerceAtLeast(1)),
            totalRounds = totalRounds,
            secondsLeft = secondsLeft(elapsed),
            progress = secondsIntoMinute(elapsed) / MINUTE_SECONDS.toFloat()
        )
    }
}
