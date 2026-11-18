package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The column of an exercise picker sheet: a header (titles, search, filters), the list and a footer button.
 *
 * Laid out like a Column with the list weighted, so at normal sizes nothing moves. When header and footer leave the
 * list less than [minListHeight] times the font scale (small screen, large font) they make way for it: scrolling the list up, or dragging
 * the header or the footer, pushes the header out at the top and then the footer out at the bottom; they come back
 * in the opposite order once the list is back at its top.
 */
@Composable
fun ExercisePickerSheetLayout(
    header: @Composable () -> Unit,
    list: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    minListHeight: Dp = 160.dp
) {
    val collapse = remember { PickerCollapse() }
    val connection = remember(collapse) { collapse.connection() }
    // Header and footer take drags only while they can make way; otherwise a drag on them moves the sheet as before
    val dragOnly = rememberScrollableState { 0f }

    Layout(
        contents = listOf(
            {
                Box(
                    Modifier
                        .drawWithContent {
                            val hidden = collapse.headerHidden().toFloat()
                            if (hidden > 0f) {
                                // The part pushed above the column is cut; sideways nothing changes
                                clipRect(left = -size.width, top = hidden, right = size.width * 2, bottom = size.height) {
                                    this@drawWithContent.drawContent()
                                }
                            } else {
                                drawContent()
                            }
                        }
                        .scrollable(dragOnly, Orientation.Vertical, enabled = collapse.canCollapse)
                ) { header() }
            },
            list,
            {
                Box(
                    Modifier
                        .drawWithContent {
                            val hidden = collapse.footerHidden().toFloat()
                            if (hidden > 0f) {
                                // The part pushed below the column is cut
                                clipRect(left = -size.width, top = 0f, right = size.width * 2, bottom = size.height - hidden) {
                                    this@drawWithContent.drawContent()
                                }
                            } else {
                                drawContent()
                            }
                        }
                        .scrollable(dragOnly, Orientation.Vertical, enabled = collapse.canCollapse)
                ) { footer() }
            }
        ),
        modifier = modifier.nestedScroll(connection)
    ) { (headerMeasurables, listMeasurables, footerMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val headerPlaceable = headerMeasurables.single().measure(loose.copy(maxHeight = Constraints.Infinity))
        val footerPlaceable = footerMeasurables.single().measure(loose)
        val headerHeight = headerPlaceable.height
        val footerHeight = footerPlaceable.height

        val bounded = constraints.hasBoundedHeight
        val available = if (bounded) constraints.maxHeight - headerHeight - footerHeight else Constraints.Infinity
        // Rows grow with the font, so the room asked for does too (about two rows at any text size)
        val canCollapse = bounded && available < minListHeight.toPx() * fontScale
        collapse.headerHeight = if (canCollapse) headerHeight else 0
        collapse.footerHeight = if (canCollapse) footerHeight else 0
        if (collapse.canCollapse != canCollapse) collapse.canCollapse = canCollapse
        val headerHidden = collapse.headerHidden()
        val footerHidden = collapse.footerHidden()

        val listMax = if (bounded) max(0, available + headerHidden + footerHidden) else Constraints.Infinity
        val listPlaceables = listMeasurables.map { it.measure(loose.copy(maxHeight = listMax)) }
        val listHeight = listPlaceables.maxOfOrNull { it.height } ?: 0

        val shownHeader = headerHeight - headerHidden
        val width = (listPlaceables + headerPlaceable + footerPlaceable).maxOf { it.width }
        val height = (shownHeader + listHeight + footerHeight - footerHidden).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(max(width, constraints.minWidth), height) {
            headerPlaceable.place(0, -headerHidden)
            listPlaceables.forEach { it.place(0, shownHeader) }
            footerPlaceable.place(0, shownHeader + listHeight)
        }
    }
}

/**
 * How far header and footer of the picker are pushed out, as one distance: the header's height first, then the
 * footer's. The heights are set by the layout and are 0 while the list has room, so nothing can move then.
 */
private class PickerCollapse {
    var offset by mutableFloatStateOf(0f)
    var canCollapse by mutableStateOf(false)
    var headerHeight = 0
    var footerHeight = 0

    private val maxOffset get() = (headerHeight + footerHeight).toFloat()
    private fun current() = min(offset, maxOffset)
    fun headerHidden(): Int = min(current().roundToInt(), headerHeight)
    fun footerHidden(): Int = (current().roundToInt() - headerHeight).coerceIn(0, footerHeight)

    fun connection() = object : NestedScrollConnection {
        // Moving up: header and footer go first, then the list scrolls
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y >= 0f) return Offset.Zero
            return Offset(0f, consume(available.y))
        }

        // Moving down: the list comes back to its top first, then footer and header
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (available.y <= 0f) return Offset.Zero
            return Offset(0f, consume(available.y))
        }
    }

    /** Applies a vertical delta and returns what it took. */
    private fun consume(delta: Float): Float {
        val before = current()
        val next = (before - delta).coerceIn(0f, maxOffset)
        offset = next
        return before - next
    }
}
