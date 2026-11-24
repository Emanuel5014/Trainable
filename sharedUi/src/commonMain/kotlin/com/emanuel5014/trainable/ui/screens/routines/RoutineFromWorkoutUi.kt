package com.emanuel5014.trainable.ui.screens.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.TIME_AND_REPS
import com.emanuel5014.trainable.data.local.entity.TIME_AND_WEIGHT
import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.prescription.RepRange
import com.emanuel5014.trainable.domain.session.RoutineExerciseDraft
import com.emanuel5014.trainable.domain.session.RoutineFromWorkout
import com.emanuel5014.trainable.domain.session.RoutineFromWorkoutResult
import com.emanuel5014.trainable.domain.session.WorkoutTitle
import com.emanuel5014.trainable.sharedui.resources.Res
import com.emanuel5014.trainable.sharedui.resources.cancel
import com.emanuel5014.trainable.sharedui.resources.create_routine
import com.emanuel5014.trainable.sharedui.resources.new_routine
import com.emanuel5014.trainable.sharedui.resources.routine_from_cardio
import com.emanuel5014.trainable.sharedui.resources.routine_from_exercises
import com.emanuel5014.trainable.sharedui.resources.routine_from_moves_body
import com.emanuel5014.trainable.sharedui.resources.routine_from_moves_title
import com.emanuel5014.trainable.sharedui.resources.routine_from_option_body
import com.emanuel5014.trainable.sharedui.resources.routine_from_option_title
import com.emanuel5014.trainable.sharedui.resources.routine_from_or_scratch
import com.emanuel5014.trainable.sharedui.resources.routine_from_pick_empty
import com.emanuel5014.trainable.sharedui.resources.routine_from_pick_hint
import com.emanuel5014.trainable.sharedui.resources.routine_from_pick_title
import com.emanuel5014.trainable.sharedui.resources.routine_from_rest
import com.emanuel5014.trainable.sharedui.resources.routine_from_sets
import com.emanuel5014.trainable.sharedui.resources.routine_from_warmups
import com.emanuel5014.trainable.sharedui.resources.routine_from_workout_of
import com.emanuel5014.trainable.sharedui.resources.routine_name
import com.emanuel5014.trainable.sharedui.resources.save_as_routine
import com.emanuel5014.trainable.sharedui.resources.superset
import com.emanuel5014.trainable.sharedui.resources.workout_saved
import com.emanuel5014.trainable.ui.components.GymButton
import com.emanuel5014.trainable.ui.components.GymInputField
import com.emanuel5014.trainable.ui.components.SheetFormBody
import com.emanuel5014.trainable.ui.components.SheetFormFooter
import com.emanuel5014.trainable.ui.components.SheetFormLayout
import com.emanuel5014.trainable.ui.components.navBarBottomClearance
import com.emanuel5014.trainable.ui.screens.history.displayText
import com.emanuel5014.trainable.ui.screens.history.title
import com.emanuel5014.trainable.ui.theme.OnPrimary
import com.emanuel5014.trainable.ui.theme.OnSurface
import com.emanuel5014.trainable.ui.theme.OnSurfaceVariant
import com.emanuel5014.trainable.ui.theme.OutlineVariant
import com.emanuel5014.trainable.ui.theme.Primary
import com.emanuel5014.trainable.ui.theme.ResponsiveSize
import com.emanuel5014.trainable.ui.theme.Surface
import com.emanuel5014.trainable.ui.theme.SurfaceContainer
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHigh
import com.emanuel5014.trainable.ui.theme.SurfaceContainerHighest
import com.emanuel5014.trainable.ui.util.fullDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The ways into "save as routine", from anywhere in the app: the History sheet of a quick workout, the end of a quick
 * workout and "New routine". The sheets themselves live once, in [RoutineFromWorkoutHost] over the tabs.
 */
object RoutineFromWorkoutRequests {
    sealed interface Request {
        data class Save(val sessionId: Int) : Request
        data object Pick : Request
    }

    private val _requests = MutableSharedFlow<Request>(extraBufferCapacity = 4)
    val requests: SharedFlow<Request> = _requests.asSharedFlow()

    /** Opens the sheet that saves workout [sessionId] as a routine. */
    fun save(sessionId: Int) {
        _requests.tryEmit(Request.Save(sessionId))
    }

    /** Opens the list of quick workouts to make a routine from. */
    fun pick() {
        _requests.tryEmit(Request.Pick)
    }

    private val _finishedQuickWorkout = MutableStateFlow<Int?>(null)

    /** A quick workout just finished: the tabs offer to save it as a routine, once. */
    val finishedQuickWorkout: StateFlow<Int?> = _finishedQuickWorkout.asStateFlow()

    fun quickWorkoutFinished(sessionId: Int) {
        _finishedQuickWorkout.value = sessionId
    }

    fun consumeFinishedQuickWorkout() {
        _finishedQuickWorkout.value = null
    }

    private val _created = MutableStateFlow<RoutineFromWorkoutResult?>(null)

    /** The routine just made from a workout: its page offers to undo it, once. */
    val created: StateFlow<RoutineFromWorkoutResult?> = _created.asStateFlow()

    fun routineCreated(result: RoutineFromWorkoutResult) {
        _created.value = result
    }

    fun consumeCreated() {
        _created.value = null
    }
}

/** A workout that can be made into a routine, as the list of them shows it. */
data class RoutineSourceWorkout(
    val sessionId: Int,
    val timestamp: Long,
    val title: WorkoutTitle,
    val exerciseNames: List<String>,
    val exercises: Int,
    val sets: Int
)

/** The routine the sheet is about to create: from which workout, its suggested name and its exercises. */
data class RoutineFromWorkoutDraft(
    val sessionId: Int,
    val timestamp: Long,
    val suggestedName: String,
    val exercises: List<Pair<RoutineExerciseDraft, ExerciseEntity?>>
)

/** What the sheets of [RoutineFromWorkoutHost] show and do. */
class RoutineFromWorkoutState(
    private val workoutRepository: WorkoutRepository,
    private val scope: CoroutineScope
) {
    private val _draft = MutableStateFlow<RoutineFromWorkoutDraft?>(null)
    val draft: StateFlow<RoutineFromWorkoutDraft?> = _draft.asStateFlow()

    /** The quick workouts to pick from while that list is open; null when it is closed. */
    private val _sources = MutableStateFlow<List<RoutineSourceWorkout>?>(null)
    val sources: StateFlow<List<RoutineSourceWorkout>?> = _sources.asStateFlow()

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()

    fun open(sessionId: Int) {
        scope.launch {
            val details = workoutRepository.getSessionWithDetails(sessionId).first() ?: return@launch
            val drafts = workoutRepository.routineDraftsOf(sessionId)
            if (drafts.isEmpty()) return@launch
            val exercises = workoutRepository.getExercisesById().first()
            _sources.value = null
            _draft.value = RoutineFromWorkoutDraft(
                sessionId = sessionId,
                timestamp = details.session.timestamp,
                suggestedName = details.session.noteSessione?.trim().orEmpty(),
                exercises = drafts.map { it to exercises[it.exerciseId] }
            )
        }
    }

    fun openPicker() {
        scope.launch {
            _sources.value = workoutRepository.getAllSessionsWithDetails().first()
                .filter { it.plan.isSystemPlan && it.session.isFinished && (it.sets.isNotEmpty() || it.cardio.isNotEmpty()) }
                .sortedByDescending { it.session.timestamp }
                .map { details ->
                    val strength = details.sets.map { it.exercise.nome to it.setLog.ordineEsercizio }
                    val cardio = details.cardio.map { it.categoria to it.ordineEsercizio }
                    val runs = (strength + cardio).distinct().sortedBy { it.second }
                    RoutineSourceWorkout(
                        sessionId = details.session.id,
                        timestamp = details.session.timestamp,
                        title = details.title,
                        exerciseNames = runs.map { it.first }.distinct(),
                        exercises = runs.size,
                        sets = details.sets.count { !it.setLog.isWarmup } + details.cardio.size
                    )
                }
        }
    }

    suspend fun canSave(sessionId: Int): Boolean = workoutRepository.routineDraftsOf(sessionId).isNotEmpty()

    fun dismiss() {
        _draft.value = null
        _sources.value = null
    }

    fun create(name: String, onCreated: (RoutineFromWorkoutResult) -> Unit) {
        val draft = _draft.value ?: return
        if (_creating.value || name.isBlank()) return
        _creating.value = true
        scope.launch {
            val result = runCatching { workoutRepository.createRoutineFromWorkout(draft.sessionId, name) }.getOrNull()
            _creating.value = false
            _draft.value = null
            if (result != null) onCreated(result)
        }
    }

    suspend fun undo(result: RoutineFromWorkoutResult) = workoutRepository.undoRoutineFromWorkout(result)
}

/**
 * Lives over the tabs: offers to save a quick workout just finished, and holds the list of quick workouts and the
 * "save as routine" sheet, whichever screen asked for them ([RoutineFromWorkoutRequests]).
 */
@Composable
fun RoutineFromWorkoutHost(
    state: RoutineFromWorkoutState,
    languageCode: String,
    onOpenRoutine: (planId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val draft by state.draft.collectAsState()
    val sources by state.sources.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val savedText = stringResource(Res.string.workout_saved)
    val saveText = stringResource(Res.string.save_as_routine)

    LaunchedEffect(state) {
        RoutineFromWorkoutRequests.requests.collect { request ->
            when (request) {
                is RoutineFromWorkoutRequests.Request.Save -> state.open(request.sessionId)
                RoutineFromWorkoutRequests.Request.Pick -> state.openPicker()
            }
        }
    }
    LaunchedEffect(state) {
        // Collected, not keyed on the value: taking it back must not cancel the snackbar on screen
        RoutineFromWorkoutRequests.finishedQuickWorkout.collect { finished ->
            val sessionId = finished ?: return@collect
            RoutineFromWorkoutRequests.consumeFinishedQuickWorkout()
            // Nothing done, nothing to keep
            if (!state.canSave(sessionId)) return@collect
            val result = snackbar.showSnackbar(savedText, actionLabel = saveText, withDismissAction = true, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) state.open(sessionId)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarBottomClearance())
        )
    }

    sources?.let { list ->
        RoutineSourcePickerSheet(
            sources = list,
            languageCode = languageCode,
            onPick = { state.open(it.sessionId) },
            onDismiss = state::dismiss
        )
    }
    draft?.let { current ->
        val creating by state.creating.collectAsState()
        SaveAsRoutineSheet(
            draft = current,
            languageCode = languageCode,
            creating = creating,
            onCreate = { name ->
                state.create(name) { result ->
                    RoutineFromWorkoutRequests.routineCreated(result)
                    onOpenRoutine(result.planId)
                }
            },
            onDismiss = state::dismiss
        )
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(CircleShape)
            .background(OnSurfaceVariant.copy(alpha = 0.4f))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveAsRoutineSheet(
    draft: RoutineFromWorkoutDraft,
    languageCode: String,
    creating: Boolean,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable(draft.sessionId) { mutableStateOf(draft.suggestedName) }
    val exercises = draft.exercises.map { it.first }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        tonalElevation = 0.dp,
        dragHandle = { SheetHandle() }
    ) {
        SheetFormLayout {
            SheetFormBody(horizontalPadding = ResponsiveSize.horizontalPadding, spacing = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(Res.string.save_as_routine),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(Res.string.routine_from_workout_of, dateInSentence(draft.timestamp, languageCode)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
                GymInputField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(Res.string.routine_name),
                    modifier = Modifier.fillMaxWidth()
                )
                MovesNotice()
                Text(
                    text = (pluralStringResource(Res.plurals.routine_from_exercises, exercises.size, exercises.size) + " · " +
                        RoutineFromWorkout.setCount(exercises).let { pluralStringResource(Res.plurals.routine_from_sets, it, it) }).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold
                )
                DraftList(draft.exercises, languageCode)
            }
            SheetFormFooter(horizontalPadding = ResponsiveSize.horizontalPadding) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GymButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        containerColor = SurfaceContainerHigh,
                        contentColor = OnSurfaceVariant
                    ) {
                        Text(stringResource(Res.string.cancel).uppercase(), fontWeight = FontWeight.ExtraBold)
                    }
                    GymButton(
                        onClick = { onCreate(name) },
                        enabled = name.isNotBlank() && !creating,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.create_routine), fontWeight = FontWeight.Black, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun MovesNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.Link, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(Res.string.routine_from_moves_title),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(Res.string.routine_from_moves_body),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
        }
    }
}

/** The exercises as joined rows; the exercises of a superset share one row under its label. */
@Composable
private fun DraftList(exercises: List<Pair<RoutineExerciseDraft, ExerciseEntity?>>, languageCode: String) {
    val groups = remember(exercises) {
        val result = mutableListOf<List<IndexedValue<Pair<RoutineExerciseDraft, ExerciseEntity?>>>>()
        exercises.withIndex().forEach { item ->
            val key = item.value.first.supersetKey
            val last = result.lastOrNull()
            if (key != null && last != null && last.last().value.first.supersetKey == key) {
                result[result.lastIndex] = last + item
            } else {
                result.add(listOf(item))
            }
        }
        result
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        groups.forEachIndexed { index, group ->
            val top = if (index == 0) 20.dp else 4.dp
            val bottom = if (index == groups.lastIndex) 20.dp else 4.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
                    .background(SurfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (group.size > 1) {
                    Text(
                        text = stringResource(Res.string.superset),
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .border(1.dp, Primary.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                group.forEach { (position, item) ->
                    DraftRow(position + 1, item.first, item.second, languageCode, highlighted = position == 0)
                }
            }
        }
    }
}

@Composable
private fun DraftRow(number: Int, draft: RoutineExerciseDraft, exercise: ExerciseEntity?, languageCode: String, highlighted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (highlighted) Primary else SurfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlighted) OnPrimary else OnSurface
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise?.let { ExerciseTranslations.translate(it.nome, languageCode) }.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = draftDetail(draft),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            if (draft.warmupsLeftOut > 0) {
                Text(
                    text = pluralStringResource(Res.plurals.routine_from_warmups, draft.warmupsLeftOut, draft.warmupsLeftOut),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

/** "4 × 10-8-7-6 · rest 2:00", "3 × 45 s", "Cardio · 20 min · 2.5 km". */
@Composable
private fun draftDetail(draft: RoutineExerciseDraft): String {
    if (draft.isCardio) {
        val time = draft.durationSeconds?.let { minutesText(it) }
        val timed = if (draft.sets > 1) listOfNotNull("${draft.sets} ×", time).joinToString(" ") else time
        return listOfNotNull(stringResource(Res.string.routine_from_cardio), timed, draft.distanceKm?.let { kmText(it) })
            .joinToString(" · ")
    }
    val reps = when (draft.exerciseType) {
        TIME_AND_WEIGHT -> "${draft.durationSeconds ?: 0} s"
        TIME_AND_REPS -> "${draft.repsTarget} / ${draft.durationSeconds ?: 0} s"
        else -> RepRange.of(draft.repsTarget.toIntOrNull(), draft.repsMax)?.text ?: draft.repsTarget
    }
    return "${draft.sets} × $reps · " + stringResource(Res.string.routine_from_rest, restText(draft.restSeconds))
}

/** The full date inside a sentence: weekdays are not capitalised there in Italian, Spanish, French and Portuguese. */
private fun dateInSentence(timestamp: Long, languageCode: String): String {
    val date = fullDate(timestamp)
    return if (languageCode in setOf("it", "es", "fr", "pt")) date.replaceFirstChar { it.lowercase() } else date
}

private fun restText(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

private fun kmText(km: Float): String {
    val tenths = kotlin.math.round(km * 10).toInt()
    return if (tenths % 10 == 0) "${tenths / 10} km" else "${tenths / 10}.${tenths % 10} km"
}

private fun minutesText(seconds: Int): String =
    if (seconds % 60 == 0) "${seconds / 60} min" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')} min"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineSourcePickerSheet(
    sources: List<RoutineSourceWorkout>,
    languageCode: String,
    onPick: (RoutineSourceWorkout) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        tonalElevation = 0.dp,
        dragHandle = { SheetHandle() }
    ) {
        SheetFormLayout {
            SheetFormBody(horizontalPadding = ResponsiveSize.horizontalPadding, spacing = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.new_routine).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(Res.string.routine_from_pick_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = stringResource(if (sources.isEmpty()) Res.string.routine_from_pick_empty else Res.string.routine_from_pick_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    sources.forEachIndexed { index, source ->
                        val top = if (index == 0) 24.dp else 4.dp
                        val bottom = if (index == sources.lastIndex) 24.dp else 4.dp
                        SourceRow(
                            source = source,
                            languageCode = languageCode,
                            shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
                            onClick = { onPick(source) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SourceRow(source: RoutineSourceWorkout, languageCode: String, shape: RoundedCornerShape, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = source.title.displayText(),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = fullDate(source.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
            Text(
                text = source.exerciseNames.joinToString(", ") { ExerciseTranslations.translate(it, languageCode) },
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pluralStringResource(Res.plurals.routine_from_exercises, source.exercises, source.exercises) + " · " +
                    pluralStringResource(Res.plurals.routine_from_sets, source.sets, source.sets),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = OnSurfaceVariant)
    }
}

/** "From a quick workout" on top of the New routine sheet, then "or from scratch" above the empty form. */
@Composable
fun RoutineFromWorkoutOption(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainer)
                .border(1.dp, Primary.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = Primary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.routine_from_option_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface
                )
                Text(
                    text = stringResource(Res.string.routine_from_option_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = OnSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).height(1.dp).background(OutlineVariant))
            Text(
                text = stringResource(Res.string.routine_from_or_scratch),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.ExtraBold
            )
            Box(Modifier.weight(1f).height(1.dp).background(OutlineVariant))
        }
    }
}
