package com.emanuel5014.trainable.ui.widgets

import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.domain.suggestion.WorkoutSuggester
import com.emanuel5014.trainable.domain.weekStartDate
import com.emanuel5014.trainable.widget.model.DayMark
import com.emanuel5014.trainable.widget.model.RoutineWidgetState
import com.emanuel5014.trainable.widget.model.routineWidgetState
import com.emanuel5014.trainable.widget.model.weekProgress
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

/**
 * What the iOS home-screen widgets show, written by the app into the App Group for the widget extension
 * (HomeWidgets.swift reads the same fields). One [HomeWidgetDay] per day from today, so the widgets move on at
 * midnight without the app: "suggested today", the week row and a new week all depend on the day.
 * Texts are already in the app's language, which can differ from the phone's.
 */
@Serializable
data class HomeWidgetSnapshot(
    val version: Int = 1,
    val accent: HomeWidgetAccent,
    val days: List<HomeWidgetDay>
)

/** 0xAARRGGBB of the app theme's primary and tertiary, light and dark: the ring and button gradient. */
@Serializable
data class HomeWidgetAccent(val lightStart: Long, val lightEnd: Long, val darkStart: Long, val darkEnd: Long)

@Serializable
data class HomeWidgetDay(
    /** Local midnight of this day, in seconds since 1970. */
    val startsAt: Long,
    val routine: HomeWidgetRoutine,
    val week: HomeWidgetWeek
)

@Serializable
data class HomeWidgetRoutine(
    /** "suggested", "inProgress" or "none": the badge, the button style and the Lock Screen icon. */
    val kind: String,
    val label: String,
    val title: String,
    val subtitle: String? = null,
    val button: String,
    /** What the button (and the small widget, the quick start) opens: start, resume or just the app. */
    val link: String,
    val accessibility: String
)

@Serializable
data class HomeWidgetWeek(
    val done: Int,
    val goal: Int,
    val met: Boolean,
    val label: String,
    /** "1 to go" or "Goal met". */
    val title: String,
    /** "of which 1 cardio", null when there was none. */
    val cardio: String? = null,
    /** "workouts", under the count in the small ring. */
    val unit: String,
    val days: List<HomeWidgetWeekDay>
)

/** [mark] is "trained", "today" (today, nothing done yet) or "empty". */
@Serializable
data class HomeWidgetWeekDay(val letter: String, val mark: String)

/** The widget texts in the app's language (the same strings as the Android widgets). */
interface HomeWidgetTexts {
    val upNext: String
    val suggestedToday: String
    val inProgress: String
    val createRoutine: String
    val start: String
    val resume: String
    val openApp: String
    val goalTitle: String
    val goalUnit: String
    val goalMet: String
    val openTrainable: String
    fun startedAt(timestampMillis: Long): String
    fun dayLetter(day: DayOfWeek): String
    suspend fun exerciseCount(count: Int): String
    suspend fun remaining(count: Int): String
    suspend fun cardio(count: Int): String
    suspend fun startName(name: String): String
    suspend fun resumeName(name: String): String
}

/** The links the widgets open; IosWidgetLinks reads them back. */
object HomeWidgetLinks {
    const val SCHEME = "trainable"
    const val OPEN = "$SCHEME://open"
    fun start(planId: Int) = "$SCHEME://workout?plan=$planId"
    fun resume(planId: Int, sessionId: Int) = "$SCHEME://workout?plan=$planId&session=$sessionId"
}

/**
 * Today and the next [dayCount] - 1 days, with the Android widgets' rules: an open workout first, then the day's
 * routine, then the next in the sequence ([WorkoutSuggester]); the week counted from the locale's first day.
 * [finished] are the finished sessions, [cardioThisWeek] the cardio ones since this week started (a later week
 * starts at zero, as nothing can be logged in it yet).
 */
suspend fun buildHomeWidgetSnapshot(
    plans: List<WorkoutPlanEntity>,
    finished: List<WorkoutSessionEntity>,
    unfinished: List<SessionWithPlanName>,
    exerciseCount: suspend (planId: Int) -> Int?,
    goal: Int,
    cardioThisWeek: Int,
    today: LocalDate,
    zone: TimeZone,
    firstDay: DayOfWeek,
    texts: HomeWidgetTexts,
    accent: HomeWidgetAccent,
    dayCount: Int = 7
): HomeWidgetSnapshot {
    val thisWeek = weekStartDate(today, firstDay)
    val timestamps = finished.map { it.timestamp }
    val counts = mutableMapOf<Int, Int?>()
    val days = (0 until dayCount).map { offset ->
        val date = today.plus(offset, DateTimeUnit.DAY)
        val suggestion = WorkoutSuggester.suggest(plans, finished, date, zone)
        val countedId = (suggestion.todayPlan ?: suggestion.suggestedPlan)?.id
        val count = countedId?.let { id -> counts.getOrPut(id) { exerciseCount(id) } }
        val state = routineWidgetState(unfinished.firstOrNull(), suggestion.todayPlan, suggestion.suggestedPlan, count)
        val cardio = if (weekStartDate(date, firstDay) == thisWeek) cardioThisWeek else 0
        val week = weekProgress(timestamps, goal, cardio, date, firstDay, zone)
        HomeWidgetDay(
            startsAt = date.atStartOfDayIn(zone).epochSeconds,
            routine = routineOf(state, texts),
            week = HomeWidgetWeek(
                done = week.done,
                goal = week.goal,
                met = week.met,
                label = texts.goalTitle,
                title = if (week.met) texts.goalMet else texts.remaining(week.remaining),
                cardio = week.cardio.takeIf { it > 0 }?.let { texts.cardio(it) },
                unit = texts.goalUnit,
                days = week.days.map { slot ->
                    HomeWidgetWeekDay(
                        letter = texts.dayLetter(slot.day),
                        mark = when (slot.mark) {
                            DayMark.TRAINED -> "trained"
                            DayMark.TODAY_EMPTY -> "today"
                            DayMark.EMPTY -> "empty"
                        }
                    )
                }
            )
        )
    }
    return HomeWidgetSnapshot(accent = accent, days = days)
}

private suspend fun routineOf(state: RoutineWidgetState, texts: HomeWidgetTexts): HomeWidgetRoutine = when (state) {
    is RoutineWidgetState.InProgress -> HomeWidgetRoutine(
        kind = "inProgress",
        label = texts.inProgress,
        title = state.name,
        subtitle = texts.startedAt(state.startedAt),
        button = texts.resume,
        link = HomeWidgetLinks.resume(state.planId, state.sessionId),
        accessibility = texts.resumeName(state.name)
    )
    is RoutineWidgetState.Suggested -> HomeWidgetRoutine(
        kind = "suggested",
        label = if (state.today) texts.suggestedToday else texts.upNext,
        title = state.name,
        subtitle = state.exerciseCount?.takeIf { it > 0 }?.let { texts.exerciseCount(it) },
        button = texts.start,
        link = HomeWidgetLinks.start(state.planId),
        accessibility = texts.startName(state.name)
    )
    RoutineWidgetState.NoRoutine -> HomeWidgetRoutine(
        kind = "none",
        label = "Trainable",
        title = texts.createRoutine,
        button = texts.openApp,
        link = HomeWidgetLinks.OPEN,
        accessibility = texts.openTrainable
    )
}
