package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.model.NavBarStyle

/** Style of the bottom navigation in use, so screens know whether the bar carries their main action. */
val LocalNavBarStyle = compositionLocalOf { NavBarStyle.Floating }

/** The expressive navbar's round action button, and how far the bar floats above the bottom edge. */
val ExpressiveNavFabSize = 80.dp
val ExpressiveNavBottomLift = 16.dp

/** Height the bar takes above the navigation area, including its lift. */
val ExpressiveNavBarHeight = ExpressiveNavFabSize + ExpressiveNavBottomLift

/** Free space the main tabs keep below their content so the last item can scroll clear of the navbar. */
@Composable
fun navBarBottomClearance(): Dp =
    if (LocalNavBarStyle.current == NavBarStyle.Expressive) {
        ExpressiveNavBarHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
    } else {
        100.dp + floatingNavSystemInset()
    }

/**
 * Height of the system bar the floating navbar must stay above: the 3-button bar on Android. It is 0 with gesture
 * navigation (the pill sits over the gesture handle on purpose) and on iOS, so those layouts don't move.
 */
@Composable
fun floatingNavSystemInset(): Dp = WindowInsets.tappableElement.asPaddingValues().calculateBottomPadding()

/** Gap kept between the floating navbar and the buttons above it when the bar is lifted over 3-button navigation. */
private val FloatingNavFabGap = 24.dp

/** Extra lift for the screens' floating buttons, so they stay clear of a floating navbar lifted over 3-button navigation. */
@Composable
fun floatingNavFabLift(): Dp =
    if (LocalNavBarStyle.current == NavBarStyle.Floating && floatingNavSystemInset() > 0.dp) FloatingNavFabGap else 0.dp

/** Index of each tab of the main pager. */
object NavBarPage {
    const val Dashboard = 0
    const val Routines = 1
    const val History = 2
    const val Analytics = 3
}

/** Main action of a tab, shown next to the expressive navbar instead of as a floating button. */
@Immutable
sealed interface NavBarAction {
    val icon: ImageVector
    val label: String

    data class Click(
        override val icon: ImageVector,
        override val label: String,
        val onClick: () -> Unit
    ) : NavBarAction

    /** An action that opens a few choices above the button. */
    data class Menu(
        override val icon: ImageVector,
        override val label: String,
        val items: List<NavBarMenuItem>
    ) : NavBarAction
}

@Immutable
data class NavBarMenuItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

/**
 * Where the tabs of the main pager hand their action to the navbar. The pager keeps every tab
 * composed, so actions are keyed by tab and the navbar picks the one of the tab on screen.
 */
object NavBarActionManager {
    private class Entry(val owner: Any, val action: NavBarAction)

    private val entries = mutableStateMapOf<Int, Entry>()

    fun actionFor(page: Int): NavBarAction? = entries[page]?.action

    internal fun update(page: Int, owner: Any, action: NavBarAction?) {
        val current = entries[page]
        if (action == null) {
            if (current?.owner === owner) entries.remove(page)
        } else if (current == null || current.owner !== owner || current.action != action) {
            entries[page] = Entry(owner, action)
        }
    }
}

/**
 * Publishes [action] as the main action of [page] while this is composed and the expressive navbar
 * is in use; pass null when the tab has nothing to offer right now.
 */
@Composable
fun NavBarActionEffect(page: Int, action: NavBarAction?) {
    val enabled = LocalNavBarStyle.current == NavBarStyle.Expressive
    // Tells apart two compositions of the same tab (e.g. while navigating back to it)
    val owner = remember { Any() }
    val published = if (enabled) action else null

    SideEffect { NavBarActionManager.update(page, owner, published) }
    DisposableEffect(page) {
        onDispose { NavBarActionManager.update(page, owner, null) }
    }
}
