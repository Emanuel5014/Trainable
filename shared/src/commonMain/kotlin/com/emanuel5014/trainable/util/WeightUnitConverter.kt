package com.emanuel5014.trainable.util

import com.emanuel5014.trainable.platform.formatDecimals

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
            rounded100 % 100 == 0 -> formatDecimals(weight.toDouble(), 0, deviceLocale = true)
            rounded100 % 10 == 0 -> formatDecimals(weight.toDouble(), 1, deviceLocale = true)
            else -> formatDecimals(weight.toDouble(), 2, deviceLocale = true)
        }
    }
    
    fun formatWithUnit(weight: Float, unit: String): String {
        return "${format(weight)} $unit"
    }
}
