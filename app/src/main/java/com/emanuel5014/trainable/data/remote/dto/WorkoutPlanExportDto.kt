package com.emanuel5014.trainable.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutPlanExportDto(
    val nome: String,
    val note: String?,
    val sessioniTargetSettimana: Int,
    val imageUri: String?, // Keep for backward compatibility
    val images: List<String> = emptyList(),
    val exercises: List<PlanExerciseExportDto>
)

@Serializable
data class PlanExerciseExportDto(
    val exerciseId: Int,
    val exerciseName: String? = null,
    val exerciseCategory: String? = null,
    val serieTarget: Int,
    val repsTarget: String,
    val recuperoTarget: Int,
    val ordine: Int
)
