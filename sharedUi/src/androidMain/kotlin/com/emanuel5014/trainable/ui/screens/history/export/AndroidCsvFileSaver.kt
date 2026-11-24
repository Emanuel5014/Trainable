package com.emanuel5014.trainable.ui.screens.history.export

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.export_failed
import com.emanuel5014.trainable.sharedui.resources.export_ready
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

/**
 * Android's [CsvFileSaver]: the system file picker asks where (the name is only a suggestion), then the CSV is built and
 * written there as UTF-8 without a BOM, as before.
 */
@Composable
fun rememberAndroidCsvFileSaver(): CsvFileSaver {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showMessage by rememberUpdatedState(LocalShowMessage.current)
    // What the picker that is open will save; the picker's answer comes back to the launcher below
    val pending = remember { arrayOfNulls<Pair<suspend () -> String, () -> Unit>>(1) }

    fun write(uri: Uri, csv: suspend () -> String, onSaved: () -> Unit) {
        scope.launch {
            try {
                val text = csv()
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("No output stream for $uri")
                    output.use { it.write(text.toByteArray()) }
                }
                showMessage(getString(Res.string.export_ready))
                onSaved()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                showMessage(getString(Res.string.export_failed))
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val job = pending[0]
        pending[0] = null
        if (uri != null && job != null) write(uri, job.first, job.second)
    }
    return remember(launcher) {
        CsvFileSaver { fileName, csv, onSaved ->
            pending[0] = csv to onSaved
            launcher.launch(fileName)
        }
    }
}
