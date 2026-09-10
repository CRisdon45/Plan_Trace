package com.example.model.design

import kotlin.math.*

/** Canonical design coordinates: metres, Double precision, x right / y up. Not screen pixels. */
data class DesignPoint(val x: Double, val y: Double) {
    init { require(x.isFinite() && y.isFinite()) { "Design coordinates must be finite" } }
    fun distanceTo(other: DesignPoint): Double = hypot(x - other.x, y - other.y)
    fun translated(dx: Double, dy: Double) = DesignPoint(x + dx, y + dy)
}

/** The outgoing edge retains its ID when this vertex or its neighbour moves. */
data class BoundaryNode(
    val vertexId: String,
    val point: DesignPoint,
    val edgeId: String,
    val bulge: Double = 0.0
) {
    init {
        require(vertexId.isNotBlank() && edgeId.isNotBlank()) { "Vertex and edge IDs are required" }
        // This first seam supports arcs up to a semicircle. Larger arcs must be split explicitly.
        require(bulge.isFinite() && abs(bulge) <= 1.0) { "Arc bulge must be finite and in [-1, 1]" }
        require(bulge == 0.0 || abs(bulge) >= 1e-8) { "Arc too shallow; use a line or an explicit supported arc" }
    }
}

/** Standard bulge: tan(signed sweep / 4). Positive is counterclockwise in y-up coordinates. */
class BoundaryEdge(val start: BoundaryNode, val end: DesignPoint) {
    val id: String get() = start.edgeId
    val chordMetres = start.point.distanceTo(end)
    val sweepRadians = 4.0 * atan(start.bulge)
    val isLine: Boolean get() = start.bulge == 0.0
    init { require(chordMetres.isFinite() && chordMetres >= 1e-6) { "Zero-length or unsupported tiny edge: $id" } }
    val radiusMetres: Double? get() = if (isLine) null else
        chordMetres * (1.0 + start.bulge * start.bulge) / (4.0 * abs(start.bulge))
    val lengthMetres: Double get() = if (isLine) chordMetres else
        chordMetres * abs(sweepRadians) / (2.0 * sin(abs(sweepRadians) / 2.0))

    val centre: DesignPoint? get() {
        if (isLine) return null
        val factor = (1.0 - start.bulge * start.bulge) / (4.0 * start.bulge)
        return DesignPoint((start.point.x + end.x) / 2.0 - (end.y - start.point.y) * factor,
            (start.point.y + end.y) / 2.0 + (end.x - start.point.x) * factor)
    }

    /** Local chord basis avoids cancellation when a shallow arc has a distant centre. */
    fun pointAt(fraction: Double): DesignPoint {
        require(fraction.isFinite() && fraction in 0.0..1.0)
        if (fraction == 0.0) return start.point
        if (fraction == 1.0) return end
        val dx = end.x - start.point.x
        val dy = end.y - start.point.y
        if (isLine) return DesignPoint(start.point.x + dx * fraction, start.point.y + dy * fraction)
        val a = sweepRadians * fraction
        val h = chordMetres * (1.0 - start.bulge * start.bulge) / (4.0 * start.bulge)
        val oneMinusCos = 2.0 * sin(a / 2.0).pow(2)
        val along = chordMetres / 2.0 * oneMinusCos + h * sin(a)
        val across = -chordMetres / 2.0 * sin(a) + h * oneMinusCos
        return DesignPoint(start.point.x + (dx * along - dy * across) / chordMetres,
            start.point.y + (dy * along + dx * across) / chordMetres)
    }

    fun tangentAt(fraction: Double): DesignPoint {
        require(fraction.isFinite() && fraction in 0.0..1.0)
        val chordAngle = atan2(end.y - start.point.y, end.x - start.point.x)
        val angle = chordAngle + sweepRadians * (fraction - 0.5)
        return DesignPoint(cos(angle), sin(angle))
    }

    /** Signed area between the directed chord and arc, independent of tessellation. */
    internal fun segmentArea(): Double {
        val t = sweepRadians
        if (isLine) return 0.0
        val factor = if (abs(t) < 1e-3) t / 12.0 + t.pow(3) / 360.0 + t.pow(5) / 10080.0
            else (t - sin(t)) / (8.0 * sin(t / 2.0).pow(2))
        return chordMetres * chordMetres * factor
    }

    /** Caller-selected metric chord error. Refuse excessive work instead of silently lowering quality. */
    fun sample(maxChordErrorMetres: Double, maxSegments: Int = 8192): List<DesignPoint> {
        require(maxChordErrorMetres.isFinite() && maxChordErrorMetres > 0.0)
        require(maxSegments in 1..100_000)
        if (isLine) return listOf(start.point, end)
        val r = radiusMetres!!
        // sagitta = 2*r*sin(delta/4)^2; asin is stable for shallow angles.
        val maxAngle = 4.0 * asin(sqrt(min(1.0, maxChordErrorMetres / (2.0 * r))))
        val required = max(1.0, ceil(abs(sweepRadians) / maxAngle))
        require(required.isFinite() && required <= maxSegments) { "Arc sampling budget exceeded for $id" }
        val count = required.toInt()
        return (0..count).map { pointAt(it.toDouble() / count) }
    }
}

/**
 * Closed, ordered design boundary. It owns exact lines/arcs, never its rendered samples.
 * Connectivity is structural. Simplicity/Boolean validity is NOT certified in this first seam.
 */
class DesignBoundary(nodes: List<BoundaryNode>) {
    val nodes: List<BoundaryNode> = java.util.Collections.unmodifiableList(nodes.toList())
    init {
        require(nodes.size in 2..4096) { "A boundary needs 2..4096 nodes" }
        require(nodes.map { it.vertexId }.distinct().size == nodes.size) { "Duplicate vertex ID" }
        require(nodes.map { it.edgeId }.distinct().size == nodes.size) { "Duplicate edge ID" }
        edges().forEach { require(it.lengthMetres.isFinite()) }
        require(nodes.size > 2 || nodes.all { it.bulge != 0.0 }) { "Two-node boundary requires two arcs" }
    }
    fun edges(): List<BoundaryEdge> = nodes.mapIndexed { i, node -> BoundaryEdge(node, nodes[(i + 1) % nodes.size].point) }
    val perimeterMetres: Double get() = edges().sumOf { it.lengthMetres }

    /** Algebraic area, NOT a certified takeoff. Self-crossing loops must not be treated as surfaces. */
    val signedAreaSquareMetres: Double get() {
        val origin = nodes.first().point
        return edges().sumOf {
            val ax = it.start.point.x - origin.x; val ay = it.start.point.y - origin.y
            val bx = it.end.x - origin.x; val by = it.end.y - origin.y
            (ax * by - ay * bx) / 2.0 + it.segmentArea()
        }
    }

    fun translated(dx: Double, dy: Double) = DesignBoundary(nodes.map { it.copy(point = it.point.translated(dx, dy)) })
    fun movedVertex(vertexId: String, point: DesignPoint): DesignBoundary {
        require(nodes.any { it.vertexId == vertexId }) { "Unknown vertex: $vertexId" }
        return DesignBoundary(nodes.map { if (it.vertexId == vertexId) it.copy(point = point) else it })
    }
    fun changedBulge(edgeId: String, bulge: Double): DesignBoundary {
        require(nodes.any { it.edgeId == edgeId }) { "Unknown edge: $edgeId" }
        return DesignBoundary(nodes.map { if (it.edgeId == edgeId) it.copy(bulge = bulge) else it })
    }

    /** Reverse winding without changing which physical edge an edge ID denotes. */
    fun reversed(): DesignBoundary = DesignBoundary(nodes.indices.reversed().map { i ->
        val incoming = nodes[(i + nodes.size - 1) % nodes.size]
        nodes[i].copy(edgeId = incoming.edgeId, bulge = -incoming.bulge)
    })

    /** Zero means tangent continuity. This reports joins, it does not silently solve constraints. */
    fun joinDeflectionRadians(vertexId: String): Double {
        val i = nodes.indexOfFirst { it.vertexId == vertexId }
        require(i >= 0) { "Unknown vertex: $vertexId" }
        val edges = edges()
        val a = edges[(i + edges.size - 1) % edges.size].tangentAt(1.0)
        val b = edges[i].tangentAt(0.0)
        return abs(atan2(a.x * b.y - a.y * b.x, a.x * b.x + a.y * b.y))
    }

    fun sample(maxChordErrorMetres: Double, maxPoints: Int = 32768): List<DesignPoint> {
        require(maxPoints in 2..1_000_000)
        val result = mutableListOf<DesignPoint>()
        edges().forEach { edge ->
            val remaining = maxPoints - result.size
            require(remaining > 0) { "Boundary sampling budget exceeded" }
            val points = edge.sample(maxChordErrorMetres, min(8192, remaining))
            result.addAll(points.dropLast(1))
        }
        return result // Closed implicitly, like the authoritative boundary.
    }
    override fun equals(other: Any?): Boolean = other is DesignBoundary && nodes == other.nodes
    override fun hashCode(): Int = nodes.hashCode()
}
