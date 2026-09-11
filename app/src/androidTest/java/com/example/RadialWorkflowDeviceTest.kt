package com.example

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.DesignWorkspaceStore
import com.example.model.design.DesignDimensions
import com.example.model.design.ProjectDesign
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.math.round

/** Continues the saved synthetic fixture on the same disposable emulator; never clears app data. */
class RadialWorkflowDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val store get() = DesignWorkspaceStore(DesignWorkspaceStore.fileIn(context.filesDir))
    @Before fun emulatorOnly() {
        assumeTrue(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone") || Build.MODEL.contains("Emulator"))
    }
    private fun open() {
        assertNotNull("Earlier creation scenario must produce the synthetic draft", store.load())
        ui.onNodeWithContentDescription("Projects").performClick()
        ui.onNodeWithTag("open-design-workspace").performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithTag("workspace-canvas").fetchSemanticsNodes().size == 1 }
        ui.onNodeWithTag("workspace-commands").assertIsDisplayed()
    }
    private fun saved(): ProjectDesign {
        ui.waitUntil(10000) { ui.onAllNodes(hasTestTag("workspace-save-status") and hasText("Saved on device")).fetchSemanticsNodes().isNotEmpty() }
        return store.load()!!
    }
    private fun tap(tag: String) { ui.onNodeWithTag(tag).performTouchInput { click(center) } }
    private fun expand(category: String) { ui.onNodeWithTag("workspace-commands").performClick(); tap("radial-category-$category") }
    private fun command(category: String, action: String) { expand(category); tap("radial-action-$action") }
    private fun dismissMenu() { tap("radial-centre"); tap("radial-centre") }
    private fun capture(name: String) {
        ui.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(300, 3000)
        val dir = File(context.getExternalFilesDir(null), "workspace-evidence").apply { mkdirs() }
        EmulatorCapture.save(context,dir,name)
        File(dir, "$name-semantics.txt").writeText(ui.onRoot().printToString())
    }
    private fun hover(x: Float, y: Float, buttons: Int) {
        ui.runOnIdle {
            val property = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_STYLUS }
            val coordinate = MotionEvent.PointerCoords().apply { this.x = x; this.y = y; pressure = 0f }
            val now = SystemClock.uptimeMillis()
            val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_HOVER_MOVE, 1, arrayOf(property), arrayOf(coordinate),
                0, buttons, 1f, 1f, 0, 0, InputDevice.SOURCE_STYLUS, 0)
            try { ui.activity.dispatchGenericMotionEvent(event) } finally { event.recycle() }
        }
    }
    @Test fun radialEditingSafety() {
        open(); val original = saved()
        ui.onNodeWithTag("workspace-object-0").performClick()
        // Typed but uncommitted dimensions and untouched formatted values must not change geometry.
        command("edit", "size")
        ui.onNodeWithTag("workspace-size-length").performImeAction()
        assertEquals(original, saved())
        ui.onNodeWithTag("workspace-size-length").performTextReplacement("40")
        ui.onNodeWithTag("workspace-size-close").performClick()
        assertEquals(original, saved())
        // A real snapped pointer edit, not just a preference toggle or standalone math test.
        command("assist", "snap")
        ui.onNodeWithTag("workspace-vertex-0").performTouchInput { swipe(center, center + Offset(31f, -19f), 350) }
        val snapped = saved()
        assertEquals(original.revision + 1, snapped.revision)
        val point = snapped.objects[0].boundary.nodes[0].point
        assertEquals(round(point.x / DesignDimensions.FOOT), point.x / DesignDimensions.FOOT, 1e-8)
        assertEquals(round(point.y / DesignDimensions.FOOT), point.y / DesignDimensions.FOOT, 1e-8)
        assertEquals(original.objects[1], snapped.objects[1])
        ui.onNodeWithTag("workspace-undo").performClick()
        assertEquals(original.objects, saved().objects)
        command("assist", "snap")
        // Freeform numeric sizing preserves arcs by uniform scaling, explicitly not stretching.
        ui.onNodeWithTag("workspace-object-1").performClick()
        val beforeSize = saved(); val before = beforeSize.objects[1]; val span = DesignDimensions.measure(before.boundary)
        command("edit", "size")
        ui.onNodeWithTag("workspace-size-length").performTextReplacement(String.format(Locale.US, "%.8f", span.lengthMetres * 1.1 / DesignDimensions.FOOT))
        ui.onNodeWithTag("workspace-size-length").performImeAction()
        val resized = saved(); val after = DesignDimensions.measure(resized.objects[1].boundary)
        assertEquals(span.lengthMetres * 1.1, after.lengthMetres, 1e-7)
        assertEquals(span.widthMetres * 1.1, after.widthMetres, 1e-7)
        assertEquals(before.coping, resized.objects[1].coping)
        assertEquals(before.boundary.nodes.map { it.bulge }, resized.objects[1].boundary.nodes.map { it.bulge })
        assertEquals(beforeSize.objects[0], resized.objects[0])
        capture("radial-exact-size")
        ui.onNodeWithTag("workspace-size-close").performClick()
        ui.onNodeWithTag("workspace-undo").performClick(); assertEquals(beforeSize.objects, saved().objects)
        // Same wheel/fan fits all four corners without changing geometry or command directions.
        val box = ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        val corners = listOf(Offset(box.left + 4, box.top + 4), Offset(box.right - 4, box.top + 4),
            Offset(box.left + 4, box.bottom - 4), Offset(box.right - 4, box.bottom - 4))
        val beforeMenus = saved()
        corners.forEachIndexed { index, p ->
            hover(p.x, p.y, 0); hover(p.x, p.y, MotionEvent.BUTTON_STYLUS_PRIMARY); hover(p.x, p.y, 0)
            tap("radial-category-edit")
            for (tag in listOf("radial-action-size", "radial-action-coping", "radial-action-delete")) {
                val bounds = ui.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
                assertTrue(bounds.left >= box.left && bounds.right <= box.right && bounds.top >= box.top && bounds.bottom <= box.bottom)
            }
            if (index == 3) capture("radial-bottom-right")
            dismissMenu()
        }
        assertEquals(beforeMenus, saved())
        command("select", "clear"); expand("edit")
        ui.onNodeWithTag("radial-action-delete").assertIsNotEnabled(); tap("radial-action-delete")
        ui.onNodeWithTag("workspace-radial").assertExists(); assertEquals(beforeMenus, saved()); dismissMenu()
        assertEquals(original.objects, saved().objects)
    }
    private fun layoutCase(compact: Boolean, name: String) {
        open(); val before = saved()
        ui.onNodeWithTag("workspace-object-0").performClick()
        expand("edit")
        if (compact) {
            ui.onNodeWithText("Size").assertExists()
            ui.onNodeWithTag("radial-centre").assertIsDisplayed()
            val back = ui.onNodeWithTag("radial-centre").fetchSemanticsNode().boundsInRoot
            val panel = ui.onNodeWithTag("radial-compact-panel").fetchSemanticsNode().boundsInRoot
            val action = ui.onNodeWithTag("radial-action-size").fetchSemanticsNode().boundsInRoot
            assertTrue("Back must stay above the action list", back.bottom <= action.top)
            assertTrue("Back must fit wholly inside the panel", back.top >= panel.top && back.bottom <= panel.bottom)
        } else ui.onNodeWithContentDescription("Size").assertExists()
        capture(name)
        tap("radial-action-size")
        ui.onNodeWithTag("workspace-size-panel").assertExists()
        ui.onNodeWithTag("workspace-size-length").assertIsDisplayed()
        ui.onNodeWithTag("workspace-size-length").performTextReplacement("100")
        ui.onNodeWithTag("workspace-size-close").performClick()
        assertEquals(before, saved()) // Closing is cancellation, not an implicit commit.
        ui.onNodeWithTag("workspace-commands").assertIsDisplayed()
        ui.onNodeWithTag("workspace-export").assertIsDisplayed()
        ui.onNodeWithTag("workspace-back").assertIsDisplayed()
    }
    @Test fun portraitCommands() { layoutCase(false, "radial-portrait") }
    @Test fun compactCommands() { layoutCase(true, "radial-compact") }
}
