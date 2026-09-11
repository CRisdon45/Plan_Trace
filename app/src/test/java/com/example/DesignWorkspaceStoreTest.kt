package com.example

import androidx.core.util.AtomicFile
import com.example.data.DesignWorkspaceStore
import com.example.model.design.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DesignWorkspaceStoreTest {
    @get:Rule val temp = TemporaryFolder()
    private fun path() = File(temp.root, "project-design/workspace.json")
    @Test fun `missing draft does not create a file and save reopens exact curves`() {
        val store = DesignWorkspaceStore(path())
        assertNull(store.load()); assertFalse(path().exists())
        val doc = DesignFixtures.document()
        store.save(doc)
        assertEquals(doc, DesignWorkspaceStore(path()).load())
    }
    @Test fun `older revision and same revision different data cannot overwrite a newer save`() {
        val store = DesignWorkspaceStore(path())
        val old = DesignFixtures.document()
        val newer = DesignCommands.apply(old, DesignCommand.Translate("pool", 2.0, 0.0))
        store.save(newer)
        assertThrows(IllegalArgumentException::class.java) { store.save(old) }
        assertThrows(IllegalArgumentException::class.java) { store.save(ProjectDesign(old.id, old.objects, newer.revision)) }
        assertEquals(newer, store.load())
    }
    @Test fun `a corrupt existing file remains intact instead of being reset`() {
        path().parentFile!!.mkdirs(); path().writeText("{incomplete")
        val store = DesignWorkspaceStore(path())
        assertThrows(Exception::class.java) { store.load() }
        assertThrows(Exception::class.java) { store.save(DesignFixtures.document()) }
        assertEquals("{incomplete", path().readText())
    }
    @Test fun `interrupted replacement reads the last committed draft`() {
        val store = DesignWorkspaceStore(path())
        val doc = DesignFixtures.document(); store.save(doc)
        val atomic = AtomicFile(path())
        val stream = atomic.startWrite(); stream.write("{unfinished".toByteArray()); stream.close()
        assertEquals(doc, DesignWorkspaceStore(path()).load())
    }
    @Test fun `incomplete initial save is retained and not mistaken for an empty workspace`() {
        path().parentFile!!.mkdirs(); File(path().path + ".new").writeText("partial")
        val store = DesignWorkspaceStore(path())
        assertThrows(Exception::class.java) { store.load() }
        assertThrows(Exception::class.java) { store.save(DesignFixtures.document()) }
        assertEquals("partial", File(path().path + ".new").readText())
    }
    @Test fun `another document identity is not allowed to replace the draft`() {
        val store = DesignWorkspaceStore(path()); val doc = DesignFixtures.document(); store.save(doc)
        assertThrows(IllegalArgumentException::class.java) { store.save(ProjectDesign("other", doc.objects, 9)) }
        store.save(doc) // Idempotent verified save.
        assertEquals(doc, store.load())
    }
}
