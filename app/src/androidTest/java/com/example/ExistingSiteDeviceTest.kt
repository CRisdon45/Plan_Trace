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

/** Continues the owned synthetic image after the source tests. No client data or physical device. */
class ExistingSiteDeviceTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val store get()=DesignWorkspaceStore(DesignWorkspaceStore.fileIn(context.filesDir))
    private val evidence get()=File(context.getExternalFilesDir(null),"workspace-evidence").apply { mkdirs() }
    private lateinit var model:DesignWorkspaceViewModel
    @Before fun disposableOnly() { assumeTrue(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone")) }
    private fun open() {
        ui.onNodeWithContentDescription("Projects").performClick();ui.onNodeWithTag("open-design-workspace").performClick()
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
    }
    private fun start(role:String) {
        command("view","site")
        ui.onNodeWithTag("site-draw-$role").performScrollTo().performClick()
        ui.onNodeWithTag("site-outline-controls").assertExists();ui.waitForIdle()
    }
    private fun corner(x:Double,y:Double,drag:Boolean=false) {
        val doc=saved();val box=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        val view=DesignViewport.fit(doc,box.width.toDouble(),box.height.toDouble())
        val p=view.toScreen(doc.siteImage!!.toWorld(ImagePoint(x,y)))
        ui.onNodeWithTag("workspace-canvas").performTouchInput {
            val at=Offset(p.x.toFloat(),p.y.toFloat())
            if(drag) swipe(at+Offset(-8f,6f),at,220) else click(at)
        }
    }
    private fun capture(name:String) {
        ui.waitForIdle();InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(300,3000)
        EmulatorCapture.save(context,evidence,name)
        File(evidence,"$name-semantics.txt").writeText(ui.onRoot().printToString())
    }
    @Test fun drawProtectedSiteOutlines() {
        open();val original=saved();assertNotNull(original.siteImage?.calibration)
        // Earlier scenario enables Touch edit; verify the explicit test mode before authoring.
        assertTrue(context.getSharedPreferences("workspace-view",0).getBoolean("touch",false))
        assertFalse(context.getSharedPreferences("workspace-view",0).getBoolean("snap",true))
        start("house")
        corner(120.0,100.0);corner(640.0,100.0,true)
        assertEquals(original,saved());assertEquals(2,model.state.value.siteDraft!!.points.size)
        ui.onNodeWithTag("workspace-canvas").performTouchInput { down(center);moveBy(Offset(12f,8f));cancel() }
        assertEquals(2,model.state.value.siteDraft!!.points.size);assertEquals(original,saved())
        ui.onNodeWithTag("site-outline-cancel").performClick()
        assertEquals(original,saved());assertNull(model.state.value.siteDraft)
        // Site-specific Undo removes a draft corner, not an existing pool or the source.
        start("house");corner(120.0,100.0);corner(640.0,100.0)
        ui.onNodeWithTag("workspace-undo").performClick()
        assertEquals(1,model.state.value.siteDraft!!.points.size);assertEquals(original,saved())
        corner(640.0,100.0);corner(640.0,160.0);corner(760.0,160.0);corner(760.0,310.0);corner(120.0,310.0)
        capture("site-house-in-progress")
        corner(120.0,100.0) // Deliberate close by returning to the first corner.
        val houseDoc=saved();val house=houseDoc.objects.last()
        assertEquals(original.revision+1,houseDoc.revision);assertTrue(house.locked)
        assertEquals(6,house.boundary.nodes.size);assertEquals(SiteOutlineRole.HOUSE,house.siteTrace!!.role)
        assertEquals(original.objects,houseDoc.objects.dropLast(1))
        ui.onNodeWithTag("workspace-vertex-0").assertDoesNotExist()
        // Target the actual protected house edge, not an arbitrary empty canvas location.
        val protectedBox=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        val protectedView=DesignViewport.fit(houseDoc,protectedBox.width.toDouble(),protectedBox.height.toDouble())
        val protectedPoint=protectedView.toScreen(house.boundary.edges().first().pointAt(0.5))
        ui.onNodeWithTag("workspace-canvas").performTouchInput {
            val at=Offset(protectedPoint.x.toFloat(),protectedPoint.y.toFloat())
            swipe(at,at+Offset(20f,10f),220)
        }
        assertEquals(houseDoc,saved());command("view","fit")
        start("property");corner(60.0,60.0);corner(1140.0,60.0);corner(1140.0,840.0);corner(60.0,840.0)
        ui.onNodeWithTag("site-outline-finish").performClick()
        val both=saved();assertEquals(original.objects.size+2,both.objects.size)
        assertEquals(SiteOutlineRole.PROPERTY,both.objects.last().siteTrace!!.role)
        ui.onNodeWithTag("workspace-undo").performClick();assertEquals(houseDoc.objects,saved().objects)
        ui.onNodeWithTag("workspace-redo").performClick();assertEquals(both.objects,saved().objects)
        // Unlock intentionally, edit the house through its actual vertex handle, then relock.
        ui.onNodeWithTag("workspace-object-${original.objects.size}").performClick()
        command("view","site");ui.onNodeWithTag("site-outline-lock").performScrollTo().performClick()
        assertFalse(saved().objects[original.objects.size].locked)
        ui.onNodeWithTag("site-close").performScrollTo().performClick()
        ui.onNodeWithTag("workspace-vertex-1").performTouchInput { swipe(center,center+Offset(8f,-6f),220) }
        val edited=saved();assertTrue(edited.objects[original.objects.size].siteTrace!!.adjusted)
        assertEquals(original.objects,edited.objects.take(original.objects.size))
        ui.onNodeWithTag("workspace-undo").performClick()
        assertEquals(house.boundary,saved().objects[original.objects.size].boundary)
        command("view","site");ui.onNodeWithTag("site-outline-lock").performScrollTo().performClick()
        ui.onNodeWithTag("site-close").performScrollTo().performClick()
        val beforeMove=saved()
        // Moving the raster cannot drag existing site objects along or preserve a false alignment claim.
        command("view","site");ui.onNodeWithTag("site-move").performScrollTo().performClick()
        ui.onNodeWithTag("workspace-canvas").performTouchInput { swipe(center,center+Offset(20f,12f),220) }
        val moved=saved();assertEquals(beforeMove.objects,moved.objects)
        assertNotEquals(beforeMove.siteImage!!.topLeft,moved.siteImage!!.topLeft)
        ui.onNodeWithTag("site-close").performScrollTo().performClick()
        ui.onNodeWithTag("workspace-registration-warning").assertExists();capture("site-registration-warning")
        ui.onNodeWithTag("workspace-undo").performClick()
        assertEquals(beforeMove.siteImage,saved().siteImage)
        ui.onNodeWithTag("workspace-registration-warning").assertDoesNotExist()
        // Hide source to prove the house/property are actual vectors, not markings baked into a bitmap.
        command("view","site");ui.onNodeWithTag("site-visibility").performScrollTo().performClick()
        ui.onNodeWithTag("site-close").performScrollTo().performClick()
        capture("site-outlines-without-image")
        command("view","site");ui.onNodeWithTag("site-visibility").performScrollTo().performClick()
        ui.onNodeWithTag("site-close").performScrollTo().performClick()
        val finished=saved();assertEquals(beforeMove.objects,finished.objects)
        capture("site-outlines-completed")
        File(evidence,"site-outlines-expected.json").writeText(DesignJsonCodec.encode(finished))
    }
    @Test fun siteOutlinesReopenAndExport()=runBlocking {
        val expected=DesignJsonCodec.decode(File(evidence,"site-outlines-expected.json").readText())
        open();assertEquals(expected,saved())
        assertTrue(saved().objects.filter { it.siteTrace!=null }.all { it.locked && it.confidence==GeometryConfidence.TRACED })
        capture("site-outlines-reopened")
        DesignOutput.png(context,expected,1400,1000,DesignOutputSettings(includeSourceImage=false))!!
            .copyTo(File(evidence,"site-outlines-vector-export.png"),overwrite=true)
        assertEquals(expected,saved())
    }
}
