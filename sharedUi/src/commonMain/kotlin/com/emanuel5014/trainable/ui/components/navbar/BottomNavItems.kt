package com.emanuel5014.trainable.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.nav_analytics
import com.emanuel5014.trainable.sharedui.resources.nav_dashboard
import com.emanuel5014.trainable.sharedui.resources.nav_history
import com.emanuel5014.trainable.sharedui.resources.nav_routines
import org.jetbrains.compose.resources.stringResource

@Composable
fun localizedNavItems(): List<LocalizedNavItem> = listOf(
    LocalizedNavItem(
        stringResource(Res.string.nav_dashboard),
        Icons.Rounded.Home
    ),
    LocalizedNavItem(
        stringResource(Res.string.nav_routines),
        Icons.AutoMirrored.Rounded.Notes
    ),
    LocalizedNavItem(
        stringResource(Res.string.nav_history),
        Icons.Rounded.History
    ),
    LocalizedNavItem(
        stringResource(Res.string.nav_analytics),
        Icons.Rounded.Insights
    )
)

data class LocalizedNavItem(
    val title: String,
    val icon: ImageVector
)
