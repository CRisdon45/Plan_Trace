package com.example.model.design

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.round

/** The side moves parallel to itself. Its endpoints slide along the two adjacent straight
 * supporting lines. Those neighbours' far corners, every other edge and all IDs remain fixed.
 * Arc-adjacent and nearly collinear joins deliberately require a different operation. */
object StraightSideEditing {
    private const val MIN_JOIN_SINE = 1e-4
    private const val MIN_EDGE = 0.01

    data class Frame internal constructor(
        val index: Int, val a: DesignPoint, val b: DesignPoint,
        val tangent: DesignPoint, val normal: DesignPoint,
        val firstVelocity: DesignPoint, val secondVelocity: DesignPoint
    ) {
        val midpoint: DesignPoint get() = a.translated((b.x-a.x)/2, (b.y-a.y)/2)
        val midpointVelocity: DesignPoint get() = DesignPoint(
            (firstVelocity.x+secondVelocity.x)/2, (firstVelocity.y+secondVelocity.y)/2)
        fun target(offset: Double): DesignPoint = midpoint.translated(
            midpointVelocity.x*offset, midpointVelocity.y*offset)
        fun offset(down: DesignPoint, current: DesignPoint): Double =
            (current.x-down.x)*normal.x + (current.y-down.y)*normal.y
    }

    fun frame(boundary: DesignBoundary, edgeId: String): Frame {
        val edges = boundary.edges()
        val index = edges.indexOfFirst { it.id == edgeId }
        require(index >= 0) { "Unknown pool side" }
        val edge = edges[index]
        val previous = edges[(index+edges.size-1)%edges.size]
        val next = edges[(index+1)%edges.size]
        require(edges.size >= 4 && edge.isLine && previous.isLine && next.isLine) {
            "Move sides requires a straight side with straight adjoining edges"
        }
        val tangent = DesignPoint((edge.end.x-edge.start.point.x)/edge.chordMetres,
            (edge.end.y-edge.start.point.y)/edge.chordMetres)
        val normal = DesignPoint(-tangent.y, tangent.x)
        fun velocity(neighbour: BoundaryEdge): DesignPoint {
            val x = (neighbour.end.x-neighbour.start.point.x)/neighbour.chordMetres
            val y = (neighbour.end.y-neighbour.start.point.y)/neighbour.chordMetres
            val crossing = x*normal.x + y*normal.y
            require(abs(crossing) >= MIN_JOIN_SINE) { "This side has a nearly straight-through join; move its corners instead" }
            return DesignPoint(x/crossing, y/crossing)
        }
        return Frame(index, edge.start.point, edge.end, tangent, normal, velocity(previous), velocity(next))
    }

    fun supports(obj: DesignObject, edgeId: String): Boolean =
        !obj.locked && obj.kind in setOf(DesignObjectKind.POOL, DesignObjectKind.SPA) && obj.coping != null &&
            runCatching { frame(obj.boundary, edgeId) }.isSuccess

    fun canEdit(obj: DesignObject?): Boolean = obj != null && obj.boundary.nodes.any { supports(obj, it.edgeId) }

    fun move(boundary: DesignBoundary, edgeId: String, offsetMetres: Double): DesignBoundary {
        require(offsetMetres.isFinite() && abs(offsetMetres) <= 1000.0) { "Pool side movement exceeds the supported range" }
        val frame = frame(boundary, edgeId)
        if (abs(offsetMetres) < 1e-10) return boundary
        val a = frame.a.translated(frame.firstVelocity.x*offsetMetres, frame.firstVelocity.y*offsetMetres)
        val b = frame.b.translated(frame.secondVelocity.x*offsetMetres, frame.secondVelocity.y*offsetMetres)
        val nodes = boundary.nodes
        val i = frame.index
        val j = (i+1)%nodes.size
        val next = DesignBoundary(nodes.mapIndexed { n, node ->
            when(n) { i -> node.copy(point=a); j -> node.copy(point=b); else -> node }
        })
        val oldEdges = boundary.edges()
        val newEdges = next.edges()
        // Do not let a large pointer jump pass THROUGH zero length and recreate a folded side.
        for (k in listOf((i+nodes.size-1)%nodes.size, i, j)) {
            val old = oldEdges[k]; val changed = newEdges[k]
            val projection = ((changed.end.x-changed.start.point.x)*(old.end.x-old.start.point.x) +
                (changed.end.y-changed.start.point.y)*(old.end.y-old.start.point.y))/old.chordMetres
            require(projection >= MIN_EDGE) { "This move would collapse or reverse a pool edge" }
        }
        require(boundary.signedAreaSquareMetres * next.signedAreaSquareMetres > 0) {
            "This move would invert the pool outline"
        }
        return next // The complete command then validates pool topology and following coping.
    }
}

/** Resolve one side-handle target before validation. Grid means one-foot perpendicular travel,
 * not independently rounding both endpoints (which would skew a rotated side). Object references
 * take priority. The oblique midpoint's actual locus is the constraint, not an invented axis. */
data class SideTarget(val command: DesignCommand.MoveSide, val point: DesignPoint, val snap: GeometrySnapMatch?)
object StraightSideTargets {
    fun resolve(obj: DesignObject, command: DesignCommand.MoveSide, grid: Boolean,
                index: GeometrySnapIndex?, toleranceMetres: Double,
                previous: GeometrySnapKey? = null): SideTarget {
        require(obj.id == command.objectId && command.offsetMetres.isFinite())
        val frame = StraightSideEditing.frame(obj.boundary, command.edgeId)
        val velocity = frame.midpointVelocity
        val length = hypot(velocity.x, velocity.y)
        val axis = SnapAxis(frame.midpoint, DesignPoint(velocity.x/length, velocity.y/length))
        val query = frame.target(command.offsetMetres)
        val match = index?.resolve(query, toleranceMetres, previous, excludeObjectId=obj.id,
            axis=axis, guideDirection=frame.tangent)
        val offset = if(match != null) frame.offset(frame.midpoint, match.point)
            else if(grid) round(command.offsetMetres/DesignDimensions.FOOT)*DesignDimensions.FOOT
            else command.offsetMetres
        return SideTarget(command.copy(offsetMetres=offset), frame.target(offset), match)
    }
}
