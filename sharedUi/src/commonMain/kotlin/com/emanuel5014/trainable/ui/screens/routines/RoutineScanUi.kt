package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState

/**
 * Scanning a routine sheet with AI from the routine screen. The shared screen asks the
 * platform for it and [NoRoutineScan] leaves it out.
 */
interface RoutineScanUi {
    /** Whether the screen offers a scan (AI on and a model or service ready). */
    val available: Boolean

    /** While a scan runs the screen records itself for the overlay's blur. */
    val isScanning: Boolean

    /** A scanned routine waits to be reviewed: the screen shows [DraftBanner] at its top. */
    val showDraftBanner: Boolean

    @Composable
    fun DraftBanner(modifier: Modifier)

    /** Asks where the photo comes from (camera or gallery). */
    fun openSource()

    /** The scan's sheets, dialogs and overlay; [viewWeek] is the week the result goes into. */
    @Composable
    fun Overlays(hazeState: HazeState, isDark: Boolean, viewWeek: Int)
}

object NoRoutineScan : RoutineScanUi {
    override val available: Boolean get() = false
    override val isScanning: Boolean get() = false
    override val showDraftBanner: Boolean get() = false

    @Composable
    override fun DraftBanner(modifier: Modifier) {}

    override fun openSource() {}

    @Composable
    override fun Overlays(hazeState: HazeState, isDark: Boolean, viewWeek: Int) {}
}
