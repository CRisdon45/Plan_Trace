package com.example

import com.example.model.design.*
import java.io.File
import kotlin.math.*
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

/** Compare only the broad phase with an exhaustive scan of the SAME exact resolver.
 * Existing independent geometry/priority tests remain the correctness oracle for the narrow phase.
 */
class SnapSpatialLookupTest {
    private fun sources(): List<GeometrySnapSource> = (0 until 36).map { n ->
        val b = if (n % 2 == 0) DesignFixtures.organic() else DesignFixtures.rectangle()
        val angle = n * 0.13
        GeometrySnapSource("object-$n", DesignBoundary(b.nodes.map { node ->
            node.copy(point = DesignPoint(node.point.x * cos(angle) - node.point.y * sin(angle) + (n % 6) * 18.0,
                node.point.x * sin(angle) + node.point.y * cos(angle) + (n / 6) * 18.0))
        }))
    }

    @Test fun `indexed and exhaustive results agree across seeded queries axes and exclusions`() {
        val sources = sources()
        val indexed = GeometrySnapIndex(sources)
        val exhaustive = GeometrySnapIndex(sources, false)
        val random = Random(48117)
        var previous: GeometrySnapKey? = null
        repeat(1200) { n ->
            val source = sources[n % sources.size]
            val edges = source.boundary.edges()
            val near = edges[n % edges.size].pointAt(random.nextDouble())
            val q = near.translated(random.nextDouble(-0.8, 0.8), random.nextDouble(-0.8, 0.8))
            val angle = random.nextDouble(-PI, PI)
            val direction = DesignPoint(cos(angle), sin(angle))
            val axis = if (n % 3 == 0) SnapAxis(q, direction) else null
            val tolerance = random.nextDouble(0.005, 0.5)
            val exclude = if (n % 5 == 0) source.objectId else null
            val expected = exhaustive.resolve(q, tolerance, previous, exclude, axis, direction)
            val actual = indexed.resolve(q, tolerance, previous, exclude, axis, direction)
            assertEquals("query $n", expected, actual)
            previous = actual?.key
        }
    }

    @Test fun `curved extrema beyond the chord box are never discarded`() {
        for (bulge in listOf(-1.0, -0.4, -1e-8, 1e-8, 0.4, 1.0)) {
            val b = DesignBoundary(listOf(BoundaryNode("a", DesignPoint(0.0, 0.0), "ab", bulge),
                BoundaryNode("b", DesignPoint(8.0, 0.0), "ba", bulge)))
            val source = listOf(GeometrySnapSource("curved", b))
            val index = GeometrySnapIndex(source)
            val exhaustive = GeometrySnapIndex(source, false)
            for (edge in b.edges()) for (fraction in listOf(0.01, 0.25, 0.5, 0.75, 0.99)) {
                val p = edge.pointAt(fraction)
                val t = edge.tangentAt(fraction)
                val q = p.translated(-t.y * 0.002, t.x * 0.002)
                val expected = exhaustive.resolve(q, 0.01)
                assertNotNull(expected)
                assertEquals("bulge=$bulge fraction=$fraction", expected, index.resolve(q, 0.01))
            }
        }
    }

    @Test fun `distant alignment guides remain candidates outside every finite envelope`() {
        val refs = listOf(GeometrySnapSource("house", DesignFixtures.rectangle()))
        val index = GeometrySnapIndex(refs)
        val q = DesignPoint(-100.0, 0.02)
        assertEquals(0 to 0, index.finiteCandidateCounts(q, 0.1))
        val match = index.resolve(q, 0.1)!!
        assertEquals(GeometrySnapKind.ALIGN_FIRST, match.key.kind)
        assertEquals(GeometrySnapIndex(refs, false).resolve(q, 0.1), match)
    }

    @Test fun `rotated construction axes keep exact finite intersections`() {
        val refs = listOf(GeometrySnapSource("house", DesignFixtures.rectangle()))
        val index = GeometrySnapIndex(refs)
        val direction = DesignPoint(sqrt(0.5), sqrt(0.5))
        val axis = SnapAxis(DesignPoint(0.0, 1.0), direction)
        val q = DesignPoint(4.99, 5.99)
        val expected = GeometrySnapIndex(refs, false).resolve(q, 0.1, axis = axis, guideDirection = direction)
        assertNotNull(expected)
        assertEquals(expected, index.resolve(q, 0.1, axis = axis, guideDirection = direction))
    }

    @Test fun `release radius and higher priority replacement survive spatial filtering`() {
        val refs = listOf(GeometrySnapSource("house", DesignFixtures.rectangle()))
        val index = GeometrySnapIndex(refs)
        val exhaustive = GeometrySnapIndex(refs, false)
        val previous = index.resolve(DesignPoint(0.01, 0.01), 0.1)!!.key
        val release = DesignPoint(0.14, 0.02)
        val held = index.resolve(release, 0.1, previous)!!
        assertEquals(previous, held.key)
        assertEquals(exhaustive.resolve(release, 0.1, previous), held)
        val edge = index.resolve(DesignPoint(0.3, 0.01), 0.1)!!.key
        assertEquals(exhaustive.resolve(DesignPoint(0.05, 0.01), 0.1, edge),
            index.resolve(DesignPoint(0.05, 0.01), 0.1, edge))
    }

    @Test fun `tree traversal order does not change deterministic tied references`() {
        val refs = (0 until 60).map { GeometrySnapSource("tie-${59-it}", DesignFixtures.rectangle()) }
        val q = DesignPoint(0.03, 0.04)
        val expected = GeometrySnapIndex(refs, false).resolve(q, 0.1)
        assertEquals(expected, GeometrySnapIndex(refs.reversed()).resolve(q, 0.1))
        assertEquals(expected, GeometrySnapIndex(refs).resolve(q, 0.1))
    }

    @Test fun `far world coordinates and huge query ranges preserve exhaustive behavior`() {
        val b = DesignFixtures.organic().translated(1e8, -1e8)
        val refs = listOf(GeometrySnapSource("distant", b))
        val index = GeometrySnapIndex(refs)
        val exhaustive = GeometrySnapIndex(refs, false)
        for (edge in b.edges()) {
            val q = edge.pointAt(0.37).translated(0.01, -0.01)
            assertEquals(exhaustive.resolve(q, 0.1), index.resolve(q, 0.1))
        }
        assertEquals(exhaustive.resolve(DesignPoint(0.0, 0.0), Double.MAX_VALUE),
            index.resolve(DesignPoint(0.0, 0.0), Double.MAX_VALUE))
    }

    @Test fun `excluded objects and oversized indexes do not leak partial results`() {
        val refs = listOf(GeometrySnapSource("house", DesignFixtures.rectangle()))
        assertNull(GeometrySnapIndex(refs).resolve(DesignPoint(0.01, 0.01), 0.1, excludeObjectId = "house"))
        val many = (0 until 2049).map { GeometrySnapSource("object-$it", DesignFixtures.rectangle()) }
        val index = GeometrySnapIndex(many)
        assertFalse(index.supported)
        assertNull(index.resolve(DesignPoint(0.01, 0.01), 0.1))
        assertEquals(0 to 0, index.finiteCandidateCounts(DesignPoint(0.0, 0.0), 0.1))
    }

    @Test fun `sparse scene shortlists one of 2048 edges without changing the selected snap`() {
        val refs = (0 until 512).map { n -> GeometrySnapSource("object-${n.toString().padStart(3, '0')}",
            DesignFixtures.rectangle().translated((n % 32) * 30.0, (n / 32) * 30.0)) }
        val indexed = GeometrySnapIndex(refs)
        val exhaustive = GeometrySnapIndex(refs, false)
        val q = DesignPoint(2.2, -0.03)
        val local = indexed.finiteCandidateCounts(q, 0.1)
        val full = exhaustive.finiteCandidateCounts(q, 0.1)
        assertEquals(0 to 1, local)
        assertEquals(4096 to 2048, full)
        assertEquals(exhaustive.resolve(q, 0.1), indexed.resolve(q, 0.1))
        val evidence = File("build/reports/design-geometry").apply { mkdirs() }
        File(evidence, "snap-spatial-work.txt").writeText(
            "Synthetic sparse scene: 512 rectangular objects.\n" +
            "Indexed finite point/edge candidates: $local\nExhaustive finite point/edge candidates: $full\n" +
            "Exact selected snap is unchanged. Alignment guides still consider all corners.\n" +
            "This measures candidate work, NOT elapsed time, frame rate or physical stylus latency.\n")
    }

    @Test fun `cached index snapshots stay independent of caller collections and later geometry`() {
        val original = GeometrySnapSource("house", DesignFixtures.rectangle())
        val refs = mutableListOf(original)
        val index = GeometrySnapIndex(refs)
        refs.clear()
        val q = DesignPoint(0.01, 0.01)
        val expected = index.resolve(q, 0.1)
        assertEquals(GeometrySnapKind.CORNER, expected!!.key.kind)
        val changed = original.copy(boundary = original.boundary.translated(30.0, 30.0))
        assertNull(GeometrySnapIndex(listOf(changed)).resolve(q, 0.1))
        assertEquals(expected, index.resolve(q, 0.1))
    }
}
