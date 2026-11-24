package com.emanuel5014.trainable.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import platform.Foundation.NSNumber
import platform.Foundation.NSSelectorFromString
import platform.Foundation.valueForKey
import platform.UIKit.UIScreen

/**
 * iOS keeps the screen's corner radius on UIScreen (in points) without a public property: it is read only when the
 * screen answers to it, so a future iOS without it gives null instead of an exception (an Objective-C exception would
 * end the app, Kotlin cannot catch it). The app is sideloaded, not reviewed by the App Store.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@Composable
actual fun rememberDisplayCornerRadius(): Dp? = remember {
    val screen = UIScreen.mainScreen
    val key = "_displayCornerRadius"
    if (!screen.respondsToSelector(NSSelectorFromString(key))) return@remember null
    val radius = (screen.valueForKey(key) as? NSNumber)?.doubleValue ?: return@remember null
    if (radius <= 0.0) 0.dp else radius.dp
}
