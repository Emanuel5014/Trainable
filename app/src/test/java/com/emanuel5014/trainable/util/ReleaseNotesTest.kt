package com.emanuel5014.trainable.util

import com.emanuel5014.trainable.data.remote.ReleaseNoteItem
import com.emanuel5014.trainable.data.remote.ReleaseNotes
import com.emanuel5014.trainable.data.remote.localized
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesTest {

    // Shape of the real v2.9.0 / v3.0.0-beta2 texts on GitHub
    private val legacy = """
        ### 📝 What's New in this Release (v3.0.0-beta2)

        ⚠️ This is a beta of Trainable 3.0: install this APK by hand over 2.9.1.

        | Category | Feature / Fix | Description |
        | :--- | :--- | :--- |
        | 🏠 **Dashboard** | **A New Dashboard** | Unfinished workouts, today's routine and the suggested one now sit right under the membership and weekly goal cards: the first as a big card, the others as rows with their own colour and icon. The card of a workout in progress turns amber |
        | 🫧 **Liquid Glass** | **Wobbly Navigation Pill** | The glass pill stretches with its speed. It is on by default |
        | 🏃 **Exercises** | **Cardio History on the Exercise Screen** | The screen of a cardio exercise now lists its past workouts like any other exercise #58 |
        | 🛠️ **Fixes & Stability** | **A Deleted Workout Ends Its Timers** | Deleting a workout from any screen now also removes its rest, warm-up or EMOM notification |
    """.trimIndent()

    @Test
    fun `old markdown table becomes items`() {
        val notes = ReleaseNotesParser.fromMarkdown(legacy)!!
        assertEquals(4, notes.items.size)
        val (dashboard, glass, exercises, fix) = notes.items
        assertEquals("dashboard", dashboard.area)
        assertEquals(ReleaseNoteItem.TYPE_NEW, dashboard.type)
        assertEquals("A New Dashboard", dashboard.title["en"])
        // A long first sentence is cut at its colon
        assertEquals(
            "Unfinished workouts, today's routine and the suggested one now sit right under the membership and weekly goal cards",
            dashboard.summary["en"]
        )
        assertTrue(dashboard.details["en"]!!.endsWith("turns amber"))
        assertEquals("liquid_glass", glass.area)
        assertEquals("The glass pill stretches with its speed", glass.summary["en"])
        assertEquals(listOf(58), exercises.issues)
        assertTrue(!exercises.summary["en"]!!.contains("#58"))
        assertEquals(ReleaseNoteItem.TYPE_FIXED, fix.type)
        // One sentence: no details to open
        assertTrue(fix.details.isEmpty())
    }

    @Test
    fun `text without a table is left as it is`() {
        assertNull(ReleaseNotesParser.fromMarkdown("### Test\n- Fake release\n- Same APK"))
    }

    @Test
    fun `area from old categories`() {
        assertEquals("routines", ReleaseNotesParser.areaOf("📋 Routines"))
        assertEquals("import_export", ReleaseNotesParser.areaOf("📤 CSV"))
        assertEquals("updates", ReleaseNotesParser.areaOf("🧪 Updates"))
        assertEquals("other", ReleaseNotesParser.areaOf("🛠️ Fixes & Stability"))
    }

    @Test
    fun `changelog json is read with language fallback`() {
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val notes = json.decodeFromString<ReleaseNotes>(
            """{"version":"3.0.0-beta3","headline":{"en":"Hello","it":"Ciao"},
               "items":[{"type":"improved","area":"history","title":{"en":"History","it":"Storico"},
               "summary":{"en":"Faster","it":"Più veloce"},"issues":[69],"future_field":true}]}"""
        )
        assertEquals("Ciao", notes.headline.localized("it"))
        assertEquals("Hello", notes.headline.localized("de")) // missing language: English
        assertEquals(ReleaseNoteItem.TYPE_IMPROVED, notes.items.single().type)
        assertEquals(listOf(69), notes.items.single().issues)
        assertEquals("", emptyMap<String, String>().localized("it"))
    }

    @Test
    fun `display version from tag`() {
        assertEquals("3.0.0 beta 2", releaseDisplayVersion("v3.0.0-beta2"))
        assertEquals("3.0.0 beta 10", releaseDisplayVersion("v3.0.0-beta.10"))
        assertEquals("3.0.0 RC 1", releaseDisplayVersion("v3.0.0-rc1"))
        assertEquals("3.0.0", releaseDisplayVersion("v3.0.0"))
        assertEquals("ios-2.9.1", releaseDisplayVersion("ios-2.9.1"))
    }
}
