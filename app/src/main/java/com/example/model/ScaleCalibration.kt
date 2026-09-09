package com.example.model

import java.util.Locale
import kotlin.math.roundToInt

data class ScaleCalibration(
    val isCalibrated: Boolean = false,
    val pixelDistance: Float = 100f,
    val realWorldUnits: Float = 10f, // in default unit
    val unit: String = "ft" // "ft", "m", "in"
) {
    val pixelsPerUnit: Float
        get() = if (realWorldUnits > 0f) pixelDistance / realWorldUnits else 1f

    fun calculateWorldUnits(pixels: Float): Float {
        val ppu = pixelsPerUnit
        return if (ppu > 0f) pixels / ppu else 0f
    }

    /**
     * Formats pixels into human architectural format like 31'-7" or 8.4m
     */
    fun formatMeasurement(pixels: Float): String {
        if (!isCalibrated || pixels <= 0f) {
            return "${pixels.roundToInt()} px"
        }
        val worldVal = calculateWorldUnits(pixels)
        return when (unit.lowercase(Locale.ROOT)) {
            "ft", "feet", "'" -> {
                val totalInches = (worldVal * 12f).roundToInt()
                val feet = totalInches / 12
                val inches = totalInches % 12
                if (inches == 0) "$feet'" else "$feet'-$inches\""
            }
            "m", "meter", "meters" -> {
                String.format(Locale.US, "%.2f m", worldVal)
            }
            "in", "inch", "inches" -> {
                String.format(Locale.US, "%.1f in", worldVal)
            }
            else -> {
                String.format(Locale.US, "%.1f %s", worldVal, unit)
            }
        }
    }

    companion object {
        fun parseInputToUnits(input: String, defaultUnit: String = "ft"): Pair<Float, String> {
            val clean = input.trim()
            // Check for feet and inches format: e.g. 20', 20'6", 20ft, 10m
            val feetInchRegex = Regex("""^(\d+(?:\.\d+)?)\s*(?:'|ft|feet)?(?:\s*(\d+(?:\.\d+)?)\s*(?:"|in|inches)?)?$""")
            val match = feetInchRegex.matchEntire(clean)
            if (match != null) {
                val feetPart = match.groupValues[1].toFloatOrNull() ?: 0f
                val inchPart = if (match.groupValues.size > 2 && match.groupValues[2].isNotEmpty()) {
                    match.groupValues[2].toFloatOrNull() ?: 0f
                } else 0f
                val totalFeet = feetPart + (inchPart / 12f)
                return Pair(if (totalFeet > 0f) totalFeet else 10f, "ft")
            }

            // Check metric: e.g. 6.5m or 6.5 m
            val metricRegex = Regex("""^(\d+(?:\.\d+)?)\s*(m|meter|meters|cm|mm)$""", RegexOption.IGNORE_CASE)
            val metricMatch = metricRegex.matchEntire(clean)
            if (metricMatch != null) {
                val value = metricMatch.groupValues[1].toFloatOrNull() ?: 5f
                val u = metricMatch.groupValues[2].lowercase(Locale.ROOT)
                return Pair(value, if (u.startsWith("m")) "m" else u)
            }

            // Simple float
            val simpleVal = clean.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 10f
            return Pair(simpleVal, defaultUnit)
        }
    }
}
