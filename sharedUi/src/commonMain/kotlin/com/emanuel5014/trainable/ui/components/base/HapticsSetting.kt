package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** The user's haptic feedback switch, provided once at the root of the app; buttons vibrate only while it is on. */
val LocalHapticsSetting = staticCompositionLocalOf<Flow<Boolean>> { flowOf(true) }
