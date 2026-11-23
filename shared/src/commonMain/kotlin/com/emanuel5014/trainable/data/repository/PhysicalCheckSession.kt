package com.emanuel5014.trainable.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock

/**
 * Whether the fingerprint/PIN was given for the physical checks, one for the whole section so the list, the
 * comparison and the security settings share it. It starts locked; it locks again when the app goes to the
 * background (not while a photo is being taken), after [TIMEOUT_MS] without use, and when the last screen of the
 * section closes, so every new visit asks again.
 */
class PhysicalCheckSession(private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }) {

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active

    private var lastActivity = now()
    private var openScreens = 0

    // Set from the main thread, read when the app stops
    private var photoCaptureInProgress = false

    fun unlocked() {
        _active.value = true
        lastActivity = now()
    }

    fun touch() {
        lastActivity = now()
    }

    fun setPhotoCapture(inProgress: Boolean) {
        photoCaptureInProgress = inProgress
    }

    /** Locks, except while the camera or the gallery is open on top of the app. */
    fun lock() {
        if (photoCaptureInProgress) return
        _active.value = false
    }

    /** Locks when unused for [TIMEOUT_MS]; called now and then while a screen is open. */
    fun lockIfIdle() {
        if (_active.value && now() - lastActivity >= TIMEOUT_MS) lock()
    }

    /** A screen of the section opened. */
    fun screenOpened() {
        openScreens++
    }

    /** A screen of the section closed; leaving the section (no screen left) always locks. */
    fun screenClosed() {
        openScreens = (openScreens - 1).coerceAtLeast(0)
        if (openScreens == 0) {
            photoCaptureInProgress = false
            _active.value = false
        }
    }

    companion object {
        const val TIMEOUT_MS = 5 * 60 * 1000L
    }
}
