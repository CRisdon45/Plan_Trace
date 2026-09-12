package com.example.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.LruCache
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Deterministic highlight and edge-detail passes layered above the pigment field. */
internal object NorthstarWaterDetails {
    private data class WebKey(val stableId: String, val aspect: Float)
    private data class Web(val quiet: Path, val strong: Path, val glints: Path)
    // Fixed normalized coordinates keep tessellation independent of zoom/output.
    // Bounded to 24 objects; cached paths are only read after construction.
    private val webs = LruCache<WebKey, Web>(24)

    internal fun clearCache() = synchronized(webs) { webs.evictAll() }

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
        val longest = max(bounds.width(), bounds.height())
        val normalized = RectF(0f, 0f, bounds.width() / longest * 1000f, bounds.height() / longest * 1000f)
        val key = WebKey(stableId, bounds.width() / bounds.height())
        val web = synchronized(webs) { webs.get(key) } ?: buildWeb(stableId, normalized).also {
            synchronized(webs) { webs.put(key, it) }
        }
        val pale = Color.rgb(239, 253, 255)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.save()
        try {
            canvas.translate(bounds.left, bounds.top)
            canvas.scale(longest / 1000f, longest / 1000f)
            fun pass(path: Path, width: Float, strength: Int) {
                paint.color = colorWithScaledAlpha(pale, strength, alpha)
                paint.strokeWidth = width
                canvas.drawPath(path, paint)
            }
            pass(web.quiet, 5.0f, 24)
            pass(web.strong, 5.8f, 34)
            pass(web.quiet, 1.15f, 112)
            pass(web.strong, 1.7f, 172)
            pass(web.glints, 2.3f, 205)
        } finally {
            canvas.restore()
        }
    }

    private fun buildWeb(stableId: String, bounds: RectF): Web {
        val columns = (21f * bounds.width() / 1000f).roundToInt().coerceIn(4, 21)
        val rows = (21f * bounds.height() / 1000f).roundToInt().coerceIn(4, 21)
        val stepX = bounds.width() / columns
        val stepY = bounds.height() / rows
        val stableSeed = stableHash(stableId)
        val sites = ArrayList<WaterPoint>(columns * rows)
        for (row in 0 until rows) for (column in 0 until columns) {
            val stagger = if (row % 2 == 0) -stepX * 0.08f else stepX * 0.08f
            val jitterX = (stableUnit(stableSeed, column, row, 11) - 0.5f) * stepX * 0.92f
            val jitterY = (stableUnit(stableSeed, column, row, 17) - 0.5f) * stepY * 0.92f
            sites += WaterPoint(
                (column + 0.5f) * stepX + stagger + jitterX,
                (row + 0.5f) * stepY + jitterY,
            )
        }

        val quiet = Path()
        val strong = Path()
        val glints = Path()
        val phase = stableUnit(stableSeed, 0, 0, 79) * 6.283185f
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
                if (presence < 0.07f) continue
                val emphasis = stableUnit(stableSeed, localMidX, localMidY, 67)
                val target = if (emphasis > 0.38f) strong else quiet
                appendWaterEdge(target, start, end, stepX, stepY, phase)
                if (emphasis > 0.80f) {
                    // A short lifted crest, not a second uniform white outline.
                    appendWaterEdge(glints, start, end, stepX, stepY, phase, 0.28f, 0.61f)
                }
            }
        }

        return Web(quiet, strong, glints)
    }

    private data class WaterPoint(val x: Float, val y: Float)

    private fun waterEdgeKey(start: WaterPoint, end: WaterPoint): String {
        val first = "${(start.x * 10f).roundToInt()}:${(start.y * 10f).roundToInt()}"
        val second = "${(end.x * 10f).roundToInt()}:${(end.y * 10f).roundToInt()}"
        return if (first <= second) "$first|$second" else "$second|$first"
    }

    private fun appendWaterEdge(
        target: Path, start: WaterPoint, end: WaterPoint,
        stepX: Float, stepY: Float, phase: Float, from: Float = 0f, to: Float = 1f,
    ) {
        // All edges use the same smooth displacement field. Shared junctions
        // remain joined, while straight polygon edges become flowing light ribbons.
        for (sample in 0..16) {
            val t = from + (to - from) * sample / 16f
            val x = start.x + (end.x - start.x) * t
            val y = start.y + (end.y - start.y) * t
            val u = x / stepX
            val v = y / stepY
            val warpedX = x + stepX * (0.27f * sin(v * 2.1f + phase) +
                0.12f * sin(u * 2.8f + v * 1.3f + phase))
            val warpedY = y + stepY * (0.24f * cos(u * 1.9f + phase) +
                0.10f * sin(v * 2.7f - u * 1.2f + phase))
            if (sample == 0) target.moveTo(warpedX, warpedY) else target.lineTo(warpedX, warpedY)
        }
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
