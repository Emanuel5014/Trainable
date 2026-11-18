package com.emanuel5014.trainable.ui.components.heart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.close
import com.emanuel5014.trainable.sharedui.resources.heart_privacy_text
import com.emanuel5014.trainable.sharedui.resources.heart_privacy_title
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.Surface
import org.jetbrains.compose.resources.stringResource

/** What Trainable does with health data: the page Health Connect opens from its permission screen. */
@Composable
fun HealthPrivacyContent(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Rounded.MonitorHeart, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
        Text(
            stringResource(Res.string.heart_privacy_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = OnSurface
        )
        Text(stringResource(Res.string.heart_privacy_text), style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant)
        TextButton(onClick = onClose) { Text(stringResource(Res.string.close), fontWeight = FontWeight.Bold) }
    }
}
