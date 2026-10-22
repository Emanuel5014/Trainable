package com.emanuel5014.trainable.ui.components.motion

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MotionScheme

/**
 * The springs of the workout screen, from the Material 3 expressive motion scheme.
 *
 * The scheme is read here and not from the theme: the app's [androidx.compose.material3.MaterialTheme] does not set a
 * `motionScheme`, and setting one would change the springs of every Material component of the app.
 *
 * Spatial specs move and resize things (position, size, shape); effects specs fade and recolour them.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object WorkoutMotion {
    private val scheme: MotionScheme = MotionScheme.expressive()

    fun <T> spatial(): FiniteAnimationSpec<T> = scheme.defaultSpatialSpec()

    fun <T> fastSpatial(): FiniteAnimationSpec<T> = scheme.fastSpatialSpec()

    fun <T> effects(): FiniteAnimationSpec<T> = scheme.defaultEffectsSpec()

    fun <T> fastEffects(): FiniteAnimationSpec<T> = scheme.fastEffectsSpec()
}
