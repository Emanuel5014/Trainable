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
            let clock = state.clock(kind: kind, isStale: context.isStale)
            return DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    // The leading region is narrow: the title shrinks instead of being cut ("Timer Recupe…").
                    HStack(spacing: 6) {
                        AppMark().frame(width: 20, height: 20)
                        Text(state.title)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.white.opacity(0.65))
                            .lineLimit(1).minimumScaleFactor(0.6)
                    }
                    .padding(.leading, 6).padding(.top, 4)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Group {
                        if done { DoneBadge(accent: accent, lineWidth: 3) }
                        else { TimerRing(interval: clock, accent: accent) }
                    }
                    .frame(width: 26, height: 26)
                    .padding(.trailing, 6).padding(.top, 2)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    if done {
                        VStack(alignment: .leading, spacing: 8) {
                            FinishedText(state: state, compact: true, accent: accent)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            if state.hasFinishedRow(kind: kind) {
                                FinishedActionRow(activityID: context.activityID, state: state, accent: accent,
                                                  height: 30, plusWidth: 72)
                            }
                        }
                        .padding(.horizontal, 6)
                    } else {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack(alignment: .center, spacing: 12) {
                                // A system timer text reserves extra width; a fixed box keeps the set text close.
                                BigCountdown(interval: clock, size: 36)
                                    .frame(width: 92, alignment: .leading)
                                NextSetText(state: state, compact: true)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                            ControlRow(kind: kind, activityID: context.activityID, state: state,
                                       accent: accent, height: 30, isStale: context.isStale)
                        }
                        .padding(.horizontal, 6)
                    }
                }
            } compactLeading: {
                AppMark()
                    .frame(width: 20, height: 20)
                    .padding(.leading, 2)
            } compactTrailing: {
                Group {
                    if done {
                        // Rest is over: time to lift.
                        Image(systemName: kind == emomKind ? "flag.checkered" : "figure.strengthtraining.traditional")
                    } else {
                        HStack(spacing: 5) {
                            TimerCountdown(interval: clock)
                                .frame(width: 38, alignment: .trailing)
                            TimerRing(interval: clock, accent: accent)
                                .frame(width: 15, height: 15)
                        }
                    }
                }
                .font(.system(.footnote, design: .rounded, weight: .semibold))
                .foregroundStyle(accent)
            } minimal: {
                Group {
                    if done { DoneBadge(accent: accent, lineWidth: 2.5) }
                    else { TimerRing(interval: state.clock(kind: kind, isStale: context.isStale), accent: accent) }
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
    @Environment(\.isLuminanceReduced) private var luminanceReduced

    // The Lock Screen cuts a Live Activity taller than 160 pt: big time + two lines + buttons only fit with the
    // caption under the ring (not in a row of its own on top).
    var body: some View {
        let accent = TimerPalette.accent(state.accentARGB)
        let clock = state.clock(kind: kind, isStale: isStale)
        let done = state.isFinished(kind: kind, isStale: isStale)
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .center, spacing: 12) {
                VStack(alignment: .leading, spacing: 1) {
                    if done {
                        // The time ran out while the app could not end the card: say so instead of a frozen 0:00.
                        FinishedText(state: state, compact: false, accent: accent)
                    } else {
                        BigCountdown(interval: clock, size: 46, showsBell: luminanceReduced)
                        NextSetText(state: state, compact: false)
                        if kind == emomKind, !state.paused, !state.showsRunEnd(kind: kind, isStale: isStale) {
                            Label {
                                TimerCountdown(interval: state.startedAt...state.endsAt)
                            } icon: { Image(systemName: "flag.checkered") }
                            .font(.caption.weight(.semibold)).foregroundStyle(.white.opacity(0.7))
                            .labelStyle(.titleAndIcon).padding(.top, 2)
                        }
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(spacing: 5) {
                    ZStack {
                        if done {
                            DoneBadge(accent: accent, lineWidth: 5)
                        } else {
                            TimerRing(interval: clock, accent: accent)
                            if state.showsRunEnd(kind: kind, isStale: isStale) {
                                // The minute passed while the app slept: the ring now counts down to the end of the run.
                                Image(systemName: "flag.checkered").font(.callout.weight(.semibold)).foregroundStyle(accent)
                            } else {
                                AppMark().frame(width: 22, height: 22)
                            }
                        }
                    }
                    .frame(width: 58, height: 58)
                    TimerCaption(kind: kind, title: state.title, accent: accent, showsIcon: false)
                        .frame(maxWidth: 84)
                }
            }
            if !done {
                ControlRow(kind: kind, activityID: activityID, state: state, accent: accent, height: 32, isStale: isStale)
            } else if state.hasFinishedRow(kind: kind) {
                FinishedActionRow(activityID: activityID, state: state, accent: accent, height: 32, plusWidth: 84)
            }
        }
        .padding(.horizontal, 16).padding(.vertical, 14)
        .foregroundStyle(.white)
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
/// System-driven ring: it empties as the time runs out. A nil interval is a paused EMOM run: a still ring.
private struct TimerRing: View {
    let interval: ClosedRange<Date>?
    let accent: Color

    var body: some View {
        if let interval {
            ProgressView(timerInterval: interval, countsDown: true) { EmptyView() } currentValueLabel: { EmptyView() }
                .progressViewStyle(.circular)
                .tint(accent)
        } else {
            ProgressView(value: 0.0) { EmptyView() } currentValueLabel: { EmptyView() }
                .progressViewStyle(.circular)
                .tint(accent)
        }
    }
}

/// A seconds countdown. With the dimmed Lock Screen (Always-On) iOS stops per-second updates and timer texts turn
/// into "<1 minute"-like labels, so there the end time is shown instead ("⏱ 11:42"), which stays exact.
private struct TimerCountdown: View {
    let interval: ClosedRange<Date>?
    @Environment(\.isLuminanceReduced) private var luminanceReduced

    var body: some View {
        if let interval {
            if luminanceReduced {
                Text(interval.upperBound, style: .time)
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
            } else {
                Text(timerInterval: interval, countsDown: true, showsHours: false)
                    .monospacedDigit()
                    .contentTransition(.numericText(countsDown: true))
            }
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
    /// A finished rest card with buttons (or the hint to open the app) instead of nothing under its text.
    func hasFinishedRow(kind: String) -> Bool {
        kind != emomKind && (!finishedActionKeys.isEmpty || needsApp)
    }

    func isFinished(kind: String, isStale: Bool) -> Bool {
        if paused { return false }
        if kind == emomKind { return endsAt <= Date() }
        return isStale || endsAt <= Date()
    }

    func showsRunEnd(kind: String, isStale: Bool) -> Bool {
        kind == emomKind && !paused && clock(kind: kind, isStale: isStale) == startedAt...endsAt
    }
}

private struct TimerCaption: View {
    let kind: String
    let title: String
    let accent: Color
    var showsIcon: Bool = true

    var body: some View {
        Label {
            Text(title).lineLimit(1).minimumScaleFactor(0.75)
        } icon: {
            if showsIcon {
                Image(systemName: kind == "workout.warmup" ? "flame.fill" : kind == emomKind ? "repeat" : "hourglass")
            }
        }
        .labelStyle(showsIcon ? AnyLabelStyle(.titleAndIcon) : AnyLabelStyle(.titleOnly))
        .font(.caption.weight(.semibold))
        .foregroundStyle(showsIcon ? accent : .white.opacity(0.65))
    }
}

/// Type-erased label style, so the caption can drop its icon where the app mark already says what it is.
private struct AnyLabelStyle: LabelStyle {
    private let make: (Configuration) -> AnyView
    init<S: LabelStyle>(_ style: S) { make = { AnyView(style.makeBody(configuration: $0)) } }
    func makeBody(configuration: Configuration) -> some View { make(configuration) }
}

/// Trainable's TR mark (vector, from the Android launcher drawable), kept in its own gradient colours.
private struct AppMark: View {
    var body: some View {
        Image("TrainableLogo").resizable().scaledToFit().accessibilityHidden(true)
    }
}

/// The big seconds countdown, the hero of the card. On the dimmed Lock Screen it becomes bell + end time.
private struct BigCountdown: View {
    let interval: ClosedRange<Date>?
    let size: CGFloat
    var showsBell: Bool = false

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            if showsBell && interval != nil {
                // Dimmed Lock Screen: the time is when the countdown ends, not a countdown.
                Image(systemName: "bell.fill").font(.system(size: size * 0.42, weight: .semibold))
            }
            TimerCountdown(interval: interval)
                .font(.system(size: showsBell ? size * 0.78 : size, weight: .semibold, design: .rounded))
                .multilineTextAlignment(.leading)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
        }
        .foregroundStyle(.white)
        .fixedSize(horizontal: false, vertical: true)
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

/// The rest is over: "+Xs" and "✓ Set done" (the wide one), or the hint to open the app when no workout answered.
private struct FinishedActionRow: View {
    let activityID: String
    let state: TrainableTimerAttributes.ContentState
    let accent: Color
    let height: CGFloat
    let plusWidth: CGFloat

    var body: some View {
        if state.needsApp {
            Text(state.openAppText)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white.opacity(0.75))
                .lineLimit(1).minimumScaleFactor(0.8)
                .frame(maxWidth: .infinity, minHeight: height, alignment: .leading)
        } else {
            let pairs = Array(zip(state.finishedActionKeys, state.finishedActionLabels))
            HStack(spacing: 8) {
                ForEach(pairs, id: \.0) { key, label in
                    let primary = key == "rest.next"
                    Button(intent: TimerControlIntent(activityID: activityID, instanceID: state.instanceID, action: key)) {
                        Group {
                            if primary {
                                Label { Text(label) } icon: { Image(systemName: "checkmark") }.labelStyle(.titleAndIcon)
                            } else {
                                Text(label).monospacedDigit()
                            }
                        }
                        .lineLimit(1).minimumScaleFactor(0.7)
                        .foregroundStyle(primary ? TimerPalette.onAccent(accent: state.accentARGB) : .white)
                        .frame(maxWidth: primary || pairs.count == 1 ? .infinity : plusWidth, minHeight: height)
                        .background(primary ? accent : .white.opacity(0.14), in: Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            .font(.subheadline.weight(.semibold))
        }
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
