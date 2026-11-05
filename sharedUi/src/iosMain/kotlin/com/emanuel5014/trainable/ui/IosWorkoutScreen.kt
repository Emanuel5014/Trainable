@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.*
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.emanuel5014.trainable.ui.screens.workout.*
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.program_completed
import com.emanuel5014.trainable.util.notification.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

/** Owns one model for this navigation entry. In-app clocks work; system notifications/Live Activities are a later phase. */
@Composable
internal fun IosWorkoutScreen(entry: NavBackStackEntry, planId: Int?, sessionId: Int?, quickStart: Boolean, workoutName: String?,
    onNavigateBack: () -> Unit, onNavigateToRoutine: (Int) -> Unit) {
    val words = rememberIosWorkoutText()
    val language = Locale.current.language
    val owner = viewModel<IosWorkoutOwner>(viewModelStoreOwner = entry) {
        IosWorkoutOwner(planId, sessionId, quickStart, workoutName, words, language)
    }
    SideEffect { owner.updateText(words, language) }
    CompositionLocalProvider(LocalWorkoutScreenPlatform provides IosWorkoutScreenPlatform) {
        WorkoutExecutionContent(onNavigateBack, onNavigateToRoutine, owner.model)
    }
}

/** Retained by the navigation entry, so visiting the routine and coming back never creates another session. */
private class IosWorkoutOwner(planId: Int?, sessionId: Int?, quickStart: Boolean, workoutName: String?,
    words: WorkoutText, language: String) : ViewModel() {
    private var currentWords = words
    private val languages = MutableStateFlow(language)
    val model = WorkoutModel(IosAppGraph.workouts, IosAppGraph.preferences, IosAppGraph.exercises,
        IosWorkoutTimers(), { currentWords }, IosWorkoutMiniSetStore(), IosAppGraph.oneRepMaxes,
        IosAppGraph.journal, languages, viewModelScope, planId, sessionId, quickStart, workoutName)

    fun updateText(words: WorkoutText, language: String) { currentWords = words; languages.value = language }
    override fun onCleared() { model.close(); super.onCleared() }
}

private object IosWorkoutScreenPlatform : WorkoutScreenPlatform {
    @Composable
    override fun KeepScreenOn(enabled: Boolean) {
        DisposableEffect(enabled) {
            val application = UIApplication.sharedApplication
            val previous = application.idleTimerDisabled
            application.idleTimerDisabled = enabled
            onDispose { application.idleTimerDisabled = previous }
        }
    }

    @Composable
    override fun rememberProgramCompletedNotifier(): () -> Unit {
        val show = LocalShowMessage.current
        val text = stringResource(Res.string.program_completed)
        return remember(show, text) { { show(text) } }
    }
}

/** Store one dictionary in a single defaults write, so a partially written mini-set cannot be resumed. */
internal class IosWorkoutMiniSetStore(private val key: String = "workout_mini_set.snapshot") : WorkoutMiniSetStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    private fun value(name: String): String? = defaults.dictionaryForKey(key)?.get(name) as? String
    override fun sessionId(): Int = value(MINI_SESSION)?.toIntOrNull() ?: -1
    override fun setId(): Int = value(MINI_SET)?.toIntOrNull() ?: -1
    override fun missing(): Int = value(MINI_MISSING)?.toIntOrNull() ?: 0
    override fun startedAt(default: Long): Long = value(MINI_STARTED)?.toLongOrNull() ?: default
    override fun endsAt(): Long = value(MINI_ENDS)?.toLongOrNull() ?: 0L
    override fun pauseSeconds(): Int = value(MINI_PAUSE)?.toIntOrNull() ?: -1
    override fun save(value: PersistedMiniSet) {
        defaults.setObject(mapOf(MINI_SESSION to value.sessionId.toString(), MINI_SET to value.setId.toString(),
            MINI_MISSING to value.missing.toString(), MINI_STARTED to value.startedAt.toString(),
            MINI_ENDS to value.endsAt.toString(), MINI_PAUSE to value.pauseSeconds.toString()), forKey = key)
    }
    override fun clear() { defaults.removeObjectForKey(key) }
}

/** The shared model owns wall-clock timers; no system notification/alarm is posted until iOS delivery is implemented. */
internal class IosWorkoutTimers : WorkoutTimers {
    override val timerEvents = MutableSharedFlow<TimerAction>(extraBufferCapacity = 1)
    override val warmupTimerEvents = MutableSharedFlow<WarmupTimerAction>(extraBufferCapacity = 1)
    override val emomEvents = MutableSharedFlow<EmomAction>(extraBufferCapacity = 4)
    override fun cancelCustomVibration() = Unit
    override fun cancelEmom() = Unit
    override fun cancelEmomBoundaryAlarm() = Unit
    override fun cancelFinishAlarm() = Unit
    override fun cancelTimer() = Unit
    override fun cancelWarmupFinishAlarm() = Unit
    override fun cancelWarmupTimer() = Unit
    override fun hasNotificationPermission(): Boolean = false
    override fun isLiveNotificationSupported(): Boolean = false
    override fun scheduleEmomBoundaryAlarm(triggerAtMillis: Long) = Unit
    override fun showEmomFinished(title: String, text: String) = Unit
    override fun showEmomNotification(info: EmomNotificationInfo) = Unit
    override fun showRestFinished() = Unit
    override fun showWarmupTimerFinished() = Unit
    override fun startOrUpdateTimerNotification(
        remainingSeconds: Int,
        sessionId: Int,
        exerciseName: String?,
        nextSetNumber: Int?,
        nextSetWeight: Float?,
        nextSetReps: Int?,
        previousReps: Int?,
        weightUnit: String?,
        totalSeconds: Int?,
        scheduleAlarm: Boolean,
        nextSetRepsLabel: String?,
        nextSetDetail: String?,
        nextSetText: String?
    ) = Unit
    override fun startOrUpdateWarmupTimerNotification(
        remainingSeconds: Int,
        totalSeconds: Int?,
        scheduleAlarm: Boolean
    ) = Unit

    override fun vibrateEmomCue(cue: EmomCue) {
        when (cue) {
            EmomCue.TICK -> UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
            EmomCue.GO -> UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy).impactOccurred()
            EmomCue.DONE -> UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
        }
    }
}
