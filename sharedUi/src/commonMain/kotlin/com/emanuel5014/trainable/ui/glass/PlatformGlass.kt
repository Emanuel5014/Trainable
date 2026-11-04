package com.emanuel5014.trainable.ui.glass

import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass as the shared screens ask for it. The Android app draws it (LiquidGlass.kt, provided at its root);
 * without a provider, as on iOS for now, every effect leaves the element as it is: the Classic look.
 */
interface PlatformGlass {
    /** The glass of the round header buttons, or [modifier] unchanged. */
    @Composable
    fun header(modifier: Modifier): Modifier

    /** The recording the floating buttons of a screen bend; null while action glass is off. */
    @Composable
    fun rememberActionSource(): GlassSource?

    /** The glass of the navbar's action button, which sits outside the screens and bends a copy of all of them. */
    @Composable
    fun rememberNavBarActionSource(): GlassSource? = null
}

/** Content recorded for the floating buttons above it; only the platform drawing the glass knows what is inside. */
interface GlassSource {
    /** Records the content of [modifier]; put it on the list under the buttons, never on the buttons. */
    fun record(modifier: Modifier): Modifier

    /** Tinted glass on a floating button over the recorded content. [cornerRadius] null means a pill or a circle. */
    @Composable
    fun bend(modifier: Modifier, tint: Color, cornerRadius: Dp?): Modifier
}

private object ClassicGlass : PlatformGlass {
    @Composable
    override fun header(modifier: Modifier): Modifier = modifier

    @Composable
    override fun rememberActionSource(): GlassSource? = null
}

val LocalPlatformGlass = staticCompositionLocalOf<PlatformGlass> { ClassicGlass }

@Composable
fun Modifier.headerGlass(): Modifier = LocalPlatformGlass.current.header(this)

@Composable
fun rememberActionGlassSource(): GlassSource? = LocalPlatformGlass.current.rememberActionSource()

fun Modifier.actionGlassSource(source: GlassSource?): Modifier = source?.record(this) ?: this

@Composable
fun Modifier.actionGlass(source: GlassSource?, tint: Color, cornerRadius: Dp? = null): Modifier =
    source?.bend(this, tint, cornerRadius) ?: this

/** The button's own fill: clear when the glass draws the body instead. */
fun actionGlassContainer(source: GlassSource?, color: Color): Color =
    if (source == null) color else Color.Transparent

/** The button's own shadow: none when the glass casts its own. */
@Composable
fun actionGlassElevation(
    source: GlassSource?,
    default: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation()
): FloatingActionButtonElevation =
    if (source == null) default else FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
