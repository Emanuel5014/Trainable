import SwiftUI
import PhotosUI
import UniformTypeIdentifiers
import LiteRTLM
import Darwin
import CryptoKit
import os

@main
struct GemmaSpikeApp: App {
    var body: some Scene {
        WindowGroup { GemmaSpikeView() }
    }
}

private enum ModelVariant: String, CaseIterable, Identifiable {
    case e2b = "E2B"
    case e4b = "E4B"

    var id: String { rawValue }
    var fileName: String { "gemma-4-\(rawValue)-it.litertlm" }
    var bytes: Int64 { self == .e2b ? 2_588_147_712 : 3_659_530_240 }

    static func recognize(_ file: URL) throws -> ModelVariant {
        let handle = try FileHandle(forReadingFrom: file)
        defer { try? handle.close() }
        let header = try handle.read(upToCount: 12) ?? Data()
        let size = try file.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0
        guard header.count == 12, header.prefix(8) == Data("LITERTLM".utf8),
              Array(header.suffix(4)) == [1, 0, 0, 0],
              let variant = allCases.first(where: { abs(Double(size) / Double($0.bytes) - 1) <= 0.03 })
        else { throw SpikeError.message("Il file non corrisponde ai modelli E2B/E4B di Trainable.") }
        return variant
    }
}

private enum ComputeMode: String, CaseIterable, Identifiable {
    case android = "CPU + visione GPU"
    case metal = "GPU + visione GPU"
    var id: String { rawValue }
    var decoder: Backend { self == .android ? .cpu() : .gpu }
}

private enum SpikeError: LocalizedError {
    case message(String)
    var errorDescription: String? {
        switch self { case .message(let message): return message }
    }
}

private struct BenchmarkRequest: Decodable {
    let id: String
    let model: String
    let backend: String
    let imageFile: String?
    let promptFile: String?
    let transcription: Bool
    let maxOutputTokens: Int?
    let cancelAfterSeconds: Double?
    let contextTokens: Int?
    let visionBackend: String?
    let freshCache: Bool?
}

private struct RunReport: Codable, Identifiable {
    var id = UUID()
    var startedAt = Date()
    var model = ""
    var backend = ""
    var runtimeVersion = "0.18.1"
    var hardwareModel: String = {
        var system = utsname()
        uname(&system)
        return withUnsafeBytes(of: &system.machine) { bytes in
            String(decoding: bytes.prefix { $0 != 0 }, as: UTF8.self)
        }
    }()
    var systemVersion = UIDevice.current.systemVersion
    var physicalMemoryBytes = ProcessInfo.processInfo.physicalMemory
    var imageSource = ""
    var imageBytes = 0
    var initialFootprintBytes: UInt64 = 0
    var sampledPeakFootprintBytes: UInt64 = 0
    var initializationSeconds: Double?
    var firstChunkSeconds: Double?
    var totalSeconds: Double?
    var decodeTokens: Int?
    var decodeTokensPerSecond: Double?
    var output = ""
    var thinking = ""
    var outcome = "running"
    var thermalState = ""
    var benchmarkID: String?
    var promptSHA256: String?
    var imageSHA256: String?
    var transcription: Bool?
    var maxOutputTokens: Int?
    var stage: String?
    var initialThermalState: String?
    var contextTokens: Int?
    var visionBackend: String?
    var freshCache: Bool?
    var initialAvailableMemoryBytes: UInt64?
    var minimumAvailableMemoryBytes: UInt64?
}

@MainActor
private final class SpikeState: ObservableObject {
    @Published var variant = ModelVariant.e2b
    @Published var mode = ComputeMode.android
    @Published var busy = false
    @Published private(set) var canCancel = false
    @Published var phase = "Importa un modello .litertlm da File."
    @Published var output = ""
    @Published var reportURL: URL?
    @Published var selectedImage: Data?
    @Published var models: Set<ModelVariant> = []
    private var conversation: Conversation?
    private var cancelRequested = false
    private var reports: [RunReport] = []

    private var documents: URL { FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0] }
    private var modelsDirectory: URL { documents.appendingPathComponent("models", isDirectory: true) }

    init() {
        let url = documents.appendingPathComponent("spike-results.json")
        if let data = try? Data(contentsOf: url),
           let saved = try? JSONDecoder().decode([RunReport].self, from: data) {
            reports = saved.map { saved in
                var recovered = saved
                if recovered.outcome == "running" { recovered.outcome = "incomplete" }
                return recovered
            }
            reportURL = url
            try? saveReports()
        }
        refreshModels()
        if ProcessInfo.processInfo.arguments.contains("--benchmark") {
            Task { runLaunchBenchmark() }
        }
    }

    private func runLaunchBenchmark() {
        do {
            let arguments = ProcessInfo.processInfo.arguments
            guard let index = arguments.firstIndex(of: "--benchmark"), index + 1 < arguments.count else {
                throw SpikeError.message("Specifica il file JSON del benchmark.")
            }
            let request = try JSONDecoder().decode(BenchmarkRequest.self, from: Data(contentsOf: benchmarkFile(arguments[index + 1])))
            guard let model = ModelVariant(rawValue: request.model), ["cpu", "gpu"].contains(request.backend),
                  request.maxOutputTokens.map({ $0 > 0 && $0 <= 8192 }) ?? true,
                  request.contextTokens.map({ $0 >= 1536 && $0 <= 32768 }) ?? true,
                  request.visionBackend.map({ ["cpu", "gpu"].contains($0) }) ?? true,
                  request.cancelAfterSeconds.map({ $0 > 0 }) ?? true else {
                throw SpikeError.message("Configurazione benchmark non valida.")
            }
            variant = model
            mode = request.backend == "cpu" ? .android : .metal
            guard models.contains(model) else { throw SpikeError.message("Modello benchmark da importare.") }
            selectedImage = try request.imageFile.map { try Self.prepareImage(Data(contentsOf: benchmarkFile($0))) }
            let prompt = try request.promptFile.map { try String(contentsOf: benchmarkFile($0), encoding: .utf8) }
            run(benchmark: request, promptOverride: prompt)
        } catch {
            phase = error.localizedDescription
            print("SPIKE_SETUP_ERROR: \(phase)")
        }
    }

    private func benchmarkFile(_ name: String) throws -> URL {
        guard !name.isEmpty, name == URL(fileURLWithPath: name).lastPathComponent else {
            throw SpikeError.message("Usa un nome file nella cartella benchmarks.")
        }
        return documents.appendingPathComponent("benchmarks", isDirectory: true).appendingPathComponent(name)
    }

    func refreshModels() {
        models = Set(ModelVariant.allCases.filter { variant in
            let file = modelsDirectory.appendingPathComponent(variant.fileName)
            return (try? ModelVariant.recognize(file)) == variant
        })
    }

    func importModel(_ source: URL) {
        busy = true
        phase = "Importazione modello…"
        let directory = modelsDirectory
        Task {
            do {
                let imported = try await Task.detached(priority: .userInitiated) {
                    let access = source.startAccessingSecurityScopedResource()
                    defer { if access { source.stopAccessingSecurityScopedResource() } }
                    let variant = try ModelVariant.recognize(source)
                    let manager = FileManager.default
                    try manager.createDirectory(at: directory, withIntermediateDirectories: true)
                    let target = directory.appendingPathComponent(variant.fileName)
                    if (try? ModelVariant.recognize(target)) == variant { return variant }
                    let free = try directory.resourceValues(forKeys: [.volumeAvailableCapacityForImportantUsageKey])
                        .volumeAvailableCapacityForImportantUsage ?? 0
                    guard free >= variant.bytes + 512 * 1024 * 1024 else {
                        throw SpikeError.message("Spazio insufficiente per copiare il modello.")
                    }
                    let temporary = directory.appendingPathComponent(UUID().uuidString + ".part")
                    defer { try? manager.removeItem(at: temporary) }
                    try manager.copyItem(at: source, to: temporary)
                    _ = try ModelVariant.recognize(temporary)
                    if manager.fileExists(atPath: target.path) {
                        _ = try manager.replaceItemAt(target, withItemAt: temporary)
                    } else {
                        try manager.moveItem(at: temporary, to: target)
                    }
                    return variant
                }.value
                variant = imported
                refreshModels()
                phase = "Modello pronto."
            } catch { phase = error.localizedDescription }
            busy = false
        }
    }

    func run(benchmark: BenchmarkRequest? = nil, promptOverride: String? = nil) {
        guard !busy, models.contains(variant) else { return }
        #if targetEnvironment(simulator)
        phase = "Esegui la prova su un iPhone fisico."
        return
        #else
        guard ProcessInfo.processInfo.physicalMemory >= 7 * 1024 * 1024 * 1024 else {
            phase = "Questo spike richiede un iPhone con almeno 8 GB di RAM."
            return
        }
        busy = true
        cancelRequested = false
        output = ""
        phase = "Caricamento…"
        let chosenVariant = variant
        let chosenMode = mode
        let vision: Backend = benchmark?.visionBackend == "cpu" ? .cpu() : .gpu
        let image = selectedImage ?? Self.fixtureImage()
        let source = benchmark?.imageFile ?? (selectedImage == nil ? "synthetic" : "photo-picker")
        let path = modelsDirectory.appendingPathComponent(chosenVariant.fileName)
        Task {
            let clock = ContinuousClock()
            let start = clock.now
            var report = RunReport()
            report.model = chosenVariant.rawValue
            report.backend = chosenMode.decoder.rawValue.uppercased() + " + visione " + vision.rawValue.uppercased()
            report.contextTokens = benchmark?.contextTokens
            report.visionBackend = vision.rawValue
            report.freshCache = benchmark?.freshCache ?? false
            report.imageSource = source
            report.imageBytes = image.count
            report.benchmarkID = benchmark?.id
            report.transcription = benchmark?.transcription ?? true
            report.maxOutputTokens = benchmark == nil ? 1536 : benchmark?.maxOutputTokens
            report.imageSHA256 = Self.digest(image)
            report.initialThermalState = String(ProcessInfo.processInfo.thermalState.rawValue)
            report.stage = "initialization"
            report.initialFootprintBytes = Self.footprint()
            report.initialAvailableMemoryBytes = UInt64(os_proc_available_memory())
            report.minimumAvailableMemoryBytes = report.initialAvailableMemoryBytes
            var peak = report.initialFootprintBytes
            let previousIdleTimer = UIApplication.shared.isIdleTimerDisabled
            UIApplication.shared.isIdleTimerDisabled = true
            // Sampling also runs during Engine.initialize(), on its own actor executor.
            let sampler = Task { @MainActor in
                var lastCheckpoint = clock.now
                while !Task.isCancelled {
                    peak = max(peak, Self.footprint())
                    report.minimumAvailableMemoryBytes = min(report.minimumAvailableMemoryBytes ?? .max, UInt64(os_proc_available_memory()))
                    if Self.seconds(lastCheckpoint.duration(to: clock.now)) >= 1 {
                        report.sampledPeakFootprintBytes = peak
                        report.totalSeconds = Self.seconds(start.duration(to: clock.now))
                        try? updateReport(report)
                        lastCheckpoint = clock.now
                    }
                    try? await Task.sleep(for: .milliseconds(100))
                }
            }
            defer {
                sampler.cancel()
                conversation = nil
                canCancel = false
                UIApplication.shared.isIdleTimerDisabled = previousIdleTimer
                busy = false
            }
            reports.append(report)
            do {
                // Keep the run identity even if iOS terminates the process during loading.
                try saveReports()
                ExperimentalFlags.optIntoExperimentalAPIs()
                ExperimentalFlags.enableBenchmark = true
                var cacheName = chosenVariant.rawValue + "-" + chosenMode.decoder.rawValue
                if vision == .cpu() { cacheName += "-vision-cpu" }
                if let tokens = benchmark?.contextTokens { cacheName += "-context-\(tokens)" }
                if benchmark?.freshCache == true { cacheName += "-" + UUID().uuidString }
                let cache = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
                    .appendingPathComponent(cacheName)
                try FileManager.default.createDirectory(at: cache, withIntermediateDirectories: true)
                let config = try EngineConfig(
                    modelPath: path.path, backend: chosenMode.decoder, visionBackend: vision,
                    maxNumTokens: benchmark?.contextTokens,
                    cacheDir: cache.path
                )
                let loaded = Engine(engineConfig: config)
                try await loaded.initialize()
                report.initializationSeconds = Self.seconds(start.duration(to: clock.now))
                let sampling = try SamplerConfig(topK: 1, topP: 1, temperature: 0.1, seed: 0)
                let session = try await loaded.createConversation(with: report.transcription == true
                    ? ConversationConfig(samplerConfig: sampling) : ConversationConfig())
                conversation = session
                canCancel = true
                phase = "Lettura della foto…"
                report.stage = "inference"
                let prompt = try promptOverride ?? Self.programPrompt()
                report.promptSHA256 = Self.digest(Data(prompt.utf8))
                try updateReport(report)
                let cancelTimer = Task { @MainActor in
                    guard let seconds = benchmark?.cancelAfterSeconds else { return }
                    do {
                        try await Task.sleep(for: .seconds(seconds))
                        cancel()
                    } catch { /* The run completed before the requested cancellation. */ }
                }
                defer { cancelTimer.cancel() }
                let stream = session.sendMessageStream(
                    Message(of: .imageData(image), .text(prompt)), maxOutputTokens: report.maxOutputTokens
                )
                for try await chunk in stream {
                    if report.firstChunkSeconds == nil {
                        report.firstChunkSeconds = Self.seconds(start.duration(to: clock.now))
                    }
                    report.output += chunk.toString
                    report.thinking += chunk.channels["thinking"] ?? ""
                    output = report.output
                }
                if let metrics = try? session.getBenchmarkInfo() {
                    report.decodeTokens = metrics.lastDecodeTokenCount
                    report.decodeTokensPerSecond = metrics.lastDecodeTokensPerSecond
                }
                report.outcome = cancelRequested ? "canceled" : "completed"
                phase = cancelRequested ? "Scansione interrotta." : "Prova completata."
            } catch {
                report.outcome = cancelRequested ? "canceled" : "failed"
                phase = cancelRequested ? "Scansione interrotta." : error.localizedDescription
                if !cancelRequested { report.output += "\n" + error.localizedDescription }
            }
            peak = max(peak, Self.footprint())
            report.sampledPeakFootprintBytes = peak
            report.totalSeconds = Self.seconds(start.duration(to: clock.now))
            report.thermalState = String(ProcessInfo.processInfo.thermalState.rawValue)
            report.stage = "finished"
            do { try updateReport(report) } catch { phase += "\nReport non salvato. " + error.localizedDescription }
            print("SPIKE_FINISHED: \(report.benchmarkID ?? report.id.uuidString) \(report.outcome)")
        }
        #endif
    }

    func cancel() {
        guard let conversation else { return }
        do {
            try conversation.cancel()
            cancelRequested = true
            canCancel = false
            phase = "Interruzione in corso…"
        } catch { phase = error.localizedDescription }
    }

    private func saveReports() throws {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        let url = documents.appendingPathComponent("spike-results.json")
        try encoder.encode(reports).write(to: url, options: .atomic)
        reportURL = url
    }

    private func updateReport(_ report: RunReport) throws {
        if let index = reports.firstIndex(where: { $0.id == report.id }) { reports[index] = report }
        try saveReports()
    }

    private static func digest(_ data: Data) -> String {
        SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined()
    }

    static func prepareImage(_ data: Data) throws -> Data {
        guard let image = UIImage(data: data) else { throw SpikeError.message("Foto non leggibile.") }
        let ratio = min(1, 2048 / max(image.size.width, image.size.height))
        let size = CGSize(width: image.size.width * ratio, height: image.size.height * ratio)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        return UIGraphicsImageRenderer(size: size, format: format).jpegData(withCompressionQuality: 0.95) {
            _ in image.draw(in: CGRect(origin: .zero, size: size))
        }
    }

    private static func programPrompt() throws -> String {
        guard let url = Bundle.main.url(forResource: "ProgramScan", withExtension: "kt") else {
            throw SpikeError.message("Prompt condiviso assente nel bundle.")
        }
        let source = try String(contentsOf: url, encoding: .utf8)
        let sections = source.components(separatedBy: "\"\"\"")
        guard sections.count >= 3 else { throw SpikeError.message("Formato del prompt cambiato.") }
        return sections[1].trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func seconds(_ duration: Duration) -> Double {
        Double(duration.components.seconds) + Double(duration.components.attoseconds) / 1e18
    }

    private static func footprint() -> UInt64 {
        var info = task_vm_info_data_t()
        var count = mach_msg_type_number_t(MemoryLayout<task_vm_info_data_t>.size / MemoryLayout<integer_t>.size)
        let result = withUnsafeMutablePointer(to: &info) { pointer in
            pointer.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
                task_info(mach_task_self_, task_flavor_t(TASK_VM_INFO), $0, &count)
            }
        }
        return result == KERN_SUCCESS ? info.phys_footprint : 0
    }

    private static func fixtureImage() -> Data {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: 1280, height: 1000), format: format)
        return renderer.jpegData(withCompressionQuality: 0.95) { context in
            UIColor.white.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 1280, height: 1000))
            let text = "Squat con Bilanciere (115kg)\n\nW1: 70% 4x5\n\nW2: 80% 3x4"
            text.draw(in: CGRect(x: 70, y: 110, width: 1140, height: 800), withAttributes: [
                .font: UIFont.monospacedSystemFont(ofSize: 50, weight: .medium),
                .foregroundColor: UIColor.black
            ])
        }
    }
}

private struct GemmaSpikeView: View {
    @StateObject private var state = SpikeState()
    @State private var importing = false
    @State private var photo: PhotosPickerItem?

    var body: some View {
        NavigationStack {
            Form {
                Section("Modello") {
                    Button("Importa .litertlm da File") { importing = true }
                    Picker("Variante", selection: $state.variant) {
                        ForEach(ModelVariant.allCases) { Text($0.rawValue).tag($0) }
                    }
                    Text(state.models.contains(state.variant) ? "File disponibile" : "File da importare")
                    Picker("Calcolo", selection: $state.mode) {
                        ForEach(ComputeMode.allCases) { Text($0.rawValue).tag($0) }
                    }
                }.disabled(state.busy)
                Section("Scansione") {
                    PhotosPicker(selection: $photo, matching: .images) { Text("Scegli una foto") }
                        .disabled(state.busy)
                    Text(state.selectedImage == nil ? "Pagina sintetica con squat e due settimane" : "Foto selezionata")
                    if state.selectedImage != nil {
                        Button("Usa pagina sintetica") { state.selectedImage = nil; photo = nil }
                            .disabled(state.busy)
                    }
                    Button("Avvia prova") { state.run() }
                        .disabled(state.busy || !state.models.contains(state.variant))
                    Button("Interrompi") { state.cancel() }.disabled(!state.canCancel)
                    Text(state.phase)
                }
                Section("Risultato") {
                    Text(state.output).font(.system(.body, design: .monospaced)).textSelection(.enabled)
                    if let url = state.reportURL { ShareLink("Esporta misure JSON", item: url) }
                }
            }
            .navigationTitle("Gemma · Spike A5")
            .fileImporter(isPresented: $importing, allowedContentTypes: [.data]) { result in
                switch result {
                case .success(let url): state.importModel(url)
                case .failure(let error): state.phase = error.localizedDescription
                }
            }
            .onChange(of: photo) { _, item in
                Task {
                    guard let item else { return }
                    do {
                        let data = try await item.loadTransferable(type: Data.self)
                        guard let data else {
                            throw SpikeError.message("Foto non leggibile.")
                        }
                        guard photo == item else { return }
                        state.selectedImage = try SpikeState.prepareImage(data)
                    } catch { state.phase = error.localizedDescription }
                }
            }
        }
    }
}
