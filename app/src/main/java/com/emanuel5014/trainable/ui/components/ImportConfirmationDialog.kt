package com.emanuel5014.trainable.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.remote.dto.WorkoutPlanExportDto
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

@Composable
fun ImportConfirmationDialog(
    plans: List<WorkoutPlanExportDto>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_plans_title)) },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.import_plans_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
                plans.forEach { plan ->
                    Text(
                        text = "• " + stringResource(
                            R.string.import_plan_summary,
                            plan.nome,
                            plan.exercises.size
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnSurface
                    )
                }
            }
        },
        confirmButton = {
            GymButton(
                onClick = onConfirm,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
            ) {
                Text(stringResource(R.string.import_confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            GymButton(
                onClick = onDismiss,
                containerColor = Color.Transparent,
                contentColor = OnSurfaceVariant,
                modifier = Modifier.height(48.dp)
            ) {
                Text(stringResource(R.string.cancel).uppercase())
            }
        },
        containerColor = SurfaceContainerHigh,
        titleContentColor = OnSurface,
        textContentColor = OnSurfaceVariant
    )
}
