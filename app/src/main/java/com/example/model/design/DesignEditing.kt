package com.example.model.design

import kotlin.math.*
import java.util.UUID

/** Small original starting outlines. Not client plans and not complete pools with coping. */
object DesignStartingShapes {
    fun create(curved: Boolean, index: Int, kind: DesignObjectKind = DesignObjectKind.POOL): DesignObject {
        val length = 20.0 * 0.3048
        val width = 12.0 * 0.3048
        val points = if (curved) listOf(DesignPoint(0.0, 0.0), DesignPoint(length, 0.0),
            DesignPoint(length, width), DesignPoint(length * 0.6, width),
            DesignPoint(length * 0.4, width), DesignPoint(0.0, width))
        else listOf(DesignPoint(0.0, 0.0), DesignPoint(length, 0.0),
            DesignPoint(length, width), DesignPoint(0.0, width))
        val bulges = if (curved) listOf(0.0, 0.45, 0.0, -0.3, 0.0, 0.45) else List(4) { 0.0 }
        val boundary = DesignBoundary(points.mapIndexed { i, p ->
            BoundaryNode("vertex-$i", p, "edge-$i", bulges[i])
        }).translated((index % 3) * 9.0, -(index / 3) * 7.0)
        val label = if (kind == DesignObjectKind.PAVING) "Paving" else if (curved) "Curved pool" else "Pool"
        return DesignObject(UUID.randomUUID().toString(), "$label ${index + 1}", kind, boundary,
            coping = if (kind == DesignObjectKind.POOL || kind == DesignObjectKind.SPA) CopingSpec() else null)
    }
}

/** Screen coordinates are separate from the canonical y-up metric document. */
data class DesignViewport(val pixelsPerMetre: Double, val offsetX: Double, val offsetY: Double) {
    init { require(pixelsPerMetre.isFinite() && pixelsPerMetre > 0 && offsetX.isFinite() && offsetY.isFinite()) }
    fun toScreen(point: DesignPoint) = DesignPoint(offsetX + point.x * pixelsPerMetre, offsetY - point.y * pixelsPerMetre)
    fun toWorld(x: Double, y: Double) = DesignPoint((x - offsetX) / pixelsPerMetre, -(y - offsetY) / pixelsPerMetre)
    fun panned(dx: Double, dy: Double) = copy(offsetX = offsetX + dx, offsetY = offsetY + dy)
    fun zoomed(factor: Double, focusX: Double, focusY: Double): DesignViewport {
        require(factor.isFinite() && factor > 0)
        val scale = (pixelsPerMetre * factor).coerceIn(0.01, 3000.0)
        val actual = scale / pixelsPerMetre
        return DesignViewport(scale, focusX + (offsetX - focusX) * actual, focusY + (offsetY - focusY) * actual)
    }
    companion object {
        fun fit(document: ProjectDesign, width: Double, height: Double): DesignViewport {
            require(width > 0 && height > 0)
            val points = document.objects.flatMap { it.copingFootprint?.outerBoundary ?: it.boundary.sample(0.01) } +
                (document.siteImage?.takeIf { it.visible }?.corners() ?: emptyList())
            val minX = points.minOfOrNull { it.x } ?: 0.0
            val maxX = points.maxOfOrNull { it.x } ?: 8.0
            val minY = points.minOfOrNull { it.y } ?: 0.0
            val maxY = points.maxOfOrNull { it.y } ?: 5.0
            val scale = min(width * 0.78 / max(maxX - minX, 1.0), height * 0.72 / max(maxY - minY, 1.0)).coerceIn(0.01, 3000.0)
            return DesignViewport(scale, width / 2 - (minX + maxX) / 2 * scale, height / 2 + (minY + maxY) / 2 * scale)
        }
    }
}

sealed interface DesignHit {
    val objectId: String
    data class Vertex(override val objectId: String, val vertexId: String) : DesignHit
    data class SmoothAnchor(override val objectId: String, val vertexId: String) : DesignHit
    data class SmoothRadius(override val objectId: String, val edgeId: String) : DesignHit
    data class Side(override val objectId: String, val edgeId: String) : DesignHit
    data class Curve(override val objectId: String, val edgeId: String) : DesignHit
    data class Body(override val objectId: String) : DesignHit
}

object DesignPicking {
    fun hit(document: ProjectDesign, selectedId: String?, point: DesignPoint, toleranceMetres: Double, sideEditing: Boolean = false): DesignHit? {
        require(toleranceMetres.isFinite() && toleranceMetres > 0)
        val selected = document.objects.firstOrNull { it.id == selectedId && !it.locked }
        if (sideEditing) {
            val current = selected ?: return null
            val edge = current.boundary.edges().filter { StraightSideEditing.supports(current, it.id) }
                .minByOrNull { it.pointAt(0.5).distanceTo(point) }
            return edge?.takeIf { it.pointAt(0.5).distanceTo(point) <= toleranceMetres }
                ?.let { DesignHit.Side(current.id, it.id) }
        }
        selected?.boundary?.nodes?.minByOrNull { it.point.distanceTo(point) }?.let {
            if (it.point.distanceTo(point) <= toleranceMetres) return DesignHit.Vertex(selected.id, it.vertexId)
        }
        selected?.takeIf { it.siteTrace==null }?.boundary?.edges()?.minByOrNull { it.pointAt(0.5).distanceTo(point) }?.let {
            if (it.pointAt(0.5).distanceTo(point) <= toleranceMetres) return DesignHit.Curve(selected.id, it.id)
        }
        // Boundary-only picking: an unvalidated outline must not imply a filled valid surface.
        return document.objects.asReversed().firstOrNull { obj ->
            !obj.locked && obj.boundary.edges().any { edge ->
                edge.sample(min(0.005, toleranceMetres / 4)).zipWithNext().any { (a, b) ->
                    distanceToSegment(point, a, b) <= toleranceMetres
                }
            }
        }?.let { DesignHit.Body(it.id) }
    }
    private fun distanceToSegment(p: DesignPoint, a: DesignPoint, b: DesignPoint): Double {
        val dx = b.x - a.x; val dy = b.y - a.y
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
        return hypot(p.x - a.x - t * dx, p.y - a.y - t * dy)
    }
    /** Compute from the gesture's original document, never from accumulated preview samples. */
    fun drag(document: ProjectDesign, target: DesignHit, down: DesignPoint, current: DesignPoint): DesignCommand = when (target) {
        is DesignHit.SmoothAnchor -> {
            val b=document.objectById(target.objectId).boundary
            val p=b.nodes.single { it.vertexId==target.vertexId }.point
            DesignCommand.MoveTangentAnchor(target.objectId,target.vertexId,p.translated(current.x-down.x,current.y-down.y),b)
        }
        is DesignHit.SmoothRadius -> {
            val b=document.objectById(target.objectId).boundary
            val edge=b.edges().single { it.id==target.edgeId }
            val p=edge.pointAt(0.5).translated(current.x-down.x,current.y-down.y)
            DesignCommand.SetTangentRadius(target.objectId,target.edgeId,SmoothPoolEditing.radiusAt(edge,p),b)
        }
        is DesignHit.Body -> DesignCommand.Translate(target.objectId, current.x - down.x, current.y - down.y)
        is DesignHit.Vertex -> {
            val original = document.objectById(target.objectId).boundary.nodes.single { it.vertexId == target.vertexId }.point
            DesignCommand.MoveVertex(target.objectId, target.vertexId, original.translated(current.x - down.x, current.y - down.y))
        }
        is DesignHit.Side -> {
            val frame = StraightSideEditing.frame(document.objectById(target.objectId).boundary, target.edgeId)
            DesignCommand.MoveSide(target.objectId, target.edgeId, frame.offset(down, current))
        }
        is DesignHit.Curve -> {
            val edge = document.objectById(target.objectId).boundary.edges().single { it.id == target.edgeId }
            val midpoint = edge.pointAt(0.5).translated(current.x - down.x, current.y - down.y)
            val dx = edge.end.x - edge.start.point.x; val dy = edge.end.y - edge.start.point.y
            val across = (-(midpoint.x - (edge.start.point.x + edge.end.x) / 2) * dy +
                (midpoint.y - (edge.start.point.y + edge.end.y) / 2) * dx) / edge.chordMetres
            var bulge = (-2 * across / edge.chordMetres).coerceIn(-1.0, 1.0)
            if (abs(bulge) < 1e-8) bulge = 0.0
            DesignCommand.ChangeBulge(target.objectId, target.edgeId, bulge)
        }
    }
}
