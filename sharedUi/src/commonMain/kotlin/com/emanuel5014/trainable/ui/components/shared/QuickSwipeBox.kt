package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The pager of the four main tabs, given to the rows with quick swipe actions inside it so a fast flick on a row
 * changes tab instead of firing the action. Null outside the main tabs: there a row only has its own actions.
 */
val LocalMainPagerState = staticCompositionLocalOf<PagerState?> { null }

/** What a horizontal swipe on a row with quick actions is for. */
enum class QuickSwipeIntent { UNDECIDED, ROW, PAGE }

/** What happens when the finger leaves a row that was being dragged. */
enum class QuickSwipeRelease { ACTION, PAGE, BACK }

/**
 * The rules that tell a tab-changing flick from a quick action, in dp so they can be tested without a screen.
 *
 * A page swipe is a short, fast flick; a quick action is a drag that is followed with the finger. So the speed of
 * the first few dp decides, and the action needs a real distance and fires only when the finger is lifted.
 */
object QuickSwipeMath {
    /** Distance past the touch slop over which the opening speed is judged. */
    const val DECISION_DP = 24f

    /** Average speed from touch down that means "change tab" (a deliberate drag is well under it). */
    const val PAGE_FLICK_DP_PER_S = 900f

    /** Release speed that still means "change tab" when the row has not gone far. */
    const val LATE_FLICK_DP_PER_S = 1000f

    /** Share of the row width the row must be dragged for the action. */
    const val ACTION_FRACTION = 0.4f

    /** The action never needs less than this, even on a narrow row. */
    const val MIN_ACTION_DP = 96f

    fun actionThresholdDp(rowWidthDp: Float): Float = maxOf(MIN_ACTION_DP, rowWidthDp * ACTION_FRACTION)

    /**
     * Decides the gesture while it opens. [travelDp] is the distance past the touch slop, [speedDpPerS] the average
     * speed since touch down, [directionHasAction] whether the row has an action that way and [canPage] whether a
     * pager can take the gesture.
     */
    fun opening(travelDp: Float, speedDpPerS: Float, directionHasAction: Boolean, canPage: Boolean): QuickSwipeIntent =
        when {
            !directionHasAction -> if (canPage) QuickSwipeIntent.PAGE else QuickSwipeIntent.ROW
            !canPage -> QuickSwipeIntent.ROW
            travelDp < DECISION_DP -> QuickSwipeIntent.UNDECIDED
            speedDpPerS >= PAGE_FLICK_DP_PER_S -> QuickSwipeIntent.PAGE
            else -> QuickSwipeIntent.ROW
        }

    /**
     * Decides what lifting the finger does to a row moved by [offsetDp] (signed). [velocityDpPerS] is the signed
     * release speed along the same axis.
     */
    fun release(offsetDp: Float, thresholdDp: Float, velocityDpPerS: Float, canPage: Boolean): QuickSwipeRelease =
        when {
            offsetDp != 0f && abs(offsetDp) >= thresholdDp -> QuickSwipeRelease.ACTION
            canPage && abs(velocityDpPerS) >= LATE_FLICK_DP_PER_S -> QuickSwipeRelease.PAGE
            else -> QuickSwipeRelease.BACK
        }
}

/**
 * A row with quick swipe actions that leaves fast flicks to the main tabs (see [QuickSwipeMath]).
 *
 * Replaces SwipeToDismissBox, which fired after 56dp or on any flick over 125dp/s and so took most tab swipes that
 * started on a row. [backgroundContent] gets the direction being dragged and the progress towards the action (1 =
 * lifting the finger fires it).
 */
@Composable
fun QuickSwipeBox(
    enableStartToEnd: Boolean,
    enableEndToStart: Boolean,
    onSwiped: (SwipeToDismissBoxValue) -> Unit,
    backgroundContent: @Composable BoxScope.(direction: SwipeToDismissBoxValue, progress: Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pager = LocalMainPagerState.current
    val pagerFling = pager?.let { PagerDefaults.flingBehavior(it) }
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptic = LocalHapticFeedback.current
    val hapticEnabled by LocalHapticsSetting.current.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    val currentOnSwiped by rememberUpdatedState(onSwiped)
    val currentHaptic by rememberUpdatedState(hapticEnabled)

    var offsetX by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }
    val settleJob = remember { arrayOfNulls<Job>(1) }

    fun directionOf(dx: Float): SwipeToDismissBoxValue =
        if ((dx > 0f) != isRtl) SwipeToDismissBoxValue.StartToEnd else SwipeToDismissBoxValue.EndToStart

    fun hasAction(direction: SwipeToDismissBoxValue) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> enableStartToEnd
        SwipeToDismissBoxValue.EndToStart -> enableEndToStart
        SwipeToDismissBoxValue.Settled -> false
    }

    fun settleRow() {
        settleJob[0]?.cancel()
        val from = offsetX
        if (from == 0f) return
        settleJob[0] = scope.launch { animate(from, 0f) { value, _ -> offsetX = value } }
    }

    val gestures = if (!enableStartToEnd && !enableEndToStart) Modifier else Modifier.pointerInput(
        enableStartToEnd, enableEndToStart, pager, isRtl
    ) {
        fun Float.toDp() = this / density.density
        // A positive pager delta goes to the next tab, which is a finger moving towards the start
        fun pagerDelta(dx: Float) = if (isRtl) dx else -dx

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val tracker = VelocityTracker()
            tracker.addPointerInputChange(down)
            var overSlop = 0f
            val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                change.consume()
                overSlop = over
            } ?: return@awaitEachGesture
            tracker.addPointerInputChange(start)

            settleJob[0]?.cancel()
            val canPage = pager != null && pagerFling != null && !BottomBarManager.swipeLocked
            val thresholdPx = QuickSwipeMath.actionThresholdDp(width.toFloat().toDp()) * density.density
            val openingDirection = directionOf(overSlop)
            var intent = QuickSwipeIntent.UNDECIDED
            var travel = 0f
            // Movement the row has shown, handed to the pager if the gesture turns out to be a flick
            var pendingPagerDelta = 0f

            fun moveRow(dx: Float) {
                val before = offsetX
                val raw = before + dx
                val maxEnd = if (hasAction(SwipeToDismissBoxValue.StartToEnd)) width.toFloat() else 0f
                val maxStart = if (hasAction(SwipeToDismissBoxValue.EndToStart)) width.toFloat() else 0f
                // In RTL the start side is on the right, so the bounds swap
                val upper = if (isRtl) maxStart else maxEnd
                val lower = if (isRtl) -maxEnd else -maxStart
                offsetX = raw.coerceIn(lower, upper)
                if (currentHaptic && (abs(before) >= thresholdPx) != (abs(offsetX) >= thresholdPx)) {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
            }

            fun handle(dx: Float, uptime: Long) {
                if (intent == QuickSwipeIntent.UNDECIDED) {
                    travel += abs(dx)
                    val elapsed = (uptime - down.uptimeMillis).coerceAtLeast(1L)
                    val speed = (abs(start.position.x - down.position.x) + travel).toDp() / (elapsed / 1000f)
                    intent = QuickSwipeMath.opening(travel.toDp(), speed, hasAction(openingDirection), canPage)
                    if (intent == QuickSwipeIntent.PAGE) {
                        pager!!.dispatchRawDelta(pagerDelta(pendingPagerDelta + dx))
                        settleRow()
                        return
                    }
                    pendingPagerDelta += dx
                    moveRow(dx)
                    return
                }
                when (intent) {
                    QuickSwipeIntent.PAGE -> pager!!.dispatchRawDelta(pagerDelta(dx))
                    else -> moveRow(dx)
                }
            }

            if (canPage) scope.launch { pager!!.stopScroll() }
            handle(overSlop, start.uptimeMillis)
            val ended = horizontalDrag(start.id) { change ->
                tracker.addPointerInputChange(change)
                handle(change.positionChange().x, change.uptimeMillis)
                change.consume()
            }

            val velocityX = if (ended) tracker.calculateVelocity().x else 0f
            val outcome = when (intent) {
                QuickSwipeIntent.PAGE -> QuickSwipeRelease.PAGE
                else -> QuickSwipeMath.release(offsetX.toDp(), thresholdPx.toDp(), velocityX.toDp(), canPage)
            }
            when (outcome) {
                QuickSwipeRelease.ACTION -> currentOnSwiped(directionOf(offsetX))
                QuickSwipeRelease.PAGE -> if (pager != null && pagerFling != null) {
                    scope.launch {
                        pager.scroll { with(pagerFling) { performFling(pagerDelta(velocityX)) } }
                    }
                }
                QuickSwipeRelease.BACK -> {}
            }
            settleRow()
        }
    }

    Box(modifier = modifier.onSizeChanged { width = it.width }.then(gestures)) {
        val direction = when {
            offsetX == 0f -> SwipeToDismissBoxValue.Settled
            else -> directionOf(offsetX)
        }
        val threshold = QuickSwipeMath.actionThresholdDp(width / density.density) * density.density
        val progress = if (threshold > 0f) (abs(offsetX) / threshold).coerceIn(0f, 1f) else 0f
        Box(modifier = Modifier.matchParentSize()) {
            backgroundContent(direction, progress)
        }
        Box(modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) }) {
            content()
        }
    }
}
