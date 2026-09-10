package com.example.model

import android.graphics.RectF
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

sealed interface VectorElement {
    val id: String
    val layerId: String
    val strokeColor: Long
    val strokeWidth: Float
    val style: StrokeStyle
    val alpha: Float
    val material: SurfaceMaterial?

    fun distanceToPoint(point: Point2D): Float
    fun isPointInside(point: Point2D): Boolean = distanceToPoint(point) <= 24f
    fun boundingBox(): RectF
    fun translate(dx: Float, dy: Float): VectorElement
    fun withStrokeColor(color: Long): VectorElement
    fun withFillColor(color: Long?): VectorElement = this
    fun getMeasurementLabel(scale: ScaleCalibration): String? = null
}

data class FreehandPath(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String,
    val points: List<Point2D>,
    override val strokeColor: Long = 0xFF1E293B,
    override val strokeWidth: Float = 3f,
    override val style: StrokeStyle = StrokeStyle.INK,
    override val alpha: Float = 1f,
    override val material: SurfaceMaterial? = null,
    val isClosed: Boolean = false,
    val fillColor: Long? = null
) : VectorElement {
    override fun distanceToPoint(point: Point2D): Float {
        if (points.isEmpty()) return Float.MAX_VALUE
        var minDistance = Float.MAX_VALUE
        for (i in 0 until points.size - 1) {
            val dist = pointToSegmentDistance(point, points[i], points[i + 1])
            if (dist < minDistance) minDistance = dist
        }
        if (isClosed && points.size > 2) {
            val dist = pointToSegmentDistance(point, points.last(), points.first())
            if (dist < minDistance) minDistance = dist
        }
        return minDistance
    }

    override fun isPointInside(point: Point2D): Boolean {
        if (!isClosed || points.size < 3) return distanceToPoint(point) <= 24f
        // Ray casting algorithm for polygon inside test
        var inside = false
        var j = points.size - 1
        for (i in points.indices) {
            val pi = points[i]
            val pj = points[j]
            if ((pi.y > point.y) != (pj.y > point.y) &&
                point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x
            ) {
                inside = !inside
            }
            j = i
        }
        return inside || distanceToPoint(point) <= 24f
    }

    override fun boundingBox(): RectF {
        if (points.isEmpty()) return RectF(0f, 0f, 0f, 0f)
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        return RectF(minX, minY, maxX, maxY)
    }

    override fun translate(dx: Float, dy: Float): VectorElement {
        return copy(points = points.map { Point2D(it.x + dx, it.y + dy) })
    }

    override fun withStrokeColor(color: Long): VectorElement = copy(strokeColor = color)
    override fun withFillColor(color: Long?): VectorElement = copy(fillColor = color, isClosed = color != null || isClosed)

    override fun getMeasurementLabel(scale: ScaleCalibration): String? {
        if (!scale.isCalibrated || points.size < 2) return null
        var totalLen = 0f
        for (i in 0 until points.size - 1) {
            totalLen += points[i].distanceTo(points[i + 1])
        }
        // Match the implicit closing segment used by rendering and hit testing.
        // An explicitly repeated first point contributes zero, so it is not doubled.
        if (isClosed && points.size > 2) totalLen += points.last().distanceTo(points.first())
        return scale.formatMeasurement(totalLen)
    }
}

data class LineElement(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String,
    val start: Point2D,
    val end: Point2D,
    override val strokeColor: Long = 0xFF1E293B,
    override val strokeWidth: Float = 3f,
    override val style: StrokeStyle = StrokeStyle.INK,
    override val alpha: Float = 1f,
    override val material: SurfaceMaterial? = null,
    val showDimension: Boolean = true
) : VectorElement {
    override fun distanceToPoint(point: Point2D): Float =
        pointToSegmentDistance(point, start, end)

    override fun boundingBox(): RectF = RectF(
        min(start.x, end.x), min(start.y, end.y),
        max(start.x, end.x), max(start.y, end.y)
    )

    override fun translate(dx: Float, dy: Float): VectorElement = copy(
        start = Point2D(start.x + dx, start.y + dy),
        end = Point2D(end.x + dx, end.y + dy)
    )

    override fun withStrokeColor(color: Long): VectorElement = copy(strokeColor = color)

    override fun getMeasurementLabel(scale: ScaleCalibration): String? {
        if (!scale.isCalibrated) return null
        val length = start.distanceTo(end)
        return scale.formatMeasurement(length)
    }
}

data class PolylineElement(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String,
    val points: List<Point2D>,
    val isClosed: Boolean = false,
    override val strokeColor: Long = 0xFF1E293B,
    override val strokeWidth: Float = 3f,
    override val style: StrokeStyle = StrokeStyle.INK,
    override val alpha: Float = 1f,
    override val material: SurfaceMaterial? = null,
    val fillColor: Long? = null
) : VectorElement {
    override fun distanceToPoint(point: Point2D): Float {
        if (points.isEmpty()) return Float.MAX_VALUE
        var minDistance = Float.MAX_VALUE
        for (i in 0 until points.size - 1) {
            val dist = pointToSegmentDistance(point, points[i], points[i + 1])
            if (dist < minDistance) minDistance = dist
        }
        if (isClosed && points.size > 2) {
            val dist = pointToSegmentDistance(point, points.last(), points.first())
            if (dist < minDistance) minDistance = dist
        }
        return minDistance
    }

    override fun boundingBox(): RectF {
        if (points.isEmpty()) return RectF(0f, 0f, 0f, 0f)
        return RectF(
            points.minOf { it.x }, points.minOf { it.y },
            points.maxOf { it.x }, points.maxOf { it.y }
        )
    }

    override fun translate(dx: Float, dy: Float): VectorElement =
        copy(points = points.map { Point2D(it.x + dx, it.y + dy) })

    override fun withStrokeColor(color: Long): VectorElement = copy(strokeColor = color)
    override fun withFillColor(color: Long?): VectorElement = copy(fillColor = color, isClosed = color != null || isClosed)

    override fun getMeasurementLabel(scale: ScaleCalibration): String? {
        if (!scale.isCalibrated || points.size < 2) return null
        var total = 0f
        for (i in 0 until points.size - 1) {
            total += points[i].distanceTo(points[i + 1])
        }
        // Closing a path adds an edge without duplicating its first stored vertex.
        if (isClosed && points.size > 2) total += points.last().distanceTo(points.first())
        return scale.formatMeasurement(total)
    }
}

data class RectangleElement(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    override val strokeColor: Long = 0xFF1E293B,
    override val strokeWidth: Float = 3f,
    override val style: StrokeStyle = StrokeStyle.INK,
    override val alpha: Float = 1f,
    override val material: SurfaceMaterial? = null,
    val isFilled: Boolean = false,
    val fillColor: Long = 0x3338BDF8 // Default pool azure wash
) : VectorElement {
    val width: Float get() = abs(right - left)
    val height: Float get() = abs(bottom - top)

    override fun distanceToPoint(point: Point2D): Float {
        val minX = min(left, right)
        val maxX = max(left, right)
        val minY = min(top, bottom)
        val maxY = max(top, bottom)

        val p1 = Point2D(minX, minY)
        val p2 = Point2D(maxX, minY)
        val p3 = Point2D(maxX, maxY)
        val p4 = Point2D(minX, maxY)

        val d1 = pointToSegmentDistance(point, p1, p2)
        val d2 = pointToSegmentDistance(point, p2, p3)
        val d3 = pointToSegmentDistance(point, p3, p4)
        val d4 = pointToSegmentDistance(point, p4, p1)

        return minOf(d1, d2, d3, d4)
    }

    override fun isPointInside(point: Point2D): Boolean {
        val minX = min(left, right)
        val maxX = max(left, right)
        val minY = min(top, bottom)
        val maxY = max(top, bottom)
        return (point.x in minX..maxX && point.y in minY..maxY) || distanceToPoint(point) <= 24f
    }

    override fun boundingBox(): RectF = RectF(min(left, right), min(top, bottom), max(left, right), max(top, bottom))

    override fun translate(dx: Float, dy: Float): VectorElement = copy(
        left = left + dx, right = right + dx,
        top = top + dy, bottom = bottom + dy
    )

    override fun withStrokeColor(color: Long): VectorElement = copy(strokeColor = color)
    override fun withFillColor(color: Long?): VectorElement = copy(
        isFilled = color != null,
        fillColor = color ?: this.fillColor
    )

    override fun getMeasurementLabel(scale: ScaleCalibration): String? {
        if (!scale.isCalibrated) return null
        val wStr = scale.formatMeasurement(width)
        val hStr = scale.formatMeasurement(height)
        return "$wStr × $hStr"
    }
}

data class EllipseElement(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String,
    val centerX: Float,
    val centerY: Float,
    val radiusX: Float,
    val radiusY: Float,
    override val strokeColor: Long = 0xFF1E293B,
    override val strokeWidth: Float = 3f,
    override val style: StrokeStyle = StrokeStyle.INK,
    override val alpha: Float = 1f,
    override val material: SurfaceMaterial? = null,
    val isFilled: Boolean = false,
    val fillColor: Long = 0x3338BDF8 // Default pool azure wash
) : VectorElement {
    override fun distanceToPoint(point: Point2D): Float {
        val dx = point.x - centerX
        val dy = point.y - centerY
        val rAvg = (radiusX + radiusY) / 2f
        val currentR = hypot(dx, dy)
        return abs(currentR - rAvg)
    }

    override fun isPointInside(point: Point2D): Boolean {
        if (radiusX <= 0f || radiusY <= 0f) return false
        val norm = hypot((point.x - centerX) / radiusX, (point.y - centerY) / radiusY)
        return norm <= 1.0f || distanceToPoint(point) <= 24f
    }

    override fun boundingBox(): RectF = RectF(
        centerX - radiusX, centerY - radiusY,
        centerX + radiusX, centerY + radiusY
    )

    override fun translate(dx: Float, dy: Float): VectorElement = copy(
        centerX = centerX + dx, centerY = centerY + dy
    )

    override fun withStrokeColor(color: Long): VectorElement = copy(strokeColor = color)
    override fun withFillColor(color: Long?): VectorElement = copy(
        isFilled = color != null,
        fillColor = color ?: this.fillColor
    )

    override fun getMeasurementLabel(scale: ScaleCalibration): String? {
        if (!scale.isCalibrated) return null
        return if (abs(radiusX - radiusY) < 5f) {
            "Ø " + scale.formatMeasurement(radiusX * 2f)
        } else {
            scale.formatMeasurement(radiusX * 2f) + " × " + scale.formatMeasurement(radiusY * 2f)
        }
    }
}

data class TextElement(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String,
    val text: String,
    val position: Point2D,
    val fontSizeSp: Float = 14f,
    override val strokeColor: Long = 0xFF1E293B,
    override val strokeWidth: Float = 1f,
    override val style: StrokeStyle = StrokeStyle.INK,
    override val alpha: Float = 1f,
    override val material: SurfaceMaterial? = null
) : VectorElement {
    override fun distanceToPoint(point: Point2D): Float = point.distanceTo(position)
    override fun boundingBox(): RectF {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSizeSp * 2.2f
            isFakeBoldText = true
        }
        val lines = text.lines()
        return RectF(position.x, position.y + paint.fontMetrics.top,
            position.x + (lines.maxOfOrNull { paint.measureText(it) } ?: 0f),
            position.y + paint.fontSpacing * (lines.size - 1) + paint.fontMetrics.bottom)
    }
    override fun isPointInside(point: Point2D): Boolean = boundingBox().contains(point.x, point.y)
    override fun translate(dx: Float, dy: Float): VectorElement = copy(
        position = Point2D(position.x + dx, position.y + dy)
    )
    override fun withStrokeColor(color: Long): VectorElement = copy(strokeColor = color)
}

data class DimensionMarkup(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String,
    val start: Point2D,
    val end: Point2D,
    val label: String,
    override val strokeColor: Long = 0xFFDC2626, // Distinct red/crimson dimensioning
    override val strokeWidth: Float = 2f,
    override val style: StrokeStyle = StrokeStyle.INK,
    override val alpha: Float = 1f,
    override val material: SurfaceMaterial? = null
) : VectorElement {
    override fun distanceToPoint(point: Point2D): Float =
        pointToSegmentDistance(point, start, end)

    override fun boundingBox(): RectF = RectF(
        min(start.x, end.x), min(start.y, end.y),
        max(start.x, end.x), max(start.y, end.y)
    )

    override fun translate(dx: Float, dy: Float): VectorElement = copy(
        start = Point2D(start.x + dx, start.y + dy),
        end = Point2D(end.x + dx, end.y + dy)
    )

    override fun withStrokeColor(color: Long): VectorElement = copy(strokeColor = color)
}

fun pointToSegmentDistance(p: Point2D, a: Point2D, b: Point2D): Float {
    val l2 = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
    if (l2 == 0f) return p.distanceTo(a)
    var t = ((p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)) / l2
    t = max(0f, min(1f, t))
    val projection = Point2D(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
    return p.distanceTo(projection)
}
