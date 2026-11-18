package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the optional "advanced programming" features (%1RM prescriptions, techniques, weekly
 * programs, 1RM tracking, RPE logging) are switched on in Settings. Provided once at the app root
 * so any screen or component can hide its powerlifting UI without threading a flag through.
 *
 * Turning the switch off never deletes data: it only stops the app from showing or applying it.
 */
val LocalAdvancedProgramming = staticCompositionLocalOf { false }
