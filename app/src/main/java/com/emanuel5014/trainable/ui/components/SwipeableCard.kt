package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Swipe direction options
 */
enum class SwipeDirection {
    START_TO_END,
    END_TO_START,
    BOTH
}

/**
 * Swipe action configuration
 */
data class SwipeAction(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val contentDescription: String,
    val backgroundColor: Color,
    val iconTint: Color,
    val onSwipe: () -> Unit,
    val threshold: Dp = 100.dp
)

/**
 * A reusable swipeable card component following Material 3 Expressive design principles.
 * Provides smooth animations and haptic feedback for swipe gestures.
 *
 * @param modifier Modifier for the card
 * @param swipeDirection Direction(s) the card can be swiped
 * @param startAction Action to perform when swiped from start to end
 * @param endAction Action to perform when swiped from end to start
 * @param enabled Whether swipe is enabled
 * @param content The card content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableCard(
    modifier: Modifier = Modifier,
    swipeDirection: SwipeDirection = SwipeDirection.END_TO_START,
    startAction: SwipeAction? = null,
    endAction: SwipeAction? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    
    val startThresholdPx = with(density) { 
        (startAction?.threshold ?: 100.dp).toPx() 
    }
    val endThresholdPx = with(density) { 
        (endAction?.threshold ?: 100.dp).toPx() 
    }
    
    val swipeOffset = remember { mutableFloatStateOf(0f) }
    var isSwipeActive by remember { mutableStateOf(false) }
    var isStartThresholdReached by remember { mutableStateOf(false) }
    var isEndThresholdReached by remember { mutableStateOf(false) }
    
    val animatable = remember { Animatable(0f) }
    
    LaunchedEffect(animatable.value) {
        val offset = animatable.value
        swipeOffset.floatValue = offset
        
        if (startAction != null) {
            isStartThresholdReached = offset > startThresholdPx
        }
        if (endAction != null) {
            isEndThresholdReached = offset < -endThresholdPx
        }
    }

    fun canSwipeTowardsStart() = swipeDirection == SwipeDirection.START_TO_END || swipeDirection == SwipeDirection.BOTH
    fun canSwipeTowardsEnd() = swipeDirection == SwipeDirection.END_TO_START || swipeDirection == SwipeDirection.BOTH

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                
                detectHorizontalDragGestures(
                    onDragStart = {
                        isSwipeActive = true
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        
                        val currentOffset = animatable.value
                        val newOffset = currentOffset + dragAmount
                        
                        // Respect swipe direction constraints
                        val constrainedOffset = when {
                            !canSwipeTowardsStart() && newOffset > 0f -> 0f
                            !canSwipeTowardsEnd() && newOffset < 0f -> 0f
                            else -> newOffset
                        }
                        
                        coroutineScope.launch {
                            animatable.snapTo(constrainedOffset)
                        }
                    },
                    onDragEnd = {
                        val wasStartThresholdReached = isStartThresholdReached
                        val wasEndThresholdReached = isEndThresholdReached
                        
                        coroutineScope.launch {
                            when {
                                startAction != null && wasStartThresholdReached -> {
                                    // Snap to start action position
                                    animatable.animateTo(
                                        startThresholdPx,
                                        animationSpec = tween(durationMillis = 200)
                                    )
                                    startAction.onSwipe()
                                    // Reset after action
                                    animatable.animateTo(
                                        0f,
                                        animationSpec = tween(durationMillis = 300)
                                    )
                                }
                                endAction != null && wasEndThresholdReached -> {
                                    // Snap to end action position
                                    animatable.animateTo(
                                        -endThresholdPx,
                                        animationSpec = tween(durationMillis = 200)
                                    )
                                    endAction.onSwipe()
                                    // Reset after action
                                    animatable.animateTo(
                                        0f,
                                        animationSpec = tween(durationMillis = 300)
                                    )
                                }
                                else -> {
                                    // Snap back to center
                                    animatable.animateTo(
                                        0f,
                                        animationSpec = tween(durationMillis = 300)
                                    )
                                }
                            }
                            isSwipeActive = false
                            isStartThresholdReached = false
                            isEndThresholdReached = false
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            animatable.animateTo(
                                0f,
                                animationSpec = tween(durationMillis = 300)
                            )
                            isSwipeActive = false
                            isStartThresholdReached = false
                            isEndThresholdReached = false
                        }
                    }
                )
            }
    ) {
        // Background layer for swipe actions
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
        ) {
            // Start action background
            if (startAction != null && canSwipeTowardsStart()) {
                val progress = (swipeOffset.floatValue / startThresholdPx).coerceIn(0f, 1f)
                val backgroundColor = startAction.backgroundColor.copy(alpha = progress * 0.9f)
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(backgroundColor)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(
                        imageVector = startAction.icon,
                        contentDescription = startAction.contentDescription,
                        tint = startAction.iconTint,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer {
                                alpha = progress
                                scaleX = 0.8f + (progress * 0.2f)
                                scaleY = 0.8f + (progress * 0.2f)
                            }
                    )
                }
            }

            // End action background
            if (endAction != null && canSwipeTowardsEnd()) {
                val progress = (-swipeOffset.floatValue / endThresholdPx).coerceIn(0f, 1f)
                val backgroundColor = endAction.backgroundColor.copy(alpha = progress * 0.9f)
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .align(Alignment.CenterEnd)
                        .background(backgroundColor)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = endAction.icon,
                        contentDescription = endAction.contentDescription,
                        tint = endAction.iconTint,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer {
                                alpha = progress
                                scaleX = 0.8f + (progress * 0.2f)
                                scaleY = 0.8f + (progress * 0.2f)
                            }
                    )
                }
            }
        }

        // Card content layer
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = swipeOffset.floatValue
                    
                    // Add slight scale and rotation for expressive feedback
                    val absOffset = kotlin.math.abs(swipeOffset.floatValue)
                    val maxOffset = kotlin.math.max(startThresholdPx, endThresholdPx)
                    val progress = (absOffset / maxOffset).coerceIn(0f, 1f)
                    
                    scaleX = 1f - (progress * 0.02f)
                    scaleY = 1f - (progress * 0.02f)
                    rotationZ = (swipeOffset.floatValue / maxOffset) * 2f
                }
        ) {
            content()
        }
    }
}
