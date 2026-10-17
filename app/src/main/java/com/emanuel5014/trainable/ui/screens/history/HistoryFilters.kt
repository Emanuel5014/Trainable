package com.emanuel5014.trainable.ui.screens.history

import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.domain.journal.JournalFilters

/** What the history is narrowed to: a routine, a date range and, with the workout journal on, tags, rating and notes. */
data class HistoryFilters(
    /** A routine, [NO_ROUTINE_PLAN_ID] for the workouts of no routine, or null for all. */
    val planId: Int? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val journal: JournalFilters = JournalFilters()
) {
    /** Whether anything narrows the list; the journal filters count only while the journal is on. */
    fun isActive(journalEnabled: Boolean): Boolean =
        planId != null || startDate != null || endDate != null || (journalEnabled && journal.isActive)

    fun apply(sessions: List<SessionWithDetails>, journalEnabled: Boolean): List<SessionWithDetails> =
        sessions.filter { matches(it, journalEnabled) }

    private fun matches(details: SessionWithDetails, journalEnabled: Boolean): Boolean {
        val session = details.session
        val matchesPlan = when (planId) {
            null -> true
            NO_ROUTINE_PLAN_ID -> details.plan.isSystemPlan
            else -> session.planId == planId
        }
        // The whole end day counts
        val matchesDate = (startDate == null || session.timestamp >= startDate) &&
            (endDate == null || session.timestamp <= endDate + END_OF_DAY_MS)
        val matchesJournal = !journalEnabled ||
            journal.matches(session.rating, session.journalNote, details.tags.map { it.id }.toSet())
        return matchesPlan && matchesDate && matchesJournal
    }

    private companion object {
        const val END_OF_DAY_MS = 86_399_999L
    }
}
