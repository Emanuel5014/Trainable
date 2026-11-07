package com.emanuel5014.trainable.ui.components.shared

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.emanuel5014.trainable.data.ai.backend.ConsentRequest
import com.emanuel5014.trainable.data.ai.remote.GeminiEndpoint
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.ai_consent_body
import com.emanuel5014.trainable.sharedui.resources.ai_consent_continue
import com.emanuel5014.trainable.sharedui.resources.ai_consent_gemini_note
import com.emanuel5014.trainable.sharedui.resources.ai_consent_title
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import org.jetbrains.compose.resources.stringResource

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
                stringResource(Res.string.ai_consent_title),
                fontWeight = FontWeight.ExtraBold,
                color = OnSurface
            )
        },
        text = {
            val body = stringResource(Res.string.ai_consent_body, request.host)
            Text(
                if (request.host.equals(GeminiEndpoint.HOST, ignoreCase = true)) {
                    "$body\n\n${stringResource(Res.string.ai_consent_gemini_note)}"
                } else {
                    body
                },
                color = OnSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(
                    stringResource(Res.string.ai_consent_continue).uppercase(),
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(stringResource(Res.string.cancel).uppercase(), color = Primary)
            }
        }
    )
}
