package com.emanuel5014.trainable.data.model

import androidx.compose.runtime.Immutable

/** The parts of the app that can be drawn in Liquid Glass, each switched on or off by the user. */
enum class GlassPart {
    /** The floating navigation bar. */
    NavBar,

    /** The neutral round buttons of the screen headers (settings, filters, back). */
    HeaderButtons,

    /** The floating action buttons (quick workout, new routine and the like). */
    ActionButtons
}

/** The user's Liquid Glass choices from Personalization. Off by default: the app then looks as it always did. */
@Immutable
data class GlassSettings(
    val enabled: Boolean = false,
    /** 0 is a nearly solid surface with a faint lens, 1 the clearest, most bending glass. */
    val intensity: Float = DEFAULT_INTENSITY,
    val parts: Set<GlassPart> = GlassPart.entries.toSet()
) {
    fun isOn(part: GlassPart): Boolean = enabled && part in parts

    companion object {
        const val DEFAULT_INTENSITY = 0.6f
    }
}
