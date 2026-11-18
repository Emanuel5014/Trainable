package com.emanuel5014.trainable.data.report

import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels

/** The words of prescriptions and logged sets in the report's language, given by each platform. */
interface ReportText {
    fun labels(weightUnit: String): PrescriptionLabels

    /** What a program edit changed, one line per block ("Block 2: 75% 3×3 → 72.5% 3×3"); empty when no block changed. */
    fun programChangeLines(weightUnit: String, before: List<PrescriptionBlock>, after: List<PrescriptionBlock>): List<String>

    /** The prescription a logged set was done against ("75%", "MAX", "Pause 2″"). */
    fun setPrescriptionBadges(set: SetLogEntity): List<String>

    /** How a logged set really went (mini-sets, help, partials, the technique as done). */
    fun setPerformanceBadges(set: SetLogEntity): List<String>
}
