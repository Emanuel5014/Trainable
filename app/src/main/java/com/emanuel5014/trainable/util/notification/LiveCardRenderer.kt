package com.emanuel5014.trainable.util.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.emanuel5014.trainable.MainActivity
import com.emanuel5014.trainable.R

/** What one drawing of the live card needs, all decided before (mode, buttons, bar). */
internal data class LiveCardInput(
    val card: WorkoutLiveCard,
    val timers: LiveTimers,
    val mode: LiveMode,
    val buttons: List<LiveButton>,
    val segments: List<LiveSegment>,
    /** A button found no open workout: the card says to open the app. */
    val needsApp: Boolean,
    val addSeconds: Int,
    val subtractSeconds: Int,
    val nowMillis: Long,
    /** During a rest or a warm-up: how much of it is gone, 0..1000, so the set it leads to fills up as time passes. */
    val countdownPermille: Int? = null
)

/** How a drawing alerts: null for the silent updates, else the channel that rings and the defaults it uses. */
internal data class LiveCardAlert(val channelId: String, val defaults: Int, val silenceVibration: Boolean)

/**
 * Draws the live card of the whole workout as one notification. From Android 16 it is a Live Update (chip in the status
 * bar, bar with one segment per set); before that a plain ongoing notification with a simple bar.
 */
internal class LiveCardRenderer(private val context: Context) {

    fun build(input: LiveCardInput, channelId: String, alert: LiveCardAlert?): Notification {
        val card = input.card
        val text = texts(input)
        val builder = NotificationCompat.Builder(context, alert?.channelId ?: channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(text.title)
            .setContentText(text.text)
            .setSubText(listOfNotNull(workoutName(card), exercisePlace(card)).joinToString(" · "))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(if (alert != null) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setContentIntent(openApp())

        if (alert != null) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH).setOnlyAlertOnce(false).setDefaults(alert.defaults)
            if (alert.silenceVibration) builder.setVibrate(longArrayOf(0))
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT).setSilent(true).setOnlyAlertOnce(true)
        }

        clock(input)?.let { (whenMillis, countDown) ->
            builder.setUsesChronometer(true).setChronometerCountDown(countDown).setWhen(whenMillis).setShowWhen(true)
        } ?: builder.setUsesChronometer(false).setShowWhen(false)

        input.buttons.forEach { button -> actions(input, button).forEach { builder.addAction(it) } }

        val filling = fillingProgress(input)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            chipText(input)?.let { builder.setShortCriticalText(it) }
            builder.setStyle(progressStyle(input.segments, filling)).setRequestPromotedOngoing(true)
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(listOfNotNull(text.text, text.detail).joinToString("\n")))
            if (input.segments.isNotEmpty()) {
                val progress = filling ?: (input.segments.count { it == LiveSegment.DONE } * SEGMENT_UNITS)
                builder.setProgress(input.segments.size * SEGMENT_UNITS, progress, false)
            }
        }
        return builder.build()
    }

    /**
     * The end of a rest, warm-up or EMOM run as a plain notification beside the card: watches and bands skip ongoing
     * notifications, so this is the one they get (like the old finished-rest notification). Same words and buttons as
     * the card; it rings on [alert]'s channel and goes once the card moves on.
     */
    fun buildAlert(input: LiveCardInput, alert: LiveCardAlert): Notification {
        val text = texts(input)
        val builder = NotificationCompat.Builder(context, alert.channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(text.title)
            .setContentText(text.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.text ?: text.title))
            .setSubText(workoutName(input.card))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(false)
            .setAutoCancel(true)
            .setDefaults(alert.defaults)
            .setContentIntent(openApp())
        if (alert.silenceVibration) builder.setVibrate(longArrayOf(0))
        input.buttons.forEach { button -> actions(input, button).forEach { builder.addAction(it) } }
        return builder.build()
    }

    private data class Texts(val title: String, val text: String?, val detail: String?)

    private fun texts(input: LiveCardInput): Texts {
        val card = input.card
        val open = if (input.needsApp) context.getString(R.string.open_app_to_continue) else null
        val setTitle = setTitle(card)
        val value = valueLine(card)
        val place = placeLine(card)
        // What comes next, as the rest and warm-up show it: a new exercise is said so
        val upcoming = when {
            card.exerciseName == null -> null
            card.startsExercise -> context.getString(R.string.live_card_next_exercise, card.exerciseName)
            else -> context.getString(R.string.live_card_next, setTitle)
        }?.let { listOfNotNull(it, card.value).joinToString(" · ") }
        val emom = input.timers.emom
        return when (input.mode) {
            LiveMode.SET, LiveMode.CLOCK -> Texts(setTitle ?: context.getString(R.string.live_card_all_done), open ?: value, place)
            LiveMode.REST -> Texts(context.getString(R.string.live_card_rest), upcoming, place)
            LiveMode.REST_FINISHED -> Texts(context.getString(R.string.live_card_rest_finished), open ?: upcoming, place)
            LiveMode.WARMUP -> Texts(context.getString(R.string.live_card_warmup), upcoming, place)
            LiveMode.WARMUP_FINISHED -> Texts(context.getString(R.string.live_card_warmup_finished), upcoming, place)
            LiveMode.EMOM -> Texts(emom?.title ?: "", emom?.text, emom?.detail)
            LiveMode.EMOM_FINISHED -> Texts(input.timers.emomFinishedTitle ?: "", input.timers.emomFinishedText, place)
            LiveMode.ALL_DONE -> {
                val res = context.resources
                val summary = listOf(
                    res.getQuantityString(R.plurals.live_card_sets, card.doneSets, card.doneSets),
                    res.getQuantityString(R.plurals.live_card_exercises, card.doneExercises, card.doneExercises),
                    elapsedText(input.nowMillis - card.startedAtMillis)
                ).joinToString(" · ")
                Texts(context.getString(R.string.live_card_all_done), summary, card.volume?.let { context.getString(R.string.live_card_volume, it) })
            }
        }
    }

    /** "Panca piana · serie 3 di 4", with "Superset · " in front within a superset. */
    private fun setTitle(card: WorkoutLiveCard): String? {
        val name = card.exerciseName ?: return null
        val title = card.setNumber?.let { context.getString(R.string.live_card_set_title, name, it, card.setsInExercise) } ?: name
        return if (card.inSuperset) context.getString(R.string.live_card_superset) + " · " + title else title
    }

    /** "80 kg × 8 · l'ultima volta 7 · 75%". */
    private fun valueLine(card: WorkoutLiveCard): String? = listOfNotNull(
        card.value,
        card.previousReps?.let { context.getString(R.string.live_card_last_time, it) },
        card.detail
    ).joinToString(" · ").ifEmpty { null }

    /** "poi Croci ai cavi": the exercise after this one, where the card has room for a second line. */
    private fun placeLine(card: WorkoutLiveCard): String? =
        card.nextExerciseName?.let { context.getString(R.string.live_card_then, it) }

    /** "Esercizio 2 di 6", next to the workout's name: Live Updates show no second text line. */
    private fun exercisePlace(card: WorkoutLiveCard): String? = when {
        card.allDone -> context.getString(R.string.live_card_exercises_done, card.doneExercises, card.exerciseCount)
        card.exerciseNumber > 0 -> context.getString(R.string.live_card_exercise_of, card.exerciseNumber, card.exerciseCount)
        else -> null
    }

    private fun workoutName(card: WorkoutLiveCard): String = card.workoutName
        ?: context.getString(if (card.cardioOnly) R.string.workout_title_cardio else R.string.workout_title_quick)

    /** The time the system counts on the card: the countdown on, else the set's clock, else the whole workout. */
    private fun clock(input: LiveCardInput): Pair<Long, Boolean>? {
        val card = input.card
        val timers = input.timers
        return when (input.mode) {
            LiveMode.REST -> timers.restEndsAtMillis?.let { it to true }
            LiveMode.WARMUP -> timers.warmupEndsAtMillis?.let { it to true }
            LiveMode.EMOM -> timers.emom?.countdownEndsAt?.let { it to true }
            LiveMode.CLOCK -> card.clockEndsAtMillis?.let { it to true } ?: card.clockStartedAtMillis?.let { it to false }
            LiveMode.ALL_DONE -> null
            else -> card.startedAtMillis to false
        }
    }

    /** The short text of the status bar chip; null lets the system show the clock. */
    private fun chipText(input: LiveCardInput): String? {
        val card = input.card
        return when (input.mode) {
            LiveMode.SET -> card.setNumber?.let { "$it/${card.setsInExercise}" }
            LiveMode.REST_FINISHED, LiveMode.WARMUP_FINISHED -> context.getString(R.string.live_card_go)
            LiveMode.ALL_DONE, LiveMode.EMOM_FINISHED -> context.getString(R.string.live_card_done)
            else -> null
        }
    }

    /**
     * During a rest or a warm-up, where the bar stands: every set before the one to do is full and that one fills up
     * with the time gone, as the bar of the rest notification does. Null when the sets before it are not all done (a
     * set ticked out of order): then the bar shows the sets as they are.
     */
    private fun fillingProgress(input: LiveCardInput): Int? {
        val permille = input.countdownPermille ?: return null
        val current = input.segments.indexOf(LiveSegment.CURRENT)
        if (current < 0 || input.segments.take(current).any { it != LiveSegment.DONE }) return null
        return current * SEGMENT_UNITS + permille * SEGMENT_UNITS / 1000
    }

    private fun progressStyle(segments: List<LiveSegment>, filling: Int?): NotificationCompat.ProgressStyle {
        // Done the strongest, the set to do in between, the ones to come faint, in the shade's light or dark look
        val night = (android.content.res.Resources.getSystem().configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val doneColor = context.getColor(if (night) android.R.color.system_accent1_200 else android.R.color.system_accent1_600)
        val currentColor = context.getColor(if (night) android.R.color.system_accent1_500 else android.R.color.system_accent1_300)
        val todoColor = context.getColor(if (night) android.R.color.system_neutral2_600 else android.R.color.system_neutral2_200)
        val pieces = segments.ifEmpty { listOf(LiveSegment.TODO) }
        val style = NotificationCompat.ProgressStyle()
        if (filling != null) {
            // The system fills the bar up to the progress in the segments' colour and fades the rest
            return style.setStyledByProgress(true)
                .setProgressSegments(pieces.map { NotificationCompat.ProgressStyle.Segment(SEGMENT_UNITS).setColor(doneColor) })
                .setProgress(filling.coerceIn(0, pieces.size * SEGMENT_UNITS))
        }
        return style.setStyledByProgress(false)
            .setProgressSegments(pieces.map { segment ->
                NotificationCompat.ProgressStyle.Segment(SEGMENT_UNITS).setColor(
                    when (segment) {
                        LiveSegment.DONE -> doneColor
                        LiveSegment.CURRENT -> currentColor
                        LiveSegment.TODO -> todoColor
                    }
                )
            })
            .setProgress(pieces.count { it == LiveSegment.DONE } * SEGMENT_UNITS)
    }

    private fun actions(input: LiveCardInput, button: LiveButton): List<NotificationCompat.Action> {
        val emom = input.timers.emom
        return when (button) {
            LiveButton.SET_DONE -> listOf(
                // A tap in a pocket must not log a set: the phone has to be unlocked first
                NotificationCompat.Action.Builder(0, context.getString(R.string.next_set_done),
                    broadcast(TimerNotificationReceiver.ACTION_LIVE_SET_DONE, 30, input.card.sessionId))
                    .setAuthenticationRequired(true)
                    .build()
            )
            LiveButton.REST_SUBTRACT -> listOf(action("−${input.subtractSeconds}s", TimerNotificationReceiver.ACTION_SUBTRACT, 5, input))
            LiveButton.REST_ADD -> listOf(action("+${input.addSeconds}s", TimerNotificationReceiver.ACTION_ADD_30S, 2, input))
            LiveButton.REST_SKIP -> listOf(action(context.getString(R.string.skip_rest), TimerNotificationReceiver.ACTION_SKIP, 1, input))
            LiveButton.REST_MORE -> listOf(action("+${input.addSeconds}s", TimerNotificationReceiver.ACTION_REST_MORE, 6, input))
            LiveButton.REST_NEXT_SET -> listOf(action(context.getString(R.string.next_set_done), TimerNotificationReceiver.ACTION_REST_NEXT_SET, 7, input))
            LiveButton.REST_DISMISS -> listOf(action(context.getString(R.string.dismiss), TimerNotificationReceiver.ACTION_DISMISS, 3, input))
            LiveButton.WARMUP_SUBTRACT -> listOf(action("−${input.subtractSeconds}s", TimerNotificationReceiver.ACTION_WARMUP_SUBTRACT, 15, input))
            LiveButton.WARMUP_ADD -> listOf(action("+${input.addSeconds}s", TimerNotificationReceiver.ACTION_WARMUP_ADD_30S, 12, input))
            LiveButton.WARMUP_SKIP -> listOf(action(context.getString(R.string.skip_rest), TimerNotificationReceiver.ACTION_WARMUP_SKIP, 11, input))
            LiveButton.WARMUP_DISMISS -> listOf(action(context.getString(R.string.dismiss), TimerNotificationReceiver.ACTION_WARMUP_DISMISS, 13, input))
            LiveButton.EMOM_ACTIONS -> emom?.actions.orEmpty().map { NotificationCompat.Action(0, it.label, emomIntent(it.action)) }
            LiveButton.FINISH -> listOf(NotificationCompat.Action(0, context.getString(R.string.live_card_finish), openApp()))
        }
    }

    private fun action(label: String, action: String, requestCode: Int, input: LiveCardInput) =
        NotificationCompat.Action(0, label, broadcast(action, requestCode, input.card.sessionId))

    /** The same request codes as the rest and warm-up cards: a press does what it does there. */
    private fun broadcast(action: String, requestCode: Int, sessionId: Int): PendingIntent {
        val intent = Intent(context, TimerNotificationReceiver::class.java).apply {
            this.action = action
            putExtra(TimerNotificationReceiver.EXTRA_SESSION_ID, sessionId)
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun emomIntent(action: EmomAction): PendingIntent {
        val (name, requestCode) = when (action) {
            EmomAction.DONE -> TimerNotificationReceiver.ACTION_EMOM_DONE to 21
            EmomAction.PAUSE -> TimerNotificationReceiver.ACTION_EMOM_PAUSE to 22
            EmomAction.RESUME -> TimerNotificationReceiver.ACTION_EMOM_RESUME to 23
            EmomAction.STOP -> TimerNotificationReceiver.ACTION_EMOM_STOP to 24
            EmomAction.BOUNDARY -> TimerNotificationReceiver.ACTION_EMOM_BOUNDARY to 25
        }
        val intent = Intent(context, TimerNotificationReceiver::class.java).apply { this.action = name }
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun openApp(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        /** Length of one set on the bar: a rest fills it a thousandth at a time. */
        const val SEGMENT_UNITS = 1000
    }

    /** "58:40", "1:02:05". */
    private fun elapsedText(millis: Long): String {
        val total = (millis / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
