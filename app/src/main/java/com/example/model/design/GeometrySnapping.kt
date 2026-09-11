package com.example.model.design

import kotlin.math.*

/** Geometric references are read-only. A snap is a one-time placement, not an attachment. */
data class GeometrySnapSource(val objectId: String, val boundary: DesignBoundary)
enum class GeometrySnapKind(val priority: Int) {
    CORNER(0), MIDPOINT(1), EDGE(2), ALIGN_FIRST(3), ALIGN_SECOND(3)
}
data class GeometrySnapKey(val objectId: String, val elementId: String, val kind: GeometrySnapKind)
data class GeometrySnapMatch(val key: GeometrySnapKey, val point: DesignPoint, val reference: DesignPoint)

/** A deliberate drafting axis takes precedence over attraction to a reference. */
data class SnapAxis(val origin: DesignPoint, val direction: DesignPoint) {
    init { require(abs(hypot(direction.x, direction.y) - 1.0) < 1e-8) }
    fun contains(p: DesignPoint): Boolean = abs(cross(p.x-origin.x, p.y-origin.y, direction.x, direction.y)) <= 1e-8
}

private fun cross(ax: Double, ay: Double, bx: Double, by: Double) = ax*by-ay*bx

/** Immutable index of canonical corners and edges, never image pixels or sampled display paths.
 * The budget is all-or-nothing: a large design is not silently indexed only in part.
 */
class GeometrySnapIndex(sources: List<GeometrySnapSource>) {
    companion object { const val MAX_EDGES = 8192 }
    val supported = sources.sumOf { it.boundary.nodes.size.toLong() } <= MAX_EDGES
    private data class PointRef(val key: GeometrySnapKey, val point: DesignPoint)
    private data class EdgeRef(val key: GeometrySnapKey, val edge: BoundaryEdge)
    private val points: List<PointRef>
    private val edges: List<EdgeRef>
    init {
        require(sources.map { it.objectId }.distinct().size == sources.size) { "Duplicate snap-source identity" }
        val ordered = if(supported) sources.sortedBy { it.objectId } else emptyList()
        points = ordered.flatMap { source ->
            source.boundary.nodes.map { PointRef(GeometrySnapKey(source.objectId,it.vertexId,GeometrySnapKind.CORNER),it.point) } +
                source.boundary.edges().map { PointRef(GeometrySnapKey(source.objectId,it.id,GeometrySnapKind.MIDPOINT),it.pointAt(0.5)) }
        }
        edges = ordered.flatMap { source -> source.boundary.edges().map {
            EdgeRef(GeometrySnapKey(source.objectId,it.id,GeometrySnapKind.EDGE),it)
        } }
    }

    /** Tolerances are provided in metres from a fixed screen-space radius, not a fixed yard distance.
     * A captured reference has a wider release radius. Higher-priority corners can replace an edge
     * capture, but equally ranked neighbours cannot alternate while the pen is held near a boundary.
     */
    fun resolve(query: DesignPoint, toleranceMetres: Double, previous: GeometrySnapKey? = null,
                excludeObjectId: String? = null, axis: SnapAxis? = null,
                guideDirection: DesignPoint = DesignPoint(1.0,0.0)): GeometrySnapMatch? {
        require(toleranceMetres.isFinite() && toleranceMetres > 0)
        require(abs(hypot(guideDirection.x,guideDirection.y)-1.0) < 1e-8)
        if(!supported) return null
        var best: GeometrySnapMatch? = null
        var bestDistance = Double.POSITIVE_INFINITY
        var held: GeometrySnapMatch? = null
        fun offer(key: GeometrySnapKey, point: DesignPoint?, reference: DesignPoint) {
            if(point==null || key.objectId==excludeObjectId || (axis!=null && !axis.contains(point))) return
            val distance=query.distanceTo(point)
            if(key==previous && distance <= toleranceMetres*1.6) held=GeometrySnapMatch(key,point,reference)
            if(distance > toleranceMetres) return
            val candidate=GeometrySnapMatch(key,point,reference)
            val prior=best
            val stableOrder = if(prior==null) -1 else compareValuesBy(key,prior.key,
                { it.objectId },{ it.elementId },{ it.kind.ordinal })
            if(prior==null || key.kind.priority<prior.key.kind.priority ||
                (key.kind.priority==prior.key.kind.priority && (distance<bestDistance-1e-10 ||
                    (abs(distance-bestDistance)<=1e-10 && stableOrder<0)))) {
                best=candidate;bestDistance=distance
            }
        }
        points.forEach { ref ->
            if(ref.key.objectId!=excludeObjectId) {
                offer(ref.key,ref.point,ref.point)
                if(ref.key.kind==GeometrySnapKind.CORNER) {
                    listOf(guideDirection,DesignPoint(-guideDirection.y,guideDirection.x)).forEachIndexed { i,direction ->
                        val line=SnapAxis(ref.point,direction)
                        val target=if(axis==null) project(query,line) else intersection(axis,line)
                        offer(ref.key.copy(kind=if(i==0) GeometrySnapKind.ALIGN_FIRST else GeometrySnapKind.ALIGN_SECOND),target,ref.point)
                    }
                }
            }
        }
        edges.forEach { ref ->
            if(ref.key.objectId!=excludeObjectId) {
                val p=if(axis==null) closestOnEdge(ref.edge,query) else constrainedEdge(ref.edge,axis,query)
                offer(ref.key,p,p ?: ref.edge.start.point)
            }
        }
        val capture=held
        return if(capture!=null && (best==null || capture.key.kind.priority<=best!!.key.kind.priority)) capture else best
    }

    private fun project(point: DesignPoint, line: SnapAxis): DesignPoint {
        val distance=(point.x-line.origin.x)*line.direction.x+(point.y-line.origin.y)*line.direction.y
        return line.origin.translated(distance*line.direction.x,distance*line.direction.y)
    }
    private fun intersection(a: SnapAxis,b: SnapAxis): DesignPoint? {
        val determinant=cross(a.direction.x,a.direction.y,b.direction.x,b.direction.y)
        // Parallel guides convey no additional endpoint along a constrained construction axis.
        if(abs(determinant)<1e-10) return null
        val distance=cross(b.origin.x-a.origin.x,b.origin.y-a.origin.y,b.direction.x,b.direction.y)/determinant
        return a.origin.translated(distance*a.direction.x,distance*a.direction.y)
    }
    private fun constrainedEdge(edge: BoundaryEdge,axis: SnapAxis,query: DesignPoint): DesignPoint? {
        // Constrained arc intersections are deliberately not approximated as straight chords.
        // Arc endpoints/midpoints can still be selected when they lie on the active axis.
        if(!edge.isLine) return null
        val dx=edge.end.x-edge.start.point.x; val dy=edge.end.y-edge.start.point.y
        val determinant=cross(axis.direction.x,axis.direction.y,dx,dy)
        if(abs(determinant)<=1e-10*edge.chordMetres)
            return if(axis.contains(edge.start.point)) closestOnEdge(edge,query) else null
        val fraction=cross(edge.start.point.x-axis.origin.x,edge.start.point.y-axis.origin.y,
            axis.direction.x,axis.direction.y)/determinant
        return if(fraction in 0.0..1.0) edge.pointAt(fraction) else null
    }

    /** Nearest point on the finite line/arc, using the actual circle rather than its rendered segments. */
    fun closestOnEdge(edge: BoundaryEdge,query: DesignPoint): DesignPoint {
        val a=edge.start.point
        val ux=(edge.end.x-a.x)/edge.chordMetres; val uy=(edge.end.y-a.y)/edge.chordMetres
        val dx=query.x-a.x; val dy=query.y-a.y
        val x=dx*ux+dy*uy; val y=-dx*uy+dy*ux
        if(edge.isLine) return edge.pointAt((x/edge.chordMetres).coerceIn(0.0,1.0))
        // Local chord basis avoids building a distant world-space centre for shallow arcs.
        val h=edge.chordMetres*(1.0-edge.start.bulge*edge.start.bulge)/(4.0*edge.start.bulge)
        val startX=-edge.chordMetres/2.0; val startY=-h
        val qx=x-edge.chordMetres/2.0; val qy=y-h
        if(hypot(qx,qy)<=1e-12) return edge.start.point
        val angle=atan2(cross(startX,startY,qx,qy),startX*qx+startY*qy)
        val fraction=angle/edge.sweepRadians
        if(fraction in 0.0..1.0) return edge.pointAt(fraction)
        return if(query.distanceTo(a)<=query.distanceTo(edge.end)) a else edge.end
    }
}

/** Maps the existing first-edge-relative right-angle setting into the snap query. */
object DrawingSnapAxes {
    fun direction(points: List<DesignPoint>): DesignPoint {
        if(points.size<2) return DesignPoint(1.0,0.0)
        val a=points[0]; val b=points[1]; val length=a.distanceTo(b)
        require(length>0)
        return DesignPoint((b.x-a.x)/length,(b.y-a.y)/length)
    }
    fun constraint(points: List<DesignPoint>,raw: DesignPoint,orthogonal: Boolean): SnapAxis? {
        if(!orthogonal || points.size<2) return null
        val u=direction(points); val last=points.last()
        val dx=raw.x-last.x; val dy=raw.y-last.y
        val v=DesignPoint(-u.y,u.x)
        return SnapAxis(last,if(abs(dx*u.x+dy*u.y)>=abs(dx*v.x+dy*v.y)) u else v)
    }
}
