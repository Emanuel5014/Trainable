package com.emanuel5014.trainable.ui.screens.history

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.animateFloatAsState
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.emanuel5014.trainable.ui.theme.actionGlass
import com.emanuel5014.trainable.ui.theme.actionGlassContainer
import com.emanuel5014.trainable.ui.theme.actionGlassSource
import com.emanuel5014.trainable.ui.theme.rememberActionGlassSource
import com.emanuel5014.trainable.data.local.entity.encodePerformed
import com.emanuel5014.trainable.data.local.entity.parts
import com.emanuel5014.trainable.data.local.entity.pauses
import com.emanuel5014.trainable.data.local.entity.performed
import com.emanuel5014.trainable.data.local.entity.isInParts
import com.emanuel5014.trainable.domain.prescription.SetPerformance
import com.emanuel5014.trainable.data.local.entity.CardioExerciseLogs
import com.emanuel5014.trainable.data.local.entity.SessionPrescriptionEditEntity
import com.emanuel5014.trainable.data.local.entity.forRun
import com.emanuel5014.trainable.data.local.entity.groupedByExercise
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.TIME_AND_WEIGHT
import com.emanuel5014.trainable.data.local.entity.asKind
import com.emanuel5014.trainable.data.local.entity.kind
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.SessionTagEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.relation.toJournal
import com.emanuel5014.trainable.ui.components.journal.JournalDetail
import com.emanuel5014.trainable.ui.components.journal.JournalSheet
import com.emanuel5014.trainable.ui.components.journal.JournalSummaryLine
import com.emanuel5014.trainable.ui.components.journal.journalHasContent
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.entity.isArchived
import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.dataStore
import com.emanuel5014.trainable.ui.components.AddCardioDialog
import com.emanuel5014.trainable.data.model.NavBarStyle
import com.emanuel5014.trainable.ui.components.BottomBarManager
import com.emanuel5014.trainable.ui.components.DeletedExercisePill
import com.emanuel5014.trainable.ui.components.EmptyState
import com.emanuel5014.trainable.ui.components.RestoreExerciseDialog
import com.emanuel5014.trainable.ui.components.ExerciseTypeSelector
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymCard
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.LocalNavBarStyle
import com.emanuel5014.trainable.ui.components.NavBarAction
import com.emanuel5014.trainable.ui.components.NavBarActionEffect
import com.emanuel5014.trainable.ui.components.NavBarMenuItem
import com.emanuel5014.trainable.ui.components.NavBarPage
import com.emanuel5014.trainable.ui.components.navBarBottomClearance
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.components.WorkoutShareCard
import com.emanuel5014.trainable.ui.components.captureViewToBitmap
import com.emanuel5014.trainable.ui.navigation.EditWorkoutSession
import com.emanuel5014.trainable.ui.navigation.CompareSessions
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.SurfaceContainerLow
import com.emanuel5014.trainable.ui.util.DateFormatter
import com.emanuel5014.trainable.util.ShareUtils
import com.emanuel5014.trainable.util.UriMigrationHelper
import com.emanuel5014.trainable.util.WeightUnitConverter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Sharing a workout of the shared history as an image: the card is drawn off screen into a bitmap, previewed, then
 * saved to the gallery or shared (ShareUtils).
 */
@Composable
fun rememberAndroidHistoryShareUi(): HistoryShareUi {
    val context = LocalContext.current
    var sessionToShare by remember { mutableStateOf<Triple<SessionWithDetails, String, List<PlanExerciseWithDetails>?>?>(null) }
    var capturedBitmapToPreview by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    DisposableEffect(capturedBitmapToPreview) {
        if (capturedBitmapToPreview != null) {
            BottomBarManager.isVisibleOverride = false
            BottomBarManager.swipeLocked = true
        }
        onDispose {
            BottomBarManager.isVisibleOverride = true
            BottomBarManager.swipeLocked = false
        }
    }

    BackHandler(capturedBitmapToPreview != null) { capturedBitmapToPreview = null }

    return object : HistoryShareUi {
        override val available: Boolean get() = true

        override fun share(session: SessionWithDetails, planName: String, planExercises: List<PlanExerciseWithDetails>?) {
            sessionToShare = Triple(session, planName, planExercises)
        }

        @Composable
        override fun Overlay(languageCode: String, weightUnit: String, showPrescription: Boolean, exercises: Map<Int, ExerciseEntity>) {
        if (sessionToShare != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {}
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GymLoadingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.preparing_share),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Box(
                        modifier = Modifier
                            .alpha(0f)
                            .zIndex(-100f)
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                ComposeView(ctx).apply {
                                    setContent {
                                        WorkoutShareCard(
                                            sessionDetails = sessionToShare!!.first,
                                            planName = sessionToShare!!.second,
                                            languageCode = languageCode,
                                            weightUnit = weightUnit,
                                            planExercises = sessionToShare!!.third,
                                            showPrescription = showPrescription,
                                            exercisesById = exercises
                                        )
                                    }
                                }
                            },
                            update = { view ->
                                view.post {
                                    val bitmap = captureViewToBitmap(view)
                                    capturedBitmapToPreview = bitmap
                                    sessionToShare = null
                                }
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = capturedBitmapToPreview != null,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.90f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.90f, animationSpec = tween(200)),
            modifier = Modifier.zIndex(200f)
        ) {
            val imageSavedMessage = stringResource(R.string.image_saved_gallery)
            val failedSaveMessage = stringResource(R.string.failed_save_image)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.76f))
                    .clickable { capturedBitmapToPreview = null },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .padding(ResponsiveSize.cardPadding)
                        .fillMaxWidth(0.92f)
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier.padding(ResponsiveSize.cardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.share_preview),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineSmall.fontSize)
                                    ),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.5).sp
                                )
                            }
                            IconButton(
                                onClick = { capturedBitmapToPreview = null }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.close),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(when {
                            ResponsiveSize.screenWidthDp < 360 -> 12.dp
                            ResponsiveSize.screenWidthDp < 400 -> 16.dp
                            else -> 18.dp
                        }))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                        ) {
                            if (capturedBitmapToPreview != null) {
                                Image(
                                    bitmap = capturedBitmapToPreview!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(when {
                            ResponsiveSize.screenWidthDp < 360 -> 14.dp
                            ResponsiveSize.screenWidthDp < 400 -> 18.dp
                            else -> 20.dp
                        }))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val success = ShareUtils.saveBitmapToGallery(context, capturedBitmapToPreview!!, "Workout")
                                    val message = if (success) imageSavedMessage else failedSaveMessage
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = stringResource(R.string.save),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    ShareUtils.shareBitmap(context, capturedBitmapToPreview!!)
                                    capturedBitmapToPreview = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = stringResource(R.string.share),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
