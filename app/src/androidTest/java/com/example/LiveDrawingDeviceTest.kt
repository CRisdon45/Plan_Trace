package com.example

import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.*
import com.example.export.*
import com.example.model.design.*
import com.example.ui.workspace.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real artwork contacts with a pointer held while observing feedback; never a mock label. */
class LiveDrawingDeviceTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val store get()=DesignWorkspaceStore(DesignWorkspaceStore.fileIn(context.filesDir))
    private val evidence get()=File(context.getExternalFilesDir(null),"workspace-evidence").apply { mkdirs() }
    private lateinit var model: DesignWorkspaceViewModel
    @Before fun emulatorOnly() { assumeTrue(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone")) }
    private fun open() {
        ui.onNodeWithContentDescription("Projects").performClick()
        ui.onNodeWithTag("open-design-workspace").performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithTag("workspace-canvas").fetchSemanticsNodes().size==1 }
        ui.runOnIdle { model=ViewModelProvider(ui.activity)[DesignWorkspaceViewModel::class.java] }
    }
    private fun saved(): ProjectDesign {
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
        val bounds=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        val view=DesignViewport.fit(model.state.value.document!!,bounds.width.toDouble(),bounds.height.toDouble())
        val at=view.toScreen(point)
        return Offset(at.x.toFloat(),at.y.toFloat())
    }
    private fun tap(point:DesignPoint) { val at=screen(point);ui.onNodeWithTag("workspace-canvas").performTouchInput { click(at) } }
    private fun capture(name:String) {
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(300,3000)
        EmulatorCapture.save(context,evidence,name)
        File(evidence,"$name-semantics.txt").writeText(ui.onRoot().printToString())
    }
    @Test fun handDrawWithLiveDistances() {
        open();val original=saved();val f=DesignDimensions.FOOT
        assertTrue(context.getSharedPreferences("workspace-view",0).getBoolean("touch",false))
        assertFalse(context.getSharedPreferences("workspace-view",0).getBoolean("snap",true))
        val start=original.siteImage!!.toWorld(ImagePoint(340.0,420.0))
        command("assist","snap")
        command("draw","pool")
        ui.onNodeWithTag("proposed-outline-controls").assertExists()
        tap(start)
        val first=model.state.value.drawingPoints!!.single()
        val ten=screen(first.translated(10.1*f,0.1*f))
        ui.onNodeWithTag("workspace-canvas").performTouchInput { down(ten) }
        ui.onNodeWithTag("drawing-live-measure").assertContentDescriptionEquals("10′ 0″")
        val twelve=screen(first.translated(12.1*f,0.1*f))
        ui.onNodeWithTag("workspace-canvas").performTouchInput { moveTo(twelve) }
        ui.onNodeWithTag("drawing-live-measure").assertContentDescriptionEquals("12′ 0″")
        val indicated=model.state.value.draftTarget!!.point
        assertEquals(original,store.load()) // Live feedback did not write a partially drawn pool.
        capture("live-pool-line")
        ui.onNodeWithTag("workspace-canvas").performTouchInput { up() }
        assertEquals(indicated,model.state.value.drawingPoints!![1])
        ui.onNodeWithTag("drawing-live-measure").assertDoesNotExist()
        tap(indicated.translated(0.1*f,-9.1*f))
        tap(first.translated(0.0,-9.0*f))
        val nearFirst=screen(first.translated(0.03*f,0.02*f))
        ui.onNodeWithTag("workspace-canvas").performTouchInput { down(nearFirst) }
        ui.onNodeWithTag("drawing-live-measure").assertContentDescriptionContains("Close · 9′ 0″",substring=true)
        assertEquals(first,model.state.value.draftTarget!!.point)
        capture("live-closing-line")
        ui.onNodeWithTag("workspace-canvas").performTouchInput { up() }
        val created=saved();val pool=created.objects.last()
        assertEquals(original.revision+1,created.revision)
        assertEquals(original.objects,created.objects.dropLast(1))
        assertNotNull(pool.copingFootprint)
        assertEquals(12*f,DesignDimensions.measure(pool.boundary).lengthMetres,1e-8)
        assertEquals(9*f,DesignDimensions.measure(pool.boundary).widthMetres,1e-8)
        ui.onNodeWithTag("workspace-undo").performClick();assertEquals(original.objects,saved().objects)
        ui.onNodeWithTag("workspace-redo").performClick();val restored=saved()
        assertEquals(created.objects,restored.objects)
        // Undoing object creation clears selection. Redo restores geometry, not an implicit
        // selection, so explicitly choose the restored pool before targeting its handles.
        ui.onNodeWithTag("workspace-object-${original.objects.size}").performClick()
        // Real edits show both affected edges; cancellation preserves committed geometry and disk.
        val handle=ui.onNodeWithTag("workspace-vertex-1")
        handle.performTouchInput { down(center);moveBy(Offset(14f,-5f)) }
        ui.onNodeWithTag("drawing-live-measure").assertExists()
        val preview=model.state.value.preview!!
        val expected=LiveMeasurements.editing(restored,preview,DesignHit.Vertex(pool.id,pool.boundary.nodes[1].vertexId),pool.boundary.nodes[1].point)
        ui.onNodeWithTag("drawing-live-measure").assertContentDescriptionEquals(expected.lines.joinToString(". "))
        capture("live-vertex-edit")
        handle.performTouchInput { cancel() }
        assertEquals(restored,saved());ui.onNodeWithTag("drawing-live-measure").assertDoesNotExist()
        command("assist","snap")
        // The identical target feedback supports existing-site corner creation too.
        command("view","site")
        ui.onNodeWithTag("site-draw-property").performScrollTo().performClick();ui.waitForIdle()
        val a=original.siteImage!!.toWorld(ImagePoint(100.0,100.0))
        val b=original.siteImage!!.toWorld(ImagePoint(500.0,100.0))
        tap(a);val at=screen(b)
        ui.onNodeWithTag("workspace-canvas").performTouchInput { down(at) }
        val siteLive=LiveMeasurements.segment(model.state.value.drawingPoints!!,model.state.value.draftTarget!!)!!
        ui.onNodeWithTag("drawing-live-measure").assertContentDescriptionEquals(siteLive.lines.joinToString(". "))
        capture("live-site-line")
        ui.onNodeWithTag("workspace-canvas").performTouchInput { cancel() }
        ui.onNodeWithTag("site-outline-cancel").performClick();assertEquals(restored,saved())
        // Concave deck boundary is authored at the pen, not inserted from a preset or a form.
        command("draw","paving")
        listOf(0.0 to 0.0,16.0 to 0.0,16.0 to -5.0,8.0 to -5.0,8.0 to -12.0,0.0 to -12.0).forEach { (x,y) ->
            tap(first.translated(x*f,y*f))
        }
        ui.onNodeWithTag("proposed-outline-finish").performClick()
        val finished=saved();assertEquals(restored.objects,finished.objects.dropLast(1))
        assertEquals(6,finished.objects.last().boundary.nodes.size)
        assertEquals(DesignObjectKind.PAVING,finished.objects.last().kind);assertNull(finished.objects.last().coping)
        assertFalse(DesignJsonCodec.encode(finished).contains("drawing-live-measure"))
        capture("hand-drawn-pool-and-deck")
        File(evidence,"live-drawing-expected.json").writeText(DesignJsonCodec.encode(finished))
    }
    @Test fun liveDrawingReopen()=runBlocking {
        val expected=DesignJsonCodec.decode(File(evidence,"live-drawing-expected.json").readText())
        open();assertEquals(expected,saved())
        assertTrue(model.state.value.drawingPoints==null)
        ui.onNodeWithTag("drawing-live-measure").assertDoesNotExist()
        DesignOutput.png(context,expected,1400,1000,DesignOutputSettings(includeSourceImage=false))!!
            .copyTo(File(evidence,"hand-drawn-export.png"),overwrite=true)
        capture("hand-drawn-reopened")
        assertEquals(expected,saved())
    }
}
