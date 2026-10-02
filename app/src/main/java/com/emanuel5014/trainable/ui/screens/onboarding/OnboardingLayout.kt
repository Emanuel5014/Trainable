package com.emanuel5014.trainable.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import kotlin.math.max

/**
 * Measures that every setup step shares, derived from the window instead of being fixed at 32dp:
 * the same title position, margins and type scale on a compact phone, a tall phone, a tablet or with
 * a large system font.
 */
internal object OnboardingMetrics {
    /** Widest the content gets; on tablets and landscape it is centered instead of stretched. */
    val maxContentWidth: Dp = 560.dp

    val horizontalPadding: Dp
        get() = when {
            ResponsiveSize.screenWidthDp < 340 -> 20.dp
            ResponsiveSize.screenWidthDp < 380 -> 24.dp
            ResponsiveSize.screenWidthDp < 600 -> 28.dp
            else -> 32.dp
        }

    /** Space between the status bar and the title: proportional to the screen height, so titles don't hug the top. */
    val topPadding: Dp
        get() = if (ResponsiveSize.isShortHeight) 16.dp
        else (ResponsiveSize.screenHeightDp * 0.06f).coerceIn(32f, 72f).dp

    /** Gap between a step's title, its groups of options and its footnote. */
    val sectionSpacing: Dp
        get() = when {
            ResponsiveSize.screenHeightDp < 640 -> 20.dp
            ResponsiveSize.screenHeightDp < 760 -> 26.dp
            else -> 32.dp
        }

    val titleSize: TextUnit get() = scaled(
        when {
            ResponsiveSize.screenWidthDp < 340 -> 26f
            ResponsiveSize.screenWidthDp < 360 -> 28f
            ResponsiveSize.screenWidthDp < 400 -> 34f
            ResponsiveSize.screenWidthDp < 600 -> 36f
            else -> 44f
        }
    )

    val welcomeTitleSize: TextUnit get() = scaled(
        when {
            ResponsiveSize.screenWidthDp < 340 -> 30f
            ResponsiveSize.screenWidthDp < 380 -> 36f
            ResponsiveSize.screenWidthDp < 600 -> 44f
            else -> 56f
        }
    )

    /** Welcome artwork scale: smaller on short screens so title and text still fit above the button. */
    val artworkScale: Float get() = (ResponsiveSize.screenHeightDp / 820f).coerceIn(0.62f, 1.1f)

    /**
     * `sp` already grows with the system font size; past 1.2× the titles would outgrow narrow screens,
     * so the nominal size gives way a little to keep the apparent size bounded.
     */
    private fun scaled(base: Float): TextUnit = (base / max(1f, ResponsiveSize.fontScale / 1.2f)).sp
}

/**
 * One step of the setup: scrolls when it doesn't fit, stays below the status bar (and above the controls, which
 * sit under the pager rather than over it) and follows the keyboard. Every step starts at the same height.
 *
 * @param centered centers the content in the free space instead of starting at the top (the welcome step).
 */
@Composable
internal fun OnboardingPage(
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(OnboardingMetrics.sectionSpacing),
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val viewport = maxHeight
        Column(
            modifier = Modifier
                .widthIn(max = OnboardingMetrics.maxContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .then(if (centered) Modifier.heightIn(min = viewport) else Modifier)
                .padding(horizontal = OnboardingMetrics.horizontalPadding)
                .padding(top = if (centered) 0.dp else OnboardingMetrics.topPadding, bottom = 24.dp),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = if (centered) Arrangement.Center else verticalArrangement,
            content = content
        )
    }
}

/** Big step title, sized for the window and the system font scale. */
@Composable
internal fun OnboardingTitle(text: String, modifier: Modifier = Modifier) {
    val size = OnboardingMetrics.titleSize
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.displaySmall,
        color = OnSurface,
        fontWeight = FontWeight.Black,
        fontSize = size,
        lineHeight = size * 1.22f
    )
}
