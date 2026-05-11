package com.emanuel5014.trainable.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.emanuel5014.trainable.R

@Composable
fun localizedNavItems(): List<LocalizedNavItem> = listOf(
    LocalizedNavItem(
        stringResource(R.string.nav_dashboard),
        Icons.Rounded.Home
    ),
    LocalizedNavItem(
        stringResource(R.string.nav_routines),
        Icons.AutoMirrored.Rounded.Notes
    ),
    LocalizedNavItem(
        stringResource(R.string.nav_history),
        Icons.Rounded.History
    ),
    LocalizedNavItem(
        stringResource(R.string.nav_analytics),
        Icons.Rounded.Insights
    )
)

data class LocalizedNavItem(
    val title: String,
    val icon: ImageVector
)
