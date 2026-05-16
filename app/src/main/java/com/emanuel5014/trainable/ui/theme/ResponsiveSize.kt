package com.emanuel5014.trainable.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Stable
object ResponsiveSize {
    var screenWidthDp: Int = 360
        private set

    val isCompact: Boolean get() = screenWidthDp < 360
    val isNarrow: Boolean get() = screenWidthDp < 400

    val horizontalPadding: Dp get() = when {
        screenWidthDp < 360 -> 16.dp
        screenWidthDp < 400 -> 20.dp
        else -> 24.dp
    }

    val cardPadding: Dp get() = when {
        screenWidthDp < 360 -> 16.dp
        else -> 24.dp
    }

    val screenHeaderLineHeightSp: TextUnit get() = when {
        screenWidthDp < 360 -> 28.sp
        screenWidthDp < 400 -> 34.sp
        else -> 40.sp
    }

    val displaySmallSize: TextUnit get() = when {
        screenWidthDp < 360 -> 24.sp
        screenWidthDp < 400 -> 30.sp
        else -> 36.sp
    }

    val headlineLargeSize: TextUnit get() = when {
        screenWidthDp < 360 -> 18.sp
        screenWidthDp < 400 -> 22.sp
        else -> 28.sp
    }

    val headlineMediumSize: TextUnit get() = when {
        screenWidthDp < 360 -> 20.sp
        screenWidthDp < 400 -> 24.sp
        else -> 28.sp
    }

    val titleLargeSize: TextUnit get() = when {
        screenWidthDp < 360 -> 16.sp
        screenWidthDp < 400 -> 18.sp
        else -> 22.sp
    }

    val titleMediumSize: TextUnit get() = when {
        screenWidthDp < 360 -> 14.sp
        screenWidthDp < 400 -> 16.sp
        else -> 16.sp
    }

    val labelLargeSize: TextUnit get() = when {
        screenWidthDp < 360 -> 12.sp
        screenWidthDp < 400 -> 13.sp
        else -> 14.sp
    }

    fun update(widthDp: Int) {
        screenWidthDp = widthDp
    }

    fun responsiveFontSize(original: TextUnit): TextUnit = when {
        original == 36.sp -> displaySmallSize
        original == 32.sp -> headlineLargeSize
        original == 28.sp -> headlineMediumSize
        original == 22.sp -> titleLargeSize
        original == 16.sp -> titleMediumSize
        original == 14.sp -> labelLargeSize
        else -> original
    }
}

@Composable
fun rememberResponsiveSize() {
    val config = LocalConfiguration.current
    ResponsiveSize.update(config.screenWidthDp)
}
