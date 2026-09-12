package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import com.example.data.ProjectJsonConverter
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
        assertEquals(first.getPixel(35,35),moved.getPixel(55,55))
        assertEquals(first.getPixel(85,85),moved.getPixel(105,105))
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
