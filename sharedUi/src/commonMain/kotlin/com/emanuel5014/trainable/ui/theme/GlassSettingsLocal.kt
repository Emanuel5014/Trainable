package com.emanuel5014.trainable.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.emanuel5014.trainable.data.model.GlassSettings

/** The user's Liquid Glass choices, provided once at the root of the app. */
val LocalGlassSettings = staticCompositionLocalOf { GlassSettings() }
