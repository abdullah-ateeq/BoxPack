package com.example.boxpack.model

import java.util.Locale

/**
 * Length unit system for user input and display.
 * All internal values are stored in millimeters (Double).
 */
enum class LengthUnit(
    val label: String,
    val symbol: String,
    val mmFactor: Double // 1 unit = mmFactor millimeters
) {
    MILLIMETERS("Millimeters", "mm", 1.0),
    CENTIMETERS("Centimeters", "cm", 10.0),
    METERS("Meters", "m", 1000.0),
    INCHES("Inches", "in", 25.4),
    FEET("Feet", "ft", 304.8);

    /**
     * Converts value in this unit to millimeters.
     */
    fun toMm(value: Double): Double = value * mmFactor

    /**
     * Converts value from millimeters to this unit.
     */
    fun fromMm(mm: Double): Double = mm / mmFactor

    /**
     * Format a millimeter dimension into this unit with the specified decimal places.
     */
    fun formatMm(mm: Double, decimals: Int = 2): String {
        val converted = fromMm(mm)
        return String.format(Locale.US, "%.${decimals}f %s", converted, symbol)
    }

    /**
     * Format without symbol.
     */
    fun formatValue(mm: Double, decimals: Int = 2): String {
        val converted = fromMm(mm)
        return String.format(Locale.US, "%.${decimals}f", converted)
    }

    companion object {
        fun formatVolume(mm3: Double, unit: LengthUnit, decimals: Int = 2): String {
            return when (unit) {
                MILLIMETERS -> {
                    val cm3 = mm3 / 1000.0
                    String.format(Locale.US, "%.${decimals}f cm³", cm3)
                }
                CENTIMETERS, METERS -> {
                    // Liters: 1 Liter = 1,000,000 mm³ = 1,000 cm³
                    val liters = mm3 / 1_000_000.0
                    if (liters >= 1000.0) {
                        val m3 = liters / 1000.0
                        String.format(Locale.US, "%.${decimals}f m³ (%.${decimals}f L)", m3, liters)
                    } else {
                        String.format(Locale.US, "%.${decimals}f L", liters)
                    }
                }
                INCHES, FEET -> {
                    // Cubic feet: 1 ft³ = 28,316,846.592 mm³
                    val cuFt = mm3 / 28_316_846.592
                    val cuIn = mm3 / 16_387.064
                    if (cuFt >= 1.0) {
                        String.format(Locale.US, "%.${decimals}f ft³", cuFt)
                    } else {
                        String.format(Locale.US, "%.${decimals}f in³", cuIn)
                    }
                }
            }
        }
    }
}
