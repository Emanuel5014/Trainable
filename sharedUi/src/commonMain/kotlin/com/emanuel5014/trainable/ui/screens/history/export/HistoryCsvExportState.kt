package com.emanuel5014.trainable.ui.screens.history.export

import com.emanuel5014.trainable.data.ExerciseTranslations
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.RoutineGroupEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.ui.screens.history.HistoryFilterCounts
import com.emanuel5014.trainable.ui.screens.history.RoutinePickerBlock
import com.emanuel5014.trainable.ui.screens.history.routinePickerBlocks
import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.data.local.relation.SessionWithDetails
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvFileName
import com.emanuel5014.trainable.domain.csvimport.HistoryCsvRowFilter
import com.emanuel5014.trainable.domain.exercise.CardioNames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** An exercise of the chosen workouts, with in how many of them it is. */
data class ExportExerciseOption(val id: Int, val name: String, val category: String, val workouts: Int)

data class HistoryCsvExportUi(
    val loading: Boolean = true,
    val selection: HistoryCsvSelection = HistoryCsvSelection(),
    /** The routines with workouts, in the blocks of the history's routine filter (groups, active and archived). */
    val routineBlocks: List<RoutinePickerBlock> = emptyList(),
    /** Workouts of no routine the rest of the choice keeps; null when the history has none at all. */
    val noRoutineWorkouts: Int? = null,
    /** All workouts the period keeps, whatever the routine (the count of "All"). */
    val anyRoutineWorkouts: Int = 0,
    val exercises: List<ExportExerciseOption> = emptyList(),
    /** The exercises picked, also those the rest of the choice no longer has (they then count 0). */
    val pickedExercises: List<ExportExerciseOption> = emptyList(),
    /** What the file will hold. */
    val workouts: Int = 0,
    val rows: Int = 0,
    /** Deload workouts and warm-up sets in the choice, for the hints of their switches. */
    val deloadWorkouts: Int = 0,
    val warmupSets: Int = 0,
    val fileName: String = "",
    val exporting: Boolean = false
)

/**
 * The export sheet's state: the [HistoryCsvSelection] being made, what it would put in the file, and the file itself.
 * [repository] also writes the CSV, so on iOS it is the one whose platform names the techniques in the app's language.
 */
class HistoryCsvExportState(
    private val repository: WorkoutRepository,
    private val preferences: UserPreferencesRepository,
    private val languageCode: String,
    initial: HistoryCsvSelection,
    private val scope: CoroutineScope,
    /** The label of the workouts of no routine, for the file name. */
    private val noRoutineLabel: String = ""
) {
    private val _ui = MutableStateFlow(HistoryCsvExportUi(selection = initial))
    val ui: StateFlow<HistoryCsvExportUi> = _ui.asStateFlow()

    private val selection = MutableStateFlow(initial)

    /** What the file of the current choice holds, kept for [csv]. */
    private var exported: List<SessionWithDetails> = emptyList()

    private data class Inputs(
        val sessions: List<SessionWithDetails>,
        val groups: List<RoutineGroupEntity>,
        val exercises: Map<Int, ExerciseEntity>
    )

    init {
        val inputs = combine(
            repository.getAllSessionsWithDetails(),
            repository.getRoutineGroups(),
            repository.getExercisesById()
        ) { sessions, groups, exercises -> Inputs(sessions, groups, exercises) }
        scope.launch {
            combine(inputs, selection) { input, choice -> input to choice }.collectLatest { (input, choice) ->
                val computed = withContext(Dispatchers.Default) { compute(input, choice) }
                exported = computed.second
                _ui.update { computed.first.copy(exporting = it.exporting) }
            }
        }
    }

    fun update(change: (HistoryCsvSelection) -> HistoryCsvSelection) {
        selection.update(change)
        _ui.update { it.copy(selection = selection.value) }
    }

    fun reset() = update { HistoryCsvSelection() }

    /** The file of the current choice. */
    suspend fun csv(): String {
        _ui.update { it.copy(exporting = true) }
        try {
            val unit = preferences.weightUnit.first()
            return repository.exportWorkoutsToCsv(exported, unit, languageCode)
        } finally {
            _ui.update { it.copy(exporting = false) }
        }
    }

    private fun compute(input: Inputs, choice: HistoryCsvSelection): Pair<HistoryCsvExportUi, List<SessionWithDetails>> {
        // What the period and the rows keep, whatever the routine: each routine's count comes from it
        val inWindow = input.sessions.filter { choice.matchesPeriod(it) && choice.rows.narrow(it) != null }
        val byPlan = inWindow.groupingBy { it.session.planId }.eachCount()

        // Only routines with workouts: the others have nothing to export
        val routinePlans = input.sessions.map { it.plan }.filter { !it.isSystemPlan }.distinctBy { it.id }
        val spans = input.sessions.filter { !it.plan.isSystemPlan }.groupBy { it.session.planId }
            .mapValues { (_, list) -> list.minOf { it.session.timestamp } to list.maxOf { it.session.timestamp } }
        val blocks = routinePickerBlocks(routinePlans, input.groups, HistoryFilterCounts(byPlan = byPlan, periodByPlan = spans))
        val hasNoRoutine = input.sessions.any { it.plan.isSystemPlan }
        val noRoutine = if (hasNoRoutine) inWindow.count { it.plan.isSystemPlan } else null

        // The exercises to pick from: those of the chosen workouts and kinds, whatever exercises are picked now
        val chosen = choice.workouts(input.sessions)
        val anyExercise = choice.rows.copy(exerciseIds = emptySet())
        val pickable = anyExercise.apply(chosen)
        val exercises = exerciseOptions(pickable, input.exercises)
        val optionsById = exercises.associateBy { it.id }
        val picked = choice.rows.exerciseIds.map { id ->
            optionsById[id] ?: input.exercises[id].let { entity ->
                ExportExerciseOption(
                    id = id,
                    name = entity?.let { ExerciseTranslations.translate(it.nome, languageCode) } ?: "#$id",
                    category = entity?.let { ExerciseTranslations.translateCategory(it.categoria, languageCode) }.orEmpty(),
                    workouts = 0
                )
            }
        }.sortedBy { it.name.lowercase() }

        val result = choice.rows.apply(chosen)
        val warmups = HistoryCsvRowFilter(kinds = choice.rows.kinds, exerciseIds = choice.rows.exerciseIds, includeDeload = choice.rows.includeDeload)
            .apply(chosen).sumOf { s -> s.sets.count { it.setLog.isWarmup } }
        val deloads = choice.rows.copy(includeDeload = true).apply(chosen).count { it.session.isDeload }

        val ui = HistoryCsvExportUi(
            loading = false,
            selection = choice,
            routineBlocks = blocks,
            noRoutineWorkouts = noRoutine,
            anyRoutineWorkouts = inWindow.size,
            exercises = exercises,
            pickedExercises = picked,
            workouts = result.size,
            rows = HistoryCsvRowFilter.rowCount(result),
            deloadWorkouts = deloads,
            warmupSets = warmups,
            fileName = fileName(choice, routinePlans, input.groups, picked, result)
        )
        return ui to result
    }

    private fun exerciseOptions(sessions: List<SessionWithDetails>, byId: Map<Int, ExerciseEntity>): List<ExportExerciseOption> {
        val workouts = mutableMapOf<Int, Int>()
        val names = mutableMapOf<Int, Pair<String, String>>()
        sessions.forEach { details ->
            val inThis = mutableSetOf<Int>()
            details.sets.forEach { set ->
                val exercise = set.exercise
                inThis += exercise.id
                names.getOrPut(exercise.id) {
                    ExerciseTranslations.translate(exercise.nome, languageCode) to ExerciseTranslations.translateCategory(exercise.categoria, languageCode)
                }
            }
            details.cardio.forEach { log ->
                val id = log.exerciseId ?: return@forEach
                inThis += id
                names.getOrPut(id) {
                    CardioNames.of(log, byId, languageCode) to ExerciseTranslations.translateCategory("Cardio", languageCode)
                }
            }
            inThis.forEach { workouts[it] = (workouts[it] ?: 0) + 1 }
        }
        return names.map { (id, label) -> ExportExerciseOption(id, label.first, label.second, workouts[id] ?: 0) }
            .sortedWith(compareByDescending<ExportExerciseOption> { it.workouts }.thenBy { it.name.lowercase() })
    }

    private fun fileName(
        choice: HistoryCsvSelection,
        routines: List<WorkoutPlanEntity>,
        groups: List<RoutineGroupEntity>,
        picked: List<ExportExerciseOption>,
        result: List<SessionWithDetails>
    ): String {
        val scope = buildList {
            groups.filter { it.id in choice.groupIds }.forEach { add(it.name) }
            routines.filter { it.id in choice.planIds }.forEach { add(it.nome) }
            if (choice.noRoutine) add(noRoutineLabel)
            if (picked.size in 1..2) picked.forEach { add(it.name) }
        }
        val span = HistoryCsvFileName.span(result)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return HistoryCsvFileName.of(scope, span?.first, span?.second, today)
    }
}
