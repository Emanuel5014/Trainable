@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.emanuel5014.trainable.ui

import com.emanuel5014.trainable.data.remote.dto.TrainablePlanParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import kotlin.test.*

class IosRoutineFilesTest {
    private val plan = """{"nome":"Prova è 🏋️","note":null,"sessioniTargetSettimana":1,"imageUri":null,"exercises":[{"exerciseId":1,"serieTarget":3,"repsTarget":"8","recuperoTarget":60,"ordine":0}],"weeksCount":4,"currentWeek":3}"""
    private fun payload() = IosRoutinePayload(plan, TrainablePlanParser.decode(plan))
    private fun fixture(test: (String) -> Unit) {
        val path = NSTemporaryDirectory() + "routine-read-test-" + NSUUID().UUIDString + ".trainableplan"
        try { test(path) } finally { FileSystem.SYSTEM.delete(path.toPath(), mustExist = false) }
    }

    @Test fun readsCurrentAndLegacyAndroidFormatsWithoutChangingTheSource() = fixture { path ->
        for (json in listOf(plan, "[$plan]", """{"version":3,"plans":[$plan]}""")) {
            FileSystem.SYSTEM.write(path.toPath()) { writeUtf8(json) }
            val read = readIosRoutineFile(NSURL.fileURLWithPath(path))
            assertEquals(json, read.json)
            assertEquals("Prova è 🏋️", read.plans.single().nome)
            assertEquals(4, read.plans.single().weeksCount)
            assertEquals(json, FileSystem.SYSTEM.read(path.toPath()) { readUtf8() })
        }
    }

    @Test fun rejectsMalformedEmptyOversizedAndNonFileInputs() = fixture { path ->
        for (text in listOf("", "not JSON", "[]", """{"version":3,"plans":[]}""")) {
            FileSystem.SYSTEM.write(path.toPath()) { writeUtf8(text) }
            assertFails { readIosRoutineFile(NSURL.fileURLWithPath(path)) }
        }
        FileSystem.SYSTEM.write(path.toPath()) { writeUtf8(plan) }
        assertFails { readIosRoutineFile(NSURL.fileURLWithPath(path), limit = 32) }
        assertFails { readIosRoutineFile(NSURL(string = "https://example.invalid/test.trainableplan")) }
        FileSystem.SYSTEM.write(path.toPath()) { write(byteArrayOf(0xff.toByte(), 0xfe.toByte())) }
        assertFails { readIosRoutineFile(NSURL.fileURLWithPath(path)) }
    }

    @Test fun aPreviewNeverWritesBeforeConfirmationAndCanBeCancelled() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var writes = 0
        val model = IosRoutineImportModel(scope) { writes++; 1 }
        try {
            model.enqueue { payload() }
            val preview = withTimeout(5000) { model.requests.first { it.firstOrNull()?.state is IosRoutineImportState.Preview } }.first()
            assertEquals(0, writes)
            model.dismiss(preview.id)
            assertTrue(model.requests.value.isEmpty())
            model.confirm(preview.id)
            assertEquals(0, writes)
        } finally { scope.cancel() }
    }

    @Test fun repeatedConfirmationAndDismissalCannotDuplicateOrCancelAnImport() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Int>()
        var writes = 0
        val model = IosRoutineImportModel(scope) { writes++; started.complete(Unit); finish.await() }
        try {
            model.enqueue { payload() }
            val preview = withTimeout(5000) { model.requests.first { it.firstOrNull()?.state is IosRoutineImportState.Preview } }.first()
            model.confirm(preview.id); model.confirm(preview.id)
            withTimeout(5000) { started.await() }
            model.dismiss(preview.id)
            assertEquals(IosRoutineImportState.Importing, model.requests.value.single().state)
            finish.complete(1)
            val done = withTimeout(5000) { model.requests.first { it.firstOrNull()?.state is IosRoutineImportState.Finished } }.single()
            assertEquals(IosRoutineImportState.Finished(true), done.state)
            model.confirm(preview.id)
            assertEquals(1, writes)
        } finally { scope.cancel() }
    }

    @Test fun multipleFilesStayInOrderAndOnlyTheFirstCanBeConfirmed() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var writes = 0
        val model = IosRoutineImportModel(scope) { writes++; 1 }
        try {
            model.enqueue { payload() }; model.enqueue { payload() }
            val queue = withTimeout(5000) { model.requests.first { list -> list.size == 2 && list.all { it.state is IosRoutineImportState.Preview } } }
            model.confirm(queue[1].id); model.dismiss(queue[1].id)
            assertEquals(0, writes); assertEquals(2, model.requests.value.size)
            model.dismiss(queue[0].id)
            assertEquals(queue[1].id, model.requests.value.single().id)
            model.confirm(queue[1].id)
            withTimeout(5000) { model.requests.first { it.firstOrNull()?.state is IosRoutineImportState.Finished } }
            assertEquals(1, writes)
        } finally { scope.cancel() }
    }

    @Test fun readAndImportFailuresAreReportedWithoutRetriesOrUnrequestedWrites() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var writes = 0
        val model = IosRoutineImportModel(scope) { writes++; error("test failure") }
        try {
            model.enqueue { error("bad file") }
            val bad = withTimeout(5000) { model.requests.first { it.firstOrNull()?.state is IosRoutineImportState.Finished } }.single()
            assertEquals(IosRoutineImportState.Finished(false), bad.state); assertEquals(0, writes)
            model.dismiss(bad.id)
            model.enqueue { payload() }
            val preview = withTimeout(5000) { model.requests.first { it.firstOrNull()?.state is IosRoutineImportState.Preview } }.single()
            model.confirm(preview.id)
            val failed = withTimeout(5000) { model.requests.first { it.firstOrNull()?.state is IosRoutineImportState.Finished } }.single()
            assertEquals(IosRoutineImportState.Finished(false), failed.state); assertEquals(1, writes)
        } finally { scope.cancel() }
    }
}
