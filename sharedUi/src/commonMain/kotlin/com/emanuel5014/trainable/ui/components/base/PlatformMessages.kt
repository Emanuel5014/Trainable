package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/** Shows a short note that goes away by itself (a toast on Android); nothing where the platform has none yet. */
val LocalShowMessage = staticCompositionLocalOf<(String) -> Unit> { {} }
