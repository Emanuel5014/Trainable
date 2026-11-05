package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.ui.screens.workout.PersistedMiniSet
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class IosWorkoutPlatformTest {
    @Test fun miniSetSnapshotPreservesLongTimestampsAcrossInstancesAndClears() {
        val key = "workout_test.${Uuid.random()}"
        val store = IosWorkoutMiniSetStore(key)
        try {
            assertEquals(-1, store.sessionId())
            assertEquals(42L, store.startedAt(42L))
            store.save(PersistedMiniSet(17, 25, 3, 8000000000000L, 8000000012345L, -1))
            val reopened = IosWorkoutMiniSetStore(key)
            assertEquals(17, reopened.sessionId())
            assertEquals(25, reopened.setId())
            assertEquals(3, reopened.missing())
            assertEquals(8000000000000L, reopened.startedAt(0L))
            assertEquals(8000000012345L, reopened.endsAt())
            assertEquals(-1, reopened.pauseSeconds())
            reopened.save(PersistedMiniSet(18, 26, 1, 9L, 10L, 15))
            assertEquals(18, store.sessionId())
            assertEquals(15, store.pauseSeconds())
            reopened.clear()
            assertEquals(-1, store.sessionId())
            assertEquals(-1, store.setId())
            assertEquals(0, store.missing())
            assertEquals(11L, store.startedAt(11L))
            assertEquals(0L, store.endsAt())
        } finally { store.clear() }
    }

    @Test fun localNotificationsDoNotClaimLiveActivitySupport() {
        val timers = IosWorkoutTimers(IosNotificationScheduler(FakeNotificationDelivery().apply { canAuthorize = false; granted = false }))
        assertFalse(timers.hasNotificationPermission())
        assertFalse(timers.isLiveNotificationSupported())
        timers.cancelTimer()
        timers.cancelWarmupTimer()
        timers.cancelEmom()
    }
}
