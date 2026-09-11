package com.example

import com.example.model.design.*
import com.example.ui.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class StraightSideEditingTest {
    private fun boundary(vararg points: Pair<Double,Double>) = DesignBoundary(points.mapIndexed { i,p ->
        BoundaryNode("v$i",DesignPoint(p.first,p.second),"e$i") })
    private fun pool(b: DesignBoundary=DesignFixtures.rectangle()) =
        DesignObject("p","Pool",DesignObjectKind.POOL,b,coping=CopingSpec())
    private fun doc(p: DesignObject=pool()) = ProjectDesign("side",listOf(p,
        DesignObject("other","Other",DesignObjectKind.PAVING,DesignFixtures.rectangle().translated(-20.0,0.0),locked=true)))
    private fun move(d: ProjectDesign,edge:String="edge-1",offset:Double=-2.0) =
        DesignCommands.apply(d,DesignCommand.MoveSide("p",edge,offset))
    private fun assertPoint(a:DesignPoint,b:DesignPoint,tol:Double=1e-8) { assertEquals(a.x,b.x,tol);assertEquals(a.y,b.y,tol) }

    @Test fun `one side moves both endpoints with opposite side and object identity fixed`() {
        val d=doc();val next=move(d);val a=d.objectById("p");val b=next.objectById("p")
        assertPoint(DesignPoint(12.0,0.0),b.boundary.nodes[1].point)
        assertPoint(DesignPoint(12.0,6.0),b.boundary.nodes[2].point)
        assertEquals(a.boundary.nodes[0],b.boundary.nodes[0]);assertEquals(a.boundary.nodes[3],b.boundary.nodes[3])
        assertEquals(a.boundary.nodes.map { it.edgeId },b.boundary.nodes.map { it.edgeId })
        assertEquals(a.boundary.nodes.map { it.vertexId },b.boundary.nodes.map { it.vertexId })
        assertEquals(a.coping,b.coping);assertEquals(d.objectById("other"),next.objectById("other"))
        assertNotEquals(a.copingFootprint!!.outerBoundary,b.copingFootprint!!.outerBoundary)
    }
    @Test fun `all rectangle sides move independently including the wraparound edge`() {
        val b=pool().boundary
        b.edges().forEachIndexed { i,e ->
            val after=move(doc(),e.id,0.4).objectById("p").boundary
            val normal=StraightSideEditing.frame(b,e.id).normal
            b.nodes.indices.forEach { k ->
                assertPoint(if(k==i || k==(i+1)%b.nodes.size) b.nodes[k].point.translated(normal.x*0.4,normal.y*0.4)
                    else b.nodes[k].point,after.nodes[k].point)
            }
            assertTrue(DesignDimensions.measure(after).rectangular)
        }
    }
    @Test fun `rotated rectangle preserves its axes and never quantizes endpoints independently`() {
        val t=0.63
        fun rotate(p:DesignPoint)=DesignPoint(p.x*cos(t)-p.y*sin(t),p.x*sin(t)+p.y*cos(t))
        val b=DesignBoundary(DesignFixtures.rectangle().nodes.map { it.copy(point=rotate(it.point)) })
        val changed=move(doc(pool(b))).objectById("p").boundary
        assertTrue(DesignDimensions.measure(changed).rectangular)
        assertEquals(12.0,DesignDimensions.measure(changed).lengthMetres,1e-8)
        assertEquals(6.0,DesignDimensions.measure(changed).widthMetres,1e-8)
        assertPoint(rotate(DesignPoint(12.0,6.0)),changed.nodes[2].point)
    }
    @Test fun `oblique neighbours retain supporting lines and the handle follows the actual midpoint`() {
        val b=boundary(0.0 to 0.0,10.0 to 0.0,8.0 to 6.0,0.0 to 6.0)
        val next=move(doc(pool(b)),"e2",-1.0).objectById("p").boundary
        assertPoint(DesignPoint(10.0-7.0/3.0,7.0),next.nodes[2].point)
        assertPoint(DesignPoint(0.0,7.0),next.nodes[3].point)
        assertPoint(StraightSideEditing.frame(b,"e2").target(-1.0),next.edges()[2].pointAt(0.5))
        assertEquals(b.nodes.take(2),next.nodes.take(2))
    }
    @Test fun `concave inside side moves without flattening an unrelated arc`() {
        val straight=boundary(0.0 to 0.0,10.0 to 0.0,10.0 to 6.0,6.0 to 6.0,6.0 to 3.0,0.0 to 3.0)
        val b=straight.changedBulge("e1",0.3)
        val next=move(doc(pool(b)),"e3",1.0).objectById("p").boundary
        assertPoint(DesignPoint(7.0,6.0),next.nodes[3].point)
        assertPoint(DesignPoint(7.0,3.0),next.nodes[4].point)
        assertEquals(b.nodes[1],next.nodes[1]);assertEquals(b.nodes[2],next.nodes[2])
        assertEquals(b.edges()[1].radiusMetres!!,next.edges()[1].radiusMetres!!,0.0)
    }
    @Test fun `reversed winding changes offset sign not the physical result`() {
        val b=DesignFixtures.rectangle();val reversed=b.reversed()
        val a=move(doc(pool(b)),"edge-1",-2.0).objectById("p").boundary
        val c=move(doc(pool(reversed)),"edge-1",2.0).objectById("p").boundary
        a.nodes.forEach { node -> assertPoint(node.point,c.nodes.single { it.vertexId==node.vertexId }.point) }
    }
    @Test fun `translated coordinate origin does not change the edit`() {
        val b=DesignFixtures.rectangle();val shift=1e8
        val a=StraightSideEditing.move(b,"edge-1",-0.125)
        val c=StraightSideEditing.move(b.translated(shift,-shift),"edge-1",-0.125)
        a.nodes.zip(c.nodes).forEach { (p,q) -> assertPoint(p.point.translated(shift,-shift),q.point,1e-7) }
    }
    @Test fun `tangential pen travel does not move the side and zero edit preserves redo`() {
        val d=doc();val e=d.objectById("p").boundary.edges()[1];val at=e.pointAt(0.5)
        val command=DesignPicking.drag(d,DesignHit.Side("p",e.id),at,at.translated(0.0,4.0)) as DesignCommand.MoveSide
        assertEquals(0.0,command.offsetMetres,0.0)
        val session=DesignSession(d);session.execute(command.copy(offsetMetres=-1.0));session.undo()
        val old=session.document;session.execute(command)
        assertEquals(old,session.document);assertFalse(session.canUndo);assertTrue(session.canRedo)
    }
    @Test fun `preview does not save and one undo restores pool coping and unrelated objects`() {
        val initial=doc();val session=DesignSession(initial);val cmd=DesignCommand.MoveSide("p","edge-1",-1.0)
        val ghost=session.preview(cmd);assertEquals(initial,session.document);assertFalse(session.canUndo)
        val next=session.execute(cmd);assertEquals(ghost,next);assertEquals(1L,next.revision)
        assertEquals(initial.objects,session.undo().objects);assertEquals(next.objects,session.redo().objects)
    }
    @Test fun `collapsed reversed and nonfinite movement fails before consuming history`() {
        val session=DesignSession(doc());session.execute(DesignCommand.MoveSide("p","edge-1",-1.0));session.undo()
        val before=session.document
        for(offset in listOf(10.0,12.0,Double.NaN,Double.POSITIVE_INFINITY,1001.0)) {
            assertThrows(IllegalArgumentException::class.java) { session.execute(DesignCommand.MoveSide("p","edge-1",offset)) }
            assertEquals(before,session.document);assertTrue(session.canRedo)
        }
    }
    @Test fun `crossing another boundary is rejected even if the three local edges did not reverse`() {
        val b=boundary(0.0 to 0.0,10.0 to 0.0,10.0 to 8.0,6.0 to 8.0,6.0 to 2.0,4.0 to 2.0,4.0 to 8.0,0.0 to 8.0)
        val session=DesignSession(doc(pool(b)));val before=session.document
        assertThrows(IllegalArgumentException::class.java) { session.execute(DesignCommand.MoveSide("p","e4",3.0)) }
        assertEquals(before,session.document);assertFalse(session.canUndo)
    }
    @Test fun `arc neighbours and collinear joins are not silently deformed`() {
        val organic=pool(DesignFixtures.organic())
        assertFalse(StraightSideEditing.supports(organic,"edge-0"))
        assertThrows(IllegalArgumentException::class.java) { move(doc(organic),"edge-0",1.0) }
        val b=boundary(0.0 to 0.0,5.0 to 0.0,10.0 to 0.0,10.0 to 6.0,0.0 to 6.0)
        assertFalse(StraightSideEditing.supports(pool(b),"e0"))
        assertThrows(IllegalArgumentException::class.java) { StraightSideEditing.move(b,"e0",1.0) }
    }
    @Test fun `locks legacy outlines and nonpool objects cannot enter this operation`() {
        val a=pool()
        for(p in listOf(a.copy(locked=true),a.copy(coping=null),a.copy(kind=DesignObjectKind.PAVING,coping=null))) {
            assertFalse(StraightSideEditing.canEdit(p))
            assertThrows(IllegalArgumentException::class.java) { move(doc(p)) }
        }
        assertThrows(IllegalArgumentException::class.java) { move(doc(),"unknown") }
    }
    @Test fun `new picking mode does not steal the existing curve or body interactions`() {
        val d=doc();val midpoint=DesignPoint(10.0,3.0)
        assertEquals(DesignHit.Curve("p","edge-1"),DesignPicking.hit(d,"p",midpoint,0.1))
        assertEquals(DesignHit.Side("p","edge-1"),DesignPicking.hit(d,"p",midpoint,0.1,true))
        assertNull(DesignPicking.hit(d,"p",DesignPoint(0.0,0.0),0.1,true))
        assertNull(DesignPicking.hit(doc(pool().copy(locked=true)),"p",midpoint,0.1,true))
    }
    @Test fun `whole side snapping uses offgrid references without altering protected geometry`() {
        val p=pool();val house=boundary(11.5 to 3.0,14.0 to 3.0,14.0 to 5.0,11.5 to 5.0)
        val index=GeometrySnapIndex(listOf(GeometrySnapSource("house",house),GeometrySnapSource("p",p.boundary)))
        val target=StraightSideTargets.resolve(p,DesignCommand.MoveSide("p","edge-1",-1.47),true,index,0.1)
        assertEquals(-1.5,target.command.offsetMetres,1e-10);assertEquals("house",target.snap!!.key.objectId)
        val changed=move(doc(),"edge-1",target.command.offsetMetres).objectById("p")
        assertPoint(target.point,changed.boundary.edges()[1].pointAt(0.5))
        assertEquals(house.nodes.first().point.x,changed.boundary.nodes[1].point.x,1e-10)
    }
    @Test fun `perpendicular grid increments retain an arbitrary rotated side direction`() {
        val angle=0.37
        val rotated=DesignBoundary(DesignFixtures.rectangle().nodes.map { n -> n.copy(point=DesignPoint(
            n.point.x*cos(angle)-n.point.y*sin(angle)+0.23,
            n.point.x*sin(angle)+n.point.y*cos(angle)-0.17)) })
        val p=pool(rotated);val target=StraightSideTargets.resolve(p,DesignCommand.MoveSide("p","edge-1",-1.49),true,null,0.1)
        assertEquals(-5*DesignDimensions.FOOT,target.command.offsetMetres,1e-10)
        val e=move(doc(p),"edge-1",target.command.offsetMetres).objectById("p").boundary.edges()[1]
        assertPoint(target.point,e.pointAt(0.5));assertEquals(6.0,e.lengthMetres,1e-9)
    }
    @Test fun `rectangle side feedback reports the dimensions that change not merely side travel`() {
        val d=doc();val next=move(d)
        val reading=LiveMeasurements.editing(d,next,DesignHit.Side("p","edge-1"),DesignPoint(10.0,3.0))
        assertEquals(listOf(LiveFeetInches.format(12.0)+" × "+LiveFeetInches.format(6.0)),reading.lines)
        assertPoint(DesignPoint(12.0,3.0),reading.target)
    }
    @Test fun `side command preserves existing radial directions and is disabled for unsupported selections`() {
        val angles=RadialCommands.actions(RadialCategory.EDIT).indices.map { RadialGeometry.childAngle(RadialCategory.EDIT,it) }
        assertEquals(listOf(-60.0,-30.0,0.0,30.0),angles)
        val disabled=RadialAvailability(true,true,true,true,false,false,true)
        assertFalse(disabled.enabled(RadialAction.SIDES))
        assertTrue(disabled.copy(canEditSides=true).enabled(RadialAction.SIDES))
        assertFalse(disabled.copy(editable=false,canEditSides=true).enabled(RadialAction.SIDES))
        assertEquals(true,disabled.copy(canEditSides=true,sideEditing=true).checked(RadialAction.SIDES))
    }
}
