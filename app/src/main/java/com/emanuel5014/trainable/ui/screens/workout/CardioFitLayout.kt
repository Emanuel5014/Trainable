package com.emanuel5014.trainable.ui.screens.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * How much room the cardio screen gives each of its parts. The screen takes the roomiest density that still leaves the
 * circle its minimum size, so a short screen (or a big font) gets a denser look instead of parts on top of each other.
 */
internal enum class CardioDensity(
    /** Space between the blocks of the screen. */
    val gap: Dp,
    /** The circle never gets smaller than this: a denser look is tried first. */
    val minCircle: Dp,
    val timerSize: TextUnit,
    val actionHeight: Int,
    /** The exercise name goes up in the bar with the badge and the buttons, which saves a whole row. */
    val nameInBar: Boolean,
    /** The "ready / running / paused" caption under the time (the button in the circle says it too). */
    val showStatus: Boolean
) {
    Regular(gap = 12.dp, minCircle = 150.dp, timerSize = 46.sp, actionHeight = 48, nameInBar = false, showStatus = true),
    Compact(gap = 8.dp, minCircle = 120.dp, timerSize = 38.sp, actionHeight = 44, nameInBar = false, showStatus = true),
    Tight(gap = 4.dp, minCircle = 80.dp, timerSize = 32.sp, actionHeight = 40, nameInBar = true, showStatus = false)
}

/** What is shown to rate the set that was just done: the whole selector, a small prompt that opens the set, or nothing. */
internal enum class CardioRpe { Row, Prompt, None }

internal data class CardioFit(val density: CardioDensity, val rpe: CardioRpe)

/** From the roomiest to the tightest. The last one is the fallback: if even it does not fit, the screen scrolls. */
internal fun cardioFits(rpeAsked: Boolean): List<CardioFit> = if (rpeAsked) {
    listOf(
        CardioFit(CardioDensity.Regular, CardioRpe.Row),
        CardioFit(CardioDensity.Regular, CardioRpe.Prompt),
        CardioFit(CardioDensity.Compact, CardioRpe.Prompt),
        CardioFit(CardioDensity.Tight, CardioRpe.Prompt),
        CardioFit(CardioDensity.Tight, CardioRpe.None)
    )
} else {
    listOf(
        CardioFit(CardioDensity.Regular, CardioRpe.None),
        CardioFit(CardioDensity.Compact, CardioRpe.None),
        CardioFit(CardioDensity.Tight, CardioRpe.None)
    )
}

private data class CardioSlot(val fit: Int, val part: Int)

/**
 * Lays out the cardio screen: [header] at the top, then [top], the [circle] and [bottom] as one group centered in
 * what is left. The circle is not given a size in advance: every other part is measured first and the circle takes the
 * room that remains, between the density's minimum and [maxCircle]. When even the densest fit leaves the circle less
 * than its minimum, the layout is simply taller than [viewportHeight], so the caller can scroll it.
 *
 * Each fit is measured for real (nothing is estimated in dp), so wrapped names, big fonts and notes are accounted for.
 */
@Composable
internal fun CardioFitLayout(
    fits: List<CardioFit>,
    viewportHeight: Dp,
    maxCircle: Dp,
    header: @Composable (CardioFit) -> Unit,
    top: @Composable (CardioFit) -> Unit,
    circle: @Composable (CardioFit, Dp) -> Unit,
    bottom: @Composable (CardioFit) -> Unit,
    modifier: Modifier = Modifier
) {
    SubcomposeLayout(modifier) { constraints ->
        val width = constraints.maxWidth
        val viewport = viewportHeight.roundToPx()
        val full = Constraints(minWidth = width, maxWidth = width)

        var chosen = fits.lastIndex
        var headerH = 0
        var topH = 0
        var bottomH = 0
        var fixed = 0
        val parts = arrayOfNulls<Placeable>(3)
        for (index in fits.indices) {
            val fit = fits[index]
            val h = subcompose(CardioSlot(index, 0)) { header(fit) }.single().measure(full)
            val t = subcompose(CardioSlot(index, 1)) { top(fit) }.single().measure(full)
            val b = subcompose(CardioSlot(index, 2)) { bottom(fit) }.single().measure(full)
            chosen = index
            parts[0] = h; parts[1] = t; parts[2] = b
            headerH = h.height; topH = t.height; bottomH = b.height
            // The header, the top, the circle and the bottom are apart by three gaps
            fixed = headerH + topH + bottomH + fit.density.gap.roundToPx() * 3
            if (fixed + fit.density.minCircle.roundToPx() <= viewport) break
        }

        val fit = fits[chosen]
        val gap = fit.density.gap.roundToPx()
        val minCircle = fit.density.minCircle.roundToPx()
        val circleSize = (viewport - fixed)
            .coerceAtMost(maxCircle.roundToPx())
            .coerceAtMost(width)
            .coerceAtLeast(minCircle)
        val circlePlaceable = subcompose("circle") { circle(fit, circleSize.toDp()) }
            .single()
            .measure(Constraints.fixed(circleSize, circleSize))

        val contentHeight = fixed + circleSize
        val height = maxOf(viewport, contentHeight)
        // What is not needed is shared above and below the group, which sits under the header
        val groupTop = headerH + gap + (height - contentHeight) / 2

        layout(width, height) {
            parts[0]!!.placeRelative(0, 0)
            parts[1]!!.placeRelative(0, groupTop)
            circlePlaceable.placeRelative((width - circleSize) / 2, groupTop + topH + gap)
            parts[2]!!.placeRelative(0, groupTop + topH + gap + circleSize + gap)
        }
    }
}
