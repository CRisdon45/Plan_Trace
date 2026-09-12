package com.example.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Deterministic highlight and edge-detail passes layered above the pigment field. */
internal object NorthstarWaterDetails {
    fun draw(
        canvas: Canvas,
        stableId: String,
        path: Path,
        bounds: RectF,
        outline: Int,
        alpha: Float,
    ) {
        drawCaustics(canvas, stableId, bounds, alpha)
        drawSelectiveShoreline(canvas, stableId, path, bounds, outline, alpha)
    }

    /**
     * A sparse, stable caustic web gives the wash a confident water identity without
     * covering the entire pool in a repeated texture. The irregular cells are
     * generated in object-local space, so moving the pool moves the same drawing.
     */
    private fun drawCaustics(canvas: Canvas, stableId: String, bounds: RectF, alpha: Float) {
        if (bounds.width() <= 0f || bounds.height() <= 0f) return
        val columns = 9
        val rows = (columns * bounds.height() / bounds.width()).roundToInt().coerceIn(5, 8)
        val stepX = bounds.width() / columns
        val stepY = bounds.height() / rows
        val stableSeed = stableHash(stableId)
        val sites = ArrayList<WaterPoint>(columns * rows)
        for (row in 0 until rows) for (column in 0 until columns) {
            val stagger = if (row % 2 == 0) -stepX * 0.08f else stepX * 0.08f
            val jitterX = (stableUnit(stableSeed, column, row, 11) - 0.5f) * stepX * 0.48f
            val jitterY = (stableUnit(stableSeed, column, row, 17) - 0.5f) * stepY * 0.48f
            sites += WaterPoint(
                (column + 0.5f) * stepX + stagger + jitterX,
                (row + 0.5f) * stepY + jitterY,
            )
        }

        val quiet = Path()
        val strong = Path()
        val expanded = RectF(
            -bounds.width() * 0.22f,
            -bounds.height() * 0.22f,
            bounds.width() * 1.22f,
            bounds.height() * 1.22f,
        )
        val waterEdges = HashSet<String>()
        sites.forEachIndexed { index, _ ->
            val cell = voronoiCell(index, sites, expanded)
            if (cell.size < 3) return@forEachIndexed
            for (edgeIndex in cell.indices) {
                val start = cell[edgeIndex]
                val end = cell[(edgeIndex + 1) % cell.size]
                val key = waterEdgeKey(start, end)
                if (!waterEdges.add(key)) continue
                val localMidX = ((start.x + end.x) * 0.5f).roundToInt()
                val localMidY = ((start.y + end.y) * 0.5f).roundToInt()
                val presence = stableUnit(stableSeed, localMidX, localMidY, 61)
                if (presence < 0.27f) continue
                val emphasis = stableUnit(stableSeed, localMidX, localMidY, 67)
                val bend = (stableUnit(stableSeed, localMidX, localMidY, 71) - 0.5f) *
                    min(stepX, stepY) * 0.16f
                appendWaterEdge(if (emphasis > 0.86f) strong else quiet, start, end, bend)
            }
        }

        val minimumSide = min(bounds.width(), bounds.height())
        val pale = Color.rgb(247, 252, 248)
        val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorWithScaledAlpha(pale, 28, alpha)
            strokeWidth = (minimumSide * 0.014f).coerceIn(2.2f, 6f)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val quietCore = Paint(halo).apply {
            color = colorWithScaledAlpha(pale, 64, alpha)
            strokeWidth = (minimumSide * 0.0055f).coerceIn(1.0f, 2.8f)
        }
        val strongCore = Paint(quietCore).apply {
            color = colorWithScaledAlpha(pale, 108, alpha)
            strokeWidth = (minimumSide * 0.0075f).coerceIn(1.3f, 3.6f)
        }
        canvas.save()
        try {
            canvas.translate(bounds.left, bounds.top)
            canvas.drawPath(quiet, halo)
            canvas.drawPath(strong, halo)
            canvas.drawPath(quiet, quietCore)
            canvas.drawPath(strong, strongCore)
        } finally {
            canvas.restore()
        }
    }

    private data class WaterPoint(val x: Float, val y: Float)

    private fun waterEdgeKey(start: WaterPoint, end: WaterPoint): String {
        val first = "${(start.x * 10f).roundToInt()}:${(start.y * 10f).roundToInt()}"
        val second = "${(end.x * 10f).roundToInt()}:${(end.y * 10f).roundToInt()}"
        return if (first <= second) "$first|$second" else "$second|$first"
    }

    private fun appendWaterEdge(target: Path, start: WaterPoint, end: WaterPoint, bend: Float) {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        target.moveTo(start.x, start.y)
        target.quadTo(
            (start.x + end.x) * 0.5f - dy / length * bend,
            (start.y + end.y) * 0.5f + dx / length * bend,
            end.x,
            end.y,
        )
    }

    private fun voronoiCell(index: Int, sites: List<WaterPoint>, bounds: RectF): List<WaterPoint> {
        var polygon = listOf(
            WaterPoint(bounds.left, bounds.top),
            WaterPoint(bounds.right, bounds.top),
            WaterPoint(bounds.right, bounds.bottom),
            WaterPoint(bounds.left, bounds.bottom),
        )
        val site = sites[index]
        sites.forEachIndexed { otherIndex, other ->
            if (otherIndex == index || polygon.isEmpty()) return@forEachIndexed
            val normalX = other.x - site.x
            val normalY = other.y - site.y
            val boundary = (
                other.x * other.x + other.y * other.y - site.x * site.x - site.y * site.y
            ) * 0.5f
            polygon = clipToHalfPlane(polygon, normalX, normalY, boundary)
        }
        return polygon
    }

    private fun clipToHalfPlane(
        polygon: List<WaterPoint>,
        normalX: Float,
        normalY: Float,
        boundary: Float,
    ): List<WaterPoint> {
        if (polygon.isEmpty()) return polygon
        val result = ArrayList<WaterPoint>(polygon.size + 1)
        var previous = polygon.last()
        var previousDistance = normalX * previous.x + normalY * previous.y - boundary
        for (current in polygon) {
            val currentDistance = normalX * current.x + normalY * current.y - boundary
            val previousInside = previousDistance <= 0f
            val currentInside = currentDistance <= 0f
            if (previousInside != currentInside) {
                val fraction = previousDistance / (previousDistance - currentDistance)
                result += WaterPoint(
                    previous.x + (current.x - previous.x) * fraction,
                    previous.y + (current.y - previous.y) * fraction,
                )
            }
            if (currentInside) result += current
            previous = current
            previousDistance = currentDistance
        }
        return result
    }

    /** Broken edge deposits replace a mechanically uniform inner ring. */
    private fun drawSelectiveShoreline(
        canvas: Canvas,
        stableId: String,
        path: Path,
        bounds: RectF,
        outline: Int,
        alpha: Float,
    ) {
        val measure = PathMeasure(path, false)
        val minimumSide = min(bounds.width(), bounds.height())
        val stableSeed = stableHash(stableId)
        var contour = 0
        do {
            val length = measure.length
            if (length <= 0f) continue
            val segmentCount = (length / (minimumSide * 0.18f).coerceAtLeast(1f))
                .roundToInt()
                .coerceIn(14, 38)
            val segmentLength = length / segmentCount
            for (segment in 0 until segmentCount) {
                val presence = stableUnit(stableSeed, segment, contour, 53)
                if (presence < 0.34f) continue
                val start = segment * segmentLength + segmentLength * 0.06f
                val end = min(length, start + segmentLength * (0.70f + presence * 0.22f))
                val deposit = Path()
                if (!measure.getSegment(start, end, deposit, true)) continue
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorWithScaledAlpha(outline, (18f + presence * 27f).roundToInt(), alpha)
                    strokeWidth = (minimumSide * (0.024f + presence * 0.026f)).coerceIn(3f, 16f)
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawPath(deposit, paint)
            }
            contour += 1
        } while (measure.nextContour())
    }

    private fun stableHash(stableId: String): Int {
        var hash = 0x811C9DC5.toInt()
        stableId.forEach { hash = (hash xor it.code) * 0x01000193 }
        return hash
    }

    private fun stableUnit(stableSeed: Int, x: Int, y: Int, salt: Int): Float {
        var hash = stableSeed
        hash = (hash xor x) * 0x01000193
        hash = (hash xor y) * 0x01000193
        hash = (hash xor salt) * 0x01000193
        hash = (hash xor (hash ushr 16)) * 0x7FEB352D
        hash = (hash xor (hash ushr 15)) * 0x846CA68B.toInt()
        hash = hash xor (hash ushr 16)
        return (hash.toUInt().toDouble() / UInt.MAX_VALUE.toDouble()).toFloat()
    }

    private fun colorWithScaledAlpha(color: Int, strength: Int, alpha: Float) = Color.argb(
        (strength * alpha).toInt().coerceIn(0, 255),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )
}
