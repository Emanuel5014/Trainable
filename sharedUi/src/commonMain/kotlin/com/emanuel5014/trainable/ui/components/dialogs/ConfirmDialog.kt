package com.emanuel5014.trainable.ui.components.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.delete
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import org.jetbrains.compose.resources.stringResource

/**
 * Asks before doing something: [title] and [message] say what, [confirmLabel] does it, Cancel does nothing.
 * [destructive] paints the button in the error colour (deleting, losing something).
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = { Text(title, fontWeight = FontWeight.ExtraBold, color = OnSurface) },
        text = { Text(message, color = OnSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel.uppercase(),
                    color = if (destructive) Error else Primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel).uppercase(), color = OnSurfaceVariant)
            }
        }
    )
}

/** [ConfirmDialog] for a deletion: the button says Delete and is red. */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(Res.string.delete)
) = ConfirmDialog(title, message, confirmLabel, onConfirm, onDismiss, destructive = true)
