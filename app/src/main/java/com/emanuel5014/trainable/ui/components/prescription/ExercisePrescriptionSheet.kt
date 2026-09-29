package com.emanuel5014.trainable.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh

/**
 * Edits the advanced prescription of one exercise of an already recorded session.
 * [initialBlocks] is what the exercise currently has (or a block seeded from its plain sets).
 * [onRemove] is offered when the exercise is already advanced and turns it back into plain sets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePrescriptionSheet(
    exerciseId: Int,
    exerciseName: String,
    initialBlocks: List<PrescriptionBlock>,
    onDismiss: () -> Unit,
    onSave: (List<PrescriptionBlock>) -> Unit,
    onRemove: (() -> Unit)?
) {
    val context = LocalContext.current
    var blocksByWeek by remember { mutableStateOf(mapOf(1 to initialBlocks)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface,
        contentColor = OnSurface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ResponsiveSize.cardPadding)
                .padding(top = Spacing.small, bottom = ResponsiveSize.cardPadding)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.large)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)) {
                Text(
                    text = stringResource(R.string.prescription).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineMedium.fontSize)
                    ),
                    color = OnSurface,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = stringResource(R.string.prescription_sheet_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }

            AdvancedPrescriptionEditor(
                blocksByWeek = blocksByWeek,
                onBlocksByWeekChange = { blocksByWeek = it },
                exerciseId = exerciseId,
                exerciseName = exerciseName
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                if (onRemove != null) {
                    GymButton(
                        onClick = onRemove,
                        modifier = Modifier.size(60.dp),
                        height = 56,
                        containerColor = Error.copy(alpha = 0.15f),
                        contentColor = Error,
                        shape = CircleShape,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.remove_prescription), modifier = Modifier.size(28.dp))
                    }
                }
                GymButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    containerColor = SurfaceContainerHigh,
                    contentColor = OnSurfaceVariant
                ) {
                    Text(stringResource(R.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
                GymButton(
                    onClick = {
                        val blocks = blocksByWeek[1].orEmpty()
                        if (blocks.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.advanced_needs_block), Toast.LENGTH_SHORT).show()
                        } else {
                            onSave(blocks)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.save).uppercase(), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
