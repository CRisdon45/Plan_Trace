package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import com.example.data.ProjectJsonConverter
import com.example.engine.NorthstarWatercolorField
import com.example.engine.NorthstarWaterDetails
import com.example.engine.NorthstarPolygonWash
import com.example.engine.WatercolorRenderer
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class SurfaceMaterialTest {
    private fun rectangle() = RectangleElement(id = "surface", layerId = "base", left = 10f, top = 10f,
        right = 90f, bottom = 90f, strokeColor = 0xFFFF0000, strokeWidth = 2f)

    @Test fun `assignment preserves geometry and original style through storage`() {
        val original = rectangle()
        val assigned = original.withMaterial(SurfaceMaterial.WATER)
        val restored = ProjectJsonConverter.deserializeElements(ProjectJsonConverter.serializeElements(listOf(assigned))).single()
        assertEquals(assigned, restored)
        assertEquals(original, restored.withMaterial(null))
        assertEquals(original.boundingBox(), restored.boundingBox())
    }

    @Test fun `legacy and unknown materials remain usable`() {
        val json = ProjectJsonConverter.serializeElements(listOf(rectangle()))
        assertEquals(rectangle(), ProjectJsonConverter.deserializeElements(json).single())
        val future = json.replace("\"type\":", "\"material\":\"FUTURE\",\"materialVersion\":9,\"type\":")
        assertEquals(rectangle(), ProjectJsonConverter.deserializeElements(future).single())
    }

    @Test fun `graphic fill is deterministic and contained by the boundary`() {
        fun render(): Bitmap {
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            WatercolorRenderer.render(Canvas(bitmap), rectangle().withMaterial(SurfaceMaterial.WATER), 1f, ScaleCalibration(), false)
            return bitmap
        }
        val first = render()
        assertEquals(SurfaceMaterial.WATER.fill.toInt(), first.getPixel(50, 50))
        assertEquals(0, first.getPixel(0, 0))
        assertTrue(first.sameAs(render()))
    }

    @Test fun `northstar water depth is deterministic clipped and sheet directed`() {
        fun render(left:Float=10f,top:Float=10f,right:Float=110f,bottom:Float=110f): Bitmap {
            val bitmap = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888)
            val element = rectangle().copy(left=left,top=top,right=right,bottom=bottom,
                material=SurfaceMaterial.WATER,style=StrokeStyle.WATERCOLOR_WASH)
            WatercolorRenderer.render(Canvas(bitmap),element,1f,ScaleCalibration(),false)
            return bitmap
        }
        val first=render()
        assertEquals(0,first.getPixel(0,0))
        assertNotEquals(first.getPixel(35,35),first.getPixel(85,85))
        assertTrue(first.sameAs(render()))
        val moved=render(30f,30f,130f,130f)
        // Subpixel antialias coverage can round a color channel after translation.
        // Sample throughout the interior, keeping the allowed total RGB error tiny.
        for (y in 25 until 96 step 5) for (x in 25 until 96 step 5) {
            val original = first.getPixel(x, y)
            val translated = moved.getPixel(x + 20, y + 20)
            val channelDelta = abs(android.graphics.Color.alpha(original) - android.graphics.Color.alpha(translated)) +
                abs(android.graphics.Color.red(original) - android.graphics.Color.red(translated)) +
                abs(android.graphics.Color.green(original) - android.graphics.Color.green(translated)) +
                abs(android.graphics.Color.blue(original) - android.graphics.Color.blue(translated))
            assertTrue("translation should preserve the interior wash within raster tolerance", channelDelta <= 3)
        }
    }

    @Test fun `northstar paving remains quieter than water`() {
        fun render(material:SurfaceMaterial): Bitmap {
            val bitmap=Bitmap.createBitmap(120,120,Bitmap.Config.ARGB_8888)
            val element=rectangle().copy(right=110f,bottom=110f,material=material,style=StrokeStyle.WATERCOLOR_WASH)
            WatercolorRenderer.render(Canvas(bitmap),element,1f,ScaleCalibration(),false)
            return bitmap
        }
        fun colorDistance(a:Int,b:Int)=abs(android.graphics.Color.red(a)-android.graphics.Color.red(b))+
            abs(android.graphics.Color.green(a)-android.graphics.Color.green(b))+
            abs(android.graphics.Color.blue(a)-android.graphics.Color.blue(b))
        val water=render(SurfaceMaterial.WATER)
        val paving=render(SurfaceMaterial.PAVING)
        assertTrue(colorDistance(water.getPixel(85,85),SurfaceMaterial.WATER.fill.toInt()) >
            colorDistance(paving.getPixel(85,85),SurfaceMaterial.PAVING.fill.toInt()))
        assertEquals(0,water.getPixel(0,0));assertEquals(0,paving.getPixel(0,0))
    }

    @Test fun `northstar water carries sparse bright caustics over a darker layered wash`() {
        val bitmap = Bitmap.createBitmap(220, 140, Bitmap.Config.ARGB_8888)
        val element = rectangle().copy(
            left = 10f,
            top = 10f,
            right = 210f,
            bottom = 130f,
            material = SurfaceMaterial.WATER,
            style = StrokeStyle.WATERCOLOR_WASH,
        )
        WatercolorRenderer.render(Canvas(bitmap), element, 1f, ScaleCalibration(), false)

        fun brightness(color: Int) = android.graphics.Color.red(color) +
            android.graphics.Color.green(color) + android.graphics.Color.blue(color)
        val interior = buildList<Int> {
            for (y in 24 until 116) for (x in 28 until 192) add(bitmap.getPixel(x, y))
        }
        val medianBrightness = interior.map(::brightness).sorted()[interior.size / 2]
        val bright = interior.count { brightness(it) > medianBrightness + 55 }
        val dark = interior.count { brightness(it) < medianBrightness - 20 }
        val blue = interior.count {
            android.graphics.Color.blue(it) > android.graphics.Color.red(it) + 40 &&
                android.graphics.Color.blue(it) > android.graphics.Color.green(it) + 5
        }

        assertTrue("expected visible caustic highlights", bright > 20)
        assertTrue("caustics should not cover the field", bright < interior.size / 2)
        assertTrue("expected confidently darker wash masses", dark > interior.size / 8)
        assertTrue("expected layered color variation", interior.distinct().size > 80)
        assertTrue("Northstar water should be blue, not pale mint", blue > interior.size * 3 / 4)
        assertEquals(0, bitmap.getPixel(0, 0))
    }

    @Test fun `blue water and caustics survive cold caches without changing saved data`() {
        val element = rectangle().copy(right = 210f, bottom = 130f,
            material = SurfaceMaterial.WATER, style = StrokeStyle.WATERCOLOR_WASH)
        val saved = ProjectJsonConverter.serializeElements(listOf(element))
        fun render(): Bitmap = Bitmap.createBitmap(230, 150, Bitmap.Config.ARGB_8888).also {
            WatercolorRenderer.render(Canvas(it), element, 1f, ScaleCalibration(), false)
        }
        val first = render()
        NorthstarWaterDetails.clearCache()
        NorthstarWatercolorField.clearCache()
        NorthstarPolygonWash.clearCache()
        assertTrue(first.sameAs(render()))
        assertEquals(saved, ProjectJsonConverter.serializeElements(listOf(element)))
        java.io.File("build/reports/northstar-watercolor").mkdirs()
        java.io.File("build/reports/northstar-watercolor/northstar-rectangular-water.png")
            .outputStream().use { first.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun `blue water details stay clipped to concave boundaries`() {
        val element = PolylineElement(id = "concave-water", layerId = "base",
            points = listOf(Point2D(10f, 10f), Point2D(210f, 10f), Point2D(210f, 130f),
                Point2D(110f, 130f), Point2D(110f, 70f), Point2D(10f, 70f)),
            isClosed = true, material = SurfaceMaterial.WATER, style = StrokeStyle.WATERCOLOR_WASH)
        val bitmap = Bitmap.createBitmap(230, 150, Bitmap.Config.ARGB_8888)
        WatercolorRenderer.render(Canvas(bitmap), element, 1f, ScaleCalibration(), false)
        for (y in 80 until 125) for (x in 20 until 100) assertEquals(0, bitmap.getPixel(x, y))
        assertTrue(android.graphics.Color.blue(bitmap.getPixel(160, 90)) >
            android.graphics.Color.red(bitmap.getPixel(160, 90)) + 40)
    }

    @Test fun `caustic light has fine and broad crests while preserving open water`() {
        fun render(id: String): Bitmap = Bitmap.createBitmap(1000, 600, Bitmap.Config.ARGB_8888).also {
            NorthstarWaterDetails.drawCaustics(Canvas(it), id, android.graphics.RectF(0f, 0f, 1000f, 600f), 1f)
        }
        val first = render("caustic-study")
        NorthstarWaterDetails.clearCache()
        assertTrue(first.sameAs(render("caustic-study")))
        assertFalse(first.sameAs(render("neighbor-caustics")))
        val pixels = IntArray(1000 * 600)
        first.getPixels(pixels, 0, 1000, 0, 0, 1000, 600)
        val core = pixels.count { android.graphics.Color.alpha(it) > 140 }
        val crests = pixels.count { android.graphics.Color.alpha(it) > 220 }
        val widths = mutableListOf<Int>()
        for (y in 30 until 570 step 13) {
            var run = 0
            for (x in 20 until 980) {
                if (android.graphics.Color.alpha(first.getPixel(x, y)) > 140) run++
                else if (run > 0) { widths += run; run = 0 }
            }
        }
        val output = java.io.File("build/reports/northstar-watercolor").apply { mkdirs() }
        java.io.File(output, "caustic-light-only.png").outputStream().use {
            first.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        java.io.File(output, "caustic-light-metrics.txt").writeText(
            "core pixels=$core/${pixels.size}\ncrest pixels=$crests\nscan widths=${widths.sorted()}\n")
        assertTrue("light should remain fine enough to expose the paint", core in pixels.size / 40..pixels.size / 4)
        assertTrue("some concentrated crests should be bright", crests > 100)
        assertTrue("retain fine threads", widths.count { it in 1..2 } > 20)
        assertTrue("retain broader folds and confluences", widths.count { it in 4..9 } > 20)
    }

    @Test fun `polygon glazes retain broad washes and fine blooms independently of caustics`() {
        fun wash(id: String): Bitmap = Bitmap.createBitmap(768, 480, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(android.graphics.Color.rgb(120, 199, 221))
            NorthstarPolygonWash.draw(Canvas(it), id, android.graphics.RectF(0f, 0f, 768f, 480f), 1f)
        }
        val first = wash("polygon-water-evidence")
        NorthstarPolygonWash.clearCache()
        assertTrue("cold paint must be identical", first.sameAs(wash("polygon-water-evidence")))
        assertFalse("different water objects need individual washes", first.sameAs(wash("neighbor-water")))
        fun brightness(x: Int, y: Int): Float {
            val color = first.getPixel(x, y)
            return (android.graphics.Color.red(color) + android.graphics.Color.green(color) +
                android.graphics.Color.blue(color)) / 3f
        }
        val broad = buildList<Float> {
            for (y in 0 until 480 step 48) for (x in 0 until 768 step 48) {
                var sum = 0f
                for (dy in 0 until 48) for (dx in 0 until 48) sum += brightness(x + dx, y + dy)
                add(sum / (48 * 48))
            }
        }
        var detail = 0f
        var count = 0
        for (y in 12 until 468 step 3) for (x in 12 until 756 step 3) {
            val neighbors = (brightness(x - 6, y) + brightness(x + 6, y) +
                brightness(x, y - 6) + brightness(x, y + 6)) * 0.25f
            detail += abs(brightness(x, y) - neighbors)
            count++
        }
        val broadRange = broad.maxOrNull()!! - broad.minOrNull()!!
        val meanDetail = detail / count
        val output = java.io.File("build/reports/northstar-watercolor").apply { mkdirs() }
        java.io.File(output, "polygon-wash-only.png").outputStream().use {
            first.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        java.io.File(output, "polygon-wash-metrics.txt").writeText(
            "48px block mean range=$broadRange\n6px local residual=$meanDetail\n")
        assertTrue("broad variations must survive averaging, not only grain", broadRange > 20f)
        assertTrue("small pigment blooms must survive without white caustics", meanDetail > 1.5f)

        val rendered = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
        rendered.eraseColor(android.graphics.Color.rgb(249, 247, 238))
        WatercolorRenderer.render(Canvas(rendered), rectangle().copy(
            id = "polygon-water-evidence", left = 70f, top = 70f, right = 1130f, bottom = 730f,
            material = SurfaceMaterial.WATER, style = StrokeStyle.WATERCOLOR_WASH,
        ), 1f, ScaleCalibration(), false)
        java.io.File(output, "northstar-rectangular-water-detail.png").outputStream().use {
            rendered.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun `bounded watercolor field is seeded settles pigment and preserves mass`() {
        val first = NorthstarWatercolorField.simulateForEvidence("pool:outline", 72, 48, SurfaceMaterial.WATER.outline.toInt())
        val repeated = NorthstarWatercolorField.simulateForEvidence("pool:outline", 72, 48, SurfaceMaterial.WATER.outline.toInt())
        val neighbor = NorthstarWatercolorField.simulateForEvidence("spa:outline", 72, 48, SurfaceMaterial.WATER.outline.toInt())

        assertArrayEquals(first.pixels, repeated.pixels)
        assertFalse(first.pixels.contentEquals(neighbor.pixels))
        assertTrue(first.depositedPigmentMass > 0f)
        assertTrue(first.mobilePigmentMass < first.initialPigmentMass)
        assertEquals(
            first.initialPigmentMass,
            first.mobilePigmentMass + first.depositedPigmentMass,
            first.initialPigmentMass * 0.12f,
        )
        val alphaValues = first.pixels.map { android.graphics.Color.alpha(it) }
        assertTrue(alphaValues.distinct().size > 24)
        assertTrue(alphaValues.maxOrNull()!! - alphaValues.minOrNull()!! > 36)
    }

    @Test fun `watercolor simulation never paints outside its material mask`() {
        val width = 64
        val height = 40
        val mask = FloatArray(width * height) { index -> if (index % width < width / 2) 1f else 0f }
        val field = NorthstarWatercolorField.simulateForEvidence(
            "masked-pool",
            width,
            height,
            SurfaceMaterial.WATER.outline.toInt(),
            mask,
        )
        for (y in 0 until height) for (x in 0 until width) {
            val alpha = android.graphics.Color.alpha(field.pixels[y * width + x])
            if (x >= width / 2) assertEquals(0, alpha)
        }
        assertTrue(field.pixels.any { android.graphics.Color.alpha(it) > 0 })
    }

    @Test fun `open paths and notes cannot be assigned surfaces from the interface`() {
        assertFalse(LineElement(layerId = "base", start = Point2D(0f, 0f), end = Point2D(50f, 0f)).supportsSurface())
        assertFalse(PolylineElement(layerId = "base", points = listOf(Point2D(0f, 0f), Point2D(10f, 10f))).supportsSurface())
        assertTrue(rectangle().supportsSurface())
    }
    @Test fun `editable example has valid layer ownership and an exact sized pool`() {
        val project = LandscapeExample.create()
        assertEquals(project.elements.size, project.elements.map { it.id }.toSet().size)
        assertTrue(project.elements.all { el -> project.layers.any { it.id == el.layerId } })
        val pool = project.elements.first { it.id == "pool" } as RectangleElement
        assertEquals(400f, pool.width, 0f)
        assertEquals(240f, pool.height, 0f)
        assertEquals(project.elements, ProjectJsonConverter.deserializeElements(ProjectJsonConverter.serializeElements(project.elements)))
        assertNotEquals(project.id, LandscapeExample.create().id)
    }

}
