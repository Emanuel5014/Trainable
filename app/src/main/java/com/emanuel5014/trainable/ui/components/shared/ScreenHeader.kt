package com.emanuel5014.trainable.ui.components

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
            Text(
                text = title,
                style = titleStyle.copy(fontSize = fontSize, lineHeight = lineHeight),
                color = OnSurface,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                lineHeight = lineHeight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                softWrap = true
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
                    Box(
                        modifier = Modifier
                            .size(iconBox)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh),
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
