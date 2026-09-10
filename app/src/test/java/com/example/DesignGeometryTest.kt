package com.example

import com.example.model.design.*
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class DesignGeometryTest {
    private val eps = 1e-9
    @Test fun `rectangle is measured in metres independently of output scale`() {
        val b = DesignFixtures.rectangle()
        assertEquals(32.0, b.perimeterMetres, eps)
        assertEquals(60.0, b.signedAreaSquareMetres, eps)
    }
    @Test fun `two semicircles retain analytic circumference and area`() {
        val b = DesignFixtures.circle(3.0)
        assertEquals(6 * PI, b.perimeterMetres, eps)
        assertEquals(9 * PI, b.signedAreaSquareMetres, eps)
        b.edges().forEach { e ->
            assertEquals(3.0, e.radiusMetres!!, eps)
            for (i in 0..40) assertEquals(3.0, e.pointAt(i / 40.0).distanceTo(e.centre!!), eps)
        }
    }
    @Test fun `reverse preserves physical edge identities and reverses signed area`() {
        val b = DesignFixtures.organic()
        val reversed = b.reversed()
        assertEquals(b.perimeterMetres, reversed.perimeterMetres, eps)
        assertEquals(-b.signedAreaSquareMetres, reversed.signedAreaSquareMetres, eps)
        b.edges().forEach { e ->
            val r = reversed.edges().single { it.id == e.id }
            assertEquals(e.start.point, r.end)
            assertEquals(e.end, r.start.point)
            assertEquals(-e.sweepRadians, r.sweepRadians, eps)
        }
        assertEquals(b, reversed.reversed())
    }
    @Test fun `translation keeps analytic metrics and IDs stable`() {
        val b = DesignFixtures.organic()
        val moved = b.translated(1e8, -1e8)
        assertEquals(b.perimeterMetres, moved.perimeterMetres, eps)
        assertEquals(b.signedAreaSquareMetres, moved.signedAreaSquareMetres, eps)
        assertEquals(b.nodes.map { it.edgeId }, moved.nodes.map { it.edgeId })
    }
    @Test fun `moving a vertex updates both incident edges without flattening or reidentifying`() {
        val b = DesignFixtures.organic()
        val moved = b.movedVertex("vertex-2", DesignPoint(11.0, 7.0))
        assertEquals(moved.nodes[2].point, moved.edges()[1].end)
        assertEquals(moved.nodes[2].point, moved.edges()[2].start.point)
        assertEquals(b.nodes.map { it.edgeId }, moved.nodes.map { it.edgeId })
        assertEquals(b.nodes.map { it.bulge }, moved.nodes.map { it.bulge })
        assertEquals(DesignPoint(10.0, 6.0), b.nodes[2].point)
    }
    @Test fun `arc edit keeps endpoints and reports rather than invents tangent constraints`() {
        val b = DesignFixtures.circle()
        b.nodes.forEach { assertEquals(0.0, b.joinDeflectionRadians(it.vertexId), eps) }
        val edited = b.changedBulge("edge-0", 0.5)
        assertEquals(b.nodes.map { it.point }, edited.nodes.map { it.point })
        assertTrue(edited.joinDeflectionRadians("vertex-0") > 0.1)
        assertEquals(1.0, b.nodes[0].bulge, 0.0)
    }
    @Test fun `sampling honours chord error without changing stored curves`() {
        val b = DesignFixtures.circle()
        val nodes = b.nodes.toList()
        val error = 0.002
        b.edges().forEach { edge ->
            val samples = edge.sample(error)
            samples.zipWithNext().forEach { (a, c) ->
                val middle = DesignPoint((a.x + c.x) / 2, (a.y + c.y) / 2)
                assertTrue(edge.radiusMetres!! - middle.distanceTo(edge.centre!!) <= error + eps)
            }
        }
        assertTrue(b.sample(error).size > b.sample(0.1).size)
        assertEquals(nodes, b.nodes)
        assertEquals(6 * PI, b.perimeterMetres, eps)
    }
    @Test fun `concave and convex analytic area agrees with independent sampled shoelace`() {
        val b = DesignFixtures.organic()
        val p = b.sample(0.00001)
        val area = p.indices.sumOf { i ->
            val a = p[i]; val c = p[(i + 1) % p.size]
            (a.x * c.y - a.y * c.x) / 2.0
        }
        assertTrue(b.nodes.any { it.bulge < 0 } && b.nodes.any { it.bulge > 0 })
        assertEquals(b.signedAreaSquareMetres, area, b.perimeterMetres * 0.00002)
    }
    @Test fun `shallow arc sampling and metric calculation are numerically stable`() {
        val edge = BoundaryEdge(BoundaryNode("v", DesignPoint(0.0, 0.0), "e", 1e-8), DesignPoint(10.0, 0.0))
        assertEquals(10.0, edge.lengthMetres, 1e-12)
        assertEquals(5.0, edge.pointAt(0.5).x, eps)
        assertEquals(-5e-8, edge.pointAt(0.5).y, 1e-15)
        assertEquals(listOf(edge.start.point, edge.end), edge.sample(0.001))
    }
    @Test fun `bad input and excessive sampling are rejected explicitly`() {
        assertThrows(IllegalArgumentException::class.java) { DesignPoint(Double.NaN, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { BoundaryNode("v", DesignPoint(0.0, 0.0), "e", 2.0) }
        assertThrows(IllegalArgumentException::class.java) { DesignFixtures.rectangle().movedVertex("missing", DesignPoint(1.0, 1.0)) }
        assertThrows(IllegalArgumentException::class.java) { DesignFixtures.rectangle().movedVertex("vertex-0", DesignPoint(10.0, 0.0)) }
        assertThrows(IllegalArgumentException::class.java) { DesignFixtures.circle().sample(1e-20, 100) }
        assertThrows(IllegalArgumentException::class.java) { DesignFixtures.circle().sample(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { DesignFixtures.circle().edges()[0].pointAt(2.0) }
    }
    @Test fun `boundary defensively owns its nodes and rejects duplicate IDs`() {
        val input = DesignFixtures.rectangle().nodes.toMutableList()
        val owned = DesignBoundary(input)
        input.clear()
        assertEquals(4, owned.nodes.size)
        assertThrows(UnsupportedOperationException::class.java) { (owned.nodes as MutableList<*>).clear() }
        assertThrows(IllegalArgumentException::class.java) { DesignBoundary(owned.nodes.map { it.copy(edgeId = "same") }) }
    }
    @Test fun `crossing boundary is algebraic geometry not a certified takeoff surface`() {
        val points = listOf(DesignPoint(0.0,0.0), DesignPoint(2.0,2.0), DesignPoint(0.0,2.0), DesignPoint(2.0,0.0))
        val b = DesignBoundary(points.mapIndexed { i,p -> BoundaryNode("v$i",p,"e$i") })
        assertEquals(0.0, b.signedAreaSquareMetres, eps)
        // Topology validation/Boolean surfaces are deliberately not claimed by this seam.
    }
}
