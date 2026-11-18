package com.emanuel5014.trainable.ui

import kotlin.test.*

class IosNextSetTextTest {
    private val words = IosNotificationWords("Rest", "Go", "Warmup", "Ready", "Membership", "In %1\$d days",
        "Round %1\$d/%2\$d", "Complete", "Get ready")
    @Test fun ordinarySeriesIncludesExerciseNumberLoadRepsAndPreviousReps() {
        assertEquals("Next: Squat - Set 2: 20 kg × 8 (last: 6)",
            words.nextSet("Squat", 2, 20f, 8, 6, "kg", null, null, null))
    }
    @Test fun advancedPrescriptionKeepsMaxAndTechniqueDetailsWithoutNumericPreviousReps() {
        assertEquals("Next: Squat - Set 3: 60 kg × MAX · 75% · Pause 2″",
            words.nextSet("Squat", 3, 60f, null, 10, "kg", "MAX", "75% · Pause 2″", null))
    }
    @Test fun timedSeriesUsesExistingFormattedTextInsteadOfInventingWeightAndReps() {
        assertEquals("Next: Sprint - Set 2: 45s · 20 reps",
            words.nextSet("Sprint", 2, 20f, 8, null, "kg", null, null, "45s · 20 reps"))
    }
    @Test fun placeholderLikeExerciseNameIsPreservedAndIncompleteContextIsEmpty() {
        assertEquals("Next: Bench %2\$s - Set 2: 20 kg × 8",
            words.nextSet("Bench %2\$s", 2, 20f, 8, null, "kg", null, null, null))
        assertEquals("", words.nextSet("Squat", null, 20f, 8, null, "kg", null, null, null))
    }
}
