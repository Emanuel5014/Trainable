package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** Draws a routine's photo from its stored path, cropped to [Modifier]; null where photos cannot be shown yet (iOS). */
val LocalPlanImage = staticCompositionLocalOf<(@Composable (uri: String, modifier: Modifier) -> Unit)?> { null }
