package com.emanuel5014.trainable.ui.components

import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.drawToBitmap
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.util.WeightUnitConverter

@Composable
fun WorkoutShareCard(
    sessionDetails: SessionWithDetails,
    planName: String,
    languageCode: String,
    weightUnit: String,
    modifier: Modifier = Modifier
) {
    // Capture the @Composable colors here, in a Composable context
    val primaryColor = Primary
    val surfaceColor = Surface
    val onSurfaceColor = OnSurface
    val onSurfaceVariantColor = OnSurfaceVariant

    Column(
        modifier = modifier
            .width(400.dp)
            .wrapContentHeight()
            .background(surfaceColor)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.8f
                    ),
                    center = Offset(size.width, 0f),
                    radius = size.width * 0.8f
                )
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: App Logo and Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = com.emanuel5014.trainable.R.drawable.ic_app_logo),
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TRAINABLE",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = primaryColor
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Workout Info
        Text(
            text = planName.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = onSurfaceColor,
            lineHeight = 32.sp
        )
        Text(
            text = DateFormatter.format(sessionDetails.session.timestamp).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = primaryColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Exercise List
        val groupedSets = sessionDetails.sets.groupBy { it.exercise.id }
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            groupedSets.forEach { (_, sets) ->
                val exerciseName = ExerciseTranslations.translate(sets.first().exercise.nome, languageCode)
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = exerciseName.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor,
                        letterSpacing = 0.5.sp
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Show all sets for this exercise
                    sets.forEach { setWithEx ->
                        val set = setWithEx.setLog
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SET ${set.numeroSerie}",
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurfaceVariantColor,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = WeightUnitConverter.formatWithUnit(
                                    WeightUnitConverter.convertDisplay(set.pesoSollevato, weightUnit),
                                    weightUnit
                                ) + " × ${set.repsEffettive}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = onSurfaceColor
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        // Footer
        Text(
            text = "TRACK YOUR PROGRESS WITH TRAINABLE",
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceVariantColor.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

fun captureViewToBitmap(view: View): Bitmap {
    return view.drawToBitmap()
}
