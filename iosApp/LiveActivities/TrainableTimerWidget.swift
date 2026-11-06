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
            LockScreenTimerView(kind: context.attributes.kind, activityID: context.activityID,
                                state: context.state, isStale: context.isStale)
                .activityBackgroundTint(TimerPalette.background)
                .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            let state = context.state
            let accent = TimerPalette.accent(state.accentARGB)
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    TimerCaption(kind: context.attributes.kind, title: state.title, accent: accent)
                        .padding(.leading, 6).padding(.top, 4)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    TimerCountdown(state: state)
                        .font(.system(.title2, design: .rounded, weight: .semibold))
                        .foregroundStyle(accent)
                        .frame(width: 84, alignment: .trailing)
                        .padding(.trailing, 6)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .center, spacing: 12) {
                            NextSetText(state: state, compact: true)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            TimerRing(state: state, accent: accent, lineWidth: 4)
                                .frame(width: 28, height: 28)
                        }
                        TimerControlButtons(activityID: context.activityID, state: state, accent: accent, height: 30)
                            .disabled(context.isStale)
                    }
                    .padding(.horizontal, 6)
                }
            } compactLeading: {
                TimerRing(state: state, accent: accent, lineWidth: 3)
                    .frame(width: 20, height: 20)
                    .padding(.leading, 2)
            } compactTrailing: {
                TimerCountdown(state: state)
                    .font(.system(.footnote, design: .rounded, weight: .semibold))
                    .foregroundStyle(accent)
                    .frame(width: 44, alignment: .trailing)
            } minimal: {
                TimerRing(state: state, accent: accent, lineWidth: 3)
                    .frame(width: 20, height: 20)
            }
            .keylineTint(accent)
        }
    }
}

// MARK: - Lock Screen

private struct LockScreenTimerView: View {
    let kind: String
    let activityID: String
    let state: TrainableTimerAttributes.ContentState
    let isStale: Bool

    var body: some View {
        let accent = TimerPalette.accent(state.accentARGB)
        HStack(alignment: .center, spacing: 16) {
            ZStack {
                TimerRing(state: state, accent: accent, lineWidth: 7)
                TimerCountdown(state: state)
                    .font(.system(size: 21, weight: .semibold, design: .rounded))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.6)
                    .frame(width: 58)
            }
            .frame(width: 82, height: 82)

            VStack(alignment: .leading, spacing: 0) {
                TimerCaption(kind: kind, title: state.title, accent: accent)
                NextSetText(state: state, compact: false).padding(.top, 3)
                Spacer(minLength: 8)
                TimerControlButtons(activityID: activityID, state: state, accent: accent, height: 32)
                    .disabled(isStale)
            }
            .frame(maxWidth: .infinity, minHeight: 82, alignment: .leading)
        }
        .padding(.horizontal, 16).padding(.vertical, 14)
        .foregroundStyle(.white)
    }
}

// MARK: - Pieces

/// System-driven ring: it empties as the time runs out, with no per-second app updates.
/// Only the built-in circular style is animated by the system for timer progress in Live Activities.
private struct TimerRing: View {
    let state: TrainableTimerAttributes.ContentState
    let accent: Color
    let lineWidth: CGFloat

    var body: some View {
        ProgressView(timerInterval: state.startedAt...state.endsAt, countsDown: true) { EmptyView() } currentValueLabel: { EmptyView() }
            .progressViewStyle(.circular)
            .tint(accent)
    }
}

private struct TimerCountdown: View {
    let state: TrainableTimerAttributes.ContentState

    var body: some View {
        Text(timerInterval: state.startedAt...state.endsAt, countsDown: true, showsHours: false)
            .monospacedDigit()
            .contentTransition(.numericText(countsDown: true))
    }
}

private struct TimerCaption: View {
    let kind: String
    let title: String
    let accent: Color

    var body: some View {
        Label {
            Text(title).lineLimit(1)
        } icon: {
            Image(systemName: kind == "workout.warmup" ? "flame.fill" : "hourglass")
        }
        .font(.caption.weight(.semibold))
        .foregroundStyle(accent)
    }
}

private struct NextSetText: View {
    let state: TrainableTimerAttributes.ContentState
    let compact: Bool

    var body: some View {
        if !state.exercise.isEmpty {
            VStack(alignment: .leading, spacing: 1) {
                Text(state.exercise)
                    .font(compact ? .subheadline.weight(.semibold) : .headline)
                    .lineLimit(1)
                if !state.setLine.isEmpty {
                    Text(state.setLine)
                        .font(compact ? .caption : .subheadline)
                        .foregroundStyle(.white.opacity(0.7))
                        .lineLimit(2)
                }
            }
        } else if !state.detail.isEmpty {
            Text(state.detail)
                .font(compact ? .caption : .subheadline)
                .foregroundStyle(.white.opacity(0.8))
                .lineLimit(compact ? 2 : 3)
        }
    }
}

private struct TimerControlButtons: View {
    let activityID: String
    let state: TrainableTimerAttributes.ContentState
    let accent: Color
    let height: CGFloat

    var body: some View {
        if state.showSubtract || state.showAdd || state.showSkip {
            HStack(spacing: 8) {
                if state.showSubtract {
                    control("subtract", fill: .white.opacity(0.14), text: .white) { Text("−\(state.subtractSeconds)s") }
                }
                if state.showAdd {
                    control("add", fill: .white.opacity(0.14), text: .white) { Text("+\(state.addSeconds)s") }
                }
                if state.showSkip {
                    control("skip", fill: accent, text: TimerPalette.onAccent(accent: state.accentARGB)) {
                        Image(systemName: "forward.end.fill")
                    }
                    .accessibilityLabel(Text(state.skipLabel))
                }
            }
            .font(.subheadline.weight(.semibold))
        }
    }

    private func control<Content: View>(_ action: String, fill: Color, text: Color,
                                        @ViewBuilder content: () -> Content) -> some View {
        Button(intent: TimerControlIntent(activityID: activityID, instanceID: state.instanceID, action: action)) {
            content()
                .lineLimit(1).minimumScaleFactor(0.8)
                .monospacedDigit()
                .foregroundStyle(text)
                .frame(maxWidth: .infinity, minHeight: height)
                .background(fill, in: Capsule())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Colours

/// The card is always dark. The accent comes from the app theme (its dark-scheme primary) when known.
enum TimerPalette {
    static let background = Color(red: 0x14 / 255, green: 0x13 / 255, blue: 0x17 / 255)
    /// Trainable's default blue, lifted for a dark surface.
    private static let fallback: Int64 = 0xFF8EA4F5

    static func accent(_ argb: Int64) -> Color {
        let (r, g, b) = components(argb == 0 ? fallback : argb)
        // Keep the ring and labels readable on the dark card even with a very dark theme colour.
        let lift = max(0, 0.45 - luminance(r, g, b)) / 0.45 * 0.6
        return Color(red: r + (1 - r) * lift, green: g + (1 - g) * lift, blue: b + (1 - b) * lift)
    }

    static func onAccent(accent argb: Int64) -> Color {
        let (r, g, b) = components(argb == 0 ? fallback : argb)
        return luminance(r, g, b) > 0.5 ? Color(red: 0.07, green: 0.07, blue: 0.09) : .white
    }

    private static func components(_ argb: Int64) -> (Double, Double, Double) {
        (Double((argb >> 16) & 0xFF) / 255, Double((argb >> 8) & 0xFF) / 255, Double(argb & 0xFF) / 255)
    }

    private static func luminance(_ r: Double, _ g: Double, _ b: Double) -> Double {
        0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
