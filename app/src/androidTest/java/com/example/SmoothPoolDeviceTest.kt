package com.example

import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.*
import com.example.model.design.*
import com.example.ui.workspace.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.*

class SmoothPoolDeviceTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private val store get()=DesignWorkspaceStore(DesignWorkspaceStore.fileIn(context.filesDir))
    private val evidence get()=File(context.getExternalFilesDir(null),"workspace-evidence").apply { mkdirs() }
    private lateinit var model:DesignWorkspaceViewModel
    private var downTime=0L
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
    private fun viewport():DesignViewport {
        val b=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        return DesignViewport.fit(model.state.value.document!!,b.width.toDouble(),b.height.toDouble())
    }
    private fun screen(p:DesignPoint):Offset {
        val node=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode()
        val at=viewport().toScreen(p)
        assertTrue("point within canvas",at.x in 2.0..(node.size.width-2.0) && at.y in 2.0..(node.size.height-2.0))
        return node.positionOnScreen+Offset(at.x.toFloat(),at.y.toFloat())
    }
    private fun pen(action:Int,at:Offset,flags:Int=0,palm:Boolean=false) {
        if(action==MotionEvent.ACTION_DOWN) downTime=SystemClock.uptimeMillis()
        val count=if(palm) 2 else 1
        val props=Array(count) { i -> MotionEvent.PointerProperties().apply { id=i;toolType=if(i==0) MotionEvent.TOOL_TYPE_STYLUS else MotionEvent.TOOL_TYPE_FINGER } }
        val coords=Array(count) { i -> MotionEvent.PointerCoords().apply { x=at.x+i*40;y=at.y+i*40;pressure=.6f;size=.04f } }
        val event=MotionEvent.obtain(downTime,SystemClock.uptimeMillis(),action,count,props,coords,0,0,1f,1f,0,0,InputDevice.SOURCE_STYLUS,flags)
        try { assertTrue(instrumentation.uiAutomation.injectInputEvent(event,true)) } finally { event.recycle() }
        ui.waitForIdle()
    }
    private fun tap(p:DesignPoint) { val at=screen(p);pen(MotionEvent.ACTION_DOWN,at);pen(MotionEvent.ACTION_UP,at) }
    private fun smooth(b:DesignBoundary) { b.nodes.forEach { assertTrue(b.joinDeflectionRadians(it.vertexId)<1e-7) } }
    private fun capture(name:String) {
        ui.waitForIdle();EmulatorCapture.save(context,evidence,name)
        File(evidence,"$name-semantics.txt").writeText(ui.onRoot().printToString())
    }
    @Test fun authorAndReshapeWithSystemPen() {
        open();val before=saved();val prefs=context.getSharedPreferences("workspace-view",0)
        if(prefs.getBoolean("touch",false)) command("assist","touch")
        if(prefs.getBoolean("snap",false)) command("assist","snap")
        if(prefs.getBoolean("geometry-snap",false)) command("assist","geometry")
        assertFalse(prefs.getBoolean("touch",true))
        command("draw","smooth_pool")
        ui.onNodeWithTag("smooth-outline-controls").assertExists()
        val box=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        val view=viewport();val center=view.toWorld(box.width/2.0,box.height/2.0)
        val points=(0 until 8).map { i -> val a=i*2*PI/8;DesignPoint(center.x+4*(cos(a)+.36*cos(2*a)),center.y+2.5*sin(a)) }
        for((i,p) in points.withIndex()) {
            tap(p);assertEquals(i+1,model.state.value.smoothDraft!!.points.size)
            assertEquals(before,store.load())
        }
        val first=model.state.value.smoothDraft!!.points.first()
        val closing=screen(first)
        pen(MotionEvent.ACTION_DOWN,closing)
        assertTrue(model.state.value.draftTarget!!.closing);assertNotNull(model.state.value.smoothPreview)
        ui.onNodeWithTag("drawing-live-measure").assertExists();capture("smooth-authoring-live")
        pen(MotionEvent.ACTION_UP,closing)
        val created=saved();val pool=created.objects.last();smooth(pool.boundary)
        assertEquals(before.objects,created.objects.dropLast(1));assertEquals(before.revision+1,created.revision)
        assertEquals(16,pool.boundary.nodes.size);assertTrue(pool.boundary.nodes.any { it.bulge<0 })
        assertNotNull(pool.copingFootprint);assertEquals(SmoothEditMode.SHAPE,model.state.value.smoothMode)
        ui.onNodeWithTag("workspace-vertex-0").assertDoesNotExist()
        ui.onNodeWithTag("workspace-smooth-handle").assertExists()
        val original=pool.boundary;val from=screen(original.nodes[0].point);val at=screen(original.nodes[0].point.translated(.3,.25))
        pen(MotionEvent.ACTION_DOWN,from);pen(MotionEvent.ACTION_MOVE,at)
        val preview=model.state.value.preview!!;val proposed=preview.objectById(pool.id)
        smooth(proposed.boundary);assertEquals(created,store.load())
        assertEquals(12,original.edges().zip(proposed.boundary.edges()).count { (a,b)->a.id==b.id && a.start.point==b.start.point && a.end==b.end && a.start.bulge==b.start.bulge })
        capture("smooth-anchor-live");pen(MotionEvent.ACTION_UP,at)
        val changed=saved();assertEquals(preview,changed);assertNotEquals(pool.copingFootprint!!.outerBoundary,changed.objectById(pool.id).copingFootprint!!.outerBoundary)
        ui.onNodeWithTag("workspace-undo").performClick();assertEquals(created.objects,saved().objects)
        ui.onNodeWithTag("workspace-redo").performClick();assertEquals(changed.objects,saved().objects)
        command("view","fit")
        // ACTION_CANCEL and rejected pen-up must not commit the visible candidate.
        for(ending in listOf(MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_UP)) {
            val initial=saved();val b=initial.objectById(pool.id).boundary
            val x=screen(b.nodes[0].point);val y=screen(b.nodes[0].point.translated(.15,.12))
            pen(MotionEvent.ACTION_DOWN,x);pen(MotionEvent.ACTION_MOVE,y);assertNotNull(model.state.value.preview)
            pen(ending,y,if(ending==MotionEvent.ACTION_UP) MotionEvent.FLAG_CANCELED else 0)
            assertEquals(initial,saved());assertNull(model.state.value.preview)
        }
        // Simulated second contact cancels an in-flight edit, not a physical palm-rejection claim.
        val initial=saved();val p=initial.objectById(pool.id).boundary.nodes[0].point
        val x=screen(p);val y=screen(p.translated(.15,.12))
        pen(MotionEvent.ACTION_DOWN,x);pen(MotionEvent.ACTION_MOVE,y)
        pen(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),y,palm=true)
        assertNull(model.state.value.preview)
        pen(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),y,palm=true)
        pen(MotionEvent.ACTION_UP,y);assertEquals(initial,saved())
        capture("smooth-anchor-completed")
        File(evidence,"smooth-author-expected.json").writeText(DesignJsonCodec.encode(saved()))
    }
    @Test fun radiusAndInvalidDragUseTheSameSavedPool() {
        open();val initial=saved();val pool=initial.objects.last();val index=initial.objects.lastIndex
        ui.onNodeWithTag("workspace-object-$index").performScrollTo().performClick()
        command("edit","radius")
        assertEquals(SmoothEditMode.RADIUS,model.state.value.smoothMode)
        val edge=pool.boundary.edges()[12];var from=screen(edge.pointAt(.5));val at=screen(edge.pointAt(.5).translated(.2,.1))
        pen(MotionEvent.ACTION_DOWN,from);pen(MotionEvent.ACTION_MOVE,at)
        val preview=model.state.value.preview!!;val after=preview.objectById(pool.id)
        smooth(after.boundary)
        assertEquals(14,pool.boundary.edges().zip(after.boundary.edges()).count { (a,b)->a.start==b.start && a.end==b.end })
        val radius=after.boundary.edges()[12].radiusMetres!!
        ui.onNodeWithTag("drawing-live-measure").assertContentDescriptionEquals("R "+LiveFeetInches.format(radius))
        capture("smooth-radius-live");pen(MotionEvent.ACTION_UP,at);assertEquals(preview,saved())
        ui.onNodeWithTag("workspace-undo").performClick();assertEquals(initial.objects,saved().objects)
        assertTrue(model.state.value.canRedo)
        command("view","fit");from=screen(edge.pointAt(.5))
        // Drag to the wrong side of the start tangent: a radius cannot flip sign to comply.
        val t=edge.tangentAt(0.0);val wrong=edge.start.point.translated(t.y*sign(edge.sweepRadians)*.2,-t.x*sign(edge.sweepRadians)*.2)
        val target=screen(wrong);pen(MotionEvent.ACTION_DOWN,from);pen(MotionEvent.ACTION_MOVE,target)
        assertNull(model.state.value.preview);ui.onNodeWithTag("drawing-live-measure").assertExists()
        capture("smooth-radius-rejected");pen(MotionEvent.ACTION_UP,target)
        assertEquals(initial.objects,saved().objects);assertTrue(model.state.value.canRedo)
        ui.onNodeWithTag("workspace-redo").performClick();val done=saved();assertEquals(after,done.objectById(pool.id))
        assertEquals(initial.objects.dropLast(1),done.objects.dropLast(1));assertEquals(initial.siteImage,done.siteImage)
        File(evidence,"smooth-final-expected.json").writeText(DesignJsonCodec.encode(done))
        capture("smooth-radius-completed")
    }
    @Test fun reopenAndCanceledNewPoolKeepTheStoredResult() {
        open();val expected=DesignJsonCodec.decode(File(evidence,"smooth-final-expected.json").readText())
        assertEquals(expected,saved());assertEquals(SmoothEditMode.OFF,model.state.value.smoothMode)
        assertNull(model.state.value.smoothDraft);assertNull(model.state.value.smoothPreview)
        smooth(expected.objects.last().boundary);capture("smooth-reopened")
        command("draw","smooth_pool")
        val box=ui.onNodeWithTag("workspace-canvas").fetchSemanticsNode().boundsInRoot
        val mid=viewport().toWorld(box.width/2.0,box.height/2.0)
        tap(mid);tap(mid.translated(2.0,0.0));tap(mid.translated(2.0,2.0))
        assertNotNull(model.state.value.smoothPreview)
        ui.onNodeWithTag("smooth-outline-back").performClick();assertEquals(2,model.state.value.smoothDraft!!.points.size)
        ui.onNodeWithTag("smooth-outline-cancel").performClick();assertEquals(expected,saved())
        assertNull(model.state.value.smoothDraft)
        // Leave the older layout tests their existing preference, without changing project state.
        val prefs=context.getSharedPreferences("workspace-view",0)
        if(!prefs.getBoolean("touch",false)) command("assist","touch")
        if(!prefs.getBoolean("geometry-snap",false)) command("assist","geometry")
        assertEquals(expected,saved())
        File(evidence,"smooth-restored.json").writeText(DesignJsonCodec.encode(saved()))
    }
}
