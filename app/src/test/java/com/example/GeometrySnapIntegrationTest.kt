package com.example

import com.example.model.design.*
import org.junit.Assert.*
import org.junit.Test

class GeometrySnapIntegrationTest {
    private fun targetObject()=DesignObject("house","House",DesignObjectKind.SITE_OUTLINE,
        DesignFixtures.rectangle().translated(1.111,2.222),locked=true)
    @Test fun `object snap overrides the grid while preserving exact reference coordinates`() {
        val house=targetObject();val index=GeometrySnapIndex(listOf(GeometrySnapSource(house.id,house.boundary)))
        val point=house.boundary.nodes[0].point
        val target=CornerTarget.resolve(emptyList(),point.translated(0.02,0.01),false,true,
            geometry=index,snapToleranceMetres=0.1)
        assertEquals(point,target.point);assertNotNull(target.snap)
        assertNotEquals(GridAssist.snapPoint(point),target.point)
    }
    @Test fun `disabled object attraction retains existing grid resolution exactly`() {
        val point=DesignPoint(1.111,2.222)
        val target=CornerTarget.resolve(emptyList(),point,false,true)
        assertNull(target.snap);assertEquals(GridAssist.snapPoint(point),target.point)
    }
    @Test fun `closing has priority and the live measure uses exactly the eventual endpoint`() {
        val points=listOf(DesignPoint(0.0,0.0),DesignPoint(3.0,0.0),DesignPoint(3.0,4.0))
        val house=targetObject();val i=GeometrySnapIndex(listOf(GeometrySnapSource(house.id,house.boundary)))
        val target=CornerTarget.resolve(points,DesignPoint(0.01,0.02),true,true,0.1,i,0.1)
        assertTrue(target.closing);assertNull(target.snap)
        assertEquals(points[0],target.point)
        assertEquals("Close · "+LiveFeetInches.format(5.0),LiveMeasurements.segment(points,target)!!.lines.first())
    }
    @Test fun `right-angle constraint and live endpoint agree after snapping to a finite edge`() {
        val edge=targetObject();val i=GeometrySnapIndex(listOf(GeometrySnapSource(edge.id,edge.boundary)))
        val points=listOf(DesignPoint(0.0,0.0),DesignPoint(0.0,3.0))
        val target=CornerTarget.resolve(points,DesignPoint(1.13,3.03),true,false,0.0,i,0.1)
        assertEquals(1.111,target.point.x,1e-9);assertEquals(3.0,target.point.y,1e-9)
        assertEquals(target.point,LiveMeasurements.segment(points,target)!!.target)
        assertTrue(LiveMeasurements.segment(points,target)!!.lines.first().contains(LiveFeetInches.format(1.111)))
    }
    @Test fun `snap references cannot unlock or mutate a protected object and edit undo retains it`() {
        val house=targetObject()
        val draft=DesignObject("deck","Deck",DesignObjectKind.PAVING,DesignFixtures.rectangle().translated(20.0,0.0))
        val session=DesignSession(ProjectDesign("s",listOf(house,draft)))
        val i=GeometrySnapIndex(session.document.objects.map { GeometrySnapSource(it.id,it.boundary) })
        val reference=house.boundary.nodes[0].point
        val target=i.resolve(reference.translated(0.02,0.01),0.1,excludeObjectId="deck")!!
        val command=DesignCommand.MoveVertex(draft.id,draft.boundary.nodes[0].vertexId,target.point)
        val before=session.document
        val preview=session.preview(command)
        assertEquals(before,session.document);assertEquals(house,preview.objectById("house"))
        session.execute(command);assertEquals(house,session.document.objectById("house"))
        assertEquals(before.objects,session.undo().objects)
        assertThrows(IllegalArgumentException::class.java) {
            session.execute(DesignCommand.Translate(house.id,1.0,0.0))
        }
    }
}
