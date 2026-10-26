package com.emanuel5014.trainable.ui.theme

import android.animation.ValueAnimator
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface as AndroidSurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.abs

/** Haze observes Alignment reads during drawing, so sensor updates never recompose the screen. */
@Stable
class GlassLight : Alignment {
    var bias by mutableStateOf(Offset(-1f, -1f))
        internal set

    override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset =
        BiasAbsoluteAlignment(bias.x, bias.y).align(size, space, layoutDirection)
}

val LocalGlassLight = staticCompositionLocalOf { GlassLight() }
val LocalGlassMotionPreference = staticCompositionLocalOf { true }

private fun reflectionSensor(manager: SensorManager): Sensor? =
    manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        ?: manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

fun hasGlassMotionSensor(context: Context): Boolean =
    (context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.let { reflectionSensor(it) != null } == true

@Composable
fun rememberGlassLight(enabled: Boolean): GlassLight {
    val context = LocalContext.current
    val view = LocalView.current
    val owner = LocalLifecycleOwner.current
    val light = remember { GlassLight() }
    val manager = remember(context) { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val sensor = remember(manager) { manager?.let(::reflectionSensor) }
    DisposableEffect(enabled, owner, manager, sensor, view) {
        if (!enabled || manager == null || sensor == null || !ValueAnimator.areAnimatorsEnabled()) {
            light.bias = Offset(-1f, -1f)
            onDispose { }
        } else {
            val rotation = FloatArray(9)
            var origin: Offset? = null
            var filtered = Offset.Zero
            var lastSample = 0L
            var registered = false
            val listener = object : SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                override fun onSensorChanged(event: SensorEvent) {
                    if (!registered) return
                    if (event.timestamp - lastSample < 40_000_000L) return
                    lastSample = event.timestamp
                    val gravity = if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                        SensorManager.getRotationMatrixFromVector(rotation, event.values)
                        Offset(rotation[6] * SensorManager.GRAVITY_EARTH, rotation[7] * SensorManager.GRAVITY_EARTH)
                    } else Offset(event.values[0], event.values[1])
                    val screenGravity = when (view.display?.rotation) {
                        AndroidSurface.ROTATION_90 -> Offset(-gravity.y, gravity.x)
                        AndroidSurface.ROTATION_180 -> -gravity
                        AndroidSurface.ROTATION_270 -> Offset(gravity.y, -gravity.x)
                        else -> gravity
                    }
                    if (!screenGravity.x.isFinite() || !screenGravity.y.isFinite()) return
                    val baseline = origin ?: screenGravity.also { origin = it }
                    val delta = (screenGravity - baseline) / 5f
                    filtered += (delta - filtered) * .18f
                    val next = Offset((-.60f + filtered.x * 1.6f).coerceIn(-1f, 1f), (-.75f + filtered.y * 1.6f).coerceIn(-1f, 1f))
                    if (abs(next.x - light.bias.x) > .012f || abs(next.y - light.bias.y) > .012f) light.bias = next
                }
            }
            fun stop() {
                manager.unregisterListener(listener)
                registered = false
                origin = null
                filtered = Offset.Zero
                lastSample = 0L
                light.bias = Offset(-1f, -1f)
            }
            fun start() {
                if (!registered && ValueAnimator.areAnimatorsEnabled()) registered = manager.registerListener(listener, sensor, 40_000)
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> start()
                    Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> stop()
                    else -> Unit
                }
            }
            owner.lifecycle.addObserver(observer)
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
            onDispose { owner.lifecycle.removeObserver(observer); stop() }
        }
    }
    return light
}
