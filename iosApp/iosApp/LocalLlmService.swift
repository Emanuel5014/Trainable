import Foundation
import LiteRTLM
import Shared

/// Gemma E2B on the iPhone through LiteRT-LM 0.18.1, for the Kotlin side (`IosLocalLlm`, A5).
/// Configuration measured on an iPhone 15 Pro (docs/ios-port/checks/a5-local-gemma): decoder on the CPU, vision on the
/// GPU, near-greedy sampler. The engine stays loaded between scans until `release()` or another model path.
final class LocalLlmService: NSObject, IosLocalLlm {
    private let state = LocalLlmState()

    func generate(
        modelPath: String,
        cacheDir: String,
        imageJpeg: Data?,
        prompt: String,
        maxOutputTokens: Int32,
        onChunk: @escaping (String) -> Void,
        onFinished: @escaping (String?) -> Void
    ) -> any IosLocalLlmRun {
        let run = LocalLlmRun()
        let state = self.state
        run.task = Task.detached(priority: .userInitiated) {
            do {
                let session = try await state.conversation(modelPath: modelPath, cacheDir: cacheDir)
                if !run.attach(session) {
                    onFinished(IosLocalLlmCompanion.shared.CANCELED)
                    return
                }
                let message: Message
                if let imageJpeg {
                    message = Message(of: .imageData(imageJpeg), .text(prompt))
                } else {
                    message = Message(of: .text(prompt))
                }
                let stream = session.sendMessageStream(message, maxOutputTokens: Int(maxOutputTokens))
                for try await chunk in stream {
                    onChunk(chunk.toString)
                }
                onFinished(run.isCanceled ? IosLocalLlmCompanion.shared.CANCELED : nil)
            } catch {
                onFinished(run.isCanceled ? IosLocalLlmCompanion.shared.CANCELED : error.localizedDescription)
            }
        }
        return run
    }

    func unload() {
        let state = self.state
        Task { await state.release() }
    }
}

/// Loads the engine once per model file and gives each scan a fresh conversation.
private actor LocalLlmState {
    private var engine: Engine?
    private var loadedPath: String?

    func conversation(modelPath: String, cacheDir: String) async throws -> Conversation {
        if engine == nil || loadedPath != modelPath {
            engine = nil
            ExperimentalFlags.optIntoExperimentalAPIs()
            try FileManager.default.createDirectory(atPath: cacheDir, withIntermediateDirectories: true)
            let config = try EngineConfig(
                modelPath: modelPath, backend: .cpu(), visionBackend: .gpu, maxNumTokens: nil, cacheDir: cacheDir
            )
            let loaded = Engine(engineConfig: config)
            try await loaded.initialize()
            engine = loaded
            loadedPath = modelPath
        }
        let sampling = try SamplerConfig(topK: 1, topP: 1, temperature: 0.1, seed: 0)
        return try await engine!.createConversation(with: ConversationConfig(samplerConfig: sampling))
    }

    func release() {
        engine = nil
        loadedPath = nil
    }
}

/// One generation: cancelling before the conversation exists marks it, after that cancels the conversation itself.
private final class LocalLlmRun: NSObject, IosLocalLlmRun {
    var task: Task<Void, Never>?
    private let lock = NSLock()
    private var conversation: Conversation?
    private var canceled = false

    var isCanceled: Bool {
        lock.lock(); defer { lock.unlock() }
        return canceled
    }

    /// False when the run was canceled before the conversation was ready.
    func attach(_ session: Conversation) -> Bool {
        lock.lock(); defer { lock.unlock() }
        conversation = session
        return !canceled
    }

    func cancel() {
        lock.lock()
        canceled = true
        let session = conversation
        lock.unlock()
        try? session?.cancel()
    }
}
