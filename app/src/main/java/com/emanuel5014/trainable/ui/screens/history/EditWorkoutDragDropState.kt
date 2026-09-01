package com.emanuel5014.trainable.ui.screens.history

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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun getWorkoutItemKey(item: Any): String {
    return when (item) {
        is EditExerciseState -> "exercise_${item.exercise.id}"
        is CardioLogEntity -> "cardio_${item.id}"
        else -> item.toString()
    }
}

fun getMergedSupersetRange(index: Int, list: List<Any>): IntRange {
    val currentEx = list.getOrNull(index) as? EditExerciseState ?: return index..index
    val sid = currentEx.sets.firstOrNull()?.supersetId ?: return index..index

    var start = index
    while (start > 0) {
        val prevEx = list[start - 1] as? EditExerciseState ?: break
        if (prevEx.sets.firstOrNull()?.supersetId != sid) break
        start--
    }

    var end = index
    while (end < list.lastIndex) {
        val nextEx = list[end + 1] as? EditExerciseState ?: break
        if (nextEx.sets.firstOrNull()?.supersetId != sid) break
        end++
    }

    return start..end
}

class EditWorkoutDragDropState(
    val lazyListState: LazyListState,
    private val items: SnapshotStateList<Any>,
    private val haptic: HapticFeedback,
    private val hapticEnabled: Boolean,
    private val scope: CoroutineScope,
    private val onOrderChanged: (List<Any>) -> Unit
) {
    var draggedItemKeys by mutableStateOf<Set<String>>(emptySet())
        private set

    val isDragging: Boolean
        get() = draggedItemKeys.isNotEmpty()

    var recentlyDroppedKeys by mutableStateOf<Set<String>>(emptySet())
        private set

    var fingerY by mutableFloatStateOf(0f)
        private set

    private var fingerOffsetInBlock by mutableFloatStateOf(0f)
    private var lastSwapFingerY by mutableFloatStateOf(0f)
    private var lastKnownFirstItemOffset by mutableFloatStateOf(0f)

    val dropAnimatable = Animatable(0f)

    private var expectedStartIndex by mutableStateOf<Int?>(null)

    fun onDragStart(offset: Offset) {
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val clickedItemInfo = visibleItems.find { info ->
            offset.y >= info.offset && offset.y <= info.offset + info.size
        } ?: return

        val clickedKey = clickedItemInfo.key.toString()
        val clickedIndex = items.indexOfFirst { getWorkoutItemKey(it) == clickedKey }
        if (clickedIndex == -1) return

        val range = getMergedSupersetRange(clickedIndex, items)
        val firstKey = getWorkoutItemKey(items[range.first])
        val firstItemInfo = visibleItems.find { it.key == firstKey } ?: clickedItemInfo

        fingerY = offset.y
        firstItemInfo.offset.let {
            lastKnownFirstItemOffset = it.toFloat()
            fingerOffsetInBlock = offset.y - it.toFloat()
        }
        lastSwapFingerY = offset.y
        expectedStartIndex = range.first
        draggedItemKeys = range.map { getWorkoutItemKey(items[it]) }.toSet()

        if (hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun onScroll(scrollDelta: Float) {
        if (!isDragging) return
        lastSwapFingerY -= scrollDelta
    }

    fun onDrag(absoluteY: Float) {
        if (draggedItemKeys.isEmpty()) return
        fingerY = absoluteY

        val currentStartIndex = items.indexOfFirst { getWorkoutItemKey(it) in draggedItemKeys }
        if (currentStartIndex == -1) return
        val currentRange = getMergedSupersetRange(currentStartIndex, items)
        val currentSize = currentRange.endInclusive - currentRange.start + 1

        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val firstItemInfo = visibleItems.find { it.key == getWorkoutItemKey(items[currentRange.start]) }
        if (firstItemInfo != null) {
            lastKnownFirstItemOffset = firstItemInfo.offset.toFloat()
        }
        val lastItemInfo = visibleItems.find { it.key == getWorkoutItemKey(items[currentRange.endInclusive]) } ?: firstItemInfo

        // Wait for Compose layout pass to catch up to the last swap before processing new swaps
        if (expectedStartIndex != null && firstItemInfo != null && firstItemInfo.index != expectedStartIndex) {
            return
        }

        // Keep lastSwapFingerY within a reasonable window of fingerY when reversing direction
        if (fingerY < lastSwapFingerY - 80f) {
            lastSwapFingerY = fingerY + 80f
        } else if (fingerY > lastSwapFingerY + 80f) {
            lastSwapFingerY = fingerY - 80f
        }

        val firstOffset = firstItemInfo?.offset?.toFloat() ?: lastKnownFirstItemOffset
        val lastOffset = lastItemInfo?.let { it.offset + it.size }?.toFloat() ?: (firstOffset + 120f)
        val draggedBlockHeight = (lastOffset - firstOffset).coerceAtLeast(60f)

        val visualTop = fingerY - fingerOffsetInBlock
        val visualCenter = visualTop + draggedBlockHeight / 2f

        // Check neighbor above (index - 1)
        if (currentRange.start > 0) {
            val prevIndex = currentRange.start - 1
            val prevRange = getMergedSupersetRange(prevIndex, items)
            val prevFirstInfo = visibleItems.find { it.index == prevRange.start }
            val prevLastInfo = visibleItems.find { it.index == prevRange.endInclusive }

            val prevCenter: Float
            val prevHeight: Float
            if (prevFirstInfo != null) {
                val prevTop = prevFirstInfo.offset.toFloat()
                val prevBottom = ((prevLastInfo ?: prevFirstInfo).offset + (prevLastInfo ?: prevFirstInfo).size).toFloat()
                prevCenter = (prevTop + prevBottom) / 2f
                prevHeight = prevBottom - prevTop
            } else {
                // Fallback for item scrolled off-screen above viewport
                val firstVisibleOffset = visibleItems.firstOrNull()?.offset?.toFloat() ?: 0f
                prevCenter = firstVisibleOffset - 60f
                prevHeight = 120f
            }

            val minStepDistance = (prevHeight * 0.2f).coerceIn(16f, 40f)

            // Require visualCenter to cross prevCenter AND fingerY to have moved UP sufficiently since last swap
            if (visualCenter < prevCenter && (lastSwapFingerY - fingerY) >= minStepDistance) {
                val draggedItems = items.subList(currentRange.start, currentRange.endInclusive + 1).toList()
                repeat(currentSize) {
                    items.removeAt(currentRange.start)
                }
                val insertIndex = prevRange.start
                items.addAll(insertIndex, draggedItems)
                expectedStartIndex = insertIndex
                lastSwapFingerY = fingerY

                if (hapticEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                return
            }
        }

        // Check neighbor below (index + 1)
        if (currentRange.endInclusive < items.lastIndex) {
            val nextIndex = currentRange.endInclusive + 1
            val nextRange = getMergedSupersetRange(nextIndex, items)
            val nextFirstInfo = visibleItems.find { it.index == nextRange.start }
            val nextLastInfo = visibleItems.find { it.index == nextRange.endInclusive }

            val nextCenter: Float
            val nextHeight: Float
            if (nextFirstInfo != null) {
                val nextTop = nextFirstInfo.offset.toFloat()
                val nextBottom = ((nextLastInfo ?: nextFirstInfo).offset + (nextLastInfo ?: nextFirstInfo).size).toFloat()
                nextCenter = (nextTop + nextBottom) / 2f
                nextHeight = nextBottom - nextTop
            } else {
                // Fallback for item scrolled off-screen below viewport
                val lastVisibleBottom = visibleItems.lastOrNull()?.let { it.offset + it.size }?.toFloat() ?: 1000f
                nextCenter = lastVisibleBottom + 60f
                nextHeight = 120f
            }

            val minStepDistance = (nextHeight * 0.2f).coerceIn(16f, 40f)

            // Require visualCenter to cross nextCenter AND fingerY to have moved DOWN sufficiently since last swap
            if (visualCenter > nextCenter && (fingerY - lastSwapFingerY) >= minStepDistance) {
                val draggedItems = items.subList(currentRange.start, currentRange.endInclusive + 1).toList()
                repeat(currentSize) {
                    items.removeAt(currentRange.start)
                }
                val nextSize = nextRange.endInclusive - nextRange.start + 1
                val insertIndex = currentRange.start + nextSize
                items.addAll(insertIndex, draggedItems)
                expectedStartIndex = insertIndex
                lastSwapFingerY = fingerY

                if (hapticEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                return
            }
        }
    }

    fun onDragEnd() {
        val keys = draggedItemKeys
        if (keys.isEmpty()) return

        expectedStartIndex = null
        lastSwapFingerY = 0f
        val currentStartIndex = items.indexOfFirst { getWorkoutItemKey(it) in keys }
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val firstItemInfo = if (currentStartIndex != -1) {
            visibleItems.find { it.key == getWorkoutItemKey(items[currentStartIndex]) }
        } else null

        val itemOffset = firstItemInfo?.offset?.toFloat() ?: lastKnownFirstItemOffset
        val currentTranslationY = (fingerY - fingerOffsetInBlock) - itemOffset

        recentlyDroppedKeys = keys
        draggedItemKeys = emptySet()

        onOrderChanged(items.toList())

        scope.launch {
            dropAnimatable.snapTo(currentTranslationY)
            dropAnimatable.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
            recentlyDroppedKeys = emptySet()
        }
    }

    fun dragTranslationY(itemKey: String): Float {
        if (itemKey in draggedItemKeys) {
            val currentStartIndex = items.indexOfFirst { getWorkoutItemKey(it) in draggedItemKeys }
            if (currentStartIndex == -1) return 0f
            val firstItemInfo = lazyListState.layoutInfo.visibleItemsInfo.find {
                it.key == getWorkoutItemKey(items[currentStartIndex])
            }
            if (firstItemInfo != null) {
                lastKnownFirstItemOffset = firstItemInfo.offset.toFloat()
            }
            return (fingerY - fingerOffsetInBlock) - lastKnownFirstItemOffset
        } else if (itemKey in recentlyDroppedKeys) {
            return dropAnimatable.value
        }
        return 0f
    }
}

@Composable
fun rememberEditWorkoutDragDropState(
    lazyListState: LazyListState,
    items: SnapshotStateList<Any>,
    haptic: HapticFeedback,
    hapticEnabled: Boolean,
    scope: CoroutineScope,
    onOrderChanged: (List<Any>) -> Unit
): EditWorkoutDragDropState {
    return remember(lazyListState, items, haptic, hapticEnabled, scope) {
        EditWorkoutDragDropState(
            lazyListState = lazyListState,
            items = items,
            haptic = haptic,
            hapticEnabled = hapticEnabled,
            scope = scope,
            onOrderChanged = onOrderChanged
        )
    }
}
