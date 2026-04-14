package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GymLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    LoadingIndicator(
        modifier = modifier.size(size),
        color = MaterialTheme.colorScheme.primary
    )
}