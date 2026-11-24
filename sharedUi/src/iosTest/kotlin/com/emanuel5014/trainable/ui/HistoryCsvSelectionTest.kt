package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.data.local.entity.SYSTEM_PLAN_NOTE
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.ui.screens.history.export.ExportPeriod
import com.emanuel5014.trainable.ui.screens.history.export.ExportPeriods
import com.emanuel5014.trainable.ui.screens.history.export.HistoryCsvSelection
import com.emanuel5014.trainable.ui.screens.history.lastDaysRange
import com.emanuel5014.trainable.ui.screens.history.yearRange
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

/** Which workouts a CSV export takes. */
class HistoryCsvSelectionTest {
    private val zone = TimeZone.of("Europe/Rome")
    private val today = LocalDate(2026, 11, 24)

    private fun at(year: Int, month: Int, day: Int) = LocalDateTime(year, month, day, 18, 0).toInstant(zone).toEpochMilliseconds()

    private fun plan(id: Int, groupId: Int? = null) = WorkoutPlanEntity(id = id, userId = 1, nome = "P$id", dataInizio = 0L, groupId = groupId)
    private val quick = WorkoutPlanEntity(id = 99, userId = 1, nome = "Quick Workout", dataInizio = 0L, isActive = false, note = SYSTEM_PLAN_NOTE)

    private val push = plan(1, groupId = 10)
    private val pull = plan(2, groupId = 10)
    private val upper = plan(3)

    private var nextId = 1
    private fun session(plan: WorkoutPlanEntity, timestamp: Long) = SessionWithDetails(
        session = WorkoutSessionEntity(id = nextId++, planId = plan.id, timestamp = timestamp, isFinished = true),
        plan = plan,
        sets = emptyList(),
        cardio = emptyList()
    )

    private val sessions = listOf(
        session(push, at(2026, 11, 20)),
        session(pull, at(2026, 11, 21)),
        session(upper, at(2026, 6, 1)),
        session(quick, at(2025, 5, 5))
    )

    private fun plans(selection: HistoryCsvSelection) = selection.workouts(sessions).map { it.session.planId }

    @Test
    fun nothingPickedTakesEveryWorkout() {
        assertEquals(listOf(1, 2, 3, 99), plans(HistoryCsvSelection()))
    }

    @Test
    fun routinesGroupsAndNoRoutineAddUp() {
        assertEquals(listOf(1, 3), plans(HistoryCsvSelection(planIds = setOf(1, 3))))
        assertEquals(listOf(1, 2), plans(HistoryCsvSelection(groupIds = setOf(10))))
        assertEquals(listOf(1, 2, 99), plans(HistoryCsvSelection(groupIds = setOf(10), noRoutine = true)))
        assertEquals(listOf(99), plans(HistoryCsvSelection(noRoutine = true)))
    }

    @Test
    fun thePeriodShortcutsAreRecognised() {
        fun period(range: Pair<Long, Long>?) = ExportPeriods.of(HistoryCsvSelection().withPeriod(range), today, zone)
        assertEquals(ExportPeriod.ALL, period(null))
        assertEquals(ExportPeriod.LAST_30_DAYS, period(lastDaysRange(30, today, zone)))
        assertEquals(ExportPeriod.LAST_3_MONTHS, period(lastDaysRange(90, today, zone)))
        assertEquals(ExportPeriod.THIS_YEAR, period(yearRange(2026, zone)))
        assertEquals(ExportPeriod.LAST_YEAR, period(yearRange(2025, zone)))
        // A month picked in the history is a range of its own
        assertEquals(ExportPeriod.CUSTOM, period(at(2026, 6, 1) to at(2026, 6, 30)))
    }

    @Test
    fun thePeriodKeepsItsWorkouts() {
        assertEquals(listOf(99), plans(HistoryCsvSelection().withPeriod(yearRange(2025, zone))))
        assertEquals(listOf(1, 2), plans(HistoryCsvSelection().withPeriod(lastDaysRange(30, today, zone))))
    }
}
