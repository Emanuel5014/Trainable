@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.emanuel5014.trainable.ui.components.glass

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.emanuel5014.trainable.ui.theme.LocalLiquidGlassEnabled
import androidx.compose.material3.TopAppBar as MaterialTopAppBar

/** Consistent backdrop through secondary-screen headers, including Material's default-color callers. */
@Composable
fun GlassTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    contentPadding: PaddingValues = TopAppBarDefaults.ContentPadding
) {
    MaterialTopAppBar(title, modifier, navigationIcon, actions, expandedHeight, windowInsets,
        if (LocalLiquidGlassEnabled.current) colors.copy(containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = .88f)) else colors,
        scrollBehavior, contentPadding)
}
