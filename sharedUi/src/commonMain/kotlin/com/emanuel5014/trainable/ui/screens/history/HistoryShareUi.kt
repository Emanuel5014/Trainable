package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.runtime.Composable
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails

/**
 * Each platform captures the shared workout card, previews it and saves/shares its image.
 * [NoHistoryShare] hides the entry on hosts without an image exporter.
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
