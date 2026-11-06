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
            let kind = context.attributes.kind
            let accent = TimerPalette.accent(state.accentARGB)
            let done = state.isFinished(kind: kind, isStale: context.isStale)
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    TimerCaption(kind: context.attributes.kind, title: state.title, accent: accent)
                        .padding(.leading, 6).padding(.top, 4)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Group {
                        if done {
                            Image(systemName: "checkmark.circle.fill")
                        } else {
                            TimerCountdown(interval: state.clock(kind: context.attributes.kind, isStale: context.isStale),
                                           overtimeFrom: state.overtimeFrom(kind: context.attributes.kind))
                        }
                    }
                    .font(.system(.title2, design: .rounded, weight: .semibold))
                    .foregroundStyle(accent)
                    .frame(width: 84, alignment: .trailing)
                    .padding(.trailing, 6)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    if done {
                        FinishedText(state: state, compact: true, accent: accent)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 6)
                    } else {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .center, spacing: 12) {
                            NextSetText(state: state, compact: true)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            TimerRing(interval: state.clock(kind: context.attributes.kind, isStale: context.isStale), accent: accent,
                                      overtimeFrom: state.overtimeFrom(kind: context.attributes.kind))
                                .frame(width: 28, height: 28)
                        }
                        ControlRow(kind: context.attributes.kind, activityID: context.activityID, state: state,
                                   accent: accent, height: 30, isStale: context.isStale)
                    }
                    .padding(.horizontal, 6)
                    }
                }
            } compactLeading: {
                Group {
                    if done { DoneBadge(accent: accent, lineWidth: 2.5) }
                    else { TimerRing(interval: state.clock(kind: kind, isStale: context.isStale), accent: accent,
                                     overtimeFrom: state.overtimeFrom(kind: kind)) }
                }
                .frame(width: 20, height: 20)
                .padding(.leading, 2)
            } compactTrailing: {
                Group {
                    if done {
                        // Rest is over: time to lift.
                        Image(systemName: kind == emomKind ? "flag.checkered" : "figure.strengthtraining.traditional")
                    } else {
                        TimerCountdown(interval: state.clock(kind: kind, isStale: context.isStale),
                                       overtimeFrom: state.overtimeFrom(kind: kind))
                    }
                }
                .font(.system(.footnote, design: .rounded, weight: .semibold))
                .foregroundStyle(accent)
                .frame(width: 44, alignment: .trailing)
            } minimal: {
                Group {
                    if done { DoneBadge(accent: accent, lineWidth: 2.5) }
                    else { TimerRing(interval: state.clock(kind: kind, isStale: context.isStale), accent: accent,
                                     overtimeFrom: state.overtimeFrom(kind: kind)) }
                }
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
        let clock = state.clock(kind: kind, isStale: isStale)
        if state.isFinished(kind: kind, isStale: isStale) {
            // The time ran out while the app could not end the card: say so instead of a frozen 0:00.
            HStack(alignment: .center, spacing: 16) {
                DoneBadge(accent: accent, lineWidth: 7).frame(width: 82, height: 82)
                VStack(alignment: .leading, spacing: 3) {
                    TimerCaption(kind: kind, title: state.title, accent: accent)
                    FinishedText(state: state, compact: false, accent: accent)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 16).padding(.vertical, 14)
            .foregroundStyle(.white)
        } else {
        HStack(alignment: .center, spacing: 16) {
            ZStack {
                TimerRing(interval: clock, accent: accent, overtimeFrom: state.overtimeFrom(kind: kind))
                VStack(spacing: 1) {
                    TimerCountdown(interval: clock, overtimeFrom: state.overtimeFrom(kind: kind))
                        .font(.system(size: 21, weight: .semibold, design: .rounded))
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                        .minimumScaleFactor(0.6)
                        .frame(width: 58)
                    if state.showsRunEnd(kind: kind, isStale: isStale) {
                        // The minute passed while the app slept: the ring now counts down to the end of the run.
                        Image(systemName: "flag.checkered").font(.caption2).foregroundStyle(accent)
                    }
                }
            }
            .frame(width: 82, height: 82)

            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 6) {
                    TimerCaption(kind: kind, title: state.title, accent: accent)
                    Spacer(minLength: 4)
                    if kind == emomKind, !state.paused, !state.showsRunEnd(kind: kind, isStale: isStale) {
                        Label {
                            TimerCountdown(interval: state.startedAt...state.endsAt)
                        } icon: { Image(systemName: "flag.checkered") }
                        .font(.caption.weight(.semibold)).foregroundStyle(.white.opacity(0.7))
                        .labelStyle(.titleAndIcon).frame(maxWidth: 76, alignment: .trailing)
                    }
                }
                NextSetText(state: state, compact: false).padding(.top, 3)
                Spacer(minLength: 8)
                ControlRow(kind: kind, activityID: activityID, state: state, accent: accent, height: 32, isStale: isStale)
            }
            .frame(maxWidth: .infinity, minHeight: 82, alignment: .leading)
        }
        .padding(.horizontal, 16).padding(.vertical, 14)
        .foregroundStyle(.white)
        }
    }
}

// MARK: - Pieces

/// A full ring with a check: the countdown is over.
private struct DoneBadge: View {
    let accent: Color
    let lineWidth: CGFloat

    var body: some View {
        ZStack {
            Circle().stroke(accent, lineWidth: lineWidth).padding(lineWidth / 2)
            Image(systemName: "checkmark").font(.system(size: lineWidth * 4, weight: .bold)).foregroundStyle(accent)
                .minimumScaleFactor(0.3)
        }
    }
}

/// "REST TERMINATED" (in the app language) and, for a rest, what comes next.
private struct FinishedText: View {
    let state: TrainableTimerAttributes.ContentState
    let compact: Bool
    let accent: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(state.finishedText.isEmpty ? state.title : state.finishedText)
                .font(compact ? .subheadline.weight(.bold) : .headline.weight(.bold))
                .lineLimit(1).minimumScaleFactor(0.7)
            if !state.exercise.isEmpty {
                Text(state.setLine.isEmpty ? state.exercise : "\(state.exercise) · \(state.setLine)")
                    .font(compact ? .caption : .subheadline)
                    .foregroundStyle(.white.opacity(0.75))
                    .lineLimit(2)
            } else if !state.detail.isEmpty {
                Text(state.detail).font(compact ? .caption : .subheadline)
                    .foregroundStyle(.white.opacity(0.75)).lineLimit(2)
            }
        }
    }
}

/// System-driven ring: it empties as the time runs out, with no per-second app updates.
/// Only the built-in circular style is animated by the system for timer progress in Live Activities.
/// A nil interval is a paused EMOM run: a still ring with a pause glyph.
/// With [overtimeFrom] (rest/warmup) an inner ring fills once the time is up, with no app update needed: iOS
/// redraws an expired card only later (about 100 s on the simulator), so the card must not freeze meanwhile.
private struct TimerRing: View {
    let interval: ClosedRange<Date>?
    let accent: Color
    var overtimeFrom: Date? = nil

    var body: some View {
        if let interval {
            ZStack {
                ProgressView(timerInterval: interval, countsDown: true) { EmptyView() } currentValueLabel: { EmptyView() }
                    .progressViewStyle(.circular)
                    .tint(accent)
                if let from = overtimeFrom {
                    ProgressView(timerInterval: from...from.addingTimeInterval(60), countsDown: false) { EmptyView() } currentValueLabel: { EmptyView() }
                        .progressViewStyle(.circular)
                        .tint(TimerPalette.go)
                        .scaleEffect(0.8)
                }
            }
        } else {
            ProgressView(value: 0.0) { EmptyView() } currentValueLabel: { EmptyView() }
                .progressViewStyle(.circular)
                .tint(accent)
        }
    }
}

/// Counts down; with [overtimeFrom] it keeps going after zero, counting the time since the end, instead of 0:00.
private struct TimerCountdown: View {
    let interval: ClosedRange<Date>?
    var overtimeFrom: Date? = nil

    var body: some View {
        if let end = overtimeFrom {
            Text(end, style: .timer)
                .monospacedDigit()
        } else if let interval {
            Text(timerInterval: interval, countsDown: true, showsHours: false)
                .monospacedDigit()
                .contentTransition(.numericText(countsDown: true))
        } else {
            Image(systemName: "pause.fill")
        }
    }
}

private let emomKind = "workout.emom"

extension TrainableTimerAttributes.ContentState {
    /// What the ring and the countdown follow: the timer, the EMOM minute, or (paused) nothing.
    /// iOS runs no app code while suspended, so once an EMOM minute ends unseen (the content turns stale at
    /// that minute) they follow the end of the whole run, which stays right until the app catches up.
    func clock(kind: String, isStale: Bool) -> ClosedRange<Date>? {
        guard kind == emomKind else { return startedAt...endsAt }
        if paused { return nil }
        if let start = roundStartsAt, let end = roundEndsAt, start < end, !isStale, end > Date() { return start...end }
        return startedAt...endsAt
    }

    /// The countdown is over (a rest/warmup past its end, or the whole EMOM run). Evaluated whenever the system
    /// renders the card: at its stale date, when the end alert arrives, or when the screen wakes.
    func isFinished(kind: String, isStale: Bool) -> Bool {
        if paused { return false }
        if kind == emomKind { return endsAt <= Date() }
        return isStale || endsAt <= Date()
    }

    /// Rest and warmup keep counting past their end (see TimerRing); EMOM has its own fallback.
    func overtimeFrom(kind: String) -> Date? { kind == emomKind ? nil : endsAt }

    func showsRunEnd(kind: String, isStale: Bool) -> Bool {
        kind == emomKind && !paused && clock(kind: kind, isStale: isStale) == startedAt...endsAt
    }
}

private struct TimerCaption: View {
    let kind: String
    let title: String
    let accent: Color

    var body: some View {
        Label {
            Text(title).lineLimit(1).minimumScaleFactor(0.75)
        } icon: {
            Image(systemName: kind == "workout.warmup" ? "flame.fill" : kind == emomKind ? "repeat" : "hourglass")
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
                    .lineLimit(compact ? 1 : 2)
                    .minimumScaleFactor(0.85)
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

/// Rest/warmup buttons (disabled once stale) or the EMOM buttons (kept active: a tap catches the run up).
private struct ControlRow: View {
    let kind: String
    let activityID: String
    let state: TrainableTimerAttributes.ContentState
    let accent: Color
    let height: CGFloat
    let isStale: Bool

    var body: some View {
        if kind == emomKind {
            EmomButtons(activityID: activityID, state: state, accent: accent, height: height)
        } else {
            TimerControlButtons(activityID: activityID, state: state, accent: accent, height: height)
                .disabled(isStale)
        }
    }
}

private struct EmomButtons: View {
    let activityID: String
    let state: TrainableTimerAttributes.ContentState
    let accent: Color
    let height: CGFloat

    var body: some View {
        let pairs = Array(zip(state.actionKeys, state.actionLabels))
        if !pairs.isEmpty {
            HStack(spacing: 8) {
                ForEach(pairs, id: \.0) { key, label in
                    let primary = key == "emom.done" || key == "emom.resume"
                    Button(intent: TimerControlIntent(activityID: activityID, instanceID: state.instanceID, action: key)) {
                        Label { Text(label) } icon: { Image(systemName: Self.symbol(key)) }
                            .labelStyle(.titleAndIcon)
                            .lineLimit(1).minimumScaleFactor(0.7)
                            .foregroundStyle(primary ? TimerPalette.onAccent(accent: state.accentARGB) : .white)
                            .frame(maxWidth: .infinity, minHeight: height)
                            .background(primary ? accent : .white.opacity(0.14), in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            .font(.subheadline.weight(.semibold))
        }
    }

    private static func symbol(_ key: String) -> String {
        switch key {
        case "emom.done": return "checkmark"
        case "emom.pause": return "pause.fill"
        case "emom.resume": return "play.fill"
        default: return "stop.fill"
        }
    }
}

// MARK: - Colours

/// The card is always dark. The accent comes from the app theme (its dark-scheme primary) when known.
enum TimerPalette {
    static let background = Color(red: 0x14 / 255, green: 0x13 / 255, blue: 0x17 / 255)
    /// "Time to lift": the inner ring that fills once the rest is over.
    static let go = Color(red: 0x5D / 255, green: 0xD3 / 255, blue: 0x8A / 255)
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
