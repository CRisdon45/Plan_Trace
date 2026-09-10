package com.example

import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.InputDevice
import com.example.model.design.DesignDimensions
import com.example.model.design.DesignPoint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.DesignJsonCodec
import com.example.data.DesignWorkspaceStore
import com.example.model.design.ProjectDesign
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Run only on a disposable emulator. Never clear or replace an existing user's workspace. */
class DesignWorkspaceDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val store get() = DesignWorkspaceStore(DesignWorkspaceStore.fileIn(context.filesDir))
    @Before fun disposableEmulatorOnly() {
        assumeTrue(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone") || Build.MODEL.contains("Emulator"))
    }
    private fun openWorkspace() {
        ui.onNodeWithContentDescription("Projects").performClick()
        ui.onNodeWithTag("open-design-workspace").performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithTag("workspace-canvas").fetchSemanticsNodes().size == 1 }
    }
    private fun command(category:String, action:String) {
        ui.onNodeWithTag("workspace-commands").performClick()
        ui.onNodeWithTag("radial-category-$category").performTouchInput { click(center) }
        ui.onNodeWithTag("radial-action-$action").performTouchInput { click(center) }
    }
    private fun genericPen(action:Int, button:Int, x:Float, y:Float) {
        ui.runOnIdle {
            val properties=MotionEvent.PointerProperties().apply { id=0;toolType=MotionEvent.TOOL_TYPE_STYLUS }
            val coords=MotionEvent.PointerCoords().apply { this.x=x;this.y=y;pressure=0f }
            val now=SystemClock.uptimeMillis()
            val event=MotionEvent.obtain(now,now,action,1,arrayOf(properties),arrayOf(coords),0,button,1f,1f,0,0,InputDevice.SOURCE_STYLUS,0)
            try { ui.activity.dispatchGenericMotionEvent(event) } finally { event.recycle() }
        }
    }
    private fun saved(): ProjectDesign {
        ui.waitUntil(10000) {
            ui.onAllNodes(hasTestTag("workspace-save-status") and hasText("Saved on device")).fetchSemanticsNodes().isNotEmpty()
        }
        return store.load()!!
    }
    private fun screenshot(name: String) {
        val dir = File(context.getExternalFilesDir(null), "workspace-evidence").apply { mkdirs() }
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(300, 3000)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        File(dir, "$name-semantics.txt").writeText(ui.onRoot().printToString())
    }
    @Test fun createEditUndoPersist() {
        assertNull("Use a fresh emulator, never erase an existing draft", store.load())
        openWorkspace()
        command("draw","pool")
        val starting = saved()
        assertEquals(1, starting.objects.size)
        // Category expansion and closing are not edits, and unavailable actions remain in place.
        val canvas=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        genericPen(MotionEvent.ACTION_HOVER_MOVE,0,canvas.left+4,canvas.top+4)
        genericPen(MotionEvent.ACTION_HOVER_MOVE,MotionEvent.BUTTON_STYLUS_PRIMARY,canvas.left+4,canvas.top+4)
        genericPen(MotionEvent.ACTION_BUTTON_RELEASE,0,canvas.left+4,canvas.top+4)
        ui.onNodeWithTag("workspace-radial").assertExists()
        ui.onNodeWithTag("radial-category-edit").performTouchInput { click(center) }
        screenshot("radial-edit-fan")
        val categoryBounds=ui.onNodeWithTag("radial-category-edit").fetchSemanticsNode().boundsInRoot
        val childBounds=ui.onNodeWithTag("radial-action-size").fetchSemanticsNode().boundsInRoot
        assertTrue(categoryBounds.left>=canvas.left && childBounds.right<=canvas.right)
        ui.onNodeWithTag("radial-centre").performTouchInput { click(center) }
        ui.onNodeWithTag("radial-centre").performTouchInput { click(center) }
        assertEquals(starting,saved())
        command("edit","size")
        ui.onNodeWithTag("workspace-size-length").performTextReplacement("28' 6\"")
        ui.onNodeWithTag("workspace-size-length").performImeAction()
        val sized=saved()
        assertEquals(28.5*DesignDimensions.FOOT,DesignDimensions.measure(sized.objects[0].boundary).lengthMetres,1e-8)
        assertEquals(starting.objects[0].boundary.nodes[0].point,sized.objects[0].boundary.nodes[0].point)
        assertEquals(starting.objects[0].coping,sized.objects[0].coping)
        ui.onNodeWithTag("workspace-size-length").performTextReplacement("-4")
        ui.onNodeWithTag("workspace-size-length").performImeAction()
        assertEquals(sized,saved())
        ui.onNodeWithTag("workspace-size-close").performClick()
        ui.onNodeWithTag("workspace-undo").performClick();assertEquals(starting.objects,saved().objects)
        ui.onNodeWithTag("workspace-redo").performClick()
        val initial=saved()
        // View/assist choices do not consume Undo or change saved project geometry.
        command("view","grid");assertEquals(initial,saved())
        command("assist","snap");assertEquals(initial,saved())
        command("assist","snap");assertEquals(initial,saved())
        assertNotNull(initial.objects[0].coping)
        assertNotNull(initial.objects[0].copingFootprint)
        // Finger navigation must not edit an outline while pen-only editing is selected.
        ui.onNodeWithTag("workspace-canvas").performTouchInput { swipe(center, center + Offset(35f, 20f), 300) }
        assertEquals(initial, saved())
        command("view","fit")
        command("assist","touch")
        ui.onNodeWithTag("workspace-vertex-1").performTouchInput { swipe(center, center + Offset(48f, -24f), 350) }
        val moved = saved()
        assertEquals(initial.revision + 1, moved.revision)
        assertNotEquals(initial.objects.first().boundary, moved.objects.first().boundary)
        assertNotEquals(initial.objects.first().copingFootprint!!.outerBoundary, moved.objects.first().copingFootprint!!.outerBoundary)
        assertEquals(initial.objects.first().boundary.nodes.map { it.edgeId }, moved.objects.first().boundary.nodes.map { it.edgeId })
        ui.onNodeWithTag("workspace-undo").performClick()
        assertEquals(initial.objects, saved().objects)
        ui.onNodeWithTag("workspace-redo").performClick()
        assertEquals(moved.objects, saved().objects)
        command("draw","curved")
        val withCurve = saved()
        ui.onNodeWithTag("workspace-curve-3").performTouchInput { swipe(center, center + Offset(0f, 26f), 350) }
        val bent = saved()
        assertEquals(withCurve.objects[1].coping, bent.objects[1].coping)
        assertNotEquals(withCurve.objects[1].copingFootprint!!.outerBoundary, bent.objects[1].copingFootprint!!.outerBoundary)
        assertEquals(withCurve.objects.first(), bent.objects.first())
        assertEquals(withCurve.objects[1].boundary.nodes.map { it.point }, bent.objects[1].boundary.nodes.map { it.point })
        assertNotEquals(withCurve.objects[1].boundary.nodes[3].bulge, bent.objects[1].boundary.nodes[3].bulge)
        // Platform cancellation must discard a preview, not save a partial gesture.
        ui.onNodeWithTag("workspace-vertex-1").performTouchInput { down(center); moveBy(Offset(15f, 18f)); cancel() }
        assertEquals(bent, saved())
        command("edit","delete")
        assertEquals(listOf(bent.objects.first()), saved().objects)
        ui.onNodeWithTag("workspace-undo").performClick()
        val restoredBeforeWidth = saved()
        assertEquals(bent.objects, restoredBeforeWidth.objects)
        ui.onNodeWithTag("workspace-object-1").performClick()
        command("edit","coping")
        ui.onNodeWithTag("workspace-coping-width").performTextReplacement("16")
        ui.onNodeWithTag("workspace-coping-width").performImeAction()
        val restored = saved()
        assertEquals(16.0,restored.objects[1].coping!!.widthInches,1e-9)
        assertEquals(bent.objects[1].boundary,restored.objects[1].boundary)
        assertEquals(bent.objects.first(),restored.objects.first())
        // Too-wide coping would consume this inside curve. Rejection must leave disk/revision intact.
        ui.onNodeWithTag("workspace-coping-width").performTextReplacement("48")
        ui.onNodeWithTag("workspace-coping-width").performImeAction()
        assertEquals(restored,saved())
        ui.onNodeWithTag("workspace-coping-width").assertTextContains("16.00")
        screenshot("rejected-coping-width")
        ui.onNodeWithTag("workspace-coping-close").performClick()
        // A crossing reshape through the actual pointer route must also preserve the saved assembly.
        ui.onNodeWithTag("workspace-object-0").performClick()
        val first = ui.onNodeWithTag("workspace-vertex-0").fetchSemanticsNode().boundsInRoot.center
        val second = ui.onNodeWithTag("workspace-vertex-1").fetchSemanticsNode().boundsInRoot.center
        val top = ui.onNodeWithTag("workspace-vertex-3").fetchSemanticsNode().boundsInRoot.center
        val invalid = first + Offset(-30f, (top.y-first.y)*0.5f)
        ui.onNodeWithTag("workspace-vertex-1").performTouchInput { swipe(center, center + (invalid-second), 350) }
        assertEquals(restored,saved())
        screenshot("rejected-crossing-edit")
        ui.onNodeWithTag("workspace-object-1").performClick()
        screenshot("edited-workspace")
        ui.onNodeWithTag("workspace-commands").performClick()
        ui.onNodeWithTag("radial-category-assist").performTouchInput { click(center) }
        screenshot("radial-assist-fan")
        ui.onNodeWithTag("radial-centre").performTouchInput { click(center) }
        ui.onNodeWithTag("radial-centre").performTouchInput { click(center) }
        val evidence = File(context.getExternalFilesDir(null), "workspace-evidence")
        File(evidence, "before-process-restart.json").writeText(DesignJsonCodec.encode(restored))
        ui.onNodeWithTag("workspace-back").performClick()
        openWorkspace()
        assertEquals(restored, saved())
        ui.onNodeWithTag("workspace-object-count").assertTextEquals("2 objects")
    }
    @Test fun reopenAfterProcessDeath() {
        val expected = DesignJsonCodec.decode(File(context.getExternalFilesDir(null), "workspace-evidence/before-process-restart.json").readText())
        openWorkspace()
        assertEquals(expected, saved())
        assertEquals(16.0,saved().objects[1].coping!!.widthInches,1e-9)
        assertNotNull(saved().objects[1].copingFootprint)
        ui.onNodeWithTag("workspace-object-count").assertTextEquals("2 objects")
        ui.onNodeWithTag("workspace-object-1").performClick()
        screenshot("reopened-workspace")
        // Activity recreation checks the actual screen state while preserving the same saved authority.
        ui.activityRule.scenario.recreate()
        ui.waitUntil(10000) { ui.onAllNodesWithTag("workspace-canvas").fetchSemanticsNodes().size == 1 }
        assertEquals(expected, saved())
        screenshot("recreated-workspace")
        assertTrue(context.getSharedPreferences("workspace-view",0).getBoolean("grid",false))
        assertFalse(context.getSharedPreferences("workspace-view",0).getBoolean("snap",true))
    }
}
