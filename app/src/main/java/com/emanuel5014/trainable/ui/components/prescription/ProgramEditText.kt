package com.emanuel5014.trainable.ui.components

import com.emanuel5014.trainable.R
import com.emanuel5014.trainable.domain.prescription.BlockChange
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionFormatter
import com.emanuel5014.trainable.domain.prescription.ProgramDiff

/** What a set of blocks says as one line, in the user's units and language ("70% 1×6 · 75% 4×5 Pause 2″"). */
fun programSummary(context: android.content.Context, weightUnit: String, blocks: List<PrescriptionBlock>): String =
    if (blocks.isEmpty()) "-" else PrescriptionFormatter.summary(blocks, prescriptionLabels(context, weightUnit))

/**
 * What an edit changed, one line per block: "Block 2: 75% 3×3 Pause 1″ → 72.5% 3×3 Pause 1″", or the block that was
 * added or taken out. Empty when only something the blocks do not show (a note, a rest) changed.
 */
fun programChangeLines(context: android.content.Context, weightUnit: String, before: List<PrescriptionBlock>, after: List<PrescriptionBlock>): List<String> {
    val labels = prescriptionLabels(context, weightUnit)
    fun text(block: PrescriptionBlock) = PrescriptionFormatter.summary(listOf(block), labels)
    return ProgramDiff.changes(before, after).map { change ->
        when (change.kind) {
            BlockChange.Kind.CHANGED -> context.getString(R.string.load_preview_line, change.blockNumber, text(change.before!!), text(change.after!!))
            BlockChange.Kind.ADDED -> context.getString(R.string.program_edit_block_added, change.blockNumber, text(change.after!!))
            BlockChange.Kind.REMOVED -> context.getString(R.string.program_edit_block_removed, change.blockNumber, text(change.before!!))
        }
    }
}
