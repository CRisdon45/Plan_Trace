package com.example

import com.example.model.design.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class PoolCopingTest {
    private fun pool(boundary: DesignBoundary = DesignFixtures.rectangle(), spec: CopingSpec = CopingSpec()) =
        DesignObject("pool", "Pool", DesignObjectKind.POOL, boundary, coping = spec)
    private fun doc(obj: DesignObject = pool()) = ProjectDesign("coping", listOf(obj,
        DesignObject("patio", "Patio", DesignObjectKind.PAVING, DesignFixtures.rectangle().translated(0.0,-10.0))))
    private fun boundary(points: List<Pair<Double,Double>>) = DesignBoundary(points.mapIndexed { i, p ->
        BoundaryNode("v$i", DesignPoint(p.first,p.second), "e$i") })

    @Test fun `rectangle coping has the requested outward width and is not another object`() {
        val d = doc(); val p = d.objectById("pool")
        val ring = p.copingFootprint!!.outerBoundary; val w = p.coping!!.widthMetres
        assertEquals(-w, ring.minOf { it.x }, 1e-9); assertEquals(10+w, ring.maxOf { it.x }, 1e-9)
        assertEquals(-w, ring.minOf { it.y }, 1e-9); assertEquals(6+w, ring.maxOf { it.y }, 1e-9)
        assertEquals(2, d.objects.size)
        assertEquals(32.0,p.boundary.perimeterMetres,1e-9)
    }
    @Test fun `circle offset follows the radius within the explicit sampled approximation`() {
        val p = pool(DesignFixtures.circle(3.0)); val radius = 3.0 + p.coping!!.widthMetres
        p.copingFootprint!!.outerBoundary.forEach { assertEquals(radius,hypot(it.x,it.y),0.001) }
        assertEquals(6*PI,p.boundary.perimeterMetres,1e-9)
    }
    @Test fun `both winding directions generate outward coping`() {
        val a=pool(); val b=pool(a.boundary.reversed())
        assertEquals(a.copingFootprint!!.outerBoundary.toSet(), b.copingFootprint!!.outerBoundary.toSet())
    }
    @Test fun `concave and convex curves can have following coping without flattening the authority`() {
        val p=pool(DesignFixtures.organic())
        assertTrue(p.copingFootprint!!.outerBoundary.size>p.boundary.nodes.size)
        assertEquals(DesignFixtures.organic(),p.boundary)
        assertTrue(p.boundary.nodes.any { it.bulge<0 })
    }
    @Test fun `reshape regenerates coping and one undo restores the entire assembly`() {
        val initial=doc(); val session=DesignSession(initial)
        val preview=session.preview(DesignCommand.MoveVertex("pool","vertex-1",DesignPoint(11.0,0.0)))
        assertEquals(initial,session.document)
        assertNotEquals(initial.objectById("pool").copingFootprint!!.outerBoundary,preview.objectById("pool").copingFootprint!!.outerBoundary)
        val changed=session.execute(DesignCommand.MoveVertex("pool","vertex-1",DesignPoint(11.0,0.0)))
        assertEquals(initial.objectById("patio"),changed.objectById("patio"))
        assertEquals(initial.objectById("pool").boundary.nodes.map { it.edgeId }, changed.objectById("pool").boundary.nodes.map { it.edgeId })
        assertEquals(initial.objects,session.undo().objects)
        assertEquals(initial.objectById("pool").copingFootprint!!.outerBoundary,session.document.objectById("pool").copingFootprint!!.outerBoundary)
        assertEquals(changed.objects,session.redo().objects)
    }
    @Test fun `width edit is exact intent and reversible without modifying the waterline`() {
        val initial=doc(); val session=DesignSession(initial)
        session.execute(DesignCommand.SetCoping("pool",CopingSpec(16.0*CopingSpec.INCH)))
        assertEquals(initial.objectById("pool").boundary,session.document.objectById("pool").boundary)
        assertEquals(16.0,session.document.objectById("pool").coping!!.widthInches,1e-9)
        assertEquals(initial.objects,session.undo().objects)
    }
    @Test fun `self crossing boundary is rejected before committing or consuming redo`() {
        val session=DesignSession(doc()); session.execute(DesignCommand.Translate("pool",1.0,0.0));session.undo()
        val before=session.document
        assertThrows(IllegalArgumentException::class.java) {
            session.execute(DesignCommand.MoveVertex("pool","vertex-1",DesignPoint(-1.0,2.0)))
        }
        assertEquals(before,session.document);assertTrue(session.canRedo)
    }
    @Test fun `nonzero algebraic area does not make a self crossing pool valid`() {
        val crossing=boundary(listOf(0.0 to 0.0,6.0 to 4.0,0.0 to 5.0,4.0 to 0.0))
        assertTrue(abs(crossing.signedAreaSquareMetres)>0)
        assertThrows(IllegalArgumentException::class.java) { doc(pool(crossing)) }
    }
    @Test fun `a coping width that consumes an inside circular arc is rejected`() {
        val p=pool(DesignFixtures.organic());val d=doc(p)
        val arc=p.boundary.edges().first { it.start.bulge<0 }
        assertThrows(IllegalArgumentException::class.java) {
            // Use a narrower inside arc, within the supported 48-inch width range.
            doc(pool(p.boundary.changedBulge(arc.id,-1.0), CopingSpec(48.0*CopingSpec.INCH)))
        }
        assertEquals(p,d.objectById("pool"))
    }
    @Test fun `a narrow open bay is not silently bridged by the coping generator`() {
        val b=boundary(listOf(0.0 to 0.0,8.0 to 0.0,8.0 to 8.0,4.2 to 8.0,
            4.2 to 2.0,3.8 to 2.0,3.8 to 8.0,0.0 to 8.0))
        assertNotNull(pool(b,CopingSpec(2.0*CopingSpec.INCH)).copingFootprint)
        assertThrows(IllegalArgumentException::class.java) { doc(pool(b)) }
    }
    @Test fun `resolution ambiguous near touch is rejected instead of certified`() {
        val b=boundary(listOf(0.0 to 0.0,8.0 to 0.0,8.0 to 8.0,4.001 to 8.0,
            4.001 to 2.0,4.0 to 2.0,4.0 to 8.0,0.0 to 8.0))
        assertThrows(IllegalArgumentException::class.java) { doc(pool(b)) }
    }
    @Test fun `invalid width or unsupported generator cannot enter a saved pool`() {
        listOf(0.0,-1.0,Double.NaN,Double.POSITIVE_INFINITY,2.0).forEach {
            assertThrows(IllegalArgumentException::class.java) { CopingSpec(it) }
        }
        assertThrows(IllegalArgumentException::class.java) { CopingSpec(generatorVersion=2) }
    }
    @Test fun `locks protect coping properties and paving cannot be assigned coping`() {
        val s=DesignSession(doc(pool().copy(locked=true)))
        assertThrows(IllegalArgumentException::class.java) { s.execute(DesignCommand.SetCoping("pool",CopingSpec(0.4))) }
        assertThrows(IllegalArgumentException::class.java) { DesignCommands.apply(doc(),DesignCommand.SetCoping("patio",CopingSpec())) }
        assertFalse(s.canUndo)
    }
    @Test fun `legacy outline attachment is explicit and undo restores the outline`() {
        val legacy=pool().copy(coping=null);val s=DesignSession(doc(legacy))
        assertNull(legacy.copingFootprint)
        s.execute(DesignCommand.SetCoping("pool",CopingSpec()))
        assertNotNull(s.document.objectById("pool").copingFootprint)
        assertEquals(legacy,s.undo().objectById("pool"))
    }
    @Test fun `translation moves the whole derived footprint without changing identities or widths`() {
        val initial=doc(pool(DesignFixtures.organic()))
        val moved=DesignCommands.apply(initial,DesignCommand.Translate("pool",40.0,-20.0))
        val a=initial.objectById("pool");val b=moved.objectById("pool")
        assertEquals(a.coping,b.coping)
        assertEquals(a.copingFootprint!!.outerBoundary.size,b.copingFootprint!!.outerBoundary.size)
        a.copingFootprint!!.outerBoundary.zip(b.copingFootprint!!.outerBoundary).forEach { (p,q) ->
            assertEquals(p.x+40.0,q.x,1e-8);assertEquals(p.y-20.0,q.y,1e-8)
        }
    }
}
