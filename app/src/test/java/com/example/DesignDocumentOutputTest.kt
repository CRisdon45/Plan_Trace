package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.DesignJsonCodec
import com.example.data.ProjectJsonConverter
import com.example.export.DesignOutput
import com.example.export.DesignOutputSettings
import com.example.model.*
import com.example.model.design.*
import kotlinx.coroutines.runBlocking
import java.io.File
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignDocumentOutputTest {
    @Test fun `roundtrip preserves exact curves identity confidence and locks`() {
        val obj = DesignFixtures.document().objectById("pool").copy(name = "A \"quoted\" study", locked = true,
            confidence = GeometryConfidence.TRACED, sourceReference = "synthetic-source")
        val doc = ProjectDesign("roundtrip", listOf(obj), 9)
        val json = DesignJsonCodec.encode(doc)
        assertEquals(doc, DesignJsonCodec.decode(json))
        assertEquals(json, DesignJsonCodec.encode(DesignJsonCodec.decode(json)))
    }
    @Test fun `unsupported version and coordinate systems fail instead of misreading geometry`() {
        val json = DesignJsonCodec.encode(DesignFixtures.document())
        listOf("version" to 5, "version" to 1.5, "revision" to 1.5, "coordinateUnit" to "foot", "yAxis" to "down").forEach { (key, value) ->
            assertThrows(IllegalArgumentException::class.java) { DesignJsonCodec.decode(JSONObject(json).put(key, value).toString()) }
        }
    }
    @Test fun `corrupt object stops the complete read rather than dropping half the project`() {
        val root = JSONObject(DesignJsonCodec.encode(DesignFixtures.document()))
        root.getJSONArray("objects").getJSONObject(1).getJSONArray("nodes").getJSONObject(0).put("bulge", 4.0)
        assertThrows(IllegalArgumentException::class.java) { DesignJsonCodec.decode(root.toString()) }
    }
    @Test fun `legacy page projects roundtrip unchanged and are not silently promoted`() {
        val legacy = TraceProject(id = "legacy", elements = listOf(RectangleElement(id="r", layerId=TraceProject.DEFAULT_LAYER_1_ID,
            left=1f, top=2f, right=50f, bottom=40f)))
        val loaded = ProjectJsonConverter.fromEntity(ProjectJsonConverter.toEntity(legacy))
        assertEquals(legacy.elements, loaded.elements)
        assertEquals(legacy.currentPageDrawing(), loaded.currentPageDrawing())
        assertThrows(Exception::class.java) { DesignJsonCodec.decode(ProjectJsonConverter.serializeElements(legacy.elements)) }
    }
    @Test fun `different view projections never create another authoritative yard`() {
        val doc = DesignFixtures.document()
        val json = DesignJsonCodec.encode(doc)
        val metric = DesignOutput.drawing(doc, DesignOutputSettings(imperialLabels=false))
        val imperial = DesignOutput.drawing(doc, DesignOutputSettings(drawingUnitsPerMetre=200.0, origin=DesignPoint(2.0,3.0)))
        assertEquals(doc.objects.size, metric.elements.filterIsInstance<PolylineElement>().size)
        assertEquals(metric.elements.map { it.id }, imperial.elements.map { it.id })
        assertTrue(metric.layers.all { it.isLocked })
        assertTrue(metric.elements.filterIsInstance<TextElement>().all { it.text.endsWith(" m") })
        assertTrue(imperial.elements.filterIsInstance<TextElement>().all { it.text.endsWith(" ft") })
        assertEquals(100f, metric.scaleCalibration.pixelsPerUnit, 1e-5f)
        assertEquals(60.96f, imperial.scaleCalibration.pixelsPerUnit, 1e-5f)
        assertEquals(Point2D(-400f,600f), (imperial.elements.first() as PolylineElement).points.first())
        assertEquals(json, DesignJsonCodec.encode(doc))
    }
    @Test fun `distant coordinates require an appropriate projection origin instead of losing precision`() {
        val doc = ProjectDesign("distant", listOf(DesignObject("pool", "Distant", DesignObjectKind.POOL,
            DesignFixtures.circle().translated(1e8, 1e8))))
        assertThrows(IllegalArgumentException::class.java) { DesignOutput.drawing(doc) }
        val drawing = DesignOutput.drawing(doc, DesignOutputSettings(origin=DesignPoint(1e8,1e8)))
        assertTrue(drawing.elements.isNotEmpty())
    }
    @Test fun `curve tessellation tolerance never changes authoritative perimeter labels`() {
        val doc = ProjectDesign("circle", listOf(DesignObject("pool", "Circle", DesignObjectKind.POOL, DesignFixtures.circle())))
        val coarse = DesignOutput.drawing(doc, DesignOutputSettings(maxChordErrorMetres=0.1))
        val fine = DesignOutput.drawing(doc, DesignOutputSettings(maxChordErrorMetres=0.001))
        assertNotEquals((coarse.elements.first() as PolylineElement).points.size, (fine.elements.first() as PolylineElement).points.size)
        assertEquals(coarse.elements.filterIsInstance<TextElement>().map { it.text }, fine.elements.filterIsInstance<TextElement>().map { it.text })
        assertTrue(fine.elements.filterIsInstance<TextElement>().single().text.contains("61.84 ft"))
    }
    @Test fun `edit save reopen and actual PNG export are repeatable without rewriting curves`() = runBlocking {
        val session = DesignSession(DesignFixtures.document())
        session.execute(DesignCommand.ChangeBulge("pool", "edge-3", -0.2))
        val saved = DesignJsonCodec.encode(session.document)
        val reopened = DesignJsonCodec.decode(saved)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = DesignOutput.png(context, reopened, 800, 600)!!.readBytes()
        val second = DesignOutput.png(context, reopened, 800, 600)!!.readBytes()
        assertArrayEquals(first, second)
        assertEquals(saved, DesignJsonCodec.encode(reopened))
        val bitmap = BitmapFactory.decodeByteArray(first, 0, first.size)
        assertEquals(800, bitmap.width); assertEquals(600, bitmap.height); bitmap.recycle()
        val without = DesignOutput.png(context, reopened, 800, 600, DesignOutputSettings(includeMeasurements=false))!!.readBytes()
        assertFalse(first.contentEquals(without))
        // Synthetic fixtures only. Preserve authentic renderer output for review, never a mockup.
        val evidence = File("build/reports/design-geometry").apply { mkdirs() }
        File(evidence, "curved-study-measurements.png").writeBytes(first)
        File(evidence, "curved-study-outline.png").writeBytes(without)
        File(evidence, "canonical-design.json").writeText(saved)
    }
}
