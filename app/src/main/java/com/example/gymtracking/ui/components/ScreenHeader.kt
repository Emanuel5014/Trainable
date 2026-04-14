package com.example.gymtracking.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtracking.ui.theme.OnSurface
import com.example.gymtracking.ui.theme.Primary
import com.example.gymtracking.ui.theme.SurfaceContainerHigh

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
    titleInRow: Boolean = false,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displaySmall
) {
    ScreenHeader(
        titleContent = {
            Text(
                text = title,
                style = titleStyle,
                color = OnSurface,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                lineHeight = 40.sp
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = if (navigationIcon != null || actions != null) 8.dp else 24.dp, bottom = 16.dp)
    ) {
        if (navigationIcon != null || actions != null || titleInRow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                        titleContent()
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions?.invoke(this)
                }
            }
        }

        if (!titleInRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clipToBounds()
                ) {
                    if (subtitle != null) {
                        Text(
                            text = subtitle.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                    titleContent()
                }
            }
        }
    }
}
