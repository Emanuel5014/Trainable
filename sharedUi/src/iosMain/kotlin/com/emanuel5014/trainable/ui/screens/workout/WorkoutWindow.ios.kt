package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.ResponsiveSize

@Composable actual fun workoutWindowWidthDp(): Int = ResponsiveSize.screenWidthDp
@Composable actual fun workoutMediaThumbSize(): Dp {
    if (ResponsiveSize.screenHeightDp < 480) return 44.dp
    return (ResponsiveSize.screenWidthDp * 0.145f).dp.coerceIn(44.dp, 76.dp)
}
