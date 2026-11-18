package com.emanuel5014.trainable.ui.screens.history

import com.emanuel5014.trainable.ui.components.heart.ShareHeart

import androidx.compose.runtime.Composable
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails

/**
 * Each platform captures the shared workout card, previews it and saves/shares its image.
 * [NoHistoryShare] hides the entry on hosts without an image exporter.
 */
interface HistoryShareUi {
    /** Whether the history offers to share a workout. */
    val available: Boolean

    /**
     * Prepares the image of [session] and shows its preview over the history. With [heart] the preview offers a
     * switch that puts the heart rate on the card.
     */
    fun share(session: SessionWithDetails, planName: String, planExercises: List<PlanExerciseWithDetails>?, heart: ShareHeart? = null)

    /**
     * The waiting layer and the preview, inside the history's root Box. [includeHeart] is the remembered state of the
     * heart-rate switch; [onIncludeHeartChange] saves a new one.
     */
    @Composable
    fun Overlay(
        languageCode: String,
        weightUnit: String,
        showPrescription: Boolean,
        exercises: Map<Int, ExerciseEntity>,
        includeHeart: Boolean,
        onIncludeHeartChange: (Boolean) -> Unit
    )
}

object NoHistoryShare : HistoryShareUi {
    override val available: Boolean get() = false

    override fun share(session: SessionWithDetails, planName: String, planExercises: List<PlanExerciseWithDetails>?, heart: ShareHeart?) {}

    @Composable
    override fun Overlay(
        languageCode: String,
        weightUnit: String,
        showPrescription: Boolean,
        exercises: Map<Int, ExerciseEntity>,
        includeHeart: Boolean,
        onIncludeHeartChange: (Boolean) -> Unit
    ) {}
}
