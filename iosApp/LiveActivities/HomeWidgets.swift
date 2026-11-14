import SwiftUI
import WidgetKit

// The home-screen and Lock Screen widgets, design "B · Anelli": the app theme's primary → tertiary gradient (the TR
// logo's blue → teal in the default theme) on the ring and the main button, SF Rounded numbers, the TR mark in the
// header. Same three widgets as Android: Routine, Weekly goal, Quick start (Lock Screen only: no 1×1 on iOS).

// MARK: - Timeline

struct HomeEntry: TimelineEntry {
    let date: Date
    let day: HomeWidgetSnapshot.Day?
    let accent: HomeWidgetSnapshot.Accent?
    var isSample = false
}

struct HomeProvider: TimelineProvider {
    func placeholder(in context: Context) -> HomeEntry { HomeSample.entry }

    func getSnapshot(in context: Context, completion: @escaping (HomeEntry) -> Void) {
        completion(entries(now: Date()).first ?? HomeSample.entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<HomeEntry>) -> Void) {
        let list = entries(now: Date())
        guard !list.isEmpty else {
            // Nothing from the app yet: say to open it, and look again later
            completion(Timeline(entries: [HomeEntry(date: Date(), day: nil, accent: nil)],
                                policy: .after(Date().addingTimeInterval(30 * 60))))
            return
        }
        completion(Timeline(entries: list, policy: .atEnd))
    }

    /// The day showing now, then one entry at each later midnight the app already worked out.
    private func entries(now: Date) -> [HomeEntry] {
        guard let snapshot = HomeWidgetStore.read(), !snapshot.days.isEmpty else { return [] }
        let starts = snapshot.days.map { Date(timeIntervalSince1970: TimeInterval($0.startsAt)) }
        let current = starts.lastIndex { $0 <= now } ?? 0
        var list = [HomeEntry(date: now, day: snapshot.days[current], accent: snapshot.accent)]
        for index in snapshot.days.indices where index > current {
            list.append(HomeEntry(date: starts[index], day: snapshot.days[index], accent: snapshot.accent))
        }
        return list
    }
}

/// The gallery preview before the app ever wrote anything.
enum HomeSample {
    static let entry = HomeEntry(
        date: Date(),
        day: .init(startsAt: 0,
                   routine: .init(kind: "suggested", label: "Trainable", title: "Push A", subtitle: "6",
                                  button: "▶", link: "trainable://open", accessibility: "Trainable"),
                   week: .init(done: 3, goal: 4, met: false, label: "Trainable", title: "3/4", cardio: nil, unit: "",
                               days: ["trained", "empty", "trained", "trained", "empty", "today", "empty"]
                                   .map { HomeWidgetSnapshot.WeekDay(letter: "•", mark: $0) })),
        accent: nil, isSample: true)
}

// MARK: - Widgets

struct RoutineHomeWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "TrainableRoutine", provider: HomeProvider()) { entry in
            RoutineWidgetView(entry: entry).homeWidgetBackground()
        }
        .configurationDisplayName(GalleryText.routineName)
        .description(GalleryText.routineDescription)
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryRectangular, .accessoryInline])
    }
}

struct WeeklyGoalHomeWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "TrainableWeeklyGoal", provider: HomeProvider()) { entry in
            WeeklyGoalWidgetView(entry: entry).homeWidgetBackground()
        }
        .configurationDisplayName(GalleryText.goalName)
        .description(GalleryText.goalDescription)
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryCircular])
    }
}

struct QuickStartHomeWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "TrainableQuickStart", provider: HomeProvider()) { entry in
            QuickStartWidgetView(entry: entry).homeWidgetBackground()
        }
        .configurationDisplayName(GalleryText.quickName)
        .description(GalleryText.quickDescription)
        .supportedFamilies([.accessoryCircular])
    }
}

// MARK: - Colours

/// The theme gradient for the current appearance, with readable text colours taken from it.
struct HomePalette {
    let start: Color
    let end: Color
    /// Labels and outlines: the start colour, lifted on dark or deepened on light when it would read poorly.
    let ink: Color
    /// Text on the gradient.
    let onGradient: Color
    /// Ring track, tonal button, empty day.
    let soft: Color

    init(accent: HomeWidgetSnapshot.Accent?, dark: Bool) {
        let startARGB = accent.map { dark ? $0.darkStart : $0.lightStart } ?? (dark ? 0xFF8EA4F5 : 0xFF3A59D1)
        let endARGB = accent.map { dark ? $0.darkEnd : $0.lightEnd } ?? 0xFF7AC6D2
        let s = RGB(startARGB)
        start = s.color
        end = RGB(endARGB).color
        if dark {
            ink = s.lifted(towards: 1, below: 0.45).color
        } else {
            ink = s.lifted(towards: 0, below: -1, above: 0.5).color
        }
        onGradient = s.luminance > 0.5 ? Color(red: 0.07, green: 0.07, blue: 0.09) : .white
        soft = s.color.opacity(dark ? 0.26 : 0.16)
    }

    var gradient: LinearGradient {
        LinearGradient(colors: [start, end], startPoint: .topLeading, endPoint: .bottomTrailing)
    }

    private struct RGB {
        let r: Double, g: Double, b: Double
        init(_ argb: Int64) {
            r = Double((argb >> 16) & 0xFF) / 255
            g = Double((argb >> 8) & 0xFF) / 255
            b = Double(argb & 0xFF) / 255
        }
        init(r: Double, g: Double, b: Double) { self.r = r; self.g = g; self.b = b }
        var luminance: Double { 0.2126 * r + 0.7152 * g + 0.0722 * b }
        var color: Color { Color(red: r, green: g, blue: b) }

        /// Moves the colour towards white (1) or black (0) the further its luminance is past the limit.
        func lifted(towards target: Double, below: Double, above: Double = 2) -> RGB {
            let l = luminance
            let amount: Double
            if l < below { amount = (below - l) / max(below, 0.01) * 0.6 }
            else if l > above { amount = (l - above) / max(1 - above, 0.01) * 0.6 }
            else { amount = 0 }
            return RGB(r: r + (target - r) * amount, g: g + (target - g) * amount, b: b + (target - b) * amount)
        }
    }
}

private extension View {
    /// White / #1C1C1E behind home-screen widgets (iOS removes it in StandBy and the tinted modes); nothing behind
    /// Lock Screen ones.
    func homeWidgetBackground() -> some View {
        containerBackground(for: .widget) { HomeBackground() }
    }
}

private struct HomeBackground: View {
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        switch family {
        case .accessoryCircular, .accessoryRectangular, .accessoryInline:
            Color.clear
        default:
            scheme == .dark ? Color(red: 0x1C / 255, green: 0x1C / 255, blue: 0x1E / 255) : Color.white
        }
    }
}

// MARK: - Shared pieces

/// The TR mark, tinted.
private struct LogoMark: View {
    let size: CGFloat
    let color: Color
    var body: some View {
        Image("TrainableLogo").renderingMode(.template).resizable().scaledToFit()
            .frame(width: size, height: size)
            .foregroundStyle(color)
            .accessibilityHidden(true)
    }
}

private struct HeaderLabel: View {
    let text: String
    let palette: HomePalette
    var size: CGFloat = 12.5
    var body: some View {
        HStack(spacing: 5) {
            LogoMark(size: size + 1.5, color: palette.ink)
            Text(text)
                .font(.system(size: size, weight: .semibold))
                .foregroundStyle(palette.ink)
                .lineLimit(1)
        }
        .widgetAccentable()
    }
}

/// The smooth ring, gradient arc over a soft track (flat accent in the tinted modes).
private struct GoalRing: View {
    let done: Int
    let goal: Int
    let lineWidth: CGFloat
    let palette: HomePalette
    @Environment(\.widgetRenderingMode) private var renderingMode

    var body: some View {
        let fraction = goal > 0 ? min(1, Double(done) / Double(goal)) : 0
        ZStack {
            Circle().stroke(renderingMode == .fullColor ? palette.soft : Color.primary.opacity(0.18), lineWidth: lineWidth)
            if fraction > 0 {
                // Like the Activity rings, the colour runs along the arc: theme primary at the start, tertiary at
                // the tip. The gradient starts a little before 0° so the round start cap keeps the start colour.
                Circle()
                    .trim(from: 0, to: fraction)
                    .stroke(renderingMode == .fullColor
                                ? AnyShapeStyle(AngularGradient(colors: [palette.start, palette.end], center: .center,
                                                                startAngle: .degrees(fraction >= 1 ? 0 : -12),
                                                                endAngle: .degrees(fraction >= 1 ? 360 : max(360 * fraction - 12, 24))))
                                : AnyShapeStyle(Color.primary),
                            style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .widgetAccentable()
                if fraction >= 1 && renderingMode == .fullColor {
                    // A full ring: the tip laps over the start with a soft shadow, as on the Activity rings
                    Circle()
                        .trim(from: 0.97, to: 1)
                        .stroke(palette.end, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                        .shadow(color: .black.opacity(0.25), radius: 1.5, x: 1.5)
                }
            }
        }
        .padding(lineWidth / 2)
    }
}

/// "3/4": the count big and rounded, the goal smaller and muted.
private struct DoneOfGoal: View {
    let week: HomeWidgetSnapshot.Week
    let big: CGFloat
    var body: some View {
        (Text("\(week.done)").font(.system(size: big, weight: .bold, design: .rounded))
         + Text("/\(week.goal)").font(.system(size: big * 0.52, weight: .bold, design: .rounded)).foregroundColor(.secondary))
            .lineLimit(1)
            .minimumScaleFactor(0.6)
    }
}

/// The week as seven capsules: filled for a day trained, outlined for today.
private struct DayBars: View {
    let days: [HomeWidgetSnapshot.WeekDay]
    let palette: HomePalette
    @Environment(\.widgetRenderingMode) private var renderingMode

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(days.enumerated()), id: \.offset) { _, day in
                VStack(spacing: 3) {
                    bar(day.mark)
                    Text(day.letter)
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(day.mark == "today" ? AnyShapeStyle(palette.ink) : AnyShapeStyle(HierarchicalShapeStyle.secondary))
                }
                .frame(maxWidth: .infinity)
            }
        }
    }

    @ViewBuilder private func bar(_ mark: String) -> some View {
        let shape = Capsule()
        let full = renderingMode == .fullColor
        switch mark {
        case "trained":
            shape.fill(full ? AnyShapeStyle(LinearGradient(colors: [palette.end, palette.start], startPoint: .top, endPoint: .bottom))
                            : AnyShapeStyle(Color.primary))
                .frame(width: 10, height: 24).widgetAccentable()
        case "today":
            shape.strokeBorder(full ? palette.ink : Color.primary, lineWidth: 1.5)
                .frame(width: 10, height: 24).widgetAccentable()
        default:
            shape.fill(full ? palette.soft : Color.primary.opacity(0.18)).frame(width: 10, height: 24)
        }
    }
}

/// The main action as a capsule: gradient to start, tonal to resume, outlined to open the app.
private struct ActionCapsule: View {
    let routine: HomeWidgetSnapshot.Routine
    let palette: HomePalette
    let height: CGFloat
    @Environment(\.widgetRenderingMode) private var renderingMode

    var body: some View {
        let full = renderingMode == .fullColor
        HStack(spacing: 5) {
            if routine.kind != "none" { Image(systemName: "play.fill").font(.system(size: 12, weight: .bold)) }
            Text(routine.button).font(.system(size: 14, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.8)
        }
        .foregroundStyle(foreground(full))
        .frame(maxWidth: .infinity)
        .frame(height: height)
        .background { background(full) }
        .widgetAccentable()
    }

    private func foreground(_ full: Bool) -> Color {
        guard full else { return .primary }
        return routine.kind == "suggested" ? palette.onGradient : palette.ink
    }

    @ViewBuilder private func background(_ full: Bool) -> some View {
        if !full {
            Capsule().fill(Color.primary.opacity(0.16))
        } else {
            switch routine.kind {
            case "suggested": Capsule().fill(palette.gradient)
            case "inProgress": Capsule().fill(palette.soft)
            default: Capsule().strokeBorder(palette.ink, lineWidth: 1.5)
            }
        }
    }
}

private struct OpenAppHint: View {
    let palette: HomePalette
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            LogoMark(size: 26, color: palette.ink)
            Spacer(minLength: 0)
            Text("Trainable").font(.system(size: 17, weight: .bold, design: .rounded))
            Text(GalleryText.openTrainable).font(.system(size: 12.5)).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .widgetURL(URL(string: "trainable://open"))
    }
}

private func glyph(for kind: String) -> String {
    switch kind {
    case "inProgress": return "dumbbell.fill"
    case "none": return "plus"
    default: return "play.fill"
    }
}

// MARK: - Routine

struct RoutineWidgetView: View {
    let entry: HomeEntry
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let palette = HomePalette(accent: entry.accent, dark: scheme == .dark)
        Group {
            if let routine = entry.day?.routine {
                switch family {
                case .systemMedium: medium(routine, palette)
                case .accessoryRectangular: rectangular(routine)
                case .accessoryInline: inline(routine)
                default: small(routine, palette)
                }
            } else if family == .accessoryInline || family == .accessoryRectangular {
                Label("Trainable", systemImage: "figure.strengthtraining.traditional")
                    .widgetURL(URL(string: "trainable://open"))
            } else {
                OpenAppHint(palette: palette)
            }
        }
        .redacted(reason: entry.isSample ? .placeholder : [])
    }

    /// The whole small widget is one tap: it does what its button says.
    private func small(_ routine: HomeWidgetSnapshot.Routine, _ palette: HomePalette) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HeaderLabel(text: routine.label, palette: palette)
            Text(routine.title)
                .font(.system(size: 21, weight: .bold, design: .rounded))
                .lineLimit(2).minimumScaleFactor(0.75)
                .padding(.top, 8)
            if let subtitle = routine.subtitle {
                Text(subtitle).font(.system(size: 12.5)).foregroundStyle(.secondary).lineLimit(1).padding(.top, 1)
            }
            Spacer(minLength: 6)
            ActionCapsule(routine: routine, palette: palette, height: 34)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .widgetURL(URL(string: routine.link))
        .accessibilityElement(children: .combine)
        .accessibilityLabel(routine.accessibility)
    }

    /// The round button does the action; the rest opens the app, like Android.
    private func medium(_ routine: HomeWidgetSnapshot.Routine, _ palette: HomePalette) -> some View {
        HStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 2) {
                HeaderLabel(text: routine.label, palette: palette)
                Spacer(minLength: 4)
                Text(routine.title)
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                    .lineLimit(2).minimumScaleFactor(0.65)
                if let subtitle = routine.subtitle {
                    Text(subtitle).font(.system(size: 13)).foregroundStyle(.secondary).lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)

            Link(destination: URL(string: routine.link) ?? URL(string: "trainable://open")!) {
                VStack(spacing: 6) {
                    BigActionButton(kind: routine.kind, palette: palette, side: 64)
                    Text(routine.button)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(palette.ink)
                        .lineLimit(1).minimumScaleFactor(0.7)
                        .widgetAccentable()
                }
                .frame(minWidth: 72)
            }
            .accessibilityLabel(routine.accessibility)
        }
        .widgetURL(URL(string: "trainable://open"))
    }

    private func rectangular(_ routine: HomeWidgetSnapshot.Routine) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 4) {
                Image("TrainableLogo").renderingMode(.template).resizable().scaledToFit().frame(width: 11, height: 11)
                Text(routine.label).font(.system(size: 12, weight: .semibold)).lineLimit(1)
            }
            .foregroundStyle(.secondary)
            Text(routine.title)
                .font(.system(size: 17, weight: .bold, design: .rounded))
                .lineLimit(1).minimumScaleFactor(0.7)
                .widgetAccentable()
            Text(routine.subtitle ?? routine.button).font(.system(size: 13)).foregroundStyle(.secondary).lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .widgetURL(URL(string: routine.link))
        .accessibilityElement(children: .combine)
        .accessibilityLabel(routine.accessibility)
    }

    private func inline(_ routine: HomeWidgetSnapshot.Routine) -> some View {
        Label(routine.title, systemImage: "figure.strengthtraining.traditional")
            .widgetURL(URL(string: routine.link))
    }
}

/// The medium widget's round button: gradient play, tonal dumbbell for an open workout, outlined plus for none.
private struct BigActionButton: View {
    let kind: String
    let palette: HomePalette
    let side: CGFloat
    @Environment(\.widgetRenderingMode) private var renderingMode

    var body: some View {
        let full = renderingMode == .fullColor
        ZStack {
            switch kind {
            case "suggested":
                Circle().fill(full ? AnyShapeStyle(palette.gradient) : AnyShapeStyle(Color.primary.opacity(0.2)))
            case "inProgress":
                Circle().fill(full ? palette.soft : Color.primary.opacity(0.2))
            default:
                Circle().strokeBorder(full ? palette.ink : Color.primary, lineWidth: 2)
            }
            Image(systemName: glyph(for: kind))
                .font(.system(size: side * 0.4, weight: .bold))
                .foregroundStyle(full ? (kind == "suggested" ? palette.onGradient : palette.ink) : Color.primary)
                .offset(x: kind == "suggested" ? side * 0.04 : 0)
        }
        .frame(width: side, height: side)
        .widgetAccentable()
    }
}

// MARK: - Weekly goal

struct WeeklyGoalWidgetView: View {
    let entry: HomeEntry
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let palette = HomePalette(accent: entry.accent, dark: scheme == .dark)
        Group {
            if let week = entry.day?.week {
                switch family {
                case .systemMedium: medium(week, palette)
                case .accessoryCircular: circular(week)
                default: small(week, palette)
                }
            } else if family == .accessoryCircular {
                ZStack {
                    AccessoryWidgetBackground()
                    Image("TrainableLogo").renderingMode(.template).resizable().scaledToFit().padding(14)
                }
                .widgetURL(URL(string: "trainable://open"))
            } else {
                OpenAppHint(palette: palette)
            }
        }
        .redacted(reason: entry.isSample ? .placeholder : [])
    }

    private func small(_ week: HomeWidgetSnapshot.Week, _ palette: HomePalette) -> some View {
        ZStack {
            GoalRing(done: week.done, goal: week.goal, lineWidth: 15, palette: palette)
            VStack(spacing: 2) {
                DoneOfGoal(week: week, big: 34)
                if week.met {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(palette.ink)
                        .widgetAccentable()
                } else {
                    Text(week.unit).font(.system(size: 11.5, weight: .medium)).foregroundStyle(.secondary).lineLimit(1)
                }
            }
            .padding(.horizontal, 24)
        }
        .widgetURL(URL(string: "trainable://open"))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(week.label): \(week.done)/\(week.goal). \(week.title)")
    }

    private func medium(_ week: HomeWidgetSnapshot.Week, _ palette: HomePalette) -> some View {
        HStack(spacing: 16) {
            ZStack {
                GoalRing(done: week.done, goal: week.goal, lineWidth: 14, palette: palette)
                DoneOfGoal(week: week, big: 28).padding(.horizontal, 20)
            }
            .aspectRatio(1, contentMode: .fit)

            VStack(alignment: .leading, spacing: 2) {
                HeaderLabel(text: week.label, palette: palette)
                Text(week.title)
                    .font(.system(size: 17, weight: .bold, design: .rounded))
                    .lineLimit(1).minimumScaleFactor(0.7)
                if let cardio = week.cardio {
                    Text(cardio).font(.system(size: 12.5)).foregroundStyle(.secondary).lineLimit(1)
                }
                Spacer(minLength: 6)
                DayBars(days: week.days, palette: palette)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .widgetURL(URL(string: "trainable://open"))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(week.label): \(week.done)/\(week.goal). \(week.title)" + (week.cardio.map { ", \($0)" } ?? ""))
    }

    private func circular(_ week: HomeWidgetSnapshot.Week) -> some View {
        Gauge(value: Double(min(week.done, max(week.goal, 1))), in: 0...Double(max(week.goal, 1))) {
            Image("TrainableLogo").renderingMode(.template).resizable().scaledToFit()
        } currentValueLabel: {
            Text("\(week.done)/\(week.goal)").font(.system(size: 15, weight: .bold, design: .rounded))
        }
        .gaugeStyle(.accessoryCircularCapacity)
        .widgetAccentable()
        .widgetURL(URL(string: "trainable://open"))
        .accessibilityLabel("\(week.label): \(week.done)/\(week.goal). \(week.title)")
    }
}

// MARK: - Quick start

/// One tap: start the suggested routine, resume the open workout or open the app.
struct QuickStartWidgetView: View {
    let entry: HomeEntry

    var body: some View {
        let routine = entry.day?.routine
        ZStack {
            AccessoryWidgetBackground()
            Image(systemName: glyph(for: routine?.kind ?? "none"))
                .font(.system(size: 22, weight: .bold))
                .offset(x: routine?.kind == "suggested" ? 1.5 : 0)
                .widgetAccentable()
        }
        .widgetURL(URL(string: routine?.link ?? "trainable://open"))
        .accessibilityLabel(routine?.accessibility ?? GalleryText.openTrainable)
    }
}

// MARK: - Gallery texts

/// Widget names in the widget gallery, in the phone's language (the same strings as the Android widgets).
enum GalleryText {
    private static let language: String = {
        let code = Locale.preferredLanguages.first.map { String($0.prefix(2)).lowercased() } ?? "en"
        return table.keys.contains(code) ? code : "en"
    }()

    private static func text(_ key: Int) -> String { table[language]?[key] ?? table["en"]![key] }

    static var routineName: String { text(0) }
    static var routineDescription: String { text(1) }
    static var goalName: String { text(2) }
    static var goalDescription: String { text(3) }
    static var quickName: String { text(4) }
    static var quickDescription: String { text(5) }
    static var openTrainable: String { text(6) }

    private static let table: [String: [String]] = [
        "en": ["Workout", "Suggested workout and quick start", "Weekly Goal", "Weekly workout progress tracker",
               "Quick start", "Start or resume your workout with one tap", "Open Trainable"],
        "it": ["Allenamento", "Allenamento suggerito e avvio rapido", "Obiettivo Settimanale", "Progressi allenamenti settimanali",
               "Avvio rapido", "Avvia o riprendi l'allenamento con un tocco", "Apri Trainable"],
        "de": ["Training", "Vorgeschlagenes Training und Schnellstart", "Wochenziel", "Wöchentliche Fortschrittsverfolgung",
               "Schnellstart", "Training mit einem Tipp starten oder fortsetzen", "Trainable öffnen"],
        "es": ["Entrenamiento", "Entrenamiento sugerido e inicio rápido", "Objetivo Semanal", "Seguimiento del progreso semanal",
               "Inicio rápido", "Inicia o reanuda tu entrenamiento con un toque", "Abrir Trainable"],
        "fr": ["Entraînement", "Entraînement suggéré et démarrage rapide", "Objectif Hebdomadaire", "Suivi des objectifs hebdomadaires",
               "Démarrage rapide", "Démarrez ou reprenez votre séance d'un geste", "Ouvrir Trainable"],
        "pt": ["Treino", "Treino sugerido e início rápido", "Meta Semanal", "Acompanhamento da meta semanal",
               "Início rápido", "Inicie ou retome seu treino com um toque", "Abrir Trainable"],
    ]
}
