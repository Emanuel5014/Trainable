package com.emanuel5014.trainable.ui.components

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.LocalGlassSettings
import com.emanuel5014.trainable.ui.glass.headerGlass
import com.emanuel5014.trainable.data.model.GlassPart

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
    titleInRow: Boolean = false,
    titleStyle: TextStyle = MaterialTheme.typography.displaySmall
) {
    val fontSize = ResponsiveSize.responsiveFontSize(titleStyle.fontSize)
    val lineHeight = when {
        titleStyle.lineHeight.value.isNaN() || titleStyle.lineHeight.value == 0f ->
            if (fontSize == titleStyle.fontSize) ResponsiveSize.screenHeaderLineHeightSp
            else fontSize * 1.1f
        else -> titleStyle.lineHeight
    }
    ScreenHeader(
        titleContent = {
            FittedScreenTitle(
                title = title,
                style = titleStyle.copy(
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = OnSurface
                )
            )
        },
        subtitle = subtitle,
        icon = icon,
        navigationIcon = navigationIcon,
        actions = actions,
        modifier = modifier,
        titleInRow = titleInRow
    )
}

@Composable
fun ScreenHeader(
    titleContent: @Composable () -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
    titleInRow: Boolean = false
) {
    val horizontalPad = ResponsiveSize.horizontalPadding
    val topPad = if (ResponsiveSize.isShortHeight) 12.dp
        else if (navigationIcon != null || (actions != null && titleInRow)) 8.dp else 24.dp
    val iconBox = if (ResponsiveSize.isCompact) 48.dp else 56.dp
    val iconInner = if (ResponsiveSize.isCompact) 24.dp else 28.dp
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPad, bottom = 16.dp)
    ) {
        if (navigationIcon != null || (actions != null && titleInRow) || titleInRow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (navigationIcon != null && titleInRow) 12.dp else horizontalPad,
                        end = horizontalPad
                    )
                    .padding(bottom = if (titleInRow) 8.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    navigationIcon?.invoke()
                    if (titleInRow) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            titleContent()
                        }
                    }
                }
                if (actions != null && (titleInRow || navigationIcon != null)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        actions.invoke(this)
                    }
                }
            }
        }

        if (!titleInRow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (ResponsiveSize.isCompact) 12.dp else 16.dp)
            ) {
                if (icon != null) {
                    val glass = LocalGlassSettings.current.isOn(GlassPart.HeaderButtons)
                    Box(
                        modifier = Modifier
                            .size(iconBox)
                            .then(if (glass) Modifier.headerGlass() else Modifier)
                            .clip(CircleShape)
                            .then(if (glass) Modifier else Modifier.background(SurfaceContainerHigh)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(iconInner)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .clipToBounds()
                ) {
                    if (subtitle != null) {
                        Text(
                            text = subtitle.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    titleContent()
                }

                if (actions != null && navigationIcon == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        actions.invoke(this)
                    }
                }
            }
        }
    }
}

/**
 * The screen title at the largest size (down to [MIN_TITLE_SCALE] of [style]) at which every word fits whole on a
 * line and the title takes at most two lines: a long single word ("STATISTICHE") between the header's buttons, a
 * large system font or a long translation shrink it instead of breaking it mid-word.
 */
@Composable
private fun FittedScreenTitle(title: String, style: TextStyle) {
    BoxWithConstraints {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth
        val fitted = remember(title, style, maxWidthPx, density) {
            fitTitle(title, style, maxWidthPx, measurer)
        }
        Text(text = title, style = fitted, maxLines = 2, overflow = TextOverflow.Ellipsis, softWrap = true)
    }
}

private const val MIN_TITLE_SCALE = 0.6f

private fun fitTitle(title: String, style: TextStyle, maxWidthPx: Int, measurer: TextMeasurer): TextStyle {
    if (maxWidthPx == Constraints.Infinity || maxWidthPx <= 0 || !style.fontSize.isSp) return style
    val words = title.split(' ').filter { it.isNotBlank() }
    val base = style.fontSize.value
    val lineRatio = if (style.lineHeight.isSp) style.lineHeight.value / base else Float.NaN
    var size = base
    val smallest = base * MIN_TITLE_SCALE
    while (true) {
        val candidate = style.copy(
            fontSize = size.sp,
            lineHeight = if (lineRatio.isNaN()) style.lineHeight else (size * lineRatio).sp
        )
        val wordsFit = words.all { word ->
            measurer.measure(word, candidate, maxLines = 1, softWrap = false).size.width <= maxWidthPx
        }
        val linesFit = wordsFit && !measurer.measure(
            title, candidate, maxLines = 2, constraints = Constraints(maxWidth = maxWidthPx)
        ).hasVisualOverflow
        if (linesFit || size <= smallest) return candidate
        size = (size - 1f).coerceAtLeast(smallest)
    }
}
