import ActivityKit
import AppIntents
import SwiftUI
import WidgetKit

@main
struct TrainableTimerWidgets: WidgetBundle {
    var body: some Widget { TrainableTimerWidget() }
}

struct TrainableTimerWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: TrainableTimerAttributes.self) { context in
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 10) {
                    Image(systemName: symbol(context.attributes.kind)).accessibilityHidden(true)
                    Text(context.state.title).font(.headline).lineLimit(1)
                    Spacer(minLength: 4)
                    countdown(context.state).font(.system(.title2, design: .rounded, weight: .semibold))
                        .minimumScaleFactor(0.7).frame(width: 76, alignment: .trailing)
                }
                if !context.state.detail.isEmpty {
                    Text(context.state.detail).font(.caption).lineLimit(3)
                }
                progress(context.state)
                TimerControlButtons(activityID: context.activityID, state: context.state).disabled(context.isStale)
            }
            .padding(14)
            .foregroundStyle(.white)
            .activityBackgroundTint(.black.opacity(0.9))
            .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Image(systemName: symbol(context.attributes.kind)).font(.title2)
                }
                DynamicIslandExpandedRegion(.center) {
                    Text(context.state.title).font(.headline).lineLimit(1)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    countdown(context.state).font(.title2.weight(.semibold)).frame(width: 76, alignment: .trailing)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(alignment: .leading, spacing: 8) {
                        if !context.state.detail.isEmpty {
                            Text(context.state.detail).font(.caption).lineLimit(3)
                        }
                        progress(context.state)
                        TimerControlButtons(activityID: context.activityID, state: context.state).disabled(context.isStale)
                    }.padding(.top, 4)
                }
            } compactLeading: {
                Image(systemName: symbol(context.attributes.kind))
            } compactTrailing: {
                countdown(context.state).font(.caption).frame(width: 50)
            } minimal: {
                countdown(context.state).font(.caption2).minimumScaleFactor(0.65)
            }
            .keylineTint(.white)
        }
    }

    private func symbol(_ kind: String) -> String {
        kind == "workout.warmup" ? "timer" : "hourglass"
    }
    private func countdown(_ state: TrainableTimerAttributes.ContentState) -> some View {
        Text(timerInterval: state.startedAt...state.endsAt, countsDown: true, showsHours: false)
            .monospacedDigit().contentTransition(.numericText(countsDown: true))
    }
    private func progress(_ state: TrainableTimerAttributes.ContentState) -> some View {
        ProgressView(timerInterval: state.startedAt...state.endsAt, countsDown: false)
            .labelsHidden().tint(.white)
    }
}

private struct TimerControlButtons: View {
    let activityID: String
    let state: TrainableTimerAttributes.ContentState

    var body: some View {
        if state.showSubtract || state.showAdd || state.showSkip {
            HStack(spacing: 8) {
                if state.showSubtract { control("subtract", label: "−\(state.subtractSeconds)s") }
                if state.showAdd { control("add", label: "+\(state.addSeconds)s") }
                if state.showSkip { control("skip", label: state.skipLabel) }
            }
            .font(.subheadline.weight(.semibold))
        }
    }
    private func control(_ action: String, label: String) -> some View {
        Button(intent: TimerControlIntent(activityID: activityID, instanceID: state.instanceID, action: action)) {
            Text(label).lineLimit(1).minimumScaleFactor(0.8)
                .frame(maxWidth: .infinity, minHeight: 36)
                .background(.white.opacity(0.16), in: Capsule())
        }
        .buttonStyle(.plain)
    }
}
