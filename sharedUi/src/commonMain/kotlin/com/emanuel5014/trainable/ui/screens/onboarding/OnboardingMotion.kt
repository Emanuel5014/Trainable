package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.runtime.Composable

/** True when the system asks for no animations (Android: animator scale 0; iOS: Reduce Motion). */
@Composable
expect fun rememberReduceMotion(): Boolean
