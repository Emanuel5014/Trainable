package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.ui.screens.routines.ScanPanelMath
import kotlin.test.Test
import kotlin.test.assertEquals

/** Where the scan review's exercise panel stops: Alto, Metà, Basso, by position and fling. */
class ScanPanelMathTest {
    // Sheet 1000 px tall, photo strip 120 px, summary peek 200 px: Alto 120, Metà 500, Basso 800
    private val anchors = ScanPanelMath.anchors(containerPx = 1000f, stripPx = 120f, peekPx = 200f)

    @Test
    fun anchorsAreAltoMetaBasso() = assertEquals(listOf(120f, 500f, 800f), anchors)

    @Test
    fun aSmallSheetKeepsTheAnchorsInOrder() {
        val small = ScanPanelMath.anchors(containerPx = 400f, stripPx = 120f, peekPx = 300f)
        assertEquals(small.sorted(), small)
        assertEquals(3, small.distinct().size)
    }

    @Test
    fun aSlowReleaseGoesToTheNearestHeight() {
        assertEquals(500f, ScanPanelMath.settle(offset = 430f, velocity = 0f, anchors = anchors, flingPx = 1000f))
        assertEquals(800f, ScanPanelMath.settle(offset = 700f, velocity = 300f, anchors = anchors, flingPx = 1000f))
    }

    @Test
    fun aFlingDownGoesToTheNextLowerHeight() =
        assertEquals(800f, ScanPanelMath.settle(offset = 520f, velocity = 2000f, anchors = anchors, flingPx = 1000f))

    @Test
    fun aFlingUpGoesToTheNextHigherHeight() =
        assertEquals(120f, ScanPanelMath.settle(offset = 480f, velocity = -2000f, anchors = anchors, flingPx = 1000f))

    @Test
    fun aFlingPastTheEndStaysAtTheEnd() {
        assertEquals(800f, ScanPanelMath.settle(offset = 800f, velocity = 3000f, anchors = anchors, flingPx = 1000f))
        assertEquals(120f, ScanPanelMath.settle(offset = 120f, velocity = -3000f, anchors = anchors, flingPx = 1000f))
    }

    @Test
    fun aFlingFromAHeightMovesOneStep() =
        assertEquals(500f, ScanPanelMath.settle(offset = 120f, velocity = 2000f, anchors = anchors, flingPx = 1000f))

    // ---- the sheet is measured again (keyboard, footer, header) --------------------------------------

    @Test
    fun aRestingPanelFollowsItsHeight() =
        assertEquals(510f, ScanPanelMath.remeasured(offset = 500f, resting = true, step = 1, newAnchors = listOf(120f, 510f, 810f)))

    @Test
    fun aPanelUnderTheFingerStaysUnderTheFinger() =
        assertEquals(554f, ScanPanelMath.remeasured(offset = 554f, resting = false, step = 1, newAnchors = listOf(120f, 500f, 803f)))

    @Test
    fun aPanelUnderTheFingerIsKeptInsideTheNewRange() =
        assertEquals(700f, ScanPanelMath.remeasured(offset = 790f, resting = false, step = 2, newAnchors = listOf(120f, 400f, 700f)))
}
