package com.example

import com.example.model.design.*
import org.junit.Assert.*
import org.junit.Test

class DesignEditingTest {
    @Test fun `viewport roundtrips and zoom preserves its focus point`() {
        val view = DesignViewport(80.0, 140.0, 500.0)
        val p = DesignPoint(3.2, -2.7)
        val s = view.toScreen(p)
        assertEquals(p.x, view.toWorld(s.x, s.y).x, 1e-12)
        assertEquals(p.y, view.toWorld(s.x, s.y).y, 1e-12)
        val focus = view.toWorld(250.0, 270.0)
        val zoomed = view.zoomed(2.0, 250.0, 270.0)
        assertEquals(focus, zoomed.toWorld(250.0, 270.0))
    }
    @Test fun `vertex handle takes priority and drag preserves its initial offset`() {
        val doc = DesignFixtures.document()
        val hit = DesignPicking.hit(doc, "pool", DesignPoint(0.01, 0.02), 0.1)
        assertEquals(DesignHit.Vertex("pool", "vertex-0"), hit)
        val cmd = DesignPicking.drag(doc, hit!!, DesignPoint(0.01, 0.02), DesignPoint(1.01, 2.02))
        val result = DesignCommands.apply(doc, cmd)
        assertEquals(DesignPoint(1.0, 2.0), result.objectById("pool").boundary.nodes.first().point)
        assertEquals(doc.objectById("patio"), result.objectById("patio"))
    }
    @Test fun `curve handle changes bulge without moving endpoints`() {
        val doc = DesignFixtures.document()
        val edge = doc.objectById("pool").boundary.edges()[3]
        val down = edge.pointAt(0.5)
        val hit = DesignPicking.hit(doc, "pool", down, 0.01)
        assertEquals(DesignHit.Curve("pool", edge.id), hit)
        val changed = DesignCommands.apply(doc, DesignPicking.drag(doc, hit!!, down, down.translated(0.0, 0.2)))
        assertEquals(doc.objectById("pool").boundary.nodes.map { it.point }, changed.objectById("pool").boundary.nodes.map { it.point })
        assertNotEquals(edge.start.bulge, changed.objectById("pool").boundary.nodes[3].bulge)
    }
    @Test fun `locked objects and unvalidated interiors are not editable hit targets`() {
        val obj = DesignObject("r", "Outline", DesignObjectKind.POOL, DesignFixtures.rectangle())
        val doc = ProjectDesign("case", listOf(obj))
        assertNull(DesignPicking.hit(doc, null, DesignPoint(5.0, 3.0), 0.1))
        assertEquals(DesignHit.Body("r"), DesignPicking.hit(doc, null, DesignPoint(5.0, 0.0), 0.1))
        assertNull(DesignPicking.hit(ProjectDesign("case", listOf(obj.copy(locked = true))), "r", DesignPoint(0.0, 0.0), 0.1))
    }
    @Test fun `fit includes distant objects instead of enforcing an unusable minimum zoom`() {
        val obj = DesignObject("a", "A", DesignObjectKind.POOL, DesignFixtures.rectangle())
        val far = obj.copy(id = "b", boundary = obj.boundary.translated(0.0, 1000.0))
        val doc = ProjectDesign("wide", listOf(obj, far))
        val view = DesignViewport.fit(doc, 1200.0, 700.0)
        doc.objects.flatMap { it.boundary.nodes }.forEach {
            val point = view.toScreen(it.point)
            assertTrue(point.x in 0.0..1200.0 && point.y in 0.0..700.0)
        }
    }
    @Test fun `starting shapes have fresh identities and both curvature signs`() {
        val a = DesignStartingShapes.create(false, 0)
        val b = DesignStartingShapes.create(true, 1)
        assertNotEquals(a.id, b.id)
        assertEquals(64 * 0.3048, a.boundary.perimeterMetres, 1e-9)
        assertTrue(b.boundary.nodes.any { it.bulge < 0 } && b.boundary.nodes.any { it.bulge > 0 })
        val doc = ProjectDesign("new", listOf(a,b))
        val view = DesignViewport.fit(doc, 1200.0, 700.0)
        doc.objects.flatMap { it.boundary.sample(0.01) }.forEach { p ->
            val screen = view.toScreen(p)
            assertTrue(screen.x in 0.0..1200.0 && screen.y in 0.0..700.0)
        }
    }
}
