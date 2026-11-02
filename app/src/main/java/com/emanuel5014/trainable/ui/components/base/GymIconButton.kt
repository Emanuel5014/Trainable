package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.LocalGlassSettings
import com.emanuel5014.trainable.ui.theme.headerGlass
import com.emanuel5014.trainable.data.model.GlassPart
import kotlinx.coroutines.flow.map

@Composable
fun GymIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceContainerHigh,
    contentColor: Color = OnSurface,
    description: String? = null,
    shape: Shape = CircleShape
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val hapticEnabled by remember(context) {
        context.dataStore.data.map { it[UserPreferencesRepository.HAPTIC_ENABLED] ?: true }
    }.collectAsState(initial = true)

    // Only the neutral round button turns to glass; a coloured one (delete, accent) keeps its fill
    val glass = containerColor == SurfaceContainerHigh && shape == CircleShape &&
        LocalGlassSettings.current.isOn(GlassPart.HeaderButtons)
    // Glass swells under the finger instead of shrinking
    val scale by animateFloatAsState(if (isPressed && !glass) 0.9f else 1f, label = "icon_button_scale")

    Box(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .then(if (glass) Modifier.headerGlass() else Modifier)
            .clip(shape)
            .then(if (glass) Modifier else Modifier.background(containerColor))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (hapticEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
