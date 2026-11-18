package com.emanuel5014.trainable.domain.exercise

/**
 * A duration typed as plain digits that fill from the right, like the Android timer: the last two are the seconds,
 * the two before the minutes, the rest the hours (`3215` is 32:15, `13000` is 1:30:00). At most six digits, so the
 * longest duration is 99:59:59; a place typed above 59 (`90` seconds) is carried over when the value is read.
 */
object DurationDigits {
    const val MAX_DIGITS = 6
    const val MAX_SECONDS = 99 * 3600 + 59 * 60 + 59

    /** Only the digits of [input], without leading zeros, the first [MAX_DIGITS] of them (more typing is ignored). */
    fun clean(input: String): String = input.filter { it in '0'..'9' }.trimStart('0').take(MAX_DIGITS)

    /** The seconds the digits stand for, places above 59 carried over. */
    fun seconds(digits: String): Int {
        val (h, m, s) = places(clean(digits))
        return (h * 3600 + m * 60 + s).coerceAtMost(MAX_SECONDS)
    }

    /** The duration read as a clock, normalised: `90` is "1:30", `13000` is "1:30:00", nothing is "0:00". */
    fun display(digits: String): String = format(seconds(digits))

    /** The digits as typed, grouped like a clock but not normalised (`90` is "0:90"); empty for no digits. */
    fun grouped(digits: String): String {
        val c = clean(digits)
        if (c.isEmpty()) return ""
        val (h, m, s) = places(c)
        return if (h > 0) "$h:${pad2(m)}:${pad2(s)}" else "$m:${pad2(s)}"
    }

    /** The digits that type [seconds] in, normalised (90 s is `130`); empty for zero. */
    fun digitsOf(seconds: Int): String {
        val total = seconds.coerceIn(0, MAX_SECONDS)
        if (total == 0) return ""
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return "$h${pad2(m)}${pad2(s)}".trimStart('0')
    }

    /** "m:ss", or "h:mm:ss" from one hour. */
    fun format(seconds: Int): String {
        val total = seconds.coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "$h:${pad2(m)}:${pad2(s)}" else "$m:${pad2(s)}"
    }

    private fun places(clean: String): Triple<Int, Int, Int> {
        val padded = clean.padStart(MAX_DIGITS, '0')
        return Triple(padded.substring(0, 2).toInt(), padded.substring(2, 4).toInt(), padded.substring(4, 6).toInt())
    }

    private fun pad2(value: Int): String = value.toString().padStart(2, '0')
}
