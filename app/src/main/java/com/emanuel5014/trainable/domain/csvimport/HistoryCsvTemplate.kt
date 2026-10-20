package com.emanuel5014.trainable.domain.csvimport

import com.emanuel5014.trainable.data.ExerciseTranslations
import java.time.LocalDate

/**
 * The example file offered next to the import: the real header of the app in its language and a few workouts that show
 * every kind of row (strength, timed hold, timed reps, cardio), two routines, a workout without one and how long each
 * workout took. Importing it
 * unchanged works, so what it shows is also what the import accepts.
 */
object HistoryCsvTemplate {
    /** Short notes written in the language of the app. */
    data class Notes(val warmup: String, val easy: String)

    private data class Row(
        val daysAgo: Long,
        val session: Int,
        val plan: String,
        /** Stored (English) name of a built-in exercise, or the name of one the import will create. */
        val exercise: String,
        val category: String,
        val type: String,
        val set: Int,
        val weight: String = "",
        val reps: String = "",
        val seconds: String = "",
        val km: String = "",
        val note: String = ""
    )

    fun build(languageCode: String, weightUnit: String, today: LocalDate, notes: Notes): String {
        val rows = rows(notes)
        return buildString {
            appendLine(HistoryCsvHeaders.basic(languageCode, weightUnit) + "," + HistoryCsvHeaders.sessionDuration(languageCode))
            rows.forEach { row ->
                val builtIn = row.exercise in BUILT_IN
                val name = if (builtIn) ExerciseTranslations.translate(row.exercise, languageCode) else row.exercise
                val category = ExerciseTranslations.translateCategory(row.category, languageCode)
                val cells = listOf(
                    today.minusDays(row.daysAgo).toString(), row.session.toString(), row.plan, name, category, row.type,
                    row.set.toString(), row.weight, row.reps, row.seconds, row.km, row.note, SESSION_MINUTES.getValue(row.session)
                )
                appendLine(cells.joinToString(",") { escape(it) })
            }
        }
    }

    private fun rows(notes: Notes): List<Row> = buildList {
        // A push day: heavy sets with a light first one, a timed hold, a cardio finish
        add(Row(10, 1, "Push A", "Flat Bench Press", "Chest", "strength", 1, "40", "10", note = notes.warmup))
        add(Row(10, 1, "Push A", "Flat Bench Press", "Chest", "strength", 2, "60", "8"))
        add(Row(10, 1, "Push A", "Flat Bench Press", "Chest", "strength", 3, "60", "8"))
        add(Row(10, 1, "Push A", "Flat Bench Press", "Chest", "strength", 4, "57.5", "9"))
        add(Row(10, 1, "Push A", "Plank", "Core", "time_and_weight", 1, "0", "", "45"))
        add(Row(10, 1, "Push A", "Plank", "Core", "time_and_weight", 2, "0", "", "50"))
        add(Row(10, 1, "Push A", "Treadmill", "Cardio", "cardio", 1, "", "", "600", "1.5", notes.easy))
        // A pull day: bodyweight sets (weight 0), and an exercise the app does not have, which the import creates
        add(Row(8, 2, "Pull A", "Pull-ups", "Back", "strength", 1, "0", "8"))
        add(Row(8, 2, "Pull A", "Pull-ups", "Back", "strength", 2, "0", "7"))
        add(Row(8, 2, "Pull A", "Pull-ups", "Back", "strength", 3, "0", "6"))
        add(Row(8, 2, "Pull A", "Burpees", "Core", "time_and_reps", 1, "", "12", "30"))
        add(Row(8, 2, "Pull A", "Burpees", "Core", "time_and_reps", 2, "", "10", "30"))
        // No routine: a quick workout
        add(Row(5, 3, "", "Barbell Squat", "Legs", "strength", 1, "80", "5"))
        add(Row(5, 3, "", "Barbell Squat", "Legs", "strength", 2, "80", "5"))
        // The same routine again, a week later, so the routine is seen to hold more than one workout
        add(Row(3, 4, "Push A", "Flat Bench Press", "Chest", "strength", 1, "62.5", "8"))
        add(Row(3, 4, "Push A", "Flat Bench Press", "Chest", "strength", 2, "62.5", "7"))
    }

    /** How long each example workout took, in minutes. */
    private val SESSION_MINUTES = mapOf(1 to "65", 2 to "50", 3 to "40", 4 to "55")

    private val BUILT_IN = setOf("Flat Bench Press", "Plank", "Treadmill", "Pull-ups", "Barbell Squat")

    private fun escape(value: String): String =
        if (value.contains(',') || value.contains('"')) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
