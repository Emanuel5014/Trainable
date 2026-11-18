package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs
import kotlinx.coroutines.flow.first

/**
 * The wobble of the floating bar's glass pill: moving makes it wider and flatter, and when it stops
 * a spring overshoots it into a brief squash the other way before it settles.
 *
 * [amount] is 0 at rest, positive while stretched and negative while squashed. Read it in draw or
 * layer lambdas, not in composition: it changes every frame while the pill wobbles.
 */
@Stable
class NavPillJiggle {
    var amount by mutableFloatStateOf(0f)
        private set

    private var speedOfAmount = 0f
    private var target = 0f
    private var lastPosition: Float? = null

    val scaleX: Float get() = 1f + WIDEN * amount
    val scaleY: Float get() = 1f - FLATTEN * amount

    val isAtRest: Boolean get() = target == 0f && amount == 0f && speedOfAmount == 0f

    /** Advances by [seconds] with the pill now at [position], in items as in [NavPillMath]. */
    fun step(position: Float, seconds: Float) {
        if (seconds <= 0f) return
        val moved = lastPosition?.let { abs(position - it) } ?: 0f
        lastPosition = position
        target = (moved / seconds / FULL_SPEED).coerceAtMost(1f)

        // A long frame must not make the integration unstable
        val dt = seconds.coerceAtMost(MAX_STEP_SECONDS)
        speedOfAmount += (STIFFNESS * (target - amount) - DAMPING * speedOfAmount) * dt
        val next = (amount + speedOfAmount * dt).coerceIn(MIN_AMOUNT, MAX_AMOUNT)

        val settled = target == 0f && abs(next) < REST_AMOUNT && abs(speedOfAmount) < REST_SPEED
        if (settled) speedOfAmount = 0f
        amount = if (settled) 0f else next
    }

    /** Drives the wobble from [position]; asks for frames only while the pill moves or wobbles. */
    suspend fun follow(position: () -> Float) {
        lastPosition = position()
        while (true) {
            val from = lastPosition
            snapshotFlow(position).first { it != from }
            var lastNanos = withFrameNanos { it }
            do {
                withFrameNanos { now ->
                    step(position(), (now - lastNanos) / NANOS_PER_SECOND)
                    lastNanos = now
                }
            } while (!isAtRest)
        }
    }

    fun reset() {
        amount = 0f
        speedOfAmount = 0f
        target = 0f
        lastPosition = null
    }

    private companion object {
        const val FULL_SPEED = 7f // items per second that give the full deformation
        const val WIDEN = 0.22f
        const val FLATTEN = 0.14f
        const val MIN_AMOUNT = -0.6f
        const val MAX_AMOUNT = 1.25f
        const val STIFFNESS = 320f
        const val DAMPING = 12.5f // about 0.35 of critical: two visible wobbles
        const val MAX_STEP_SECONDS = 1f / 30f
        const val REST_AMOUNT = 0.002f
        const val REST_SPEED = 0.02f
        const val NANOS_PER_SECOND = 1_000_000_000f
    }
}

/** With [enabled] false the pill stays at [NavPillJiggle.amount] 0 and no frame is ever requested. */
@Composable
fun rememberNavPillJiggle(pillState: NavPillState, enabled: Boolean): NavPillJiggle {
    val jiggle = remember { NavPillJiggle() }
    LaunchedEffect(jiggle, pillState, enabled) {
        jiggle.reset()
        if (enabled) jiggle.follow { pillState.position }
    }
    return jiggle
}
