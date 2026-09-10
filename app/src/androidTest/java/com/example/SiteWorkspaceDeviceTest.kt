package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
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

/** Original raster fixture, not a client site. Intake callback is real; external picker UI is not automated. */
class SiteWorkspaceDeviceTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val draft get()=DesignWorkspaceStore(DesignWorkspaceStore.fileIn(context.filesDir))
    private val evidence get()=File(context.getExternalFilesDir(null),"workspace-evidence").apply{mkdirs()}
    private lateinit var model:DesignWorkspaceViewModel
    @Before fun disposableOnly() { assumeTrue(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone")) }
    private fun open() {
        ui.onNodeWithContentDescription("Projects").performClick();ui.onNodeWithTag("open-design-workspace").performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithTag("workspace-canvas").fetchSemanticsNodes().size==1 }
        ui.runOnIdle { model=ViewModelProvider(ui.activity)[DesignWorkspaceViewModel::class.java] }
    }
    private fun command(category:String,action:String) {
        ui.onNodeWithTag("workspace-commands").performClick()
        ui.onNodeWithTag("radial-category-$category").performTouchInput{click(center)}
        ui.onNodeWithTag("radial-action-$action").performTouchInput{click(center)}
    }
    private fun saved():ProjectDesign {
        ui.waitUntil(15000) { ui.onAllNodes(hasTestTag("workspace-save-status") and hasText("Saved on device")).fetchSemanticsNodes().isNotEmpty() }
        return draft.load()!!
    }
    private fun capture(name:String) {
        ui.waitForIdle();InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(300,3000)
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(evidence,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        File(evidence,"$name-semantics.txt").writeText(ui.onRoot().printToString())
    }
    private fun fixture():File {
        val b=Bitmap.createBitmap(1200,900,Bitmap.Config.ARGB_8888);val c=Canvas(b)
        c.drawColor(0xFFE6E4D8.toInt());val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color=0xFFBDBEB6.toInt();c.drawRect(120f,100f,640f,310f,paint)
        paint.color=0xFF7C8680.toInt();c.drawRect(640f,160f,760f,310f,paint)
        paint.color=0xFF3E4F47.toInt();paint.style=Paint.Style.STROKE;paint.strokeWidth=6f
        c.drawRect(60f,60f,1140f,840f,paint);paint.style=Paint.Style.FILL;paint.textSize=28f
        c.drawText("SYNTHETIC SITE IMAGE - NOT A CLIENT PROPERTY",90f,790f,paint)
        c.drawText("EXISTING HOUSE (RASTER REFERENCE)",145f,190f,paint)
        paint.color=0xFFBD472A.toInt();c.drawCircle(250f,650f,8f,paint);c.drawCircle(850f,650f,8f,paint)
        paint.strokeWidth=3f;c.drawLine(250f,650f,850f,650f,paint);c.drawText("40 ft reference",440f,625f,paint)
        val f=File(context.cacheDir,"synthetic-site.png");f.outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle();return f
    }
    @Test fun siteImportCalibrateAndMove() {
        open();val original=saved();assertNull(original.siteImage)
        command("view","site");ui.onNodeWithTag("site-import").assertExists()
        val f=fixture()
        // Same ViewModel method as the ACTION_OPEN_DOCUMENT result. No special import/model path.
        ui.runOnIdle { model.importSiteImage(Uri.fromFile(f)) }
        ui.waitUntil(15000) { model.state.value.siteBitmap!=null && !model.state.value.siteImporting && model.state.value.saved }
        val imported=saved();assertEquals(original.objects,imported.objects)
        f.delete() // The source must remain usable without the external original.
        ui.onNodeWithTag("site-calibrate").performScrollTo().performClick();ui.waitForIdle()
        val box=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        val view=DesignViewport.fit(imported,box.width.toDouble(),box.height.toDouble())
        fun tap(p:ImagePoint) {
            val screen=view.toScreen(imported.siteImage!!.toWorld(p))
            ui.onNodeWithTag("workspace-canvas").performTouchInput { click(Offset(screen.x.toFloat(),screen.y.toFloat())) }
        }
        tap(ImagePoint(250.0,650.0));tap(ImagePoint(850.0,650.0))
        ui.onNodeWithTag("site-distance-40").performScrollTo().performClick()
        val scaled=saved();assertEquals(original.objects,scaled.objects)
        assertEquals(40*0.3048,scaled.siteImage!!.calibration!!.distanceMetres,1e-8)
        assertEquals(0.02032,scaled.siteImage!!.metresPerPixel,0.00001)
        ui.onNodeWithTag("site-move").performScrollTo().performClick()
        ui.onNodeWithTag("workspace-canvas").performTouchInput { swipe(center,center+Offset(35f,20f),350) }
        val moved=saved();assertNotEquals(scaled.siteImage!!.topLeft,moved.siteImage!!.topLeft)
        assertEquals(scaled.objects,moved.objects);assertEquals(scaled.siteImage!!.calibration,moved.siteImage!!.calibration)
        ui.onNodeWithTag("site-close").performScrollTo().performClick()
        ui.onNodeWithTag("workspace-undo").performClick();assertEquals(scaled.siteImage,saved().siteImage)
        ui.onNodeWithTag("workspace-redo").performClick();assertEquals(moved.siteImage,saved().siteImage)
        command("view","site");ui.onNodeWithTag("site-move").performScrollTo().performClick()
        val beforeCancel=saved()
        ui.onNodeWithTag("workspace-canvas").performTouchInput { down(center);moveBy(Offset(30f,10f));cancel() }
        assertEquals(beforeCancel,saved());ui.onNodeWithTag("site-tool-cancel").performClick()
        ui.onNodeWithTag("site-remove").performScrollTo().performClick();assertNull(saved().siteImage)
        ui.onNodeWithTag("site-close").performScrollTo().performClick();ui.onNodeWithTag("workspace-undo").performClick()
        val restored=saved();assertEquals(moved.siteImage,restored.siteImage)
        ui.waitUntil(10000) { model.state.value.siteBitmap!=null }
        command("view","fit");capture("site-registered-workspace")
        File(evidence,"site-expected.json").writeText(DesignJsonCodec.encode(restored))
    }
    @Test fun siteReopenAndExport() = runBlocking {
        val expected=DesignJsonCodec.decode(File(evidence,"site-expected.json").readText())
        open();assertEquals(expected,saved())
        ui.waitUntil(10000) { model.state.value.siteBitmap!=null }
        SiteImageStore.inFiles(context.filesDir).load(expected.siteImage!!.asset).recycle()
        capture("site-reopened-workspace")
        val png=DesignOutput.png(context,expected,1200,900)!!
        png.copyTo(File(evidence,"site-registered-export.png"),overwrite=true)
        val pdf=DesignOutput.pdf(context,expected,PdfExportOptions(includeTitleBlock=false))!!
        pdf.copyTo(File(evidence,"site-registered-export.pdf"),overwrite=true)
        assertEquals(expected,saved())
    }
}
