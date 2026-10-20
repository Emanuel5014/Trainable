package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.data.local.entity.groupedByExercise
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.finalBlocks
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.drawToBitmap
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.local.relation.SetWithExercise
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.util.WeightUnitConverter

@Composable
fun WorkoutShareCard(
    sessionDetails: SessionWithDetails,
    planName: String,
    languageCode: String,
    weightUnit: String,
    planExercises: List<PlanExerciseWithDetails>? = null,
    /** Show %1RM blocks and set badges (advanced programming on). Passed in because the card is rendered in its own ComposeView. */
    showPrescription: Boolean = false,
    modifier: Modifier = Modifier
) {
    val primaryColor = Primary
    
    val planExerciseMap = planExercises?.associateBy { it.planExercise.exerciseId }
    val shareContext = androidx.compose.ui.platform.LocalContext.current
    val shareLabels = com.emanuel5014.trainable.ui.components.rememberPrescriptionLabels(weightUnit)

    // Curated dark obsidian brand palette
    val obsidianBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0C0C0E), Color(0xFF18181C))
    )
    val cardBackground = Color(0xFF1E1E24).copy(alpha = 0.5f)
    val cardBorder = Color.White.copy(alpha = 0.06f)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF9E9EA8)

    // Calculate Workout Statistics
    val sortedSets = sessionDetails.sets.sortedWith(
        compareBy(
            { it.setLog.ordineEsercizio },
            { it.setLog.numeroSerie }
        )
    )

    val exercisesWithSets = mutableListOf<ShareExerciseItem>()
    sortedSets.forEach { setWithEx ->
        val existingIndex = exercisesWithSets.indexOfFirst { 
            it.exercise.id == setWithEx.exercise.id && 
            it.sets.first().setLog.ordineEsercizio == setWithEx.setLog.ordineEsercizio 
        }
        if (existingIndex != -1) {
            val existing = exercisesWithSets[existingIndex]
            exercisesWithSets[existingIndex] = existing.copy(sets = existing.sets + setWithEx)
        } else {
            exercisesWithSets.add(ShareExerciseItem(setWithEx.exercise, listOf(setWithEx)))
        }
    }

    val blocks = mutableListOf<ShareBlock>()
    var currentSupersetId: String? = null
    var currentBlock = mutableListOf<ShareExerciseItem>()

    exercisesWithSets.forEach { item ->
        val sid = item.sets.first().setLog.supersetId
        if (sid != null) {
            if (sid == currentSupersetId) {
                currentBlock.add(item)
            } else {
                if (currentBlock.isNotEmpty()) {
                    blocks.add(ShareBlock(currentSupersetId, currentBlock.toList()))
                    currentBlock = mutableListOf()
                }
                currentSupersetId = sid
                currentBlock.add(item)
            }
        } else {
            if (currentBlock.isNotEmpty()) {
                blocks.add(ShareBlock(currentSupersetId, currentBlock.toList()))
                currentBlock = mutableListOf()
            }
            currentSupersetId = null
            blocks.add(ShareBlock(null, listOf(item)))
        }
    }
    if (currentBlock.isNotEmpty()) {
        blocks.add(ShareBlock(currentSupersetId, currentBlock.toList()))
    }

    val unifiedItems = mutableListOf<UnifiedShareItem>()
    blocks.forEach { block ->
        val ord = block.exercises.firstOrNull()?.sets?.firstOrNull()?.setLog?.ordineEsercizio ?: 0
        unifiedItems.add(UnifiedShareItem.Strength(block, ord))
    }
    val cardioExercises = sessionDetails.cardio.groupedByExercise()
    cardioExercises.forEach { cardio ->
        unifiedItems.add(UnifiedShareItem.Cardio(cardio, cardio.order))
    }
    val sortedUnifiedItems = unifiedItems.sortedBy { it.order }

    val totalExercises = exercisesWithSets.size + cardioExercises.size
    val totalSets = sessionDetails.sets.size
    val totalWeight = sessionDetails.sets.sumOf { (it.setLog.pesoSollevato * it.setLog.repsEffettive).toDouble() }.toFloat()
    val durationMs = sessionDetails.session.durationMs

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .wrapContentHeight()
            .background(obsidianBackground)
            .drawBehind {
                // Top-right glowing radial primary gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.9f
                    ),
                    center = Offset(size.width, 0f),
                    radius = size.width * 0.9f
                )
                // Bottom-left glowing accent gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(0f, size.height),
                        radius = size.width * 0.7f
                    ),
                    center = Offset(0f, size.height),
                    radius = size.width * 0.7f
                )
            }
            .padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sleek App Logo & Brand Capsule
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.03f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), CircleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                painter = painterResource(id = com.emanuel5014.trainable.R.drawable.ic_app_logo),
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = com.emanuel5014.trainable.R.string.app_name).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = textPrimary,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Title and Date
        Text(
            text = planName.uppercase(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = textPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = DateFormatter.format(sessionDetails.session.timestamp).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = primaryColor,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Glassmorphic Workout Statistics Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(20.dp))
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statLabelColor = textSecondary.copy(alpha = 0.7f)

            // Exercises Count Segment
            ShareStat(
                value = "$totalExercises",
                label = stringResource(id = com.emanuel5014.trainable.R.string.share_exercises),
                valueColor = textPrimary,
                labelColor = statLabelColor
            )

            // Divider 1
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // Total Sets Segment
            ShareStat(
                value = "$totalSets",
                label = stringResource(id = com.emanuel5014.trainable.R.string.share_sets),
                valueColor = textPrimary,
                labelColor = statLabelColor
            )

            // Divider 2
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // Duration Segment
            if (durationMs != null) {
                val durHours = durationMs / 3600000
                val durMinutes = (durationMs % 3600000) / 60000
                ShareStat(
                    value = if (durHours > 0) "${durHours}h ${durMinutes}m" else "${durMinutes}m",
                    label = stringResource(id = com.emanuel5014.trainable.R.string.share_duration),
                    valueColor = textPrimary,
                    labelColor = statLabelColor
                )

                // Divider 3
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )
            }

            // Total Tonnage Segment
            ShareStat(
                value = WeightUnitConverter.formatWithUnit(
                    WeightUnitConverter.convertDisplay(totalWeight, weightUnit),
                    weightUnit
                ),
                label = stringResource(id = com.emanuel5014.trainable.R.string.share_volume),
                valueColor = textPrimary,
                labelColor = statLabelColor
            )
        }

        // Muscle Groups Section
        val trainedCategories = exercisesWithSets.map { it.exercise.categoria }.distinct().sorted()

        if (trainedCategories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trainedCategories.forEach { category ->
                    val translatedCategory = ExerciseTranslations.translateCategory(category, languageCode)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(primaryColor.copy(alpha = 0.12f))
                            .border(BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = translatedCategory.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Exercises Cards List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            sortedUnifiedItems.forEach { unifiedItem ->
                when (unifiedItem) {
                    is UnifiedShareItem.Strength -> {
                        val block = unifiedItem.block
                        val isSuperset = block.supersetId != null

                        if (isSuperset) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(cardBackground)
                                    .border(BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Link,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(id = com.emanuel5014.trainable.R.string.superset).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = primaryColor,
                                        letterSpacing = 1.sp
                                    )
                                }

                                block.exercises.forEachIndexed { exIndex, item ->
                                    if (exIndex > 0) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(Color.White.copy(alpha = 0.05f))
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    ShareExerciseHeader(
                                        name = ExerciseTranslations.translate(item.exercise.nome, languageCode),
                                        target = planExerciseMap?.get(item.exercise.id)?.let {
                                            shareTargetText(it, item.exercise.id, sessionDetails, showPrescription, shareLabels)
                                        },
                                        nameColor = primaryColor,
                                        targetColor = textSecondary
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    item.sets.forEach { setWithEx ->
                                        val set = setWithEx.setLog
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ShareSetPill(text = "SET ${set.numeroSerie}", color = textSecondary)

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = com.emanuel5014.trainable.ui.components.setValueText(set, weightUnit),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = textPrimary
                                            )
                                            val shareBadges = com.emanuel5014.trainable.ui.components.setLogDetailsText(shareContext, set, withPrescription = showPrescription)
                                            if (shareBadges != null) {
                                                Text(
                                                    text = "  $shareBadges",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = primaryColor,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (!set.note.isNullOrBlank()) {
                                            Text(
                                                text = set.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = textSecondary,
                                                fontStyle = FontStyle.Italic,
                                                modifier = Modifier.padding(start = 46.dp, top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            block.exercises.forEach { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(cardBackground)
                                        .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    ShareExerciseHeader(
                                        name = ExerciseTranslations.translate(item.exercise.nome, languageCode),
                                        target = planExerciseMap?.get(item.exercise.id)?.let {
                                            shareTargetText(it, item.exercise.id, sessionDetails, showPrescription, shareLabels)
                                        },
                                        nameColor = primaryColor,
                                        targetColor = textSecondary
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    item.sets.forEach { setWithEx ->
                                        val set = setWithEx.setLog
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ShareSetPill(text = "SET ${set.numeroSerie}", color = textSecondary)

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = com.emanuel5014.trainable.ui.components.setValueText(set, weightUnit),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = textPrimary
                                            )
                                            val shareBadges = com.emanuel5014.trainable.ui.components.setLogDetailsText(shareContext, set, withPrescription = showPrescription)
                                            if (shareBadges != null) {
                                                Text(
                                                    text = "  $shareBadges",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = primaryColor,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (!set.note.isNullOrBlank()) {
                                            Text(
                                                text = set.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = textSecondary,
                                                fontStyle = FontStyle.Italic,
                                                modifier = Modifier.padding(start = 46.dp, top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is UnifiedShareItem.Cardio -> {
                        val cardio = unifiedItem.cardio.first
                        val cardioSets = unifiedItem.cardio.sets
                        val cardioTitle = ExerciseTranslations.translate(cardio.categoria, languageCode)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBackground)
                                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = cardioTitle.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black,
                                color = primaryColor,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            if (cardioSets.size > 1) cardioSets.forEachIndexed { index, set ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ShareSetPill(text = "SET ${index + 1}", color = textSecondary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = com.emanuel5014.trainable.ui.components.cardioSetValueText(set),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textPrimary
                                    )
                                }
                                if (!set.note.isNullOrBlank()) {
                                    Text(text = set.note, style = MaterialTheme.typography.bodySmall, color = textSecondary, modifier = Modifier.padding(start = 46.dp))
                                }
                            } else Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${cardio.distanza} KM",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textPrimary
                                )
                                val h = cardio.durataSecondi / 3600
                                val m = (cardio.durataSecondi % 3600) / 60
                                val s = cardio.durataSecondi % 60
                                val durationText = if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Divider & Premium Footer Brand Statement
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.05f))
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(id = com.emanuel5014.trainable.R.string.share_footer_message).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = textSecondary.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold
        )
    }
}

fun captureViewToBitmap(view: View): Bitmap {
    return view.drawToBitmap()
}

/** The most of the row a target may take beside the exercise name; a longer one goes on its own line. */
private const val TARGET_BESIDE_NAME_MAX_SHARE = 0.45f

/**
 * An exercise's name with its target on the right ("PANCA PIANA      3×8"). A target too wide to sit beside the name,
 * like a week of %1RM or bodyweight blocks, goes on its own line under it: beside the name it used to take the whole
 * row and leave the name a letter per line.
 */
@Composable
internal fun ShareExerciseHeader(
    name: String,
    target: String?,
    nameColor: Color,
    targetColor: Color,
    modifier: Modifier = Modifier
) {
    Layout(
        modifier = modifier,
        content = {
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black,
                color = nameColor,
                letterSpacing = 0.5.sp
            )
            if (target != null) {
                Text(
                    text = target,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = targetColor
                )
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val nameMeasurable = measurables[0]
        val targetMeasurable = measurables.getOrNull(1)
        if (targetMeasurable == null) {
            val name = nameMeasurable.measure(loose)
            return@Layout layout(width, name.height) { name.placeRelative(0, 0) }
        }

        val gap = 8.dp.roundToPx()
        val targetWidth = targetMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        if (targetWidth + gap <= width * TARGET_BESIDE_NAME_MAX_SHARE) {
            val target = targetMeasurable.measure(loose.copy(maxWidth = targetWidth))
            val name = nameMeasurable.measure(loose.copy(maxWidth = width - target.width - gap))
            val height = maxOf(name.height, target.height)
            layout(width, height) {
                name.placeRelative(0, (height - name.height) / 2)
                target.placeRelative(width - target.width, (height - target.height) / 2)
            }
        } else {
            val name = nameMeasurable.measure(loose)
            val target = targetMeasurable.measure(loose)
            val spacing = 2.dp.roundToPx()
            layout(width, name.height + spacing + target.height) {
                name.placeRelative(0, 0)
                target.placeRelative(0, name.height + spacing)
            }
        }
    }
}

/** What the routine asked of an exercise: that week's blocks (as changed during the workout, if they were) or "3×8". */
@Composable
private fun shareTargetText(
    planExercise: PlanExerciseWithDetails,
    exerciseId: Int,
    sessionDetails: SessionWithDetails,
    showPrescription: Boolean,
    labels: com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
): String {
    val isTime = planExercise.planExercise.exerciseType == "time_and_weight"
    // A prescription changed during the workout is what the exercise was done against
    val editedBlocks = if (showPrescription) sessionDetails.prescriptionEdits.finalBlocks(exerciseId) else null
    val resolved = if (showPrescription) planExercise.resolve(sessionDetails.session.programWeek ?: 1) else com.emanuel5014.trainable.domain.prescription.ResolvedPrescription.Legacy
    return if (editedBlocks != null) {
        editedBlocks.joinToString(" · ") { com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter.headline(it, labels) } + " ✎"
    } else if (resolved is com.emanuel5014.trainable.domain.prescription.ResolvedPrescription.Blocks) {
        resolved.blocks.joinToString(" · ") { com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter.headline(it, labels) }
    } else {
        val targetDisplay = if (planExercise.planExercise.exerciseType == TIME_AND_REPS) {
            stringResource(
                com.emanuel5014.trainable.R.string.time_reps_value,
                planExercise.planExercise.durataTargetSecondi ?: TimeAndReps.DEFAULT_SECONDS,
                TimeAndReps.goalOf(planExercise.planExercise.repsTarget)
            )
        } else if (isTime) {
            planExercise.planExercise.durataTargetSecondi?.let { "${it}s" } ?: planExercise.planExercise.repsTarget
        } else planExercise.planExercise.repsDisplay
        "${planExercise.planExercise.serieTarget}×$targetDisplay"
    }
}

/**
 * One figure of the stats bar. With the duration there are four columns, and a value like "22070 kg" or the label
 * "TONNELLAGGIO" shrinks to fit its column instead of wrapping.
 */
@Composable
internal fun RowScope.ShareStat(value: String, label: String, valueColor: Color, labelColor: Color) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val valueStyle = MaterialTheme.typography.titleLarge
        Text(
            text = value,
            style = valueStyle,
            fontWeight = FontWeight.Black,
            color = valueColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = valueStyle.fontSize)
        )
        Spacer(modifier = Modifier.height(2.dp))
        val labelStyle = MaterialTheme.typography.labelSmall
        Text(
            text = label.uppercase(),
            style = labelStyle,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            // About 1sp at full size, and it shrinks with the text (a fixed 1sp alone overflowed the column)
            letterSpacing = 0.09.em,
            textAlign = TextAlign.Center,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = labelStyle.fontSize)
        )
    }
}

/** The "SET 3" tag in front of a set. Its fixed width keeps the values in a column, so "SET 10" shrinks to fit it. */
@Composable
internal fun ShareSetPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .width(38.dp)
            .heightIn(min = 18.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        val style = MaterialTheme.typography.labelSmall
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = style.fontSize)
        )
    }
}

private data class ShareExerciseItem(
    val exercise: ExerciseEntity,
    val sets: List<SetWithExercise>
)

private data class ShareBlock(
    val supersetId: String?,
    val exercises: List<ShareExerciseItem>
)

private sealed interface UnifiedShareItem {
    val order: Int
    data class Strength(val block: ShareBlock, override val order: Int) : UnifiedShareItem
    data class Cardio(val cardio: com.emanuel5014.trainable.data.local.entity.CardioExerciseLogs, override val order: Int) : UnifiedShareItem
}
