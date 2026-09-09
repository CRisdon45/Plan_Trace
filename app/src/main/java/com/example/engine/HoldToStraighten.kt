package com.example.engine

import com.example.model.Point2D
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

sealed interface StraightenedResult {
    data class Line(val start: Point2D, val end: Point2D) : StraightenedResult
    data class CircleOrEllipse(
        val centerX: Float,
        val centerY: Float,
        val radiusX: Float,
        val radiusY: Float
    ) : StraightenedResult
    data class Rectangle(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    ) : StraightenedResult
}

object HoldToStraightenDetector {

    /**
     * Attempts to recognize whether a drawn list of points resembles a clean geometric primitive:
     * - Straight Line
     * - Circle / Ellipse
     * - Rectangle
     */
    fun analyze(points: List<Point2D>): StraightenedResult? {
        if (points.size < 5) return null

        val start = points.first()
        val end = points.last()
        val chordLength = start.distanceTo(end)

        // Calculate total perimeter length
        var totalPerimeter = 0f
        for (i in 0 until points.size - 1) {
            totalPerimeter += points[i].distanceTo(points[i + 1])
        }
        if (totalPerimeter < 25f) return null

        val isClosed = (chordLength / totalPerimeter < 0.28f) || chordLength < 45f

        if (isClosed && points.size >= 8) {
            // Check for Circle / Ellipse or Rectangle
            val circleResult = checkCircleOrEllipse(points)
            if (circleResult != null) return circleResult

            val rectResult = checkRectangle(points)
            if (rectResult != null) return rectResult
        }

        // Check for Straight Line
        val lineResult = checkStraightLine(points, start, end, chordLength)
        if (lineResult != null) return lineResult

        return null
    }

    private fun checkStraightLine(
        points: List<Point2D>,
        start: Point2D,
        end: Point2D,
        chordLength: Float
    ): StraightenedResult.Line? {
        if (chordLength < 30f) return null

        var maxPerpDist = 0f
        val dx = end.x - start.x
        val dy = end.y - start.y

        for (p in points) {
            // Distance from point p to line start->end
            val numerator = abs(dy * p.x - dx * p.y + end.x * start.y - end.y * start.x)
            val dist = numerator / chordLength
            if (dist > maxPerpDist) maxPerpDist = dist
        }

        // Allow up to 14% deviation relative to line length, or up to 25px
        val tolerance = max(24f, chordLength * 0.14f)
        if (maxPerpDist <= tolerance) {
            return StraightenedResult.Line(start, end)
        }
        return null
    }

    private fun checkCircleOrEllipse(points: List<Point2D>): StraightenedResult.CircleOrEllipse? {
        // Compute centroid
        var sumX = 0f
        var sumY = 0f
        for (p in points) {
            sumX += p.x
            sumY += p.y
        }
        val centerX = sumX / points.size
        val centerY = sumY / points.size

        // Calculate distances to centroid
        val radii = points.map { hypot(it.x - centerX, it.y - centerY) }
        val avgRadius = radii.average().toFloat()
        if (avgRadius < 15f) return null

        var varianceSum = 0f
        for (r in radii) {
            varianceSum += (r - avgRadius) * (r - avgRadius)
        }
        val stdDev = sqrt(varianceSum / radii.size)

        // If radius is relatively uniform, it's a circle
        if (stdDev / avgRadius < 0.25f) {
            return StraightenedResult.CircleOrEllipse(centerX, centerY, avgRadius, avgRadius)
        }

        // Check ellipse: bounds
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val rx = (maxX - minX) / 2f
        val ry = (maxY - minY) / 2f

        if (rx > 15f && ry > 15f) {
            // Check normalized ellipse distance
            var ellipseDevSum = 0f
            for (p in points) {
                val norm = hypot((p.x - centerX) / rx, (p.y - centerY) / ry)
                ellipseDevSum += abs(norm - 1.0f)
            }
            val avgEllipseDev = ellipseDevSum / points.size
            if (avgEllipseDev < 0.26f) {
                return StraightenedResult.CircleOrEllipse(centerX, centerY, rx, ry)
            }
        }

        return null
    }

    private fun checkRectangle(points: List<Point2D>): StraightenedResult.Rectangle? {
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }

        val width = maxX - minX
        val height = maxY - minY
        if (width < 25f || height < 25f) return null

        // Count how many points are close to the 4 sides
        val tolerance = max(20f, min(width, height) * 0.18f)
        var nearEdgeCount = 0
        for (p in points) {
            val distLeft = abs(p.x - minX)
            val distRight = abs(p.x - maxX)
            val distTop = abs(p.y - minY)
            val distBottom = abs(p.y - maxY)
            val minDistToEdge = minOf(distLeft, distRight, distTop, distBottom)
            if (minDistToEdge <= tolerance) {
                nearEdgeCount++
            }
        }

        val ratio = nearEdgeCount.toFloat() / points.size
        if (ratio >= 0.75f) {
            return StraightenedResult.Rectangle(minX, minY, maxX, maxY)
        }
        return null
    }
}
