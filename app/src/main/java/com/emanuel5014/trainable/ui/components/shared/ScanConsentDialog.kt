package com.emanuel5014.trainable.ui.components.shared

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ai.backend.ConsentRequest
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/** Asks, once per provider, whether a photo of a sheet may leave the phone for [ConsentRequest.host]. */
@Composable
fun ScanConsentDialog(
    request: ConsentRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = SurfaceContainerHigh,
        title = {
            Text(
                stringResource(R.string.ai_consent_title),
                fontWeight = FontWeight.ExtraBold,
                color = OnSurface
            )
        },
        text = {
            Text(
                stringResource(R.string.ai_consent_body, request.host),
                color = OnSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(
                    stringResource(R.string.ai_consent_continue).uppercase(),
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(stringResource(R.string.cancel).uppercase(), color = Primary)
            }
        }
    )
}
