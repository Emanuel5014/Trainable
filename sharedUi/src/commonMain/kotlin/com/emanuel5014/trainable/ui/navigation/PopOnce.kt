package com.emanuel5014.trainable.ui.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/**
 * Leaves the screen of [entry] only while it is still the one on top. A second "back" for the same screen (the
 * workout finishing while the close button is tapped, two taps in a row) then does nothing instead of also
 * popping the screen under it, which left the app on an empty page.
 */
fun NavController.popOnce(entry: NavBackStackEntry): Boolean =
    if (currentBackStackEntry?.id == entry.id) popBackStack() else false
