@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.*
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.emanuel5014.trainable.ui.screens.workout.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication

/** Owns one model for this navigation entry. In-app clocks and local notifications share the same wall-clock deadlines. */
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
    private val timers = IosWorkoutTimers(context = ::timerContext)
    val model = WorkoutModel(IosAppGraph.workouts, IosAppGraph.preferences, IosAppGraph.exercises,
        timers, { currentWords }, IosWorkoutMiniSetStore(), IosAppGraph.oneRepMaxes,
        IosAppGraph.journal, languages, viewModelScope, planId, sessionId, quickStart, workoutName)

    init {
        IosWorkoutTimerActions.bind(model, timers)
        viewModelScope.launch {
            model.state.map { timerContext() }.distinctUntilChanged().collect { timers.refreshControls() }
        }
    }

    private fun timerContext(): IosLiveTimerContext = model.state.value.let { state ->
        IosLiveTimerContext(state.sessionId ?: -1, state.sessionStartTime ?: 0,
            state.timerAddSeconds, state.timerSubtractSeconds,
            state.timerShowTimeButtons && state.timerAddEnabled,
            state.timerShowTimeButtons && state.timerSubtractEnabled, state.timerShowSkipButton)
    }

    fun updateText(words: WorkoutText, language: String) { currentWords = words; languages.value = language }
    override fun onCleared() { IosWorkoutTimerActions.unbind(model); model.close(); super.onCleared() }
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
    // Completing a workout returns to the dashboard without any completion message.
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
