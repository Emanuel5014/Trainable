package com.example.gymtracking.util

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
        return String.format(Locale.getDefault(), "%.1f", weight)
    }
    
    fun formatWithUnit(weight: Float, unit: String): String {
        return "${format(weight)} $unit"
    }
}
