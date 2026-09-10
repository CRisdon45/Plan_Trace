package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.example.export.ExportManager
import com.example.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exercises the actual PNG file/renderer path, not a mock export implementation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PngMeasurementExportTest {
    @Test fun `PNG measurement flag changes annotations not project data and is repeatable`() = runBlocking {
        val layer = DrawingLayer(id = "base", name = "Synthetic geometry")
        val project = TraceProject(
            title = "synthetic-measurement-export",
            layers = listOf(layer), activeLayerId = layer.id,
            scaleCalibration = ScaleCalibration(true, 20f, 1f, "ft"),
            elements = listOf(RectangleElement(
                id = "rectangle", layerId = layer.id,
                left = 100f, top = 100f, right = 500f, bottom = 340f))
        )
        val before = project.copy(elements = project.elements.toList())
        val context = ApplicationProvider.getApplicationContext<Context>()
        suspend fun export(show: Boolean): ByteArray {
            val file = ExportManager.exportToPng(context, project, null,
                includeBackground = false, width = 640, height = 480, showDimensions = show)
            assertNotNull("The real PNG export must succeed", file)
            return file!!.readBytes()
        }
        val without = export(false)
        val with = export(true)
        val withoutAgain = export(false)
        assertFalse("The flag must change the exported pixels", without.contentEquals(with))
        assertArrayEquals("Identical exports must remain stable", without, withoutAgain)
        assertEquals(before, project)
        val bitmap = BitmapFactory.decodeByteArray(without, 0, without.size)
        assertNotNull(bitmap)
        assertEquals(640, bitmap.width)
        assertEquals(480, bitmap.height)
        bitmap.recycle()
    }
}
