package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.ui.theme.LocalLiquidGlassEnabled
import com.emanuel5014.trainable.ui.theme.liquidGlassSurface
import androidx.compose.material3.contentColorFor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import com.emanuel5014.trainable.ui.components.glass.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.SurfaceContainerLow

@Composable
fun GymCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceContainerLow,
    border: androidx.compose.foundation.BorderStroke? = null,
    shape: Shape = Shapes.extraLarge,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.liquidGlassSurface(shape, containerColor),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (LocalLiquidGlassEnabled.current) Color.Transparent else containerColor,
            contentColor = contentColorFor(containerColor)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = border
    ) {
        Column(
            modifier = Modifier.padding(ResponsiveSize.cardPadding),
            content = content
        )
    }
}

