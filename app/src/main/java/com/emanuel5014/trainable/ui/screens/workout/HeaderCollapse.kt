package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion

/** One step of the collapse: where the header is now ([offsetPx], 0 expanded) and how much of the scroll it took. */
data class CollapseStep(val offsetPx: Float, val consumedPx: Float)

/**
 * The numbers behind the exercise header that shrinks to one row while the sets are scrolled: plain math, so it can be
 * tested without a screen. Deltas follow the scroll: negative is the content moving up (the finger moving up).
 */
object HeaderCollapse {
    /** How collapsed the header is, from 0 (expanded) to 1 (one row); 0 while there is nothing to collapse. */
    fun fraction(offsetPx: Float, travelPx: Float): Float =
        if (travelPx <= 0f) 0f else (offsetPx / travelPx).coerceIn(0f, 1f)

    /** The list is about to scroll up: the header takes what it can of it first, so it collapses before the sets move. */
    fun preScroll(offsetPx: Float, deltaPx: Float, travelPx: Float): CollapseStep {
        if (deltaPx >= 0f || travelPx <= 0f) return CollapseStep(offsetPx.coerceIn(0f, travelPx.coerceAtLeast(0f)), 0f)
        val next = (offsetPx - deltaPx).coerceIn(0f, travelPx)
        return CollapseStep(next, -(next - offsetPx))
    }

    /** What the list could not use of a downward scroll (it is at the top) expands the header. */
    fun postScroll(offsetPx: Float, remainingDeltaPx: Float, travelPx: Float): CollapseStep {
        if (remainingDeltaPx <= 0f || travelPx <= 0f) return CollapseStep(offsetPx.coerceIn(0f, travelPx.coerceAtLeast(0f)), 0f)
        val next = (offsetPx - remainingDeltaPx).coerceIn(0f, travelPx)
        return CollapseStep(next, offsetPx - next)
    }

    /** Where the header goes when the list is moved by code (not by a finger): away from the top it collapses. */
    fun snapTarget(listScrolled: Boolean): Float = if (listScrolled) 1f else 0f

    /** Where a header let go half way ends up: the nearer end, 0 (expanded) or 1 (one row). */
    fun settleTarget(offsetPx: Float, travelPx: Float): Float = if (fraction(offsetPx, travelPx) >= 0.5f) 1f else 0f

    /** The expanded header is gone by the half of the collapse, so it never shows together with the one-row header. */
    fun expandedAlpha(fraction: Float): Float = (1f - 2f * fraction).coerceIn(0f, 1f)

    /** The one-row header only shows in the second half of the collapse. */
    fun compactAlpha(fraction: Float): Float = (2f * fraction - 1f).coerceIn(0f, 1f)
}

/** When the tools of the exercise leave the row of the pills for a row of their own. */
object HeaderLayout {
    private const val NARROW_WIDTH_DP = 340f
    private const val LARGE_FONT_SCALE = 1.3f

    fun toolsOnOwnRow(widthDp: Float, fontScale: Float): Boolean =
        widthDp < NARROW_WIDTH_DP || fontScale >= LARGE_FONT_SCALE
}

/**
 * The collapse of the header of one exercise: [travelPx] is how much shorter the one-row header is than the expanded
 * one (measured by the header itself), [offsetPx] how far it has collapsed. The list of the sets reports its scroll
 * through [connection].
 */
@Stable
class HeaderCollapseState {
    var travelPx by mutableFloatStateOf(0f)
    var offsetPx by mutableFloatStateOf(0f)

    val fraction: Float get() = HeaderCollapse.fraction(offsetPx, travelPx)

    /** Whether scrolling up may collapse the header: not when every set already fits on screen, it would only hide the name. */
    var canCollapse: () -> Boolean = { true }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < 0f && !canCollapse()) return Offset.Zero
            val step = HeaderCollapse.preScroll(offsetPx, available.y, travelPx)
            offsetPx = step.offsetPx
            return Offset(0f, step.consumedPx)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            val step = HeaderCollapse.postScroll(offsetPx, available.y, travelPx)
            offsetPx = step.offsetPx
            return Offset(0f, step.consumedPx)
        }

        // The finger is up (a drag or a fling ended): a header left half way goes to the nearer end
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle()
            return Velocity.Zero
        }
    }

    suspend fun settle() {
        if (offsetPx > 0f && offsetPx < travelPx) snapTo(HeaderCollapse.settleTarget(offsetPx, travelPx))
    }

    /** Moves the header to [target] (0 expanded, 1 collapsed) with a spring. */
    suspend fun snapTo(target: Float) {
        animate(offsetPx, target.coerceIn(0f, 1f) * travelPx, animationSpec = WorkoutMotion.spatial()) { value, _ ->
            offsetPx = value
        }
    }
}

/** The collapse state of the exercise on screen: a fresh, expanded one for each exercise. */
@Composable
fun rememberHeaderCollapseState(exerciseIndex: Int): HeaderCollapseState = remember(exerciseIndex) { HeaderCollapseState() }
