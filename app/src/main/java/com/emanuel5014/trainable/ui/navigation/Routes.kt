package com.emanuel5014.trainable.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object MainTabs

@Serializable
object Dashboard

@Serializable
object Routines

@Serializable
object Analytics

@Serializable
object Settings

@Serializable
object Onboarding

@Serializable
object History

@Serializable
data class WorkoutExecution(
    val planId: Int? = null,
    val sessionId: Int? = null,
    val quickStart: Boolean = false,
    val workoutName: String? = null
)

@Serializable
data class RoutineDetail(val planId: Int)

@Serializable
data class WorkoutDetail(val sessionId: Int)

@Serializable
data class EditWorkoutSession(val sessionId: Int)

@Serializable
data class CompareSessions(val sessionId1: Int, val sessionId2: Int)

@Serializable
object PhysicalCheck

@Serializable
object PhysicalCheckSettings

@Serializable
data class PhysicalCheckCompare(val id1: Int, val id2: Int)

@Serializable
data class Report(val planIdsString: String)

