package com.emanuel5014.trainable.domain.csvimport

import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.WorkoutPlanEntity
import com.emanuel5014.trainable.data.local.entity.isSystemPlan
import com.emanuel5014.trainable.util.WeightUnitConverter
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/** Plans the app creates for itself to hold workouts that belong to no routine. */
enum class SystemPlanKind(val planName: String, internal val aliases: Set<String>) {
    QUICK("Quick Workout", setOf("quick workout", "allenamento veloce")),
    CUSTOM("Custom Workout", setOf("custom workout")),
    CARDIO("Cardio", setOf("cardio"))
}

/** Where an imported workout goes. */
sealed interface RoutineTarget {
    data class Existing(val plan: WorkoutPlanEntity) : RoutineTarget

    /** A routine named in the file that does not exist yet and will be created. */
    data class Create(val name: String, val key: String) : RoutineTarget

    /** [plan] is null while the device has not created that system plan yet (it is, at import). */
    data class System(val kind: SystemPlanKind, val plan: WorkoutPlanEntity?) : RoutineTarget
}

data class ImportOptions(
    /** The unit the weights of the file are written in. */
    val weightUnit: String = "kg",
    /** Leave out workouts that are already in the app. */
    val skipDuplicates: Boolean = true,
    /** Give a routine created by the import the exercises of its most recent imported workout. */
    val fillNewRoutines: Boolean = true,
    /** Routines named in the file that the user does not want created (their workouts become quick workouts). */
    val declinedRoutines: Set<String> = emptySet()
)

/** What the device already holds that an import is matched against. */
data class KnownData(
    val exercises: List<ExerciseEntity>,
    val customCategories: List<String>,
    val plans: List<WorkoutPlanEntity>,
    /** Finished workouts, to tell what an import would add twice. */
    val sessions: List<KnownSession>
)

/** A set reduced to what tells two copies of a workout apart. */
data class SetSignature(val key: String, val weightKg: Float, val reps: Int, val seconds: Int, val km: Float)

data class KnownSession(val id: Int, val planId: Int, val date: LocalDate, val sets: List<SetSignature>)

data class PlannedSet(
    val number: Int,
    val weightKg: Float,
    val reps: Int,
    /** Null for a plain strength set. */
    val durationSeconds: Int?,
    val distanceKm: Float,
    val note: String?,
    val rpe: Float?,
    val warmup: Boolean,
    val parts: List<Int>,
    val pauses: List<Int>,
    val assisted: Int,
    val partials: Int
)

/** One exercise as done in one workout: its sets in order. */
data class PlannedEntry(
    val exercise: ResolvedExercise,
    val kind: RowKind,
    val order: Int,
    val sets: List<PlannedSet>
)

data class PlannedWorkout(
    val key: String,
    val date: LocalDate,
    val timestamp: Long,
    /** The "Session ID" and "Plan" cells as the file wrote them. */
    val sessionLabel: String,
    val planLabel: String,
    val routine: RoutineTarget,
    /** The name the workout is shown with when it has no routine of that name (a declined routine). */
    val sessionNote: String?,
    val programWeek: Int?,
    /** How long the workout took, when the file says. */
    val durationMs: Long?,
    val entries: List<PlannedEntry>,
    /** The workout already in the app that this one repeats. */
    val duplicateOf: Int?
) {
    val isDuplicate: Boolean get() = duplicateOf != null
    val setCount: Int get() = entries.sumOf { it.sets.size }
    val volumeKg: Float
        get() = entries.filter { it.kind == RowKind.STRENGTH }.sumOf { entry ->
            entry.sets.filter { !it.warmup }.sumOf { (it.weightKg * it.reps).toDouble() }
        }.toFloat()
}

/** An exercise of a routine the import creates. */
data class TemplateExercise(
    val exercise: ResolvedExercise,
    val kind: RowKind,
    val sets: Int,
    val repsTarget: String,
    val durationSeconds: Int?
)

enum class RoutineStatus { EXISTING, NEW, DECLINED }

data class PlannedRoutine(
    val key: String,
    val name: String,
    val status: RoutineStatus,
    val existing: WorkoutPlanEntity?,
    val workouts: Int,
    val firstDate: LocalDate,
    val lastDate: LocalDate,
    /** What a NEW routine is created with (empty when the option is off, or for the others). */
    val template: List<TemplateExercise>
)

/** [fileName] is how the file first wrote the exercise, which can differ from the one it was matched to. */
data class ExerciseUse(val exercise: ResolvedExercise, val sets: Int, val workouts: Int, val fileName: String)

data class ImportPlan(
    val options: ImportOptions,
    /** Oldest first, duplicates included. */
    val workouts: List<PlannedWorkout>,
    /** Of the workouts that are imported. */
    val exercises: List<ExerciseUse>,
    val routines: List<PlannedRoutine>,
    /** Workouts that go to the app's own plans, by plan. */
    val systemWorkouts: Map<SystemPlanKind, Int>,
    val newCategories: List<String>,
    val issues: List<ImportIssue>,
    val skippedRows: Int,
    val recordCount: Int
) {
    val toImport: List<PlannedWorkout> = workouts.filter { !it.isDuplicate || !options.skipDuplicates }
    val duplicates: List<PlannedWorkout> = workouts.filter { it.isDuplicate }
    val newExercises: List<ExerciseUse> = exercises.filter { it.exercise is ResolvedExercise.New }
    val existingExercises: List<ExerciseUse> = exercises.filter { it.exercise is ResolvedExercise.Existing }
    val setCount: Int = toImport.sumOf { it.setCount }
    val volumeKg: Float = toImport.sumOf { it.volumeKg.toDouble() }.toFloat()
    val errorCount: Int = issues.count { it.severity == IssueSeverity.ERROR }
    val warningCount: Int = issues.count { it.severity == IssueSeverity.WARNING }
    val firstDate: LocalDate? = toImport.minOfOrNull { it.date }
    val lastDate: LocalDate? = toImport.maxOfOrNull { it.date }
    val newRoutines: List<PlannedRoutine> = routines.filter { it.status == RoutineStatus.NEW }
}

/**
 * Turns the rows read from a CSV into what an import would do: which workouts it adds, to which routines, with which
 * exercises (existing or new), and what it leaves out. It only decides: nothing is written, so the same plan is what
 * the preview shows and what is then applied, and choosing another option simply plans again.
 */
object HistoryImportPlanner {
    private const val DEFAULT_SETS_LIMIT = 20

    private class ValidRow(val row: ParsedRow, val exercise: ResolvedExercise, val kind: RowKind, val set: PlannedSet)

    private class Instance(val key: String, val exercise: ResolvedExercise, val kind: RowKind, val order: Int) {
        val rows = mutableListOf<ValidRow>()
        var highestGiven = 0
    }

    fun plan(
        history: ParsedHistory,
        known: KnownData,
        options: ImportOptions,
        zone: ZoneId = ZoneId.systemDefault()
    ): ImportPlan {
        val issues = history.issues.toMutableList()
        val resolver = HistoryExerciseResolver(known.exercises, known.customCategories)
        val valid = history.rows.mapNotNull { validate(it, resolver, options, issues) }

        val userPlans = known.plans.filter { !it.isSystemPlan }
            .groupBy { HistoryCsvColumns.fold(it.nome) }
            .mapValues { (_, plans) -> plans.sortedWith(compareByDescending<WorkoutPlanEntity> { it.isActive }.thenBy { it.ordine }.thenBy { it.id }).first() }
        // The file only names a routine, and a name can belong to several plans (an old "Push" and a new one): a
        // workout is already in the app if it is in any of them
        val plansByName = known.plans.groupBy({ HistoryCsvColumns.fold(it.nome) }, { it.id })
        val systemPlans = SystemPlanKind.entries.associateWith { kind ->
            known.plans.firstOrNull { it.isSystemPlan && HistoryCsvColumns.fold(it.nome) in kind.aliases }
        }

        // Rows of one workout: same day, same session id, same routine
        val groups = LinkedHashMap<Triple<LocalDate, String, String>, MutableList<ValidRow>>()
        valid.forEach { groups.getOrPut(Triple(it.row.date, it.row.sessionId, HistoryCsvColumns.fold(it.row.plan))) { mutableListOf() }.add(it) }

        val draft = groups.entries.mapIndexed { index, (key, rows) ->
            Draft(index, key.first, key.second, rows.first().row.plan, rows, buildEntries(rows))
        }.sortedWith(compareBy<Draft>({ it.date }, { it.time ?: LocalTime.NOON }, { it.sessionNumber ?: Long.MAX_VALUE }, { it.index }))

        // Several workouts of one day keep their order: noon, then a minute apart unless the file gives a time
        val perDay = mutableMapOf<LocalDate, Int>()
        val workouts = draft.map { d ->
            val slot = perDay.merge(d.date, 1, Int::plus)!! - 1
            val time = d.time ?: LocalTime.NOON.plusMinutes(slot.coerceAtMost(600).toLong())
            val target = targetFor(d.plan, userPlans, systemPlans, options)
            val note = when {
                d.plan.isBlank() -> null
                target is RoutineTarget.System && target.kind == SystemPlanKind.QUICK && !isSystemName(d.plan) -> d.plan
                else -> null
            }
            val plan = (target as? RoutineTarget.Existing)?.plan
            val week = d.rows.firstNotNullOfOrNull { it.row.week }
                ?.takeIf { plan != null && plan.weeksCount > 1 && it in 1..plan.weeksCount }
            val entries = d.entries
            PlannedWorkout(
                key = "${d.date}|${d.sessionId}|${HistoryCsvColumns.fold(d.plan)}",
                date = d.date,
                timestamp = d.date.atTime(time).atZone(zone).toInstant().toEpochMilli(),
                sessionLabel = d.sessionId,
                planLabel = d.plan,
                routine = target,
                sessionNote = note,
                programWeek = week,
                durationMs = d.rows.firstNotNullOfOrNull { it.row.sessionDurationMs },
                entries = entries,
                duplicateOf = duplicateOf(d.date, target, entries, known.sessions, plansByName)
            )
        }

        val importing = workouts.filter { !it.isDuplicate || !options.skipDuplicates }
        val typedNames = valid.groupBy({ it.exercise.key }, { it.row.exercise }).mapValues { it.value.first() }
        val exercises = importing.flatMap { w -> w.entries.map { w to it } }
            .groupBy { (_, entry) -> entry.exercise.key }
            .map { (_, list) ->
                val exercise = list.first().second.exercise
                ExerciseUse(exercise, list.sumOf { it.second.sets.size }, list.map { it.first.key }.distinct().size, typedNames[exercise.key] ?: exerciseName(exercise))
            }
            .sortedWith(compareBy({ it.exercise is ResolvedExercise.Existing }, { exerciseName(it.exercise).lowercase() }))
        val newCategories = exercises.map { it.exercise }.filterIsInstance<ResolvedExercise.New>()
            .filter { it.categoryIsNew }.map { it.category }.distinctBy { HistoryCsvColumns.fold(it) }

        val skipped = issues.filter { it.severity == IssueSeverity.ERROR }.map { it.line }.distinct().size
        return ImportPlan(
            options = options,
            workouts = workouts,
            exercises = exercises,
            routines = routinesOf(importing, options),
            systemWorkouts = importing.mapNotNull { (it.routine as? RoutineTarget.System)?.kind }
                .groupingBy { it }.eachCount().toSortedMap(),
            newCategories = newCategories,
            issues = issues.sortedWith(compareBy({ it.line }, { it.severity })),
            skippedRows = skipped,
            recordCount = history.recordCount
        )
    }

    fun exerciseName(exercise: ResolvedExercise): String = when (exercise) {
        is ResolvedExercise.Existing -> exercise.exercise.nome
        is ResolvedExercise.New -> exercise.name
    }

    // --- Rows ---

    private fun validate(row: ParsedRow, resolver: HistoryExerciseResolver, options: ImportOptions, issues: MutableList<ImportIssue>): ValidRow? {
        fun reject(code: IssueCode, column: CsvColumn? = null, value: String = ""): ValidRow? {
            issues.add(ImportIssue(row.line, IssueSeverity.ERROR, code, column, value))
            return null
        }

        // What the row says about being cardio: its type or category, nothing when it has neither
        val cardioHint: Boolean? = when {
            row.declaredKind == RowKind.CARDIO || resolver.categoryOf(row.category).equals(ResolvedExercise.CARDIO, ignoreCase = true) -> true
            row.declaredKind != null -> false
            else -> null
        }
        val exercise = resolver.resolve(row.exercise, row.category, cardioHint)
        val kind = when {
            exercise.isCardio -> RowKind.CARDIO
            row.declaredKind == RowKind.CARDIO -> return reject(IssueCode.CARDIO_NOT_CARDIO_EXERCISE, CsvColumn.EXERCISE, row.exercise)
            row.declaredKind != null -> row.declaredKind
            row.durationSeconds != null && row.reps != null -> RowKind.TIME_AND_REPS
            row.durationSeconds != null -> RowKind.TIME_AND_WEIGHT
            row.reps == null && row.weight == null && row.distanceKm != null ->
                return reject(IssueCode.CARDIO_NOT_CARDIO_EXERCISE, CsvColumn.EXERCISE, row.exercise)
            else -> RowKind.STRENGTH
        }

        when (kind) {
            RowKind.STRENGTH -> if (row.reps == null) return reject(IssueCode.MISSING_REPS, CsvColumn.REPS)
            RowKind.TIME_AND_WEIGHT -> if (row.durationSeconds == null) return reject(IssueCode.MISSING_DURATION, CsvColumn.DURATION)
            RowKind.TIME_AND_REPS -> {
                if (row.durationSeconds == null) return reject(IssueCode.MISSING_DURATION, CsvColumn.DURATION)
                if (row.reps == null) return reject(IssueCode.MISSING_REPS, CsvColumn.REPS)
            }
            RowKind.CARDIO -> if ((row.durationSeconds ?: 0) == 0 && (row.distanceKm ?: 0f) == 0f) {
                return reject(IssueCode.MISSING_CARDIO_VALUES)
            }
        }

        // Mini-sets only make sense for a plain strength set whose reps are their sum
        var parts = emptyList<Int>()
        var pauses = emptyList<Int>()
        if (row.parts.isNotEmpty()) {
            if (kind == RowKind.STRENGTH && row.parts.sum() == row.reps) {
                parts = row.parts
                pauses = row.pauses
            } else {
                issues.add(ImportIssue(row.line, IssueSeverity.WARNING, IssueCode.IGNORED_VALUE, CsvColumn.PARTS, row.parts.joinToString("+")))
            }
        }

        val weightKg = when (kind) {
            RowKind.TIME_AND_REPS, RowKind.CARDIO -> 0f
            else -> row.weight?.let { WeightUnitConverter.convertStorage(it, options.weightUnit) } ?: 0f
        }
        val set = PlannedSet(
            number = 0,
            weightKg = weightKg,
            reps = when (kind) {
                RowKind.TIME_AND_WEIGHT, RowKind.CARDIO -> 0
                else -> row.reps ?: 0
            },
            durationSeconds = when (kind) {
                RowKind.STRENGTH -> null
                else -> row.durationSeconds ?: 0
            },
            distanceKm = if (kind == RowKind.CARDIO) row.distanceKm ?: 0f else 0f,
            note = row.note.ifBlank { null },
            rpe = if (kind == RowKind.STRENGTH || kind == RowKind.TIME_AND_WEIGHT) row.rpe else null,
            warmup = row.warmup && kind != RowKind.CARDIO,
            parts = parts,
            pauses = pauses,
            assisted = if (kind == RowKind.STRENGTH) row.assisted else 0,
            partials = if (kind == RowKind.STRENGTH) row.partials else 0
        )
        return ValidRow(row, exercise, kind, set)
    }

    // --- Workouts ---

    private class Draft(
        val index: Int,
        val date: LocalDate,
        val sessionId: String,
        val plan: String,
        val rows: List<ValidRow>,
        val entries: List<PlannedEntry>
    ) {
        val time: LocalTime? = rows.mapNotNull { it.row.time }.minOrNull()
        val sessionNumber: Long? = sessionId.toLongOrNull()
    }

    /**
     * The exercises of a workout in the order they first appear. An exercise seen again after another one is the same
     * entry while its set numbers keep growing (supersets alternate their rows), and a new one - a treadmill warm-up
     * and cool-down - when they start over.
     */
    private fun buildEntries(rows: List<ValidRow>): List<PlannedEntry> {
        val instances = mutableListOf<Instance>()
        val byKey = mutableMapOf<String, MutableList<Instance>>()
        var lastKey: String? = null
        rows.forEach { valid ->
            val key = "${valid.exercise.key}|${valid.kind}"
            val list = byKey.getOrPut(key) { mutableListOf() }
            val current = list.lastOrNull()
            val given = valid.row.set
            val target = when {
                current == null -> null
                lastKey == key -> current
                given == null -> current
                given > current.highestGiven -> current
                else -> null
            } ?: Instance(key, valid.exercise, valid.kind, instances.size).also { instances.add(it); list.add(it) }
            target.rows.add(valid)
            if (given != null) target.highestGiven = maxOf(target.highestGiven, given)
            lastKey = key
        }
        return instances.map { instance ->
            val ordered = if (instance.rows.all { it.row.set != null }) instance.rows.sortedBy { it.row.set } else instance.rows
            PlannedEntry(
                exercise = instance.exercise,
                kind = instance.kind,
                order = instance.order,
                sets = ordered.mapIndexed { i, valid -> valid.set.copy(number = i + 1) }
            )
        }
    }

    // --- Routines ---

    private fun isSystemName(plan: String): Boolean {
        val folded = HistoryCsvColumns.fold(plan)
        return SystemPlanKind.entries.any { folded in it.aliases }
    }

    private fun targetFor(
        planName: String,
        userPlans: Map<String, WorkoutPlanEntity>,
        systemPlans: Map<SystemPlanKind, WorkoutPlanEntity?>,
        options: ImportOptions
    ): RoutineTarget {
        val folded = HistoryCsvColumns.fold(planName)
        if (folded.isEmpty()) return RoutineTarget.System(SystemPlanKind.QUICK, systemPlans[SystemPlanKind.QUICK])
        // A routine of the user wins over the app's own plans of the same name
        userPlans[folded]?.let { return RoutineTarget.Existing(it) }
        SystemPlanKind.entries.firstOrNull { folded in it.aliases }?.let { return RoutineTarget.System(it, systemPlans[it]) }
        if (folded in options.declinedRoutines) return RoutineTarget.System(SystemPlanKind.QUICK, systemPlans[SystemPlanKind.QUICK])
        return RoutineTarget.Create(planName, folded)
    }

    private fun routinesOf(importing: List<PlannedWorkout>, options: ImportOptions): List<PlannedRoutine> {
        data class Key(val key: String, val name: String, val status: RoutineStatus, val existing: WorkoutPlanEntity?)

        val byRoutine = LinkedHashMap<Key, MutableList<PlannedWorkout>>()
        importing.forEach { workout ->
            val key = when (val target = workout.routine) {
                is RoutineTarget.Existing -> Key("plan:${target.plan.id}", target.plan.nome, RoutineStatus.EXISTING, target.plan)
                is RoutineTarget.Create -> Key(target.key, target.name, RoutineStatus.NEW, null)
                is RoutineTarget.System -> {
                    val declined = workout.sessionNote?.let { HistoryCsvColumns.fold(it) }
                    if (declined != null && target.kind == SystemPlanKind.QUICK && declined in options.declinedRoutines) {
                        Key(declined, workout.sessionNote, RoutineStatus.DECLINED, null)
                    } else return@forEach
                }
            }
            byRoutine.getOrPut(key) { mutableListOf() }.add(workout)
        }
        return byRoutine.map { (key, workouts) ->
            val latest = workouts.maxWith(compareBy({ it.timestamp }, { it.key }))
            PlannedRoutine(
                key = key.key,
                name = key.name,
                status = key.status,
                existing = key.existing,
                workouts = workouts.size,
                firstDate = workouts.minOf { it.date },
                lastDate = workouts.maxOf { it.date },
                template = if (key.status == RoutineStatus.NEW && options.fillNewRoutines) templateOf(latest) else emptyList()
            )
        }.sortedWith(compareBy({ it.status.ordinal }, { it.name.lowercase() }))
    }

    private fun templateOf(workout: PlannedWorkout): List<TemplateExercise> = workout.entries.map { entry ->
        val working = entry.sets.filter { !it.warmup }.ifEmpty { entry.sets }.take(DEFAULT_SETS_LIMIT)
        val reps = working.map { it.reps }
        TemplateExercise(
            exercise = entry.exercise,
            kind = entry.kind,
            sets = working.size,
            repsTarget = when (entry.kind) {
                RowKind.TIME_AND_WEIGHT -> "${working.first().durationSeconds ?: 0}s"
                RowKind.CARDIO -> "1"
                RowKind.TIME_AND_REPS -> reps.first().toString()
                RowKind.STRENGTH -> if (reps.distinct().size == 1) reps.first().toString() else reps.joinToString("-")
            },
            durationSeconds = if (entry.kind == RowKind.STRENGTH) null else working.first().durationSeconds
        )
    }

    // --- Duplicates ---

    private fun duplicateOf(
        date: LocalDate,
        target: RoutineTarget,
        entries: List<PlannedEntry>,
        sessions: List<KnownSession>,
        plansByName: Map<String, List<Int>>
    ): Int? {
        val planIds: Set<Int> = when (target) {
            is RoutineTarget.Existing -> plansByName[HistoryCsvColumns.fold(target.plan.nome)].orEmpty().toSet() + target.plan.id
            is RoutineTarget.System ->
                target.kind.aliases.flatMap { plansByName[it].orEmpty() }.toSet() + listOfNotNull(target.plan?.id)
            is RoutineTarget.Create -> emptySet()
        }
        if (planIds.isEmpty()) return null
        val signature = signatureOf(entries) ?: return null
        return sessions.firstOrNull { it.planId in planIds && it.date == date && sameSets(signature, it.sets) }?.id
    }

    /**
     * Null when a strength exercise is new: a workout with an exercise the app has never seen cannot be in it already.
     * Cardio is told by the activity name instead, since old cardio logs hold free text ("Bike") rather than an exercise.
     */
    private fun signatureOf(entries: List<PlannedEntry>): List<SetSignature>? = entries.flatMap { entry ->
        val key = when {
            entry.kind == RowKind.CARDIO -> cardioKey(exerciseName(entry.exercise))
            entry.exercise is ResolvedExercise.Existing -> exerciseKey(entry.exercise.exercise.id)
            else -> return null
        }
        entry.sets.map { SetSignature(key, it.weightKg, it.reps, it.durationSeconds ?: 0, it.distanceKm) }
    }

    fun exerciseKey(exerciseId: Int) = "e$exerciseId"

    /** The same for "Treadmill" and its translations. */
    fun cardioKey(activity: String) = "c${HistoryCsvColumns.fold(HistoryExerciseResolver.canonicalName(activity))}"

    private val signatureOrder = compareBy<SetSignature>({ it.key }, { it.reps }, { it.seconds }, { (it.weightKg * 10).roundToInt() }, { (it.km * 100).roundToInt() })

    private fun sameSets(a: List<SetSignature>, b: List<SetSignature>): Boolean {
        if (a.size != b.size) return false
        val left = a.sortedWith(signatureOrder)
        val right = b.sortedWith(signatureOrder)
        return left.indices.all { i ->
            val x = left[i]
            val y = right[i]
            x.key == y.key && x.reps == y.reps && x.seconds == y.seconds &&
                abs(x.weightKg - y.weightKg) <= WEIGHT_TOLERANCE_KG && abs(x.km - y.km) <= KM_TOLERANCE
        }
    }

    // Weights written in pounds come back from kilograms a hair off
    private const val WEIGHT_TOLERANCE_KG = 0.06f
    private const val KM_TOLERANCE = 0.011f
}
