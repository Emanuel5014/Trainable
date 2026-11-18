package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@Composable
internal actual fun imeVisible(): Boolean = WindowInsets.ime.getBottom(LocalDensity.current) > 0
