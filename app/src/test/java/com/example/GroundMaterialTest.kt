package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.example.data.ProjectJsonConverter
import com.example.engine.NorthstarGroundMaterials
import com.example.engine.NorthstarGrassPaint
import com.example.engine.WatercolorRenderer
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GroundMaterialTest {
    @Test fun `texture lifts paint and final detail adds a distinct dark range`() {
        var seed = 0xCBF29CE484222325uL.toLong()
        for (c in "ground-study") seed = (seed xor c.code.toLong()) * 0x100000001B3L
        val stages = linkedMapOf<String, IntArray>()
        NorthstarGrassPaint.render(1024, 717, seed) { name, width, height, pixels ->
            if (name.startsWith("03") || name.startsWith("04") || name.startsWith("05")) {
                stages[name] = pixels
                val bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
                save(bitmap, "grass-stage-$name.png")
                bitmap.recycle()
            }
        }
        val shadow = stages.getValue("03-drying-fronts")
        val texture = stages.getValue("04-lifted-texture")
        val finished = stages.getValue("05-final-grass")
        fun light(c: Int) = (Color.red(c) + Color.green(c) + Color.blue(c)) / 3f
        var lifted = 0; var repainted = 0; var textureDark = 0; var finalDark = 0
        for (i in shadow.indices) {
            if (light(texture[i]) - light(shadow[i]) > 12) lifted++
            if (light(shadow[i]) - light(texture[i]) > 8) repainted++
            if (light(texture[i]) < 110) textureDark++
            if (light(finished[i]) < 110) finalDark++
        }
        // These prove distinct paint operations, not a subjective quality score.
        // 110 is an olive-black watercolor tail, not a requirement for burnt near-black fills.
        assertTrue("Texture must materially lift existing washes", lifted > shadow.size * .07)
        assertTrue("Texture must also deposit smaller paint forms", repainted > shadow.size * .10)
        assertTrue("Final accents must create a distinct dark range", finalDark > textureDark + shadow.size * .02)
    }

    private fun element(material: SurfaceMaterial, id: String = "ground-study") = RectangleElement(
        id = id, layerId = "base", left = 50f, top = 50f, right = 1050f, bottom = 750f,
        strokeWidth = 1.7f, material = material, style = StrokeStyle.WATERCOLOR_WASH)
    private fun render(element: VectorElement, scale: ScaleCalibration = ScaleCalibration(true, 200f, 1f, "m"),
                       alpha: Float = 1f): Bitmap = Bitmap.createBitmap(1150, 850, Bitmap.Config.ARGB_8888).also {
        WatercolorRenderer.render(Canvas(it), element, alpha, scale, false)
    }
    private fun save(bitmap: Bitmap, name: String) {
        val dir = File("build/reports/northstar-watercolor").apply { mkdirs() }
        val paper = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        paper.eraseColor(Color.rgb(249,247,238))
        Canvas(paper).drawBitmap(bitmap, 0f, 0f, null)
        File(dir, name).outputStream().use { paper.compress(Bitmap.CompressFormat.PNG, 100, it) }
        paper.recycle()
    }

    @Test fun `ground washes survive cold caches translation and concave clipping without data changes`() {
        for (material in listOf(SurfaceMaterial.TURF, SurfaceMaterial.PAVING)) {
            val original = element(material)
            val json = ProjectJsonConverter.serializeElements(listOf(original))
            val first = render(original)
            NorthstarGroundMaterials.clearCache()
            assertTrue(first.sameAs(render(original)))
            assertFalse(first.sameAs(render(original.copy(id = "neighbor"))))
            assertEquals(json, ProjectJsonConverter.serializeElements(listOf(original)))
            val moved = render(original.copy(left = 70f, top = 70f, right = 1070f, bottom = 770f))
            for (y in 80 until 700 step 11) for (x in 80 until 1000 step 11) {
                val a = first.getPixel(x, y); val b = moved.getPixel(x + 20, y + 20)
                val delta = abs(Color.red(a) - Color.red(b)) + abs(Color.green(a) - Color.green(b)) + abs(Color.blue(a) - Color.blue(b))
                assertTrue("translated pigment/joints must remain registered: $delta", delta <= 3)
            }
            val concave = PolylineElement(id = original.id, layerId = "base", isClosed = true,
                points = listOf(Point2D(50f,50f), Point2D(1050f,50f), Point2D(1050f,750f),
                    Point2D(550f,750f), Point2D(550f,400f), Point2D(50f,400f)),
                material = material, style = StrokeStyle.WATERCOLOR_WASH)
            val clipped = render(concave)
            for (y in 430 until 720 step 7) for (x in 80 until 520 step 7) assertEquals(0, clipped.getPixel(x,y))
            assertEquals(0, render(original, alpha = 0f).getPixel(350,350))
            if (material == SurfaceMaterial.TURF) assertEquals(
                "Baked grass underpainting must receive surface opacity exactly once", 128,
                Color.alpha(render(original, alpha = .5f).getPixel(350,350)))
            assertEquals(material.fill.toInt(), render(original.copy(style = StrokeStyle.INK)).getPixel(350,350))
            save(clipped, "northstar-${material.name.lowercase()}-concave.png")
        }
    }

    @Test fun `grass paint stays fixed across zoom pan and cache eviction`() {
        val grass = element(SurfaceMaterial.TURF)
        val original = render(grass)
        fun view(zoom: Float): Bitmap = Bitmap.createBitmap(1150, 850, Bitmap.Config.ARGB_8888).also {
            val canvas = Canvas(it)
            canvas.translate(43f, 27f)
            canvas.scale(zoom, zoom)
            WatercolorRenderer.render(canvas, grass, 1f, ScaleCalibration(true, 200f, 1f, "m"), false)
        }
        for (zoom in listOf(.5f, 1.5f)) {
            val warm = view(zoom)
            NorthstarGroundMaterials.clearCache()
            val cold = view(zoom)
            assertTrue("Zoomed paint must reproduce after cache eviction", warm.sameAs(cold))
            assertTrue("Returning from pan and zoom must preserve paint", original.sameAs(render(grass)))
            save(warm, "northstar-turf-zoom-${if (zoom < 1f) "half" else "detail"}.png")
            warm.recycle(); cold.recycle()
        }
        original.recycle()
    }

    @Test fun `material studies retain broad glazes and minute detail at actual renderer output`() {
        val report = StringBuilder()
        for (material in listOf(SurfaceMaterial.TURF, SurfaceMaterial.PAVING)) {
            val bitmap = render(element(material))
            if (material == SurfaceMaterial.PAVING) {
                var warm = 0; var samples = 0
                for (y in 80..720 step 3) for (x in 80..1020 step 3) {
                    val c = bitmap.getPixel(x,y)
                    if (Color.red(c) >= Color.green(c) && Color.green(c) >= Color.blue(c)) warm++
                    samples++
                }
                assertTrue("neutral stone glazes must not drift pink or green while accumulating", warm > samples * .97)
            }
            fun luminance(x: Int, y: Int): Float {
                val c = bitmap.getPixel(x,y)
                return (Color.red(c) + Color.green(c) + Color.blue(c)) / 3f
            }
            val means = mutableListOf<Float>()
            for (y in 80..620 step 60) for (x in 80..920 step 60) {
                var sum = 0f
                for (dy in 0 until 60) for (dx in 0 until 60) sum += luminance(x+dx,y+dy)
                means += sum / 3600f
            }
            var detail = 0f; var count = 0
            for (y in 80..720 step 3) for (x in 80..1020 step 3) {
                detail += abs(luminance(x,y) - (luminance(x-4,y)+luminance(x+4,y)+luminance(x,y-4)+luminance(x,y+4))/4f)
                count++
            }
            val range = means.maxOrNull()!! - means.minOrNull()!!
            val residual = detail / count
            report.append("$material: 60px block range=$range; 4px residual=$residual\n")
            assertTrue("broad material variation must survive averaging", range > if (material == SurfaceMaterial.TURF) 12f else 3f)
            assertTrue("fine detail cannot be only a soft gradient", residual > .4f)
            save(bitmap, "northstar-${material.name.lowercase()}-detail.png")
        }
        File("build/reports/northstar-watercolor/ground-material-metrics.txt").writeText(report.toString())
    }

    @Test fun `travertine joints use equivalent physical calibration in metres and feet`() {
        val paving = element(SurfaceMaterial.PAVING)
        val metric = render(paving, ScaleCalibration(true, 200f, 1f, "m"))
        val imperial = render(paving, ScaleCalibration(true, 60.96f, 1f, "ft"))
        for (y in 80..720 step 7) for (x in 80..1020 step 7) {
            val a = metric.getPixel(x,y); val b = imperial.getPixel(x,y)
            assertTrue(abs(Color.red(a)-Color.red(b)) + abs(Color.green(a)-Color.green(b)) + abs(Color.blue(a)-Color.blue(b)) <= 3)
        }
        assertFalse("different scale must change physical joint spacing", metric.sameAs(
            render(paving, ScaleCalibration(true, 100f, 1f, "m"))))
    }
}
