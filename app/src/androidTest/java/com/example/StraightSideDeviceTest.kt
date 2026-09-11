package com.example

import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.DesignJsonCodec
import com.example.data.DesignWorkspaceStore
import com.example.export.DesignOutput
import com.example.export.DesignOutputSettings
import com.example.model.design.*
import com.example.ui.workspace.DesignWorkspaceViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Reuse the existing synthetic design. Actual held contacts, not injected geometry changes. */
class StraightSideDeviceTest {
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
        ui.waitUntil(15000) { model.state.value.siteBitmap!=null }
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
    private fun capture(name:String) {
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(300,3000)
        EmulatorCapture.save(context,evidence,name)
        File(evidence,"$name-semantics.txt").writeText(ui.onRoot().printToString())
    }
    private fun scale(d:ProjectDesign):Double {
        val b=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        return DesignViewport.fit(d,b.width.toDouble(),b.height.toDouble()).pixelsPerMetre
    }
    private fun poolIndex(d:ProjectDesign)=d.objects.indexOfFirst {
        it.kind==DesignObjectKind.POOL && DesignDimensions.measure(it.boundary).rectangular &&
            kotlin.math.abs(DesignDimensions.measure(it.boundary).widthMetres-9*DesignDimensions.FOOT)<1e-6
    }.also { assertTrue("The earlier hand-drawn pool must exist",it>=0) }

    @Test fun moveWholeSideWithRecoveryAndSnapping() {
        open();val initial=saved();val index=poolIndex(initial);val pool=initial.objects[index];val foot=DesignDimensions.FOOT
        assertTrue(context.getSharedPreferences("workspace-view",0).getBoolean("touch",false))
        assertTrue(context.getSharedPreferences("workspace-view",0).getBoolean("geometry-snap",false))
        assertFalse(context.getSharedPreferences("workspace-view",0).getBoolean("snap",true))
        command("assist","geometry");command("assist","snap")
        ui.onNodeWithTag("workspace-object-$index").performScrollTo().performClick()
        command("edit","sides")
        assertTrue(model.state.value.sideEditing);assertEquals(initial,saved())
        ui.onNodeWithTag("workspace-vertex-1").assertDoesNotExist()
        ui.onNodeWithTag("workspace-curve-1").assertDoesNotExist()
        val bounds=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        val pixels=scale(initial)
        val side=ui.onNodeWithTag("workspace-side-1")
        // Tangential drift must not skew either end or change the opposite side.
        side.performTouchInput { down(center);moveBy(Offset((2.1*foot*pixels).toFloat(),(0.45*foot*pixels).toFloat())) }
        val preview=model.state.value.preview!!;val enlarged=preview.objects[index]
        assertEquals(14*foot,DesignDimensions.measure(enlarged.boundary).lengthMetres,1e-7)
        assertEquals(9*foot,DesignDimensions.measure(enlarged.boundary).widthMetres,1e-7)
        assertEquals(pool.boundary.nodes[0],enlarged.boundary.nodes[0])
        assertEquals(pool.boundary.nodes[3],enlarged.boundary.nodes[3])
        assertEquals(pool.coping,enlarged.coping)
        assertEquals(initial.objects.filter { it.id!=pool.id },preview.objects.filter { it.id!=pool.id })
        assertEquals(initial,store.load());assertEquals(bounds,ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot)
        ui.onNodeWithTag("drawing-live-measure").assertContentDescriptionEquals("14′ 0″ × 9′ 0″")
        capture("pool-side-live")
        side.performTouchInput { up() }
        val changed=saved();assertEquals(initial.revision+1,changed.revision)
        assertEquals(enlarged,changed.objects[index])
        assertNotEquals(pool.copingFootprint!!.outerBoundary,changed.objects[index].copingFootprint!!.outerBoundary)
        ui.onNodeWithTag("drawing-live-measure").assertDoesNotExist()
        ui.onNodeWithTag("workspace-undo").performClick();assertEquals(initial.objects,saved().objects)
        assertTrue(model.state.value.canRedo)
        // A large jump through the opposite side cannot consume redo or create a folded pool.
        val beforeInvalid=saved()
        side.performTouchInput { down(center);moveBy(Offset((-20*foot*pixels).toFloat(),0f)) }
        assertNull(model.state.value.preview)
        ui.onNodeWithTag("drawing-live-measure").assertContentDescriptionContains("Invalid edit",substring=true)
        capture("pool-side-rejected")
        side.performTouchInput { up() }
        assertEquals(beforeInvalid,saved());assertTrue(model.state.value.canRedo)
        ui.onNodeWithTag("workspace-redo").performClick();val restored=saved()
        assertEquals(changed.objects,restored.objects)
        side.performTouchInput { down(center);moveBy(Offset((foot*pixels).toFloat(),0f));cancel() }
        assertEquals(restored,saved());ui.onNodeWithTag("drawing-live-measure").assertDoesNotExist()
        // Snap the top pool side to a real protected house edge while grid is also enabled.
        command("assist","geometry")
        val house=restored.objects.single { it.siteTrace?.role==SiteOutlineRole.HOUSE }
        val desiredY=house.boundary.nodes[4].point.y
        val top=restored.objects[index].boundary.edges()[0].pointAt(0.5)
        val delta=Offset(3f,(-(desiredY-top.y)*scale(restored)+2.0).toFloat())
        val topSide=ui.onNodeWithTag("workspace-side-0")
        topSide.performTouchInput { down(center);moveBy(delta) }
        val aligned=model.state.value.preview!!
        assertEquals(desiredY,aligned.objects[index].boundary.nodes[0].point.y,1e-7)
        assertEquals(desiredY,aligned.objects[index].boundary.nodes[1].point.y,1e-7)
        assertEquals(house,aligned.objectById(house.id));assertTrue(aligned.objectById(house.id).locked)
        assertEquals(restored.siteImage,aligned.siteImage)
        ui.onNodeWithTag("geometry-snap-guide").assertExists()
        capture("pool-side-house-alignment")
        topSide.performTouchInput { up() }
        saved();ui.onNodeWithTag("workspace-undo").performClick()
        assertEquals(restored.objects,saved().objects)
        // Side mode is not an alternate way around a protected site's lock.
        val houseIndex=restored.objects.indexOfFirst { it.id==house.id }
        ui.onNodeWithTag("workspace-object-$houseIndex").performScrollTo().performClick()
        assertFalse(model.state.value.sideEditing)
        ui.onNodeWithTag("workspace-commands").performClick()
        ui.onNodeWithTag("radial-category-edit").performTouchInput { click(center) }
        ui.onNodeWithTag("radial-action-sides").assertIsNotEnabled()
        ui.onNodeWithTag("radial-action-sides").performTouchInput { click(center) }
        ui.onNodeWithTag("workspace-radial").assertExists()
        ui.onNodeWithTag("radial-centre").performTouchInput { click(center) }
        ui.onNodeWithTag("radial-centre").performTouchInput { click(center) }
        command("assist","snap") // Restore the starting preferences, not project history.
        ui.onNodeWithTag("workspace-object-$index").performScrollTo().performClick()
        command("edit","sides");capture("pool-side-completed")
        val finished=saved()
        assertEquals(initial.objects.filter { it.id!=pool.id },finished.objects.filter { it.id!=pool.id })
        assertEquals(initial.siteImage,finished.siteImage)
        File(evidence,"pool-side-expected.json").writeText(DesignJsonCodec.encode(finished))
    }
    @Test fun wholeSideReopenAndExport()=runBlocking {
        val expected=DesignJsonCodec.decode(File(evidence,"pool-side-expected.json").readText())
        open();assertEquals(expected,saved());assertFalse(model.state.value.sideEditing)
        assertNull(model.state.value.preview)
        ui.onNodeWithTag("workspace-side-0").assertDoesNotExist()
        ui.onNodeWithTag("drawing-live-measure").assertDoesNotExist()
        val index=poolIndex(expected)
        assertEquals(14*DesignDimensions.FOOT,DesignDimensions.measure(expected.objects[index].boundary).lengthMetres,1e-7)
        ui.onNodeWithTag("workspace-object-$index").performScrollTo().performClick()
        capture("pool-side-reopened")
        DesignOutput.png(context,expected,1400,1000,DesignOutputSettings(includeSourceImage=false,includeMeasurements=false))!!
            .copyTo(File(evidence,"pool-side-export.png"),overwrite=true)
        assertEquals(expected,saved())
    }
}
