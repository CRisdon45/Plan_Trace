package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DesignJsonCodec
import com.example.data.DesignWorkspaceStore
import com.example.export.DesignOutput
import com.example.export.DesignOutputSettings
import com.example.model.design.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TangentDocumentTest {
    @get:Rule val temp=TemporaryFolder()
    private fun document(notch:Double=.3):ProjectDesign {
        val pool=DesignObject("pool","Synthetic tangent proof",DesignObjectKind.POOL,
            TangentProofCases.fixture(notch),coping=CopingSpec())
        val other=DesignObject("protected","Protected site reference",DesignObjectKind.SITE_OUTLINE,
            DesignFixtures.rectangle().translated(-15.0,-12.0),locked=true)
        return ProjectDesign("tangent-proof",listOf(pool,other))
    }
    private fun anchor(d:ProjectDesign,dx:Double=.35,dy:Double=.4):DesignCommand.MoveTangentAnchor {
        val b=d.objectById("pool").boundary
        return DesignCommand.MoveTangentAnchor("pool","v4",b.nodes[4].point.translated(dx,dy),b)
    }
    private fun radius(d:ProjectDesign):DesignCommand.SetTangentRadius {
        val b=d.objectById("pool").boundary
        return DesignCommand.SetTangentRadius("pool","e0",b.edges()[0].radiusMetres!!*.92,b)
    }
    @Test fun anchorPreviewCommitCopingAndOneUndo() {
        val initial=document();val s=DesignSession(initial);val command=anchor(initial)
        val preview=s.preview(command)
        assertEquals(initial,s.document);assertFalse(s.canUndo)
        val before=initial.objectById("pool");val after=preview.objectById("pool")
        TangentProofCases.smooth(after.boundary)
        assertEquals(before.coping,after.coping)
        assertNotEquals(before.copingFootprint!!.outerBoundary,after.copingFootprint!!.outerBoundary)
        assertEquals(initial.objectById("protected"),preview.objectById("protected"))
        assertEquals(12,before.boundary.edges().zip(after.boundary.edges()).count { (a,b)->TangentProofCases.sameEdge(a,b) })
        assertEquals(preview,s.execute(command));assertEquals(1L,s.document.revision)
        assertEquals(initial.objects,s.undo().objects);assertFalse(s.canUndo);assertTrue(s.canRedo)
        assertEquals(preview.objects,s.redo().objects)
    }
    @Test fun radiusUsesActualCommandAndKeepsFourteenEdges() {
        val initial=document();val s=DesignSession(initial);val cmd=radius(initial)
        val next=s.execute(cmd);val b=next.objectById("pool").boundary
        TangentProofCases.smooth(b)
        assertEquals(cmd.metres,b.edges()[0].radiusMetres!!,1e-8)
        assertEquals(14,initial.objectById("pool").boundary.edges().zip(b.edges()).count { (a,c)->TangentProofCases.sameEdge(a,c) })
        assertNotEquals(initial.objectById("pool").copingFootprint!!.outerBoundary,next.objectById("pool").copingFootprint!!.outerBoundary)
        assertEquals(initial.objects,s.undo().objects)
    }
    @Test fun discardedPreviewsLeaveHistoryAndStoredProjectAlone() {
        val initial=document();val s=DesignSession(initial)
        val file=File(temp.root,"draft.json");val store=DesignWorkspaceStore(file)
        store.save(initial);val bytes=file.readBytes()
        repeat(20) { s.preview(anchor(initial,.05+it*.002,.1)) }
        assertEquals(initial,s.document);assertFalse(s.canUndo);assertFalse(s.canRedo)
        assertArrayEquals(bytes,file.readBytes());assertEquals(initial,store.load())
    }
    @Test fun completedEditsRoundtripUsingRealCodecAndAtomicStore() {
        val s=DesignSession(document());s.execute(anchor(s.document));s.execute(radius(s.document))
        val accepted=s.document;val json=DesignJsonCodec.encode(accepted)
        assertEquals(accepted,DesignJsonCodec.decode(json));assertEquals(json,DesignJsonCodec.encode(DesignJsonCodec.decode(json)))
        val file=File(temp.root,"workspace.json");DesignWorkspaceStore(file).save(accepted)
        assertEquals(accepted,DesignWorkspaceStore(file).load())
        assertTrue(json.contains("\"version\":5"));assertFalse(json.contains("biarcBalance"))
    }
    @Test fun staleBoundaryCannotOverwriteAnotherEdit() {
        val initial=document();val s=DesignSession(initial);val old=anchor(initial)
        s.execute(DesignCommand.Translate("pool",.1,.2));val newer=s.document
        assertThrows(IllegalArgumentException::class.java) { s.execute(old) }
        assertEquals(newer,s.document);assertEquals(initial.objects,s.undo().objects)
        assertFalse(s.canUndo);assertTrue(s.canRedo)
    }
    @Test fun lockedPoolAndNonPoolCannotUseTangentCommands() {
        val initial=document();val s=DesignSession(initial);val cmd=anchor(initial)
        s.execute(DesignCommand.SetLocked("pool",true));val locked=s.document
        assertThrows(IllegalArgumentException::class.java) { s.execute(cmd) };assertEquals(locked,s.document)
        val b=initial.objectById("pool").boundary
        val other=ProjectDesign("other",listOf(DesignObject("pool","Not a pool",DesignObjectKind.PAVING,b)))
        assertThrows(IllegalArgumentException::class.java) { DesignCommands.apply(other,cmd) }
        val noCoping=ProjectDesign("missing-coping",listOf(initial.objectById("pool").copy(coping=null)))
        assertThrows(IllegalArgumentException::class.java) { DesignCommands.apply(noCoping,cmd) }
    }
    @Test fun failedAndNoOpCommandsDoNotConsumeRedo() {
        val initial=document();val s=DesignSession(initial);s.execute(anchor(initial));s.undo()
        val before=s.document;val b=before.objectById("pool").boundary
        assertThrows(IllegalArgumentException::class.java) {
            s.execute(DesignCommand.MoveTangentAnchor("pool","v4",b.nodes[2].point,b)) }
        assertEquals(before,s.document);assertTrue(s.canRedo)
        s.execute(DesignCommand.MoveTangentAnchor("pool","v4",b.nodes[4].point,b))
        s.execute(DesignCommand.SetTangentRadius("pool","e0",b.edges()[0].radiusMetres!!,b))
        assertEquals(before,s.document);assertTrue(s.canRedo);assertFalse(s.canUndo)
    }
    @Test fun infeasibleRadiusDoesNotRelaxItsFixedConnections() {
        val initial=document();val s=DesignSession(initial);val b=initial.objectById("pool").boundary
        assertThrows(IllegalArgumentException::class.java) { s.execute(DesignCommand.SetTangentRadius("pool","e0",1e-4,b)) }
        assertEquals(initial,s.document);assertFalse(s.canUndo)
    }
    @Test fun renderActualBeforeAfterAndReopenedOutputs()=runBlocking {
        // Use the production document/export renderer. No second proof renderer or client art.
        val original=document();val s=DesignSession(original);s.execute(anchor(original))
        val moved=s.document;s.execute(radius(moved));val edited=s.document
        val reopened=DesignJsonCodec.decode(DesignJsonCodec.encode(edited))
        val context=ApplicationProvider.getApplicationContext<Context>()
        val evidence=File("build/reports/design-geometry/freeform").apply { mkdirs() }
        // Exclude the remote protected object from the VIEW only, not from the saved project.
        fun view(d:ProjectDesign)=ProjectDesign(d.id,listOf(d.objectById("pool")),d.revision)
        val settings=DesignOutputSettings(includeMeasurements=false,includeSourceImage=false)
        val before=DesignOutput.png(context,view(original),1400,1000,settings)!!.readBytes()
        val after=DesignOutput.png(context,view(edited),1400,1000,settings)!!.readBytes()
        val restored=DesignOutput.png(context,view(reopened),1400,1000,settings)!!.readBytes()
        assertFalse(before.contentEquals(after));assertArrayEquals(after,restored)
        File(evidence,"before.png").writeBytes(before);File(evidence,"after.png").writeBytes(after)
        File(evidence,"reopened.png").writeBytes(restored)
        File(evidence,"before.json").writeText(DesignJsonCodec.encode(original))
        File(evidence,"after.json").writeText(DesignJsonCodec.encode(edited))
        File(evidence,"evidence.txt").writeText("Synthetic production-renderer evidence. Not Northstar acceptance or physical-pen testing.\n")
    }
}
