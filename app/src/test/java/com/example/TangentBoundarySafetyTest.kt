package com.example

import com.example.model.design.*
import org.junit.Assert.*
import org.junit.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.operation.valid.IsValidOp

/** A smooth local solve is not itself a safe complete pool. No device or renderer required. */
class TangentBoundarySafetyTest {
    private fun document():ProjectDesign = ProjectDesign("tangent-safety",listOf(
        DesignObject("pool","Original synthetic pool",DesignObjectKind.POOL,
            TangentProofCases.fixture(.3),coping=CopingSpec())))

    @Test fun globallyCrossingButLocallySmoothCandidateIsRejectedWithoutLosingRedo() {
        val initial=document();val session=DesignSession(initial);val b=initial.objectById("pool").boundary
        session.execute(DesignCommand.MoveTangentAnchor("pool","v4",b.nodes[4].point.translated(.35,.4),b))
        session.undo();val before=session.document
        val target=b.nodes[0].point.translated(-6.0,-6.0)
        // The local branch accepts this candidate and its joins stay smooth. Separately
        // establish its global crossing using the same declared chord error as coping.
        val candidate=TangentSpanEditing.moveAnchor(b,"v0",target)
        TangentProofCases.smooth(candidate)
        val points=candidate.sample(PoolCoping.CHORD_ERROR_METRES).map { Coordinate(it.x,it.y) }
        val polygon=GeometryFactory().createPolygon((points+points.first().copy()).toTypedArray())
        assertFalse(IsValidOp(polygon).isValid)
        assertThrows(IllegalArgumentException::class.java) {
            session.execute(DesignCommand.MoveTangentAnchor("pool","v0",target,b)) }
        assertEquals(before,session.document);assertFalse(session.canUndo);assertTrue(session.canRedo)
    }

    @Test fun canonicalDimensionsAndQuantitiesFollowTheAcceptedBoundary() {
        val initial=document();val b=initial.objectById("pool").boundary
        val next=DesignCommands.apply(initial,DesignCommand.MoveTangentAnchor("pool","v4",b.nodes[4].point.translated(.35,.4),b))
        val changed=next.objectById("pool").boundary
        assertNotEquals(DesignDimensions.measure(b),DesignDimensions.measure(changed))
        assertNotEquals(b.perimeterMetres,changed.perimeterMetres,1e-8)
        assertNotEquals(b.signedAreaSquareMetres,changed.signedAreaSquareMetres,1e-8)
        assertEquals(b.nodes.map { it.vertexId to it.edgeId },changed.nodes.map { it.vertexId to it.edgeId })
        assertEquals(initial.objectById("pool").coping,next.objectById("pool").coping)
    }
}
