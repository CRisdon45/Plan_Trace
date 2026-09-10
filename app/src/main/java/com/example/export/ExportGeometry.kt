package com.example.export

import android.graphics.Paint
import android.graphics.RectF
import com.example.model.*
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

/** One coordinate system for the underlay, marks, and all output formats. */
object ExportGeometry {
    fun contentBounds(project: TraceProject, backgroundWidth: Int?, backgroundHeight: Int?, registeredBackground: RectF? = null): RectF {
        var bounds: RectF? = if (registeredBackground != null) RectF(registeredBackground) else if (backgroundWidth != null && backgroundHeight != null) {
            RectF(0f, 0f, backgroundWidth.toFloat(), backgroundHeight.toFloat())
        } else null
        val visibleLayers = project.layers.filter { it.isVisible }.map { it.id }.toSet()
        for (element in project.elements.filter { it.layerId in visibleLayers }) {
            val box = element.boundingBox()
            // Reserve stroke overshoot and annotation clearance, including zero-height lines.
            val pad = maxOf(32f, element.strokeWidth * 3f)
            box.inset(-pad, -pad)
            if (listOf(box.left, box.top, box.right, box.bottom).any { !it.isFinite() }) continue
            if (bounds == null) bounds = RectF(box) else bounds.union(box)
        }
        return bounds ?: RectF(0f, 0f, 2048f, 1536f)
    }

    data class Fit(val scale: Float, val translateX: Float, val translateY: Float)

    fun fit(bounds: RectF, target: RectF): Fit {
        require(bounds.width() > 0 && bounds.height() > 0 && target.width() > 0 && target.height() > 0)
        val scale = min(target.width() / bounds.width(), target.height() / bounds.height())
        return Fit(scale,
            target.centerX() - bounds.centerX() * scale,
            target.centerY() - bounds.centerY() * scale)
    }

    data class ScaleBar(val segmentUnits: Float, val segmentPoints: Float)

    fun scaleBar(calibration: ScaleCalibration, pointsPerWorldPixel: Float, maxWidth: Float): ScaleBar? {
        if (!calibration.isCalibrated || calibration.pixelDistance <= 0 || calibration.realWorldUnits <= 0) return null
        val pointsPerUnit = pointsPerWorldPixel * calibration.pixelsPerUnit
        if (!pointsPerUnit.isFinite() || pointsPerUnit <= 0 || !maxWidth.isFinite() || maxWidth <= 0) return null
        val maxSegmentUnits = maxWidth / 4f / pointsPerUnit
        if (!maxSegmentUnits.isFinite() || maxSegmentUnits <= 0f) return null
        val magnitude = 10.0.pow(floor(log10(maxSegmentUnits.toDouble()))).toFloat()
        val units = listOf(1f, 2f, 5f, 10f).map { it * magnitude }.last { it <= maxSegmentUnits }
        return ScaleBar(units, units * pointsPerUnit)
    }
}
