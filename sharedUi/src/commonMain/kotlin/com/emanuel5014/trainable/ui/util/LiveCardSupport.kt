package com.emanuel5014.trainable.ui.util

/** The platform draws the workout live card (Android notification, iOS Live Activity). */
internal expect val workoutLiveCardSupported: Boolean

/**
 * The end of a timer needs a plain alert beside the card for watches and bands to get it (Android). On iOS the end of
 * a timer is always a local notification of its own, so there is nothing to switch.
 */
internal expect val workoutLiveCardSeparateAlertSupported: Boolean
