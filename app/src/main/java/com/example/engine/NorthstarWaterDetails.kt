package com.example.engine

import android.graphics.Bitmap
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
import kotlin.math.sqrt
import kotlin.math.pow

/** Deterministic highlight and edge-detail passes layered above the pigment field. */
internal object NorthstarWaterDetails {
    private data class WebKey(val stableId: String, val aspect: Float)
    private data class Web(val halo: Path, val quiet: Path, val strong: Path, val glints: Path)
    private data class RasterKey(val web: WebKey, val alpha: Float)
    // Fixed normalized coordinates keep tessellation independent of zoom/output.
    // Bounded to 24 objects; cached paths are only read after construction.
    private val webs = LruCache<WebKey, Web>(24)
    // Thin filled paths on the hardware canvas can fall between coverage samples
    // at sheet zoom. A mipmapped texture integrates that light before minification.
    // Export retains the vector ribbons. This cache is byte-bounded, not per-frame.
    private val rasters = object : LruCache<RasterKey, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: RasterKey, value: Bitmap) = value.allocationByteCount
    }

    internal fun clearCache() {
        synchronized(webs) { webs.evictAll() }
        synchronized(rasters) { rasters.evictAll() }
    }

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
    internal fun drawCaustics(
        canvas: Canvas, stableId: String, bounds: RectF, alpha: Float,
        rasterize: Boolean = canvas.isHardwareAccelerated,
    ) {
        if (bounds.width() <= 0f || bounds.height() <= 0f || alpha <= 0f) return
        val longest = max(bounds.width(), bounds.height())
        val normalized = RectF(0f, 0f, bounds.width() / longest * 1000f, bounds.height() / longest * 1000f)
        val key = WebKey(stableId, bounds.width() / bounds.height())
        val web = synchronized(webs) { webs.get(key) } ?: buildWeb(stableId, normalized).also {
            synchronized(webs) { webs.put(key, it) }
        }
        if (rasterize) {
            val rasterKey = RasterKey(key, alpha.coerceIn(0f, 1f))
            val bitmap = synchronized(rasters) { rasters.get(rasterKey) } ?: run {
                val width = (normalized.width() * 1.024f).roundToInt().coerceIn(1, 1024)
                val height = (normalized.height() * 1.024f).roundToInt().coerceIn(1, 1024)
                val generated = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val surface = Canvas(generated)
                surface.scale(width / normalized.width(), height / normalized.height())
                drawWeb(surface, web, rasterKey.alpha)
                generated.setHasMipMap(true)
                synchronized(rasters) { rasters.put(rasterKey, generated) }
                generated
            }
            canvas.drawBitmap(bitmap, null, bounds, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            return
        }
        canvas.save()
        try {
            canvas.translate(bounds.left, bounds.top)
            canvas.scale(longest / 1000f, longest / 1000f)
            drawWeb(canvas, web, alpha)
        } finally {
            canvas.restore()
        }
    }

    private fun drawWeb(canvas: Canvas, web: Web, alpha: Float) {
        val pale = Color.rgb(239, 253, 255)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        fun pass(path: Path, strength: Int) {
            paint.color = colorWithScaledAlpha(pale, strength, alpha)
            canvas.drawPath(path, paint)
        }
        pass(web.halo, 18)
        pass(web.quiet, 58)
        pass(web.strong, 130)
        pass(web.glints, 220)
    }

    private fun buildWeb(stableId: String, bounds: RectF): Web {
        val columns = (21f * bounds.width() / 1000f).roundToInt().coerceIn(4, 21)
        val rows = (21f * bounds.height() / 1000f).roundToInt().coerceIn(4, 21)
        val stepX = bounds.width() / columns
        val stepY = bounds.height() / rows
        val stableSeed = stableHash(stableId)
        val sites = ArrayList<WaterPoint>(columns * rows)
        for (row in 0 until rows) for (column in 0 until columns) {
            // Leave occasional open areas and introduce close pairs elsewhere.
            // A cell per lattice position otherwise reads as a regular mosaic.
            val occupancy = stableUnit(stableSeed, column, row, 19)
            if (occupancy < 0.10f) continue
            val stagger = if (row % 2 == 0) -stepX * 0.08f else stepX * 0.08f
            val jitterX = (stableUnit(stableSeed, column, row, 11) - 0.5f) * stepX * 0.92f
            val jitterY = (stableUnit(stableSeed, column, row, 17) - 0.5f) * stepY * 0.92f
            sites += WaterPoint(
                (column + 0.5f) * stepX + stagger + jitterX,
                (row + 0.5f) * stepY + jitterY,
            )
            if (occupancy > 0.80f) {
                val center = sites.last()
                val angle = stableUnit(stableSeed, column, row, 23) * 6.283185f
                sites += WaterPoint(center.x + cos(angle) * stepX * 0.36f,
                    center.y + sin(angle) * stepY * 0.36f)
            }
        }

        val halo = Path()
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
                if (presence < 0.035f) continue
                val emphasis = stableUnit(stableSeed, localMidX, localMidY, 67)
                appendLightRibbon(halo, quiet, strong, glints, start, end,
                    stepX, stepY, phase, emphasis, presence)
            }
        }

        return Web(halo, quiet, strong, glints)
    }

    private data class WaterPoint(val x: Float, val y: Float)

    private fun waterEdgeKey(start: WaterPoint, end: WaterPoint): String {
        val first = "${(start.x * 10f).roundToInt()}:${(start.y * 10f).roundToInt()}"
        val second = "${(end.x * 10f).roundToInt()}:${(end.y * 10f).roundToInt()}"
        return if (first <= second) "$first|$second" else "$second|$first"
    }

    private fun appendLightRibbon(
        halo: Path, quiet: Path, strong: Path, glints: Path,
        start: WaterPoint, end: WaterPoint,
        stepX: Float, stepY: Float, phase: Float, emphasis: Float, presence: Float,
    ) {
        val dx = end.x - start.x
        val dy = end.y - start.y
        if (dx * dx + dy * dy < 0.01f) return
        val samples = (sqrt(dx * dx + dy * dy) / 1.8f).roundToInt().coerceIn(16, 160)
        val points = ArrayList<WaterPoint>(samples + 1)
        val widths = FloatArray(samples + 1)
        val crests = FloatArray(samples + 1)
        for (sample in 0..samples) {
            val t = sample.toFloat() / samples
            val point = flowPoint(start.x + dx * t, start.y + dy * t, stepX, stepY, phase)
            points += point
            // The broad light field is continuous through junctions. Local crests
            // swell and taper along each ribbon rather than ending in round dashes.
            val illumination = (0.62f + 0.25f * sin(point.x * 0.013f + point.y * 0.009f + phase) +
                0.13f * cos(point.y * 0.024f - point.x * 0.007f + phase)).coerceIn(0.15f, 1f)
            val crest = sin(t * 3.141593f).coerceAtLeast(0f).pow(1.4f)
            val rhythm = 0.68f + 0.32f * sin(t * 6.283185f + emphasis * 8f)
            // Near a node the band widens into a small luminous confluence.
            val junction = (1f - (sin(t * 3.141593f)).coerceAtLeast(0f)).pow(7f)
            val fade = if (presence < 0.075f) crest else 1f
            val dryEdge = 0.93f + 0.07f * sin(point.x * 0.75f + point.y * 0.32f + phase)
            widths[sample] = (0.24f + illumination * 0.45f + crest * rhythm * emphasis * 0.95f +
                junction * illumination * 0.65f) * fade * dryEdge
            crests[sample] = if (emphasis > 0.57f) widths[sample] * crest *
                (0.25f + 0.75f * (emphasis - 0.57f) / 0.43f) else 0f
        }
        appendRibbon(halo, points, FloatArray(widths.size) { widths[it] * 3.4f })
        appendRibbon(quiet, points, FloatArray(widths.size) { widths[it] * 1.7f })
        appendRibbon(strong, points, widths)
        if (emphasis > 0.57f) appendRibbon(glints, points, crests)
    }

    private fun flowPoint(x: Float, y: Float, stepX: Float, stepY: Float, phase: Float): WaterPoint {
        var u = x / stepX
        var v = y / stepY
        // Successive shears remain invertible. All neighboring edges share the
        // same map, so stronger curvature cannot tear their common junctions.
        u += 0.23f * sin(v * 1.8f + phase) + 0.07f * sin(v * 4.4f - phase)
        v += 0.21f * sin(u * 1.7f + phase) + 0.07f * sin(u * 4.1f + phase)
        u += 0.06f * sin(v * 5.1f + phase)
        // A diagonal shear varies local direction without folding the map.
        val diagonal = 0.055f * sin((u + v) * 4.8f + phase)
        u += diagonal
        v -= diagonal
        return WaterPoint(u * stepX, v * stepY)
    }

    /** A filled ribbon supplies continuous variable width, including tapered ends. */
    private fun appendRibbon(target: Path, points: List<WaterPoint>, widths: FloatArray) {
        val normals = points.indices.map { i ->
            val before = points[(i - 1).coerceAtLeast(0)]
            val after = points[(i + 1).coerceAtMost(points.lastIndex)]
            val dx = after.x - before.x
            val dy = after.y - before.y
            val length = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
            WaterPoint(-dy / length, dx / length)
        }
        for (i in points.indices) {
            val x = points[i].x + normals[i].x * widths[i]
            val y = points[i].y + normals[i].y * widths[i]
            if (i == 0) target.moveTo(x, y) else target.lineTo(x, y)
        }
        for (i in points.indices.reversed()) {
            target.lineTo(points[i].x - normals[i].x * widths[i], points[i].y - normals[i].y * widths[i])
        }
        target.close()
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
