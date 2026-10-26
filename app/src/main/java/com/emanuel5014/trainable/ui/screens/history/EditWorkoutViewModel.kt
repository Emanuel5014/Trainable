package com.emanuel5014.trainable.ui.screens.history

import com.emanuel5014.trainable.data.local.dao.RunMove
import com.emanuel5014.trainable.data.local.entity.CardioExerciseLogs
import com.emanuel5014.trainable.data.local.entity.nextSet
import com.emanuel5014.trainable.data.local.entity.setsOf
import com.emanuel5014.trainable.data.local.entity.SetLogKind
import com.emanuel5014.trainable.data.local.entity.asKind
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emanuel5014.trainable.data.local.entity.CardioLogEntity
import com.emanuel5014.trainable.data.local.entity.ExerciseEntity
import com.emanuel5014.trainable.data.local.entity.SessionPrescriptionEditEntity
import com.emanuel5014.trainable.data.local.entity.SessionTagEntity
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.local.entity.toPrescriptionBlocks
import com.emanuel5014.trainable.data.local.relation.toJournal
import com.emanuel5014.trainable.data.repository.JournalRepository
import com.emanuel5014.trainable.domain.journal.SessionJournal
import com.emanuel5014.trainable.domain.prescription.ProgramEditKind
import com.emanuel5014.trainable.domain.prescription.ProgramEditScope
import com.emanuel5014.trainable.data.local.entity.repsRange
import com.emanuel5014.trainable.data.local.entity.withRepsRange
import com.emanuel5014.trainable.data.local.entity.withoutPrescription
import com.emanuel5014.trainable.data.repository.ExerciseRepository
import com.emanuel5014.trainable.data.repository.OneRepMaxRepository
import com.emanuel5014.trainable.data.repository.PrescriptionSetSync
import com.emanuel5014.trainable.data.repository.SetSyncResult
import com.emanuel5014.trainable.domain.prescription.LegacyReps
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.RepRange
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.data.repository.WorkoutRepository
import com.emanuel5014.trainable.util.AppLocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditWorkoutState(
    val isLoading: Boolean = true,
    val sessionId: Int = 0,
    val planName: String = "",
    val exercises: List<EditExerciseState> = emptyList(),
    val cardioLogs: List<CardioLogEntity> = emptyList(),
    val availableExercises: List<ExerciseEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val sessionTimestamp: Long = 0L,
    val sessionDurationMs: Long? = null,
    val error: String? = null,
    val weightUnit: String = "kg",
    /** Week of the routine the workout followed. */
    val programWeek: Int? = null,
    /** Changes made to the prescription of an exercise while the workout was done, oldest first. */
    val prescriptionEdits: List<SessionPrescriptionEditEntity> = emptyList(),
    /** The workout journal is switched on in the settings. */
    val journalEnabled: Boolean = false,
    /** Rating, note and tag ids of the workout. */
    val journal: SessionJournal = SessionJournal(),
    /** The tags of the workout, for showing them. */
    val journalTags: List<SessionTagEntity> = emptyList()
)

data class EditExerciseState(
    val exercise: ExerciseEntity,
    val sets: List<SetLogEntity> = emptyList(),
    /** 0 for the first time the exercise was done in the workout, 1 for the second...: keeps the list keys apart. */
    val instance: Int = 0
)

@HiltViewModel
class EditWorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val localeManager: AppLocaleManager,
    private val oneRepMaxRepository: OneRepMaxRepository,
    private val journalRepository: JournalRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(EditWorkoutState())
    val state: StateFlow<EditWorkoutState> = _state.asStateFlow()

    private val sessionId: Int = savedStateHandle.get<Int>("sessionId") ?: 0

    private val _languageCode = MutableStateFlow("en")
    val languageCode: StateFlow<String> = _languageCode.asStateFlow()

    val setPerformanceEnabled = userPreferencesRepository.setPerformanceEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    init {
        loadSessionData()
        loadAvailableExercises()
        loadCategories()
        viewModelScope.launch {
            userPreferencesRepository.userLanguage.collect { _ ->
                _languageCode.value = localeManager.getResolvedLanguage()
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.weightUnit.collect { unit ->
                _state.update { it.copy(weightUnit = unit) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.journalEnabled.collect { enabled ->
                _state.update { it.copy(journalEnabled = enabled) }
            }
        }
    }

    /** Replaces the rating, note and tags of the workout; the screen follows through the session flow. */
    fun saveJournal(journal: SessionJournal) {
        viewModelScope.launch { journalRepository.saveJournal(sessionId, journal) }
    }

    private fun loadSessionData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            workoutRepository.getSessionWithDetails(sessionId)
                .onEach { sessionDetails ->
                    if (sessionDetails != null) {
                        val exercises = sessionDetails.sets.toEditExercises()

                        // Check if we need to normalize orders (e.g., after migration they might all be 0)
                        val orders = exercises.map { it.sets.firstOrNull()?.ordineEsercizio ?: 0 }
                        if (orders.size > 1 && orders.distinct().size < orders.size) {
                            normalizeExerciseOrders(exercises)
                        }
                        
                        _state.update {
                            it.copy(
                                isLoading = false,
                                sessionId = sessionId,
                                planName = sessionName(sessionDetails.session.noteSessione, sessionDetails.plan.nome),
                                sessionTimestamp = sessionDetails.session.timestamp,
                                sessionDurationMs = sessionDetails.session.durationMs,
                                exercises = exercises,
                                cardioLogs = sessionDetails.cardio,
                                programWeek = sessionDetails.session.programWeek,
                                prescriptionEdits = sessionDetails.prescriptionEdits,
                                journal = sessionDetails.toJournal(),
                                journalTags = sessionDetails.tags
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Session not found") }
                    }
                }
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect()
        }
    }

    private fun normalizeExerciseOrders(exercises: List<EditExerciseState>) {
        viewModelScope.launch {
            val setsToUpdate = mutableListOf<SetLogEntity>()
            exercises.forEachIndexed { index, exerciseState ->
                exerciseState.sets.forEach { set ->
                    setsToUpdate.add(set.copy(ordineEsercizio = index))
                }
            }
            if (setsToUpdate.isNotEmpty()) {
                workoutRepository.updateSetOrders(setsToUpdate)
            }
            // The program edits made to a run follow its new order
            val moves = exercises.mapIndexedNotNull { index, exerciseState ->
                exerciseState.key.takeIf { it.order != index }?.let { RunMove(it.exerciseId, it.order, index) }
            }
            if (moves.isNotEmpty()) workoutRepository.moveRunEdits(sessionId, moves)
        }
    }

    private fun loadAvailableExercises() {
        viewModelScope.launch {
            exerciseRepository.getAllExercises().collect { exercises ->
                _state.update { it.copy(availableExercises = exercises) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            exerciseRepository.getCategories().collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    fun updateSet(updatedSet: SetLogEntity) {
        viewModelScope.launch {
            workoutRepository.updateSet(updatedSet)
        }
    }

    fun updateCardioLog(updatedCardio: CardioLogEntity) {
        viewModelScope.launch {
            workoutRepository.saveCardioLog(updatedCardio)
        }
    }

    fun addCardioLog(categoria: String, distanza: Float, durataSecondi: Int, sets: Int = 1, note: String? = null) {
        viewModelScope.launch {
            val maxOrder = maxOf(
                _state.value.exercises.maxOfOrNull { it.sets.firstOrNull()?.ordineEsercizio ?: 0 } ?: 0,
                _state.value.cardioLogs.maxOfOrNull { it.ordineEsercizio } ?: 0
            )
            (1..sets.coerceAtLeast(1)).forEach { number ->
                workoutRepository.saveCardioLog(
                    CardioLogEntity(
                        sessionId = sessionId,
                        categoria = categoria,
                        distanza = distanza,
                        durataSecondi = durataSecondi,
                        timestamp = _state.value.sessionTimestamp,
                        ordineEsercizio = maxOrder + 1,
                        numeroSerie = number,
                        note = note.takeIf { number == 1 }
                    )
                )
            }
        }
    }

    /** One more set of a cardio exercise, starting from the values of its last one. */
    fun addCardioSet(cardio: CardioExerciseLogs) {
        val last = cardio.sets.last()
        viewModelScope.launch {
            workoutRepository.saveCardioLog(
                last.nextSet(cardio.sets.size + 1)
            )
        }
    }

    /** Removes one cardio set; the others of its exercise are numbered again. */
    fun deleteCardioLog(cardio: CardioLogEntity) {
        viewModelScope.launch {
            val remaining = _state.value.cardioLogs.setsOf(cardio).filter { it.id != cardio.id }
            // Optimistic update
            _state.update { curr ->
                curr.copy(cardioLogs = curr.cardioLogs.filter { it.id != cardio.id })
            }
            workoutRepository.deleteCardioLog(cardio)
            remaining.forEachIndexed { index, set ->
                if (set.numeroSerie != index + 1) workoutRepository.updateCardioSetNumber(set.id, index + 1)
            }
        }
    }

    /** Removes a whole cardio exercise, every set of it. */
    fun deleteCardioExercise(cardio: CardioExerciseLogs) {
        viewModelScope.launch {
            val ids = cardio.sets.map { it.id }.toSet()
            _state.update { curr -> curr.copy(cardioLogs = curr.cardioLogs.filter { it.id !in ids }) }
            cardio.sets.forEach { workoutRepository.deleteCardioLog(it) }
        }
    }

    /** Moves a cardio exercise one place up ([delta] -1) or down (+1) among the [items] on screen. */
    fun moveCardio(cardio: CardioExerciseLogs, items: List<Any>, delta: Int) {
        val index = items.indexOfFirst { it is CardioExerciseLogs && it.first.id == cardio.first.id }
        val target = index + delta
        if (index < 0 || target !in items.indices) return
        val reordered = items.toMutableList().apply { add(target, removeAt(index)) }
        updateItemsOrder(reordered)
    }

    fun deleteSet(set: SetLogEntity) {
        viewModelScope.launch {
            // Optimistic update
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val index = mutableExercises.indexOfFirst { it.key == set.exerciseKey }
                if (index != -1) {
                    val exState = mutableExercises[index]
                    // Use equality instead of ID to avoid deleting all id=0 sets
                    val newSets = exState.sets.filter { it != set }
                    mutableExercises[index] = exState.copy(sets = newSets)
                }
                curr.copy(exercises = mutableExercises)
            }
            
            workoutRepository.deleteSet(set)
            // After deleting, we might need to reorder remaining sets
            reorderSets(set.exerciseKey)
        }
    }

    fun addSet(key: ExerciseKey) {
        viewModelScope.launch {
            val exerciseId = key.exerciseId
            val exerciseState = _state.value.exercises.find { it.key == key }
            val nextSerie = (exerciseState?.sets?.maxOfOrNull { it.numeroSerie } ?: 0) + 1
            val lastWeight = exerciseState?.sets?.lastOrNull()?.pesoSollevato ?: 0f
            val lastReps = exerciseState?.sets?.lastOrNull()?.repsEffettive ?: 8
            val lastDuration = exerciseState?.sets?.lastOrNull()?.durataSecondi
            val exerciseOrder = exerciseState?.sets?.firstOrNull()?.ordineEsercizio ?: 0
            val supersetId = exerciseState?.sets?.firstOrNull()?.supersetId

            val lastSet = exerciseState?.sets?.lastOrNull()
            var newSet = SetLogEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                pesoSollevato = lastWeight,
                repsEffettive = lastReps,
                numeroSerie = nextSerie,
                ordineEsercizio = exerciseOrder,
                supersetId = supersetId,
                durataSecondi = lastDuration,
                exerciseType = lastSet?.exerciseType
            )
            if (lastSet != null && lastSet.blockIndex == null) {
                // A set added to a plain exercise done against a rep range keeps that range
                newSet = newSet.withRepsRange(lastSet.repsRange)
            }
            if (lastSet != null && lastSet.blockIndex != null && lastSet.repMode != "total") {
                // A set added to an advanced exercise repeats the prescription of the one before it
                newSet = newSet.copy(
                    targetPercent = lastSet.targetPercent,
                    targetRpe = lastSet.targetRpe,
                    targetReps = lastSet.targetReps,
                    repMode = lastSet.repMode,
                    blockIndex = lastSet.blockIndex,
                    techniques = lastSet.techniques,
                    isExtra = lastSet.isExtra
                )
            }

            // Optimistic update to prevent duplicates on rapid clicks
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val index = mutableExercises.indexOfFirst { it.key == key }
                if (index != -1) {
                    val exState = mutableExercises[index]
                    mutableExercises[index] = exState.copy(sets = exState.sets + newSet)
                }
                curr.copy(exercises = mutableExercises)
            }

            val id = workoutRepository.logSet(newSet)
            
            // Update the optimistic set with the real ID
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val exIndex = mutableExercises.indexOfFirst { it.key == key }
                if (exIndex != -1) {
                    val exState = mutableExercises[exIndex]
                    val mutableSets = exState.sets.toMutableList()
                    val setIndex = mutableSets.indexOfFirst { it.numeroSerie == nextSerie && it.id == 0 }
                    if (setIndex != -1) {
                        mutableSets[setIndex] = mutableSets[setIndex].copy(id = id.toInt())
                    }
                    mutableExercises[exIndex] = exState.copy(sets = mutableSets)
                }
                curr.copy(exercises = mutableExercises)
            }
        }
    }

    fun deleteExercise(key: ExerciseKey) {
        viewModelScope.launch {
            // Optimistic update
            _state.update { curr ->
                curr.copy(exercises = curr.exercises.filter { it.key != key })
            }
            // Only this run: the same exercise done again elsewhere in the workout stays
            workoutRepository.deleteExerciseRows(sessionId, key.exerciseId, key.order)
        }
    }

    fun deleteSession() {
        viewModelScope.launch {
            workoutRepository.deleteSession(sessionId)
        }
    }

    fun swapExercise(
        old: ExerciseKey,
        newExerciseId: Int,
        exerciseType: String = "strength",
        durataTargetSecondi: Int? = null,
        /** The rep range the replacement was done against; the logged reps themselves are kept. */
        repsRange: RepRange? = null
    ) {
        viewModelScope.launch {
            val oldExerciseState = _state.value.exercises.find { it.key == old } ?: return@launch
            // The percentages referred to the old exercise's 1RM, so a plain swap drops the prescription
            val sets = oldExerciseState.sets.map { it.withoutPrescription().withRepsRange(repsRange) }
            val exerciseOrder = old.order
            val supersetId = sets.firstOrNull()?.supersetId
            val kind = SetLogKind.ofExerciseType(exerciseType)
            val targetSec = durataTargetSecondi ?: 45

            // Optimistic update
            val replacementExercise = _state.value.availableExercises.find { it.id == newExerciseId }
            if (replacementExercise != null) {
                _state.update { curr ->
                    val mutableExercises = curr.exercises.toMutableList()
                    val index = mutableExercises.indexOfFirst { it.key == old }
                    if (index != -1) {
                        mutableExercises[index] = EditExerciseState(
                            exercise = replacementExercise,
                            sets = sets.map {
                                it.copy(
                                    exerciseId = newExerciseId,
                                    id = 0,
                                    supersetId = supersetId
                                ).asKind(kind, targetSec)
                            }
                        )
                    }
                    // The replacement may be an exercise that is in the workout already, as another run
                    curr.copy(exercises = mutableExercises.numbered())
                }
            }

            // Delete old sets: only this run, the same exercise done again elsewhere in the workout stays
            workoutRepository.deleteExerciseRows(sessionId, old.exerciseId, old.order)

            // Insert new sets with new exerciseId and same order, then update state with real IDs
            val newSetIds = sets.map { set ->
                workoutRepository.logSet(
                    set.copy(
                        id = 0,
                        exerciseId = newExerciseId,
                        ordineEsercizio = exerciseOrder,
                        supersetId = supersetId
                    ).asKind(kind, targetSec)
                )
            }
            
            // Update IDs in state
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val index = mutableExercises.indexOfFirst { it.key == ExerciseKey(newExerciseId, exerciseOrder) }
                if (index != -1) {
                    val exState = mutableExercises[index]
                    mutableExercises[index] = exState.copy(
                        sets = exState.sets.mapIndexed { i, s -> 
                            if (s.id == 0 && i < newSetIds.size) s.copy(id = newSetIds[i].toInt()) else s 
                        }
                    )
                }
                curr.copy(exercises = mutableExercises)
            }
        }
    }

    /** Swaps the exercise keeping the logged rows and giving them the advanced prescription [blocks]. */
    fun swapExerciseAdvanced(oldKey: ExerciseKey, newExerciseId: Int, blocks: List<PrescriptionBlock>) {
        viewModelScope.launch {
            val old = _state.value.exercises.find { it.key == oldKey } ?: return@launch
            val first = old.sets.firstOrNull()
            val (unit, increment) = prescriptionEnvironment()
            val result = PrescriptionSetSync.apply(
                existing = old.sets,
                blocks = blocks,
                sessionId = sessionId,
                exerciseId = newExerciseId,
                order = first?.ordineEsercizio ?: 0,
                supersetId = first?.supersetId,
                restSeconds = first?.restTimerSeconds,
                oneRepMaxKg = oneRepMaxRepository.currentOnce(newExerciseId)?.weightKg,
                unit = unit,
                increment = increment
            )
            val deleted = result.toDelete.map { it.id }.toSet()
            val updates = result.toUpdate.associateBy { it.id }
            val moved = old.sets.filter { it.id !in deleted }.map { (updates[it.id] ?: it).copy(exerciseId = newExerciseId).asKind(SetLogKind.STRENGTH) }
            workoutRepository.applySetSync(SetSyncResult(result.toInsert, moved, result.toDelete))
        }
    }

    /** Adds an exercise at the end of the session, already carrying the advanced prescription [blocks]. */
    fun addAdvancedExercise(exerciseId: Int, blocks: List<PrescriptionBlock>) {
        viewModelScope.launch {
            val nextOrder = (_state.value.exercises.maxOfOrNull { it.sets.firstOrNull()?.ordineEsercizio ?: 0 } ?: -1) + 1
            val (unit, increment) = prescriptionEnvironment()
            val result = PrescriptionSetSync.apply(
                existing = emptyList(),
                blocks = blocks,
                sessionId = sessionId,
                exerciseId = exerciseId,
                order = nextOrder,
                supersetId = null,
                restSeconds = null,
                oneRepMaxKg = oneRepMaxRepository.currentOnce(exerciseId)?.weightKg,
                unit = unit,
                increment = increment
            )
            workoutRepository.applySetSync(result)
        }
    }

    /**
     * Gives an exercise of this session a new prescription (or removes it with empty [blocks]).
     * Weights, reps, RPE and notes that were logged are kept; the session list refreshes from the database.
     */
    fun applyPrescription(key: ExerciseKey, blocks: List<PrescriptionBlock>) {
        viewModelScope.launch {
            val exerciseId = key.exerciseId
            val exercise = _state.value.exercises.find { it.key == key } ?: return@launch
            val first = exercise.sets.firstOrNull()
            val (unit, increment) = prescriptionEnvironment()
            val result = PrescriptionSetSync.apply(
                existing = exercise.sets,
                blocks = blocks,
                sessionId = sessionId,
                exerciseId = exerciseId,
                order = first?.ordineEsercizio ?: 0,
                supersetId = first?.supersetId,
                restSeconds = first?.restTimerSeconds,
                oneRepMaxKg = oneRepMaxRepository.currentOnce(exerciseId)?.weightKg,
                unit = unit,
                increment = increment
            )
            workoutRepository.applySetSync(result)
            // Changing the prescription of an exercise that already had one is written down like a change made in the workout
            val before = exercise.sets.toPrescriptionBlocks()
            if (before.isNotEmpty() && blocks.isNotEmpty() && before != blocks) {
                workoutRepository.recordPrescriptionEdit(
                    SessionPrescriptionEditEntity.of(
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        planExerciseId = null,
                        programWeek = _state.value.programWeek,
                        kind = ProgramEditKind.PROGRAM,
                        scope = ProgramEditScope.WORKOUT,
                        // What the exercise was done against as its sets show it now (sets never done are gone by now)
                        before = before,
                        after = blocks,
                        order = key.order
                    )
                )
            }
        }
    }

    /** How many logged rows a new prescription would delete, so the screen can ask before applying it. */
    fun removedSetsCount(key: ExerciseKey, blocks: List<PrescriptionBlock>): Int {
        val exercise = _state.value.exercises.find { it.key == key } ?: return 0
        return PrescriptionSetSync.removedCount(exercise.sets, blocks)
    }

    private suspend fun prescriptionEnvironment(): Pair<String, Float> =
        userPreferencesRepository.weightUnit.first() to userPreferencesRepository.loadRoundingIncrement.first()

    fun swapExerciseWithCardio(old: ExerciseKey, cardioCategory: String, durationMinutes: Int, sets: Int = 1) {
        viewModelScope.launch {
            val order = old.order

            // Only this run: the same exercise done again elsewhere in the workout stays
            workoutRepository.deleteExerciseRows(sessionId, old.exerciseId, old.order)
            _state.update { curr ->
                curr.copy(exercises = curr.exercises.filter { it.key != old }.numbered())
            }

            val added = (1..sets.coerceAtLeast(1)).map { number ->
                val newCardio = CardioLogEntity(
                    sessionId = sessionId,
                    categoria = cardioCategory,
                    distanza = 0f,
                    durataSecondi = durationMinutes * 60,
                    timestamp = _state.value.sessionTimestamp,
                    ordineEsercizio = order,
                    numeroSerie = number
                )
                newCardio.copy(id = workoutRepository.saveCardioLog(newCardio).toInt())
            }
            _state.update { curr ->
                curr.copy(cardioLogs = curr.cardioLogs + added)
            }
        }
    }

    fun addExercise(
        exerciseId: Int,
        targetSets: Int = 1,
        repsTarget: String = "8",
        exerciseType: String = "strength",
        durataTargetSecondi: Int? = null,
        /** Top of a rep range; [repsTarget] is then its bottom. */
        repsMax: Int? = null
    ) {
        viewModelScope.launch {
            val nextOrder = (_state.value.exercises.maxOfOrNull { it.sets.firstOrNull()?.ordineEsercizio ?: 0 } ?: -1) + 1
            val exercise = _state.value.availableExercises.find { it.id == exerciseId }
            val kind = SetLogKind.ofExerciseType(exerciseType)
            val targetSec = if (kind.isTimed) (durataTargetSecondi ?: 45) else null
            val setsCount = targetSets.coerceAtLeast(1)
            // A range goes on every row, so the history (and the next edit) know what the exercise was done against
            val repsRange = if (exerciseType == "strength") RepRange.of(repsTarget.trim().toIntOrNull(), repsMax) else null
            // One number for every set, or a list ("8-6-4") that goes set by set like in a live workout
            val plannedReps = LegacyReps.parse(repsTarget, setsCount)

            val newSets = (1..setsCount).map { setNum ->
                SetLogEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    pesoSollevato = 0f,
                    repsEffettive = if (kind == SetLogKind.TIME_AND_WEIGHT) 0 else plannedReps[setNum - 1],
                    numeroSerie = setNum,
                    ordineEsercizio = nextOrder,
                    supersetId = null,
                    durataSecondi = targetSec,
                    exerciseType = kind.rowType
                ).withRepsRange(repsRange)
            }

            // Optimistic update
            if (exercise != null) {
                _state.update { curr ->
                    // The exercise may already be in the workout: the new run gets the next number, so its list key is its own
                    curr.copy(exercises = (curr.exercises + EditExerciseState(exercise, newSets)).numbered())
                }
            }

            // Add sets to repository and update state with real IDs
            val realIds = newSets.map { set ->
                workoutRepository.logSet(set).toInt()
            }

            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val index = mutableExercises.indexOfFirst { it.key == ExerciseKey(exerciseId, nextOrder) }
                if (index != -1) {
                    val exState = mutableExercises[index]
                    mutableExercises[index] = exState.copy(
                        sets = exState.sets.mapIndexed { i, s ->
                            if (s.id == 0 && i < realIds.size) s.copy(id = realIds[i]) else s
                        }
                    )
                }
                curr.copy(exercises = mutableExercises)
            }
        }
    }

    fun addCustomExercise(name: String, category: String, onCreated: (ExerciseEntity) -> Unit = {}) {
        viewModelScope.launch {
            val exerciseId = exerciseRepository.addCustomExercise(name, category)
            val newEx = ExerciseEntity(id = exerciseId, nome = name, categoria = category)
            onCreated(newEx)
        }
    }

    /** Adds an exercise the user deleted from the exercise list back, from a workout that still has it. */
    fun restoreExercise(exerciseId: Int) {
        viewModelScope.launch { exerciseRepository.restoreExercise(exerciseId) }
    }

    fun moveExerciseUp(key: ExerciseKey) {
        val exercises = _state.value.exercises
        val index = exercises.indexOfFirst { it.key == key }
        if (index > 0) {
            val currentEx = exercises[index]
            val prevEx = exercises[index - 1]
            
            // Ensure we have valid orders to swap
            val currentOrder = currentEx.sets.firstOrNull()?.ordineEsercizio ?: index
            val prevOrder = prevEx.sets.firstOrNull()?.ordineEsercizio ?: (index - 1)
            
            // If they are the same, we must force a difference
            val finalPrevOrder = if (currentOrder == prevOrder) index - 1 else prevOrder
            val finalCurrentOrder = if (currentOrder == prevOrder) index else currentOrder

            // Optimistic update
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                mutableExercises[index] = currentEx.copy(sets = currentEx.sets.map { it.copy(ordineEsercizio = finalPrevOrder) })
                mutableExercises[index - 1] = prevEx.copy(sets = prevEx.sets.map { it.copy(ordineEsercizio = finalCurrentOrder) })
                // Re-sort the list for UI
                curr.copy(exercises = mutableExercises.sortedBy { it.sets.firstOrNull()?.ordineEsercizio ?: 0 })
            }

            viewModelScope.launch {
                val setsToUpdate = mutableListOf<SetLogEntity>()
                currentEx.sets.forEach { setsToUpdate.add(it.copy(ordineEsercizio = finalPrevOrder)) }
                prevEx.sets.forEach { setsToUpdate.add(it.copy(ordineEsercizio = finalCurrentOrder)) }
                if (setsToUpdate.isNotEmpty()) {
                    workoutRepository.updateSetOrders(setsToUpdate)
                }
                // The program edits made to the two runs go with them
                if (currentOrder != prevOrder) {
                    workoutRepository.moveRunEdits(
                        sessionId,
                        listOf(RunMove(currentEx.exercise.id, currentOrder, finalPrevOrder), RunMove(prevEx.exercise.id, prevOrder, finalCurrentOrder))
                    )
                }
            }
        }
    }

    fun moveExerciseDown(key: ExerciseKey) {
        val exercises = _state.value.exercises
        val index = exercises.indexOfFirst { it.key == key }
        if (index in 0 until exercises.size - 1) {
            val currentEx = exercises[index]
            val nextEx = exercises[index + 1]
            
            val currentOrder = currentEx.sets.firstOrNull()?.ordineEsercizio ?: index
            val nextOrder = nextEx.sets.firstOrNull()?.ordineEsercizio ?: (index + 1)
            
            // If they are the same, we must force a difference
            val finalNextOrder = if (currentOrder == nextOrder) index + 1 else nextOrder
            val finalCurrentOrder = if (currentOrder == nextOrder) index else currentOrder

            // Optimistic update
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                mutableExercises[index] = currentEx.copy(sets = currentEx.sets.map { it.copy(ordineEsercizio = finalNextOrder) })
                mutableExercises[index + 1] = nextEx.copy(sets = nextEx.sets.map { it.copy(ordineEsercizio = finalCurrentOrder) })
                // Re-sort the list for UI
                curr.copy(exercises = mutableExercises.sortedBy { it.sets.firstOrNull()?.ordineEsercizio ?: 0 })
            }

            viewModelScope.launch {
                val setsToUpdate = mutableListOf<SetLogEntity>()
                currentEx.sets.forEach { setsToUpdate.add(it.copy(ordineEsercizio = finalNextOrder)) }
                nextEx.sets.forEach { setsToUpdate.add(it.copy(ordineEsercizio = finalCurrentOrder)) }
                if (setsToUpdate.isNotEmpty()) {
                    workoutRepository.updateSetOrders(setsToUpdate)
                }
                // The program edits made to the two runs go with them
                if (currentOrder != nextOrder) {
                    workoutRepository.moveRunEdits(
                        sessionId,
                        listOf(RunMove(currentEx.exercise.id, currentOrder, finalNextOrder), RunMove(nextEx.exercise.id, nextOrder, finalCurrentOrder))
                    )
                }
            }
        }
    }

    fun moveSetUp(set: SetLogEntity) {
        val exerciseState = _state.value.exercises.find { it.key == set.exerciseKey } ?: return
        // Use indexOf(set) to correctly identify the set even if multiple have id=0
        val index = exerciseState.sets.indexOf(set)
        if (index > 0) {
            val prevSet = exerciseState.sets[index - 1]
            
            // Optimistic update
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val exIndex = mutableExercises.indexOfFirst { it.key == set.exerciseKey }
                if (exIndex != -1) {
                    val exState = mutableExercises[exIndex]
                    val mutableSets = exState.sets.toMutableList()
                    mutableSets[index] = set.copy(numeroSerie = prevSet.numeroSerie)
                    mutableSets[index - 1] = prevSet.copy(numeroSerie = set.numeroSerie)
                    mutableExercises[exIndex] = exState.copy(sets = mutableSets.sortedBy { it.numeroSerie })
                }
                curr.copy(exercises = mutableExercises)
            }

            viewModelScope.launch {
                workoutRepository.updateSetOrders(listOf(
                    set.copy(numeroSerie = prevSet.numeroSerie),
                    prevSet.copy(numeroSerie = set.numeroSerie)
                ))
            }
        }
    }

    fun moveSetDown(set: SetLogEntity) {
        val exerciseState = _state.value.exercises.find { it.key == set.exerciseKey } ?: return
        // Use indexOf(set) to correctly identify the set even if multiple have id=0
        val index = exerciseState.sets.indexOf(set)
        if (index < exerciseState.sets.size - 1) {
            val nextSet = exerciseState.sets[index + 1]

            // Optimistic update
            _state.update { curr ->
                val mutableExercises = curr.exercises.toMutableList()
                val exIndex = mutableExercises.indexOfFirst { it.key == set.exerciseKey }
                if (exIndex != -1) {
                    val exState = mutableExercises[exIndex]
                    val mutableSets = exState.sets.toMutableList()
                    mutableSets[index] = set.copy(numeroSerie = nextSet.numeroSerie)
                    mutableSets[index + 1] = nextSet.copy(numeroSerie = set.numeroSerie)
                    mutableExercises[exIndex] = exState.copy(sets = mutableSets.sortedBy { it.numeroSerie })
                }
                curr.copy(exercises = mutableExercises)
            }

            viewModelScope.launch {
                workoutRepository.updateSetOrders(listOf(
                    set.copy(numeroSerie = nextSet.numeroSerie),
                    nextSet.copy(numeroSerie = set.numeroSerie)
                ))
            }
        }
    }

    private suspend fun reorderSets(key: ExerciseKey) {
        val exerciseState = _state.value.exercises.find { it.key == key } ?: return
        val sets = exerciseState.sets.sortedBy { it.numeroSerie }
        
        val updatedSets = sets.mapIndexed { index, setLogEntity ->
            setLogEntity.copy(numeroSerie = index + 1)
        }

        // Optimistic update for reordering
        _state.update { curr ->
            val mutableExercises = curr.exercises.toMutableList()
            val exIndex = mutableExercises.indexOfFirst { it.key == key }
            if (exIndex != -1) {
                mutableExercises[exIndex] = exerciseState.copy(sets = updatedSets)
            }
            curr.copy(exercises = mutableExercises)
        }
        
        workoutRepository.updateSetOrders(updatedSets)
    }

    fun toggleSupersetWithNext(key: ExerciseKey) {
        viewModelScope.launch {
            val exercises = _state.value.exercises
            val index = exercises.indexOfFirst { it.key == key }
            if (index != -1 && index < exercises.size - 1) {
                val currentEx = exercises[index]
                val nextEx = exercises[index + 1]

                val currentSid = currentEx.sets.firstOrNull()?.supersetId
                val nextSid = nextEx.sets.firstOrNull()?.supersetId

                val isLinked = currentSid != null && currentSid == nextSid

                val newSid = if (isLinked) {
                    null
                } else {
                    currentSid ?: nextSid ?: java.util.UUID.randomUUID().toString()
                }

                val setsToUpdate = mutableListOf<SetLogEntity>()
                val updatedCurrentSets = currentEx.sets.map { it.copy(supersetId = newSid) }
                val updatedNextSets = nextEx.sets.map { it.copy(supersetId = newSid) }

                setsToUpdate.addAll(updatedCurrentSets)
                setsToUpdate.addAll(updatedNextSets)

                // Optimistic update
                _state.update { curr ->
                    val mutableExercises = curr.exercises.toMutableList()
                    mutableExercises[index] = currentEx.copy(sets = updatedCurrentSets)
                    mutableExercises[index + 1] = nextEx.copy(sets = updatedNextSets)
                    curr.copy(exercises = mutableExercises)
                }

                workoutRepository.updateSetOrders(setsToUpdate)
            }
        }
    }

    fun updateSessionName(name: String) {
        viewModelScope.launch {
            val sessionDetails = workoutRepository.getSessionWithDetails(sessionId).first()
            if (sessionDetails != null) {
                workoutRepository.updateSession(sessionDetails.session.copy(noteSessione = name.ifBlank { null }))
            }
        }
    }

    fun updateSessionDate(newTimestamp: Long) {
        viewModelScope.launch {
            val sessionDetails = workoutRepository.getSessionWithDetails(sessionId).first()
            if (sessionDetails != null) {
                workoutRepository.updateSession(sessionDetails.session.copy(timestamp = newTimestamp))
            }
        }
    }

    fun updateSessionDuration(durationMs: Long?) {
        viewModelScope.launch {
            val sessionDetails = workoutRepository.getSessionWithDetails(sessionId).first()
            if (sessionDetails != null) {
                workoutRepository.updateSession(sessionDetails.session.copy(durationMs = durationMs))
                _state.update { it.copy(sessionDurationMs = durationMs) }
            }
        }
    }

    fun updateSessionDetails(name: String, timestamp: Long, durationMs: Long?) {
        viewModelScope.launch {
            val sessionDetails = workoutRepository.getSessionWithDetails(sessionId).first()
            if (sessionDetails != null) {
                val updatedSession = sessionDetails.session.copy(
                    noteSessione = name.ifBlank { null },
                    timestamp = timestamp,
                    durationMs = durationMs
                )
                workoutRepository.updateSession(updatedSession)
                _state.update {
                    it.copy(
                        planName = name.ifBlank { sessionDetails.plan.nome },
                        sessionTimestamp = timestamp,
                        sessionDurationMs = durationMs
                    )
                }
            }
        }
    }

    fun updateItemsOrder(orderedItems: List<Any>) {
        viewModelScope.launch {
            val setsToUpdate = mutableListOf<SetLogEntity>()
            val cardioToUpdate = mutableListOf<CardioLogEntity>()
            val updatedExercises = mutableListOf<EditExerciseState>()
            val updatedCardioLogs = mutableListOf<CardioLogEntity>()
            // The program edits made to a run follow it to its new place
            val runMoves = orderedItems.mapIndexedNotNull { index, item ->
                (item as? EditExerciseState)?.key?.takeIf { it.order != index }?.let { RunMove(it.exerciseId, it.order, index) }
            }

            orderedItems.forEachIndexed { index, item ->
                when (item) {
                    is EditExerciseState -> {
                        val updatedSets = item.sets.map { it.copy(ordineEsercizio = index) }
                        setsToUpdate.addAll(updatedSets)
                        updatedExercises.add(item.copy(sets = updatedSets))
                    }
                    is CardioExerciseLogs -> {
                        val updatedCardio = item.sets.map { it.copy(ordineEsercizio = index) }
                        cardioToUpdate.addAll(updatedCardio)
                        updatedCardioLogs.addAll(updatedCardio)
                    }
                }
            }

            // Optimistic update
            _state.update { curr ->
                curr.copy(
                    exercises = updatedExercises,
                    cardioLogs = updatedCardioLogs
                )
            }

            if (setsToUpdate.isNotEmpty()) {
                workoutRepository.updateSetOrders(setsToUpdate)
            }
            if (runMoves.isNotEmpty()) workoutRepository.moveRunEdits(sessionId, runMoves)
            cardioToUpdate.forEach { cardio ->
                workoutRepository.updateCardioLog(cardio)
            }
        }
    }
}
