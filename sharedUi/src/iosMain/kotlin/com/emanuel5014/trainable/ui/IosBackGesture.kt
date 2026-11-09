@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcher.Companion.PRIORITY_OVERLAY
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.emanuel5014.trainable.ui.components.imeVisible
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.endEditing

/**
 * The iOS edge swipe works like Android's back: it first hides the keyboard, then closes the dialog, sheet or
 * menu on top, and only then goes back a screen.
 *
 * Compose's own dialog/popup handler only reacts to a plain back (no progress) and swallows the edge swipe, so
 * on iOS a dialog stayed open and the screen under it did not move either. This bridge takes the swipe only while
 * the keyboard is open or such a handler is on top, and hands it on to that handler as a plain back.
 * Otherwise it stays off, so NavHost keeps its interactive back animation.
 */
internal class IosBackGestureBridge(
    private val dispatcher: NavigationEventDispatcher,
    private val post: (() -> Unit) -> Unit,
    private val hideKeyboard: () -> Unit,
    /**
     * Compose's Dialog and Popup register an OnBackClickEventHandler with this info. ModalBottomSheet adds its own
     * progress-aware handler above it, so sheets already follow the swipe and the bridge stays off for them.
     */
    private val isPlainBackOnly: (NavigationEventInfo?) -> Boolean =
        { it != null && it::class.simpleName == "BackClickHandlerInfo" }
) {
    private object BridgeInfo : NavigationEventInfo()

    private val plainBack = DirectNavigationEventInput()
    private val handler = object : NavigationEventHandler<NavigationEventInfo>(BridgeInfo, isBackEnabled = false) {
        override fun onBackCompleted() = onSwipe()
    }

    var keyboardOpen = false
        set(value) { field = value; refresh() }

    /** A plain-back handler (dialog, sheet, menu) is the one the swipe would reach. */
    var dialogOnTop by mutableStateOf(false)
        private set

    val active: Boolean get() = handler.isBackEnabled

    fun attach() {
        dispatcher.addInput(plainBack)
        dispatcher.addHandler(handler, PRIORITY_OVERLAY)
        refresh()
    }

    fun detach() {
        handler.remove()
        dispatcher.removeInput(plainBack)
    }

    /** Looks at what the swipe would reach without the bridge; never changes anything during a gesture. */
    fun refresh() {
        if (dispatcher.transitionState.value != NavigationEventTransitionState.Idle) return
        handler.isBackEnabled = false
        val history = dispatcher.history.value
        dialogOnTop = isPlainBackOnly(history.mergedHistory.getOrNull(history.currentIndex))
        handler.isBackEnabled = keyboardOpen || dialogOnTop
    }

    private fun onSwipe() {
        if (keyboardOpen) {
            hideKeyboard()
            return
        }
        if (!dialogOnTop) return
        // Outside the current dispatch: the dialog gets a plain back, as a tap on the scrim would do
        post {
            handler.isBackEnabled = false
            plainBack.backCompleted()
            refresh()
        }
    }
}

/** Installs [IosBackGestureBridge] for the whole app (screens, dialogs, sheets, onboarding). */
@Composable
internal fun IosBackGesture(content: @Composable () -> Unit) {
    content()
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
    val focusManager by rememberUpdatedState(LocalFocusManager.current)
    val keyboard by rememberUpdatedState(LocalSoftwareKeyboardController.current)
    val scope = rememberCoroutineScope()
    val bridge = remember(dispatcher) {
        IosBackGestureBridge(
            dispatcher,
            post = { action -> scope.launch { action() } },
            hideKeyboard = {
                focusManager.clearFocus(force = true)
                keyboard?.hide()
                // A field inside a dialog or sheet lives in another Compose layer, out of reach of the two calls
                // above: end editing in every window, as UIKit does for its own text fields
                UIApplication.sharedApplication.connectedScenes
                    .flatMap { scene -> (scene as? UIWindowScene)?.windows.orEmpty() }
                    .filterIsInstance<UIWindow>()
                    .forEach { it.endEditing(true) }
            }
        )
    }
    DisposableEffect(bridge) {
        bridge.attach()
        onDispose { bridge.detach() }
    }
    val keyboardOpen = imeVisible()
    LaunchedEffect(bridge, keyboardOpen) { bridge.keyboardOpen = keyboardOpen }
    // A dialog or sheet opening changes what the swipe would reach
    LaunchedEffect(bridge) { dispatcher.history.collect { bridge.refresh() } }
    // While on, nothing tells the bridge that a dialog closed by a tap: look again a few times a second
    LaunchedEffect(bridge, bridge.dialogOnTop) {
        while (bridge.dialogOnTop) {
            delay(200)
            bridge.refresh()
        }
    }
}
