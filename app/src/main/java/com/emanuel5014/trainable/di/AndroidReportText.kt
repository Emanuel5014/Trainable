package com.emanuel5014.trainable.di

import android.content.Context
import com.emanuel5014.trainable.data.local.entity.SetLogEntity
import com.emanuel5014.trainable.data.report.ReportText
import com.emanuel5014.trainable.domain.prescription.PrescriptionBlock
import com.emanuel5014.trainable.domain.prescription.PrescriptionLabels
import com.emanuel5014.trainable.ui.components.prescriptionLabels
import com.emanuel5014.trainable.ui.components.programChangeLines
import com.emanuel5014.trainable.ui.components.setLogPerformanceBadges
import com.emanuel5014.trainable.ui.components.setLogPrescriptionBadges

/** The report's words from the Android resources of [context] (already set to the report's language). */
class AndroidReportText(private val context: Context) : ReportText {
    override fun labels(weightUnit: String): PrescriptionLabels = prescriptionLabels(context, weightUnit)

    override fun programChangeLines(weightUnit: String, before: List<PrescriptionBlock>, after: List<PrescriptionBlock>) =
        programChangeLines(context, weightUnit, before, after)

    override fun setPrescriptionBadges(set: SetLogEntity): List<String> = setLogPrescriptionBadges(context, set)

    override fun setPerformanceBadges(set: SetLogEntity): List<String> = setLogPerformanceBadges(context, set)
}
