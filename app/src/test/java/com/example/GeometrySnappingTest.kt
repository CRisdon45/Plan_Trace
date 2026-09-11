package com.example

import com.example.model.design.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class GeometrySnappingTest {
    private fun rectangle(x:Double=0.0,y:Double=0.0) = DesignBoundary(listOf(
        DesignPoint(x,y),DesignPoint(x+4,y),DesignPoint(x+4,y+4),DesignPoint(x,y+4)
    ).mapIndexed { i,p -> BoundaryNode("v$i",p,"e$i") })
    private fun index() = GeometrySnapIndex(listOf(GeometrySnapSource("house",rectangle())))

    @Test fun `corners midpoints and finite edges have explicit priorities`() {
        val i=index()
        val corner=i.resolve(DesignPoint(0.01,0.01),0.1)!!
        assertEquals(GeometrySnapKind.CORNER,corner.key.kind); assertEquals(DesignPoint(0.0,0.0),corner.point)
        val middle=i.resolve(DesignPoint(2.0,0.01),0.1)!!
        assertEquals(GeometrySnapKind.MIDPOINT,middle.key.kind); assertEquals(DesignPoint(2.0,0.0),middle.point)
        val edge=i.resolve(DesignPoint(1.2,0.01),0.1)!!
        assertEquals(GeometrySnapKind.EDGE,edge.key.kind); assertEquals(DesignPoint(1.2,0.0),edge.point)
    }
    @Test fun `corner alignment is projected outside the finite reference edge without inventing an endpoint`() {
        val match=index().resolve(DesignPoint(-2.0,0.03),0.1)!!
        assertEquals(GeometrySnapKind.ALIGN_FIRST,match.key.kind)
        assertEquals(DesignPoint(-2.0,0.0),match.point)
        assertEquals(DesignPoint(0.0,0.0),match.reference)
    }
    @Test fun `attraction radius follows the zoom-dependent caller tolerance`() {
        val query=DesignPoint(0.06,0.06)
        assertNotNull(index().resolve(query,0.1))
        assertNull(index().resolve(query,0.02))
    }
    @Test fun `hysteresis holds a corner until release instead of alternating nearby references`() {
        val i=GeometrySnapIndex(listOf(GeometrySnapSource("a",rectangle()),GeometrySnapSource("b",rectangle(0.1,0.0))))
        val first=i.resolve(DesignPoint(0.04,0.0),0.08)!!
        assertEquals("a",first.key.objectId)
        assertEquals(first.key,i.resolve(DesignPoint(0.06,0.0),0.08,first.key)!!.key)
        assertEquals("b",i.resolve(DesignPoint(0.17,0.0),0.08,first.key)!!.key.objectId)
    }
    @Test fun `corner can replace captured edge and disabled or missing reference is not retained`() {
        val i=index();val edge=i.resolve(DesignPoint(1.0,0.01),0.2)!!
        assertEquals(GeometrySnapKind.EDGE,edge.key.kind)
        assertEquals(GeometrySnapKind.CORNER,i.resolve(DesignPoint(0.1,0.01),0.2,edge.key)!!.key.kind)
        assertNull(i.resolve(DesignPoint(1.0,0.01),0.2,edge.key,excludeObjectId="house"))
    }
    @Test fun `document ordering cannot choose a different tied reference`() {
        val a=GeometrySnapSource("a",rectangle());val b=GeometrySnapSource("b",rectangle())
        val query=DesignPoint(0.03,0.04)
        assertEquals(GeometrySnapIndex(listOf(a,b)).resolve(query,0.1),GeometrySnapIndex(listOf(b,a)).resolve(query,0.1))
    }
    @Test fun `active right angle is never broken to reach a nearby incompatible corner`() {
        val axis=SnapAxis(DesignPoint(0.0,0.02),DesignPoint(1.0,0.0))
        val match=index().resolve(DesignPoint(3.99,0.02),0.1,axis=axis)!!
        assertEquals(GeometrySnapKind.EDGE,match.key.kind)
        assertEquals(DesignPoint(4.0,0.02),match.point)
        assertTrue(axis.contains(match.point))
    }
    @Test fun `straight edge intersections respect rotated construction axes`() {
        val root=sqrt(0.5);val axis=SnapAxis(DesignPoint(0.0,1.0),DesignPoint(root,root))
        val match=index().resolve(DesignPoint(2.97,3.97),0.1,axis=axis,guideDirection=axis.direction)!!
        assertEquals(GeometrySnapKind.EDGE,match.key.kind)
        assertEquals(3.0,match.point.x,1e-9);assertEquals(4.0,match.point.y,1e-9)
        assertTrue(axis.contains(match.point))
    }
    @Test fun `nearest arc point uses the circle not its chord for either winding and shallow curves`() {
        for(bulge in listOf(-1.0,-0.4,-1e-8,1e-8,0.4,1.0)) {
            val edge=BoundaryEdge(BoundaryNode("v",DesignPoint(40.0,-20.0),"e",bulge),DesignPoint(46.0,-20.0))
            val point=edge.pointAt(0.27);val tangent=edge.tangentAt(0.27)
            val raw=point.translated(-tangent.y*0.03,tangent.x*0.03)
            val actual=index().closestOnEdge(edge,raw)
            assertEquals("bulge=$bulge",point.x,actual.x,1e-7)
            assertEquals("bulge=$bulge",point.y,actual.y,1e-7)
        }
    }
    @Test fun `finite arc projection clamps outside its sweep instead of snapping to a full circle`() {
        val edge=BoundaryEdge(BoundaryNode("v",DesignPoint(0.0,0.0),"e",1.0),DesignPoint(4.0,0.0))
        assertEquals(DesignPoint(0.0,0.0),index().closestOnEdge(edge,DesignPoint(-1.0,2.0)))
        assertEquals(DesignPoint(4.0,0.0),index().closestOnEdge(edge,DesignPoint(5.0,2.0)))
    }
    @Test fun `curved edge midpoint and unconstrained nearest edge remain canonical targets`() {
        val circle=DesignBoundary(listOf(BoundaryNode("a",DesignPoint(-2.0,0.0),"ab",1.0),
            BoundaryNode("b",DesignPoint(2.0,0.0),"ba",1.0)))
        val i=GeometrySnapIndex(listOf(GeometrySnapSource("pool",circle)))
        val midpoint=i.resolve(DesignPoint(0.0,-2.03),0.1)!!
        assertEquals(GeometrySnapKind.MIDPOINT,midpoint.key.kind)
        val target=circle.edges()[0].pointAt(0.3)
        val raw=target.translated(target.x*0.01,target.y*0.01)
        val near=i.resolve(raw,0.1)!!
        assertEquals(GeometrySnapKind.EDGE,near.key.kind)
        assertEquals(target.x,near.point.x,1e-9);assertEquals(target.y,near.point.y,1e-9)
    }
    @Test fun `oversized reference sets do not silently index an arbitrary subset`() {
        val ring=DesignBoundary((0 until 4096).map { n ->
            val a=n*2*PI/4096;BoundaryNode("v$n",DesignPoint(10*cos(a),10*sin(a)),"e$n")
        })
        val i=GeometrySnapIndex(listOf("a","b","c").map { GeometrySnapSource(it,ring) })
        assertFalse(i.supported);assertNull(i.resolve(DesignPoint(10.0,0.0),0.1))
    }
    @Test fun `invalid query controls fail explicitly`() {
        assertThrows(IllegalArgumentException::class.java) { index().resolve(DesignPoint(0.0,0.0),Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { index().resolve(DesignPoint(0.0,0.0),-1.0) }
        assertThrows(IllegalArgumentException::class.java) { SnapAxis(DesignPoint(0.0,0.0),DesignPoint(2.0,0.0)) }
    }
}
