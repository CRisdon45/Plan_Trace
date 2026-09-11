package com.example

import com.example.model.design.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class LiveDrawingTest {
    private val zero=DesignPoint(0.0,0.0)
    @Test fun `feet inches fractions and carry are display-only`() {
        assertEquals("0′ 0″",LiveFeetInches.format(0.0))
        assertEquals("12′ 4 1/2″",LiveFeetInches.format((12*12+4.5)*0.0254))
        assertEquals("≈ 13′ 0″",LiveFeetInches.format((12*12+11.99)*0.0254))
        assertEquals("0′ 1/8″",LiveFeetInches.format(0.0254/8))
        assertTrue(LiveFeetInches.format(0.001).startsWith("≈ "))
        assertTrue(LiveFeetInches.format(1e12).contains("ft"))
    }
    @Test fun `bad display numbers never pretend to be valid lengths`() {
        listOf(-1.0,Double.NaN,Double.POSITIVE_INFINITY).forEach { n ->
            assertThrows(IllegalArgumentException::class.java) { LiveFeetInches.format(n) }
        }
    }
    @Test fun `resolved snapped target drives both guide and measured line`() {
        val raw=DesignPoint(1.1*0.3048,2.2*0.3048)
        val target=CornerTarget.resolve(listOf(zero),raw,false,true)
        val live=LiveMeasurements.segment(listOf(zero),target)!!
        assertEquals(GridAssist.snapPoint(raw),target.point)
        assertEquals(target.point,live.target)
        assertTrue(live.lines.first().contains(LiveFeetInches.format(zero.distanceTo(target.point))))
        assertNotEquals(zero.distanceTo(raw),zero.distanceTo(target.point),1e-6)
    }
    @Test fun `orthogonal reading follows rotated first edge not raw pointer diagonal`() {
        val points=listOf(zero,DesignPoint(2.0,2.0))
        val target=CornerTarget.resolve(points,DesignPoint(2.0,5.0),true,false)
        val delta=target.point.translated(-2.0,-2.0)
        assertEquals(abs(delta.x),abs(delta.y),1e-10)
        assertEquals("Right angle",target.assistance)
        assertTrue(LiveMeasurements.segment(points,target)!!.lines.first().contains(LiveFeetInches.format(hypot(delta.x,delta.y))))
    }
    @Test fun `closing uses exact first vertex for preview and committed final edge`() {
        val points=listOf(zero,DesignPoint(4.0,0.0),DesignPoint(4.0,3.0))
        val target=CornerTarget.resolve(points,DesignPoint(0.05,0.02),true,true,0.1)
        assertTrue(target.closing);assertEquals(zero,target.point)
        assertEquals("Close · "+LiveFeetInches.format(5.0),LiveMeasurements.segment(points,target)!!.lines.first())
        assertNull(LiveMeasurements.segment(emptyList(),target))
    }
    @Test fun `pool is hand placed in project coordinates with following coping and one undo`() {
        val original=DesignFixtures.document()
        var draft=ProposedOutlineDraft(DesignObjectKind.POOL,original.id,original.revision)
        listOf(DesignPoint(20.0,10.0),DesignPoint(26.0,10.0),DesignPoint(26.0,14.0),DesignPoint(20.0,14.0)).forEach { draft=draft.append(it) }
        val pool=draft.finish();assertNotNull(pool.copingFootprint);assertFalse(pool.locked)
        assertEquals(draft.points,pool.boundary.nodes.map { it.point })
        val session=DesignSession(original);session.execute(DesignCommand.Add(pool))
        assertEquals(original.objects,session.document.objects.dropLast(1))
        assertEquals(original.objects,session.undo().objects)
        assertEquals(pool,session.redo().objects.last())
    }
    @Test fun `concave deck closes without inventing pool coping or trace provenance`() {
        var draft=ProposedOutlineDraft(DesignObjectKind.PAVING,"deck",0)
        listOf(zero,DesignPoint(8.0,0.0),DesignPoint(8.0,2.0),DesignPoint(3.0,2.0),DesignPoint(3.0,6.0),DesignPoint(0.0,6.0)).forEach { draft=draft.append(it) }
        val deck=draft.finish();assertNull(deck.coping);assertNull(deck.siteTrace)
        assertEquals(GeometryConfidence.DESIGNED,deck.confidence)
        assertEquals(6,deck.boundary.nodes.size)
    }
    @Test fun `invalid closure preserves recoverable draft and its stable identity`() {
        var draft=ProposedOutlineDraft(DesignObjectKind.POOL,"bad",0)
        listOf(zero,DesignPoint(4.0,4.0),DesignPoint(0.0,4.0),DesignPoint(4.0,0.0)).forEach { draft=draft.append(it) }
        assertThrows(IllegalArgumentException::class.java) { draft.finish() }
        assertEquals(4,draft.points.size);assertEquals(draft.objectId,draft.back().objectId)
        assertThrows(IllegalArgumentException::class.java) { draft.append(draft.points.last()) }
    }
    @Test fun `vertex feedback reads actual two incident edges of preview`() {
        val before=DesignFixtures.document()
        val hit=DesignHit.Vertex("pool","vertex-2")
        val after=DesignCommands.apply(before,DesignCommand.MoveVertex("pool","vertex-2",DesignPoint(11.0,7.0)))
        val live=LiveMeasurements.editing(before,after,hit,zero)
        val edges=after.objectById("pool").boundary.edges()
        assertEquals(DesignPoint(11.0,7.0),live.target)
        assertTrue(live.lines[0].contains(LiveFeetInches.format(edges[1].lengthMetres)))
        assertTrue(live.lines[1].contains(LiveFeetInches.format(edges[2].lengthMetres)))
    }
    @Test fun `curve readout is analytic arc length rather than the straight chord`() {
        val doc=DesignFixtures.document();val edge=doc.objectById("pool").boundary.edges()[1]
        val live=LiveMeasurements.editing(doc,doc,DesignHit.Curve("pool",edge.id),zero)
        assertTrue(live.lines[0].startsWith("Arc · "))
        assertTrue(live.lines[0].contains(LiveFeetInches.format(edge.lengthMetres)))
        assertNotEquals(edge.chordMetres,edge.lengthMetres,1e-6)
        assertEquals(edge.pointAt(0.5),live.target)
    }
    @Test fun `move feedback is actual displacement after snapping not an edge length`() {
        val before=DesignFixtures.document()
        val after=DesignCommands.apply(before,DesignCommand.Translate("pool",3.0,4.0))
        val live=LiveMeasurements.editing(before,after,DesignHit.Body("pool"),DesignPoint(2.0,2.0))
        assertEquals(DesignPoint(5.0,6.0),live.target)
        assertEquals(listOf("Move · "+LiveFeetInches.format(5.0)),live.lines)
    }
}
