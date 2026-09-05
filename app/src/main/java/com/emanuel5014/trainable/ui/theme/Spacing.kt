package com.emanuel5014.trainable.ui.theme

import androidx.compose.ui.unit.dp

object Spacing {
    val Default = 0.dp
    val xtraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val doubleLarge = 32.dp
    val extraLarge = 48.dp
    val extreme = 64.dp
    
    val SectionSpacing = 48.dp
    val ContainerPadding = 32.dp
    val CardPadding = 24.dp

    /** Responsive variants: shrink on very small / short screens, grow on tablets. */
    val screenHorizontal get() = ResponsiveSize.horizontalPadding
    val cardResponsive get() = ResponsiveSize.cardPadding
    val sectionResponsive
        get() = when {
            ResponsiveSize.isShortHeight || ResponsiveSize.isCompact -> 32.dp
            ResponsiveSize.isTablet -> 56.dp
            else -> 48.dp
        }
    val bottomNavSpacer
        get() = when {
            ResponsiveSize.isShortHeight -> 80.dp
            ResponsiveSize.isTablet -> 110.dp
            else -> 100.dp
        }
}
