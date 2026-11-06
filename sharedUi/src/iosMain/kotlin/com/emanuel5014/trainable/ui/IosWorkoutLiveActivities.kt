package com.emanuel5014.trainable.ui

/** Swift implements ActivityKit; Kotlin continues to own the workout and its notification policy. */
interface IosWorkoutLiveActivities {
    fun isAvailable(): Boolean
    fun update(timer: IosLiveTimer)
    fun cancel(kind: String)
    fun cancelAll()
}

/** One native date-based countdown, rather than a stream of per-second Kotlin updates. */
data class IosLiveTimer(
    val kind: String,
    val instanceId: String,
    val title: String,
    val detail: String,
    val endsAtMillis: Long,
    val totalSeconds: Int,
    val sessionId: Int = -1,
    val sessionStartedAtMillis: Long = 0,
    val addSeconds: Int = 30,
    val subtractSeconds: Int = 10,
    val showAdd: Boolean = true,
    val showSubtract: Boolean = true,
    val showSkip: Boolean = true,
    val skipLabel: String = "Skip",
    val finishedText: String = "",
    /** The next set split in two lines for the native card; empty when it cannot be split safely. */
    val exercise: String = "",
    val setLine: String = "",
    /** 0xAARRGGBB of the app theme's dark-scheme primary; 0 lets the card use its own default. */
    val accentArgb: Long = 0,
    /** EMOM only: the minute (or lead-in) on the clock; 0 when paused or not an EMOM run. */
    val roundStartsAtMillis: Long = 0,
    val roundEndsAtMillis: Long = 0,
    val paused: Boolean = false,
    /** EMOM only: buttons in order, keys "emom.done|pause|resume|stop" with labels in the app language. */
    val actionKeys: List<String> = emptyList(),
    val actionLabels: List<String> = emptyList()
)

/** Updated by the iOS theme, read whenever a countdown is published. */
object IosLiveActivityStyle {
    var accentArgb: Long = 0
}

/** Installed by Swift before the Compose root starts, following the existing CryptoKit bridge pattern. */
object IosWorkoutLiveActivityBridge {
    var service: IosWorkoutLiveActivities? = null
}

internal data class IosLiveTimerContext(
    val sessionId: Int = -1,
    val sessionStartedAtMillis: Long = 0,
    val addSeconds: Int = 30,
    val subtractSeconds: Int = 10,
    val showAdd: Boolean = true,
    val showSubtract: Boolean = true,
    val showSkip: Boolean = true
) {
    fun apply(timer: IosLiveTimer, skipLabel: String): IosLiveTimer = timer.copy(
        sessionId = sessionId, sessionStartedAtMillis = sessionStartedAtMillis,
        addSeconds = addSeconds, subtractSeconds = subtractSeconds,
        showAdd = showAdd && sessionId >= 0, showSubtract = showSubtract && sessionId >= 0,
        showSkip = showSkip && sessionId >= 0, skipLabel = skipLabel)
}
