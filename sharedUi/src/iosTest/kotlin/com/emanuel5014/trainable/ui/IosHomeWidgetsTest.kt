package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.ui.widgets.HomeWidgetAccent
import com.emanuel5014.trainable.ui.widgets.HomeWidgetSnapshot
import com.emanuel5014.trainable.ui.widgets.HomeWidgetTexts
import com.emanuel5014.trainable.ui.widgets.IosWidgetLink
import com.emanuel5014.trainable.ui.widgets.IosWidgetLinks
import com.emanuel5014.trainable.ui.widgets.buildHomeWidgetSnapshot
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosHomeWidgetsTest {
    private val zone = TimeZone.UTC
    // Saturday 14 November 2026; weeks start on Monday
    private val today = LocalDate(2026, 11, 14)
    private val accent = HomeWidgetAccent(1, 2, 3, 4)

    private val texts = object : HomeWidgetTexts {
        override val upNext = "Up next"
        override val suggestedToday = "Suggested today"
        override val inProgress = "In progress"
        override val createRoutine = "Create a routine"
        override val start = "Start"
        override val resume = "Resume"
        override val openApp = "Open app"
        override val goalTitle = "Weekly goal"
        override val goalUnit = "workouts"
        override val goalMet = "Goal met"
        override val openTrainable = "Open Trainable"
        override fun startedAt(timestampMillis: Long) = "Started at $timestampMillis"
        override fun dayLetter(day: DayOfWeek) = day.name.take(1)
        override suspend fun exerciseCount(count: Int) = "$count exercises"
        override suspend fun remaining(count: Int) = "$count to go"
        override suspend fun cardio(count: Int) = "of which $count cardio"
        override suspend fun startName(name: String) = "Start $name"
        override suspend fun resumeName(name: String) = "Resume $name"
    }

    private fun at(date: LocalDate, hour: Int = 18) = LocalDateTime(date.year, date.month, date.day, hour, 0).toInstant(zone).toEpochMilliseconds()
    private fun plan(id: Int, name: String, days: String? = null, order: Int = id) =
        WorkoutPlanEntity(id = id, userId = 1, nome = name, dataInizio = 0, ordine = order, giorniSettimana = days)
    private fun finished(id: Int, planId: Int, date: LocalDate) =
        WorkoutSessionEntity(id = id, planId = planId, timestamp = at(date), isFinished = true)

    private suspend fun build(
        plans: List<WorkoutPlanEntity>,
        finished: List<WorkoutSessionEntity> = emptyList(),
        unfinished: List<SessionWithPlanName> = emptyList(),
        goal: Int = 4,
        cardio: Int = 0
    ) = buildHomeWidgetSnapshot(plans, finished, unfinished, { 6 }, goal, cardio, today, zone, DayOfWeek.MONDAY, texts, accent)

    @Test
    fun oneDayPerMidnightFromToday() = runBlocking {
        val snapshot = build(listOf(plan(1, "Push A")))
        assertEquals(7, snapshot.days.size)
        assertEquals(today.atStartOfDayIn(zone).epochSeconds, snapshot.days[0].startsAt)
        assertEquals(snapshot.days[0].startsAt + 6 * 86_400, snapshot.days[6].startsAt)
    }

    @Test
    fun suggestedRoutineStartsIt() = runBlocking {
        val routine = build(listOf(plan(7, "Push A"))).days[0].routine
        assertEquals("suggested", routine.kind)
        assertEquals("Up next", routine.label)
        assertEquals("Push A", routine.title)
        assertEquals("6 exercises", routine.subtitle)
        assertEquals("trainable://workout?plan=7", routine.link)
        assertEquals("Start Push A", routine.accessibility)
    }

    @Test
    fun routineOfTheDayFollowsTheDate() = runBlocking {
        // Monday's routine is "suggested today" only on Monday 16
        val days = build(listOf(plan(1, "Legs", days = "1"))).days
        assertEquals("Up next", days[0].routine.label)
        assertEquals("Suggested today", days[2].routine.label)
    }

    @Test
    fun openWorkoutWinsAndResumesThatSession() = runBlocking {
        val open = SessionWithPlanName(WorkoutSessionEntity(id = 40, planId = 3, timestamp = at(today, 9)), "Pull B")
        val routine = build(listOf(plan(1, "Push A"), plan(3, "Pull B")), unfinished = listOf(open)).days[0].routine
        assertEquals("inProgress", routine.kind)
        assertEquals("Pull B", routine.title)
        assertEquals("trainable://workout?plan=3&session=40", routine.link)
        assertEquals("Resume", routine.button)
    }

    @Test
    fun noRoutineOpensTheApp() = runBlocking {
        val routine = build(emptyList()).days[0].routine
        assertEquals("none", routine.kind)
        assertEquals("trainable://open", routine.link)
        assertNull(routine.subtitle)
    }

    @Test
    fun weekCountsThisWeekAndStartsAgainOnMonday() = runBlocking {
        val sessions = listOf(
            finished(1, 1, LocalDate(2026, 11, 9)),   // Monday
            finished(2, 1, LocalDate(2026, 11, 11)),  // Wednesday
            finished(3, 1, LocalDate(2026, 11, 12)),  // Thursday
            finished(4, 1, LocalDate(2026, 11, 6))    // last week
        )
        val days = build(listOf(plan(1, "Push A")), finished = sessions, cardio = 1).days
        val week = days[0].week
        assertEquals(3, week.done)
        assertEquals("1 to go", week.title)
        assertEquals("of which 1 cardio", week.cardio)
        assertEquals(listOf("trained", "empty", "trained", "trained", "empty", "today", "empty"), week.days.map { it.mark })
        assertEquals("M", week.days[0].letter)
        // Monday 16 is a new week: nothing done, no cardio, Monday is today
        val monday = days[2].week
        assertEquals(0, monday.done)
        assertNull(monday.cardio)
        assertEquals("today", monday.days[0].mark)
    }

    @Test
    fun goalMet() = runBlocking {
        val sessions = (9..12).map { finished(it, 1, LocalDate(2026, 11, it)) }
        val week = build(listOf(plan(1, "Push A")), finished = sessions).days[0].week
        assertTrue(week.met)
        assertEquals("Goal met", week.title)
    }

    @Test
    fun snapshotRoundTripsAsJson() = runBlocking {
        val snapshot = build(listOf(plan(1, "Push A")))
        val json = Json { encodeDefaults = true }
        val text = json.encodeToString(HomeWidgetSnapshot.serializer(), snapshot)
        assertTrue("\"lightStart\":1" in text)
        assertEquals(snapshot, json.decodeFromString(HomeWidgetSnapshot.serializer(), text))
    }

    @Test
    fun linksParse() {
        assertEquals(IosWidgetLink(7, null), IosWidgetLinks.parse("trainable://workout?plan=7"))
        assertEquals(IosWidgetLink(3, 40), IosWidgetLinks.parse("trainable://workout?plan=3&session=40"))
        assertNull(IosWidgetLinks.parse("trainable://open"))
        assertNull(IosWidgetLinks.parse("trainable://workout"))
    }

    @Test
    fun onlyTrainableLinksAreTaken() {
        assertTrue(IosWidgetLinks.open("trainable://open"))
        assertFalse(IosWidgetLinks.open("altstore-com.emanuel5014.trainable://"))
        assertFalse(IosWidgetLinks.open("file:///tmp/a.trainableplan"))
    }
}
