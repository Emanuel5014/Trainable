import ActivityKit
import AppIntents

/// Compiled into both targets. LiveActivityIntent is executed in the parent app process, not the widget sandbox.
struct TimerControlIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Timer"
    static var isDiscoverable: Bool = false
    static var openAppWhenRun: Bool = false

    @Parameter(title: "Activity") var activityID: String
    @Parameter(title: "Timer") var instanceID: String
    @Parameter(title: "Action") var action: String

    init() {}
    init(activityID: String, instanceID: String, action: String) {
        self.activityID = activityID
        self.instanceID = instanceID
        self.action = action
    }
    func perform() async throws -> some IntentResult {
        #if TRAINABLE_APP
        await TimerLiveActivityCoordinator.shared.perform(activityID: activityID, instanceID: instanceID, action: action)
        #endif
        return .result()
    }
}
