@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.emanuel5014.trainable.ui

import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectContainsPoint
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIGestureRecognizer
import platform.UIKit.UIGestureRecognizerDelegateProtocol
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.UITouch
import platform.UIKit.UIView
import platform.darwin.NSObject

/**
 * A tap on a spot that is not the text field being typed in hides the keyboard, as in iOS apps.
 *
 * It sits on the window, so it also covers dialogs and sheets (other Compose layers) and never takes a touch away
 * from what is under it: a button still works, a drag or a scroll is not a tap. Compose keeps the view that owns the
 * keyboard exactly over the focused field, so "outside the field" is outside that view. When the tap moved the focus
 * to another field, or opened something that focused one, the keyboard stays.
 */
internal class IosTapToHideKeyboard(
    private val scope: CoroutineScope,
    private val keyboardOpen: () -> Boolean,
    private val hideKeyboard: () -> Unit
) : NSObject(), UIGestureRecognizerDelegateProtocol {

    private val recognizer = UITapGestureRecognizer(target = this, action = NSSelectorFromString("onTap:")).also {
        it.cancelsTouchesInView = false
        it.delaysTouchesBegan = false
        it.delaysTouchesEnded = false
        it.delegate = this
    }

    /** The view that owned the keyboard when the finger went down; null when there was no keyboard. */
    private var editingView: UIView? = null

    fun attach(window: UIView) {
        window.addGestureRecognizer(recognizer)
    }

    fun detach() {
        recognizer.view?.removeGestureRecognizer(recognizer)
    }

    @ObjCSignatureOverride
    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWithGestureRecognizer: UIGestureRecognizer
    ): Boolean = true

    @ObjCSignatureOverride
    override fun gestureRecognizer(gestureRecognizer: UIGestureRecognizer, shouldReceiveTouch: UITouch): Boolean {
        editingView = if (keyboardOpen()) gestureRecognizer.view?.let(::firstResponderIn) else null
        return true
    }

    @ObjCAction
    fun onTap(sender: UITapGestureRecognizer) {
        val editing = editingView ?: return
        val window = sender.view ?: return
        val point = sender.locationInView(window)
        scope.launch {
            // Let Compose handle the same tap first: it may have moved the focus to another field
            delay(120)
            if (firstResponderIn(window) !== editing || !keyboardOpen()) return@launch
            val field = editing.convertRect(editing.bounds, toView = window)
            if (!CGRectContainsPoint(field, point)) hideKeyboard()
        }
    }

    private fun firstResponderIn(view: UIView): UIView? {
        if (view.isFirstResponder()) return view
        for (child in view.subviews) {
            firstResponderIn(child as UIView)?.let { return it }
        }
        return null
    }
}

