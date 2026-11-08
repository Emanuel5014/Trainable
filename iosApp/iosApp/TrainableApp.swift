import SwiftUI
import CryptoKit
import BackgroundTasks
import Shared

@main
struct TrainableApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        // AES-GCM for the physical checks: CryptoKit is Swift only, Kotlin calls it through this bridge
        IosCryptoBridge.shared.aesGcm = CryptoKitAesGcm()
        // On-device Gemma E2B (LiteRT-LM is Swift only); used by the local scan backend (A5)
        IosLocalLlmBridge.shared.engine = LocalLlmService()
        IosWorkoutLiveActivityBridge.shared.service = WorkoutLiveActivityService()
        // Automatic backups (B4): the system task must be registered before launch ends
        IosBackgroundTasksBridge.shared.tasks = AutoBackupTasks()
        AutoBackupTasks.register()
        IosAutoBackup.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in IosRoutineFiles.shared.open(url: url) }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .background { IosAutoBackup.shared.onEnteredBackground() }
        }
        .backgroundTask(.urlSession(IosWebDavHttp.companion.BACKGROUND_SESSION)) {
            IosAutoBackup.shared.reconnectUploads()
        }
    }
}

/// The automatic backup as a BGProcessingTask; Kotlin decides when (IosBackupScheduler) and what runs (IosAutoBackup).
final class AutoBackupTasks: NSObject, IosBackgroundTasks {
    static let identifier = "com.emanuel5014.trainable.autobackup"

    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: identifier, using: nil) { task in
            task.expirationHandler = { IosAutoBackup.shared.cancelScheduled() }
            IosAutoBackup.shared.runScheduled { ok in task.setTaskCompleted(success: ok.boolValue) }
        }
    }

    func submit(earliestMillis: Int64, needsNetwork: Bool) {
        let request = BGProcessingTaskRequest(identifier: Self.identifier)
        request.earliestBeginDate = Date(timeIntervalSince1970: Double(earliestMillis) / 1000)
        request.requiresNetworkConnectivity = needsNetwork
        request.requiresExternalPower = false
        try? BGTaskScheduler.shared.submit(request)
    }

    func cancel() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.identifier)
    }
}

/// AES-256-GCM with a 12-byte nonce and a 16-byte tag after the ciphertext, the layout Android's Cipher uses.
final class CryptoKitAesGcm: NSObject, IosAesGcm {
    func seal(key: Data, nonce: Data, plaintext: Data) -> Data? {
        guard let nonce = try? AES.GCM.Nonce(data: nonce),
              let box = try? AES.GCM.seal(plaintext, using: SymmetricKey(data: key), nonce: nonce) else { return nil }
        return box.ciphertext + box.tag
    }

    func open(key: Data, nonce: Data, sealed: Data) -> Data? {
        guard sealed.count >= 16, let nonce = try? AES.GCM.Nonce(data: nonce),
              let box = try? AES.GCM.SealedBox(nonce: nonce, ciphertext: sealed.dropLast(16), tag: sealed.suffix(16))
        else { return nil }
        return try? AES.GCM.open(box, using: SymmetricKey(data: key))
    }
}
