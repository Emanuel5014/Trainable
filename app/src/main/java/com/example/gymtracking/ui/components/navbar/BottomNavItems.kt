package com.example.gymtracking.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.gymtracking.R

@Composable
fun localizedNavItems(): List<LocalizedNavItem> = listOf(
    LocalizedNavItem(
        stringResource(R.string.nav_dashboard),
        Icons.Rounded.Home
    ),
    LocalizedNavItem(
        stringResource(R.string.nav_routines),
        Icons.Rounded.FitnessCenter
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
