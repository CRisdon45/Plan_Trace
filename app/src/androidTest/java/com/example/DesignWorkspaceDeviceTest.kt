package com.example

import android.graphics.Bitmap
import android.os.Build
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
        ui.onNodeWithTag("workspace-add-pool").performClick()
        val initial = saved()
        assertEquals(1, initial.objects.size)
        assertNotNull(initial.objects[0].coping)
        assertNotNull(initial.objects[0].copingFootprint)
        // Finger navigation must not edit an outline while pen-only editing is selected.
        ui.onNodeWithTag("workspace-canvas").performTouchInput { swipe(center, center + Offset(35f, 20f), 300) }
        assertEquals(initial, saved())
        ui.onNodeWithTag("workspace-fit").performClick()
        ui.onNodeWithTag("workspace-touch-edit").performClick()
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
        ui.onNodeWithTag("workspace-add-curved").performClick()
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
        ui.onNodeWithTag("workspace-delete").performClick()
        assertEquals(listOf(bent.objects.first()), saved().objects)
        ui.onNodeWithTag("workspace-undo").performClick()
        val restoredBeforeWidth = saved()
        assertEquals(bent.objects, restoredBeforeWidth.objects)
        ui.onNodeWithTag("workspace-object-1").performClick()
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
    }
}
