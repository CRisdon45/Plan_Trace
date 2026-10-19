package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
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
        // Finishing now occupies shape-guided edge bands, not the whole rectangle.
        // Keep a distinct lift and dark tail without requiring dense interior stamps.
        assertTrue("Texture must materially lift existing washes", lifted > shadow.size * .03)
        assertTrue("Texture must also deposit smaller paint forms", repainted > shadow.size * .10)
        assertTrue("Final accents must create a distinct dark range", finalDark > textureDark + shadow.size * .01)
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

    private fun shapeRender(path: Path): Bitmap = Bitmap.createBitmap(1150, 850, Bitmap.Config.ARGB_8888).also {
        val canvas = Canvas(it)
        canvas.clipPath(path)
        NorthstarGroundMaterials.draw(canvas, "shape-study", path, RectF(50f,50f,1050f,750f),
            SurfaceMaterial.TURF, 1f, ScaleCalibration(true,200f,1f,"m"))
    }
    @Test fun `same bounds shape edit changes paint near the new inner edge`() {
        val rectangle = Path().apply { addRect(50f,50f,1050f,750f,Path.Direction.CW) }
        val concave = Path().apply {
            moveTo(50f,50f); lineTo(1050f,50f); lineTo(1050f,750f)
            lineTo(550f,750f); lineTo(550f,400f); lineTo(50f,400f); close()
        }
        val first = shapeRender(rectangle)
        val edited = shapeRender(concave)
        fun mean(bitmap: Bitmap): Double {
            var sum=0.0; var count=0
            for(y in 355..385 step 3)for(x in 250..450 step 3) {
                val c=bitmap.getPixel(x,y);sum+=(Color.red(c)+Color.green(c)+Color.blue(c))/3.0;count++
            }
            return sum/count
        }
        assertTrue("New inner boundary must guide pigment, not just clip the old rectangle", mean(edited)<mean(first)-5)
        NorthstarGroundMaterials.clearCache()
        assertTrue("Edited shape must reproduce after eviction", edited.sameAs(shapeRender(concave)))
        assertTrue("Returning to original geometry must reproduce its paint", first.sameAs(shapeRender(rectangle)))
        save(first,"grass-shape-rectangle.png");save(edited,"grass-shape-concave.png")
    }
    @Test fun `curves holes and narrow turns retain shape guided paint and exact clipping`() {
        val curved = Path().apply {
            moveTo(50f,350f); cubicTo(50f,50f,350f,50f,550f,130f)
            cubicTo(850f,0f,1050f,150f,1050f,400f)
            cubicTo(1050f,750f,750f,750f,550f,640f)
            cubicTo(250f,850f,50f,680f,50f,350f); close()
        }
        val courtyard = Path().apply {
            fillType=Path.FillType.EVEN_ODD
            addRect(50f,50f,1050f,750f,Path.Direction.CW)
            addRoundRect(RectF(250f,220f,850f,580f),55f,55f,Path.Direction.CW)
        }
        val narrow = Path().apply {
            moveTo(50f,50f);lineTo(1050f,50f);lineTo(1050f,150f)
            lineTo(150f,150f);lineTo(150f,750f);lineTo(50f,750f);close()
        }
        for((name,path) in listOf("curved" to curved,"courtyard" to courtyard,"narrow" to narrow)) {
            val actual=shapeRender(path)
            assertEquals("Outside shape must stay transparent",0,actual.getPixel(15,15))
            if(name!="curved")assertEquals("Hole or notch must stay transparent",0,actual.getPixel(500,400))
            if(name=="narrow")assertTrue("Painting must reach a narrow turn",Color.alpha(actual.getPixel(100,100))>0)
            NorthstarGroundMaterials.clearCache()
            assertTrue(actual.sameAs(shapeRender(path)))
            save(actual,"grass-shape-$name.png")
        }
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
