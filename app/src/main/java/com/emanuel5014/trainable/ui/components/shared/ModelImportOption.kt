package com.emanuel5014.trainable.ui.components.shared

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.emanuel5014.trainable.ui.components.glass.GlassTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ai.ModelImportError
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary

/**
 * "Import a model you already have": opens the file picker and hands the chosen file to [onPicked]. Under the
 * button it says that the file is copied (so it takes space twice) and, when the last import was refused, why.
 */
@Composable
fun ModelImportOption(
    error: ModelImportError?,
    onPicked: (Uri) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
    centered: Boolean = false
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPicked(uri)
    }
    val alignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TextButton(
            onClick = {
                onClearError()
                // The picker cannot filter on the .litertlm extension, so every file is offered and checked afterwards
                picker.launch(arrayOf("*/*"))
            }
        ) {
            Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.ai_model_import),
                color = Primary,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            text = stringResource(R.string.ai_model_import_note),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Text(
                text = error.message(),
                style = MaterialTheme.typography.bodySmall,
                color = Error,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ModelImportError.message(): String = when (this) {
    ModelImportError.NotLiteRtLm -> stringResource(R.string.ai_import_error_format)
    ModelImportError.UnsupportedModel -> stringResource(R.string.ai_import_error_unsupported)
    is ModelImportError.NotEnoughSpace ->
        stringResource(R.string.ai_import_error_space, Formatter.formatFileSize(LocalContext.current, neededBytes))
    ModelImportError.CannotRead -> stringResource(R.string.ai_import_error_read)
    ModelImportError.Busy -> stringResource(R.string.ai_import_error_busy)
}
