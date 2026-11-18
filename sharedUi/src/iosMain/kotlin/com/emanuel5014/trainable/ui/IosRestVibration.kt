package com.emanuel5014.trainable.ui

/**
 * Long vibration at the end of a rest while the phone is locked (Android's "Lock Screen vibration").
 * Swift keeps the app running during the workout (HealthKit session, iOS 26+) and vibrates at [scheduleRestEnd].
 */
interface IosRestVibration {
    fun isSupported(): Boolean
    /** Arms the vibration for the rest ending at [endsAtMillis]; replaces the one armed before. */
    fun scheduleRestEnd(endsAtMillis: Long, seconds: Int)
    fun cancelRestEnd()
    /** Stops a vibration in progress (the app was opened, the set was logged). */
    fun stopVibration()
    /** The workout is over: no more rests to wait for. */
    fun endKeepAlive()
}

object IosRestVibrationBridge {
    var service: IosRestVibration? = null
    /** The chosen seconds (0 = the normal notification), kept current by [IosNotificationEffects]. */
    internal var seconds: Int = 0
}
