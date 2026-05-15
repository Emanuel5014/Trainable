package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Spacing

@Composable
fun ExerciseGroupHeader(
    groupName: String,
    exerciseCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored Vertical Bar
        Box(
            modifier = Modifier
                .height(24.dp)
                .width(4.dp)
                .clip(RoundedCornerShape(size = 2.dp))
                .background(Primary)
        )
        Spacer(modifier = Modifier.width(Spacing.medium))
        
        Text(
            text = groupName.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.width(Spacing.small))
        Text(
            text = "• $exerciseCount exercises",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurface.copy(alpha = 0.6f)
        )
    }
}
