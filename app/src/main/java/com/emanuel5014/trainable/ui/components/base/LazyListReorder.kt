package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emanuel5014.trainable.ui.theme.Primary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.pow

/**
 * Long-press-and-drag reordering for a vertical `LazyColumn`.
 *
 * The state does not own the data: it only owns the order *while a drag is running*
 * ([ordered] applies it on top of the list the screen already has) and hands the final order to
 * [onCommit] once the finger lifts. Doing the reorder locally means a swap is instant and costs no
 * ViewModel round trip or disk write, and that a screen can never get out of step with the layout.
 *
 * What keeps a drag controllable:
 *  - the dragged item follows the finger one-to-one, drawn from the layout that is actually on
 *    screen, and is kept inside the list however far the finger strays;
 *  - a swap happens when the finger passes the centre of the direct neighbour, whatever the size of
 *    either card, and never while the layout still shows the previous swap;
 *  - a card too tall to see its surroundings is drawn smaller while it is held, around the point
 *    the finger holds it by, and its slot stays marked so it is clear where it will land;
 *  - the list scrolls on its own only after the finger has moved, speeds up gradually towards the
 *    edge, and holds still rather than scroll the dragged item's slot out of sight;
 *  - a swap never makes the list jump: the scroll position is pinned by index, not by key.
 *
 * Items that must travel together (a superset) are described by [groupOf]: neighbours that share a
 * non-null group are dragged and swapped as a single block.
 */
@Stable
class LazyListReorderState internal constructor(
    private val lazyListState: LazyListState,
    private val scope: CoroutineScope,
    private val haptic: HapticFeedback,
    private val metrics: Metrics,
    private val keys: () -> List<Any>,
    private val groupOf: (Any) -> Any?,
    private val hapticEnabled: () -> Boolean,
    private val onCommit: (List<Any>) -> Unit
) {
    internal class Metrics(
        val hitTolerance: Float,
        val minEdgeZone: Float,
        val maxEdgeZone: Float,
        val maxScrollSpeed: Float,
        val armDistance: Float,
        val keepVisible: Float,
        val swapMargin: Float
    )

    private class Bounds(val top: Float, val bottom: Float) {
        val extent: Float get() = bottom - top
        val center: Float get() = (top + bottom) / 2f
    }

    /** Keys being held by the finger. Empty when idle. */
    var draggedKeys by mutableStateOf<Set<Any>>(emptySet())
        private set

    val isDragging: Boolean get() = draggedKeys.isNotEmpty()

    private var settlingKeys by mutableStateOf<Set<Any>>(emptySet())
    private var settleProgress by mutableFloatStateOf(1f)
    private var settleFromTop = 0f
    private var settleJob: Job? = null

    /** The order the user is building. Null whenever the list should just show the host order. */
    private var liveOrder by mutableStateOf<List<Any>?>(null)

    // Everything below is in the list's item coordinates: 0 is the start of the content area, the
    // same space `LazyListItemInfo.offset` and `viewportStartOffset` use.
    private var block: List<Any> = emptyList()
    private var fingerY by mutableFloatStateOf(0f)
    private var startFingerY = 0f
    private var grabOffset = 0f
    private var armed = false

    /** Scale a tall block is drawn at while held (1 when it is small enough to stay as it is). */
    private var blockScale = 1f
    private var lastTop = 0f
    private var lastExtent = 0f

    fun isDragged(key: Any): Boolean = key in draggedKeys

    fun isSettling(key: Any): Boolean = key in settlingKeys

    /** [items] in the order the user is currently building, or untouched when no drag is active. */
    fun <T> ordered(items: List<T>, keyOf: (T) -> Any): List<T> {
        val order = liveOrder ?: return items
        val byKey = HashMap<Any, T>(items.size)
        items.forEach { byKey[keyOf(it)] = it }
        val result = ArrayList<T>(items.size)
        order.forEach { key -> byKey.remove(key)?.let(result::add) }
        // Anything added while dragging goes last, in the host's order
        items.forEach { if (byKey.containsKey(keyOf(it))) result.add(it) }
        return result
    }

    internal fun onDragStart(pointerY: Float) {
        if (isDragging) return
        val order = keys()
        if (order.isEmpty()) return
        val info = lazyListState.layoutInfo
        val finger = pointerY - info.beforeContentPadding
        val position = positions(order)

        var hit: LazyListItemInfo? = null
        var hitGap = Float.MAX_VALUE
        for (item in info.visibleItemsInfo) {
            if (item.key !in position) continue
            val gap = when {
                finger < item.offset -> item.offset - finger
                finger > item.offset + item.size -> finger - (item.offset + item.size)
                else -> 0f
            }
            if (gap < hitGap) {
                hit = item
                hitGap = gap
            }
        }
        if (hit == null || hitGap > metrics.hitTolerance) return

        val range = blockAround(order, position.getValue(hit.key))
        val moving = order.subList(range.first, range.last + 1).toList()
        val first = info.visibleItemsInfo.firstOrNull { it.key == moving.first() }
        val last = info.visibleItemsInfo.firstOrNull { it.key == moving.last() }
        val top = (first ?: hit).offset.toFloat()
        val extent = if (first != null && last != null) {
            (last.offset + last.size - first.offset).toFloat()
        } else {
            hit.size.toFloat()
        }

        settleJob?.cancel()
        settlingKeys = emptySet()
        block = moving
        liveOrder = order.toList()
        startFingerY = finger
        fingerY = finger
        grabOffset = finger - top
        armed = false
        val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
        blockScale = if (extent > 0f) (viewport * TALL_CARD_VIEWPORT_FRACTION / extent).coerceIn(MIN_BLOCK_SCALE, 1f) else 1f
        lastTop = top
        lastExtent = extent
        draggedKeys = moving.toSet()
        tick(HapticFeedbackType.LongPress)
    }

    internal fun onDrag(pointerY: Float) {
        if (!isDragging) return
        fingerY = pointerY - lazyListState.layoutInfo.beforeContentPadding
        if (!armed && abs(fingerY - startFingerY) > metrics.armDistance) armed = true
        evaluateSwap()
    }

    internal fun onDragEnd() {
        if (!isDragging) return
        val info = lazyListState.layoutInfo
        val bounds = layoutBounds(info)
        val releasedAt = visualTop(bounds.extent, info)
        val finalOrder = liveOrder ?: keys()
        val changed = finalOrder != keys()

        // Hand the block to the settle animation in the same frame it leaves the finger, so it
        // never snaps back to its slot for a frame
        settleFromTop = releasedAt
        settleProgress = 0f
        settlingKeys = draggedKeys
        draggedKeys = emptySet()

        if (changed) onCommit(finalOrder)

        settleJob = scope.launch {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            ) { value, _ -> settleProgress = value }
            settlingKeys = emptySet()
        }
        scope.launch { releaseOrderOnceHostCaughtUp(finalOrder) }
    }

    /**
     * Keeps showing the dropped order until the host's list agrees with it, so a slow ViewModel
     * can't flash the old order back for a few frames. Gives up after a moment: the host wins.
     */
    private suspend fun releaseOrderOnceHostCaughtUp(committed: List<Any>) {
        withTimeoutOrNull(HOST_SYNC_TIMEOUT_MS) {
            snapshotFlow { sameRelativeOrder(keys(), committed) }.first { it }
        }
        if (!isDragging && liveOrder === committed) liveOrder = null
    }

    /**
     * Runs for as long as a drag lasts, once per frame: re-checks for a swap (the finger may have
     * stopped halfway through a multi-item move) and scrolls the list when the finger is near an
     * edge.
     */
    internal suspend fun driveWhileDragging() {
        var previous = withFrameNanos()
        var scrollingForMs = 0f
        while (true) {
            val now = withFrameNanos()
            val seconds = ((now - previous) / 1_000_000_000f).coerceAtMost(MAX_FRAME_SECONDS)
            previous = now

            evaluateSwap()

            val velocity = autoScrollVelocity()
            if (velocity == 0f) {
                scrollingForMs = 0f
                continue
            }
            scrollingForMs += seconds * 1000f
            val ramp = (RAMP_START + (1f - RAMP_START) * scrollingForMs / RAMP_MS).coerceAtMost(1f)
            val consumed = lazyListState.scrollBy(velocity * ramp * seconds)
            lastTop -= consumed
        }
    }

    /** Translation, in px, to draw [key] at. Read from a `graphicsLayer` block: draw phase only. */
    internal fun translationOf(key: Any): Float {
        if (block.isEmpty()) return 0f
        return when {
            key in draggedKeys -> {
                val info = lazyListState.layoutInfo
                val bounds = layoutBounds(info)
                visualTop(bounds.extent, info) - bounds.top
            }
            key in settlingKeys -> {
                val bounds = layoutBounds(lazyListState.layoutInfo)
                (1f - settleProgress) * (settleFromTop - bounds.top)
            }
            else -> 0f
        }
    }

    /**
     * Scale to draw [key] at while held: the shrink that makes a tall card manageable, or
     * [base] (a slight lift) for a card that is small enough as it is.
     */
    internal fun heldScale(base: Float): Float = if (blockScale < 1f) blockScale else base

    /**
     * Where, as a fraction of [key]'s own height, the finger holds the block. Scaling around it
     * keeps the held point under the finger however much the card is shrunk.
     */
    internal fun pivotFractionY(key: Any): Float {
        if (block.isEmpty()) return 0.5f
        val info = lazyListState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return 0.5f
        val blockTop = layoutBounds(info).top
        return (blockTop + grabOffset - item.offset) / item.size.coerceAtLeast(1)
    }

    private fun evaluateSwap() {
        if (block.isEmpty() || !isDragging) return
        val order = liveOrder ?: return
        val info = lazyListState.layoutInfo
        val position = positions(order)
        // The layout is one swap behind until it has re-measured: deciding on it now would
        // swap the same pair twice
        if (!layoutFollows(position, info)) return

        val visible = HashMap<Any, LazyListItemInfo>()
        info.visibleItemsInfo.forEach { visible[it.key] = it }
        val start = position[block.first()] ?: return
        val end = start + block.size - 1

        layoutBounds(info)
        val finger = fingerY.coerceIn(info.viewportStartOffset.toFloat(), info.viewportEndOffset.toFloat())

        // The finger must get past the middle of the neighbour, by a small margin, before the two
        // trade places. The margin plus the size of the held block is what it takes to swap them
        // back, so a hand that wobbles on the line never makes them flicker
        if (start > 0) {
            val above = blockAround(order, start - 1)
            val bounds = boundsOf(order, above, visible)
            if (bounds != null && finger < bounds.center - metrics.swapMargin) {
                moveBlock(order, start, end, above, up = true)
                return
            }
        }
        if (end < order.lastIndex) {
            val below = blockAround(order, end + 1)
            val bounds = boundsOf(order, below, visible)
            if (bounds != null && finger > bounds.center + metrics.swapMargin) {
                moveBlock(order, start, end, below, up = false)
            }
        }
    }

    private fun moveBlock(order: List<Any>, start: Int, end: Int, neighbour: IntRange, up: Boolean) {
        val moving = order.subList(start, end + 1).toList()
        val rest = order.toMutableList()
        repeat(moving.size) { rest.removeAt(start) }
        val insertAt = if (up) neighbour.first else start + (neighbour.last - neighbour.first + 1)
        rest.addAll(insertAt, moving)

        // A lazy list keeps its scroll position by key, not by index. When the item that is first
        // on screen is the one that just changed slot, the whole viewport would follow it and
        // jump by the height of the item it passed, which in turn triggers the next swap and
        // sends the block racing through the list. Pinning the same index and offset keeps every
        // other item exactly where it is.
        val firstIndex = lazyListState.firstVisibleItemIndex
        val firstOffset = lazyListState.firstVisibleItemScrollOffset
        liveOrder = rest
        lazyListState.requestScrollToItem(firstIndex, firstOffset)
        tick(HapticFeedbackType.TextHandleMove)
    }

    /** Signed px/s: positive scrolls the content up (towards the end of the list). */
    private fun autoScrollVelocity(): Float {
        if (!armed || block.isEmpty()) return 0f
        val info = lazyListState.layoutInfo
        val start = info.viewportStartOffset.toFloat()
        val end = info.viewportEndOffset.toFloat()
        val zone = ((end - start) * EDGE_ZONE_FRACTION).coerceIn(metrics.minEdgeZone, metrics.maxEdgeZone)
            .coerceAtMost((end - start) / 3f)
        if (zone <= 0f) return 0f

        val intoTop = (start + zone - fingerY) / zone
        val intoBottom = (fingerY - (end - zone)) / zone
        val velocity = when {
            intoTop > 0f -> -metrics.maxScrollSpeed * ease(intoTop)
            intoBottom > 0f -> metrics.maxScrollSpeed * ease(intoBottom)
            else -> 0f
        }
        if (velocity == 0f) return 0f

        // The lazy list drops an item as soon as it is fully off screen, and with it the held
        // block. So the slot the block belongs to must never be scrolled out of sight: if the
        // swaps haven't caught up with the finger, hold the list still until they do.
        val bounds = layoutBounds(info)
        val keep = minOf(bounds.extent, metrics.keepVisible)
        if (velocity > 0f && bounds.bottom < start + keep) return 0f
        if (velocity < 0f && bounds.top > end - keep) return 0f
        return velocity
    }

    /** Where the dragged block sits in the layout now, falling back to its last known place. */
    private fun layoutBounds(info: LazyListLayoutInfo): Bounds {
        val firstKey = block.first()
        val lastKey = block.last()
        var first: LazyListItemInfo? = null
        var last: LazyListItemInfo? = null
        for (item in info.visibleItemsInfo) {
            if (item.key == firstKey) first = item
            if (item.key == lastKey) last = item
        }
        val extent = if (first != null && last != null) {
            (last.offset + last.size - first.offset).toFloat()
        } else {
            lastExtent
        }
        val top = when {
            first != null -> first.offset.toFloat()
            last != null -> last.offset + last.size - extent
            else -> lastTop
        }
        lastTop = top
        lastExtent = extent
        return Bounds(top, top + extent)
    }

    /**
     * Top of the dragged block as the user sees it: held by the finger and kept inside the list,
     * allowing for the smaller size a tall block is drawn at.
     */
    private fun visualTop(extent: Float, info: LazyListLayoutInfo): Float {
        val start = info.viewportStartOffset.toFloat()
        val end = info.viewportEndOffset.toFloat()
        val above = blockScale * grabOffset
        val below = blockScale * (extent - grabOffset)
        val lowest = minOf(start + above, end - below)
        val highest = maxOf(start + above, end - below)
        return fingerY.coerceIn(start, end).coerceIn(lowest, highest) - grabOffset
    }

    private fun boundsOf(order: List<Any>, range: IntRange, visible: Map<Any, LazyListItemInfo>): Bounds? {
        val first = visible[order[range.first]] ?: return null
        val last = visible[order[range.last]] ?: return null
        return Bounds(first.offset.toFloat(), (last.offset + last.size).toFloat())
    }

    private fun blockAround(order: List<Any>, index: Int): IntRange {
        val group = groupOf(order[index]) ?: return index..index
        var first = index
        var last = index
        while (first > 0 && groupOf(order[first - 1]) == group) first--
        while (last < order.lastIndex && groupOf(order[last + 1]) == group) last++
        return first..last
    }

    private fun positions(order: List<Any>): Map<Any, Int> {
        val result = HashMap<Any, Int>(order.size)
        order.forEachIndexed { index, key -> result[key] = index }
        return result
    }

    private fun layoutFollows(position: Map<Any, Int>, info: LazyListLayoutInfo): Boolean {
        var previous = -1
        for (item in info.visibleItemsInfo) {
            val index = position[item.key] ?: continue
            if (index < previous) return false
            previous = index
        }
        return true
    }

    private fun sameRelativeOrder(host: List<Any>, other: List<Any>): Boolean {
        val hostKeys = host.toHashSet()
        val otherKeys = other.toHashSet()
        return host.filter { it in otherKeys } == other.filter { it in hostKeys }
    }

    private fun ease(depth: Float): Float {
        return depth.coerceIn(0f, 1f).pow(EASE_EXPONENT)
    }

    private fun tick(type: HapticFeedbackType) {
        if (hapticEnabled()) haptic.performHapticFeedback(type)
    }

    private suspend fun withFrameNanos(): Long = androidx.compose.runtime.withFrameNanos { it }

    private companion object {
        const val HOST_SYNC_TIMEOUT_MS = 1500L
        const val MAX_FRAME_SECONDS = 0.05f
        const val RAMP_START = 0.45f
        const val RAMP_MS = 220f
        const val EDGE_ZONE_FRACTION = 0.22f
        const val EASE_EXPONENT = 1.7f
        const val TALL_CARD_VIEWPORT_FRACTION = 0.4f
        const val MIN_BLOCK_SCALE = 0.5f
    }
}

/**
 * @param keys keys of the draggable items in the host's display order, i.e. the keys given to
 *   `items(key = …)` minus any header or footer that must stay put.
 * @param groupOf items that are neighbours and share the same non-null value move as one block.
 * @param onCommit called once on drop with the new order, only if it differs from the host's.
 */
@Composable
fun rememberLazyListReorderState(
    lazyListState: LazyListState,
    keys: () -> List<Any>,
    groupOf: (Any) -> Any? = { null },
    hapticEnabled: Boolean = true,
    onCommit: (List<Any>) -> Unit
): LazyListReorderState {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentKeys by rememberUpdatedState(keys)
    val currentGroupOf by rememberUpdatedState(groupOf)
    val currentHapticEnabled by rememberUpdatedState(hapticEnabled)
    val currentOnCommit by rememberUpdatedState(onCommit)

    return remember(lazyListState, density) {
        LazyListReorderState(
            lazyListState = lazyListState,
            scope = scope,
            haptic = haptic,
            metrics = with(density) {
                LazyListReorderState.Metrics(
                    hitTolerance = 12.dp.toPx(),
                    minEdgeZone = 110.dp.toPx(),
                    maxEdgeZone = 200.dp.toPx(),
                    maxScrollSpeed = 1100.dp.toPx(),
                    armDistance = 12.dp.toPx(),
                    keepVisible = 48.dp.toPx(),
                    swapMargin = 12.dp.toPx()
                )
            },
            keys = { currentKeys() },
            groupOf = { currentGroupOf(it) },
            hapticEnabled = { currentHapticEnabled },
            onCommit = { currentOnCommit(it) }
        )
    }
}

/** Lets the list start a drag with a long press on any reorderable item. */
fun Modifier.reorderGestures(state: LazyListReorderState): Modifier = pointerInput(state) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset -> state.onDragStart(offset.y) },
        onDrag = { change, _ ->
            if (state.isDragging) {
                change.consume()
                state.onDrag(change.position.y)
            }
        },
        onDragEnd = { state.onDragEnd() },
        onDragCancel = { state.onDragEnd() }
    )
}

/**
 * Modifier for one item of the list: slides it aside when others are dragged past it, and lifts,
 * moves and settles it when it is the one being dragged.
 *
 * While an item is held its slot stays marked with a dashed outline, so it is always clear where
 * it will land, and a card too tall to see around is drawn smaller.
 *
 * Nothing here recomposes while the finger moves: the translation, scale and fade are all read
 * from the draw phase.
 *
 * @param ghostInsetX horizontal space between the item's bounds and the card it draws, if any
 * @param ghostInsetY vertical space between the item's bounds and the card it draws, if any
 */
@Composable
fun LazyItemScope.reorderableItem(
    state: LazyListReorderState,
    key: Any,
    shape: Shape = RectangleShape,
    liftedScale: Float = 1.03f,
    liftedElevation: Dp = 0.dp,
    ghostInsetX: Dp = 0.dp,
    ghostInsetY: Dp = 0.dp
): Modifier {
    val dragged = state.isDragged(key)
    val settling = state.isSettling(key)
    val othersFaded = state.isDragging && !dragged

    val lift = animateFloatAsState(
        targetValue = if (dragged) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "reorder_lift"
    )
    val fade = animateFloatAsState(
        targetValue = if (othersFaded) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "reorder_fade"
    )
    val elevationPx = with(LocalDensity.current) { liftedElevation.toPx() }

    val accent = Primary
    val ghost = remember(lift, shape, ghostInsetX, ghostInsetY, accent) {
        Modifier.drawBehind {
            val strength = lift.value.coerceIn(0f, 1f)
            if (strength > 0.01f) {
                inset(ghostInsetX.toPx(), ghostInsetY.toPx()) {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    drawOutline(outline, color = accent.copy(alpha = 0.08f * strength))
                    drawOutline(
                        outline,
                        color = accent.copy(alpha = 0.45f * strength),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16.dp.toPx(), 10.dp.toPx()))
                        )
                    )
                }
            }
        }
    }

    val layer = remember(state, key, shape, lift, fade, liftedScale, elevationPx) {
        Modifier.graphicsLayer {
            translationY = state.translationOf(key)
            val liftValue = lift.value
            val scale = 1f + (state.heldScale(liftedScale) - 1f) * liftValue
            scaleX = scale
            scaleY = scale
            transformOrigin = if (liftValue > 0.001f) {
                TransformOrigin(0.5f, state.pivotFractionY(key))
            } else {
                TransformOrigin.Center
            }
            alpha = 1f - OTHERS_FADE * fade.value
            shadowElevation = (elevationPx * liftValue).coerceAtLeast(0f)
            this.shape = shape
            clip = false
        }
    }

    return Modifier
        // The held item is positioned by the finger; letting it animate its own placement too
        // would fight the translation every time it swaps slot
        .then(
            if (dragged) {
                Modifier
            } else {
                Modifier.animateItem(
                    placementSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = 800f,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )
                )
            }
        )
        // Stay on top until the dropped item has settled into its slot
        .zIndex(if (dragged || settling) 1f else 0f)
        .then(ghost)
        .then(layer)
}

private const val OTHERS_FADE = 0.3f
