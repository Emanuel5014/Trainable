package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import com.emanuel5014.trainable.ui.components.motion.PressMorph
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes

@Composable
fun GymButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Primary,
    contentColor: Color = OnPrimary,
    shape: Shape = Shapes.large,
    enabled: Boolean = true,
    height: Int = if (ResponsiveSize.isCompact) 48 else 56,
    contentPadding: PaddingValues = PaddingValues(horizontal = if (ResponsiveSize.isCompact) 16.dp else 24.dp),
    /** While pressed the corners tighten (from [shape]'s own 32 dp to 10 dp). Only the workout asks for it; [shape] is then ignored. */
    pressMorph: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val hapticEnabled by LocalHapticsSetting.current.collectAsState(initial = true)

    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "button_scale")
    val pressedFraction by animateFloatAsState(if (isPressed) 1f else 0f, WorkoutMotion.fastSpatial(), label = "button_press_morph")
    val buttonShape: Shape = if (pressMorph) {
        RoundedCornerShape(PressMorph.cornerDp(PRESS_MORPH_REST_DP, pressedFraction).coerceAtLeast(0f).dp)
    } else {
        shape
    }

    Button(
        onClick = {
            if (hapticEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onClick()
        },
        modifier = modifier.heightIn(min = height.dp).scale(scale).widthIn(min = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.3f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        shape = buttonShape,
        enabled = enabled,
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = contentPadding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

/** The corner radius of [com.emanuel5014.trainable.ui.theme.Shapes.large], where a pressed workout button starts tightening from. */
private const val PRESS_MORPH_REST_DP = 32f

/**
 * A [GymButton] label on one line: at the button's own text size whenever it fits (so nothing changes where it already
 * did), smaller only when the button is too narrow for it. Two buttons side by side on a 402 pt iPhone 17 wrapped
 * "AGGIUNGI" and "ANNULLA" over two lines and clipped "SCANSIONA".
 */
@Composable
fun GymButtonText(text: String, fontWeight: FontWeight? = null, modifier: Modifier = Modifier) {
    val size = LocalTextStyle.current.fontSize
    Text(
        text = text,
        modifier = modifier,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = size, stepSize = 0.5.sp)
    )
}
