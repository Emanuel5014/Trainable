package com.emanuel5014.trainable.ui.components

import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
) {
    val primaryColor = Primary
    
    val planExerciseMap = planExercises?.associateBy { it.planExercise.exerciseId }

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

    val totalExercises = exercisesWithSets.size + sessionDetails.cardio.size
    val totalSets = sessionDetails.sets.size
    val totalWeight = sessionDetails.sets.sumOf { (it.setLog.pesoSollevato * it.setLog.repsEffettive).toDouble() }.toFloat()
    val durationMs = sessionDetails.session.durationMs

    Column(
        modifier = modifier
            .width(420.dp)
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
            // Exercises Count Segment
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$totalExercises",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(id = com.emanuel5014.trainable.R.string.share_exercises).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
            }

            // Divider 1
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // Total Sets Segment
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$totalSets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(id = com.emanuel5014.trainable.R.string.share_sets).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
            }

            // Divider 2
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // Duration Segment
            if (durationMs != null) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val durHours = durationMs / 3600000
                    val durMinutes = (durationMs % 3600000) / 60000
                    Text(
                        text = if (durHours > 0) "${durHours}h ${durMinutes}m" else "${durMinutes}m",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(id = com.emanuel5014.trainable.R.string.share_duration).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                }

                // Divider 3
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )
            }

            // Total Tonnage Segment
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = WeightUnitConverter.formatWithUnit(
                        WeightUnitConverter.convertDisplay(totalWeight, weightUnit),
                        weightUnit
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(id = com.emanuel5014.trainable.R.string.share_volume).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
            }
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
            blocks.forEach { block ->
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

                            val exerciseName = ExerciseTranslations.translate(item.exercise.nome, languageCode)
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = exerciseName.uppercase(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = primaryColor,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                val planExercise = planExerciseMap?.get(item.exercise.id)
                                if (planExercise != null) {
                                    Text(
                                        text = "${planExercise.planExercise.serieTarget}×${planExercise.planExercise.repsTarget}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary
                                    )
                                }
                            }

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
                                    Box(
                                        modifier = Modifier
                                            .size(width = 38.dp, height = 18.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.05f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "SET ${set.numeroSerie}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = WeightUnitConverter.formatWithUnit(
                                            WeightUnitConverter.convertDisplay(set.pesoSollevato, weightUnit),
                                            weightUnit
                                        ) + " × ${set.repsEffettive}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textPrimary
                                    )
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
                        val exerciseName = ExerciseTranslations.translate(item.exercise.nome, languageCode)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBackground)
                                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = exerciseName.uppercase(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = primaryColor,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                val planExercise = planExerciseMap?.get(item.exercise.id)
                                if (planExercise != null) {
                                    Text(
                                        text = "${planExercise.planExercise.serieTarget}×${planExercise.planExercise.repsTarget}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary
                                    )
                                }
                            }

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
                                    Box(
                                        modifier = Modifier
                                            .size(width = 38.dp, height = 18.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.05f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "SET ${set.numeroSerie}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = WeightUnitConverter.formatWithUnit(
                                            WeightUnitConverter.convertDisplay(set.pesoSollevato, weightUnit),
                                            weightUnit
                                        ) + " × ${set.repsEffettive}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textPrimary
                                    )
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

            sessionDetails.cardio.forEach { cardio ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBackground)
                        .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = cardio.categoria.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = primaryColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
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

private data class ShareExerciseItem(
    val exercise: ExerciseEntity,
    val sets: List<SetWithExercise>
)

private data class ShareBlock(
    val supersetId: String?,
    val exercises: List<ShareExerciseItem>
)
