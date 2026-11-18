package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration

@Composable actual fun workoutWindowWidthDp(): Int = LocalConfiguration.current.screenWidthDp
@Composable actual fun workoutMediaThumbSize(): Dp {
    if (LocalConfiguration.current.screenHeightDp < 480) return 44.dp
    return (LocalConfiguration.current.screenWidthDp * 0.145f).dp.coerceIn(44.dp, 76.dp)
}
