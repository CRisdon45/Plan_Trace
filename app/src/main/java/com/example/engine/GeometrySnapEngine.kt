package com.example.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.model.DimensionMarkup
import com.example.model.EllipseElement
import com.example.model.FreehandPath
import com.example.model.LineElement
import com.example.model.Point2D
import com.example.model.PolylineElement
import com.example.model.RectangleElement
import com.example.model.VectorElement
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

enum class SnapType(val label: String, val colorInt: Int) {
    ENDPOINT("Endpoint", 0xFF10B981.toInt()),       // Emerald Green
    MIDPOINT("Midpoint", 0xFF06B6D4.toInt()),       // Cyan
    CENTER("Center", 0xFFF59E0B.toInt()),           // Amber
    GRID_INTERSECTION("Grid", 0xFF8B5CF6.toInt()),  // Purple
    ORTHO_HORIZONTAL("Ortho H", 0xFF3B82F6.toInt()),// Blue
    ORTHO_VERTICAL("Ortho V", 0xFF3B82F6.toInt())   // Blue
}

data class SnapResult(
    val snappedPoint: Point2D,
    val type: SnapType,
    val sourcePoint: Point2D,
    val distancePx: Float
)

data class SnapSettings(
    val snapEnabled: Boolean = true,
    val snapToEndpoints: Boolean = true,
    val snapToMidpoints: Boolean = true,
    val snapToCenters: Boolean = true,
    val snapToGrid: Boolean = true,
    val snapOrthogonal: Boolean = true,
    val gridSpacingPx: Float = 50f
)

object GeometrySnapEngine {

    /**
     * Evaluates candidates from visible vector elements and grid,
     * returning the highest-priority snap match within screen-space threshold.
     */
    fun findBestSnap(
        rawPoint: Point2D,
        elements: List<VectorElement>,
        visibleLayerIds: Set<String>,
        zoomScale: Float,
        settings: SnapSettings = SnapSettings(),
        strokeStart: Point2D? = null,
        snapThresholdScreenPx: Float = 26f
    ): SnapResult? {
        if (!settings.snapEnabled) return null

        val thresholdWorld = snapThresholdScreenPx / zoomScale
        val thresholdWorldSq = thresholdWorld * thresholdWorld

        val visibleElements = elements.filter { it.layerId in visibleLayerIds }

        // 1. ENDPOINTS (Highest priority)
        if (settings.snapToEndpoints) {
            var bestEndpoint: Point2D? = null
            var bestDistSq = Float.MAX_VALUE

            for (el in visibleElements) {
                when (el) {
                    is LineElement -> {
                        checkCandidate(rawPoint, el.start, thresholdWorldSq)?.let {
                            if (it < bestDistSq) { bestDistSq = it; bestEndpoint = el.start }
                        }
                        checkCandidate(rawPoint, el.end, thresholdWorldSq)?.let {
                            if (it < bestDistSq) { bestDistSq = it; bestEndpoint = el.end }
                        }
                    }
                    is RectangleElement -> {
                        val corners = listOf(
                            Point2D(min(el.left, el.right), min(el.top, el.bottom)),
                            Point2D(max(el.left, el.right), min(el.top, el.bottom)),
                            Point2D(max(el.left, el.right), max(el.top, el.bottom)),
                            Point2D(min(el.left, el.right), max(el.top, el.bottom))
                        )
                        for (c in corners) {
                            checkCandidate(rawPoint, c, thresholdWorldSq)?.let {
                                if (it < bestDistSq) { bestDistSq = it; bestEndpoint = c }
                            }
                        }
                    }
                    is PolylineElement -> {
                        for (p in el.points) {
                            checkCandidate(rawPoint, p, thresholdWorldSq)?.let {
                                if (it < bestDistSq) { bestDistSq = it; bestEndpoint = p }
                            }
                        }
                    }
                    is FreehandPath -> {
                        el.points.firstOrNull()?.let { p ->
                            checkCandidate(rawPoint, p, thresholdWorldSq)?.let {
                                if (it < bestDistSq) { bestDistSq = it; bestEndpoint = p }
                            }
                        }
                        el.points.lastOrNull()?.let { p ->
                            checkCandidate(rawPoint, p, thresholdWorldSq)?.let {
                                if (it < bestDistSq) { bestDistSq = it; bestEndpoint = p }
                            }
                        }
                    }
                    is DimensionMarkup -> {
                        checkCandidate(rawPoint, el.start, thresholdWorldSq)?.let {
                            if (it < bestDistSq) { bestDistSq = it; bestEndpoint = el.start }
                        }
                        checkCandidate(rawPoint, el.end, thresholdWorldSq)?.let {
                            if (it < bestDistSq) { bestDistSq = it; bestEndpoint = el.end }
                        }
                    }
                    else -> Unit
                }
            }

            if (bestEndpoint != null) {
                val dist = kotlin.math.sqrt(bestDistSq) * zoomScale
                return SnapResult(
                    snappedPoint = bestEndpoint!!,
                    type = SnapType.ENDPOINT,
                    sourcePoint = bestEndpoint!!,
                    distancePx = dist
                )
            }
        }

        // 2. MIDPOINTS (Second priority)
        if (settings.snapToMidpoints) {
            var bestMidpoint: Point2D? = null
            var bestDistSq = Float.MAX_VALUE

            for (el in visibleElements) {
                when (el) {
                    is LineElement -> {
                        val mid = Point2D((el.start.x + el.end.x) / 2f, (el.start.y + el.end.y) / 2f)
                        checkCandidate(rawPoint, mid, thresholdWorldSq)?.let {
                            if (it < bestDistSq) { bestDistSq = it; bestMidpoint = mid }
                        }
                    }
                    is RectangleElement -> {
                        val minX = min(el.left, el.right)
                        val maxX = max(el.left, el.right)
                        val minY = min(el.top, el.bottom)
                        val maxY = max(el.top, el.bottom)
                        val midX = (minX + maxX) / 2f
                        val midY = (minY + maxY) / 2f

                        val mids = listOf(
                            Point2D(midX, minY),
                            Point2D(midX, maxY),
                            Point2D(minX, midY),
                            Point2D(maxX, midY)
                        )
                        for (m in mids) {
                            checkCandidate(rawPoint, m, thresholdWorldSq)?.let {
                                if (it < bestDistSq) { bestDistSq = it; bestMidpoint = m }
                            }
                        }
                    }
                    is PolylineElement -> {
                        for (i in 0 until el.points.size - 1) {
                            val p1 = el.points[i]
                            val p2 = el.points[i + 1]
                            val mid = Point2D((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                            checkCandidate(rawPoint, mid, thresholdWorldSq)?.let {
                                if (it < bestDistSq) { bestDistSq = it; bestMidpoint = mid }
                            }
                        }
                    }
                    else -> Unit
                }
            }

            if (bestMidpoint != null) {
                val dist = kotlin.math.sqrt(bestDistSq) * zoomScale
                return SnapResult(
                    snappedPoint = bestMidpoint!!,
                    type = SnapType.MIDPOINT,
                    sourcePoint = bestMidpoint!!,
                    distancePx = dist
                )
            }
        }

        // 3. CENTERS (Third priority)
        if (settings.snapToCenters) {
            var bestCenter: Point2D? = null
            var bestDistSq = Float.MAX_VALUE

            for (el in visibleElements) {
                when (el) {
                    is RectangleElement -> {
                        val center = Point2D((el.left + el.right) / 2f, (el.top + el.bottom) / 2f)
                        checkCandidate(rawPoint, center, thresholdWorldSq)?.let {
                            if (it < bestDistSq) { bestDistSq = it; bestCenter = center }
                        }
                    }
                    is EllipseElement -> {
                        val center = Point2D(el.centerX, el.centerY)
                        checkCandidate(rawPoint, center, thresholdWorldSq)?.let {
                            if (it < bestDistSq) { bestDistSq = it; bestCenter = center }
                        }
                    }
                    else -> Unit
                }
            }

            if (bestCenter != null) {
                val dist = kotlin.math.sqrt(bestDistSq) * zoomScale
                return SnapResult(
                    snappedPoint = bestCenter!!,
                    type = SnapType.CENTER,
                    sourcePoint = bestCenter!!,
                    distancePx = dist
                )
            }
        }

        // 4. ORTHOGONAL (Horizontal / Vertical lock to stroke origin)
        if (settings.snapOrthogonal && strokeStart != null) {
            val dx = abs(rawPoint.x - strokeStart.x)
            val dy = abs(rawPoint.y - strokeStart.y)

            // Horizontal lock
            if (dy <= thresholdWorld && dx > thresholdWorld * 0.5f) {
                val locked = Point2D(rawPoint.x, strokeStart.y)
                return SnapResult(
                    snappedPoint = locked,
                    type = SnapType.ORTHO_HORIZONTAL,
                    sourcePoint = strokeStart,
                    distancePx = dy * zoomScale
                )
            }

            // Vertical lock
            if (dx <= thresholdWorld && dy > thresholdWorld * 0.5f) {
                val locked = Point2D(strokeStart.x, rawPoint.y)
                return SnapResult(
                    snappedPoint = locked,
                    type = SnapType.ORTHO_VERTICAL,
                    sourcePoint = strokeStart,
                    distancePx = dx * zoomScale
                )
            }
        }

        // 5. GRID INTERSECTION (Fifth priority)
        if (settings.snapToGrid && settings.gridSpacingPx > 0f) {
            val gridStep = settings.gridSpacingPx
            val gx = round(rawPoint.x / gridStep) * gridStep
            val gy = round(rawPoint.y / gridStep) * gridStep
            val gridPoint = Point2D(gx, gy)

            val dSq = (rawPoint.x - gx) * (rawPoint.x - gx) + (rawPoint.y - gy) * (rawPoint.y - gy)
            if (dSq <= thresholdWorldSq) {
                val dist = kotlin.math.sqrt(dSq) * zoomScale
                return SnapResult(
                    snappedPoint = gridPoint,
                    type = SnapType.GRID_INTERSECTION,
                    sourcePoint = gridPoint,
                    distancePx = dist
                )
            }
        }

        return null
    }

    private inline fun checkCandidate(raw: Point2D, candidate: Point2D, maxDistSq: Float): Float? {
        val dx = raw.x - candidate.x
        val dy = raw.y - candidate.y
        val dSq = dx * dx + dy * dy
        return if (dSq <= maxDistSq) dSq else null
    }

    /**
     * Renders an architectural snap reticle at the snapped coordinate.
     */
    fun renderSnapIndicator(
        canvas: Canvas,
        snap: SnapResult,
        zoomScale: Float,
        strokeStart: Point2D? = null
    ) {
        val pt = snap.snappedPoint
        val glyphRadius = 7f / zoomScale
        val color = snap.type.colorInt

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.style = Paint.Style.STROKE
            this.strokeWidth = 2.5f / zoomScale
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = (color and 0x00FFFFFF) or 0x40000000 // 25% alpha
            this.style = Paint.Style.FILL
        }

        when (snap.type) {
            SnapType.ENDPOINT -> {
                // Architectural green square
                val rect = RectF(pt.x - glyphRadius, pt.y - glyphRadius, pt.x + glyphRadius, pt.y + glyphRadius)
                canvas.drawRect(rect, fillPaint)
                canvas.drawRect(rect, paint)
                // Diagonal cross
                canvas.drawLine(rect.left, rect.top, rect.right, rect.bottom, paint)
                canvas.drawLine(rect.left, rect.bottom, rect.right, rect.top, paint)
            }

            SnapType.MIDPOINT -> {
                // Architectural cyan triangle
                val path = Path().apply {
                    moveTo(pt.x, pt.y - glyphRadius * 1.2f)
                    lineTo(pt.x + glyphRadius * 1.1f, pt.y + glyphRadius * 0.8f)
                    lineTo(pt.x - glyphRadius * 1.1f, pt.y + glyphRadius * 0.8f)
                    close()
                }
                canvas.drawPath(path, fillPaint)
                canvas.drawPath(path, paint)
            }

            SnapType.CENTER -> {
                // Architectural amber circle with center pip
                canvas.drawCircle(pt.x, pt.y, glyphRadius * 1.1f, fillPaint)
                canvas.drawCircle(pt.x, pt.y, glyphRadius * 1.1f, paint)
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    this.style = Paint.Style.FILL
                }
                canvas.drawCircle(pt.x, pt.y, 2f / zoomScale, dotPaint)
            }

            SnapType.GRID_INTERSECTION -> {
                // Purple architectural crosshair
                val arm = glyphRadius * 1.3f
                canvas.drawLine(pt.x - arm, pt.y, pt.x + arm, pt.y, paint)
                canvas.drawLine(pt.x, pt.y - arm, pt.x, pt.y + arm, paint)
                canvas.drawCircle(pt.x, pt.y, glyphRadius * 0.6f, paint)
            }

            SnapType.ORTHO_HORIZONTAL -> {
                // Blue horizontal dashed alignment guide
                val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    this.style = Paint.Style.STROKE
                    this.strokeWidth = 1.5f / zoomScale
                    this.pathEffect = DashPathEffect(floatArrayOf(6f / zoomScale, 4f / zoomScale), 0f)
                }
                if (strokeStart != null) {
                    canvas.drawLine(strokeStart.x, pt.y, pt.x, pt.y, guidePaint)
                }
                canvas.drawCircle(pt.x, pt.y, glyphRadius, paint)
            }

            SnapType.ORTHO_VERTICAL -> {
                // Blue vertical dashed alignment guide
                val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    this.style = Paint.Style.STROKE
                    this.strokeWidth = 1.5f / zoomScale
                    this.pathEffect = DashPathEffect(floatArrayOf(6f / zoomScale, 4f / zoomScale), 0f)
                }
                if (strokeStart != null) {
                    canvas.drawLine(pt.x, strokeStart.y, pt.x, pt.y, guidePaint)
                }
                canvas.drawCircle(pt.x, pt.y, glyphRadius, paint)
            }
        }

        // Draw micro badge label
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            this.textSize = (11f / zoomScale).coerceIn(8f, 28f)
            this.typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val text = snap.type.label
        val textWidth = textPaint.measureText(text)
        val textHeight = textPaint.textSize
        val badgePadding = 3f / zoomScale

        val badgeRect = RectF(
            pt.x + glyphRadius + 4f / zoomScale,
            pt.y - textHeight / 2f - badgePadding,
            pt.x + glyphRadius + 4f / zoomScale + textWidth + badgePadding * 2f,
            pt.y + textHeight / 2f + badgePadding
        )

        val bgBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, 3f / zoomScale, 3f / zoomScale, bgBadgePaint)
        canvas.drawText(text, badgeRect.left + badgePadding, badgeRect.bottom - badgePadding * 1.3f, textPaint)
    }
}
