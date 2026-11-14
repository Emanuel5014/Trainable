import AudioToolbox
import HealthKit
import os
import Shared
import UIKit

/// Long rest-end vibration (the "Lock Screen vibration" setting, as on Android).
///
/// A notification buzzes once and a suspended app cannot vibrate, so during rests a HealthKit workout session
/// (iPhone, iOS 26+) keeps the app running; at the end of the rest it vibrates for the chosen seconds,
/// 1 s on and 1 s off like Android, unless the app is in front. Nothing is saved to Health: the session has no builder.
final class RestVibrationService: NSObject, IosRestVibration {
    private let log = Logger(subsystem: "com.emanuel5014.trainable", category: "RestVibration")
    private var restEnd: DispatchSourceTimer?
    private var buzzing: DispatchSourceTimer?
    private var idle: DispatchSourceTimer?
    private var keepAlive: AnyObject?
    /// After the last rest, the session stays this long for the next set, then ends by itself.
    private let idleSeconds: TimeInterval = 15 * 60

    override init() {
        super.init()
        NotificationCenter.default.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil,
                                               queue: .main) { [weak self] _ in self?.stopVibration() }
        // Closing the app from the app switcher: the session would outlive the process, so end it now
        NotificationCenter.default.addObserver(forName: UIApplication.willTerminateNotification, object: nil,
                                               queue: .main) { [weak self] _ in self?.endKeepAlive() }
        // A session left by a previous process (closed or crashed app) still shows in the Dynamic Island:
        // keep it only if a rest has been armed meanwhile, otherwise end it
        if #available(iOS 26.0, *), HKHealthStore.isHealthDataAvailable() {
            let session = WorkoutKeepAlive(log: log)
            keepAlive = session
            session.recover { [weak self] in self?.restEnd != nil }
        }
    }

    func isSupported() -> Bool {
        guard #available(iOS 26.0, *) else { return false }
        return HKHealthStore.isHealthDataAvailable()
    }

    func scheduleRestEnd(endsAtMillis: Int64, seconds: Int32) {
        guard isSupported(), seconds > 0 else { cancelRestEnd(); return }
        startKeepAlive()
        idle?.cancel(); idle = nil
        restEnd?.cancel()
        let delay = max(0, Double(endsAtMillis) / 1000 - Date().timeIntervalSince1970)
        let timer = DispatchSource.makeTimerSource(queue: .main)
        timer.schedule(deadline: .now() + delay, leeway: .milliseconds(50))
        timer.setEventHandler { [weak self] in
            guard let self else { return }
            self.restEnd?.cancel(); self.restEnd = nil
            let state = UIApplication.shared.applicationState
            self.log.info("rest end, app state \(state.rawValue), vibrate \(seconds) s")
            if state != .active { self.vibrate(seconds: Int(seconds)) }
            self.armIdle()
        }
        restEnd = timer
        timer.resume()
        log.info("rest end armed in \(delay) s")
    }

    func cancelRestEnd() {
        restEnd?.cancel(); restEnd = nil
        if keepAlive != nil { armIdle() }
    }

    func stopVibration() {
        buzzing?.cancel(); buzzing = nil
    }

    func endKeepAlive() {
        cancelRestEnd()
        stopVibration()
        idle?.cancel(); idle = nil
        if #available(iOS 26.0, *) { (keepAlive as? WorkoutKeepAlive)?.stop() }
        keepAlive = nil
    }

    /// Two system buzzes back to back (about 1 s), then 1 s of pause, until the time is up.
    private func vibrate(seconds: Int) {
        stopVibration()
        let started = Date()
        let timer = DispatchSource.makeTimerSource(queue: .main)
        timer.schedule(deadline: .now(), repeating: .milliseconds(2000), leeway: .milliseconds(20))
        timer.setEventHandler { [weak self] in
            guard Date().timeIntervalSince(started) < Double(seconds) else { self?.stopVibration(); return }
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                guard self?.buzzing === timer, Date().timeIntervalSince(started) < Double(seconds) else { return }
                AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
            }
        }
        buzzing = timer
        timer.resume()
    }

    private func startKeepAlive() {
        guard #available(iOS 26.0, *) else { return }
        let session = (keepAlive as? WorkoutKeepAlive) ?? WorkoutKeepAlive(log: log)
        keepAlive = session
        session.start()
    }

    private func armIdle() {
        idle?.cancel()
        let timer = DispatchSource.makeTimerSource(queue: .main)
        timer.schedule(deadline: .now() + idleSeconds)
        timer.setEventHandler { [weak self] in
            self?.log.info("no rest for \(Int(self?.idleSeconds ?? 0)) s, ending the session")
            self?.endKeepAlive()
        }
        idle = timer
        timer.resume()
    }
}

@available(iOS 26.0, *)
private final class WorkoutKeepAlive: NSObject, HKWorkoutSessionDelegate {
    private let store = HKHealthStore()
    private let log: Logger
    private var session: HKWorkoutSession?
    private var asking = false
    /// [start] came while [recover] was still asking: start once it is done.
    private var wantsStart = false

    init(log: Logger) { self.log = log }

    func recover(keep: @escaping () -> Bool) {
        guard session == nil, !asking else { return }
        asking = true
        store.recoverActiveWorkoutSession { [weak self] recovered, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.asking = false
                if let recovered {
                    if keep() {
                        recovered.delegate = self
                        self.session = recovered
                        self.log.info("workout session recovered and kept")
                    } else {
                        recovered.end()
                        self.log.info("workout session left by a previous run, ended")
                    }
                } else if let error {
                    self.log.info("no workout session to recover: \(error.localizedDescription)")
                }
                if self.wantsStart { self.wantsStart = false; self.start() }
            }
        }
    }

    func start() {
        guard session == nil else { return }
        guard !asking else { wantsStart = true; return }
        asking = true
        // Asks only the first time; afterwards it answers at once
        store.requestAuthorization(toShare: [HKObjectType.workoutType()], read: []) { [weak self] granted, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.asking = false
                if !granted { self.log.error("Health permission refused: \(String(describing: error))"); return }
                self.begin()
            }
        }
    }

    private func begin() {
        guard session == nil else { return }
        let configuration = HKWorkoutConfiguration()
        configuration.activityType = .traditionalStrengthTraining
        configuration.locationType = .indoor
        do {
            let session = try HKWorkoutSession(healthStore: store, configuration: configuration)
            session.delegate = self
            self.session = session
            session.startActivity(with: Date())
            log.info("workout session started")
        } catch {
            log.error("workout session failed: \(error.localizedDescription)")
        }
    }

    func stop() {
        wantsStart = false
        session?.end()
        session = nil
        log.info("workout session ended")
    }

    func workoutSession(_ workoutSession: HKWorkoutSession, didChangeTo toState: HKWorkoutSessionState,
                        from fromState: HKWorkoutSessionState, date: Date) {
        log.info("workout session \(fromState.rawValue) -> \(toState.rawValue)")
        if toState == .ended || toState == .stopped {
            DispatchQueue.main.async { if self.session === workoutSession { self.session = nil } }
        }
    }

    func workoutSession(_ workoutSession: HKWorkoutSession, didFailWithError error: any Error) {
        log.error("workout session error: \(error.localizedDescription)")
        DispatchQueue.main.async { if self.session === workoutSession { self.session = nil } }
    }
}
