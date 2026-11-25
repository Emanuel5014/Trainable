package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.util.notification.LiveSegment
import com.emanuel5014.trainable.util.notification.WorkoutLiveCard
import kotlin.test.*

private class RecordingLiveActivities : IosWorkoutLiveActivities {
    val cards = mutableMapOf<String, IosLiveTimer>()
    override fun isAvailable() = true
    override fun update(timer: IosLiveTimer) { cards[timer.kind] = timer }
    override fun cancel(kind: String) { cards.remove(kind) }
    override fun cancelAll() { cards.clear() }
    override fun cancelSession(sessionId: Int) { cards.values.removeAll { it.sessionId == sessionId } }
}

class IosLiveCardsTest {
    private val words = IosLiveCardWords(setTitle = "%1\$s · serie %2\$d di %3\$d", next = "Prossima: %1\$s",
        nextExercise = "Prossimo esercizio: %1\$s", exerciseOf = "Esercizio %1\$d di %2\$d", then = "poi %1\$s",
        setOne = "%d serie", setMany = "%d serie", exerciseOne = "%d esercizio", exerciseMany = "%d esercizi",
        setDone = "Serie fatta")
    private val card = WorkoutLiveCard(sessionId = 7, workoutName = "Push A", startedAtMillis = 1_000_000L,
        exerciseName = "Panca", exerciseNumber = 2, exerciseCount = 6, setNumber = 3, setsInExercise = 4,
        value = "80 kg × 8", nextExerciseName = "Croci", setDoneToken = "1001:3:80.0:8",
        segments = listOf(LiveSegment.DONE, LiveSegment.DONE, LiveSegment.CURRENT, LiveSegment.TODO))
    private fun rest(endsAt: Long = 1_090_000L) = IosLiveTimer(REST_NOTICE, "rest-1", "Rest", "Next: Panca", endsAt, 90,
        sessionId = 7, sessionStartedAtMillis = 1_000_000L, finishedText = "REST OVER")
    private fun composer(target: RecordingLiveActivities) = IosLiveCardComposer(target, { words }, { 1_010_000L })

    @Test fun withoutALiveCardTimerCardsPassStraightThrough() {
        val target = RecordingLiveActivities(); val c = composer(target)
        c.update(rest())
        assertEquals(setOf(REST_NOTICE), target.cards.keys)
        c.cancel(REST_NOTICE)
        assertTrue(target.cards.isEmpty())
    }

    @Test fun inASetTheCardShowsTheValueAndSetDone() {
        val target = RecordingLiveActivities(); val c = composer(target)
        c.showBase(card)
        val live = target.cards.getValue(LIVE_NOTICE)
        assertEquals(LiveModes.SET, live.liveMode)
        assertEquals("80 kg × 8", live.liveValue)
        assertEquals("Panca · serie 3 di 4", live.liveTitle)
        assertEquals("poi Croci", live.liveDetail)
        assertEquals("Esercizio 2 di 6", live.liveSide)
        assertEquals("Push A", live.workoutName)
        assertEquals(listOf(2, 2, 1, 0), live.segments)
        assertEquals(listOf(LIVE_NEXT_ACTION), live.actionKeys); assertEquals(listOf("Serie fatta"), live.actionLabels)
        assertEquals("1001:3:80.0:8", live.nextSetToken)
        assertEquals("3/4", live.compactText)
        assertEquals(1_000_000L, live.workoutStartedAtMillis)
        assertTrue(live.liveUntilMillis > live.workoutStartedAtMillis)
    }

    @Test fun theLiveCardTakesThePlaceOfTheRestCardAndKeepsItsButtons() {
        val target = RecordingLiveActivities(); val c = composer(target)
        c.update(rest())
        c.showBase(card)
        assertFalse(REST_NOTICE in target.cards)
        val live = target.cards.getValue(LIVE_NOTICE)
        assertEquals(LiveModes.REST, live.liveMode)
        assertEquals(REST_NOTICE, live.timerKind); assertEquals("rest-1", live.timerInstanceId)
        assertEquals(1_090_000L, live.endsAtMillis); assertEquals(90, live.totalSeconds)
        assertEquals("REST OVER", live.finishedText)
        assertEquals("Prossima: Panca · serie 3 di 4", live.liveTitle)
        assertTrue(live.actionKeys.isEmpty())
        // A rest that ends goes back to the set
        c.cancel(REST_NOTICE)
        assertEquals(LiveModes.SET, target.cards.getValue(LIVE_NOTICE).liveMode)
        assertFalse(REST_NOTICE in target.cards)
    }

    @Test fun theFirstSetOfAnExerciseIsAnnouncedAsANewExercise() {
        val target = RecordingLiveActivities(); val c = composer(target)
        c.showBase(card.copy(startsExercise = true, exerciseName = "Croci", setNumber = 1))
        c.update(rest())
        assertEquals("Prossimo esercizio: Croci", target.cards.getValue(LIVE_NOTICE).liveTitle)
    }

    @Test fun endingTheLiveCardBringsTheTimerCardsBack() {
        val target = RecordingLiveActivities(); val c = composer(target)
        c.showBase(card)
        c.update(rest())
        c.endBase(7)
        assertFalse(LIVE_NOTICE in target.cards)
        assertEquals("rest-1", target.cards.getValue(REST_NOTICE).instanceId)
        c.endBase(7)
        assertFalse(c.isLive)
    }

    @Test fun anotherWorkoutsEndLeavesTheCard() {
        val target = RecordingLiveActivities(); val c = composer(target)
        c.showBase(card)
        c.endBase(8)
        assertTrue(LIVE_NOTICE in target.cards)
    }

    @Test fun aPressWithoutTheWorkoutHidesSetDoneUntilTheWorkoutSpeaks() {
        val target = RecordingLiveActivities(); val c = composer(target)
        c.showBase(card)
        c.needingApp()
        val live = target.cards.getValue(LIVE_NOTICE)
        assertTrue(live.needsApp); assertTrue(live.actionKeys.isEmpty())
        c.showBase(card)
        assertFalse(target.cards.getValue(LIVE_NOTICE).needsApp)
    }

    @Test fun onceAllIsDoneTheCardSaysSo() {
        val target = RecordingLiveActivities(); val c = composer(target)
        c.showBase(card.copy(allDone = true, doneSets = 22, doneExercises = 6, volume = "8420 kg",
            segments = List(6) { LiveSegment.DONE }))
        c.update(rest())
        val live = target.cards.getValue(LIVE_NOTICE)
        assertEquals(LiveModes.DONE, live.liveMode)
        assertEquals("22 serie · 6 esercizi", live.liveTitle)
        assertTrue(live.liveDetail.contains("8420 kg"))
    }

    @Test fun deletingTheWorkoutClearsItsLiveCard() {
        val target = RecordingLiveActivities(); val c = composer(target)
        c.showBase(card)
        c.cancelSession(7)
        assertFalse(c.isLive)
        assertTrue(target.cards.isEmpty())
    }

    @Test fun wordsFillNumberedAndPlainPlaceholders() {
        assertEquals("Panca · serie 3 di 4", words.fill(words.setTitle, "Panca", 3, 4))
        assertEquals("1 serie", words.sets(1)); assertEquals("2 esercizi", words.exercises(2))
    }
}
