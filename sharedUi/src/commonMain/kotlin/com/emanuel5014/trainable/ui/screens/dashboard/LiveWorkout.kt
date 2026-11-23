package com.emanuel5014.trainable.ui.screens.dashboard

import com.emanuel5014.trainable.domain.session.WorkoutTitle
import com.emanuel5014.trainable.data.local.entity.WorkoutSessionEntity
import com.emanuel5014.trainable.data.local.relation.SessionWithPlanName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the open workout last announced about itself, saved so the dashboard can show it once the workout screen is
 * closed: the exercises with their sets done, where the lifter is, and the next set to do (the same set the rest
 * notification announces). Timers are not here: they live on the session row, which every writer keeps current.
 */
@Serializable
data class LiveWorkoutSnapshot(
    val sessionId: Int,
    /** Index in [exercises] of the exercise on screen. */
    val currentIndex: Int,
    val exercises: List<LiveExercise>,
    /** The next set to do; null when every set is done, or for a moment (the pause of a mini-set). */
    val next: LiveNextSet? = null,
    val allDone: Boolean = false,
    /** Every exercise is cardio: a quick workout without a name is shown as a cardio workout (#75). */
    val cardioOnly: Boolean = false,
    val writtenAt: Long
) {
    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): LiveWorkoutSnapshot? =
            text?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
    }
}

@Serializable
data class LiveExercise(
    val name: String,
    val done: Int,
    val total: Int,
    /** What its next set asks ("50 kg × 8", "20 min"); null once it is done. */
    val detail: String? = null
)

/** The next set, with what the rest notification needs to be drawn again (weight in kg, as the notification takes it). */
@Serializable
data class LiveNextSet(
    val exerciseIndex: Int,
    val exerciseName: String,
    val setNumber: Int,
    val setsInExercise: Int,
    val weight: Float,
    val reps: Int,
    val weightUnit: String,
    val previousReps: Int? = null,
    val repsLabel: String? = null,
    val detail: String? = null,
    /** The whole value of a timed or cardio set ("45s · 20 reps"), which has no weight × reps. */
    val text: String? = null,
    /** [com.emanuel5014.trainable.util.notification.NextSetKey.token] of the set "Serie fatta" logs; null hides it. */
    val keyToken: String? = null,
    val kind: SetKind = SetKind.STRENGTH,
    /** Seconds the set aims at: a timed set's time, a cardio set's duration. */
    val targetSeconds: Int? = null,
    /** Kilometres a cardio set aims at. */
    val targetKm: Float? = null,
    /** Round (1-based) of an EMOM set within its run, and how many rounds the run has. */
    val emomRound: Int? = null,
    val emomRounds: Int? = null
)

/** What kind of set comes next: each one has its own way to be shown and done. */
@Serializable
enum class SetKind { STRENGTH, TIMED, CARDIO, EMOM }

/** Which unfinished workout the dashboard follows, and what −/+/Skip do to a rest saved on the session row. */
object LiveWorkout {

    /**
     * The workout with a running timer; else the one the open workout last wrote about; else the most recent one.
     * Null when nothing is unfinished.
     */
    fun pick(unfinished: List<SessionWithPlanName>, snapshot: LiveWorkoutSnapshot?, now: Long): SessionWithPlanName? {
        if (unfinished.isEmpty()) return null
        unfinished.filter { it.session.hasRunningTimer(now) }.maxByOrNull { it.session.timestamp }?.let { return it }
        snapshot?.let { s -> unfinished.firstOrNull { it.session.id == s.sessionId } }?.let { return it }
        return unfinished.maxByOrNull { it.session.timestamp }
    }

    private fun WorkoutSessionEntity.hasRunningTimer(now: Long): Boolean =
        (restTimerEndTime ?: 0L) > now || (warmupTimerEndTime ?: 0L) > now || setTimerRunning || cardioTimerRunning

    /** The rest after a button: [end] null means it is over. */
    data class RestChange(val end: Long?, val total: Int?)

    /**
     * −/+ move the end by [deltaSeconds]; Skip, or a − that leaves nothing, ends the rest. The total grows and shrinks
     * with it but never below what is left, as in the workout.
     */
    fun adjustRest(currentEnd: Long, oldTotal: Int?, deltaSeconds: Int, skip: Boolean, now: Long): RestChange {
        val targetEnd = currentEnd + deltaSeconds * 1000L
        val remaining = ((targetEnd - now) / 1000L).toInt().coerceAtLeast(0)
        if (skip || (deltaSeconds < 0 && remaining <= 0)) return RestChange(null, null)
        val fallbackTotal = ((currentEnd - now) / 1000L).toInt().coerceAtLeast(0)
        val total = ((oldTotal ?: fallbackTotal).toLong() + deltaSeconds)
            .coerceIn(0, Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(remaining)
        return RestChange(targetEnd, total)
    }
}

/** The −/+/Skip buttons of the rest on the dashboard. */
enum class RestButton { SUBTRACT, ADD, SKIP }

/** What the rest buttons show, from the timer settings (the same ones the notification follows). */
data class RestButtons(
    val addSeconds: Int = 30,
    val subtractSeconds: Int = 10,
    val showAdd: Boolean = true,
    val showSubtract: Boolean = true,
    val showSkip: Boolean = true,
    val showSetDone: Boolean = true
)

/** What the live card and the workout panel draw, built from the session row and the snapshot. */
data class LiveWorkoutUi(
    val sessionId: Int,
    val title: String,
    val elapsedMinutes: Int,
    /** What the workout is called, translated where it is shown; [title] is the stored name. */
    val workoutTitle: WorkoutTitle = WorkoutTitle.Named(title),
    /** When the workout started (epoch ms), for the h:mm:ss clock of the status line. */
    val startedAt: Long = 0L,
    val phase: Phase,
    /** Seconds left of the rest or warm-up; seconds run of a set or cardio timer. */
    val seconds: Int = 0,
    /** Whole length of the rest or warm-up, or the target of a set or cardio timer (0 = none), for the ring. */
    val totalSeconds: Int = 0,
    /** The set or cardio timer stands still. */
    val paused: Boolean = false,
    /** The set to do (or, during a rest, the one after it); null when the snapshot is missing. */
    val next: LiveNextSet? = null,
    /** Index (0-based) in [exercises] of the exercise on screen; -1 when unknown. */
    val currentIndex: Int = -1,
    val exercises: List<LiveExercise> = emptyList(),
    /** Done flags of the sets of the exercise of [next], in order. */
    val setDots: List<Boolean> = emptyList(),
    val nextExerciseName: String? = null
) {
    enum class Phase { REST, WARMUP, SET_TIMER, CARDIO, EMOM, LIFTING, ALL_DONE }

    val hasProgress: Boolean get() = exercises.isNotEmpty()
    val setsLeft: Int get() = exercises.sumOf { (it.total - it.done).coerceAtLeast(0) }
    val exercisesDone: Int get() = exercises.count { it.total > 0 && it.done >= it.total }
    val progress: Float get() = exercises.sumOf { it.total }.takeIf { it > 0 }
        ?.let { total -> exercises.sumOf { it.done.coerceAtMost(it.total) }.toFloat() / total } ?: 0f

    companion object {
        fun from(item: SessionWithPlanName, snapshot: LiveWorkoutSnapshot?, now: Long): LiveWorkoutUi {
            val session = item.session
            val snap = snapshot?.takeIf { it.sessionId == session.id }
            val elapsed = ((now - session.timestamp) / 60_000L).toInt().coerceAtLeast(0)
            val rest = session.restTimerEndTime?.takeIf { it > now }
            val warmup = session.warmupTimerEndTime?.takeIf { it > now }
            val next = snap?.next
            val (phase, seconds, total) = when {
                rest != null -> {
                    val left = ((rest - now + 999) / 1000L).toInt()
                    Triple(Phase.REST, left, (session.totalRestSeconds ?: left).coerceAtLeast(left))
                }
                warmup != null -> {
                    val left = ((warmup - now + 999) / 1000L).toInt()
                    Triple(Phase.WARMUP, left, (session.totalWarmupSeconds ?: left).coerceAtLeast(left))
                }
                // An EMOM run only ticks inside the open workout, which parks it on reopening: its clock on the row is stale
                next?.kind == SetKind.EMOM -> Triple(Phase.EMOM, 0, 0)
                session.setTimerRunning || session.setTimerPaused ->
                    Triple(Phase.SET_TIMER, runSeconds(session.setTimerSeconds, session.setTimerRunning, session.setTimerStartedAt, now), next?.targetSeconds ?: 0)
                session.cardioTimerRunning || session.cardioTimerPaused ->
                    Triple(Phase.CARDIO, runSeconds(session.cardioTimerSeconds, session.cardioTimerRunning, session.cardioTimerStartedAt, now), next?.targetSeconds ?: 0)
                snap != null && snap.allDone -> Triple(Phase.ALL_DONE, 0, 0)
                else -> Triple(Phase.LIFTING, 0, 0)
            }
            val nextIndex = next?.exerciseIndex ?: -1
            val dots = snap?.exercises?.getOrNull(nextIndex)?.let { ex -> List(ex.total) { it < ex.done } } ?: emptyList()
            val following = snap?.exercises?.withIndex()
                ?.firstOrNull { (i, ex) -> i > nextIndex && ex.done < ex.total }?.value?.name
            return LiveWorkoutUi(
                sessionId = session.id,
                title = item.displayName,
                elapsedMinutes = elapsed,
                workoutTitle = WorkoutTitle.of(
                    item.session.noteSessione, item.planNome, item.planIsSystem,
                    hasStrength = false, hasCardio = snap?.cardioOnly == true
                ),
                startedAt = session.timestamp,
                phase = phase,
                seconds = seconds,
                totalSeconds = total,
                paused = (phase == Phase.SET_TIMER && session.setTimerPaused && !session.setTimerRunning) ||
                    (phase == Phase.CARDIO && session.cardioTimerPaused && !session.cardioTimerRunning),
                next = next,
                currentIndex = if (nextIndex >= 0) nextIndex else snap?.currentIndex ?: -1,
                exercises = snap?.exercises ?: emptyList(),
                setDots = dots,
                nextExerciseName = following
            )
        }

        private fun runSeconds(saved: Int, running: Boolean, startedAt: Long?, now: Long): Int =
            if (running && startedAt != null) saved + ((now - startedAt) / 1000L).toInt().coerceAtLeast(0) else saved
    }
}
