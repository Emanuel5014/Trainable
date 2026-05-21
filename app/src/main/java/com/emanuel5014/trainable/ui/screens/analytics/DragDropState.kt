package com.emanuel5014.trainable.ui.screens.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DragDropState(
    val lazyListState: LazyListState,
    private val haptic: HapticFeedback,
    private val scope: CoroutineScope,
    private val onMove: (String, Boolean) -> Unit
) {
    var draggedWidgetId by mutableStateOf<String?>(null)
        private set

    var recentlyDroppedWidgetId by mutableStateOf<String?>(null)
        private set

    var fingerY by mutableFloatStateOf(0f)
        private set

    private var fingerOffsetInItem by mutableFloatStateOf(0f)
    
    val dropAnimatable = Animatable(0f)

    private var expectedIndex by mutableStateOf<Int?>(null)

    fun onDragStart(absoluteInitialY: Float, itemRelativeY: Float, widgetId: String) {
        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.find { it.key == widgetId } ?: return
        draggedWidgetId = widgetId
        fingerY = absoluteInitialY
        fingerOffsetInItem = itemRelativeY
        expectedIndex = itemInfo.index
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun onDrag(absoluteY: Float) {
        val widgetId = draggedWidgetId ?: return
        fingerY = absoluteY

        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val draggedItemInfo = visibleItems.find { it.key == widgetId } ?: return
        
        // Wait for the layout to catch up to the last swap
        if (expectedIndex != null && draggedItemInfo.index != expectedIndex) {
            return
        }

        val visualTop = fingerY - fingerOffsetInItem
        val visualCenter = visualTop + draggedItemInfo.size / 2f

        val currentIndex = draggedItemInfo.index
        var targetIndex: Int? = null
        var up = false

        // Check immediate neighbor above (index - 1)
        if (currentIndex > 1) { // Skip index 0 (carousel header)
            val itemAbove = visibleItems.find { it.index == currentIndex - 1 }
            if (itemAbove != null) {
                val threshold = itemAbove.offset + itemAbove.size / 2f
                if (visualCenter < threshold) {
                    targetIndex = currentIndex - 1
                    up = true
                }
            }
        }

        // Check immediate neighbor below (index + 1)
        if (targetIndex == null) {
            val itemBelow = visibleItems.find { it.index == currentIndex + 1 }
            if (itemBelow != null) {
                val threshold = itemBelow.offset + itemBelow.size / 2f
                if (visualCenter > threshold) {
                    targetIndex = currentIndex + 1
                    up = false
                }
            }
        }

        if (targetIndex != null) {
            expectedIndex = targetIndex
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onMove(widgetId, up)
        }
    }

    fun onDragEnd() {
        val widgetId = draggedWidgetId ?: return
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val draggedItemInfo = visibleItems.find { it.key == widgetId }

        expectedIndex = null

        if (draggedItemInfo != null) {
            val visualTop = fingerY - fingerOffsetInItem
            val currentTranslationY = visualTop - draggedItemInfo.offset
            
            recentlyDroppedWidgetId = widgetId
            draggedWidgetId = null
            
            scope.launch {
                dropAnimatable.snapTo(currentTranslationY)
                dropAnimatable.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                recentlyDroppedWidgetId = null
            }
        } else {
            draggedWidgetId = null
        }
    }

    fun dragTranslationY(widgetId: String): Float {
        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.find { it.key == widgetId } ?: return 0f
        return (fingerY - fingerOffsetInItem) - itemInfo.offset
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    onMove: (String, Boolean) -> Unit
): DragDropState {
    return remember(lazyListState, haptic, scope) {
        DragDropState(lazyListState, haptic, scope, onMove)
    }
}
