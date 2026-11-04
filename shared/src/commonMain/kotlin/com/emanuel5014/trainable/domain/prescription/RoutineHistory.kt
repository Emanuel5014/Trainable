package com.emanuel5014.trainable.domain.prescription

import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Which past workouts of a routine are its "last time" (the weights, reps, notes and progression a new workout starts
 * from). A routine emptied and filled again with a new start date is a new routine: the workouts before that day
 * belonged to the old one. The start date counts from the first minute of the day it shows. Quick and custom
 * workouts are not routines and keep all their workouts (an import can put older ones in them).
 */
object RoutineHistory {
    fun since(startDate: Long, isSystemPlan: Boolean, zone: TimeZone = TimeZone.currentSystemDefault()): Long {
        if (isSystemPlan) return 0L
        return Instant.fromEpochMilliseconds(startDate).toLocalDateTime(zone).date.atStartOfDayIn(zone).toEpochMilliseconds()
    }
}
