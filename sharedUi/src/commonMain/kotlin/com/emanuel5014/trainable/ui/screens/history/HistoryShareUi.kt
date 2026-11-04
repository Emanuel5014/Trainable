package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.runtime.Composable
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails

/**
 * Sharing a workout of the history as an image: Android only for now (it draws the card into a bitmap, previews it,
 * saves it to the gallery or shares it), so the shared screen asks the platform for it and [NoHistoryShare] leaves
 * it out.
 */
interface HistoryShareUi {
    /** Whether the history offers to share a workout. */
    val available: Boolean

    /** Prepares the image of [session] and shows its preview over the history. */
    fun share(session: SessionWithDetails, planName: String, planExercises: List<PlanExerciseWithDetails>?)

    /** The waiting layer and the preview, inside the history's root Box. */
    @Composable
    fun Overlay(languageCode: String, weightUnit: String, showPrescription: Boolean)
}

object NoHistoryShare : HistoryShareUi {
    override val available: Boolean get() = false

    override fun share(session: SessionWithDetails, planName: String, planExercises: List<PlanExerciseWithDetails>?) {}

    @Composable
    override fun Overlay(languageCode: String, weightUnit: String, showPrescription: Boolean) {}
}
