package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.data.local.entity.RoutineGroupEntity
import com.emanuel5014.trainable.data.local.entity.SYSTEM_PLAN_NOTE
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.ui.screens.history.HistoryFilters
import com.emanuel5014.trainable.ui.screens.history.NO_ROUTINE_PLAN_ID
import com.emanuel5014.trainable.ui.screens.history.RoutineBlockKind
import com.emanuel5014.trainable.ui.screens.history.historyFilterCounts
import com.emanuel5014.trainable.ui.screens.history.lastDaysRange
import com.emanuel5014.trainable.ui.screens.history.routinePickerBlocks
import com.emanuel5014.trainable.ui.screens.history.yearRange
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The routine blocks and the counts of the history's filter sheet. */
class HistoryFilterOptionsTest {
    private val zone = TimeZone.of("Europe/Rome")

    private fun at(year: Int, month: Int, day: Int) = LocalDateTime(year, month, day, 18, 0).toInstant(zone).toEpochMilliseconds()

    private fun plan(id: Int, name: String, active: Boolean = true, groupId: Int? = null) =
        WorkoutPlanEntity(id = id, userId = 1, nome = name, dataInizio = 0L, isActive = active, groupId = groupId)

    // Plan 99 stands for the app's own quick-workout plan
    private val quick = WorkoutPlanEntity(id = 99, userId = 1, nome = "Quick Workout", dataInizio = 0L, isActive = false, note = SYSTEM_PLAN_NOTE)

    private val summer1 = plan(1, "1 GIORNO", active = false, groupId = 10)
    private val summer2 = plan(2, "2 GIORNO", active = false, groupId = 10)
    private val autumn1 = plan(3, "1 GIORNO", groupId = 20)
    private val loose = plan(4, "Gambe", active = false)
    private val looseTwin = plan(5, "gambe ")
    private val plans = listOf(summer1, summer2, autumn1, loose, looseTwin)

    private val summer = RoutineGroupEntity(id = 10, name = "Estate", ordine = 1)
    private val autumn = RoutineGroupEntity(id = 20, name = "Autunno", ordine = 0)

    private var nextId = 1
    private fun session(plan: WorkoutPlanEntity, timestamp: Long) = SessionWithDetails(
        session = WorkoutSessionEntity(id = nextId++, planId = plan.id, timestamp = timestamp),
        plan = plan,
        sets = emptyList(),
        cardio = emptyList()
    )

    private val sessions = listOf(
        session(summer1, at(2025, 7, 1)),
        session(summer1, at(2025, 8, 3)),
        session(summer2, at(2025, 7, 2)),
        session(autumn1, at(2026, 10, 1)),
        session(autumn1, at(2026, 11, 2)),
        session(loose, at(2026, 3, 5)),
        session(quick, at(2026, 10, 9))
    )

    private fun counts(filters: HistoryFilters = HistoryFilters()) = historyFilterCounts(sessions, filters, journalEnabled = false, zone = zone)

    @Test
    fun aGroupKeepsEveryRoutineOfIt() {
        val ids = HistoryFilters(groupId = 10).apply(sessions, journalEnabled = false).map { it.session.planId }
        assertEquals(listOf(1, 1, 2), ids)
    }

    @Test
    fun countsLeaveTheirOwnFilterOut() {
        val counts = counts(HistoryFilters(planId = 3, startDate = at(2026, 10, 1) - 1, endDate = at(2026, 10, 31)))
        assertEquals(7, counts.total)
        assertEquals(1, counts.shown)
        // Routines count within the period, whatever routine is picked
        assertEquals(mapOf(3 to 1), counts.byPlan)
        assertEquals(1, counts.noRoutine)
        // Months count within the routine, whatever period is picked
        assertEquals(mapOf((2026 to 9) to 1, (2026 to 10) to 1), counts.byMonth)
        assertEquals(2025, counts.firstYear)
    }

    @Test
    fun periodsSpanTheWholeHistory() {
        val counts = counts(HistoryFilters(planId = 4))
        assertEquals(at(2025, 7, 1) to at(2025, 8, 3), counts.periodByPlan[1])
        assertNull(counts.periodByPlan[99])
    }

    @Test
    fun groupsComeInTheirOrderThenTheRoutinesOfNoGroup() {
        val blocks = routinePickerBlocks(plans + quick, listOf(summer, autumn), counts())
        assertEquals(listOf("Autunno", "Estate", null), blocks.map { it.group?.name })
        assertEquals(RoutineBlockKind.Loose, blocks.last().kind)
        // Active routines first, the quick-workout plan never
        assertEquals(listOf(5, 4), blocks.last().routines.map { it.plan.id })
        assertTrue(blocks[1].archived)
        assertFalse(blocks[1].openByDefault)
        assertTrue(blocks[0].openByDefault)
        assertEquals(3, blocks[1].workouts)
        assertEquals(at(2025, 7, 1) to at(2025, 8, 3), blocks[1].period)
    }

    @Test
    fun twinNamesInOneBlockShowTheirPeriod() {
        val blocks = routinePickerBlocks(plans, listOf(summer, autumn), counts())
        assertTrue(blocks.last().routines.all { it.showPeriod })
        // Same name in two different groups: the group already tells them apart
        assertFalse(blocks.first().routines.single().showPeriod)
    }

    @Test
    fun withoutGroupsTheBlocksAreActiveAndArchived() {
        val blocks = routinePickerBlocks(plans, emptyList(), counts())
        assertEquals(listOf(RoutineBlockKind.Active, RoutineBlockKind.Archived), blocks.map { it.kind })
        assertEquals(listOf(3, 5), blocks[0].routines.map { it.plan.id })
        // A group that is gone counts as no group
        val gone = routinePickerBlocks(plans, listOf(autumn), counts())
        assertEquals(listOf(RoutineBlockKind.Group, RoutineBlockKind.Loose), gone.map { it.kind })
        assertEquals(listOf(5, 1, 2, 4), gone[1].routines.map { it.plan.id })
    }

    @Test
    fun noRoutineAndGroupFiltersDoNotMix() {
        val shown = HistoryFilters(planId = NO_ROUTINE_PLAN_ID).apply(sessions, journalEnabled = false)
        assertEquals(listOf(99), shown.map { it.session.planId })
        assertEquals(HistoryFilters(startDate = 1L), HistoryFilters(planId = 3, groupId = 10, startDate = 1L).anyRoutine())
    }

    @Test
    fun yearAndLastDaysAreWholeLocalDays() {
        assertEquals(
            LocalDateTime(2026, 1, 1, 0, 0).toInstant(zone).toEpochMilliseconds() to
                LocalDateTime(2027, 1, 1, 0, 0).toInstant(zone).toEpochMilliseconds() - 1,
            yearRange(2026, zone)
        )
        // Across the end of summer time (25 October 2026)
        val (start, end) = lastDaysRange(30, LocalDate(2026, 11, 15), zone)
        assertEquals(LocalDateTime(2026, 10, 17, 0, 0).toInstant(zone).toEpochMilliseconds(), start)
        assertEquals(LocalDateTime(2026, 11, 16, 0, 0).toInstant(zone).toEpochMilliseconds() - 1, end)
    }
}
