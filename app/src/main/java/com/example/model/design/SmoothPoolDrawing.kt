package com.example.model.design

import java.util.UUID
import kotlin.math.*

/** Temporary guide points only. Accepted pools store the existing canonical boundary, not guides. */
class SmoothPoolDraft(val documentId: String, val revision: Long,
    val objectId: String = UUID.randomUUID().toString(), points: List<DesignPoint> = emptyList()) {
    val points: List<DesignPoint> = java.util.Collections.unmodifiableList(points.toList())
    init { require(documentId.isNotBlank() && objectId.isNotBlank() && revision >= 0 && points.size <= 32) }
    fun append(p: DesignPoint): SmoothPoolDraft {
        require(points.size < 32) { "Close this pool before adding more shape points" }
        require(points.isEmpty() || points.last().distanceTo(p) >= 0.02) { "Place the next point farther away" }
        return SmoothPoolDraft(documentId, revision, objectId, points + p)
    }
    fun back() = SmoothPoolDraft(documentId, revision, objectId, points.dropLast(1))
    fun finish(): DesignObject {
        val b = SmoothPoolDrawing.boundary(points, objectId)
        return DesignObject(objectId, "Smooth pool", DesignObjectKind.POOL, b, coping = CopingSpec())
            .also { it.copingFootprint } // The same full-pool acceptance gate as a later edit.
    }
}

object SmoothPoolDrawing {
    /** Angle-bisector tangents through deliberate guide points; no fitting or hidden point deletion. */
    fun boundary(points: List<DesignPoint>, id: String): DesignBoundary {
        require(id.isNotBlank() && points.size in 3..32) { "Place at least three shape points" }
        val n = points.size
        val tangents = points.indices.map { i ->
            val p = points[i]; val prev = points[(i+n-1)%n]; val next = points[(i+1)%n]
            require(p.distanceTo(prev) >= 0.02 && p.distanceTo(next) >= 0.02) { "Shape points are too close" }
            val a = TangentBiarc.unit(DesignPoint(p.x-prev.x,p.y-prev.y))
            val b = TangentBiarc.unit(DesignPoint(next.x-p.x,next.y-p.y))
            val sum = DesignPoint(a.x+b.x,a.y+b.y)
            require(hypot(sum.x,sum.y) > 0.05) { "The outline turns back on itself; move or remove that point" }
            TangentBiarc.unit(sum)
        }
        return DesignBoundary(points.indices.flatMap { i ->
            val j = (i+1)%n
            val pair = TangentBiarc.connect(points[i],tangents[i],points[j],tangents[j])
            listOf(BoundaryNode("$id:v${2*i}",points[i],"$id:e${2*i}",pair.firstBulge),
                BoundaryNode("$id:v${2*i+1}",pair.point,"$id:e${2*i+1}",pair.secondBulge))
        })
    }
}

enum class SmoothEditMode { OFF, SHAPE, RADIUS }

/** Scope/picking and gesture mathematics shared by the canvas and tests. No screen-unit authority. */
object SmoothPoolEditing {
    fun canEdit(obj: DesignObject?): Boolean = obj != null && !obj.locked &&
        obj.kind in setOf(DesignObjectKind.POOL,DesignObjectKind.SPA) && obj.coping != null &&
        obj.boundary.nodes.size >= 6 && obj.boundary.nodes.all {
            obj.boundary.joinDeflectionRadians(it.vertexId) <= TangentBiarc.ANGLE_TOLERANCE }

    fun hit(obj: DesignObject, mode: SmoothEditMode, point: DesignPoint, tolerance: Double): DesignHit? {
        if(!canEdit(obj) || mode == SmoothEditMode.OFF) return null
        val b = obj.boundary; val edges = b.edges()
        // Boundary picking, not an arbitrary point anywhere in the pool's bounding box.
        val edge = edges.minBy { e -> (0..24).minOf { e.pointAt(it/24.0).distanceTo(point) } }
        if((0..24).minOf { edge.pointAt(it/24.0).distanceTo(point) } > tolerance) return null
        return if(mode == SmoothEditMode.SHAPE) {
            val node = b.nodes.minBy { it.point.distanceTo(point) }
            DesignHit.SmoothAnchor(obj.id,node.vertexId)
        } else {
            val i = edges.indexOf(edge); val next = edges[(i+1)%edges.size]
            if(edge.isLine || next.isLine) null else DesignHit.SmoothRadius(obj.id,edge.id)
        }
    }

    fun radiusAt(edge: BoundaryEdge, target: DesignPoint): Double {
        require(!edge.isLine) { "Select a circular arc" }
        val p = edge.start.point; val t = edge.tangentAt(0.0)
        val dx = target.x-p.x; val dy = target.y-p.y
        val across = (-t.y*dx+t.x*dy)*sign(edge.sweepRadians)
        require(across > 1e-8 && hypot(dx,dy) > 1e-6) { "This drag cannot retain the selected arc direction" }
        val radius = (dx*dx+dy*dy)/(2*across)
        require(radius.isFinite()) { "Radius is numerically uncertain" }
        return radius
    }

    fun handles(obj: DesignObject, mode: SmoothEditMode, focusId: String?): Triple<DesignPoint,DesignPoint,DesignPoint>? {
        if(!canEdit(obj) || mode==SmoothEditMode.OFF) return null
        val nodes=obj.boundary.nodes; val n=nodes.size
        val i=nodes.indexOfFirst { if(mode==SmoothEditMode.SHAPE) it.vertexId==focusId else it.edgeId==focusId }
        if(i<0) return null
        return if(mode==SmoothEditMode.SHAPE) Triple(nodes[i].point,nodes[(i+n-2)%n].point,nodes[(i+2)%n].point)
            else Triple(obj.boundary.edges()[i].pointAt(0.5),nodes[i].point,nodes[(i+2)%n].point)
    }
}
