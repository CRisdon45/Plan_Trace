package com.example

import com.example.model.design.*
import org.junit.Assert.*
import org.junit.Test

class DesignSessionTest {
    @Test fun `preview does not alter the authority or history`() {
        val initial = DesignFixtures.document()
        val session = DesignSession(initial)
        val preview = session.preview(DesignCommand.Translate("pool", 2.0, 3.0))
        assertNotEquals(initial, preview)
        assertEquals(initial, session.document)
        assertFalse(session.canUndo)
        assertFalse(session.canRedo)
    }
    @Test fun `one command undo and redo preserve unrelated objects and monotonic revisions`() {
        val initial = DesignFixtures.document()
        val session = DesignSession(initial)
        val changed = session.execute(DesignCommand.MoveVertex("pool", "vertex-2", DesignPoint(11.0, 7.0)))
        assertEquals(initial.objectById("patio"), changed.objectById("patio"))
        assertEquals(1L, changed.revision)
        assertEquals(initial.objects, session.undo().objects)
        assertEquals(2L, session.document.revision)
        assertEquals(changed.objects, session.redo().objects)
        assertEquals(3L, session.document.revision)
    }
    @Test fun `failed command is atomic and does not disturb redo`() {
        val session = DesignSession(DesignFixtures.document())
        session.execute(DesignCommand.Translate("pool", 1.0, 0.0)); session.undo()
        val before = session.document
        assertThrows(IllegalArgumentException::class.java) {
            session.execute(DesignCommand.MoveVertex("pool", "vertex-0", DesignPoint(10.0, 0.0)))
        }
        assertEquals(before, session.document)
        assertTrue(session.canRedo)
    }
    @Test fun `lock protects every mutation route`() {
        val obj = DesignFixtures.document().objectById("pool").copy(locked = true)
        val document = ProjectDesign("locked-case", listOf(obj))
        val commands = listOf(DesignCommand.Remove("pool"), DesignCommand.Translate("pool", 1.0, 1.0),
            DesignCommand.MoveVertex("pool", "vertex-0", DesignPoint(0.0, 1.0)),
            DesignCommand.ChangeBulge("pool", "edge-0", 0.2), DesignCommand.ReplaceBoundary("pool", DesignFixtures.circle()))
        val session = DesignSession(document)
        commands.forEach { c -> assertThrows(IllegalArgumentException::class.java) { session.execute(c) } }
        assertEquals(document, session.document)
        assertFalse(session.canUndo)
    }
    @Test fun `no-op does not erase redo or create an undo step`() {
        val session = DesignSession(DesignFixtures.document())
        session.execute(DesignCommand.Translate("pool", 1.0, 0.0)); session.undo()
        val before = session.document
        session.execute(DesignCommand.Translate("pool", 0.0, 0.0))
        assertEquals(before, session.document)
        assertTrue(session.canRedo)
        assertFalse(session.canUndo)
    }
    @Test fun `new edit after undo clears redo and history is bounded`() {
        val session = DesignSession(DesignFixtures.document(), historyLimit = 2)
        repeat(4) { session.execute(DesignCommand.Translate("pool", 1.0, 0.0)) }
        session.undo(); session.undo()
        assertFalse(session.canUndo)
        session.execute(DesignCommand.Translate("patio", 1.0, 0.0))
        assertFalse(session.canRedo)
    }
    @Test fun `add remove and replacement are whole reversible operations`() {
        val session = DesignSession(ProjectDesign("new"))
        val obj = DesignFixtures.document().objectById("pool")
        session.execute(DesignCommand.Add(obj))
        assertThrows(IllegalArgumentException::class.java) { session.execute(DesignCommand.Add(obj)) }
        session.execute(DesignCommand.ReplaceBoundary("pool", DesignFixtures.circle()))
        session.undo(); assertEquals(obj, session.document.objectById("pool"))
        session.execute(DesignCommand.Remove("pool")); assertTrue(session.document.objects.isEmpty())
        session.undo(); assertEquals(obj, session.document.objectById("pool"))
    }
    @Test fun `design owns its object collection and preserves provenance`() {
        val obj = DesignFixtures.document().objectById("pool").copy(confidence = GeometryConfidence.TRACED, sourceReference = "synthetic-source")
        val input = mutableListOf(obj)
        val doc = ProjectDesign("owned", input)
        input.clear()
        val updated = DesignCommands.apply(doc, DesignCommand.Translate("pool", 1.0, 2.0))
        assertEquals(GeometryConfidence.TRACED, updated.objectById("pool").confidence)
        assertEquals("synthetic-source", updated.objectById("pool").sourceReference)
        assertEquals(listOf(obj), doc.objects)
        assertThrows(UnsupportedOperationException::class.java) { (doc.objects as MutableList<*>).clear() }
    }
}
