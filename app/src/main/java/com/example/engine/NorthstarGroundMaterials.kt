package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.LruCache
import com.example.model.ScaleCalibration
import com.example.model.SurfaceMaterial
import java.util.Random
import kotlin.math.*

/** Paint-only material study. Exact clipping and final boundary ink belong to the caller.
 * Reuses the existing recursive polygon method; no reference art is embedded.
 * Fixed work and byte-bounded prefiltered images keep redraw independent of deposit count.
 */
internal object NorthstarGroundMaterials {
    private const val SIDE = 1024
    private data class Key(val id: String, val width: Int, val height: Int, val grass: Boolean)
    private data class Wash(val levels: List<Bitmap>)
    private val cache = object : LruCache<Key, Wash>(24 * 1024 * 1024) {
        override fun sizeOf(key: Key, value: Wash) = value.levels.sumOf { it.allocationByteCount }
    }
    internal fun clearCache() = synchronized(cache) { cache.evictAll() }

    fun draw(canvas: Canvas, id: String, path: Path, bounds: RectF,
             material: SurfaceMaterial, alpha: Float, scale: ScaleCalibration) {
        val longest = max(bounds.width(), bounds.height())
        if (!longest.isFinite() || longest <= 0f || alpha <= 0f) return
        val grass = material == SurfaceMaterial.TURF
        val key = Key(id, (SIDE * bounds.width() / longest).roundToInt().coerceIn(1, SIDE),
            (SIDE * bounds.height() / longest).roundToInt().coerceIn(1, SIDE), grass)
        val wash = synchronized(cache) { cache.get(key) } ?: generate(key).also {
            synchronized(cache) { cache.put(key, it) }
        }
        val extent = deviceExtent(canvas, bounds)
        val bitmap = wash.levels.lastOrNull { max(it.width, it.height) >= extent } ?: wash.levels.first()
        canvas.drawBitmap(bitmap, null, bounds, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.alpha = (alpha.coerceIn(0f, 1f) * 255).roundToInt()
        })
        // The coping outer polygon is overdrawn by the pool. A deck grid would
        // imply incorrect cross-pool coping joints, so only the stone wash applies.
        if (!grass && !id.endsWith(":coping-outer")) drawJoints(canvas, bounds, scale, alpha, id)
        drawDryBoundary(canvas, path, bounds, grass, alpha, id)
    }

    private fun generate(key: Key): Wash {
        val random = Random(seed(key.id))
        fun between(a: Float, b: Float) = a + random.nextFloat() * (b - a)
        // Accumulate faint glazes in floating point. Repeated 3/255 deposits in
        // 8-bit premultiplied storage biased warm neutral pigment toward pink/green.
        // Convert once after painting; the retained cache remains ordinary ARGB.
        val wetPaint = Bitmap.createBitmap(key.width, key.height, Bitmap.Config.RGBA_F16)
        val canvas = Canvas(wetPaint)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val grass = key.grass
        val palette = if (grass) intArrayOf(Color.rgb(160, 181, 63), Color.rgb(120, 151, 61),
            Color.rgb(191, 199, 83), Color.rgb(83, 120, 48)) else intArrayOf(
            Color.rgb(192, 155, 99), Color.rgb(207, 184, 139), Color.rgb(163, 141, 105), Color.rgb(224, 201, 159))
        fun boundary(x: Float, y: Float, radius: Float): List<NorthstarPolygonWash.Vertex> {
            val stretch = between(.7f, 1.4f)
            val phase = between(0f, 6.283185f)
            return NorthstarPolygonWash.deform(List(9) { i ->
                val angle = phase + i * 6.283185f / 9
                val reach = radius * between(.76f, 1.22f)
                NorthstarPolygonWash.Vertex(x + cos(angle) * reach * stretch,
                    y + sin(angle) * reach / stretch, between(.08f, .30f))
            }, random, 2)
        }
        fun polygon(vertices: List<NorthstarPolygonWash.Vertex>) = Path().apply {
            moveTo(vertices[0].x, vertices[0].y)
            for (i in 1 until vertices.size) lineTo(vertices[i].x, vertices[i].y)
            close()
        }
        fun glaze(x: Float, y: Float, radius: Float, color: Int, opacity: Int, layers: Int, dry: Boolean) {
            val ring = boundary(x, y, radius)
            repeat(layers) {
                paint.style = Paint.Style.FILL
                paint.color = color; paint.alpha = opacity
                canvas.drawPath(polygon(NorthstarPolygonWash.deform(ring, random, 2)), paint)
            }
            if (dry) {
                // Only parts of a wash front collect pigment. Retain its shared
                // contour through the wet layers, then lay a fine, broken dry rim.
                val front = NorthstarPolygonWash.deform(ring, random, 2)
                val rim = Path()
                for (i in 1 until front.size) {
                    val a = front[i - 1]; val b = front[i]
                    if (sin(i * .13f + x) > .35f && random.nextFloat() > .12f) {
                        rim.moveTo(a.x, a.y); rim.lineTo(b.x, b.y)
                    }
                }
                paint.style = Paint.Style.STROKE; paint.strokeWidth = between(.65f, 1.35f)
                paint.color = color; paint.alpha = if (grass) 45 else 27
                canvas.drawPath(rim, paint)
            }
        }
        // Broad yellow-green/ochre glazes, followed by distinct smaller blooms.
        repeat(18) { i -> glaze(between(0f, key.width.toFloat()), between(0f, key.height.toFloat()),
            between(95f, 210f), palette[i % 4], if (grass) 6 else 3, 9, false) }
        repeat(70) { i ->
            val x = between(0f, key.width.toFloat()); val y = between(0f, key.height.toFloat())
            val radius = between(18f, 58f)
            glaze(x, y, radius, palette[i % 4], if (grass) 7 else 4, 7, i % 3 != 0)
            repeat(5) {
                glaze(x + between(-radius, radius), y + between(-radius, radius),
                    between(3f, 19f), palette[i % 4], if (grass) 9 else 5, 4, true)
            }
            repeat(10) {
                glaze(x + between(-radius, radius), y + between(-radius, radius),
                    between(.8f, 4.5f), palette[i % 4], if (grass) 17 else 10, 3, false)
            }
        }
        // Dry paper/lifting is irregular paint coverage, not a uniform white noise layer.
        repeat(if (grass) 420 else 260) {
            val x = between(0f, key.width.toFloat()); val y = between(0f, key.height.toFloat())
            glaze(x, y, between(1.4f, if (grass) 9f else 5f), Color.rgb(255, 253, 232), 16, 2, false)
        }
        paint.style = Paint.Style.FILL
        // Deposits/pits gather in patches. Sparse sharp marks sit over softer washes.
        repeat(if (grass) 1800 else 1300) {
            val x = between(0f, key.width.toFloat()); val y = between(0f, key.height.toFloat())
            val grouping = sin(x * .024f + sin(y * .017f)) * cos(y * .021f)
            if (random.nextFloat() > .42f + grouping * .32f) return@repeat
            val radius = between(.6f, if (grass) 3.6f else 2.5f)
            val dark = if (grass) Color.rgb(57, 83, 28) else Color.rgb(117, 89, 54)
            if (!grass) {
                paint.color = Color.rgb(255, 252, 235); paint.alpha = 75
                canvas.drawOval(x - radius, y - radius, x + radius * 1.3f, y + radius * 1.6f, paint)
            }
            paint.color = dark; paint.alpha = between(60f, if (grass) 205f else 160f).toInt()
            val deposit = polygon(boundary(x, y, radius))
            canvas.drawPath(deposit, paint)
            if (grass && random.nextFloat() > .72f) {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = between(.7f, 1.1f)
                val blade = Path().apply {
                    moveTo(x, y); quadTo(x - 1.5f, y - 2f, x - between(1f, 3f), y - between(3f, 6f))
                    moveTo(x, y); quadTo(x + 1f, y - 1f, x + 2f, y - between(2f, 4f))
                }
                canvas.drawPath(blade, paint); paint.style = Paint.Style.FILL
            }
        }
        val bitmap = wetPaint.copy(Bitmap.Config.ARGB_8888, true)
        wetPaint.recycle()
        // Fine tooth modulates existing pigment rather than creating dark pixels on bare paper.
        val pixels = IntArray(key.width * key.height)
        bitmap.getPixels(pixels, 0, key.width, 0, 0, key.width, key.height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val tooth = .83f + random.nextFloat() * .27f
            pixels[i] = Color.argb((Color.alpha(c) * tooth).roundToInt().coerceIn(0, 255),
                Color.red(c), Color.green(c), Color.blue(c))
        }
        bitmap.setPixels(pixels, 0, key.width, 0, 0, key.width, key.height)
        val levels = mutableListOf(bitmap)
        while (levels.last().width > 1 || levels.last().height > 1) {
            val previous = levels.last()
            levels += Bitmap.createScaledBitmap(previous, (previous.width / 2).coerceAtLeast(1),
                (previous.height / 2).coerceAtLeast(1), true)
        }
        return Wash(levels)
    }

    /** Illustrative 12x24 inch running bond. It is appearance, not a cut/takeoff layout. */
    private fun drawJoints(canvas: Canvas, bounds: RectF, scale: ScaleCalibration, alpha: Float, id: String) {
        val metresPerUnit = when (scale.unit.lowercase()) {
            "ft", "feet", "'" -> .3048f
            "in", "inch", "inches" -> .0254f
            "cm" -> .01f
            "mm" -> .001f
            else -> 1f
        }
        val ppu = if (scale.isCalibrated) scale.pixelsPerUnit / metresPerUnit
            else max(bounds.width(), bounds.height()) / 3.6576f
        var width = ppu * .6096f
        if (!width.isFinite() || width <= 0f) return
        // A giant uncalibrated/imported extent cannot create unbounded joint work.
        width = max(width, max(bounds.width(), bounds.height()) / 128f)
        val height = width * .5f
        val random = Random(seed(id) xor 9143L)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.BUTT }
        val rows = ceil(bounds.height() / height).toInt()
        val columns = ceil(bounds.width() / width).toInt() + 1
        val lineWidth = width * .009f
        for (row in 0..rows) {
            val y = bounds.top + row * height
            val offset = if (row % 2 == 0) 0f else -width * .5f
            for (col in 0..columns) {
                val x = bounds.left + offset + col * width
                paint.color = Color.rgb(146, 121, 85)
                paint.alpha = (random.nextInt(10) * alpha).toInt()
                canvas.drawRect(x, y, x + width, y + height, paint)
                // Continuous quiet joints establish exact orientation; small darker
                // segments and intersections add dry ink without moving the grid.
                paint.color = Color.rgb(105, 99, 78); paint.alpha = (115 * alpha).toInt()
                paint.strokeWidth = lineWidth
                canvas.drawLine(x, y, x + width, y, paint)
                canvas.drawLine(x, y, x, y + height, paint)
                paint.alpha = ((55 + random.nextInt(90)) * alpha).toInt()
                paint.strokeWidth = lineWidth * .65f
                val start = random.nextFloat() * .5f
                canvas.drawLine(x + width * start, y, x + width * (start + .24f), y, paint)
                paint.alpha = (125 * alpha).toInt()
                canvas.drawCircle(x, y, lineWidth * .7f, paint)
            }
        }
    }

    private fun drawDryBoundary(canvas: Canvas, path: Path, bounds: RectF, grass: Boolean, alpha: Float, id: String) {
        val unit = max(bounds.width(), bounds.height()) / SIDE
        val measure = PathMeasure(path, true)
        val random = Random(seed(id) xor 5911L)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
            color = if (grass) Color.rgb(67, 102, 35) else Color.rgb(152, 117, 71)
        }
        var distance = 0f
        var count = 0
        while (distance < measure.length && count++ < 500) {
            val length = (8f + random.nextFloat() * 34f) * unit
            val edge = Path()
            measure.getSegment(distance, min(distance + length, measure.length), edge, true)
            paint.strokeWidth = (5f + random.nextFloat() * 10f) * unit
            paint.alpha = ((if (grass) 24 else 13) * alpha).toInt()
            canvas.drawPath(edge, paint)
            paint.strokeWidth = (1f + random.nextFloat() * 2f) * unit
            paint.alpha = ((if (grass) 100 else 53) * alpha).toInt()
            canvas.drawPath(edge, paint)
            distance += length + (3f + random.nextFloat() * 17f) * unit
        }
    }

    @Suppress("DEPRECATION")
    private fun deviceExtent(canvas: Canvas, bounds: RectF): Float {
        val matrix = Matrix(); canvas.getMatrix(matrix)
        val v = FloatArray(9); matrix.getValues(v)
        return max(hypot(v[Matrix.MSCALE_X], v[Matrix.MSKEW_Y]) * bounds.width(),
            hypot(v[Matrix.MSKEW_X], v[Matrix.MSCALE_Y]) * bounds.height()).coerceAtLeast(1f)
    }
    private fun seed(id: String): Long {
        var hash = 0xCBF29CE484222325uL.toLong()
        for (c in id) hash = (hash xor c.code.toLong()) * 0x100000001B3L
        return hash
    }
}
