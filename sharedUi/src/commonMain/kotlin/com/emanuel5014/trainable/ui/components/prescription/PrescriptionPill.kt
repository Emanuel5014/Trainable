package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/** Small rounded pill, the shared visual unit for prescriptions (same family as the WARM UP chip). */
@Composable
fun PrescriptionPill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceContainerHigh,
    contentColor: Color = OnSurfaceVariant,
    emphasized: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (emphasized) FontWeight.Black else FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            // Long ones (a custom technique as done) wrap instead of losing their end
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
