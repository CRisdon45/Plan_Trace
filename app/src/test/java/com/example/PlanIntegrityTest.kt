package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import com.example.export.ExportGeometry
import com.example.export.ExportManager
import com.example.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlanIntegrityTest {
    private val base = DrawingLayer(id = "base", name = "Base")
    private val patio = DrawingLayer(id = "patio", name = "Patio")
    private fun project() = TraceProject(layers = listOf(base, patio), activeLayerId = patio.id,
        elements = listOf(
            LineElement(id = "a", layerId = patio.id, start = Point2D(100f, 100f), end = Point2D(300f, 100f)),
            LineElement(id = "b", layerId = patio.id, start = Point2D(100f, 200f), end = Point2D(300f, 200f))))

    @Test fun `undo layer removal restores both objects and ownership together`() {
        val before = project()
        val history = EditHistory()
        history.record(before)
        val deleted = before.copy(layers = listOf(base), elements = emptyList(), activeLayerId = base.id)
        val restored = history.undo(deleted)
        assertEquals(before.layers, restored.layers)
        assertEquals(before.elements, restored.elements)
        assertEquals(patio.id, restored.activeLayerId)
        assertTrue(restored.elements.all { e -> restored.layers.any { it.id == e.layerId } })
        val redone = history.redo(restored)
        assertEquals(deleted.layers, redone.layers)
        assertEquals(deleted.elements, redone.elements)
    }

    @Test fun `one undo reverses a hundred drag updates and redo restores final position`() {
        val original = project()
        val history = EditHistory()
        history.begin(original)
        var current = original
        repeat(100) {
            history.record(current)
            current = current.copy(elements = current.elements.map { e -> e.translate(1f, 2f) })
        }
        history.commit(current)
        val undone = history.undo(current)
        assertEquals(original.elements, undone.elements)
        assertFalse(history.canUndo)
        assertEquals(current.elements, history.redo(undone).elements)
    }

    @Test fun `cancelled drag restores state without consuming redo`() {
        val original = project()
        val history = EditHistory()
        history.record(original)
        val moved = original.copy(elements = original.elements.map { it.translate(10f, 0f) })
        val undone = history.undo(moved)
        history.begin(undone)
        val cancelled = history.cancel(moved)
        assertEquals(original.elements, cancelled.elements)
        assertTrue(history.canRedo)
        assertEquals(moved.elements, history.redo(cancelled).elements)
    }

    @Test fun `bounds retain negative and outside geometry but ignore hidden layers`() {
        val p = project().copy(layers = listOf(base, patio.copy(isVisible = false)), elements = listOf(
            LineElement(layerId = base.id, start = Point2D(-200f, -100f), end = Point2D(1300f, 900f)),
            LineElement(layerId = patio.id, start = Point2D(9000f, 9000f), end = Point2D(10000f, 10000f))))
        val b = ExportGeometry.contentBounds(p, 1000, 700)
        assertTrue(b.left < -200f && b.top < -100f && b.right > 1300f && b.bottom > 900f)
        assertTrue(b.right < 2000f)
        val fit = ExportGeometry.fit(b, RectF(0f, 0f, 2048f, 1536f))
        assertEquals(1024f, b.centerX() * fit.scale + fit.translateX, .01f)
        assertEquals(768f, b.centerY() * fit.scale + fit.translateY, .01f)
    }

    @Test fun `graphic scale measures the plan at multiple sheet transforms`() {
        val scale = ScaleCalibration(true, 401.288f, 20f, "ft")
        for (transform in listOf(.12f, .42f, 2.0f)) {
            val bar = ExportGeometry.scaleBar(scale, transform, 240f)!!
            assertEquals(bar.segmentUnits * scale.pixelsPerUnit * transform, bar.segmentPoints, .001f)
            assertTrue(bar.segmentPoints * 4 <= 240f)
        }
        assertNull(ExportGeometry.scaleBar(ScaleCalibration(), .42f, 240f))
    }

    @Test fun `PNG preserves source reference and vector registration for a nonstandard image`() = runBlocking {
        val background = Bitmap.createBitmap(1000, 700, Bitmap.Config.ARGB_8888)
        background.eraseColor(Color.WHITE)
        Canvas(background).drawRect(100f, 300f, 500f, 320f, Paint().apply { color = Color.RED })
        val p = TraceProject(title = "alignment-regression", layers = listOf(base), activeLayerId = base.id,
            backgroundOpacity = 1f, elements = listOf(LineElement(layerId = base.id,
                start = Point2D(100f, 400f), end = Point2D(500f, 400f), strokeColor = 0xFF0000FF, strokeWidth = 10f)))
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = ExportManager.exportToPng(context, p, background)!!
        val output = BitmapFactory.decodeFile(file.path)
        val fit = ExportGeometry.fit(RectF(0f, 0f, 1000f, 700f), RectF(0f, 0f, 2048f, 1536f))
        fun pixel(x: Float, y: Float) = output.getPixel((x * fit.scale + fit.translateX).toInt(), (y * fit.scale + fit.translateY).toInt())
        assertEquals(Color.RED, pixel(300f, 310f))
        assertEquals(Color.BLUE, pixel(300f, 400f))
        assertEquals(Color.WHITE, pixel(600f, 310f))
        assertEquals(Color.WHITE, pixel(600f, 400f))
        background.recycle()
        output.recycle()
    }

    @Test fun `share chooser supports application context and grants access to attachment`() {
        val uri = android.net.Uri.parse("content://qa.fileprovider/exports/plan.png")
        val chooser = ExportManager.createShareChooser(uri, "image/png", "QA")
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertTrue(chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals("image/png", send.type)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(uri, send.clipData!!.getItemAt(0).uri)
    }

    @Test fun `bottom right resize changes dimensions without moving the opposite anchor`() {
        val original = RectangleElement(layerId = "base", left = 100f, top = 200f, right = 500f, bottom = 500f)
        val session = RectangleResize.hit(original, Point2D(508f, 508f), 1f)!!
        assertEquals(original, session.at(Point2D(508f, 508f)))
        val resized = session.at(Point2D(608f, 708f))
        assertEquals(100f, resized.left, 0f)
        assertEquals(200f, resized.top, 0f)
        assertEquals(600f, resized.right, 0f)
        assertEquals(700f, resized.bottom, 0f)
    }

    @Test fun `exact dimensions use calibrated units and reject invalid sizes`() {
        val original = RectangleElement(layerId = "base", left = 100f, top = 200f, right = 500f, bottom = 500f)
        val resized = original.withExactSize(22.5f, 15f, ScaleCalibration(true, 400f, 20f, "ft"))
        assertEquals(100f, resized.left, 0f)
        assertEquals(200f, resized.top, 0f)
        assertEquals(450f, resized.boundingBox().width(), 0f)
        assertEquals(300f, resized.boundingBox().height(), 0f)
        assertThrows(IllegalArgumentException::class.java) { original.withExactSize(Float.NaN, 15f, ScaleCalibration()) }
        assertThrows(IllegalArgumentException::class.java) { original.withExactSize(-1f, 15f, ScaleCalibration()) }
    }

    @Test fun `multiline note bounds include later lines and support selection across text`() {
        val one = TextElement(layerId = "base", text = "First line", position = Point2D(100f, 200f))
        val two = one.copy(text = "First line\nA much longer second line")
        assertTrue(two.boundingBox().height() > one.boundingBox().height())
        assertTrue(two.boundingBox().width() > one.boundingBox().width())
        val bounds = two.boundingBox()
        assertTrue(two.isPointInside(Point2D(bounds.right - 2f, bounds.bottom - 2f)))
    }
}
