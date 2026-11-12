package com.emanuel5014.trainable.ui.components

import android.graphics.Bitmap
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.drawToBitmap
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails

/** Android keeps its original assets, prescription helpers and native bitmap capture. */
@Composable
fun WorkoutShareCard(
    sessionDetails: SessionWithDetails,
    planName: String,
    languageCode: String,
    weightUnit: String,
    planExercises: List<PlanExerciseWithDetails>? = null,
    showPrescription: Boolean = false,
    modifier: Modifier = Modifier,
    exercisesById: Map<Int, ExerciseEntity> = emptyMap()
) {
    val context = LocalContext.current
    WorkoutShareCardContent(
        sessionDetails, planName, languageCode, weightUnit, planExercises, showPrescription, modifier,
        logo = painterResource(R.drawable.ic_app_logo),
        appName = stringResource(R.string.app_name),
        setDetailsText = { set, prescription -> setLogDetailsText(context, set, withPrescription = prescription) },
        exercisesById = exercisesById
    )
}

fun captureViewToBitmap(view: View): Bitmap {
    return view.drawToBitmap()
}
