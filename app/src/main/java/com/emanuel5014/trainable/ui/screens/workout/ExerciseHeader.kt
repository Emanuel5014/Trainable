package com.emanuel5014.trainable.ui.screens.workout

import com.emanuel5014.trainable.ui.theme.LocalLiquidGlassEnabled
import com.emanuel5014.trainable.ui.theme.liquidGlassSurface
import com.emanuel5014.trainable.ui.theme.glassControlContentColor

import androidx.compose.animation.animateContentSize
import com.emanuel5014.trainable.ui.components.motion.WorkoutMotion
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.rounded.Info
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.emanuel5014.trainable.ui.components.glass.GlassSurface as Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.domain.prescription.IntensityType
import com.emanuel5014.trainable.domain.prescription.PrescriptionExpander
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.ui.components.ExerciseMediaSlot
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.components.ProgramEditPill
import com.emanuel5014.trainable.ui.components.ProgressionSuggestionCard
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSecondaryContainer
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SecondaryContainer
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.Tertiary
import kotlin.math.abs
import kotlin.math.roundToInt

private val COMPACT_HEIGHT = 56.dp
private const val EXPANDED_ID = "expanded"
private const val COMPACT_ID = "compact"

/**
 * What sits above the sets of a strength or timed exercise. Expanded: the pills of what the exercise prescribes (and
 * the superset and EMOM badges), the tools of the exercise, its name with the media thumbnail and the progression
 * card. Once the sets are scrolled it collapses ([collapse]) to one row: name, week and tools.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ExerciseHeader(
    targetExState: WorkoutExerciseState,
    targetIndex: Int,
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    viewModel: WorkoutViewModel,
    languageCode: String,
    exerciseMedia: Map<Int, String>,
    largeMediaVisible: Boolean,
    pickMedia: (Int) -> Unit,
    prescriptionLabels: PrescriptionLabels,
    onOpenDetail: (Int) -> Unit,
    collapse: HeaderCollapseState
) {
    // Both are composed only while they can be seen: the tools of the one that is gone cannot be tapped
    val showExpanded by remember(collapse) { derivedStateOf { collapse.fraction < 0.99f } }
    val showCompact by remember(collapse) { derivedStateOf { collapse.fraction > 0.01f } }
    Layout(
        modifier = Modifier.fillMaxWidth().clipToBounds(),
        content = {
            if (showExpanded) {
                Box(Modifier.layoutId(EXPANDED_ID).graphicsLayer { alpha = HeaderCollapse.expandedAlpha(collapse.fraction) }) {
                    ExpandedHeader(
                        targetExState, targetIndex, state, ui, viewModel, languageCode, exerciseMedia,
                        largeMediaVisible, pickMedia, prescriptionLabels, onOpenDetail
                    )
                }
            }
            if (showCompact) {
                Box(Modifier.layoutId(COMPACT_ID).graphicsLayer { alpha = HeaderCollapse.compactAlpha(collapse.fraction) }) {
                    CompactHeader(targetExState, targetIndex, state, ui, viewModel, languageCode, onOpenDetail)
                }
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val compactPx = COMPACT_HEIGHT.roundToPx()
        val expanded = measurables.firstOrNull { it.layoutId == EXPANDED_ID }
            ?.measure(Constraints(minWidth = width, maxWidth = width, minHeight = 0, maxHeight = Constraints.Infinity))
        if (expanded != null) {
            val travel = (expanded.height - compactPx).coerceAtLeast(0).toFloat()
            if (abs(collapse.travelPx - travel) > 0.5f) collapse.updateTravel(travel)
        }
        val compact = measurables.firstOrNull { it.layoutId == COMPACT_ID }?.measure(Constraints.fixed(width, compactPx))
        val expandedHeight = expanded?.height ?: (compactPx + collapse.travelPx.roundToInt())
        val height = lerp(expandedHeight, compactPx, collapse.fraction)
        layout(width, height) {
            expanded?.placeRelative(0, 0)
            compact?.placeRelative(0, 0)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpandedHeader(
    targetExState: WorkoutExerciseState,
    targetIndex: Int,
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    viewModel: WorkoutViewModel,
    languageCode: String,
    exerciseMedia: Map<Int, String>,
    largeMediaVisible: Boolean,
    pickMedia: (Int) -> Unit,
    prescriptionLabels: PrescriptionLabels,
    onOpenDetail: (Int) -> Unit
) {
    val toolsOnOwnRow = HeaderLayout.toolsOnOwnRow(
        widthDp = LocalConfiguration.current.screenWidthDp.toFloat(),
        fontScale = LocalDensity.current.fontScale
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (toolsOnOwnRow) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ExerciseTools(targetExState, targetIndex, state, ui, viewModel, onOpenDetail)
                }
                Spacer(modifier = Modifier.height(8.dp))
                HeaderPills(targetExState, targetIndex, state, ui, prescriptionLabels, Modifier.fillMaxWidth())
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    HeaderPills(targetExState, targetIndex, state, ui, prescriptionLabels, Modifier.weight(1f))
                    ExerciseTools(targetExState, targetIndex, state, ui, viewModel, onOpenDetail)
                }
            }
            val exerciseDisplayName = ExerciseTranslations.translate(targetExState.exercise.nome, languageCode)
            Spacer(modifier = Modifier.height(8.dp))
            if (state.exerciseMediaEnabled) {
                // The thumbnail takes width from the name, so the name steps down one size
                // to wrap onto the same number of lines it did at full width.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = exerciseDisplayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    ExerciseMediaSlot(
                        fileName = exerciseMedia[targetExState.exercise.id],
                        onOpen = { ui.mediaViewer = targetExState.exercise.id to exerciseDisplayName },
                        onAdd = { pickMedia(targetExState.exercise.id) },
                        playing = !largeMediaVisible
                    )
                }
            } else {
                Text(
                    text = exerciseDisplayName,
                    style = MaterialTheme.typography.displaySmall,
                    color = OnSurface,
                    fontWeight = FontWeight.Black
                )
            }
        }

        targetExState.progressionToShow?.let { suggestion ->
            ProgressionSuggestionCard(
                suggestion = suggestion,
                weightUnit = state.weightUnit,
                onApply = { viewModel.applyProgression(targetIndex) },
                onDismiss = { viewModel.dismissProgression(targetIndex) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            )
        }
    }
}

/** One row of the name, the week and the tools: the header once the sets have been scrolled. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CompactHeader(
    targetExState: WorkoutExerciseState,
    targetIndex: Int,
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    viewModel: WorkoutViewModel,
    languageCode: String,
    onOpenDetail: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(COMPACT_HEIGHT)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = ExerciseTranslations.translate(targetExState.exercise.nome, languageCode),
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (targetExState.isAdvanced) {
            state.programWeek?.let { week ->
                PrescriptionPill(
                    text = stringResource(R.string.week_short, week),
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    emphasized = true
                )
            }
        }
        ExerciseTools(targetExState, targetIndex, state, ui, viewModel, onOpenDetail)
    }
}

/**
 * What the exercise prescribes, as pills: the week and the 1RM it is loaded from (an advanced exercise), or sets by
 * reps, then the story of a changed program, and the superset and EMOM badges.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderPills(
    targetExState: WorkoutExerciseState,
    targetIndex: Int,
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    prescriptionLabels: PrescriptionLabels,
    modifier: Modifier
) {
    // An advanced exercise reads what the current week prescribes: the plan's own
    // sets and reps targets are only kept in step with the first week
    val weekTargets = if (targetExState.isAdvanced) {
        PrescriptionExpander.legacyTargets(targetExState.blocks)
    } else null
    val setsCount = weekTargets?.first ?: targetExState.sets.size
    val repsCount = if (targetExState.isTimeAndReps) {
        val sec = targetExState.timeTargetSeconds ?: targetExState.sets.firstOrNull()?.timeSeconds ?: 45
        stringResource(R.string.time_reps_value, sec, targetExState.repsGoal ?: 0)
    } else if (targetExState.isTimeAndWeight) {
        val sec = targetExState.timeTargetSeconds ?: targetExState.sets.firstOrNull()?.timeSeconds ?: 45
        "${sec}s"
    } else {
        weekTargets?.second ?: targetExState.repsDisplay.takeIf { it.isNotBlank() } ?: run {
            if (targetExState.sets.isEmpty()) "0"
            else {
                val allReps = targetExState.sets.map { it.reps }
                if (allReps.distinct().size == 1) allReps.first().toString()
                else allReps.joinToString("-")
            }
        }
    }
    FlowRow(
        modifier = modifier.animateContentSize(WorkoutMotion.spatial()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        if (targetExState.isAdvanced) {
            state.programWeek?.let { week ->
                PrescriptionPill(
                    text = stringResource(R.string.week_short, week),
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    emphasized = true
                )
            }
            val usesPercent = targetExState.blocks.any { it.intensityType == IntensityType.PERCENT }
            if (usesPercent) {
                PrescriptionPill(
                    text = targetExState.oneRepMaxKg?.let { stringResource(R.string.one_rep_max_value, prescriptionLabels.weight(it)) }
                        ?: stringResource(R.string.one_rep_max_missing),
                    containerColor = if (targetExState.oneRepMaxKg != null) Primary.copy(alpha = 0.12f) else Error.copy(alpha = 0.12f),
                    contentColor = if (targetExState.oneRepMaxKg != null) Primary else Error
                )
            } else {
                Text(
                    text = "$setsCount × $repsCount",
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            // The program of this exercise was changed during the workout: tap for the story
            targetExState.lastProgramEdit?.let { edit ->
                ProgramEditPill(edit, Modifier.clickable { ui.programHistoryFor = targetIndex })
            }
        } else {
            Text(
                text = "$setsCount × $repsCount",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold
            )
        }
        if (targetExState.emomStartIndex != null && targetExState.exercise.id in ui.emomManualExercises) {
            BadgePill(
                text = stringResource(R.string.emom_use_timer),
                color = Tertiary,
                onClick = { ui.emomManualExercises = ui.emomManualExercises - targetExState.exercise.id }
            )
        }
        if (targetExState.supersetId != null) {
            BadgePill(text = stringResource(R.string.superset), color = Primary, onClick = null)
        }
    }
}

/** The superset and EMOM badges: a timer icon and a word in a pill of the colour of the badge. */
@Composable
private fun BadgePill(text: String, color: androidx.compose.ui.graphics.Color, onClick: (() -> Unit)?) {
    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = color,
                fontWeight = FontWeight.Black
            )
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp), content = content)
    } else {
        Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), content = content)
    }
}

/**
 * The tools of the exercise in one tonal group, in the order they always had: link to the next exercise as a
 * superset, edit the program, details, swap. 44 dp wide each and 48 dp tall.
 */
@Composable
internal fun ExerciseTools(
    targetExState: WorkoutExerciseState,
    targetIndex: Int,
    state: WorkoutState,
    ui: WorkoutScreenUiState,
    viewModel: WorkoutViewModel,
    onOpenDetail: (Int) -> Unit
) {
    val groupLabel = stringResource(R.string.exercise_tools_group)
    val glass = LocalLiquidGlassEnabled.current
    Row(
        modifier = Modifier
            .then(
                if (glass) Modifier
                else Modifier.clip(RoundedCornerShape(16.dp)).background(SecondaryContainer)
            )
            .semantics { contentDescription = groupLabel },
        horizontalArrangement = Arrangement.spacedBy(if (glass) 8.dp else 0.dp)
    ) {
        if (targetIndex < state.exercises.lastIndex) {
            val nextEx = state.exercises[targetIndex + 1]
            val isLinked = targetExState.supersetId != null && targetExState.supersetId == nextEx.supersetId
            ToolButton(
                icon = if (isLinked) Icons.Rounded.LinkOff else Icons.Rounded.Link,
                description = stringResource(R.string.toggle_superset_with_next),
                tint = if (isLinked) Primary else OnSecondaryContainer,
                onClick = { viewModel.toggleSupersetWithNext(targetIndex) }
            )
        }
        if (targetExState.isAdvanced && !targetExState.isCardio && !targetExState.isTimed) {
            ToolButton(
                icon = Icons.Rounded.DesignServices,
                description = stringResource(R.string.edit_program),
                tint = if (targetExState.lastProgramEdit != null) Primary else OnSecondaryContainer,
                onClick = { ui.programEditFor = targetIndex }
            )
        }
        if (!targetExState.isCardio) {
            ToolButton(
                icon = Icons.Rounded.Info,
                description = stringResource(R.string.exercise_detail_open),
                tint = OnSecondaryContainer,
                onClick = { onOpenDetail(targetExState.exercise.id) }
            )
        }
        ToolButton(
            icon = Icons.Rounded.SwapHoriz,
            description = stringResource(R.string.swap_exercise),
            tint = OnSecondaryContainer,
            onClick = { ui.showSwapExerciseSheet = true }
        )
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    if (LocalLiquidGlassEnabled.current) {
        com.emanuel5014.trainable.ui.components.GymIconButton(
            icon = icon, description = description, contentColor = tint, onClick = onClick,
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(14.dp), quiet = true
        )
    } else {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 48.dp)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}
