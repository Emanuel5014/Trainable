import Foundation

/// The App Group file the app writes (HomeWidgetPublisher) and the widgets read (HomeWidgets).
/// Compiled into both the app and the widget extension.
enum HomeWidgetStore {
    static let declaredGroup = "group.com.emanuel5014.trainable"
    private static let baseBundleID = "com.emanuel5014.trainable"
    private static let fileName = "home-widgets.json"

    /// AltStore re-signs the app under the user's own team and renames the group to
    /// `group.com.emanuel5014.trainable.<TEAMID>`, so the group is looked up at run time: the groups AltStore lists in
    /// Info.plist, then the declared name with the team taken from the app's own (renamed) bundle id, then the
    /// declared name. The first one this process can open wins.
    static let groupIdentifier: String? = {
        var candidates: [String] = []
        for bundle in [Bundle.main, hostApp].compactMap({ $0 }) {
            if let groups = bundle.object(forInfoDictionaryKey: "ALTAppGroups") as? [String] {
                candidates += groups.filter { $0.hasPrefix(declaredGroup) }
            }
        }
        if let id = (hostApp ?? Bundle.main).bundleIdentifier, id.hasPrefix(baseBundleID + ".") {
            let team = id.dropFirst(baseBundleID.count + 1)
            if !team.isEmpty, !team.contains(".") { candidates.append(declaredGroup + "." + team) }
        }
        candidates.append(declaredGroup)
        return candidates.first { FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: $0) != nil }
    }()

    /// The app around this extension (…/Trainable.app/PlugIns/X.appex), or nil when this is the app itself.
    private static var hostApp: Bundle? {
        let url = Bundle.main.bundleURL
        guard url.pathExtension == "appex" else { return nil }
        return Bundle(url: url.deletingLastPathComponent().deletingLastPathComponent())
    }

    private static var fileURL: URL? {
        guard let group = groupIdentifier else { return nil }
        return FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group)?
            .appendingPathComponent(fileName)
    }

    /// Writes the JSON when it changed; true when the widgets need a redraw.
    @discardableResult
    static func write(_ json: String) -> Bool {
        guard let url = fileURL, let data = json.data(using: .utf8) else { return false }
        if let old = try? Data(contentsOf: url), old == data { return false }
        do {
            try data.write(to: url, options: .atomic)
            return true
        } catch {
            return false
        }
    }

    static func read() -> HomeWidgetSnapshot? {
        guard let url = fileURL, let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(HomeWidgetSnapshot.self, from: data)
    }
}

/// The same fields as HomeWidgetSnapshot.kt.
struct HomeWidgetSnapshot: Codable {
    let version: Int
    let accent: Accent
    let days: [Day]

    struct Accent: Codable {
        let lightStart: Int64
        let lightEnd: Int64
        let darkStart: Int64
        let darkEnd: Int64
    }

    struct Day: Codable {
        let startsAt: Int64
        let routine: Routine
        let week: Week
    }

    struct Routine: Codable {
        let kind: String
        let label: String
        let title: String
        let subtitle: String?
        let button: String
        let link: String
        let accessibility: String
    }

    struct Week: Codable {
        let done: Int
        let goal: Int
        let met: Bool
        let label: String
        let title: String
        let cardio: String?
        let unit: String
        let days: [WeekDay]
    }

    struct WeekDay: Codable {
        let letter: String
        let mark: String
    }
}
