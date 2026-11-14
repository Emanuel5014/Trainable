package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Where the exercise panel of the scan review stops, as the distance of its top edge from the top of the sheet. */
object ScanPanelMath {
    /**
     * Alto (a [stripPx] strip of photo stays visible), Metà (half and half) and Basso (only the [peekPx] summary
     * and buttons), always in that order and distinct even on a very short sheet.
     */
    fun anchors(containerPx: Float, stripPx: Float, peekPx: Float): List<Float> {
        val alto = stripPx.coerceIn(0f, containerPx)
        val basso = (containerPx - peekPx).coerceAtLeast(alto + 2f)
        val meta = (containerPx * 0.5f).coerceIn(alto + 1f, basso - 1f)
        return listOf(alto, meta, basso)
    }

    /**
     * The panel's position after the sheet was measured again: at rest it follows its height to the new value; under
     * the finger or while settling it stays where it is (only kept inside the new range), else it would jump.
     */
    fun remeasured(offset: Float, resting: Boolean, step: Int, newAnchors: List<Float>): Float =
        if (resting || offset.isNaN()) newAnchors[step.coerceIn(0, newAnchors.lastIndex)]
        else offset.coerceIn(newAnchors.first(), newAnchors.last())

    /** A slow release goes to the nearest height, a fling one height further in its direction. */
    fun settle(offset: Float, velocity: Float, anchors: List<Float>, flingPx: Float): Float {
        if (abs(velocity) < flingPx) return anchors.minBy { abs(it - offset) }
        return if (velocity > 0f) {
            anchors.firstOrNull { it > offset + 1f } ?: anchors.last()
        } else {
            anchors.lastOrNull { it < offset - 1f } ?: anchors.first()
        }
    }
}

/** The panel's position; it starts at Metà and keeps its height (by step) when the sheet is resized. */
@Stable
class ScanPanelState internal constructor(private val scope: CoroutineScope, private val flingPx: Float) {
    var anchors by mutableStateOf(emptyList<Float>())
        private set
    var offset by mutableFloatStateOf(Float.NaN)
        private set
    private var step = 1
    private var job: Job? = null
    /** False from the first finger movement until the panel has settled on a height again. */
    private var resting = true

    internal fun onMeasured(newAnchors: List<Float>) {
        if (newAnchors == anchors) return
        anchors = newAnchors
        offset = ScanPanelMath.remeasured(offset, resting, step, newAnchors)
        // A settle in progress aims at the height as it is now
        if (!resting && job?.isActive == true) animateTo(newAnchors[step])
    }

    /** Moves the panel by a finger's [delta] and returns how much of it was used. */
    internal fun dragBy(delta: Float): Float {
        if (anchors.isEmpty()) return 0f
        job?.cancel()
        resting = false
        val before = offset
        offset = (offset + delta).coerceIn(anchors.first(), anchors.last())
        return offset - before
    }

    internal fun settle(velocity: Float) {
        if (anchors.isEmpty()) return
        animateTo(ScanPanelMath.settle(offset, velocity, anchors, flingPx))
    }

    /** From Basso up to Metà, so a row being opened is in view; higher stays where it is. */
    fun raise() {
        if (anchors.size == 3 && offset > anchors[1] + 0.5f) animateTo(anchors[1])
    }

    /** Up to Alto, for editing with the keyboard. */
    fun expand() {
        if (anchors.isNotEmpty()) animateTo(anchors.first())
    }

    private fun animateTo(target: Float) {
        step = anchors.indexOf(target).coerceAtLeast(0)
        job?.cancel()
        resting = false
        job = scope.launch {
            animate(offset, target, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) { value, _ ->
                offset = value
            }
            resting = true
        }
    }

}

@Composable
fun rememberScanPanelState(): ScanPanelState {
    val scope = rememberCoroutineScope()
    val flingPx = with(LocalDensity.current) { 600.dp.toPx() }
    return remember { ScanPanelState(scope, flingPx) }
}

/**
 * The scan review on a phone: the photo of the sheet fills the space above the exercise panel, which is dragged by
 * its handle and [header] (only those) between Alto, Metà and Basso. [list] scrolls inside it on its own, so a
 * scroll or a slider never moves the panel; [footer] stays at its bottom.
 */
@Composable
fun ScanPhotoPanel(
    state: ScanPanelState,
    /** The photo, given its area and how many of its bottom pixels the panel covers right now. */
    photo: @Composable (Modifier, coveredPx: () -> Float) -> Unit,
    header: @Composable () -> Unit,
    list: @Composable ColumnScope.() -> Unit,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // Handle + header + footer: what stays visible at Basso
    var headerPx by remember { mutableIntStateOf(0) }
    var footerPx by remember { mutableIntStateOf(0) }
    val chromePx = headerPx + footerPx
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerPx = constraints.maxHeight.toFloat()
        val stripPx = with(density) { PHOTO_STRIP.toPx() }
        val cornerPx = with(density) { PANEL_CORNER.toPx() }
        LaunchedEffect(containerPx, chromePx) {
            if (headerPx > 0 && footerPx > 0) state.onMeasured(ScanPanelMath.anchors(containerPx, stripPx, chromePx.toFloat()))
        }
        val draggable = rememberDraggableState { delta -> state.dragBy(delta) }

        // The photo fits the space above the panel, but never gets smaller than at Metà: at Alto it keeps a
        // readable size and the strip shows its top, the rest can be dragged into view
        fun panelTop() = state.offset.takeUnless { it.isNaN() } ?: (containerPx * 0.5f)
        fun photoHeight() = maxOf(panelTop(), state.anchors.getOrNull(1) ?: (containerPx * 0.5f)) + cornerPx
        photo(
            Modifier.fillMaxWidth().layout { measurable, constraints ->
                val height = photoHeight().roundToInt().coerceIn(0, constraints.maxHeight)
                val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        ) { (photoHeight() - panelTop() - cornerPx).coerceAtLeast(0f) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val top = (state.offset.takeUnless { it.isNaN() } ?: (containerPx * 0.5f)).roundToInt()
                    val height = (constraints.maxHeight - top).coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, top) }
                }
                .clip(RoundedCornerShape(topStart = PANEL_CORNER, topEnd = PANEL_CORNER))
                .background(Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { headerPx = it.height }
                    .draggable(
                        state = draggable,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity -> state.settle(velocity) }
                    )
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(OnSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
                header()
            }
            Column(modifier = Modifier.weight(1f).fillMaxWidth(), content = list)
            Box(modifier = Modifier.fillMaxWidth().onSizeChanged { footerPx = it.height }) { footer() }
        }
    }
}

/** Photo always left visible above the panel at Alto. */
private val PHOTO_STRIP = 120.dp
private val PANEL_CORNER = 28.dp
