package com.emanuel5014.trainable.domain.journal

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * A tag's name is written in its colour, so a colour picked freely must still read on the surface it sits on: pure
 * blue on a dark card does not. [readableOn] moves such a colour toward white (dark surface) or black (light surface)
 * just far enough, and leaves a colour that already reads untouched.
 */
object TagContrast {
    /** WCAG AA for text. */
    const val MIN_RATIO = 4.5

    private const val STEPS = 20

    fun readableOn(argb: Int, backgroundArgb: Int): Int {
        if (ratio(argb, backgroundArgb) >= MIN_RATIO) return argb
        val target = if (luminance(backgroundArgb) < 0.5) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        for (step in 1..STEPS) {
            val candidate = mix(argb, target, step.toFloat() / STEPS)
            if (ratio(candidate, backgroundArgb) >= MIN_RATIO) return candidate
        }
        return target
    }

    /** WCAG contrast ratio, 1 (none) to 21 (black on white). */
    fun ratio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** WCAG relative luminance of an sRGB colour, 0 (black) to 1 (white). */
    fun luminance(argb: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((argb shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private fun mix(from: Int, to: Int, t: Float): Int {
        fun channel(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + (b - a) * t).roundToInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
