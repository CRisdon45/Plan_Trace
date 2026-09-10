package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ClosedPathMeasurementTest {
    private val scale = ScaleCalibration(true, 10f, 1f, "ft")
    private val triangle = listOf(Point2D(0f, 0f), Point2D(30f, 0f), Point2D(30f, 40f))

    private fun paths(points: List<Point2D>, closed: Boolean): List<VectorElement> = listOf(
        PolylineElement(id = "polyline", layerId = "geometry", points = points, isClosed = closed),
        FreehandPath(id = "freehand", layerId = "geometry", points = points, isClosed = closed)
    )

    @Test fun `closed triangle includes its implicit last to first edge`() {
        paths(triangle, true).forEach { assertEquals("12'", it.getMeasurementLabel(scale)) }
    }

    @Test fun `open triangle measures only the drawn chain`() {
        paths(triangle, false).forEach { assertEquals("7'", it.getMeasurementLabel(scale)) }
    }

    @Test fun `explicit closing point is not counted twice`() {
        paths(triangle + triangle.first(), true).forEach {
            assertEquals("12'", it.getMeasurementLabel(scale))
        }
    }

    @Test fun `concave orthogonal loop includes the closing edge`() {
        val lShape = listOf(Point2D(0f, 0f), Point2D(60f, 0f), Point2D(60f, 20f),
            Point2D(20f, 20f), Point2D(20f, 50f), Point2D(0f, 50f))
        paths(lShape, true).forEach { assertEquals("22'", it.getMeasurementLabel(scale)) }
    }

    @Test fun `reversal and translation preserve length without editing vertices`() {
        val elements = paths(triangle, true)
        val originals = elements.toList()
        elements.forEach {
            assertEquals("12'", it.translate(-100f, 80f).getMeasurementLabel(scale))
            assertEquals("12'", it.getMeasurementLabel(scale))
        }
        paths(triangle.reversed(), true).forEach { assertEquals("12'", it.getMeasurementLabel(scale)) }
        assertEquals(originals, elements)
    }

    @Test fun `degenerate drafts and uncalibrated paths do not invent perimeters`() {
        for (closed in listOf(false, true)) {
            paths(emptyList(), closed).forEach { assertNull(it.getMeasurementLabel(scale)) }
            paths(listOf(Point2D(0f, 0f)), closed).forEach { assertNull(it.getMeasurementLabel(scale)) }
            paths(triangle.take(2), closed).forEach { assertEquals("3'", it.getMeasurementLabel(scale)) }
            paths(triangle, closed).forEach { assertNull(it.getMeasurementLabel(ScaleCalibration())) }
        }
    }

    @Test fun `repeated vertices and metric calibration keep their meanings`() {
        val withDuplicate = listOf(triangle[0], triangle[1], triangle[1], triangle[2])
        paths(withDuplicate, true).forEach {
            assertEquals("12'", it.getMeasurementLabel(scale))
            assertEquals("12.00 m", it.getMeasurementLabel(scale.copy(unit = "m")))
        }
    }
}
