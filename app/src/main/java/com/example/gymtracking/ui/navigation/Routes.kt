package com.example.gymtracking.ui.navigation

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
data class WorkoutExecution(val planId: Int? = null, val sessionId: Int? = null)

@Serializable
data class RoutineDetail(val planId: Int)

@Serializable
data class WorkoutDetail(val sessionId: Int)
