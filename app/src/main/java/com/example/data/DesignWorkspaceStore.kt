package com.example.data

import androidx.core.util.AtomicFile
import com.example.model.design.ProjectDesign
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** One explicitly opt-in local draft, separate from the legacy Room project database. */
class DesignWorkspaceStore(private val file: File) {
    private val atomic = AtomicFile(file)
    private val lock = locks.getOrPut(file.canonicalPath) { Any() }

    fun load(): ProjectDesign? = synchronized(lock) {
        if (!file.exists() && !File(file.path + ".bak").exists()) {
            if (File(file.path + ".new").exists()) throw IOException("An incomplete first save was retained. No empty replacement was created.")
            return@synchronized null
        }
        val bytes = atomic.openRead().use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var count = input.read(buffer)
            while (count != -1) {
                if (output.size() + count > 8_000_000) throw IOException("Saved design exceeds the supported size")
                output.write(buffer, 0, count)
                count = input.read(buffer)
            }
            output.toByteArray()
        }
        DesignJsonCodec.decode(bytes.toString(Charsets.UTF_8))
    }

    fun save(document: ProjectDesign) = synchronized(lock) {
        val encoded = DesignJsonCodec.encode(document).toByteArray(Charsets.UTF_8)
        val previous = load() // Corrupt files are not overwritten with a new empty/default document.
        if (previous != null) {
            require(previous.id == document.id) { "Saved design identity changed; refusing replacement" }
            require(document.revision >= previous.revision) { "A newer design is already saved" }
            if (document.revision == previous.revision) {
                require(previous == document) { "Conflicting content at the same revision" }
                return@synchronized
            }
        }
        val stream = atomic.startWrite()
        try {
            stream.write(encoded)
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
        check(load() == document) { "Saved design could not be verified" }
    }
    companion object {
        private val locks = ConcurrentHashMap<String, Any>()
        fun fileIn(filesDir: File) = File(filesDir, "project-design/workspace.json")
    }
}
