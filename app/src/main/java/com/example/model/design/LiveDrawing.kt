package com.example.model.design

import kotlin.math.*
import java.util.UUID

/** One resolved endpoint is shared by the guide, readout and eventual commit. */
data class DrawingTarget(val point: DesignPoint, val closing: Boolean, val assistance: String,
                         val snap: GeometrySnapMatch? = null)
object CornerTarget {
    fun resolve(points: List<DesignPoint>, raw: DesignPoint, orthogonal: Boolean, grid: Boolean,
                closeToleranceMetres: Double = 0.0, geometry: GeometrySnapIndex? = null,
                snapToleranceMetres: Double = 0.1, previousSnap: GeometrySnapKey? = null): DrawingTarget {
        require(closeToleranceMetres.isFinite() && closeToleranceMetres >= 0)
        if (points.size >= 3 && raw.distanceTo(points.first()) <= closeToleranceMetres)
            return DrawingTarget(points.first(), true, "Close outline")
        val axis = DrawingSnapAxes.constraint(points,raw,orthogonal)
        val query = axis?.let {
            val along=(raw.x-it.origin.x)*it.direction.x+(raw.y-it.origin.y)*it.direction.y
            it.origin.translated(along*it.direction.x,along*it.direction.y)
        } ?: raw
        val match = geometry?.resolve(query,snapToleranceMetres,previousSnap,axis=axis,
            guideDirection=DrawingSnapAxes.direction(points))
        // A geometric snap uses the exact reference, not a rounded nearby grid point.
        if(match!=null) return DrawingTarget(match.point,false,if(axis!=null) "Right angle" else "",match)
        var point = if (grid) GridAssist.snapPoint(raw) else raw
        if (orthogonal && points.size >= 2) {
            val first = points[0]; val second = points[1]; val last = points.last()
            val angle = atan2(second.y-first.y, second.x-first.x)
            val ux = cos(angle); val uy = sin(angle)
            val dx = point.x-last.x; val dy = point.y-last.y
            val along = dx*ux+dy*uy; val across = -dx*uy+dy*ux
            point = if (abs(along) >= abs(across)) DesignPoint(last.x+along*ux,last.y+along*uy)
                else DesignPoint(last.x-across*uy,last.y+across*ux)
        }
        // Right-angle projection takes precedence: do not claim an absolute grid intersection
        // if projecting onto a rotated/local axis moved the target off that grid.
        val assist = if (orthogonal && points.size >= 2) "Right angle" else if (grid) "1 ft grid" else ""
        return DrawingTarget(point,false,assist)
    }
}

/** Display rounding never changes stored coordinates. Approximately marks rounded readings. */
object LiveFeetInches {
    fun format(metres: Double): String {
        require(metres.isFinite() && metres >= 0)
        if(metres > 1e9) return java.lang.String.format(java.util.Locale.US,"≈ %.3g ft",metres/0.3048)
        val eighths = (metres / 0.0254 * 8).roundToLong()
        val feet = eighths / 96; val inches = (eighths % 96) / 8; val fraction = (eighths % 8).toInt()
        val parts = arrayOf("", "1/8", "1/4", "3/8", "1/2", "5/8", "3/4", "7/8")
        val suffix = if (fraction == 0) "$inches" else if (inches == 0L) parts[fraction] else "$inches ${parts[fraction]}"
        val approximate = abs(metres - eighths * 0.0254 / 8) > 1e-7
        return (if (approximate) "≈ " else "") + "$feet′ $suffix″"
    }
}

/** Transient feedback only, not a saved dimension annotation or inferred survey measurement. */
data class LiveMeasure(val target: DesignPoint, val lines: List<String>, val from: DesignPoint? = null,
                       val invalid: Boolean = false)
object LiveMeasurements {
    fun segment(points: List<DesignPoint>, target: DrawingTarget): LiveMeasure? {
        val from = points.lastOrNull() ?: return null
        return LiveMeasure(target.point, listOf((if (target.closing) "Close · " else "Line · ") +
            LiveFeetInches.format(from.distanceTo(target.point))) +
            (if (target.assistance.isNotEmpty() && !target.closing) listOf(target.assistance) else emptyList()), from)
    }
    /** Read edited edge lengths from the validated preview, never from raw pen travel. */
    fun editing(before: ProjectDesign, after: ProjectDesign, hit: DesignHit, down: DesignPoint): LiveMeasure {
        val obj = after.objectById(hit.objectId)
        return when (hit) {
            is DesignHit.Vertex -> {
                val nodes = obj.boundary.nodes; val index = nodes.indexOfFirst { it.vertexId == hit.vertexId }
                require(index >= 0)
                val edges = obj.boundary.edges()
                val incoming = edges[(index+edges.size-1)%edges.size]; val outgoing = edges[index]
                LiveMeasure(nodes[index].point, listOf(
                    "Previous ${if(incoming.isLine) "line" else "arc"} · ${LiveFeetInches.format(incoming.lengthMetres)}",
                    "Next ${if(outgoing.isLine) "line" else "arc"} · ${LiveFeetInches.format(outgoing.lengthMetres)}"))
            }
            is DesignHit.Curve -> {
                val edge = obj.boundary.edges().single { it.id == hit.edgeId }
                LiveMeasure(edge.pointAt(0.5), listOf("${if(edge.isLine) "Line" else "Arc"} · ${LiveFeetInches.format(edge.lengthMetres)}") +
                    (edge.radiusMetres?.let { listOf("Radius · ${LiveFeetInches.format(it)}") } ?: emptyList()))
            }
            is DesignHit.Body -> {
                val first = before.objectById(hit.objectId).boundary.nodes.first().point
                val last = obj.boundary.nodes.first().point
                val target = down.translated(last.x-first.x,last.y-first.y)
                LiveMeasure(target,listOf("Move · ${LiveFeetInches.format(first.distanceTo(last))}"),down)
            }
        }
    }
}

/** Hand-placed straight-edged proposed boundaries. Pool coping validates before any state is saved. */
class ProposedOutlineDraft(val kind: DesignObjectKind, val documentId: String, val revision: Long,
    val objectId: String = UUID.randomUUID().toString(), points: List<DesignPoint> = emptyList()) {
    val points: List<DesignPoint> = java.util.Collections.unmodifiableList(points.toList())
    val label: String get() = if(kind == DesignObjectKind.POOL) "Pool" else "Deck outline"
    init {
        require(kind == DesignObjectKind.POOL || kind == DesignObjectKind.PAVING)
        require(documentId.isNotBlank() && objectId.isNotBlank() && revision >= 0 && points.size <= 128)
    }
    fun append(point: DesignPoint): ProposedOutlineDraft {
        require(points.size < 128) { "Close the outline before adding more corners" }
        require(points.isEmpty() || points.last().distanceTo(point) >= 0.01) { "Choose a different corner" }
        return ProposedOutlineDraft(kind,documentId,revision,objectId,points+point)
    }
    fun back() = ProposedOutlineDraft(kind,documentId,revision,objectId,points.dropLast(1))
    fun finish(): DesignObject {
        require(points.size >= 3) { "Mark at least three corners before closing" }
        val boundary = DesignBoundary(points.mapIndexed { i, point -> BoundaryNode("$objectId:v$i",point,"$objectId:e$i") })
        SiteOutlineGeometry.validate(boundary)
        return DesignObject(objectId,label,kind,boundary,coping=if(kind==DesignObjectKind.POOL) CopingSpec() else null)
            .also { it.copingFootprint } // Fail a bad band before clearing the user's draft.
    }
}
