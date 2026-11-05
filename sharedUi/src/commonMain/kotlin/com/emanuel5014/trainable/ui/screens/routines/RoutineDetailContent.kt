@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.ui.util.bodyPadding
import com.emanuel5014.trainable.ui.util.scrollEndInset
import com.emanuel5014.trainable.ui.util.plusBottom
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.add
import com.emanuel5014.trainable.sharedui.resources.add_exercise
import com.emanuel5014.trainable.sharedui.resources.advanced_needs_block
import com.emanuel5014.trainable.sharedui.resources.ai_scan_empty_state_add_manual
import com.emanuel5014.trainable.sharedui.resources.ai_scan_empty_state_desc
import com.emanuel5014.trainable.sharedui.resources.ai_scan_empty_state_scan_button
import com.emanuel5014.trainable.sharedui.resources.ai_scan_empty_state_title
import com.emanuel5014.trainable.sharedui.resources.auto_advance_week
import com.emanuel5014.trainable.sharedui.resources.auto_advance_week_desc
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.cardio_duration_slider
import com.emanuel5014.trainable.sharedui.resources.confirm
import com.emanuel5014.trainable.sharedui.resources.current_badge
import com.emanuel5014.trainable.sharedui.resources.delete
import com.emanuel5014.trainable.sharedui.resources.delete_week
import com.emanuel5014.trainable.sharedui.resources.delete_week_message
import com.emanuel5014.trainable.sharedui.resources.delete_week_title
import com.emanuel5014.trainable.sharedui.resources.edit_exercise
import com.emanuel5014.trainable.sharedui.resources.edit_routine
import com.emanuel5014.trainable.sharedui.resources.exercise_details
import com.emanuel5014.trainable.sharedui.resources.existing_workout_message
import com.emanuel5014.trainable.sharedui.resources.expiration_date
import com.emanuel5014.trainable.sharedui.resources.expires
import com.emanuel5014.trainable.sharedui.resources.link_with_next
import com.emanuel5014.trainable.sharedui.resources.no_exercises_in_routine
import com.emanuel5014.trainable.sharedui.resources.program_current_week
import com.emanuel5014.trainable.sharedui.resources.reset
import com.emanuel5014.trainable.sharedui.resources.reset_routine_content
import com.emanuel5014.trainable.sharedui.resources.reset_routine_content_message
import com.emanuel5014.trainable.sharedui.resources.rest_seconds
import com.emanuel5014.trainable.sharedui.resources.resume_workout
import com.emanuel5014.trainable.sharedui.resources.routine_details
import com.emanuel5014.trainable.sharedui.resources.routine_exercises_section
import com.emanuel5014.trainable.sharedui.resources.routine_header_collapse
import com.emanuel5014.trainable.sharedui.resources.routine_header_expand
import com.emanuel5014.trainable.sharedui.resources.routine_name
import com.emanuel5014.trainable.sharedui.resources.routine_notes
import com.emanuel5014.trainable.sharedui.resources.save
import com.emanuel5014.trainable.sharedui.resources.schedule_days
import com.emanuel5014.trainable.sharedui.resources.select_exercise
import com.emanuel5014.trainable.sharedui.resources.set_as_current_week
import com.emanuel5014.trainable.sharedui.resources.sets
import com.emanuel5014.trainable.sharedui.resources.skip_this_week
import com.emanuel5014.trainable.sharedui.resources.start
import com.emanuel5014.trainable.sharedui.resources.start_date
import com.emanuel5014.trainable.sharedui.resources.superset
import com.emanuel5014.trainable.sharedui.resources.tap_plus_to_add
import com.emanuel5014.trainable.sharedui.resources.tap_to_set
import com.emanuel5014.trainable.sharedui.resources.unlink_superset
import com.emanuel5014.trainable.sharedui.resources.update_exercise
import com.emanuel5014.trainable.sharedui.resources.update_plan
import com.emanuel5014.trainable.sharedui.resources.week_of
import com.emanuel5014.trainable.sharedui.resources.week_short
import com.emanuel5014.trainable.sharedui.resources.weekly_program
import com.emanuel5014.trainable.sharedui.resources.weekly_program_desc
import com.emanuel5014.trainable.sharedui.resources.weeks_count
import com.emanuel5014.trainable.sharedui.resources.routine_exercises_count
import com.emanuel5014.trainable.sharedui.resources.routine_sets_count
import com.emanuel5014.trainable.ui.components.LocalHapticsSetting
import com.emanuel5014.trainable.ui.components.LocalRoutineImagePicker
import com.emanuel5014.trainable.ui.components.LocalShowMessage
import com.emanuel5014.trainable.ui.util.narrowDayName
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import com.emanuel5014.trainable.ui.glass.actionGlass
import com.emanuel5014.trainable.ui.glass.actionGlassContainer
import com.emanuel5014.trainable.ui.glass.actionGlassElevation
import com.emanuel5014.trainable.ui.glass.actionGlassSource
import com.emanuel5014.trainable.ui.glass.rememberActionGlassSource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import com.emanuel5014.trainable.data.local.entity.OneRepMaxEntity
import com.emanuel5014.trainable.domain.exercise.ExerciseTypeMemory
import com.emanuel5014.trainable.domain.prescription.LoadCalculator
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionResolver
import com.emanuel5014.trainable.domain.prescription.RepsDraft
import com.emanuel5014.trainable.domain.prescription.ResolvedPrescription
import com.emanuel5014.trainable.domain.prescription.StartingWeight
import com.emanuel5014.trainable.ui.components.NumberStepper
import com.emanuel5014.trainable.ui.components.AdvancedPrescriptionEditor
import com.emanuel5014.trainable.ui.components.WeekEditing
import com.emanuel5014.trainable.domain.prescription.WeekShift
import com.emanuel5014.trainable.ui.components.OneRepMaxCard
import com.emanuel5014.trainable.ui.components.OneRepMaxDialog
import com.emanuel5014.trainable.ui.components.PrescriptionBlocksEditor
import com.emanuel5014.trainable.ui.components.PrescriptionPill
import com.emanuel5014.trainable.ui.components.WeekSelector
import com.emanuel5014.trainable.ui.components.ExerciseTypeSelector
import com.emanuel5014.trainable.ui.components.LocalAdvancedProgramming
import com.emanuel5014.trainable.ui.components.rememberPrescriptionLabels
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import com.emanuel5014.trainable.ui.components.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.relation.PlanExerciseWithDetails
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import com.emanuel5014.trainable.ui.components.ExerciseEntryCard
import com.emanuel5014.trainable.ui.components.ExerciseBadgeSize
import com.emanuel5014.trainable.ui.components.ExercisePickerBottomSheet
import com.emanuel5014.trainable.ui.components.RestoreExerciseDialog
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymIconButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.SheetFormBody
import com.emanuel5014.trainable.ui.components.SheetFormFooter
import com.emanuel5014.trainable.ui.components.SheetFormLayout
import com.emanuel5014.trainable.ui.components.GlowProgressOverlay
import com.emanuel5014.trainable.ui.components.GymLoadingIndicator
import com.emanuel5014.trainable.ui.components.ScreenHeader
import com.emanuel5014.trainable.ui.components.TargetSecondsSlider
import com.emanuel5014.trainable.ui.components.exercise.TimeAndRepsEditor
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.domain.prescription.TimeAndReps
import com.emanuel5014.trainable.util.WeightUnitConverter
import com.emanuel5014.trainable.ui.components.exercise.RepsEditor
import com.emanuel5014.trainable.ui.components.exercise.StartingWeightField
import com.emanuel5014.trainable.ui.theme.Error
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSecondaryContainer
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.glass.headerGlassOn
import com.emanuel5014.trainable.ui.glass.headerGroupGlass
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.SecondaryContainer
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Shapes
import com.emanuel5014.trainable.ui.theme.Spacing
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.theme.Tertiary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private fun getSupersetRange(index: Int, list: List<PlanExerciseWithDetails>): IntRange {
    val sid = list.getOrNull(index)?.planExercise?.supersetId ?: return index..index
    
    var start = index
    while (start > 0 && list[start - 1].planExercise.supersetId == sid) {
        start--
    }
    
    var end = index
    while (end < list.lastIndex && list[end + 1].planExercise.supersetId == sid) {
        end++
    }
    
    return start..end
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RoutineDetailContent(
    state: RoutineDetailState,
    languageCode: String,
    scan: RoutineScanUi,
    onNavigateBack: () -> Unit,
    onStartWorkout: (Int, Int?) -> Unit
) {
    val uiState by state.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val hapticEnabled by LocalHapticsSetting.current.collectAsState(initial = true)
    val themeMode by state.themeMode.collectAsState()
    val showMessage = LocalShowMessage.current
    val advancedNeedsBlockText = stringResource(Res.string.advanced_needs_block)
    val isDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()
    val exerciseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val routineSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scanHazeState = rememberHazeState()

    var showExerciseSheet by remember { mutableStateOf(false) }
    var showExercisePicker by remember { mutableStateOf(false) }


    var weekToDelete by remember { mutableStateOf<Int?>(null) }
    var showRoutineEditSheet by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var existingSessionForPlan by remember { mutableStateOf<SessionWithPlanName?>(null) }
    var routineName by remember { mutableStateOf("") }
    var routineNote by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val selectedDays = remember { mutableStateListOf<DayOfWeek>() }

    var selectedExerciseId by remember { mutableStateOf<Int?>(null) }
    var selectedExerciseType by remember { mutableStateOf("strength") }
    // The type each exercise was last added with: adding it again opens the selector on it
    val rememberedTypes by state.exerciseTypes.collectAsState()
    var timeTargetSecondsText by remember { mutableStateOf("45") }
    var setsText by remember { mutableStateOf("3") }
    // Reps goal of a Time & Reps exercise
    var repsGoalText by remember { mutableStateOf(TimeAndReps.DEFAULT_REPS_GOAL.toString()) }
    var repsDraft by remember { mutableStateOf(RepsDraft()) }
    // Typed in the user's weight unit; blank means no starting weight
    var presetText by remember { mutableStateOf("") }
    // What the field showed when the sheet opened: left alone, the stored weight is kept as it is
    var presetTextAtOpen by remember { mutableStateOf("") }
    var restText by remember { mutableStateOf("120") }
    var cardioDurationText by remember { mutableStateOf("20") }
    var cardioSetsText by remember { mutableStateOf("1") }

    // Advanced (%1RM / blocks) prescription drafts
    val advancedOn = LocalAdvancedProgramming.current
    val weightUnit by state.weightUnit.collectAsState()
    val startingWeightEnabled by state.startingWeightEnabled.collectAsState()
    val oneRepMaxes by state.oneRepMaxes.collectAsState()
    val prescriptionLabels = rememberPrescriptionLabels(weightUnit)
    val advancedWeeks = remember { mutableStateMapOf<Int, List<PrescriptionBlock>>() }
    val draftExcludedWeeks = remember { mutableStateListOf<Int>() }
    var draftWeeksCount by remember { mutableIntStateOf(1) }
    var editorWeek by remember { mutableIntStateOf(1) }
    val planWeeksCount = uiState.planDetails?.plan?.weeksCount ?: 1
    val planCurrentWeek = uiState.planDetails?.plan?.currentWeek ?: 1
    var viewWeek by remember { mutableIntStateOf(1) }
    LaunchedEffect(uiState.planDetails?.plan?.id, planCurrentWeek) { viewWeek = planCurrentWeek }
    LaunchedEffect(planWeeksCount) { if (viewWeek > planWeeksCount) viewWeek = planWeeksCount }

    var routineWeeksCount by remember { mutableIntStateOf(1) }
    var routineCurrentWeek by remember { mutableIntStateOf(1) }
    var routineAutoAdvance by remember { mutableStateOf(true) }

    // Local state for dragging to ensure smoothness
    val localExercises = remember { mutableStateListOf<PlanExerciseWithDetails>() }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    // After the finger lifts the card is still on its way into the slot: it is no longer lifted, but keeps moving
    var dragReleasing by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    fun releaseDrag() {
        dragReleasing = true
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(
                initialValue = dragOffsetY,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
            ) { value, _ -> dragOffsetY = value }
            draggedItemIndex = null
            dragOffsetY = 0f
            dragReleasing = false
        }
    }

    // Real height of a run of rows plus the gaps between them: how far a block moves when another one passes it
    fun rowsPitchPx(range: IntRange, fallbackPx: Float, gapPx: Float): Float {
        val visible = listState.layoutInfo.visibleItemsInfo
        var total = 0f
        for (i in range) {
            val id = localExercises.getOrNull(i)?.planExercise?.id
            total += (visible.firstOrNull { it.key == id }?.size?.toFloat() ?: fallbackPx) + gapPx
        }
        return total
    }

    val trainedMuscleGroups = remember(localExercises.size, localExercises.map { it.exercise.categoria }) {
        localExercises.map { it.exercise.categoria }
            .distinct()
            .filter { it.isNotBlank() }
    }

    var editingExerciseId by remember { mutableStateOf<Int?>(null) }
    // Looked up on every composition: a cached copy would still hold the blocks / starting weight the exercise had
    // before it was last saved, and saving would write those old values back
    val editingExercise = localExercises.find { it.planExercise.id == editingExerciseId }

    // Sync local list with UI state when not dragging
    LaunchedEffect(uiState.planDetails?.exercises, draggedItemIndex == null) {
        if (draggedItemIndex == null) {
            localExercises.clear()
            uiState.planDetails?.exercises?.let { localExercises.addAll(it) }
        }
    }




    fun openAddSheet() {
        editingExerciseId = null
        selectedExerciseId = null
        selectedExerciseType = "strength"
        setsText = "3"
        repsDraft = RepsDraft()
        repsGoalText = TimeAndReps.DEFAULT_REPS_GOAL.toString()
        presetText = ""
        presetTextAtOpen = ""
        timeTargetSecondsText = "45"
        cardioDurationText = "20"
        cardioSetsText = "1"
        advancedWeeks.clear()
        draftExcludedWeeks.clear()
        draftWeeksCount = planWeeksCount
        editorWeek = viewWeek
        // Inherit rest from the last exercise in the list, default to 120 if empty
        restText = localExercises.lastOrNull()?.planExercise?.recuperoTarget?.toString() ?: "120"
        showExercisePicker = true
    }

    // The exercise deleted from the exercise list that the user tapped
    var deletedExerciseTapped by remember { mutableStateOf<PlanExerciseWithDetails?>(null) }

    fun openEditSheet(item: PlanExerciseWithDetails) {
        editingExerciseId = item.planExercise.id
        selectedExerciseId = item.exercise.id
        selectedExerciseType = if (item.isAdvanced && advancedOn) "advanced" else item.planExercise.exerciseType
        advancedWeeks.clear()
        advancedWeeks.putAll(item.blocksByWeek)
        draftExcludedWeeks.clear()
        draftExcludedWeeks.addAll(item.excludedWeeks)
        draftWeeksCount = maxOf(planWeeksCount, item.blocksByWeek.keys.maxOrNull() ?: 1)
        editorWeek = viewWeek.coerceAtMost(draftWeeksCount)
        setsText = item.planExercise.serieTarget.toString()
        repsDraft = RepsDraft.from(item.planExercise.repsTarget, item.planExercise.repsMax)
        // A plain exercise turned into Time & Reps keeps its reps as the goal
        repsGoalText = TimeAndReps.goalOf(item.planExercise.repsRange?.min?.toString() ?: item.planExercise.repsTarget).toString()
        presetText = item.planExercise.weightPreset
            ?.let { WeightUnitConverter.format(WeightUnitConverter.convertDisplay(it, weightUnit)) }
            .orEmpty()
        presetTextAtOpen = presetText
        // Only a hold's reps ("45s") hold its time: a plain exercise's "20" are reps, not seconds
        timeTargetSecondsText = item.planExercise.durataTargetSecondi?.toString()
            ?: item.planExercise.repsTarget.takeIf { item.planExercise.exerciseType == "time_and_weight" }?.filter { it.isDigit() }?.ifBlank { null }
            ?: "45"
        restText = item.planExercise.recuperoTarget.toString()
        cardioDurationText = item.planExercise.durataTargetSecondi?.let { (it / 60).toString() } ?: "20"
        cardioSetsText = item.planExercise.serieTarget.coerceAtLeast(1).toString()
        showExerciseSheet = true
    }

    // Tapping an exercise deleted from the exercise list offers to add it back before editing it
    deletedExerciseTapped?.let { tapped ->
        RestoreExerciseDialog(
            exerciseNames = listOf(ExerciseTranslations.translate(tapped.exercise.nome, languageCode)),
            onRestore = {
                state.restoreExercise(tapped.exercise.id)
                deletedExerciseTapped = null
                openEditSheet(tapped)
            },
            onSkip = {
                deletedExerciseTapped = null
                openEditSheet(tapped)
            },
            onDismiss = { deletedExerciseTapped = null }
        )
    }

    fun openRoutineEditSheet() {
        uiState.planDetails?.plan?.let { plan ->
            routineName = plan.nome
            routineNote = plan.note.orEmpty()
            startDate = plan.dataInizio
            endDate = plan.dataFine
            routineWeeksCount = plan.weeksCount
            routineCurrentWeek = plan.currentWeek
            routineAutoAdvance = plan.autoAdvanceWeek
            selectedDays.clear()
            plan.giorniSettimana?.split(",")?.forEach {
                it.toIntOrNull()?.let { value -> selectedDays.add(DayOfWeek(value)) }
            }
            showRoutineEditSheet = true
        }
    }

    weekToDelete?.let { week ->
        AlertDialog(
            onDismissRequest = { weekToDelete = null },
            title = { Text(stringResource(Res.string.delete_week_title, week), fontWeight = FontWeight.ExtraBold) },
            text = { Text(stringResource(Res.string.delete_week_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        state.deleteWeek(week)
                        weekToDelete = null
                    },
                    containerColor = Error.copy(alpha = 0.12f),
                    contentColor = Error,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(Res.string.delete).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { weekToDelete = null },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (existingSessionForPlan != null) {
        AlertDialog(
            onDismissRequest = { existingSessionForPlan = null },
            title = { Text(stringResource(Res.string.resume_workout)) },
            text = { Text(stringResource(Res.string.existing_workout_message, existingSessionForPlan!!.displayName)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        existingSessionForPlan?.let { session ->
                            onStartWorkout(session.session.planId, session.session.id)
                        }
                        existingSessionForPlan = null
                    },
                    containerColor = Primary,
                    contentColor = OnPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(Res.string.resume_workout), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { existingSessionForPlan = null },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(stringResource(Res.string.reset_routine_content)) },
            text = { Text(stringResource(Res.string.reset_routine_content_message)) },
            confirmButton = {
                GymButton(
                    onClick = {
                        state.resetRoutineContent()
                        showResetConfirmDialog = false
                    },
                    containerColor = Error.copy(alpha = 0.1f),
                    contentColor = Error,
                    modifier = Modifier.padding(horizontal = 8.dp).height(48.dp)
                ) {
                    Text(stringResource(Res.string.reset).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showResetConfirmDialog = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(Res.string.cancel).uppercase())
                }
            },
            containerColor = SurfaceContainerHigh,
            titleContentColor = OnSurface,
            textContentColor = OnSurfaceVariant
        )
    }

    val isScanActive = scan.isScanning


    // The Start button shrinks to its icon once the list is scrolled away from the top
    val startFabExpanded by remember { derivedStateOf { !listState.canScrollBackward } }

    val actionGlass = rememberActionGlassSource()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isScanActive) Modifier.hazeSource(state = scanHazeState) else Modifier
            ),
        containerColor = Surface,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FloatingActionButton(
                    onClick = { openAddSheet() },
                    containerColor = actionGlassContainer(actionGlass, SecondaryContainer),
                    contentColor = OnSecondaryContainer,
                    shape = RoundedCornerShape(18.dp),
                    elevation = actionGlassElevation(actionGlass),
                    modifier = Modifier.actionGlass(actionGlass, SecondaryContainer, cornerRadius = 18.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(Res.string.add_exercise))
                }

                if (localExercises.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            val planId = uiState.planDetails?.plan?.id
                            if (planId != null) {
                                val existing = uiState.unfinishedSessions.find { it.session.planId == planId }
                                if (existing != null) {
                                    existingSessionForPlan = existing
                                } else {
                                    onStartWorkout(planId, null)
                                }
                            }
                        },
                        expanded = startFabExpanded,
                        icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                        text = { Text(stringResource(Res.string.start), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold) },
                        containerColor = actionGlassContainer(actionGlass, Primary),
                        contentColor = OnPrimary,
                        shape = RoundedCornerShape(20.dp),
                        elevation = actionGlassElevation(actionGlass),
                        modifier = Modifier.actionGlass(actionGlass, Primary, cornerRadius = 20.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                GymLoadingIndicator()
            }
        } else if (uiState.planDetails != null) {
            val details = uiState.planDetails!!

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .actionGlassSource(actionGlass)
                    .padding(paddingValues.bodyPadding()),
                contentPadding = PaddingValues(bottom = 120.dp).plusBottom(paddingValues.scrollEndInset()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ScreenHeader(
                        title = details.plan.nome,
                        subtitle = stringResource(Res.string.routine_details).uppercase(),
                        navigationIcon = {
                            GymIconButton(
                                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                onClick = onNavigateBack,
                                containerColor = Color.Transparent,
                                contentColor = OnSurface,
                                description = "Back"
                            )
                        },
                        actions = {
                            if (headerGlassOn()) {
                                // One capsule of glass for the pair, as iOS groups its toolbar buttons
                                Row(modifier = Modifier.headerGroupGlass()) {
                                    GymIconButton(
                                        icon = Icons.Rounded.RestartAlt,
                                        onClick = { showResetConfirmDialog = true },
                                        containerColor = Color.Transparent,
                                        contentColor = Error,
                                        description = "Reset Routine Content"
                                    )
                                    GymIconButton(
                                        icon = Icons.Rounded.Edit,
                                        onClick = { openRoutineEditSheet() },
                                        containerColor = Color.Transparent,
                                        contentColor = OnSurface,
                                        description = "Edit Routine"
                                    )
                                }
                            } else {
                            // Connected pair, like the day and week pickers
                            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                                GymIconButton(
                                    icon = Icons.Rounded.RestartAlt,
                                    onClick = { showResetConfirmDialog = true },
                                    containerColor = SecondaryContainer,
                                    contentColor = Error,
                                    description = "Reset Routine Content",
                                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                                )
                                GymIconButton(
                                    icon = Icons.Rounded.Edit,
                                    onClick = { openRoutineEditSheet() },
                                    containerColor = SecondaryContainer,
                                    contentColor = OnSecondaryContainer,
                                    description = "Edit Routine",
                                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp)
                                )
                            }
                            }
                        },
                        titleInRow = true,
                        titleStyle = MaterialTheme.typography.headlineLarge
                    )
                }

                if (scan.showDraftBanner) {
                    item {
                        scan.DraftBanner(Modifier.padding(horizontal = ResponsiveSize.horizontalPadding))
                    }
                }

                item(key = "overview") {
                    val headerExpanded by state.headerExpanded.collectAsState()
                    RoutineOverviewSection(
                        startDate = details.plan.dataInizio,
                        endDate = details.plan.dataFine,
                        scheduledDaysCsv = details.plan.giorniSettimana,
                        note = details.plan.note,
                        muscleGroups = trainedMuscleGroups.map { ExerciseTranslations.translateCategory(it, languageCode) },
                        images = details.images,
                        expanded = headerExpanded,
                        onToggle = { state.setHeaderExpanded(!headerExpanded) },
                        onImageAdd = { uri -> state.addPlanImage(uri) },
                        onImageRemove = { image -> state.removePlanImage(image) },
                        modifier = Modifier.padding(horizontal = ResponsiveSize.horizontalPadding)
                    )
                }

                if (advancedOn && details.plan.weeksCount > 1) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ResponsiveSize.horizontalPadding)
                                .clip(Shapes.large)
                                .background(SurfaceContainerHigh)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(Res.string.week_of, viewWeek, details.plan.weeksCount).uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                    color = Primary,
                                    fontWeight = FontWeight.Black
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (viewWeek == details.plan.currentWeek) {
                                        PrescriptionPill(
                                            text = stringResource(Res.string.current_badge),
                                            containerColor = Primary.copy(alpha = 0.12f),
                                            contentColor = Primary
                                        )
                                    } else {
                                        TextButton(onClick = { state.setCurrentWeek(viewWeek) }) {
                                            Text(
                                                text = stringResource(Res.string.set_as_current_week).uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Primary
                                            )
                                        }
                                    }
                                    IconButton(onClick = { weekToDelete = viewWeek }, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = stringResource(Res.string.delete_week),
                                            tint = OnSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            WeekSelector(
                                weeksCount = details.plan.weeksCount,
                                selectedWeek = viewWeek,
                                onWeekSelected = { viewWeek = it },
                                currentWeek = details.plan.currentWeek,
                                hapticEnabled = hapticEnabled,
                                containerColor = SurfaceContainerHighest
                            )
                        }
                    }
                }

                if (localExercises.isEmpty()) {
                    item {
                        val cookieShape = MaterialShapes.Cookie9Sided.toShape()
                        if (scan.available) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ResponsiveSize.horizontalPadding)
                                    .padding(top = 12.dp, bottom = 40.dp)
                                    .clip(Shapes.large)
                                    .background(SurfaceContainerHigh)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(cookieShape)
                                        .background(Primary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DocumentScanner,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(Res.string.ai_scan_empty_state_title),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.titleMedium.fontSize)
                                        ),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = OnSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = stringResource(Res.string.ai_scan_empty_state_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                GymButton(
                                    onClick = { scan.openSource() },
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = Primary,
                                    contentColor = OnPrimary
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DocumentScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(Res.string.ai_scan_empty_state_scan_button),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                GymButton(
                                    onClick = { openAddSheet() },
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = SecondaryContainer,
                                    contentColor = OnSecondaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(Res.string.ai_scan_empty_state_add_manual),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = ResponsiveSize.horizontalPadding)
                                    .padding(top = 48.dp, bottom = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(cookieShape)
                                        .background(Primary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FitnessCenter,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(Res.string.no_exercises_in_routine),
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.titleMedium.fontSize)),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OnSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = stringResource(Res.string.tap_plus_to_add),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                GymButton(
                                    onClick = { openAddSheet() },
                                    containerColor = Primary,
                                    contentColor = OnPrimary
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(Res.string.add_exercise),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item(key = "exercises_header") {
                        // Excluded exercises (advanced programming, skipped this week) do not count
                        var activeExercises = 0
                        var totalSets = 0
                        localExercises.forEach { entry ->
                            when (val resolved = if (advancedOn) entry.resolve(viewWeek) else ResolvedPrescription.Legacy) {
                                is ResolvedPrescription.Excluded -> Unit
                                is ResolvedPrescription.Blocks -> {
                                    activeExercises++
                                    totalSets += resolved.blocks.sumOf { it.effectiveSets }
                                }
                                is ResolvedPrescription.Legacy -> {
                                    activeExercises++
                                    totalSets += entry.planExercise.serieTarget
                                }
                            }
                        }
                        // The summary drops under the title when a large font leaves no room beside it
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ResponsiveSize.horizontalPadding)
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.routine_exercises_section),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = OnSurface
                            )
                            Text(
                                text = pluralStringResource(Res.plurals.routine_exercises_count, activeExercises, activeExercises) +
                                    " \u00B7 " + pluralStringResource(Res.plurals.routine_sets_count, totalSets, totalSets),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSecondaryContainer,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SecondaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    itemsIndexed(localExercises, key = { _, item -> item.planExercise.id }) { index, item ->
                        val draggedRange = draggedItemIndex?.let { getSupersetRange(it, localExercises) }
                        val isDragging = draggedRange != null && index in draggedRange
                        
                        // Superset logic
                        val currentSid = item.planExercise.supersetId
                        val isSuperset = currentSid != null
                        val isStart = isSuperset && (index == 0 || localExercises[index - 1].planExercise.supersetId != currentSid)
                        val isEnd = isSuperset && (index == localExercises.lastIndex || localExercises[index + 1].planExercise.supersetId != currentSid)
                        val sidePadding = ResponsiveSize.horizontalPadding
                        // Rail linking the number badges of a superset: centred on the badge, drawn behind the cards
                        val railCenterX = ResponsiveSize.cardPadding + ExerciseBadgeSize / 2
                        val railWidth = 4.dp
                        val railGap = 12.dp
                        val railColor = Primary

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                // The rows around a dragged card slide into place; the dragged card follows the finger
                                .animateItem(
                                    placementSpec = if (isDragging) null else spring(
                                        dampingRatio = 0.7f,
                                        stiffness = Spring.StiffnessMediumLow,
                                        visibilityThreshold = IntOffset.VisibilityThreshold
                                    )
                                )
                                .zIndex(if (isDragging) 1f else 0f)
                        ) {
                            if (isStart) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = sidePadding + 16.dp, bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Link,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(Res.string.superset),
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                        color = Primary,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = sidePadding)
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragOffsetY else 0f
                                    }
                                    .pointerInput(item.planExercise.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                settleJob?.cancel()
                                                dragReleasing = false
                                                draggedItemIndex = localExercises.indexOfFirst { it.planExercise.id == item.planExercise.id }
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                
                                                val gapPx = 12.dp.toPx()
                                                val fallbackRowPx = 80.dp.toPx()
                                                val currentDraggedIndex = draggedItemIndex
                                                if (currentDraggedIndex != null) {
                                                    val range = getSupersetRange(currentDraggedIndex, localExercises)
                                                    val size = range.endInclusive - range.start + 1
                                                    
                                                    if (dragOffsetY > 0 && range.endInclusive < localExercises.lastIndex) {
                                                        val nextIndex = range.endInclusive + 1
                                                        val nextRange = getSupersetRange(nextIndex, localExercises)
                                                        val nextSize = nextRange.endInclusive - nextRange.start + 1
                                                        val nextPitch = rowsPitchPx(nextRange, fallbackRowPx, gapPx)
                                                        val threshold = nextPitch / 2f
                                                        
                                                        if (dragOffsetY > threshold) {
                                                            val draggedItems = localExercises.subList(range.start, range.endInclusive + 1).toList()
                                                            repeat(size) {
                                                                localExercises.removeAt(range.start)
                                                            }
                                                            val insertIndex = range.start + nextSize
                                                            localExercises.addAll(insertIndex, draggedItems)
                                                            
                                                            draggedItemIndex = insertIndex + (currentDraggedIndex - range.start)
                                                            dragOffsetY -= nextPitch
                                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                    } else if (dragOffsetY < 0 && range.start > 0) {
                                                        val prevIndex = range.start - 1
                                                        val prevRange = getSupersetRange(prevIndex, localExercises)
                                                        val prevSize = prevRange.endInclusive - prevRange.start + 1
                                                        val prevPitch = rowsPitchPx(prevRange, fallbackRowPx, gapPx)
                                                        val threshold = -prevPitch / 2f
                                                        
                                                        if (dragOffsetY < threshold) {
                                                            val draggedItems = localExercises.subList(range.start, range.endInclusive + 1).toList()
                                                            repeat(size) {
                                                                localExercises.removeAt(range.start)
                                                            }
                                                            val insertIndex = prevRange.start
                                                            localExercises.addAll(insertIndex, draggedItems)
                                                            
                                                            draggedItemIndex = insertIndex + (currentDraggedIndex - range.start)
                                                            dragOffsetY += prevPitch
                                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                state.updateExercisesOrder(localExercises.map { it.planExercise })
                                                releaseDrag()
                                            },
                                            onDragCancel = { releaseDrag() }
                                        )
                                    }
                                    .then(
                                        if (isSuperset && !isDragging) {
                                            Modifier.drawBehind {
                                                val x = railCenterX.toPx()
                                                val half = railGap.toPx() / 2f
                                                val centerY = size.height / 2f
                                                if (!isStart) {
                                                    drawLine(railColor, Offset(x, centerY), Offset(x, -half), strokeWidth = railWidth.toPx())
                                                }
                                                if (!isEnd) {
                                                    drawLine(railColor, Offset(x, centerY), Offset(x, size.height + half), strokeWidth = railWidth.toPx())
                                                }
                                            }
                                        } else Modifier
                                    )
                            ) {
                                ExerciseEntryCard(
                                    item = item,
                                    number = index + 1,
                                    isDragging = isDragging && !dragReleasing,
                                    onClick = { if (item.exercise.isDeleted) deletedExerciseTapped = item else openEditSheet(item) },
                                    modifier = Modifier.weight(1f),
                                    languageCode = languageCode,
                                    isSuperset = isSuperset,
                                    week = viewWeek,
                                    weeksCount = planWeeksCount,
                                    labels = if (advancedOn) prescriptionLabels else null,
                                    oneRepMaxKg = oneRepMaxes[item.exercise.id]
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(140.dp)) }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Routine not found", color = OnSurfaceVariant)
            }
        }
    }

    if (showExerciseSheet) {
        // The exercise being edited is looked up on the plan: it may be one deleted from the exercise list, which the list no longer has
        val selectedExercise = editingExercise?.exercise?.takeIf { it.id == selectedExerciseId }
            ?: uiState.availableExercises.firstOrNull { it.id == selectedExerciseId }
        val isSelectedCardio = selectedExercise?.categoria?.equals("Cardio", ignoreCase = true) == true || editingExercise?.planExercise?.exerciseType == "cardio"
        // With starting weights off the field is hidden and an exercise keeps the weight it already had
        val presetKg: Float? = when {
            !startingWeightEnabled -> editingExercise?.planExercise?.weightPreset
            // Untouched: a round trip through pounds would nudge the stored kilograms (62.5 -> 62.5002)
            editingExercise != null && presetText == presetTextAtOpen -> editingExercise.planExercise.weightPreset
            else -> StartingWeight.parse(presetText)?.let { WeightUnitConverter.convertStorage(it, weightUnit) }
        }
        val presetInvalid = startingWeightEnabled && presetText.isNotBlank() && presetKg == null
        val strengthRepsInvalid = !isSelectedCardio && selectedExerciseType == "strength" && !repsDraft.isValid
        val repsGoalInvalid = !isSelectedCardio && selectedExerciseType == TIME_AND_REPS && TimeAndReps.parseGoal(repsGoalText) == null
        val canSaveExercise = !presetInvalid && !strengthRepsInvalid && !repsGoalInvalid

        ModalBottomSheet(
            onDismissRequest = { showExerciseSheet = false },
            sheetState = exerciseSheetState,
            containerColor = Surface,
            contentColor = OnSurface,
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(OnSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        ) {
            SheetFormLayout {
                SheetFormBody(horizontalPadding = ResponsiveSize.cardPadding, spacing = Spacing.large) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xtraSmall)) {
                        Text(
                            text = if (editingExercise == null) stringResource(Res.string.add_exercise) else stringResource(Res.string.edit_exercise),
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (editingExercise == null) stringResource(Res.string.exercise_details) else stringResource(Res.string.update_exercise),
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineMedium.fontSize)),
                            color = OnSurface,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch { exerciseSheetState.hide() }.invokeOnCompletion {
                                    if (!exerciseSheetState.isVisible) {
                                        showExerciseSheet = false
                                        showExercisePicker = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.large,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = OnSurface
                            ),
                            border = BorderStroke(1.dp, OnSurfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = selectedExercise?.let {
                                            ExerciseTranslations.translate(it.nome, languageCode)
                                        } ?: stringResource(Res.string.select_exercise),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.bodyLarge.fontSize)),
                                        color = OnSurface
                                    )
                                    selectedExercise?.let {
                                        Text(
                                            text = ExerciseTranslations.translateCategory(it.categoria, languageCode),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant
                                )
                            }
                        }

                        if (isSelectedCardio) {
                            // Several sets: intervals, or a warm-up and a cool-down; the rest is the one between them
                            GymInputField(
                                value = cardioSetsText,
                                onValueChange = { cardioSetsText = it.filter { c -> c.isDigit() }.take(2) },
                                label = stringResource(Res.string.sets),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            CardioDurationSlider(
                                valueMinutes = cardioDurationText.toIntOrNull() ?: 20,
                                onValueChange = { cardioDurationText = it.toString() },
                                hapticEnabled = hapticEnabled,
                                haptic = haptic,
                                modifier = Modifier.fillMaxWidth()
                            )

                            RestSlider(
                                value = restText.toIntOrNull() ?: 120,
                                onValueChange = { restText = it.toString() },
                                hapticEnabled = hapticEnabled,
                                haptic = haptic,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            ExerciseTypeSelector(
                                selectedType = selectedExerciseType,
                                showAdvanced = advancedOn,
                                onTypeSelected = { type ->
                                    if (type == "advanced" && selectedExerciseType != "advanced" && advancedWeeks.values.none { it.isNotEmpty() }) {
                                        // Carry the simple sets × reps over as a first free block
                                        val sets = setsText.trim().toIntOrNull() ?: 3
                                        val reps = repsDraft.target?.repsTarget ?: "5"
                                        advancedWeeks[editorWeek] = listOf(PrescriptionBlock(sets = sets, reps = reps))
                                    }
                                    selectedExerciseType = type
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (selectedExerciseType == "advanced") {
                                AdvancedPrescriptionEditor(
                                    blocksByWeek = advancedWeeks.toMap(),
                                    onBlocksByWeekChange = { updated ->
                                        advancedWeeks.clear()
                                        advancedWeeks.putAll(updated)
                                    },
                                    exerciseId = selectedExerciseId,
                                    exerciseName = selectedExercise?.let { ExerciseTranslations.translate(it.nome, languageCode) },
                                    weeks = WeekEditing(
                                        weeksCount = draftWeeksCount,
                                        onWeeksCountChange = { draftWeeksCount = it },
                                        persistedWeeksCount = planWeeksCount,
                                        currentWeek = planCurrentWeek,
                                        excludedWeeks = draftExcludedWeeks.toSet(),
                                        onExcludedWeeksChange = { updated ->
                                            draftExcludedWeeks.clear()
                                            draftExcludedWeeks.addAll(updated)
                                        },
                                        onDeleteWeek = { week ->
                                            // Weeks the routine already has are removed from every exercise; weeks only added in this draft just disappear
                                            if (week <= planWeeksCount) state.deleteWeek(week)
                                            val shifted = WeekShift.removeWeek(advancedWeeks.toMap(), week)
                                            advancedWeeks.clear()
                                            advancedWeeks.putAll(shifted)
                                            val excludedShifted = WeekShift.removeWeek(draftExcludedWeeks.toSet(), week)
                                            draftExcludedWeeks.clear()
                                            draftExcludedWeeks.addAll(excludedShifted)
                                            draftWeeksCount = (draftWeeksCount - 1).coerceAtLeast(1)
                                        },
                                        initialWeek = editorWeek
                                    )
                                )
                            } else if (selectedExerciseType == "time_and_weight") {
                                GymInputField(
                                    value = setsText,
                                    onValueChange = { setsText = it },
                                    label = stringResource(Res.string.sets),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                TargetSecondsSlider(
                                    valueSeconds = timeTargetSecondsText.toIntOrNull() ?: 45,
                                    onValueChange = { timeTargetSecondsText = it.toString() },
                                    hapticEnabled = hapticEnabled,
                                    haptic = haptic,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (startingWeightEnabled) {
                                    StartingWeightField(text = presetText, onTextChange = { presetText = it }, weightUnit = weightUnit)
                                }
                            } else if (selectedExerciseType == TIME_AND_REPS) {
                                // No weight, so no starting weight either
                                TimeAndRepsEditor(
                                    setsText = setsText,
                                    onSetsChange = { setsText = it },
                                    repsGoalText = repsGoalText,
                                    onRepsGoalChange = { repsGoalText = it },
                                    seconds = timeTargetSecondsText.toIntOrNull() ?: TimeAndReps.DEFAULT_SECONDS,
                                    onSecondsChange = { timeTargetSecondsText = it.toString() },
                                    hapticEnabled = hapticEnabled,
                                    haptic = haptic,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                RepsEditor(
                                    draft = repsDraft,
                                    onDraftChange = { repsDraft = it },
                                    setsText = setsText,
                                    onSetsChange = { setsText = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (startingWeightEnabled) {
                                    StartingWeightField(text = presetText, onTextChange = { presetText = it }, weightUnit = weightUnit)
                                }
                            }

                            RestSlider(
                                value = restText.toIntOrNull() ?: 120,
                                onValueChange = { restText = it.toString() },
                                hapticEnabled = hapticEnabled,
                                haptic = haptic,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                            if (advancedOn && planWeeksCount > 1 && selectedExerciseType != "advanced") {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = stringResource(Res.string.skip_this_week),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = OnSurfaceVariant,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        (1..planWeeksCount).forEach { week ->
                                            val skipped = week in draftExcludedWeeks
                                            FilterChip(
                                                selected = skipped,
                                                onClick = { if (skipped) draftExcludedWeeks.remove(week) else draftExcludedWeeks.add(week) },
                                                label = { Text(stringResource(Res.string.week_short, week)) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Error.copy(alpha = 0.15f),
                                                    selectedLabelColor = Error
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            if (editingExercise != null && localExercises.indexOfFirst { it.planExercise.id == editingExercise.planExercise.id } < localExercises.size - 1) {
                                val nextItem = localExercises.getOrNull(localExercises.indexOfFirst { it.planExercise.id == editingExercise.planExercise.id } + 1)
                                val isLinked = editingExercise.planExercise.supersetId != null && editingExercise.planExercise.supersetId == nextItem?.planExercise?.supersetId
                            
                                GymButton(
                                    onClick = {
                                        editingExercise.let { current ->
                                            val index = localExercises.indexOfFirst { it.planExercise.id == current.planExercise.id }
                                            if (index != -1 && index < localExercises.size - 1) {
                                                val nextIndex = index + 1
                                                val nextItem = localExercises[nextIndex]
                                                val newSid = if (isLinked) null else (current.planExercise.supersetId ?: nextItem.planExercise.supersetId ?: Uuid.random().toString())
                                            
                                                // Update local list for instant feedback
                                                val updatedCurrent = current.copy(planExercise = current.planExercise.copy(supersetId = newSid))
                                                val updatedNext = nextItem.copy(planExercise = nextItem.planExercise.copy(supersetId = newSid))
                                            
                                                localExercises[index] = updatedCurrent
                                                localExercises[nextIndex] = updatedNext
                                            
                                                state.toggleSupersetWithNext(current.planExercise, newSid)
                                            }
                                        }
                                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = if (isLinked) Error.copy(alpha = 0.12f) else Primary.copy(alpha = 0.12f),
                                    contentColor = if (isLinked) Error else Primary
                                ) {
                                    Icon(
                                        imageVector = if (isLinked) Icons.Rounded.LinkOff else Icons.Rounded.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isLinked) stringResource(Res.string.unlink_superset).uppercase() else stringResource(Res.string.link_with_next).uppercase(),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                }
                SheetFormFooter(horizontalPadding = ResponsiveSize.cardPadding) {
                        Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         GymButton(
                            onClick = {
                                scope.launch { exerciseSheetState.hide() }.invokeOnCompletion {
                                    if (!exerciseSheetState.isVisible) {
                                        showExerciseSheet = false
                                    }
                                }
                                editingExercise?.let {
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    state.removeExercise(it.planExercise)
                                }
                            },
                            modifier = Modifier.size(60.dp),
                            height = 56,
                            containerColor = Error.copy(alpha = 0.15f),
                            contentColor = Error,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(28.dp))
                        }

                        GymButton(
                            onClick = {
                                scope.launch { exerciseSheetState.hide() }.invokeOnCompletion {
                                    if (!exerciseSheetState.isVisible) {
                                        showExerciseSheet = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = SurfaceContainerHigh,
                            contentColor = OnSurfaceVariant
                        ) {
                            Text(stringResource(Res.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
                        }
                    
                        GymButton(
                            onClick = {
                                val exerciseId = selectedExerciseId ?: return@GymButton
                                val current = editingExercise
                                if (isSelectedCardio) {
                                    val durSec = cardioDurationText.trim().toIntOrNull()?.let { it * 60 }
                                    val cardioSets = (cardioSetsText.toIntOrNull() ?: 1).coerceIn(1, 20)
                                    val rest = restText.trim().toIntOrNull() ?: 0
                                    val category = selectedExercise?.categoria ?: "Cardio"

                                    if (current == null) {
                                        state.addCardioExercise(
                                            exerciseId = exerciseId,
                                            cardioCategoria = category,
                                            durataTargetSecondi = durSec,
                                            recuperoTarget = rest,
                                            sets = cardioSets
                                        )
                                    } else {
                                        state.updateExercise(
                                            original = current.planExercise.copy(
                                                exerciseType = "cardio",
                                                cardioCategoria = category,
                                                durataTargetSecondi = durSec
                                            ),
                                            exerciseId = exerciseId,
                                            serieTarget = cardioSets,
                                            repsTarget = "1",
                                            recuperoTarget = rest,
                                            excludedWeeks = draftExcludedWeeks.toSet(),
                                            clearAdvanced = current.isAdvanced && advancedOn
                                        )
                                    }
                                } else if (selectedExerciseType == "advanced") {
                                    val rest = restText.trim().toIntOrNull() ?: return@GymButton
                                    if (advancedWeeks.values.none { it.isNotEmpty() }) {
                                        showMessage(advancedNeedsBlockText)
                                        return@GymButton
                                    }
                                    state.saveAdvancedExercise(
                                        original = current?.planExercise,
                                        exerciseId = exerciseId,
                                        recuperoTarget = rest,
                                        blocksByWeek = advancedWeeks.toMap(),
                                        excludedWeeks = draftExcludedWeeks.toSet(),
                                        weeksCount = draftWeeksCount
                                    )
                                } else if (selectedExerciseType == "time_and_weight") {
                                    val sets = setsText.trim().toIntOrNull() ?: return@GymButton
                                    val rest = restText.trim().toIntOrNull() ?: return@GymButton
                                    val targetSec = timeTargetSecondsText.trim().toIntOrNull() ?: 45

                                    if (current == null) {
                                        state.addExercise(
                                            exerciseId = exerciseId,
                                            serieTarget = sets,
                                            repsTarget = "${targetSec}s",
                                            recuperoTarget = rest,
                                            exerciseType = "time_and_weight",
                                            durataTargetSecondi = targetSec,
                                            excludedWeeks = draftExcludedWeeks.toSet(),
                                            weightPreset = presetKg
                                        )
                                    } else {
                                        state.updateExercise(
                                            original = current.planExercise,
                                            exerciseId = exerciseId,
                                            serieTarget = sets,
                                            repsTarget = "${targetSec}s",
                                            recuperoTarget = rest,
                                            exerciseType = "time_and_weight",
                                            durataTargetSecondi = targetSec,
                                            excludedWeeks = draftExcludedWeeks.toSet(),
                                            clearAdvanced = current.isAdvanced && advancedOn,
                                            weightPreset = presetKg
                                        )
                                    }
                                } else if (selectedExerciseType == TIME_AND_REPS) {
                                    val sets = setsText.trim().toIntOrNull() ?: return@GymButton
                                    val rest = restText.trim().toIntOrNull() ?: return@GymButton
                                    val goal = TimeAndReps.parseGoal(repsGoalText) ?: return@GymButton
                                    val targetSec = timeTargetSecondsText.trim().toIntOrNull() ?: TimeAndReps.DEFAULT_SECONDS

                                    if (current == null) {
                                        state.addExercise(
                                            exerciseId = exerciseId,
                                            serieTarget = sets,
                                            repsTarget = goal.toString(),
                                            recuperoTarget = rest,
                                            exerciseType = TIME_AND_REPS,
                                            durataTargetSecondi = targetSec,
                                            excludedWeeks = draftExcludedWeeks.toSet(),
                                            weightPreset = null
                                        )
                                    } else {
                                        state.updateExercise(
                                            original = current.planExercise,
                                            exerciseId = exerciseId,
                                            serieTarget = sets,
                                            repsTarget = goal.toString(),
                                            recuperoTarget = rest,
                                            exerciseType = TIME_AND_REPS,
                                            durataTargetSecondi = targetSec,
                                            excludedWeeks = draftExcludedWeeks.toSet(),
                                            clearAdvanced = current.isAdvanced && advancedOn,
                                            weightPreset = null
                                        )
                                    }
                                } else {
                                    val sets = setsText.trim().toIntOrNull() ?: return@GymButton
                                    val rest = restText.trim().toIntOrNull() ?: return@GymButton
                                    val reps = repsDraft.target ?: return@GymButton

                                    if (current == null) {
                                        state.addExercise(
                                            exerciseId = exerciseId,
                                            serieTarget = sets,
                                            repsTarget = reps.repsTarget,
                                            recuperoTarget = rest,
                                            exerciseType = "strength",
                                            durataTargetSecondi = null,
                                            excludedWeeks = draftExcludedWeeks.toSet(),
                                            repsMax = reps.repsMax,
                                            weightPreset = presetKg
                                        )
                                    } else {
                                        state.updateExercise(
                                            original = current.planExercise,
                                            exerciseId = exerciseId,
                                            serieTarget = sets,
                                            repsTarget = reps.repsTarget,
                                            recuperoTarget = rest,
                                            exerciseType = "strength",
                                            durataTargetSecondi = null,
                                            excludedWeeks = draftExcludedWeeks.toSet(),
                                            clearAdvanced = current.isAdvanced && advancedOn,
                                            repsMax = reps.repsMax,
                                            weightPreset = presetKg
                                        )
                                    }
                                }
                                scope.launch { exerciseSheetState.hide() }.invokeOnCompletion {
                                    if (!exerciseSheetState.isVisible) {
                                        showExerciseSheet = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = canSaveExercise
                        ) {
                            Text(
                                text = if (editingExercise == null) stringResource(Res.string.add).uppercase() else stringResource(Res.string.save).uppercase(),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExercisePicker) {
        ExercisePickerBottomSheet(
            exercises = uiState.availableExercises,
            categories = uiState.categories,
            onExerciseSelected = { exercise ->
                selectedExerciseId = exercise.id
                // Adding opens on the type this exercise was last added with; editing keeps the type being edited
                if (editingExerciseId == null) selectedExerciseType = ExerciseTypeMemory.typeFor(rememberedTypes, exercise.id)
                showExercisePicker = false
                showExerciseSheet = true
            },
            onAddCustomExercise = { nome, categoria, onCreated ->
                state.addCustomExercise(nome, categoria, onCreated)
            },
            onDismiss = { showExercisePicker = false },
            languageCode = languageCode,
        )
    }

    if (showRoutineEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRoutineEditSheet = false },
            sheetState = routineSheetState,
            containerColor = Surface,
            contentColor = OnSurface,
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(OnSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        ) {
            SheetFormLayout {
                SheetFormBody(horizontalPadding = ResponsiveSize.cardPadding, spacing = 24.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.edit_routine),
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = stringResource(Res.string.update_plan),
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.headlineMedium.fontSize)),
                            color = OnSurface,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        GymInputField(
                            value = routineName,
                            onValueChange = { routineName = it },
                            label = stringResource(Res.string.routine_name),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).clickable { showStartDatePicker = true }) {
                                GymInputField(
                                    value = com.emanuel5014.trainable.ui.util.DateFormatter.format(startDate),
                                    onValueChange = {},
                                    label = stringResource(Res.string.start_date).replace(":", ""),
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    enabled = false,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarMonth,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }

                            Box(modifier = Modifier.weight(1f).clickable { showEndDatePicker = true }) {
                                GymInputField(
                                    value = endDate?.let { com.emanuel5014.trainable.ui.util.DateFormatter.format(it) } ?: stringResource(Res.string.tap_to_set),
                                    onValueChange = {},
                                    label = stringResource(Res.string.expiration_date),
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    enabled = false,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarMonth,
                                            contentDescription = null,
                                            tint = if (endDate != null) Primary else OnSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(Res.string.schedule_days),
                                style = MaterialTheme.typography.labelMedium,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                DayOfWeek.entries.forEachIndexed { index, day ->
                                    val isSelected = selectedDays.contains(day)
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            if (isSelected) selectedDays.remove(day)
                                            else selectedDays.add(day)
                                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            DayOfWeek.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        }
                                    ) {
                                        Text(
                                            text = narrowDayName(day),
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = ResponsiveSize.responsiveFontSize(MaterialTheme.typography.bodyLarge.fontSize)),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        if (advancedOn) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(Res.string.weekly_program),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = OnSurfaceVariant,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = stringResource(Res.string.weekly_program_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                    Switch(
                                        checked = routineWeeksCount > 1,
                                        onCheckedChange = { enabled ->
                                            routineWeeksCount = if (enabled) maxOf(4, planWeeksCount) else 1
                                            routineCurrentWeek = routineCurrentWeek.coerceAtMost(routineWeeksCount)
                                        },
                                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                                    )
                                }
                                AnimatedVisibility(visible = routineWeeksCount > 1) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        NumberStepper(
                                            value = routineWeeksCount.toFloat(),
                                            step = 1f,
                                            range = 2f..RoutineDetailState.MAX_WEEKS.toFloat(),
                                            label = stringResource(Res.string.weeks_count),
                                            onValueChange = {
                                                routineWeeksCount = it.toInt()
                                                routineCurrentWeek = routineCurrentWeek.coerceAtMost(routineWeeksCount)
                                            }
                                        )
                                        NumberStepper(
                                            value = routineCurrentWeek.toFloat(),
                                            step = 1f,
                                            range = 1f..routineWeeksCount.toFloat().coerceAtLeast(1f),
                                            label = stringResource(Res.string.program_current_week),
                                            onValueChange = { routineCurrentWeek = it.toInt() }
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(Res.string.auto_advance_week),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = OnSurface,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = stringResource(Res.string.auto_advance_week_desc),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = OnSurfaceVariant
                                                )
                                            }
                                            Switch(
                                                checked = routineAutoAdvance,
                                                onCheckedChange = { routineAutoAdvance = it },
                                                colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                                            )
                                        }
                                    }
                                }
                            }
    }

                        GymInputField(
                            value = routineNote,
                            onValueChange = { routineNote = it },
                            label = stringResource(Res.string.routine_notes),
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                SheetFormFooter(horizontalPadding = ResponsiveSize.cardPadding) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GymButton(
                            onClick = {
                                scope.launch { routineSheetState.hide() }.invokeOnCompletion {
                                    if (!routineSheetState.isVisible) {
                                        showRoutineEditSheet = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = SurfaceContainerHigh,
                            contentColor = OnSurfaceVariant
                        ) {
                            Text(stringResource(Res.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
                        }
                    
                        GymButton(
                            onClick = {
                                val trimmedName = routineName.trim()
                                if (trimmedName.isNotEmpty()) {
                                    val note = routineNote.trim().takeIf { it.isNotBlank() }
                                    val daysString = if (selectedDays.isEmpty()) null 
                                                   else selectedDays.sortedBy { it.isoDayNumber }.joinToString(",") { it.isoDayNumber.toString() }
                                    state.updatePlan(
                                        nome = trimmedName,
                                        note = note,
                                        giorniSettimana = daysString,
                                        dataInizio = startDate,
                                        dataFine = endDate,
                                        weeksCount = routineWeeksCount,
                                        currentWeek = routineCurrentWeek,
                                        autoAdvanceWeek = routineAutoAdvance
                                    )
                                    scope.launch { routineSheetState.hide() }.invokeOnCompletion {
                                        if (!routineSheetState.isVisible) {
                                            showRoutineEditSheet = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(Res.string.save).uppercase(),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate ?: Clock.System.now().toEpochMilliseconds())

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                GymButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { startDate = it }
                        showStartDatePicker = false
                    },
                    containerColor = Color.Transparent,
                    contentColor = Primary
                ) {
                    Text(stringResource(Res.string.confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = { showStartDatePicker = false },
                    containerColor = Color.Transparent,
                    contentColor = OnSurfaceVariant
                ) {
                    Text(stringResource(Res.string.cancel).uppercase())
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                GymButton(
                    onClick = {
                        endDate = endDatePickerState.selectedDateMillis
                        showEndDatePicker = false
                    },
                    containerColor = Color.Transparent,
                    contentColor = Primary
                ) {
                    Text(stringResource(Res.string.confirm).uppercase(), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                GymButton(
                    onClick = {
                        endDate = null
                        showEndDatePicker = false
                    },
                    containerColor = Color.Transparent,
                    contentColor = Error
                ) {
                    Text(stringResource(Res.string.reset).uppercase())
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    // The scan's own sheets, dialogs and overlay, over everything else
    scan.Overlays(hazeState = scanHazeState, isDark = isDark, viewWeek = viewWeek)
}

@Composable
private fun RestSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    hapticEnabled: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    val steps = listOf(0, 30, 60, 90, 120, 180, 240, 300)
    val currentIndex = remember(value) { steps.indexOf(value).coerceAtLeast(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.rest_seconds),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${value}s",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = formatRestTime(value),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )
        }
        
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { rawValue ->
                val index = kotlin.math.round(rawValue).toInt()
                val clampedIndex = index.coerceIn(0, steps.size - 1)
                if (clampedIndex != currentIndex) {
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(steps[clampedIndex])
                }
            },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SurfaceContainerHighest
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun formatRestTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return when {
        minutes == 0 -> "${secs}s"
        secs == 0 -> "${minutes}m"
        else -> "${minutes}m ${secs}s"
    }
}

@Composable
private fun CardioDurationSlider(
    valueMinutes: Int,
    onValueChange: (Int) -> Unit,
    hapticEnabled: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier
) {
    val steps = listOf(5, 10, 15, 20, 25, 30, 45, 60, 90, 120)
    val closestIndex = remember(valueMinutes) {
        steps.indexOf(steps.minByOrNull { kotlin.math.abs(it - valueMinutes) } ?: 20).coerceAtLeast(0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.cardio_duration_slider),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${valueMinutes} min",
                style = MaterialTheme.typography.titleMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold
            )
            // The value is in minutes: from an hour up it is also written in hours ("1h 30m")
            val displayHours = valueMinutes / 60
            val displayMins = valueMinutes % 60
            if (displayHours > 0) {
                Text(
                    text = if (displayMins == 0) "${displayHours}h" else "${displayHours}h ${displayMins}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant
                )
            }
        }

        Slider(
            value = closestIndex.toFloat(),
            onValueChange = { rawValue ->
                val index = kotlin.math.round(rawValue).toInt()
                val clampedIndex = index.coerceIn(0, steps.size - 1)
                if (clampedIndex != closestIndex) {
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(steps[clampedIndex])
                }
            },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = Primary,
                activeTrackColor = Primary,
                inactiveTrackColor = SurfaceContainerHighest
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DropMenuItem(
    text: String,
    category: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(text, style = MaterialTheme.typography.bodyLarge)
                Text(category.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }
        },
        onClick = onClick
    )
}

/**
 * Top of the routine screen. Compact: the dates and a slim strip of scheduled days. Expanded: photos above, bigger
 * day pills, the note and the muscle groups below. Tapping the card toggles it.
 */
@Composable
private fun RoutineOverviewSection(
    startDate: Long,
    endDate: Long?,
    scheduledDaysCsv: String?,
    note: String?,
    muscleGroups: List<String>,
    images: List<com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onImageAdd: (String) -> Unit,
    onImageRemove: (com.emanuel5014.trainable.data.local.entity.WorkoutPlanImageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val expandSpec = spring<IntSize>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    val enter = expandVertically(animationSpec = expandSpec) + fadeIn(tween(180))
    val exit = shrinkVertically(animationSpec = expandSpec) + fadeOut(tween(120))
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "overview_chevron"
    )
    val dayHeight by animateDpAsState(
        targetValue = if (expanded) 36.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "overview_day_height"
    )
    val scheduledDays = remember(scheduledDaysCsv) {
        scheduledDaysCsv.orEmpty().split(",").mapNotNull {
            it.toIntOrNull()?.let { value -> DayOfWeek(value) }
        }
    }
    val cleanNote = note?.takeIf { it.isNotBlank() }
    val toggleLabel = stringResource(if (expanded) Res.string.routine_header_collapse else Res.string.routine_header_expand)

    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = expanded, enter = enter, exit = exit) {
            LocalRoutineImagePicker.current(images, onImageAdd, onImageRemove, Modifier.padding(bottom = 12.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.large)
                .background(SurfaceContainerHigh)
                .clickable(onClickLabel = toggleLabel, onClick = onToggle)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.start_date) + " " + com.emanuel5014.trainable.ui.util.DateFormatter.format(startDate),
                        style = MaterialTheme.typography.labelLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    endDate?.let { expiry ->
                        Text(
                            text = stringResource(Res.string.expires) + " " + com.emanuel5014.trainable.ui.util.DateFormatter.format(expiry),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (expiry < Clock.System.now().toEpochMilliseconds()) Error else OnSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }

            if (!scheduledDaysCsv.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    DayOfWeek.entries.forEachIndexed { index, day ->
                        val isScheduled = scheduledDays.contains(day)
                        val dayShape = if (isScheduled) RoundedCornerShape(50)
                        else when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes().shape
                            DayOfWeek.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes().shape
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes().shape
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = dayHeight)
                                .clip(dayShape)
                                .background(if (isScheduled) Primary else SurfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = narrowDayName(day),
                                style = if (expanded) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isScheduled) OnPrimary else OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded && (cleanNote != null || muscleGroups.isNotEmpty()),
                enter = enter,
                exit = exit
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    cleanNote?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    if (muscleGroups.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            muscleGroups.forEach { category ->
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Tertiary,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Tertiary.copy(alpha = 0.16f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
