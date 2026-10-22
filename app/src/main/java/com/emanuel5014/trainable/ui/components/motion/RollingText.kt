package com.emanuel5014.trainable.ui.components.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * A text whose characters roll when they change, like the digits of a counter: a digit that grows rolls up, one that
 * shrinks rolls down. Characters are matched from the end, so "9" turning into "10" keeps the units in place.
 */
@Composable
fun RollingText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    Row(modifier = modifier) {
        text.forEachIndexed { index, char ->
            key(text.length - 1 - index) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val up = targetState > initialState
                        (slideInVertically(WorkoutMotion.spatial()) { if (up) it else -it } +
                            fadeIn(WorkoutMotion.effects())) togetherWith
                            (slideOutVertically(WorkoutMotion.spatial()) { if (up) -it else it } +
                                fadeOut(WorkoutMotion.effects())) using SizeTransform(clip = false)
                    },
                    label = "rollingChar"
                ) { shown ->
                    Text(text = shown.toString(), style = style, color = color, fontWeight = fontWeight)
                }
            }
        }
    }
}
