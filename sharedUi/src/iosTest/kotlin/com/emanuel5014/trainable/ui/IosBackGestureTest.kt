package com.emanuel5014.trainable.ui

import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import kotlin.test.*

class IosBackGestureTest {
    private object ScreenInfo : NavigationEventInfo()
    /** Same name as Compose's dialog/popup info, which the bridge recognises. */
    private class BackClickHandlerInfo : NavigationEventInfo()

    /** Like NavHost: follows the finger, so it must keep receiving the progress events. */
    private class ScreenHandler : NavigationEventHandler<NavigationEventInfo>(ScreenInfo, isBackEnabled = true) {
        var progressed = 0; var completed = 0
        override fun onBackProgressed(event: NavigationEvent) { progressed++ }
        override fun onBackCompleted() { completed++ }
    }

    /** Like Compose's OnBackClickEventHandler: a gesture with progress is ignored, a plain back dismisses. */
    private class DialogHandler(private val dismissable: Boolean = true) :
        NavigationEventHandler<NavigationEventInfo>(BackClickHandlerInfo(), isBackEnabled = true) {
        private var sawProgress = false
        var dismissed = 0
        override fun onBackProgressed(event: NavigationEvent) { sawProgress = true }
        override fun onBackCompleted() { if (!sawProgress && dismissable) dismissed++; sawProgress = false }
        override fun onBackCancelled() { sawProgress = false }
    }

    private val dispatcher = NavigationEventDispatcher()
    private val edge = DirectNavigationEventInput().also { dispatcher.addInput(it) }
    private var keyboardHidden = 0
    private val bridge = IosBackGestureBridge(dispatcher, post = { it() }, hideKeyboard = { keyboardHidden++ })
        .also { it.attach() }

    private fun swipeBack() {
        edge.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT))
        edge.backProgressed(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT, progress = 0.6f, touchX = 200f))
        edge.backCompleted()
    }

    @AfterTest fun tearDown() { bridge.detach(); dispatcher.dispose() }

    @Test fun withNothingOpenTheScreenKeepsTheInteractiveGesture() {
        val screen = ScreenHandler().also { dispatcher.addHandler(it) }
        bridge.refresh()
        assertFalse(bridge.active)
        swipeBack()
        assertEquals(1, screen.progressed); assertEquals(1, screen.completed)
    }

    @Test fun aSwipeClosesTheDialogAndNotTheScreenUnderIt() {
        val screen = ScreenHandler().also { dispatcher.addHandler(it) }
        val dialog = DialogHandler().also { dispatcher.addHandler(it) }
        bridge.refresh()
        assertTrue(bridge.active)
        swipeBack()
        assertEquals(1, dialog.dismissed)
        assertEquals(0, screen.completed); assertEquals(0, screen.progressed)
    }

    @Test fun aDialogThatCannotBeDismissedStaysOpen() {
        val screen = ScreenHandler().also { dispatcher.addHandler(it) }
        val dialog = DialogHandler(dismissable = false).also { dispatcher.addHandler(it) }
        bridge.refresh()
        swipeBack()
        assertEquals(0, dialog.dismissed); assertEquals(0, screen.completed)
    }

    @Test fun onceTheDialogIsGoneTheScreenGetsTheGestureAgain() {
        val screen = ScreenHandler().also { dispatcher.addHandler(it) }
        val dialog = DialogHandler().also { dispatcher.addHandler(it) }
        bridge.refresh()
        dialog.remove() // closed by a tap on ANNULLA, while the bridge was on
        bridge.refresh()
        assertFalse(bridge.active)
        swipeBack()
        assertEquals(1, screen.progressed); assertEquals(1, screen.completed)
    }

    @Test fun withTheKeyboardOpenTheSwipeOnlyHidesIt() {
        val screen = ScreenHandler().also { dispatcher.addHandler(it) }
        val dialog = DialogHandler().also { dispatcher.addHandler(it) }
        bridge.keyboardOpen = true
        swipeBack()
        assertEquals(1, keyboardHidden)
        assertEquals(0, dialog.dismissed); assertEquals(0, screen.completed)
        // Keyboard gone: the next swipe closes the dialog, as on Android
        bridge.keyboardOpen = false
        swipeBack()
        assertEquals(1, dialog.dismissed); assertEquals(1, keyboardHidden)
    }

    @Test fun theKeyboardIsHiddenEvenWithNoOtherHandler() {
        bridge.keyboardOpen = true
        assertTrue(bridge.active)
        swipeBack()
        assertEquals(1, keyboardHidden)
        bridge.keyboardOpen = false
        assertFalse(bridge.active)
    }

    @Test fun theStateIsNotChangedDuringAGesture() {
        val screen = ScreenHandler().also { dispatcher.addHandler(it) }
        bridge.refresh()
        edge.backStarted(NavigationEvent(swipeEdge = NavigationEvent.EDGE_LEFT))
        val dialog = DialogHandler().also { dispatcher.addHandler(it) }
        bridge.refresh() // ignored: the screen's gesture is in progress
        assertFalse(bridge.active)
        edge.backCompleted()
        assertEquals(1, screen.completed); assertEquals(0, dialog.dismissed)
    }
}
