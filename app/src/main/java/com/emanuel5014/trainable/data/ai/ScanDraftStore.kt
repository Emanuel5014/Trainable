package com.emanuel5014.trainable.data.ai

import android.content.Context
import android.net.Uri
import com.emanuel5014.trainable.util.ImageStorageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanDraftStore @Inject constructor(
    @ApplicationContext private val context: Context
) : RoutineScanDraftStore {
    private val directory: File get() = File(context.filesDir, "scan_drafts").apply { mkdirs() }
    private fun jsonFile(planId: Int) = File(directory, "plan_$planId.json")
    private fun imageFile(planId: Int) = File(directory, "plan_$planId.jpg")

    override fun create(planId: Int, entries: List<ScannedExerciseEntry>, sourceImage: String): String? {
        val target = imageFile(planId)
        target.delete()
        ImageStorageUtils.readAndCompressImage(context, Uri.parse(sourceImage))?.let { target.writeBytes(it) }
        save(planId, entries)
        return target.takeIf { it.exists() }?.let { Uri.fromFile(it).toString() }
    }

    override fun save(planId: Int, entries: List<ScannedExerciseEntry>) {
        val content = ScanDraftCodec.encode(entries, System.currentTimeMillis())
        runCatching { jsonFile(planId).writeText(content) }
    }

    override fun load(planId: Int): ScanDraft? {
        val file = jsonFile(planId)
        if (!file.exists()) return null
        return runCatching {
            val imageUri = imageFile(planId).takeIf { it.exists() }?.let { Uri.fromFile(it).toString() }
            ScanDraftCodec.decode(file.readText(), imageUri)
        }.getOrNull()
    }

    override fun clear(planId: Int) {
        jsonFile(planId).delete()
        imageFile(planId).delete()
    }
}
