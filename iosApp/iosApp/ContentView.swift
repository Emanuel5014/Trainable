import SwiftUI
import Shared

/// The first iOS screen: the routines and the last workouts, read from the database of the shared Kotlin module.
struct ContentView: View {
    @State private var routines: [RoutineItem] = []
    @State private var workouts: [WorkoutItem] = []
    @State private var exerciseCount: Int32 = 0
    @State private var loadError: String?

    private let data = HomeData()

    var body: some View {
        NavigationStack {
            List {
                Section("Routines") {
                    if routines.isEmpty {
                        Text("No routines yet").foregroundStyle(.secondary)
                    }
                    ForEach(routines, id: \.id) { routine in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(routine.name).font(.headline)
                            Text("\(routine.exerciseCount) exercises" + (routine.active ? "" : " · archived"))
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                Section("Recent workouts") {
                    if workouts.isEmpty {
                        Text("No workouts yet").foregroundStyle(.secondary)
                    }
                    ForEach(workouts, id: \.id) { workout in
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(workout.routine)
                                Text(workout.date).font(.subheadline).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Text("\(workout.workingSets) sets").foregroundStyle(.secondary)
                        }
                    }
                }
                Section {
                    Text("Exercises in the library: \(exerciseCount)")
                } footer: {
                    if let loadError {
                        Text(loadError).foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle("Trainable")
            .task { await load() }
        }
    }

    private func load() async {
        do {
            routines = try await data.routines()
            workouts = try await data.recentWorkouts(limit: 10)
            exerciseCount = try await data.exerciseCount().int32Value
        } catch {
            loadError = error.localizedDescription
        }
    }
}
