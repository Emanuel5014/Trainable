package com.emanuel5014.trainable.util

/** How much the +/- buttons of the rest and warmup timers (and of their notifications) move the countdown. */
object TimerAdjustment {
    const val DEFAULT_ADD_SECONDS = 30
    const val DEFAULT_SUBTRACT_SECONDS = 10

    /** What the settings offer; a stored value outside these is still honoured. */
    val ADD_OPTIONS = listOf(10, 15, 30, 45, 60)
    val SUBTRACT_OPTIONS = listOf(5, 10, 15, 30)

    /** Anything unreadable or silly falls back to the default, so a timer button never does nothing. */
    fun sanitizeAdd(seconds: Int?): Int = seconds?.takeIf { it in 1..600 } ?: DEFAULT_ADD_SECONDS
    fun sanitizeSubtract(seconds: Int?): Int = seconds?.takeIf { it in 1..600 } ?: DEFAULT_SUBTRACT_SECONDS
}
