package com.emanuel5014.trainable.ui

import kotlin.test.*

/** The timings of Android's ic_splash_logo_animated and MainActivity's exit animation. */
class IosSplashTest {
    private fun assertNear(expected: Float, actual: Float) = assertEquals(expected, actual, 0.001f)

    @Test fun thePiecesStartSmallTransparentAndTurnedOppositeWays() {
        val a = splashPartA(0)
        val b = splashPartB(0)
        assertNear(0.2f, a.scale); assertNear(0.2f, b.scale)
        assertNear(-35f, a.rotation); assertNear(35f, b.rotation)
        assertNear(0f, a.alpha); assertNear(0f, b.alpha)
    }

    @Test fun thePiecesSettleAtDifferentTimes() {
        val a = splashPartA(560)
        assertNear(1f, a.scale); assertNear(0f, a.rotation); assertNear(1f, a.alpha)
        val b = splashPartB(560)
        assertTrue(b.scale != 1f && b.rotation > 0f, "the second piece is still moving")
        val settled = splashPartB(660)
        assertNear(1f, settled.scale); assertNear(0f, settled.rotation); assertNear(1f, settled.alpha)
        assertEquals(settled, splashPartB(SPLASH_LOGO_MILLIS))
    }

    @Test fun theScaleOvershootsBeforeSettling() {
        assertTrue((0..520).maxOf { splashPartA(it.toLong()).scale } > 1f)
        assertTrue((0..640).maxOf { splashPartB(it.toLong()).scale } > 1f)
    }

    @Test fun theOverlayGrowsAndFadesAtTheEnd() {
        val start = splashFadeOut(0)
        assertNear(1f, start.alpha); assertNear(1f, start.scale)
        val end = splashFadeOut(SPLASH_FADE_MILLIS)
        assertNear(0f, end.alpha); assertNear(1.08f, end.scale)
        assertTrue(splashFadeOut(SPLASH_FADE_MILLIS / 2).alpha > 0.5f, "accelerates: slow at first")
    }

    @Test fun aSlowFrameDoesNotSkipTheAnimation() {
        assertEquals(16L, splashFrameStep(16))
        assertEquals(SPLASH_MAX_STEP_MILLIS, splashFrameStep(400))
        assertEquals(0L, splashFrameStep(-1))
    }

    @Test fun theLogoPathsAreThere() {
        assertFalse(SplashLogo.partA.isEmpty)
        assertFalse(SplashLogo.partB.isEmpty)
    }
}
