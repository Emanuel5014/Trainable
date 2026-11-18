package com.emanuel5014.trainable.ui.screens.routines

import com.emanuel5014.trainable.data.ai.ScanPhase
import com.emanuel5014.trainable.data.ai.ScannedProgram
import com.emanuel5014.trainable.data.ai.backend.ConsentRequest
import com.emanuel5014.trainable.data.ai.backend.ScanBackendException
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ProgramScanModelTest {
    private val request = ConsentRequest("fixture", "Fixture", "provider.example.invalid")
    private val result = ScannedProgram(mapOf(1 to listOf(PrescriptionBlock(sets = 3, reps = "5"))))

    @Test
    fun consentDeclineNeverStartsTheBackendAndAcceptPreservesThePhoto() = runBlocking {
        var calls = 0
        var approved: ConsentRequest? = null
        var image: String? = null
        val backend = object : ProgramScanBackend {
            override suspend fun consentRequired() = request
            override suspend fun acceptConsent(request: ConsentRequest) { approved = request }
            override suspend fun scan(
                imageUri: String, exerciseName: String?, onPhase: (ScanPhase) -> Unit,
                onStreamUpdate: (String, String) -> Unit
            ): ScannedProgram {
                calls++
                image = imageUri
                assertEquals("Squat", exerciseName)
                return result
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val model = ProgramScanModel<Nothing>(backend, scope, Dispatchers.Unconfined)
            model.scan("/fixture.jpg", "Squat")
            assertEquals(request, model.consent.value)
            assertEquals(0, calls)
            model.declineConsent()
            assertNull(model.consent.value)
            assertEquals(0, calls)
            model.scan("/fixture.jpg", "Squat")
            model.acceptConsent()
            assertEquals(request, approved)
            assertEquals("/fixture.jpg", image)
            assertEquals(1, calls)
            assertEquals(result, assertIs<ProgramScanState.Result>(model.state.value).program)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun failedRequestCanBeRetriedAfterReset() = runBlocking {
        var calls = 0
        val backend = object : ProgramScanBackend {
            override suspend fun consentRequired(): ConsentRequest? = null
            override suspend fun acceptConsent(request: ConsentRequest) = Unit
            override suspend fun scan(
                imageUri: String, exerciseName: String?, onPhase: (ScanPhase) -> Unit,
                onStreamUpdate: (String, String) -> Unit
            ): ScannedProgram {
                if (++calls == 1) throw ScanBackendException.Unauthorized("synthetic error")
                return result
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val model = ProgramScanModel<Nothing>(backend, scope, Dispatchers.Unconfined)
            model.scan("/fixture.jpg", null)
            assertIs<ScanBackendException.Unauthorized>(assertIs<ProgramScanState.Failed>(model.state.value).failure)
            model.reset()
            model.scan("/fixture.jpg", null)
            assertEquals(result, assertIs<ProgramScanState.Result>(model.state.value).program)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun canceledRequestCannotReplaceTheNextResultWithALateFailure() = runBlocking {
        val releaseFirst = CompletableDeferred<Unit>()
        val firstExited = CompletableDeferred<Unit>()
        var calls = 0
        val backend = object : ProgramScanBackend {
            override suspend fun consentRequired(): ConsentRequest? = null
            override suspend fun acceptConsent(request: ConsentRequest) = Unit
            override suspend fun scan(
                imageUri: String, exerciseName: String?, onPhase: (ScanPhase) -> Unit,
                onStreamUpdate: (String, String) -> Unit
            ): ScannedProgram {
                if (++calls == 1) {
                    try {
                        awaitCancellation()
                    } finally {
                        withContext(NonCancellable) { releaseFirst.await() }
                        firstExited.complete(Unit)
                        throw ScanBackendException.StreamError("late transport failure")
                    }
                }
                return result
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val model = ProgramScanModel<Nothing>(backend, scope, Dispatchers.Unconfined)
            model.scan("/first.jpg", null)
            model.cancel()
            assertIs<ProgramScanState.Idle>(model.state.value)
            model.scan("/second.jpg", null)
            assertIs<ProgramScanState.Result>(model.state.value)
            releaseFirst.complete(Unit)
            firstExited.await()
            assertEquals(result, assertIs<ProgramScanState.Result>(model.state.value).program)
        } finally {
            releaseFirst.complete(Unit)
            scope.cancel()
        }
    }
}
