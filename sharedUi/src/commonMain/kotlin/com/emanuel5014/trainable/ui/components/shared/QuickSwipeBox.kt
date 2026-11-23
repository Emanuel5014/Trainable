package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.emanuel5014.trainable.ui.components.motion.toComposePath
import com.emanuel5014.trainable.ui.screens.onboarding.rememberReduceMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

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
 * One quick action of a row: its icon, the label read by screen readers, and the colour pair it is drawn with
 * ([color] for the shape, [onColor] for the icon on it, [container] for the flood behind).
 */
data class QuickSwipeAction(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onColor: Color,
    val container: Color
)

/** Delete in the theme's error colours. */
@Composable
fun quickSwipeDanger(icon: ImageVector, label: String): QuickSwipeAction = with(MaterialTheme.colorScheme) {
    QuickSwipeAction(icon, label, error, onError, errorContainer)
}

/** Archive, edit and the other non-destructive actions in the theme's primary colours. */
@Composable
fun quickSwipePrimary(icon: ImageVector, label: String): QuickSwipeAction = with(MaterialTheme.colorScheme) {
    QuickSwipeAction(icon, label, primary, onPrimary, primaryContainer)
}

/** Where the icon sits, measured from the edge the row uncovers. */
private val IconInset = 40.dp

/**
 * A row with quick swipe actions that leaves fast flicks to the main tabs (see [QuickSwipeMath]).
 *
 * Replaces SwipeToDismissBox, which fired after 56dp or on any flick over 125dp/s and so took most tab swipes that
 * started on a row. A null action turns that direction off.
 *
 * Behind the row the action's icon sits in a Material shape that grows and turns with the finger. Past the
 * threshold it pops into a cookie that keeps turning and pulsing while its container colour floods the row from the
 * icon; firing the action sends out a ring as the row springs back. All of it is drawn, so dragging never recomposes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuickSwipeBox(
    startToEnd: QuickSwipeAction?,
    endToStart: QuickSwipeAction?,
    onSwiped: (SwipeToDismissBoxValue) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pager = LocalMainPagerState.current
    val pagerFling = pager?.let { PagerDefaults.flingBehavior(it) }
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptic = LocalHapticFeedback.current
    val hapticEnabled by LocalHapticsSetting.current.collectAsState(initial = true)
    val reduceMotion = rememberReduceMotion()
    val scope = rememberCoroutineScope()
    val currentOnSwiped by rememberUpdatedState(onSwiped)
    val currentHaptic by rememberUpdatedState(hapticEnabled)
    val enableStartToEnd = startToEnd != null
    val enableEndToStart = endToStart != null

    var offsetX by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }
    var armed by remember { mutableStateOf(false) }
    val settleJob = remember { arrayOfNulls<Job>(1) }

    // Drawn values: read only in drawBehind
    val flood = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val burst = remember { Animatable(1f) }
    var spin by remember { mutableFloatStateOf(0f) }
    // Which side was last uncovered (-1 right, 1 left), so the ring and the spring back keep their side at 0
    var side by remember { mutableFloatStateOf(1f) }

    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    val startPainter = startToEnd?.let { rememberVectorPainter(it.icon) }
    val endPainter = endToStart?.let { rememberVectorPainter(it.icon) }
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }

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
        settleJob[0] = scope.launch {
            animate(from, 0f, animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) { value, _ ->
                offsetX = value
            }
        }
    }

    // Past the threshold: the shape pops, floods the row and keeps turning; back under it, everything settles
    LaunchedEffect(armed) {
        launch {
            flood.animateTo(
                if (armed) 1f else 0f,
                spring(dampingRatio = 0.75f, stiffness = if (armed) 260f else 400f)
            )
        }
        if (reduceMotion) return@LaunchedEffect
        if (armed) {
            launch {
                pop.snapTo(1.22f)
                pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
            }
            var last = withFrameNanos { it }
            while (isActive) {
                withFrameNanos { now ->
                    spin += (now - last) / 1_000_000_000f * 126f
                    last = now
                }
            }
        } else {
            pop.snapTo(0.92f)
            pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f))
        }
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
                val raw = offsetX + dx
                val maxEnd = if (hasAction(SwipeToDismissBoxValue.StartToEnd)) width.toFloat() else 0f
                val maxStart = if (hasAction(SwipeToDismissBoxValue.EndToStart)) width.toFloat() else 0f
                // In RTL the start side is on the right, so the bounds swap
                val upper = if (isRtl) maxStart else maxEnd
                val lower = if (isRtl) -maxEnd else -maxStart
                offsetX = raw.coerceIn(lower, upper)
                if (offsetX != 0f) side = if (offsetX > 0f) 1f else -1f
                val nowArmed = abs(offsetX) >= thresholdPx
                if (nowArmed != armed) {
                    armed = nowArmed
                    if (currentHaptic) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
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
            armed = false
            when (outcome) {
                QuickSwipeRelease.ACTION -> {
                    if (!reduceMotion) scope.launch {
                        burst.snapTo(0f)
                        burst.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
                    }
                    currentOnSwiped(directionOf(offsetX))
                }
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

    // Screen readers cannot swipe: the same actions as custom actions
    val a11y = Modifier.semantics {
        customActions = listOfNotNull(
            startToEnd?.let { CustomAccessibilityAction(it.label) { currentOnSwiped(SwipeToDismissBoxValue.StartToEnd); true } },
            endToStart?.let { CustomAccessibilityAction(it.label) { currentOnSwiped(SwipeToDismissBoxValue.EndToStart); true } }
        )
    }

    Box(modifier = modifier.onSizeChanged { width = it.width }.then(gestures).then(a11y)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .drawBehind {
                    val x = offsetX
                    val ring = burst.value
                    val ringRunning = ring < 1f
                    if (abs(x) < 0.5f && !ringRunning) return@drawBehind
                    val leftSide = (if (x != 0f) x > 0f else side > 0f)
                    // A finger moving right uncovers the left edge: that is StartToEnd in LTR, EndToStart in RTL
                    val startSide = leftSide != isRtl
                    val action = (if (startSide) startToEnd else endToStart) ?: return@drawBehind
                    val painter = (if (startSide) startPainter else endPainter) ?: return@drawBehind
                    val threshold = QuickSwipeMath.actionThresholdDp(size.width.toDp().value).dp.toPx()
                    val progress = (abs(x) / threshold).coerceIn(0f, 1f)
                    drawSwipeAction(
                        action, painter, morph,
                        center = Offset(if (leftSide) IconInset.toPx() else size.width - IconInset.toPx(), size.height / 2f),
                        progress = progress, flood = flood.value, pop = pop.value, spin = spin, ring = ring,
                        surface = surface
                    )
                }
        )
        Box(modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) }) {
            content()
        }
    }
}

/** One frame of the action behind the row (see [QuickSwipeBox]). */
private fun DrawScope.drawSwipeAction(
    action: QuickSwipeAction,
    painter: VectorPainter,
    morph: Morph,
    center: Offset,
    progress: Float,
    flood: Float,
    pop: Float,
    spin: Float,
    ring: Float,
    surface: Color
) {
    drawRect(surface)

    // The container colour spreads from the icon, then floods the whole row once armed
    val small = 26.dp.toPx() * progress
    val full = hypot(size.width, size.height)
    val floodRadius = small + (full - small) * flood
    if (floodRadius > 0f) {
        drawCircle(action.container.copy(alpha = 0.55f + 0.45f * flood), floodRadius, center)
    }

    // Ring sent out when the action fires
    val ringOn = ring < 1f
    if (ringOn) {
        val fade = 1f - ring
        drawCircle(
            action.color.copy(alpha = fade),
            radius = 24.dp.toPx() + 120.dp.toPx() * ring,
            center = center,
            style = Stroke(width = 6.dp.toPx() * fade)
        )
    }

    // Circle under the threshold, cookie over it; it grows and turns with the finger and pulses while armed
    val pulse = 1f + 0.03f * flood * sin(spin / 180f * PI.toFloat() * 4f)
    val grow = if (ringOn) 1f + 0.6f * ring else 1f
    val radius = (16.dp.toPx() + 8.dp.toPx() * progress) * pop * pulse * grow
    val shapeAlpha = if (ringOn) 1f - ring else 1f
    val path = morph.toComposePath(flood.coerceIn(0f, 1f))
    path.transform(Matrix().apply { scale(radius * 2f, radius * 2f) })
    path.translate(center - Offset(radius, radius))
    rotate(progress * 90f + spin, pivot = center) {
        drawPath(path, action.color.copy(alpha = action.color.alpha * shapeAlpha))
    }

    // The icon stays upright on top
    val iconSize = 24.dp.toPx()
    val iconAlpha = (progress / 0.15f).coerceIn(0f, 1f) * shapeAlpha
    if (iconAlpha > 0f) {
        translate(center.x - iconSize / 2f, center.y - iconSize / 2f) {
            scale(pop, pivot = Offset(iconSize / 2f, iconSize / 2f)) {
                with(painter) {
                    draw(Size(iconSize, iconSize), alpha = iconAlpha, colorFilter = ColorFilter.tint(action.onColor))
                }
            }
        }
    }
}
