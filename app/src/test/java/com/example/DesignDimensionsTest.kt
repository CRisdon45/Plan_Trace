package com.example

import com.example.model.design.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class DesignDimensionsTest {
    @Test fun `rectangle length and width preserve first corner opposite axis and stable edge IDs`() {
        val b=DesignFixtures.rectangle();val resized=DesignDimensions.resize(b,SizeAxis.LENGTH,14.0)
        assertEquals(b.nodes[0],resized.nodes[0]);assertEquals(b.nodes[3],resized.nodes[3])
        assertEquals(14.0,DesignDimensions.measure(resized).lengthMetres,1e-9)
        assertEquals(6.0,DesignDimensions.measure(resized).widthMetres,1e-9)
        assertEquals(b.nodes.map { it.edgeId },resized.nodes.map { it.edgeId })
    }
    @Test fun `rotated rectangle dimensions follow its own axes rather than the screen bounds`() {
        val b=DesignBoundary(DesignFixtures.rectangle().nodes.map { it.copy(point=DesignPoint(
            it.point.x*cos(0.7)-it.point.y*sin(0.7),it.point.x*sin(0.7)+it.point.y*cos(0.7))) })
        val r=DesignDimensions.resize(b,SizeAxis.WIDTH,8.0)
        assertTrue(DesignDimensions.measure(r).rectangular)
        assertEquals(10.0,DesignDimensions.measure(r).lengthMetres,1e-8)
        assertEquals(8.0,DesignDimensions.measure(r).widthMetres,1e-8)
    }
    @Test fun `exact spans include circular extrema not just stored endpoints`() {
        val s=DesignDimensions.measure(DesignFixtures.circle(3.0))
        assertFalse(s.rectangular);assertEquals(6.0,s.lengthMetres,1e-9);assertEquals(6.0,s.widthMetres,1e-9)
        val reversed=DesignDimensions.measure(DesignFixtures.circle(3.0).reversed())
        assertEquals(s.lengthMetres,reversed.lengthMetres,1e-9);assertEquals(s.widthMetres,reversed.widthMetres,1e-9)
    }
    @Test fun `freeform size uniformly scales exact curves and keeps the anchor and bulges`() {
        val b=DesignFixtures.organic();val old=DesignDimensions.measure(b)
        val r=DesignDimensions.resize(b,SizeAxis.LENGTH,old.lengthMetres*1.4)
        val size=DesignDimensions.measure(r)
        assertEquals(old.lengthMetres*1.4,size.lengthMetres,1e-8)
        assertEquals(old.widthMetres*1.4,size.widthMetres,1e-8)
        assertEquals(b.nodes.first(),r.nodes.first())
        assertEquals(b.nodes.map { it.bulge },r.nodes.map { it.bulge })
        assertEquals(b.perimeterMetres*1.4,r.perimeterMetres,1e-8)
    }
    @Test fun `size command updates following coping once and preserves unrelated objects and width`() {
        val pool=DesignObject("p","Pool",DesignObjectKind.POOL,DesignFixtures.rectangle(),coping=CopingSpec())
        val patio=pool.copy(id="other",coping=null,kind=DesignObjectKind.PAVING)
        val session=DesignSession(ProjectDesign("s",listOf(pool,patio)))
        val next=session.execute(DesignCommand.SetDimension("p",SizeAxis.LENGTH,12.0))
        assertEquals(1L,next.revision);assertEquals(patio,next.objectById("other"))
        assertEquals(pool.coping,next.objectById("p").coping)
        assertNotEquals(pool.copingFootprint!!.outerBoundary,next.objectById("p").copingFootprint!!.outerBoundary)
        assertEquals(listOf(pool,patio),session.undo().objects)
        assertEquals(next.objects,session.redo().objects)
    }
    @Test fun `locked invalid and no-op exact edits keep history and geometry truthful`() {
        val b=DesignFixtures.rectangle();val obj=DesignObject("p","P",DesignObjectKind.POOL,b,locked=true)
        val session=DesignSession(ProjectDesign("s",listOf(obj)))
        assertThrows(IllegalArgumentException::class.java) { session.execute(DesignCommand.SetDimension("p",SizeAxis.LENGTH,14.0)) }
        assertFalse(session.canUndo)
        assertEquals(b,DesignDimensions.resize(b,SizeAxis.LENGTH,10.0))
        listOf(-1.0,0.0,Double.NaN,Double.POSITIVE_INFINITY).forEach { v ->
            assertThrows(IllegalArgumentException::class.java) { DesignDimensions.resize(b,SizeAxis.WIDTH,v) }
        }
    }
    @Test fun `feet inches and fractions parse with explicit unit meanings`() {
        mapOf("30" to 30.0,"30.5" to 30.5,"30' 6\"" to 30.5,"30'-6\"" to 30.5,
            "30 ft 6 in" to 30.5,"30' 6 1/2\"" to (30+6.5/12),"16\"" to (16.0/12),
            "30′ 6″" to 30.5,"1/2" to 0.5).forEach { (text,feet) ->
            assertEquals(text,feet*DesignDimensions.FOOT,FeetInchesInput.parseMetres(text),1e-10)
        }
    }
    @Test fun `ambiguous malformed or out of range dimensions are not silently corrected`() {
        listOf("","-3","NaN","1e6","30' 6","30' 13\"","2/0","two","1001","0","30''").forEach { t ->
            assertThrows(t,IllegalArgumentException::class.java) { FeetInchesInput.parseMetres(t) }
        }
    }
    @Test fun `grid snapping modifies only vertices and movement deltas not exact dimensions or arcs`() {
        val foot=DesignDimensions.FOOT
        val vertex=DesignCommand.MoveVertex("p","v",DesignPoint(foot*1.1,-foot*1.7))
        assertEquals(DesignPoint(foot,-2*foot),(GridAssist.snap(vertex) as DesignCommand.MoveVertex).point)
        val move=GridAssist.snap(DesignCommand.Translate("p",foot*2.1,-foot*2.7)) as DesignCommand.Translate
        assertEquals(2*foot,move.dxMetres,1e-10);assertEquals(-3*foot,move.dyMetres,1e-10)
        val size=DesignCommand.SetDimension("p",SizeAxis.LENGTH,9.15);assertEquals(size,GridAssist.snap(size))
        val arc=DesignCommand.ChangeBulge("p","e",0.321);assertEquals(arc,GridAssist.snap(arc))
    }
}
