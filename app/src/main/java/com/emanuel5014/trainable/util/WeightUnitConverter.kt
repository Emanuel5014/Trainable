package com.emanuel5014.trainable.util

import java.util.Locale

object WeightUnitConverter {
    private const val KG_TO_LB = 2.20462f

    fun kgToLb(kg: Float): Float = kg * KG_TO_LB
    
    fun lbToKg(lb: Float): Float = lb / KG_TO_LB

    fun convertDisplay(weight: Float, toUnit: String): Float {
        return if (toUnit == "lb") kgToLb(weight) else weight
    }

    fun convertStorage(weight: Float, fromUnit: String): Float {
        return if (fromUnit == "lb") lbToKg(weight) else weight
    }

    fun format(weight: Float): String {
        val rounded100 = kotlin.math.round(weight * 100).toInt()
        return when {
            rounded100 % 100 == 0 -> String.format(Locale.getDefault(), "%.0f", weight)
            rounded100 % 10 == 0 -> String.format(Locale.getDefault(), "%.1f", weight)
            else -> String.format(Locale.getDefault(), "%.2f", weight)
        }
    }
    
    fun formatWithUnit(weight: Float, unit: String): String {
        return "${format(weight)} $unit"
    }
}
