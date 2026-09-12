package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.LruCache
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Translucent pigment glazes, independently implemented from the recursive polygon
 * method described by Tyler Hobbs (2017). No upstream code or artwork is bundled.
 * Different scales/hues overlap; this is illustrative paint, not fluid physics.
 * The caller supplies the exact vector clip. Neither this raster nor its polygons
 * become editable/project geometry. Cold generation is bounded; repaint is a blit.
 */
internal object NorthstarPolygonWash {
    private const val SIDE = 768
    private const val CACHE_BYTES = 12 * 1024 * 1024
    private data class Key(val id: String, val width: Int, val height: Int)
    private data class Vertex(val x: Float, val y: Float, val variance: Float)
    private data class Glaze(
        val boundary: List<Vertex>, val color: Int, val opacity: Int, val layers: Int,
        val x: Float, val y: Float, val radius: Float,
    )
    private val cache = object : LruCache<Key, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: Key, value: Bitmap) = value.allocationByteCount
    }

    fun draw(canvas: Canvas, stableId: String, bounds: RectF, alpha: Float) {
        val longest = max(bounds.width(), bounds.height())
        if (!longest.isFinite() || bounds.width() <= 0f || bounds.height() <= 0f || alpha <= 0f) return
        val key = Key(stableId,
            (SIDE * bounds.width() / longest).roundToInt().coerceIn(1, SIDE),
            (SIDE * bounds.height() / longest).roundToInt().coerceIn(1, SIDE))
        val bitmap = synchronized(cache) { cache.get(key) } ?: paintGlazes(key).also {
            synchronized(cache) { cache.put(key, it) }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.alpha = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
        }
        canvas.drawBitmap(bitmap, null, bounds, paint)
    }

    internal fun clearCache() = synchronized(cache) { cache.evictAll() }

    private fun paintGlazes(key: Key): Bitmap {
        var seed = 0xCBF29CE484222325uL.toLong()
        for (character in key.id) seed = (seed xor character.code.toLong()) * 0x100000001B3L
        val random = Random(seed)
        fun between(low: Float, high: Float) = low + random.nextFloat() * (high - low)
        val glazes = mutableListOf<Glaze>()
        // Cool blue, cyan-blue and lighter washes interleave instead of turning
        // every overlapping layer into another application of the same dark tint.
        val colors = intArrayOf(Color.rgb(23, 111, 166), Color.rgb(34, 145, 183),
            Color.rgb(57, 162, 202), Color.rgb(142, 216, 236))
        fun add(x: Float, y: Float, radius: Float, color: Int, opacity: Int, layers: Int) {
            val stretch = between(0.65f, 1.4f)
            val phase = between(0f, (PI * 2).toFloat())
            val ring = List(9) { index ->
                val angle = phase + index * (PI * 2 / 9).toFloat()
                val reach = radius * between(0.70f, 1.23f)
                Vertex(x + cos(angle) * reach * stretch, y + sin(angle) * reach / stretch,
                    between(0.10f, 0.52f))
            }
            glazes += Glaze(deform(ring, random, 2), color, opacity, layers, x, y, radius)
        }
        repeat(14) { index ->
            add(between(0f, key.width.toFloat()), between(0f, key.height.toFloat()),
                between(95f, 230f), colors[index % colors.size], 7, 16)
        }
        // Medium blooms distribute unevenly; their offspring keep smaller deposits
        // associated with a wash rather than scattering uniform screen-space noise.
        repeat(48) { index ->
            val x = between(0f, key.width.toFloat())
            val y = between(0f, key.height.toFloat())
            val radius = between(24f, 72f)
            add(x, y, radius, colors[index % colors.size], 8, 10)
            repeat(6) {
                val bx = x + between(-radius, radius)
                val by = y + between(-radius, radius)
                add(bx, by, between(4f, 18f), colors[index % colors.size], 9, 6)
                repeat(3) {
                    add(bx + between(-13f, 13f), by + between(-13f, 13f),
                        between(0.9f, 3.2f), colors[index % colors.size], 11, 3)
                }
            }
        }
        val bitmap = Bitmap.createBitmap(key.width, key.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        // Interleave colors, preserving luminous overlaps and shared base contours.
        repeat(16) { layer ->
            for (glaze in glazes) {
                if (layer >= glaze.layers) continue
                val boundary = deform(glaze.boundary, random, if (glaze.radius > 20f) 3 else 2)
                val path = Path().apply {
                    fillType = Path.FillType.EVEN_ODD
                    moveTo(boundary[0].x, boundary[0].y)
                    for (i in 1 until boundary.size) lineTo(boundary[i].x, boundary[i].y)
                    close()
                }
                // Small dry-paper gaps interrupt individual deposits, never erase
                // previous paint. Different gaps in each layer accumulate soft grain.
                if (glaze.radius > 20f) {
                    val holes = Path()
                    repeat(14) {
                        val hx = glaze.x + between(-glaze.radius * 0.7f, glaze.radius * 0.7f)
                        val hy = glaze.y + between(-glaze.radius * 0.7f, glaze.radius * 0.7f)
                        holes.addCircle(hx, hy, between(1f, 5f), Path.Direction.CW)
                    }
                    // Difference cannot add pigment outside the wash, unlike simply
                    // appending holes to an EVEN_ODD boundary. One operation per layer.
                    path.op(holes, Path.Op.DIFFERENCE)
                }
                paint.color = glaze.color
                paint.alpha = glaze.opacity
                canvas.drawPath(path, paint)
            }
        }
        return bitmap
    }

    private fun deform(input: List<Vertex>, random: Random, rounds: Int): List<Vertex> {
        var boundary = input
        repeat(rounds) {
            val next = ArrayList<Vertex>(boundary.size * 2)
            for (i in boundary.indices) {
                val a = boundary[i]
                val b = boundary[(i + 1) % boundary.size]
                val dx = b.x - a.x
                val dy = b.y - a.y
                val perpendicular = random.nextGaussian().toFloat() * a.variance
                val along = random.nextGaussian().toFloat() * a.variance * 0.32f
                next += a.copy(variance = a.variance * 0.88f)
                next += Vertex((a.x + b.x) * 0.5f - dy * perpendicular + dx * along,
                    (a.y + b.y) * 0.5f + dx * perpendicular + dy * along,
                    a.variance * (0.72f + random.nextFloat() * 0.28f))
            }
            boundary = next
        }
        return boundary
    }
}
