package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity

/**
 * The photos of a routine: shown, added from the camera or the gallery, removed. Each platform draws them (Android:
 * AndroidRoutineImagePicker, iOS: IosRoutineImagePicker, both drawing the shared RoutineImagePicker).
 */
val LocalRoutineImagePicker = staticCompositionLocalOf<@Composable (
    images: List<WorkoutPlanImageEntity>,
    onImageAdd: (String) -> Unit,
    onImageRemove: (WorkoutPlanImageEntity) -> Unit,
    modifier: Modifier
) -> Unit> { { _, _, _, _ -> } }
