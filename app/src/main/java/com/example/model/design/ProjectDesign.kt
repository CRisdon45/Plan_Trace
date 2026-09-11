package com.example.model.design

import java.util.ArrayDeque
/** Design meaning and provenance are not inferred from paint colours or imported layer names. */
enum class DesignObjectKind { POOL, SPA, PAVING, TURF, GRAVEL, WALL, SITE_OUTLINE }
enum class GeometryConfidence { DESIGNED, ASSUMED, TRACED, FIELD_VERIFIED }

data class DesignObject(
    val id: String,
    val name: String,
    val kind: DesignObjectKind,
    val boundary: DesignBoundary,
    val locked: Boolean = false,
    val confidence: GeometryConfidence = GeometryConfidence.DESIGNED,
    val sourceReference: String? = null,
    val coping: CopingSpec? = null,
    val siteTrace: SiteTrace? = null
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(sourceReference == null || sourceReference.isNotBlank())
        require(coping == null || kind == DesignObjectKind.POOL || kind == DesignObjectKind.SPA) { "Coping belongs to a pool or spa" }
        siteTrace?.let {
            require(kind==DesignObjectKind.SITE_OUTLINE && confidence==GeometryConfidence.TRACED &&
                sourceReference==it.source.asset.sha256 && coping==null) { "Site trace metadata and object meaning disagree" }
            SiteOutlineGeometry.validate(boundary)
        }
    }
    /** Cached per immutable object; recomputed after an edit or decode, never saved as duplicate geometry. */
    val copingFootprint: CopingFootprint? by lazy { coping?.let { PoolCoping.derive(boundary, it) } }
}

/** Storage-neutral project authority. No PDF page owns these objects or their metric coordinates. */
class ProjectDesign(val id: String, objects: List<DesignObject> = emptyList(), val revision: Long = 0, val siteImage: SiteImage? = null) {
    val objects: List<DesignObject> = java.util.Collections.unmodifiableList(objects.toList())
    init {
        require(id.isNotBlank() && revision >= 0)
        require(objects.size <= 512) { "Initial design object budget exceeded" }
        require(objects.map { it.id }.distinct().size == objects.size) { "Duplicate design object ID" }
        objects.forEach { it.copingFootprint } // Reject the whole next state before history or saving is changed.
    }
    fun objectById(id: String): DesignObject = objects.singleOrNull { it.id == id }
        ?: throw IllegalArgumentException("Unknown design object: $id")
    internal fun revised(objects: List<DesignObject>, siteImage: SiteImage? = this.siteImage): ProjectDesign {
        require(revision < Long.MAX_VALUE) { "Revision overflow" }
        return ProjectDesign(id, objects, revision + 1, siteImage)
    }
    override fun equals(other: Any?): Boolean = other is ProjectDesign &&
        id == other.id && revision == other.revision && objects == other.objects && siteImage == other.siteImage
    override fun hashCode(): Int = 31 * (31 * id.hashCode() + objects.hashCode()) + revision.hashCode() + (siteImage?.hashCode() ?: 0)
}

sealed interface DesignCommand {
    data class SetSiteImage(val value: SiteImage?, val expected: SiteImage?) : DesignCommand
    data class Add(val value: DesignObject) : DesignCommand
    data class Remove(val objectId: String) : DesignCommand
    data class SetLocked(val objectId: String, val locked: Boolean) : DesignCommand
    data class Translate(val objectId: String, val dxMetres: Double, val dyMetres: Double) : DesignCommand
    data class MoveVertex(val objectId: String, val vertexId: String, val point: DesignPoint) : DesignCommand
    data class ChangeBulge(val objectId: String, val edgeId: String, val bulge: Double) : DesignCommand
    data class ReplaceBoundary(val objectId: String, val boundary: DesignBoundary) : DesignCommand
    data class SetCoping(val objectId: String, val spec: CopingSpec) : DesignCommand
    data class SetDimension(val objectId: String, val axis: SizeAxis, val metres: Double) : DesignCommand
}

/** Pure command path. It either returns a complete next state or throws without changing the input. */
object DesignCommands {
    fun apply(document: ProjectDesign, command: DesignCommand): ProjectDesign {
        if (command is DesignCommand.SetSiteImage) {
            require(document.siteImage == command.expected) { "Source changed while this operation was open. Start again" }
            return if (document.siteImage == command.value) document else document.revised(document.objects, command.value)
        }
        if (command is DesignCommand.Add) {
            require(document.objects.none { it.id == command.value.id }) { "Duplicate design object ID" }
            return document.revised(document.objects + command.value)
        }
        val id = when (command) {
            is DesignCommand.Remove -> command.objectId
            is DesignCommand.SetLocked -> command.objectId
            is DesignCommand.Translate -> command.objectId
            is DesignCommand.MoveVertex -> command.objectId
            is DesignCommand.ChangeBulge -> command.objectId
            is DesignCommand.ReplaceBoundary -> command.objectId
            is DesignCommand.SetCoping -> command.objectId
            is DesignCommand.SetDimension -> command.objectId
            else -> error("Unsupported command")
        }
        val current = document.objectById(id)
        if(command is DesignCommand.SetLocked) {
            if(current.locked==command.locked) return document
            return document.revised(document.objects.map { if(it.id==id) it.copy(locked=command.locked) else it })
        }
        require(!current.locked) { "Design object is locked: $id" }
        if (command is DesignCommand.Remove) return document.revised(document.objects.filterNot { it.id == id })
        if (command is DesignCommand.SetCoping) {
            if (command.spec == current.coping) return document
            return document.revised(document.objects.map { if (it.id == id) it.copy(coping = command.spec) else it })
        }
        val boundary = when (command) {
            is DesignCommand.Translate -> {
                require(command.dxMetres.isFinite() && command.dyMetres.isFinite())
                current.boundary.translated(command.dxMetres, command.dyMetres)
            }
            is DesignCommand.MoveVertex -> current.boundary.movedVertex(command.vertexId, command.point)
            is DesignCommand.ChangeBulge -> current.boundary.changedBulge(command.edgeId, command.bulge)
            is DesignCommand.ReplaceBoundary -> command.boundary
            is DesignCommand.SetDimension -> DesignDimensions.resize(current.boundary, command.axis, command.metres)
            else -> error("Unsupported boundary command")
        }
        if (boundary == current.boundary) return document
        return document.revised(document.objects.map { if (it.id == id) it.copy(boundary = boundary, siteTrace = it.siteTrace?.copy(adjusted=true)) else it })
    }
}

/** One deliberate command is one Undo. Preview and failed commands never create history entries. */
class DesignSession(initial: ProjectDesign, private val historyLimit: Int = 128) {
    var document: ProjectDesign = initial
        private set
    private val past = ArrayDeque<ProjectDesign>()
    private val future = ArrayDeque<ProjectDesign>()
    init { require(historyLimit in 1..1024) }
    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()
    fun preview(command: DesignCommand): ProjectDesign = DesignCommands.apply(document, command)
    fun execute(command: DesignCommand): ProjectDesign {
        val next = preview(command)
        if (next == document) return document
        past.addLast(document)
        if (past.size > historyLimit) past.removeFirst()
        future.clear()
        document = next
        return document
    }
    fun undo(): ProjectDesign {
        if (past.isEmpty()) return document
        val restored = document.revised(past.last().objects, past.last().siteImage) // validate before touching history
        future.addLast(document)
        past.removeLast()
        document = restored
        return document
    }
    fun redo(): ProjectDesign {
        if (future.isEmpty()) return document
        val restored = document.revised(future.last().objects, future.last().siteImage)
        past.addLast(document)
        future.removeLast()
        document = restored
        return document
    }
}
