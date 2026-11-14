package com.emanuel5014.trainable.ui.widgets

import androidx.compose.ui.graphics.toArgb
import com.emanuel5014.trainable.data.repository.IosWorkoutsChanged
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.platform.IosAppLocale
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.widget_action_open_app
import com.emanuel5014.trainable.sharedui.resources.widget_action_resume
import com.emanuel5014.trainable.sharedui.resources.widget_action_start
import com.emanuel5014.trainable.sharedui.resources.widget_cd_open
import com.emanuel5014.trainable.sharedui.resources.widget_cd_resume
import com.emanuel5014.trainable.sharedui.resources.widget_cd_start
import com.emanuel5014.trainable.sharedui.resources.widget_create_routine
import com.emanuel5014.trainable.sharedui.resources.widget_exercise_count
import com.emanuel5014.trainable.sharedui.resources.widget_goal_cardio
import com.emanuel5014.trainable.sharedui.resources.widget_goal_met
import com.emanuel5014.trainable.sharedui.resources.widget_goal_remaining
import com.emanuel5014.trainable.sharedui.resources.widget_goal_title
import com.emanuel5014.trainable.sharedui.resources.widget_goal_unit
import com.emanuel5014.trainable.sharedui.resources.widget_label_in_progress
import com.emanuel5014.trainable.sharedui.resources.widget_label_suggested_today
import com.emanuel5014.trainable.sharedui.resources.widget_label_up_next
import com.emanuel5014.trainable.sharedui.resources.widget_started_at
import com.emanuel5014.trainable.ui.IosPaletteColors
import com.emanuel5014.trainable.ui.screens.dashboard.firstDayOfWeek
import com.emanuel5014.trainable.ui.util.narrowDayName
import com.emanuel5014.trainable.domain.weekStartMillis
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.time.Clock

/** Swift writes the snapshot into the App Group and asks WidgetKit to redraw (HomeWidgetPublisher.swift). */
interface IosHomeWidgetPublisher {
    fun publish(json: String)
}

object IosHomeWidgetBridge {
    var publisher: IosHomeWidgetPublisher? = null
}

/**
 * Keeps the widgets' snapshot in step with the app while it runs: routines, finished and open workouts, weekly goal,
 * theme colours, any workout change ([IosWorkoutsChanged]) and the day. The language is part of the texts; a new
 * language rebuilds the app's tree, and so this.
 */
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal object IosHomeWidgets {
    private val json = Json { encodeDefaults = true }

    private data class Inputs(
        val plans: List<com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity>,
        val finished: List<com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity>,
        val unfinished: List<com.emanuel5014.trainable.data.local.relation.SessionWithPlanName>,
        val goal: Int,
        val theme: Triple<Int, Int?, Int>,
        val today: LocalDate
    )

    suspend fun run(workouts: WorkoutRepository, preferences: UserPreferencesRepository) {
        val theme = combine(preferences.themePalette, preferences.dynamicColorSeed, preferences.themeStyle) { p, s, st -> Triple(p, s, st) }
        val data = combine(
            workouts.getActivePlans(), workouts.getAllSessions(), workouts.getUnfinishedSessionsWithPlanName(),
            preferences.weeklyGoal, theme
        ) { plans, finished, unfinished, goal, colors -> Inputs(plans, finished, unfinished, goal, colors, today()) }
        combine(data, days(), IosWorkoutsChanged.ticks.onStart { emit(Unit) }) { inputs, today, _ -> inputs.copy(today = today) }
            .debounce(400)
            .mapLatest { inputs -> json.encodeToString(HomeWidgetSnapshot.serializer(), snapshot(inputs, workouts)) }
            .distinctUntilChanged()
            .collect { IosHomeWidgetBridge.publisher?.publish(it) }
    }

    private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    /** Today, then again at every midnight (late when the app was suspended, which is fine: the days are absolute). */
    private fun days(): Flow<LocalDate> = flow {
        while (true) {
            val zone = TimeZone.currentSystemDefault()
            val today = today()
            emit(today)
            val next = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
            delay((next - Clock.System.now().toEpochMilliseconds()).coerceAtLeast(1_000) + 1_000)
        }
    }

    private suspend fun snapshot(inputs: Inputs, workouts: WorkoutRepository): HomeWidgetSnapshot {
        val zone = TimeZone.currentSystemDefault()
        val firstDay = firstDayOfWeek()
        val cardio = workouts.getCardioSessionCountSince(weekStartMillis(inputs.today, firstDay, zone)).first()
        return buildHomeWidgetSnapshot(
            plans = inputs.plans,
            finished = inputs.finished,
            unfinished = inputs.unfinished,
            exerciseCount = { workouts.countPlanExercises(it).first() },
            goal = inputs.goal,
            cardioThisWeek = cardio,
            today = inputs.today,
            zone = zone,
            firstDay = firstDay,
            texts = texts(),
            accent = accent(inputs.theme)
        )
    }

    private fun accent(theme: Triple<Int, Int?, Int>): HomeWidgetAccent {
        val (palette, seed, style) = theme
        val light = IosPaletteColors.colorScheme(palette, seed, style, false)
        val dark = IosPaletteColors.colorScheme(palette, seed, style, true)
        fun argb(color: androidx.compose.ui.graphics.Color) = color.toArgb().toLong() and 0xFFFFFFFFL
        return HomeWidgetAccent(argb(light.primary), argb(light.tertiary), argb(dark.primary), argb(dark.tertiary))
    }

    private suspend fun texts(): HomeWidgetTexts {
        val upNext = getString(Res.string.widget_label_up_next)
        val suggestedToday = getString(Res.string.widget_label_suggested_today)
        val inProgress = getString(Res.string.widget_label_in_progress)
        val createRoutine = getString(Res.string.widget_create_routine)
        val start = getString(Res.string.widget_action_start)
        val resume = getString(Res.string.widget_action_resume)
        val openApp = getString(Res.string.widget_action_open_app)
        val goalTitle = getString(Res.string.widget_goal_title)
        val goalUnit = getString(Res.string.widget_goal_unit)
        val goalMet = getString(Res.string.widget_goal_met)
        val openTrainable = getString(Res.string.widget_cd_open)
        val timeFormat = NSDateFormatter().apply {
            locale = IosAppLocale.formattingLocale()
            dateStyle = NSDateFormatterNoStyle
            timeStyle = NSDateFormatterShortStyle
        }
        val startedAtTemplate = getString(Res.string.widget_started_at, "%TIME%")
        return object : HomeWidgetTexts {
            override val upNext = upNext
            override val suggestedToday = suggestedToday
            override val inProgress = inProgress
            override val createRoutine = createRoutine
            override val start = start
            override val resume = resume
            override val openApp = openApp
            override val goalTitle = goalTitle
            override val goalUnit = goalUnit
            override val goalMet = goalMet
            override val openTrainable = openTrainable
            override fun startedAt(timestampMillis: Long) = startedAtTemplate.replace(
                "%TIME%", timeFormat.stringFromDate(NSDate.dateWithTimeIntervalSince1970(timestampMillis / 1000.0))
            )
            override fun dayLetter(day: DayOfWeek) = narrowDayName(day)
            override suspend fun exerciseCount(count: Int) = getPluralString(Res.plurals.widget_exercise_count, count, count)
            override suspend fun remaining(count: Int) = getPluralString(Res.plurals.widget_goal_remaining, count, count)
            override suspend fun cardio(count: Int) = getString(Res.string.widget_goal_cardio, count)
            override suspend fun startName(name: String) = getString(Res.string.widget_cd_start, name)
            override suspend fun resumeName(name: String) = getString(Res.string.widget_cd_resume, name)
        }
    }
}

/** A widget tap that should open a workout: start [planId], or resume [sessionId] when there is one. */
data class IosWidgetLink(val planId: Int, val sessionId: Int?)

/**
 * The trainable:// links the widgets open. The app may still be on its splash or onboarding, so a link waits here
 * until the navigation takes it (IosNavGraph).
 */
object IosWidgetLinks {
    private val mutablePending = MutableStateFlow<IosWidgetLink?>(null)
    internal val pending: StateFlow<IosWidgetLink?> = mutablePending.asStateFlow()

    /** True when [url] is a widget link (even one that only opens the app), so it is not taken for a routine file. */
    fun open(url: String): Boolean {
        val components = NSURLComponents(string = url) ?: return false
        if (components.scheme?.lowercase() != HomeWidgetLinks.SCHEME) return false
        parse(components)?.let { mutablePending.value = it }
        return true
    }

    internal fun consume(link: IosWidgetLink) {
        mutablePending.compareAndSet(link, null)
    }

    internal fun parse(url: String): IosWidgetLink? = NSURLComponents(string = url)?.let(::parse)

    private fun parse(components: NSURLComponents): IosWidgetLink? {
        if (components.host != "workout") return null
        val query = components.queryItems.orEmpty().filterIsInstance<NSURLQueryItem>().associate { it.name to it.value }
        val planId = query["plan"]?.toIntOrNull() ?: return null
        return IosWidgetLink(planId, query["session"]?.toIntOrNull())
    }
}
