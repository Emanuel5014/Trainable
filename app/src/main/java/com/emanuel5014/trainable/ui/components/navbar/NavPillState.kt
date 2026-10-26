package com.emanuel5014.trainable.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Where the floating nav bar's pill is, in items (see [NavPillMath]). Three things move it, in this order:
 * a finger dragging it, a settle spring after a tap or a release, and otherwise the pager itself.
 *
 * Every switch between those keeps the pill where it was drawn: the jump is parked in [bridge]
 * and eased out, so the pill never teleports.
 */
@Stable
class NavPillState(
    private val pagerState: PagerState,
    val count: Int,
    private val scope: CoroutineScope
) {
    private val lastIndex = (count - 1).coerceAtLeast(0)

    private var shown by mutableFloatStateOf(pagerState.currentPage.toFloat())
    private var bridge by mutableFloatStateOf(0f)
    private var dragFinger by mutableFloatStateOf(0f)
    private var isSettling by mutableStateOf(false)
    private var settleJob: Job? = null
    private var bridgeJob: Job? = null
    private var settleToken = 0

    var isDragging by mutableStateOf(false)
        private set

    /** Read this in draw or layout lambdas, not in composition: it changes every frame while the pill moves. */
    val position: Float
        get() = ((if (isDragging) dragFinger else shown) + bridge).coerceIn(0f, lastIndex.toFloat())

    /** [fingerPosition] is where the pill would sit centred under the finger. */
    fun beginDrag(fingerPosition: Float) {
        carryOver(from = position, to = fingerPosition)
        dragFinger = fingerPosition
        isDragging = true
    }

    fun dragTo(fingerPosition: Float) {
        dragFinger = fingerPosition
    }

    /** A cancelled gesture goes back to the page the pager was heading to instead of committing the drop. */
    fun endDrag(cancelled: Boolean) {
        if (!isDragging) return
        val dropped = position
        val target = if (cancelled) pagerState.targetPage else NavPillMath.nearestIndex(dropped, count)
        shown = dropped
        clearBridge()
        isDragging = false
        settleOn(target)
    }

    /** Springs the pill to [index] while the pager scrolls there; also used by taps. */
    fun settleOn(index: Int) {
        val target = index.coerceIn(0, lastIndex)
        shown = position
        clearBridge()
        val token = ++settleToken
        settleJob?.cancel()
        isSettling = true
        settleJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                coroutineScope {
                    launch { pagerState.animateScrollToPage(target) }
                    launch {
                        animate(
                            initialValue = shown,
                            targetValue = target.toFloat(),
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { value, _ -> shown = value }
                    }
                }
            } finally {
                // a newer settle already owns the flag
                if (token == settleToken) isSettling = false
            }
        }
    }

    /** Glues the pill to the pager whenever neither a finger nor a settle spring owns it. */
    suspend fun followPager() {
        var held = false
        snapshotFlow {
            if (isDragging || isSettling) null
            else NavPillMath.positionForPager(pagerState.currentPage, pagerState.currentPageOffsetFraction, count)
        }.collect { pagerPosition ->
            if (pagerPosition == null) {
                held = true
            } else {
                if (held) {
                    held = false
                    // e.g. the user swiped the pager while a settle spring was still running
                    carryOver(from = position, to = pagerPosition)
                }
                shown = pagerPosition
            }
        }
    }

    private fun carryOver(from: Float, to: Float) {
        bridge = from - to
        bridgeJob?.cancel()
        bridgeJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            animate(
                initialValue = bridge,
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) { value, _ -> bridge = value }
        }
    }

    private fun clearBridge() {
        bridgeJob?.cancel()
        bridge = 0f
    }
}

@Composable
fun rememberNavPillState(pagerState: PagerState, count: Int): NavPillState {
    val scope = rememberCoroutineScope()
    val state = remember(pagerState, count) { NavPillState(pagerState, count, scope) }
    LaunchedEffect(state) { state.followPager() }
    return state
}
