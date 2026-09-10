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
        // Finger navigation must not edit an outline while pen-only editing is selected.
        ui.onNodeWithTag("workspace-canvas").performTouchInput { swipe(center, center + Offset(35f, 20f), 300) }
        assertEquals(initial, saved())
        ui.onNodeWithTag("workspace-fit").performClick()
        ui.onNodeWithTag("workspace-touch-edit").performClick()
        ui.onNodeWithTag("workspace-vertex-1").performTouchInput { swipe(center, center + Offset(48f, -24f), 350) }
        val moved = saved()
        assertEquals(initial.revision + 1, moved.revision)
        assertNotEquals(initial.objects.first().boundary, moved.objects.first().boundary)
        assertEquals(initial.objects.first().boundary.nodes.map { it.edgeId }, moved.objects.first().boundary.nodes.map { it.edgeId })
        ui.onNodeWithTag("workspace-undo").performClick()
        assertEquals(initial.objects, saved().objects)
        ui.onNodeWithTag("workspace-redo").performClick()
        assertEquals(moved.objects, saved().objects)
        ui.onNodeWithTag("workspace-add-curved").performClick()
        val withCurve = saved()
        ui.onNodeWithTag("workspace-curve-3").performTouchInput { swipe(center, center + Offset(0f, 26f), 350) }
        val bent = saved()
        assertEquals(withCurve.objects.first(), bent.objects.first())
        assertEquals(withCurve.objects[1].boundary.nodes.map { it.point }, bent.objects[1].boundary.nodes.map { it.point })
        assertNotEquals(withCurve.objects[1].boundary.nodes[3].bulge, bent.objects[1].boundary.nodes[3].bulge)
        // Platform cancellation must discard a preview, not save a partial gesture.
        ui.onNodeWithTag("workspace-vertex-1").performTouchInput { down(center); moveBy(Offset(15f, 18f)); cancel() }
        assertEquals(bent, saved())
        ui.onNodeWithTag("workspace-delete").performClick()
        assertEquals(listOf(bent.objects.first()), saved().objects)
        ui.onNodeWithTag("workspace-undo").performClick()
        val restored = saved()
        assertEquals(bent.objects, restored.objects)
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
