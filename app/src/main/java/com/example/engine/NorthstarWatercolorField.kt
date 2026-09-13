package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.LruCache
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A bounded, deterministic watercolor field for presentation-only material rendering.
 *
 * This is an independently implemented, deliberately small simulation inspired by the
 * water/pigment/deposition model described by Curtis et al. It is not project authority:
 * the resulting raster is always clipped by the caller's exact vector path.
 */
internal object NorthstarWatercolorField {
    private const val STYLE_VERSION = 2
    private const val MAX_GRID_SIDE = 128
    private const val MIN_GRID_SIDE = 36
    private const val SETTLING_STEPS = 32
    private const val CACHE_BYTES = 12 * 1024 * 1024

    private data class CacheKey(
        val stableId: String,
        val width: Int,
        val height: Int,
        val geometryFingerprint: Long,
        val pigmentColor: Int,
        val styleVersion: Int,
    )

    internal data class FieldEvidence(
        val width: Int,
        val height: Int,
        val mask: FloatArray,
        val initialPigmentMass: Float,
        val mobilePigmentMass: Float,
        val depositedPigmentMass: Float,
        val pixels: IntArray,
    )

    private val cache = object : LruCache<CacheKey, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: CacheKey, value: Bitmap): Int = value.allocationByteCount
    }

    fun draw(
        canvas: Canvas,
        stableId: String,
        geometryFingerprint: Long,
        path: Path,
        bounds: RectF,
        pigmentColor: Int,
        alpha: Float,
    ) {
        if (bounds.width() <= 0f || bounds.height() <= 0f || alpha <= 0f) return
        val (width, height) = gridSize(bounds)
        val key = CacheKey(stableId, width, height, geometryFingerprint, pigmentColor, STYLE_VERSION)
        val bitmap = synchronized(cache) {
            cache.get(key)
        } ?: run {
            val mask = rasterMask(path, bounds, width, height)
            if (mask.none { it > 0f }) return
            val generated = simulate(stableId, width, height, mask, pigmentColor).bitmap
            synchronized(cache) { cache.put(key, generated) }
            generated
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.alpha = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
            isDither = true
        }
        canvas.drawBitmap(bitmap, null, bounds, paint)
    }

    internal fun simulateForEvidence(
        stableId: String,
        width: Int,
        height: Int,
        pigmentColor: Int,
        mask: FloatArray = FloatArray(width * height) { 1f },
    ): FieldEvidence = simulate(stableId, width, height, mask.copyOf(), pigmentColor).evidence

    internal fun clearCache() = synchronized(cache) { cache.evictAll() }

    private data class Simulation(val bitmap: Bitmap, val evidence: FieldEvidence)

    private fun simulate(
        stableId: String,
        width: Int,
        height: Int,
        mask: FloatArray,
        pigmentColor: Int,
    ): Simulation {
        require(width in 2..MAX_GRID_SIDE && height in 2..MAX_GRID_SIDE)
        require(mask.size == width * height)

        val count = width * height
        val random = StableRandom(stableSeed(stableId))
        val finePaper = FloatArray(count) { random.nextFloat() }
        var paper = finePaper.copyOf()
        repeat(4) { paper = smoothed(paper, width, height, mask, 0.58f) }

        var wetness = FloatArray(count)
        var mobile = FloatArray(count)
        var deposited = FloatArray(count)
        val velocityX = FloatArray(count)
        val velocityY = FloatArray(count)

        for (y in 0 until height) {
            val v = y.toFloat() / max(1, height - 1)
            for (x in 0 until width) {
                val i = index(x, y, width)
                if (mask[i] <= 0f) continue
                val u = x.toFloat() / max(1, width - 1)
                val coarseGrain = paper[i] - 0.5f
                val fineGrain = finePaper[i] - 0.5f
                wetness[i] = mask[i] * (
                    0.64f + 0.17f * (1f - v) + coarseGrain * 0.13f + fineGrain * 0.035f
                )
                mobile[i] = mask[i] * (
                    0.14f + 0.17f * (u * 0.32f + v * 0.68f) + coarseGrain * 0.09f + fineGrain * 0.025f
                )

                val px = paper[index((x + 1).coerceAtMost(width - 1), y, width)] -
                    paper[index((x - 1).coerceAtLeast(0), y, width)]
                val py = paper[index(x, (y + 1).coerceAtMost(height - 1), width)] -
                    paper[index(x, (y - 1).coerceAtLeast(0), width)]
                velocityX[i] = 0.14f + py * 0.48f
                velocityY[i] = 0.10f - px * 0.48f
            }
        }

        // Overlapping elliptical charges create deliberate wash masses rather than a
        // uniformly noisy tint. A soft annular charge gives a few restrained backruns.
        repeat(6) {
            val centerX = random.nextFloat() * (width - 1)
            val centerY = random.nextFloat() * (height - 1)
            val radiusX = width * (0.13f + random.nextFloat() * 0.19f)
            val radiusY = height * (0.11f + random.nextFloat() * 0.18f)
            val amount = 0.10f + random.nextFloat() * 0.14f
            for (y in 0 until height) for (x in 0 until width) {
                val i = index(x, y, width)
                if (mask[i] <= 0f) continue
                val dx = (x - centerX) / radiusX
                val dy = (y - centerY) / radiusY
                val distance = sqrt(dx * dx + dy * dy)
                val centerCharge = (1f - distance).coerceIn(0f, 1f)
                val backrun = (1f - abs(distance - 0.72f) / 0.20f).coerceIn(0f, 1f)
                mobile[i] += (centerCharge * centerCharge * amount + backrun * amount * 0.18f) * mask[i]
                wetness[i] = max(wetness[i], max(centerCharge * 0.90f, backrun * 0.76f) * mask[i])
            }
        }

        val initialMass = mobile.sum()
        val edgePhaseX = stablePhase(stableId, 1)
        val edgePhaseY = stablePhase(stableId, 2)
        val edgePhaseDiagonal = stablePhase(stableId, 3)
        val edgeDeposition = FloatArray(count)
        for (y in 0 until height) for (x in 0 until width) {
            val i = index(x, y, width)
            if (mask[i] <= 0f) continue
            val edgeRhythm = (
                0.50f +
                    sin(x * 0.19f + edgePhaseX) * 0.22f +
                    sin(y * 0.27f + edgePhaseY) * 0.18f +
                    sin((x + y) * 0.11f + edgePhaseDiagonal) * 0.16f
                ).coerceIn(0f, 1f)
            edgeDeposition[i] = boundaryFactor(mask, width, height, x, y) *
                ((edgeRhythm - 0.34f) / 0.42f).coerceIn(0f, 1f)
        }
        repeat(SETTLING_STEPS) {
            val nextWetness = FloatArray(count)
            val nextMobile = FloatArray(count)
            val nextDeposited = deposited.copyOf()
            for (y in 0 until height) for (x in 0 until width) {
                val i = index(x, y, width)
                val coverage = mask[i]
                if (coverage <= 0f) continue

                val localWetness = wetness[i]
                val advected = bilinear(
                    mobile,
                    width,
                    height,
                    x - velocityX[i] * (0.45f + localWetness),
                    y - velocityY[i] * (0.45f + localWetness),
                )
                val neighborPigment = neighborAverage(mobile, mask, width, height, x, y)
                var carried = advected + (neighborPigment - advected) * (0.055f + localWetness * 0.045f)

                val neighborWetness = neighborAverage(wetness, mask, width, height, x, y)
                val paperPull = (paper[i] - 0.45f).coerceIn(0f, 0.55f)
                val wet = (localWetness + (neighborWetness - localWetness) * (0.07f + paperPull * 0.04f)) * 0.965f
                nextWetness[i] = wet.coerceIn(0f, 1f) * coverage

                val drying = (1f - wet).coerceIn(0f, 1f)
                val transferRate = 0.006f + drying * 0.029f + edgeDeposition[i] * 0.072f + paperPull * 0.012f
                val transfer = min(carried, carried * transferRate)
                carried -= transfer
                nextMobile[i] = max(0f, carried) * coverage
                nextDeposited[i] += transfer * coverage
            }
            wetness = nextWetness
            mobile = nextMobile
            deposited = nextDeposited
        }

        val pixels = IntArray(count)
        val red = Color.red(pigmentColor)
        val green = Color.green(pigmentColor)
        val blue = Color.blue(pigmentColor)
        for (i in 0 until count) {
            if (mask[i] <= 0f) continue
            val density = (mobile[i] * 0.68f + deposited[i] * 1.72f).coerceIn(0f, 1f)
            val coarseGrain = (paper[i] - 0.5f) * 13f
            val fineGrain = (finePaper[i] - 0.5f) * 10f
            val opacity = ((3f + density * 88f + coarseGrain + fineGrain) * mask[i])
                .roundToInt()
                .coerceIn(0, 82)
            pixels[i] = Color.argb(opacity, red, green, blue)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return Simulation(
            bitmap,
            FieldEvidence(
                width = width,
                height = height,
                mask = mask,
                initialPigmentMass = initialMass,
                mobilePigmentMass = mobile.sum(),
                depositedPigmentMass = deposited.sum(),
                pixels = pixels,
            ),
        )
    }

    private fun gridSize(bounds: RectF): Pair<Int, Int> {
        val aspect = bounds.width() / bounds.height()
        return if (aspect >= 1f) {
            MAX_GRID_SIDE to (MAX_GRID_SIDE / aspect).roundToInt().coerceIn(MIN_GRID_SIDE, MAX_GRID_SIDE)
        } else {
            (MAX_GRID_SIDE * aspect).roundToInt().coerceIn(MIN_GRID_SIDE, MAX_GRID_SIDE) to MAX_GRID_SIDE
        }
    }

    private fun rasterMask(path: Path, bounds: RectF, width: Int, height: Int): FloatArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val localPath = Path(path)
        val matrix = Matrix().apply {
            setRectToRect(bounds, RectF(0.5f, 0.5f, width - 0.5f, height - 0.5f), Matrix.ScaleToFit.FILL)
        }
        localPath.transform(matrix)
        Canvas(bitmap).drawPath(localPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        })
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()
        return FloatArray(pixels.size) { Color.alpha(pixels[it]) / 255f }
    }

    private fun smoothed(
        source: FloatArray,
        width: Int,
        height: Int,
        mask: FloatArray,
        centerWeight: Float,
    ): FloatArray {
        val output = FloatArray(source.size)
        for (y in 0 until height) for (x in 0 until width) {
            val i = index(x, y, width)
            if (mask[i] <= 0f) continue
            val around = neighborAverage(source, mask, width, height, x, y)
            output[i] = (source[i] * centerWeight + around * (1f - centerWeight)) * mask[i]
        }
        return output
    }

    private fun neighborAverage(
        source: FloatArray,
        mask: FloatArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
    ): Float {
        var total = 0f
        var weight = 0f
        for ((dx, dy) in NEIGHBORS) {
            val nx = (x + dx).coerceIn(0, width - 1)
            val ny = (y + dy).coerceIn(0, height - 1)
            val ni = index(nx, ny, width)
            val coverage = mask[ni]
            total += source[ni] * coverage
            weight += coverage
        }
        return if (weight > 0f) total / weight else 0f
    }

    private fun boundaryFactor(mask: FloatArray, width: Int, height: Int, x: Int, y: Int): Float {
        var open = 0f
        for ((dx, dy) in NEIGHBORS) {
            val nx = x + dx
            val ny = y + dy
            open += if (nx !in 0 until width || ny !in 0 until height) 1f
            else 1f - mask[index(nx, ny, width)]
        }
        return (open / NEIGHBORS.size).coerceIn(0f, 1f)
    }

    private fun bilinear(source: FloatArray, width: Int, height: Int, x: Float, y: Float): Float {
        val clampedX = x.coerceIn(0f, (width - 1).toFloat())
        val clampedY = y.coerceIn(0f, (height - 1).toFloat())
        val x0 = clampedX.toInt()
        val y0 = clampedY.toInt()
        val x1 = min(width - 1, x0 + 1)
        val y1 = min(height - 1, y0 + 1)
        val tx = clampedX - x0
        val ty = clampedY - y0
        val top = source[index(x0, y0, width)] * (1f - tx) + source[index(x1, y0, width)] * tx
        val bottom = source[index(x0, y1, width)] * (1f - tx) + source[index(x1, y1, width)] * tx
        return top * (1f - ty) + bottom * ty
    }

    private fun stableSeed(value: String): Int {
        var hash = 0x811C9DC5.toInt()
        for (character in value) {
            hash = (hash xor character.code) * 0x01000193
        }
        return hash xor (STYLE_VERSION * 0x5F356495)
    }

    private fun stablePhase(value: String, salt: Int): Float {
        val seed = stableSeed("$value:$salt")
        return (seed.toUInt().toDouble() / UInt.MAX_VALUE.toDouble() * Math.PI * 2.0).toFloat()
    }

    private fun index(x: Int, y: Int, width: Int): Int = y * width + x

    private class StableRandom(seed: Int) {
        private var state = if (seed == 0) 0x6D2B79F5 else seed

        fun nextFloat(): Float {
            var x = state
            x = x xor (x shl 13)
            x = x xor (x ushr 17)
            x = x xor (x shl 5)
            state = x
            return (x.toUInt().toDouble() / UInt.MAX_VALUE.toDouble()).toFloat()
        }
    }

    private val NEIGHBORS = arrayOf(0 to 0, -1 to 0, 1 to 0, 0 to -1, 0 to 1)
}
