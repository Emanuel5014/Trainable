import ActivityKit
import Foundation
import Shared
import UIKit
import UserNotifications

/// ActivityKit is Swift-only. This is the narrow adapter installed before the Kotlin UI starts.
final class WorkoutLiveActivityService: NSObject, IosWorkoutLiveActivities {
    override init() {
        super.init()
        DispatchQueue.main.async { TimerLiveActivityCoordinator.shared.refresh() }
    }

    func isAvailable() -> Bool { ActivityAuthorizationInfo().areActivitiesEnabled }

    func update(timer: IosLiveTimer) {
        let endsAt = Date(timeIntervalSince1970: Double(timer.endsAtMillis) / 1000)
        let state = TrainableTimerAttributes.ContentState(
            instanceID: timer.instanceId,
            title: String(timer.title.prefix(160)),
            detail: String(timer.detail.prefix(300)),
            startedAt: endsAt.addingTimeInterval(-Double(max(timer.totalSeconds, 1))),
            endsAt: endsAt, sessionID: Int(timer.sessionId), sessionStartedAtMillis: timer.sessionStartedAtMillis,
            addSeconds: Int(timer.addSeconds), subtractSeconds: Int(timer.subtractSeconds),
            showAdd: timer.showAdd, showSubtract: timer.showSubtract, showSkip: timer.showSkip,
            skipLabel: String(timer.skipLabel.prefix(50)), finishedText: String(timer.finishedText.prefix(160)),
            exercise: String(timer.exercise.prefix(120)), setLine: String(timer.setLine.prefix(200)),
            accentARGB: timer.accentArgb,
            roundStartsAt: timer.roundStartsAtMillis > 0 ? Date(timeIntervalSince1970: Double(timer.roundStartsAtMillis) / 1000) : nil,
            roundEndsAt: timer.roundEndsAtMillis > 0 ? Date(timeIntervalSince1970: Double(timer.roundEndsAtMillis) / 1000) : nil,
            paused: timer.paused,
            actionKeys: Array(timer.actionKeys.prefix(4)), actionLabels: Array(timer.actionLabels.prefix(4)).map { String($0.prefix(30)) })
        let kind = timer.kind
        DispatchQueue.main.async { TimerLiveActivityCoordinator.shared.set(kind: kind, state: state) }
    }

    func cancel(kind: String) {
        DispatchQueue.main.async { TimerLiveActivityCoordinator.shared.cancel(kind: kind) }
    }

    func cancelAll() {
        DispatchQueue.main.async { TimerLiveActivityCoordinator.shared.cancelAll() }
    }

    func cancelSession(sessionId: Int32) {
        let session = Int(sessionId)
        DispatchQueue.main.async { TimerLiveActivityCoordinator.shared.cancel(session: session) }
    }
}

/// Serializes ActivityKit mutations and coalesces cancel/start pairs into the latest desired countdown.
/// No background execution is assumed: SwiftUI owns the ticking text; local notifications own the alert.
@MainActor
final class TimerLiveActivityCoordinator {
    static let shared = TimerLiveActivityCoordinator()
    typealias TimerActivity = Activity<TrainableTimerAttributes>
    typealias State = TrainableTimerAttributes.ContentState
    private let kinds: Set<String> = ["workout.rest", "workout.warmup", "workout.emom"]
    private var desired: [String: State] = [:]
    private var knownInstances: [String: String] = [:]
    private var dismissedInstances: [String: String] = [:]
    private var revision = 0
    private var synchronization: Task<Void, Never>?
    private var expirations: [String: Task<Void, Never>] = [:]
    private var observers: [NSObjectProtocol] = []

    private init() {
        // Existing activities survive process recreation. Restore only their display state, never workout data.
        for activity in TimerActivity.activities where kinds.contains(activity.attributes.kind) {
            let kind = activity.attributes.kind
            let state = activity.content.state
            if isRunning(activity), state.endsAt > Date() {
                desired[kind] = state
                knownInstances[kind] = state.instanceID
            }
        }
        for name in [UIApplication.didBecomeActiveNotification, UIApplication.significantTimeChangeNotification] {
            observers.append(NotificationCenter.default.addObserver(forName: name, object: nil, queue: .main) { [weak self] _ in
                Task { @MainActor in self?.refresh() }
            })
        }
    }

    func set(kind: String, state: State) {
        guard kinds.contains(kind), state.startedAt < state.endsAt else { return }
        // Respect a user's dismissal until the next logical timer, rather than immediately recreating it.
        guard dismissedInstances[kind] != state.instanceID else { return }
        desired[kind] = state
        changed()
    }

    func cancel(kind: String) {
        desired.removeValue(forKey: kind)
        dismissedInstances.removeValue(forKey: kind)
        changed()
    }

    func cancelAll() {
        desired.removeAll()
        dismissedInstances.removeAll()
        changed()
    }

    /// The workout was deleted: end its cards, also those restored after a relaunch, and the alerts still pending
    /// for them. Cards of another workout stay.
    func cancel(session: Int) {
        var ended = Set(desired.filter { $0.value.sessionID == session }.keys)
        for activity in TimerActivity.activities where kinds.contains(activity.attributes.kind)
            && activity.content.state.sessionID == session {
            ended.insert(activity.attributes.kind)
        }
        guard !ended.isEmpty else { return }
        for kind in ended {
            desired.removeValue(forKey: kind)
            dismissedInstances.removeValue(forKey: kind)
        }
        var alerts = ended.filter { $0 != "workout.emom" }
        if ended.contains("workout.emom") { alerts.formUnion((0..<60).map { "workout.emom.\($0)" }) }
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: Array(alerts))
        changed()
    }

    func refresh() { changed() }

    func perform(activityID: String, instanceID: String, action: String) async {
        guard let activity = TimerActivity.activities.first(where: { $0.id == activityID }),
              activity.content.state.instanceID == instanceID, isRunning(activity) else { return }
        let state = activity.content.state
        let request = IosLiveTimer(kind: activity.attributes.kind, instanceId: instanceID, title: state.title,
            detail: state.detail, endsAtMillis: Int64(state.endsAt.timeIntervalSince1970 * 1000),
            totalSeconds: Int32(clamping: Int(state.endsAt.timeIntervalSince(state.startedAt))),
            sessionId: Int32(clamping: state.sessionID), sessionStartedAtMillis: state.sessionStartedAtMillis,
            addSeconds: Int32(clamping: state.addSeconds), subtractSeconds: Int32(clamping: state.subtractSeconds),
            showAdd: state.showAdd, showSubtract: state.showSubtract, showSkip: state.showSkip,
            skipLabel: state.skipLabel, finishedText: state.finishedText,
            exercise: state.exercise, setLine: state.setLine, accentArgb: state.accentARGB,
            roundStartsAtMillis: state.roundStartsAt.map { Int64($0.timeIntervalSince1970 * 1000) } ?? 0,
            roundEndsAtMillis: state.roundEndsAt.map { Int64($0.timeIntervalSince1970 * 1000) } ?? 0,
            paused: state.paused, actionKeys: state.actionKeys, actionLabels: state.actionLabels)
        _ = try? await IosWorkoutTimerActions.shared.perform(request: request, action: action)
        // Kotlin forwards ActivityKit mutations through the serial main queue. Drain it before the intent exits.
        await withCheckedContinuation { continuation in
            DispatchQueue.main.async { continuation.resume() }
        }
        refresh()
        while let pending = synchronization { await pending.value }
    }

    private func changed() {
        revision += 1
        guard synchronization == nil else { return }
        synchronization = Task { [weak self] in
            guard let self else { return }
            while true {
                let currentRevision = revision
                await reconcile()
                if revision == currentRevision { break }
            }
            synchronization = nil
            armExpirations()
        }
    }

    private func isRunning(_ activity: TimerActivity) -> Bool {
        activity.activityState == .active || activity.activityState == .stale
    }

    private func reconcile() async {
        let now = Date()
        desired = desired.filter { $0.value.endsAt > now }
        if !ActivityAuthorizationInfo().areActivitiesEnabled { desired.removeAll() }
        let snapshot = desired
        let activities = TimerActivity.activities.filter { kinds.contains($0.attributes.kind) }
        for activity in activities where snapshot[activity.attributes.kind] == nil {
            await activity.end(nil, dismissalPolicy: .immediate)
            knownInstances.removeValue(forKey: activity.attributes.kind)
        }
        for (kind, state) in snapshot {
            let candidates = activities.filter { $0.attributes.kind == kind && isRunning($0) }
            let existing = candidates.first { $0.content.state.instanceID == state.instanceID } ?? candidates.first
            for duplicate in candidates where duplicate.id != existing?.id {
                await duplicate.end(nil, dismissalPolicy: .immediate)
            }
            // An EMOM card turns stale when its minute ends without the app awake; it then shows the run's end.
            let content = ActivityContent(state: state, staleDate: state.roundEndsAt ?? state.endsAt,
                relevanceScore: kind == "workout.warmup" ? 50 : 100)
            if let existing {
                if existing.content.state != state { await existing.update(content) }
                knownInstances[kind] = state.instanceID
            } else if knownInstances[kind] == state.instanceID {
                // The OS/user removed the activity while we were suspended.
                dismissedInstances[kind] = state.instanceID
                desired.removeValue(forKey: kind)
                knownInstances.removeValue(forKey: kind)
            } else if UIApplication.shared.applicationState == .active {
                do {
                    _ = try TimerActivity.request(attributes: TrainableTimerAttributes(kind: kind),
                        content: content, pushType: nil)
                    knownInstances[kind] = state.instanceID
                } catch {
                    // Notification delivery remains available; no new alert, prompt, or workout behavior.
                    NSLog("Trainable: unable to start timer Live Activity")
                }
            }
        }
    }

    private func armExpirations() {
        expirations.values.forEach { $0.cancel() }
        expirations.removeAll()
        for (kind, state) in desired {
            let delay = max(0, state.endsAt.timeIntervalSinceNow)
            expirations[kind] = Task { [weak self] in
                do { try await Task.sleep(nanoseconds: UInt64(min(delay, 86_400) * 1_000_000_000)) }
                catch { return }
                guard let self, desired[kind] == state else { return }
                // Executes at expiry while runnable, otherwise on resume. This is NOT a background alarm.
                refresh()
            }
        }
    }
}
