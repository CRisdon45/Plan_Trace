package com.example

import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.DesignJsonCodec
import com.example.data.DesignWorkspaceStore
import com.example.model.design.*
import com.example.ui.workspace.DesignWorkspaceViewModel
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Actual pointer placement against protected source geometry, not injected snap-result state. */
class GeometrySnapDeviceTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val store get()=DesignWorkspaceStore(DesignWorkspaceStore.fileIn(context.filesDir))
    private val evidence get()=File(context.getExternalFilesDir(null),"workspace-evidence").apply { mkdirs() }
    private lateinit var model:DesignWorkspaceViewModel
    @Before fun disposableOnly() { assumeTrue(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone")) }
    private fun open() {
        ui.onNodeWithContentDescription("Projects").performClick()
        ui.onNodeWithTag("open-design-workspace").performClick()
        ui.waitUntil(15000) { ui.onAllNodesWithTag("workspace-canvas").fetchSemanticsNodes().size==1 }
        ui.runOnIdle { model=ViewModelProvider(ui.activity)[DesignWorkspaceViewModel::class.java] }
    }
    private fun saved():ProjectDesign {
        ui.waitUntil(15000) { model.state.value.saved && model.state.value.preview==null }
        return store.load()!!
    }
    private fun command(category:String,action:String) {
        ui.onNodeWithTag("workspace-commands").performClick()
        ui.onNodeWithTag("radial-category-$category").performTouchInput { click(center) }
        ui.onNodeWithTag("radial-action-$action").performTouchInput { click(center) }
        ui.waitForIdle()
    }
    private fun screen(point:DesignPoint):Offset {
        ui.waitForIdle()
        val box=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        val p=DesignViewport.fit(model.state.value.document!!,box.width.toDouble(),box.height.toDouble()).toScreen(point)
        return Offset(p.x.toFloat(),p.y.toFloat())
    }
    private fun capture(name:String) {
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(300,3000)
        EmulatorCapture.save(context,evidence,name)
        File(evidence,"$name-semantics.txt").writeText(ui.onRoot().printToString())
    }
    @Test fun snapToProtectedGeometry() {
        open();val original=saved()
        val house=original.objects.single { it.siteTrace?.role==SiteOutlineRole.HOUSE }
        assertTrue(house.locked)
        assertFalse(context.getSharedPreferences("workspace-view",0).getBoolean("geometry-snap",false))
        assertFalse(context.getSharedPreferences("workspace-view",0).getBoolean("snap",true))
        command("assist","geometry");command("assist","snap")
        assertEquals(original,saved()) // Preferences have no geometry/history effect.
        command("draw","paving")
        val first=house.boundary.nodes[0].point
        val second=house.boundary.nodes[1].point
        val third=house.boundary.nodes[2].point
        var at=screen(first)+Offset(-6f,3f)
        ui.onNodeWithTag("workspace-canvas").performTouchInput { down(at) }
        assertEquals(first,model.state.value.draftTarget!!.point)
        assertEquals(house.id,model.state.value.draftTarget!!.snap!!.key.objectId)
        ui.onNodeWithTag("geometry-snap-guide").assertExists()
        ui.onNodeWithTag("workspace-canvas").performTouchInput { up() }
        assertEquals(first,model.state.value.drawingPoints!!.single())
        assertNotEquals(GridAssist.snapPoint(first),first) // Real off-grid reference was not quantized.
        at=screen(second)+Offset(6f,-3f)
        ui.onNodeWithTag("workspace-canvas").performTouchInput { down(at) }
        val before=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        assertEquals(second,model.state.value.draftTarget!!.point)
        ui.onNodeWithTag("drawing-live-measure").assertContentDescriptionContains(LiveFeetInches.format(first.distanceTo(second)),substring=true)
        // Small motion does not alternate the acquired corner or change the canvas under the pen.
        ui.onNodeWithTag("workspace-canvas").performTouchInput { moveBy(Offset(2f,1f)) }
        assertEquals(second,model.state.value.draftTarget!!.point)
        assertEquals(before,ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot)
        assertEquals(original,store.load())
        capture("geometry-snapped-line")
        ui.onNodeWithTag("workspace-canvas").performTouchInput { up() }
        assertEquals(second,model.state.value.drawingPoints!![1])
        at=screen(third)+Offset(2f,-2f)
        ui.onNodeWithTag("workspace-canvas").performTouchInput { click(at) }
        assertEquals(third,model.state.value.drawingPoints!![2])
        ui.onNodeWithTag("proposed-outline-finish").performClick()
        val created=saved();val deck=created.objects.last()
        assertEquals(original.revision+1,created.revision)
        assertEquals(original.objects,created.objects.dropLast(1))
        assertEquals(listOf(first,second,third),deck.boundary.nodes.map { it.point })
        ui.onNodeWithTag("workspace-undo").performClick();assertEquals(original.objects,saved().objects)
        ui.onNodeWithTag("workspace-redo").performClick();val restored=saved()
        ui.onNodeWithTag("workspace-object-${original.objects.size}").performScrollTo().performClick()
        // Move one corner to another real house corner; the house stays locked and unchanged.
        val destination=house.boundary.nodes[3].point
        val targetScreen=screen(destination)+Offset(3f,-2f)
        val bounds=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        val handle=ui.onNodeWithTag("workspace-vertex-2")
        val handleCentre=handle.fetchSemanticsNode().boundsInRoot.center-bounds.topLeft
        handle.performTouchInput { down(center);moveBy(targetScreen-handleCentre) }
        val preview=model.state.value.preview!!
        assertEquals(destination,preview.objects.last().boundary.nodes[2].point)
        assertEquals(house,preview.objectById(house.id))
        ui.onNodeWithTag("geometry-snap-guide").assertExists()
        capture("geometry-snapped-vertex")
        handle.performTouchInput { cancel() }
        assertEquals(restored,saved());ui.onNodeWithTag("geometry-snap-guide").assertDoesNotExist()
        handle.performTouchInput { swipe(center,center+(targetScreen-handleCentre),300) }
        val edited=saved()
        assertEquals(destination,edited.objects.last().boundary.nodes[2].point)
        assertEquals(original.objects,edited.objects.dropLast(1))
        assertTrue(edited.objectById(house.id).locked)
        ui.onNodeWithTag("workspace-undo").performClick();assertEquals(created.objects,saved().objects)
        ui.onNodeWithTag("workspace-redo").performClick();val final=saved()
        assertEquals(edited.objects,final.objects)
        command("assist","snap") // Original grid preference restored; geometric snapping remains explicit.
        assertEquals(final,saved())
        assertFalse(DesignJsonCodec.encode(final).contains("GeometrySnap"))
        File(evidence,"geometry-snap-expected.json").writeText(DesignJsonCodec.encode(final))
    }
    @Test fun snapResultReopensWithoutTemporaryConstraints() {
        val expected=DesignJsonCodec.decode(File(evidence,"geometry-snap-expected.json").readText())
        open();assertEquals(expected,saved())
        assertTrue(context.getSharedPreferences("workspace-view",0).getBoolean("geometry-snap",false))
        assertNull(model.state.value.draftTarget)
        ui.onNodeWithTag("geometry-snap-guide").assertDoesNotExist()
        assertTrue(expected.objects.filter { it.siteTrace!=null }.all { it.locked })
        capture("geometry-snap-reopened")
    }
}
