package com.emanuel5014.trainable.ui.components.deload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.deload_badge
import com.emanuel5014.trainable.ui.theme.OnSecondaryContainer
import com.emanuel5014.trainable.ui.theme.SecondaryContainer
import org.jetbrains.compose.resources.stringResource
import com.emanuel5014.trainable.ui.util.uppercaseUi

/**
 * "DELOAD": marks a deload workout (see DeloadRules) in the workout bar, the history and the exercise detail. The
 * neutral secondary container, never the teal of "done" or the red of "delete".
 */
@Composable
fun DeloadBadge(modifier: Modifier = Modifier, showIcon: Boolean = true) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(SecondaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showIcon) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                contentDescription = null,
                tint = OnSecondaryContainer,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = stringResource(Res.string.deload_badge).uppercaseUi(),
            style = MaterialTheme.typography.labelSmall,
            color = OnSecondaryContainer,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}
