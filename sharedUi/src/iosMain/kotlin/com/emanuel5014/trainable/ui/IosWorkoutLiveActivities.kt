package com.emanuel5014.trainable.ui

/** Swift implements ActivityKit; Kotlin continues to own the workout and its notification policy. */
interface IosWorkoutLiveActivities {
    fun isAvailable(): Boolean
    fun update(timer: IosLiveTimer)
    fun cancel(kind: String)
    fun cancelAll()
    /** Ends every card of a workout that was deleted, with the alerts still pending for it. */
    fun cancelSession(sessionId: Int)
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
    val actionLabels: List<String> = emptyList(),
    /** Rest only, once it is over: keys "rest.more|rest.next" with labels, in that order; empty hides the row. */
    val finishedActionKeys: List<String> = emptyList(),
    val finishedActionLabels: List<String> = emptyList(),
    /**
     * Rest only: "Open Trainable to continue", carried with the buttons so that a press reaching no open workout
     * (cold launch, no Compose strings yet) can still say it. Shown only when [needsApp].
     */
    val openAppText: String = "",
    val needsApp: Boolean = false,
    /** Rest only: the set "Set done" would log, as the card shows it ([NextSetKey.token]); empty when none. */
    val nextSetToken: String = "",
    /** Live card only ([LIVE_NOTICE]): what it shows ([LiveModes]), all words already in the app language. */
    val liveMode: String = "",
    val workoutName: String = "",
    /** The big text of a set or of the finished workout ("80 kg × 8"); empty where the big text is a clock. */
    val liveValue: String = "",
    val liveTitle: String = "",
    val liveDetail: String = "",
    /** "Exercise 2 of 6", next to the workout's name. */
    val liveSide: String = "",
    /** The bar: one value per set, 0 to come, 1 the one to do, 2 done. */
    val segments: List<Int> = emptyList(),
    val workoutStartedAtMillis: Long = 0,
    /** When the live card is no longer wanted (the workout should be long over). */
    val liveUntilMillis: Long = 0,
    /** The timer card a live card shows, so its buttons reach that timer: kind and instance, empty for none. */
    val timerKind: String = "",
    val timerInstanceId: String = "",
    /** The set in the Dynamic Island ("3/4"). */
    val compactText: String = ""
)

/** Buttons of the finished rest card, as the Live Activity intent sends them. */
internal const val REST_MORE_ACTION = "rest.more"
internal const val REST_NEXT_ACTION = "rest.next"

/** The finished rest card once a button found no open workout: no buttons, only the hint to open the app. */
internal fun IosLiveTimer.needingApp(): IosLiveTimer =
    copy(finishedActionKeys = emptyList(), finishedActionLabels = emptyList(), needsApp = true)

/** Updated by the iOS theme, read whenever a countdown is published. */
object IosLiveActivityStyle {
    var accentArgb: Long = 0
}

/** Installed by Swift before the Compose root starts, following the existing CryptoKit bridge pattern. */
object IosWorkoutLiveActivityBridge {
    /** Swift's service, behind the live card composer: callers keep seeing one [IosWorkoutLiveActivities]. */
    var service: IosWorkoutLiveActivities? = null
        set(value) { field = value?.let { if (it is IosLiveCardComposer) it else IosLiveCardComposer(it) } }
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
