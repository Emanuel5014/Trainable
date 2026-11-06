import SwiftUI
import CryptoKit
import Shared

@main
struct TrainableApp: App {
    init() {
        // AES-GCM for the physical checks: CryptoKit is Swift only, Kotlin calls it through this bridge
        IosCryptoBridge.shared.aesGcm = CryptoKitAesGcm()
        IosWorkoutLiveActivityBridge.shared.service = WorkoutLiveActivityService()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
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
