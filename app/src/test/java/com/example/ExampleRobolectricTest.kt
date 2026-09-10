package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProjectJsonConverter
import com.example.engine.HoldToStraightenDetector
import com.example.engine.StraightenedResult
import com.example.model.DrawingLayer
import com.example.model.EllipseElement
import com.example.model.FreehandPath
import com.example.model.LineElement
import com.example.model.Point2D
import com.example.model.RectangleElement
import com.example.model.ScaleCalibration
import com.example.model.StrokeStyle
import com.example.model.TraceProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.cos
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals(if (BuildConfig.DEBUG) "Plan Trace Dev" else "Plan Trace", appName)
    }

    @Test
    fun `point2d distance calculation`() {
        val p1 = Point2D(0f, 0f)
        val p2 = Point2D(30f, 40f)
        assertEquals(50f, p1.distanceTo(p2), 0.001f)
    }

    @Test
    fun `scale calibration formats real world architectural dimensions`() {
        // 200 pixels = 20 feet (10 pixels per foot)
        val scale = ScaleCalibration(
            isCalibrated = true,
            pixelDistance = 200f,
            realWorldUnits = 20f,
            unit = "ft"
        )
        // 315.8 pixels should format into 31'-7" (31.58 feet)
        val formatted = scale.formatMeasurement(315.8f)
        assertEquals("31'-7\"", formatted)
    }

    @Test
    fun `scale calibration parses feet and inches strings`() {
        val (val1, u1) = ScaleCalibration.parseInputToUnits("20'")
        assertEquals(20f, val1, 0.01f)
        assertEquals("ft", u1)

        val (val2, u2) = ScaleCalibration.parseInputToUnits("15'6\"")
        assertEquals(15.5f, val2, 0.01f)
        assertEquals("ft", u2)

        val (val3, u3) = ScaleCalibration.parseInputToUnits("6.5m")
        assertEquals(6.5f, val3, 0.01f)
        assertEquals("m", u3)
    }

    @Test
    fun `element measurement labels with scale calibration`() {
        val scale = ScaleCalibration(
            isCalibrated = true,
            pixelDistance = 100f,
            realWorldUnits = 10f,
            unit = "ft" // 10 px per foot
        )

        val rect = RectangleElement(
            layerId = "layer_1",
            left = 0f, top = 0f, right = 200f, bottom = 400f // 20' x 40' pool
        )
        assertEquals("20' × 40'", rect.getMeasurementLabel(scale))

        val circle = EllipseElement(
            layerId = "layer_1",
            centerX = 100f, centerY = 100f, radiusX = 80f, radiusY = 80f // 16' diameter
        )
        assertEquals("Ø 16'", circle.getMeasurementLabel(scale))
    }

    @Test
    fun `hold to straighten detects straight line`() {
        val points = mutableListOf<Point2D>()
        for (i in 0..20) {
            points.add(Point2D(i * 10f, 50f + (if (i % 2 == 0) 1.5f else -1.5f)))
        }
        val result = HoldToStraightenDetector.analyze(points)
        assertNotNull(result)
        assertTrue(result is StraightenedResult.Line)
    }

    @Test
    fun `hold to straighten detects circle`() {
        val points = mutableListOf<Point2D>()
        val radius = 100f
        val center = 200f
        for (i in 0..36) {
            val angle = Math.toRadians(i * 10.0)
            points.add(Point2D((center + radius * cos(angle)).toFloat(), (center + radius * sin(angle)).toFloat()))
        }
        val result = HoldToStraightenDetector.analyze(points)
        assertNotNull(result)
        assertTrue(result is StraightenedResult.CircleOrEllipse)
    }

    @Test
    fun `vector elements and layers json serialization roundtrip`() {
        val layer = DrawingLayer(id = "layer_1", name = "Test Layer")
        val line = LineElement(
            id = "line_1",
            layerId = "layer_1",
            start = Point2D(10f, 20f),
            end = Point2D(100f, 200f),
            strokeColor = 0xFF1E293B,
            strokeWidth = 3f,
            style = StrokeStyle.INK
        )
        val freehand = FreehandPath(
            id = "free_1",
            layerId = "layer_1",
            points = listOf(Point2D(5f, 5f), Point2D(15f, 15f), Point2D(25f, 25f))
        )

        val project = TraceProject(
            title = "Test Sketch",
            layers = listOf(layer),
            elements = listOf(line, freehand)
        )

        val entity = ProjectJsonConverter.toEntity(project)
        val restored = ProjectJsonConverter.fromEntity(entity)

        assertEquals(project.title, restored.title)
        assertEquals(1, restored.layers.size)
        assertEquals("Test Layer", restored.layers[0].name)
        assertEquals(2, restored.elements.size)
        assertTrue(restored.elements[0] is LineElement)
        assertTrue(restored.elements[1] is FreehandPath)
    }
}
