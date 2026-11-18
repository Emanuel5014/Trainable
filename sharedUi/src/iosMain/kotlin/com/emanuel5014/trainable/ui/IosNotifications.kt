@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.emanuel5014.trainable.ui

import androidx.compose.runtime.*
import com.emanuel5014.trainable.data.repository.UserPreferencesRepository
import com.emanuel5014.trainable.util.WeightUnitConverter
import com.emanuel5014.trainable.sharedui.resources.*
import com.emanuel5014.trainable.ui.screens.settings.NotificationSettingsContent
import com.emanuel5014.trainable.ui.screens.settings.SettingsState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.*
import platform.UIKit.*
import platform.UserNotifications.*
import platform.darwin.NSObject
import kotlin.time.Clock
import kotlin.time.Instant

internal const val REST_NOTICE = "workout.rest"
internal const val WARMUP_NOTICE = "workout.warmup"
internal const val EMOM_NOTICE = "workout.emom"
internal const val MEMBERSHIP_NOTICE = "membership.expiry"
internal fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

internal data class IosNotice(val id: String, val title: String, val body: String, val at: Long,
    val calendar: Boolean = false, val onScheduled: () -> Unit = {})

/** Callbacks run on the main thread, just like the workout model and settings actions. */
internal interface IosNotificationDelivery {
    val canAuthorize: Boolean
    fun authorize(prompt: Boolean, result: (Boolean) -> Unit)
    fun put(notice: IosNotice)
    fun remove(ids: List<String>, delivered: Boolean)
}

/** Invalidate pending permission callbacks when a timer is skipped, replaced, or notifications are disabled. */
internal class IosNotificationScheduler(private val delivery: IosNotificationDelivery) {
    private val desired = mutableMapOf<String, IosNotice>()
    val canAuthorize get() = delivery.canAuthorize
    fun schedule(notices: List<IosNotice>, prompt: Boolean = false) {
        notices.forEach { desired[it.id] = it }
        delivery.authorize(prompt) { granted ->
            if (granted) notices.filter { desired[it.id] === it }.forEach { notice ->
                delivery.put(notice.copy(onScheduled = { if (desired[notice.id] === notice) notice.onScheduled() }))
            }
        }
    }
    fun cancel(ids: List<String>, delivered: Boolean = true) {
        ids.forEach(desired::remove)
        delivery.remove(ids, delivered)
    }
}

private class AppleNotificationDelivery : IosNotificationDelivery {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val operations = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private var status = UNAuthorizationStatusNotDetermined
    // Not-determined is eligible: scheduling a user-started timer can request permission asynchronously.
    override val canAuthorize get() = status != UNAuthorizationStatusDenied
    private val waiting = mutableListOf<(Boolean) -> Unit>()
    private var requesting = false
    // UNUserNotificationCenter retains its delegate weakly; retain it for the process lifetime.
    private val delegate = object : NSObject(), UNUserNotificationCenterDelegateProtocol {
        override fun userNotificationCenter(center: UNUserNotificationCenter, willPresentNotification: UNNotification,
            withCompletionHandler: (UNNotificationPresentationOptions) -> Unit) {
            withCompletionHandler(UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionSound)
        }
    }
    init {
        center.delegate = delegate
        scope.launch { for (operation in operations) operation() }
        authorize(false) {}
    }
    override fun authorize(prompt: Boolean, result: (Boolean) -> Unit) {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            scope.launch {
                status = settings!!.authorizationStatus
                when (status) {
                    UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional, UNAuthorizationStatusEphemeral -> result(true)
                    UNAuthorizationStatusNotDetermined -> if (!prompt) result(false) else {
                        waiting += result
                        if (!requesting) {
                            requesting = true
                            center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
                                scope.launch {
                                    status = if (granted) UNAuthorizationStatusAuthorized else UNAuthorizationStatusDenied
                                    requesting = false
                                    val callbacks = waiting.toList(); waiting.clear()
                                    callbacks.forEach { it(granted) }
                                }
                            }
                        }
                    }
                    else -> result(false)
                }
            }
        }
    }
    override fun put(notice: IosNotice) {
        val content = UNMutableNotificationContent().apply {
            setTitle(notice.title); setBody(notice.body); setSound(UNNotificationSound.defaultSound)
        }
        operations.trySend {
            // Build relative triggers when the request is actually enqueued with iOS, after earlier mutations.
            val trigger = if (notice.calendar && notice.at > nowMillis()) {
                val date = NSDate.dateWithTimeIntervalSince1970(notice.at / 1000.0)
                val parts = NSCalendar.currentCalendar.components(
                    NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond,
                    fromDate = date)
                UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(parts, repeats = false)
            } else UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                ((notice.at - nowMillis()) / 1000.0).coerceAtLeast(0.1), repeats = false)
            val succeeded = suspendCoroutine<Boolean> { continuation ->
                center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(notice.id, content, trigger)) { error ->
                    continuation.resume(error == null)
                }
            }
            if (succeeded) notice.onScheduled()
        }
    }
    override fun remove(ids: List<String>, delivered: Boolean) {
        operations.trySend {
            center.removePendingNotificationRequestsWithIdentifiers(ids)
            if (delivered) center.removeDeliveredNotificationsWithIdentifiers(ids)
        }
    }
}

internal data class IosNotificationWords(val restTitle: String, val restBody: String, val warmupTitle: String,
    val warmupBody: String, val membershipTitle: String, val membershipTemplate: String,
    val emomRoundTemplate: String, val emomFinished: String, val emomReady: String,
    val nextSetTemplate: String = "Next: %1\$s - Set %2\$d: %3\$s × %4\$d",
    val nextSetDetailTemplate: String = "Next: %1\$s - Set %2\$d: %3\$s × %4\$s",
    val nextSetTextTemplate: String = "Next: %1\$s - Set %2\$d: %3\$s",
    val lastRepsTemplate: String = "(last: %1\$d)", val skipLabel: String = "Skip",
    val nextSetDoneLabel: String = "Set done", val openAppText: String = "Open Trainable to continue") {
    private fun fill(template: String, vararg args: Any): String =
        Regex("%(\\d+)\\$[sd]").replace(template) { match -> args[match.groupValues[1].toInt() - 1].toString() }

    /** Same branches and wording as Android main's TimerNotificationHelper.nextSetLabel. */
    fun nextSet(exercise: String?, number: Int?, weight: Float?, reps: Int?, previousReps: Int?, unit: String?,
        repsLabel: String?, detail: String?, timedText: String?): String {
        if (exercise != null && number != null && timedText != null) return fill(nextSetTextTemplate, exercise, number, timedText)
        if (exercise == null || number == null || weight == null || (reps == null && repsLabel == null) || unit == null) return ""
        val load = WeightUnitConverter.formatWithUnit(WeightUnitConverter.convertDisplay(weight, unit), unit)
        val base = if (repsLabel != null || detail != null) {
            fill(nextSetDetailTemplate, exercise, number, load, repsLabel ?: reps.toString()) + (detail?.let { " · $it" } ?: "")
        } else fill(nextSetTemplate, exercise, number, load, reps!!)
        return if (previousReps != null && previousReps != reps && repsLabel == null) "$base ${fill(lastRepsTemplate, previousReps)}" else base
    }
    /** [nextSet] as (exercise, rest of the line), using the same template; ("", "") when there is no next set. */
    fun nextSetParts(exercise: String?, number: Int?, weight: Float?, reps: Int?, previousReps: Int?, unit: String?,
        repsLabel: String?, detail: String?, timedText: String?): Pair<String, String> {
        val full = nextSet(exercise, number, weight, reps, previousReps, unit, repsLabel, detail, timedText)
        if (full.isEmpty() || exercise == null) return "" to ""
        val template = when {
            timedText != null -> nextSetTextTemplate
            repsLabel != null || detail != null -> nextSetDetailTemplate
            else -> nextSetTemplate
        }
        val lead = template.substringBefore("%1\$s", missingDelimiterValue = "\u0000") + exercise
        if (!full.startsWith(lead)) return "" to ""
        return exercise to full.removePrefix(lead).trimStart(' ', '-', '\u2013', '\u2014', ':', '\u00B7')
    }
    fun membership(days: Int) = membershipTemplate.replace("%1\$d", days.toString())
    fun round(round: Int, rounds: Int) = emomRoundTemplate.replace("%1\$d", round.toString()).replace("%2\$d", rounds.toString())
}

/** Process-wide delivery survives navigation and is shared by settings and all workout owners. */
internal object IosNotifications {
    private val delivery: IosNotificationDelivery by lazy { AppleNotificationDelivery() }
    val scheduler by lazy { IosNotificationScheduler(delivery) }
    var words: IosNotificationWords? = null
    private val emomIds = (0..59).map { "workout.emom.$it" }
    fun cancelEmom() = scheduler.cancel(emomIds)
    fun cancelTimers() {
        scheduler.cancel(listOf(REST_NOTICE, WARMUP_NOTICE) + emomIds)
        IosLiveTimerSnapshots.clear()
        IosWorkoutLiveActivityBridge.service?.cancelAll()
        IosRestVibrationBridge.service?.endKeepAlive()
    }
    fun permissionFromSettings(enabled: Boolean, save: (Boolean) -> Unit) {
        if (!enabled) { save(false); return }
        schedulerPermission { granted ->
            save(granted)
            if (!granted) UIApplication.sharedApplication.openURL(NSURL.URLWithString(UIApplicationOpenSettingsURLString)!!,
                options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }
    private fun schedulerPermission(result: (Boolean) -> Unit) = delivery.authorize(true, result)
    /** First-launch opt-in: denial simply keeps the selected alerts off, without opening Settings. */
    suspend fun authorizeForOnboarding(): Boolean = suspendCancellableCoroutine { continuation ->
        delivery.authorize(true) { granted -> if (continuation.isActive) continuation.resume(granted) }
    }
    fun refreshPermission() = delivery.authorize(false) {}
}

/** Calendar days, rather than 24-hour intervals, preserve the reminder date across daylight-saving changes. */
internal fun membershipNoticeAt(expiry: Long, daysBefore: Int, now: Long, zone: TimeZone): Pair<Long, Int>? {
    val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(zone).date
    val expiryDay = Instant.fromEpochMilliseconds(expiry).toLocalDateTime(zone).date
    val daysLeft = today.daysUntil(expiryDay)
    if (daysLeft < 0) return null
    val reminderDay = expiryDay.minus(daysBefore.coerceIn(1, 30), DateTimeUnit.DAY)
    val at = reminderDay.atTime(9, 0).toInstant(zone).toEpochMilliseconds().coerceAtLeast(now + 1000)
    val remaining = Instant.fromEpochMilliseconds(at).toLocalDateTime(zone).date.daysUntil(expiryDay)
    return at to remaining
}

@Composable
internal fun IosNotificationEffects(preferences: UserPreferencesRepository) {
    val words = IosNotificationWords(stringResource(Res.string.rest_timer), stringResource(Res.string.rest_end),
        stringResource(Res.string.warmup_timer), stringResource(Res.string.warmup_timer_finished),
        stringResource(Res.string.gym_membership_expiring), stringResource(Res.string.gym_membership_expiring_desc),
        stringResource(Res.string.emom_round_progress), stringResource(Res.string.emom_notification_finished_title), stringResource(Res.string.emom_notification_title_get_ready),
        stringResource(Res.string.notification_next_set), stringResource(Res.string.notification_next_set_detail),
        stringResource(Res.string.notification_next_set_text), stringResource(Res.string.notification_last_reps), stringResource(Res.string.skip_rest),
        stringResource(Res.string.next_set_done), stringResource(Res.string.open_app_to_continue))
    SideEffect { IosNotifications.words = words }
    var activation by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observers = listOf(UIApplicationDidBecomeActiveNotification, UIApplicationSignificantTimeChangeNotification).map { name ->
            center.addObserverForName(name, null, NSOperationQueue.mainQueue) { activation++ }
        }
        onDispose { observers.forEach(center::removeObserver) }
    }
    LaunchedEffect(activation, words) {
        // Refresh OS permission after returning from Settings, without showing a prompt on launch.
        IosNotifications.refreshPermission()
        combine(preferences.gymMembershipExpiryDate, preferences.gymMembershipExpiryNotificationsEnabled,
            preferences.gymMembershipExpiryNotificationDaysBefore, preferences.lastNotifiedExpiryDate) { expiry, enabled, days, last ->
            MembershipConfiguration(expiry, enabled, days, last)
        }.collect { config ->
            val scheduler = IosNotifications.scheduler
            scheduler.cancel(listOf(MEMBERSHIP_NOTICE), delivered = !config.enabled || config.expiry == null)
            val expiry = config.expiry ?: return@collect
            if (!config.enabled || config.last == expiry) return@collect
            val now = nowMillis()
            val defaults = NSUserDefaults.standardUserDefaults
            val previousExpiry = defaults.stringForKey("notifications.membership.expiry")?.toLongOrNull()
            val previousAt = defaults.stringForKey("notifications.membership.at")?.toLongOrNull()
            if (previousExpiry == expiry && previousAt != null && previousAt <= now) return@collect
            val (at, days) = membershipNoticeAt(expiry, config.days, now, TimeZone.currentSystemDefault()) ?: return@collect
            scheduler.schedule(listOf(IosNotice(MEMBERSHIP_NOTICE, words.membershipTitle, words.membership(days), at, calendar = true,
                onScheduled = {
                    defaults.setObject(expiry.toString(), forKey = "notifications.membership.expiry")
                    defaults.setObject(at.toString(), forKey = "notifications.membership.at")
                })))
        }
    }
    LaunchedEffect(Unit) {
        preferences.timerFinishedLockscreenVibrationDuration.collect { seconds ->
            IosRestVibrationBridge.seconds = seconds
            if (seconds == 0) IosRestVibrationBridge.service?.endKeepAlive()
        }
    }
    LaunchedEffect(Unit) {
        // EMOM sessions reopen paused; never leave a previous process's round cues running.
        IosNotifications.cancelEmom()
        preferences.timerNotificationsEnabled.collect { if (!it) IosNotifications.cancelTimers() }
    }
}

private data class MembershipConfiguration(val expiry: Long?, val enabled: Boolean, val days: Int, val last: Long)

@Composable
internal fun IosNotificationSettings(state: SettingsState, onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    NotificationSettingsContent(onNavigateBack, state,
        onTimerNotificationsChange = { enabled ->
            IosNotifications.permissionFromSettings(enabled) { state.setTimerNotificationsEnabled(it) }
        }, onMembershipNotificationsChange = { enabled ->
            IosNotifications.permissionFromSettings(enabled) { allowed ->
                scope.launch { IosAppGraph.preferences.setGymMembershipExpiryNotificationsEnabled(allowed) }
            }
        }, onMembershipDaysBeforeChange = { days ->
            scope.launch { IosAppGraph.preferences.setGymMembershipExpiryNotificationDaysBefore(days) }
        }, customVibrationAvailable = IosRestVibrationBridge.service?.isSupported() == true)
}
