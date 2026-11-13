import ActivityKit
import Foundation

/// Included in both targets; the widget extension does not link the Kotlin framework or open the database.
struct TrainableTimerAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var instanceID: String
        var title: String
        var detail: String
        var startedAt: Date
        var endsAt: Date
        var sessionID: Int
        var sessionStartedAtMillis: Int64
        var addSeconds: Int
        var subtractSeconds: Int
        var showAdd: Bool
        var showSubtract: Bool
        var showSkip: Bool
        var skipLabel: String
        var finishedText: String
        var exercise: String = ""
        var setLine: String = ""
        var accentARGB: Int64 = 0
        /** EMOM only: the minute on the clock (nil while paused), pause flag and buttons. */
        var roundStartsAt: Date? = nil
        var roundEndsAt: Date? = nil
        var paused: Bool = false
        var actionKeys: [String] = []
        var actionLabels: [String] = []
        /** Rest only, once it is over: "rest.more" / "rest.next" buttons, or the hint to open the app. */
        var finishedActionKeys: [String] = []
        var finishedActionLabels: [String] = []
        var openAppText: String = ""
        var needsApp: Bool = false
        /** Rest only: the set "Set done" would log, as this card shows it. */
        var nextSetToken: String = ""

        enum CodingKeys: String, CodingKey {
            case instanceID, title, detail, startedAt, endsAt, sessionID, sessionStartedAtMillis
            case addSeconds, subtractSeconds, showAdd, showSubtract, showSkip, skipLabel, finishedText
            case exercise, setLine, accentARGB
            case roundStartsAt, roundEndsAt, paused, actionKeys, actionLabels
            case finishedActionKeys, finishedActionLabels, openAppText, needsApp, nextSetToken
        }
    }
    var kind: String
}

// Older display-only activities can still be dismissed after an app update; their controls stay hidden.
extension TrainableTimerAttributes.ContentState {
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        instanceID = try c.decode(String.self, forKey: .instanceID)
        title = try c.decode(String.self, forKey: .title)
        detail = try c.decode(String.self, forKey: .detail)
        startedAt = try c.decode(Date.self, forKey: .startedAt)
        endsAt = try c.decode(Date.self, forKey: .endsAt)
        sessionID = try c.decodeIfPresent(Int.self, forKey: .sessionID) ?? -1
        sessionStartedAtMillis = try c.decodeIfPresent(Int64.self, forKey: .sessionStartedAtMillis) ?? 0
        addSeconds = try c.decodeIfPresent(Int.self, forKey: .addSeconds) ?? 30
        subtractSeconds = try c.decodeIfPresent(Int.self, forKey: .subtractSeconds) ?? 10
        showAdd = try c.decodeIfPresent(Bool.self, forKey: .showAdd) ?? false
        showSubtract = try c.decodeIfPresent(Bool.self, forKey: .showSubtract) ?? false
        showSkip = try c.decodeIfPresent(Bool.self, forKey: .showSkip) ?? false
        skipLabel = try c.decodeIfPresent(String.self, forKey: .skipLabel) ?? ""
        finishedText = try c.decodeIfPresent(String.self, forKey: .finishedText) ?? ""
        exercise = try c.decodeIfPresent(String.self, forKey: .exercise) ?? ""
        setLine = try c.decodeIfPresent(String.self, forKey: .setLine) ?? ""
        accentARGB = try c.decodeIfPresent(Int64.self, forKey: .accentARGB) ?? 0
        roundStartsAt = try c.decodeIfPresent(Date.self, forKey: .roundStartsAt)
        roundEndsAt = try c.decodeIfPresent(Date.self, forKey: .roundEndsAt)
        paused = try c.decodeIfPresent(Bool.self, forKey: .paused) ?? false
        actionKeys = try c.decodeIfPresent([String].self, forKey: .actionKeys) ?? []
        actionLabels = try c.decodeIfPresent([String].self, forKey: .actionLabels) ?? []
        finishedActionKeys = try c.decodeIfPresent([String].self, forKey: .finishedActionKeys) ?? []
        finishedActionLabels = try c.decodeIfPresent([String].self, forKey: .finishedActionLabels) ?? []
        openAppText = try c.decodeIfPresent(String.self, forKey: .openAppText) ?? ""
        needsApp = try c.decodeIfPresent(Bool.self, forKey: .needsApp) ?? false
        nextSetToken = try c.decodeIfPresent(String.self, forKey: .nextSetToken) ?? ""
    }
}
