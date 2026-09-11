package com.example

import com.example.model.design.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SmoothPoolDrawingTest {
    private fun points(q:Double=.36)= (0 until 8).map { val a=it*2*PI/8;DesignPoint(4*(cos(a)+q*cos(2*a)),2.5*sin(a)) }
    private fun pool()=SmoothPoolDraft("d",0,"p",points()).finish()
    @Test fun guidePointsBecomeTangentArcsWithFollowingCoping() {
        val p=pool();val b=p.boundary
        assertEquals(16,b.nodes.size);assertEquals(points(),b.nodes.filterIndexed { i,_ -> i%2==0 }.map { it.point })
        assertTrue(b.nodes.any { it.bulge<0 });assertNotNull(p.copingFootprint)
        TangentProofCases.smooth(b)
    }
    @Test fun convexAndConcaveDirectionsAndTranslatedInputsWork() {
        for(q in listOf(0.0,.2,.36,.45)) {
            val p=points(q);val a=SmoothPoolDrawing.boundary(p,"a")
            val b=SmoothPoolDrawing.boundary(p.reversed(),"b")
            val c=SmoothPoolDrawing.boundary(p.map { it.translated(100.0,-200.0) },"c")
            TangentProofCases.smooth(a);TangentProofCases.smooth(b)
            assertEquals(a.signedAreaSquareMetres,-b.signedAreaSquareMetres,1e-8)
            assertEquals(a.perimeterMetres,c.perimeterMetres,1e-8)
        }
    }
    @Test fun draftBackAndInvalidCompletionDoNotMutatePoints() {
        val start=SmoothPoolDraft("d",0,"x");val one=start.append(DesignPoint(0.0,0.0))
        assertTrue(start.points.isEmpty());assertTrue(one.back().points.isEmpty())
        assertThrows(IllegalArgumentException::class.java) { one.finish() }
        assertThrows(IllegalArgumentException::class.java) { one.append(DesignPoint(.001,0.0)) }
        assertThrows(IllegalArgumentException::class.java) { SmoothPoolDrawing.boundary(listOf(DesignPoint(0.0,0.0),DesignPoint(2.0,0.0),DesignPoint(1.0,0.0)),"u") }
        assertEquals(1,one.points.size)
    }
    @Test fun localPenCommandRetainsTwelveEdgesAndUndo() {
        val p=pool();val d=ProjectDesign("d",listOf(p));val b=p.boundary
        val hit=DesignHit.SmoothAnchor(p.id,b.nodes[0].vertexId)
        val cmd=DesignPicking.drag(d,hit,b.nodes[0].point,b.nodes[0].point.translated(.15,.12))
        val session=DesignSession(d);val next=session.preview(cmd)
        assertEquals(d,session.document);assertEquals(next,session.execute(cmd))
        TangentProofCases.smooth(next.objectById(p.id).boundary)
        assertEquals(12,b.edges().zip(next.objectById(p.id).boundary.edges()).count { (a,c)->TangentProofCases.sameEdge(a,c) })
        assertEquals(d.objects,session.undo().objects);assertFalse(session.canUndo)
    }
    @Test fun radiusPenCommandChangesOnlyTwoArcsAndReportsActualRadius() {
        val p=pool();val d=ProjectDesign("d",listOf(p));val e=p.boundary.edges()[12]
        val hit=DesignHit.SmoothRadius(p.id,e.id);val down=e.pointAt(.5)
        val cmd=DesignPicking.drag(d,hit,down,down.translated(.2,.1)) as DesignCommand.SetTangentRadius
        val next=DesignCommands.apply(d,cmd);val edited=next.objectById(p.id)
        assertEquals(cmd.metres,edited.boundary.edges()[12].radiusMetres!!,1e-7)
        assertEquals(14,p.boundary.edges().zip(edited.boundary.edges()).count { (a,c)->TangentProofCases.sameEdge(a,c) })
        val live=LiveMeasurements.editing(d,next,hit,down)
        assertTrue(live.plain);assertEquals("R "+LiveFeetInches.format(cmd.metres),live.lines.single())
    }
    @Test fun hitTestingAndLocksCannotUseAnInteriorOrUnrelatedObject() {
        val p=pool();val b=p.boundary
        val on=b.nodes[4].point
        assertEquals(DesignHit.SmoothAnchor(p.id,b.nodes[4].vertexId),SmoothPoolEditing.hit(p,SmoothEditMode.SHAPE,on,.1))
        assertNull(SmoothPoolEditing.hit(p,SmoothEditMode.SHAPE,DesignPoint(0.0,0.0),.01))
        assertFalse(SmoothPoolEditing.canEdit(p.copy(locked=true)))
        assertFalse(SmoothPoolEditing.canEdit(p.copy(kind=DesignObjectKind.PAVING,coping=null)))
        assertThrows(IllegalArgumentException::class.java) {
            DesignCommands.apply(ProjectDesign("d",listOf(p.copy(locked=true))),DesignCommand.MoveTangentAnchor(p.id,b.nodes[0].vertexId,on,b)) }
    }
    @Test fun invalidRadiusCannotChangeTheAcceptedDocumentOrRedo() {
        val p=pool();val s=DesignSession(ProjectDesign("d",listOf(p)))
        s.execute(DesignCommand.Translate(p.id,1.0,0.0));s.undo();val saved=s.document
        assertThrows(IllegalArgumentException::class.java) { s.execute(DesignCommand.SetTangentRadius(p.id,p.boundary.nodes[0].edgeId,-1.0,p.boundary)) }
        assertEquals(saved,s.document);assertTrue(s.canRedo)
    }
    @Test fun objectSnapIsAnExactInputToTheTangentSolve() {
        val p=pool();val target=p.boundary.nodes[0].point.translated(.15,.12)
        val reference=DesignObject("ref","Reference",DesignObjectKind.SITE_OUTLINE,DesignFixtures.rectangle().translated(target.x,target.y),locked=true)
        val index=GeometrySnapIndex(listOf(GeometrySnapSource(reference.id,reference.boundary)))
        val match=index.resolve(target.translated(.01,.01),.1)!!
        val next=DesignCommands.apply(ProjectDesign("d",listOf(p,reference)),DesignCommand.MoveTangentAnchor(p.id,p.boundary.nodes[0].vertexId,match.point,p.boundary))
        assertEquals(target,next.objectById(p.id).boundary.nodes[0].point)
        TangentProofCases.smooth(next.objectById(p.id).boundary);assertEquals(reference,next.objectById(reference.id))
    }
}
