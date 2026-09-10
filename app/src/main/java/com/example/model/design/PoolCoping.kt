package com.example.model.design

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.operation.buffer.BufferOp
import org.locationtech.jts.operation.buffer.BufferParameters
import org.locationtech.jts.operation.valid.IsValidOp
import org.locationtech.jts.precision.MinimumClearance
import kotlin.math.abs

/** Saved intent, not a second editable polygon. V1 uses mitred joins with limit 4. */
data class CopingSpec(val widthMetres: Double = 12.0 * INCH, val generatorVersion: Int = 1) {
    init {
        require(generatorVersion == 1) { "Unsupported coping generator; the saved pool has not been replaced" }
        require(widthMetres.isFinite() && widthMetres in 2.0 * INCH..48.0 * INCH) {
            "This preview supports coping widths from 2 to 48 inches"
        }
    }
    val widthInches: Double get() = widthMetres / INCH
    companion object { const val INCH = 0.0254 }
}

/** Immutable derived output. These points are never serialized as project authority. */
class CopingFootprint internal constructor(outer: List<DesignPoint>) {
    val outerBoundary: List<DesignPoint> = java.util.Collections.unmodifiableList(outer.toList())
}

/**
 * Resolution-guarded planar coping, not an analytic offset kernel or construction certification.
 * Exact pool lines/arcs remain authoritative. JTS works on a 0.25 mm chord approximation in local
 * coordinates. Near-degenerate boundaries, disappearing concavities and uncertain results fail
 * closed rather than being repaired with buffer(0) or dropping components. No area takeoff is exposed.
 */
object PoolCoping {
    const val CHORD_ERROR_METRES = 0.00025
    private const val CLEARANCE = 0.002
    private const val RECOVERY_TOLERANCE = 0.002
    private const val MAX_NODES = 256
    private const val MAX_SAMPLES = 4096
    private val factory = GeometryFactory()

    fun derive(boundary: DesignBoundary, spec: CopingSpec): CopingFootprint {
        require(boundary.nodes.size <= MAX_NODES) { "This coping preview supports at most $MAX_NODES source edges" }
        require(abs(boundary.signedAreaSquareMetres) > CLEARANCE * CLEARANCE) {
            "Pool boundary crosses itself or has no usable enclosed area"
        }
        // An outward offset reduces a concave circular arc's radius. Never erase that arc silently.
        val winding = if (boundary.signedAreaSquareMetres > 0) 1 else -1
        boundary.edges().filter { it.sweepRadians * winding < 0 }.forEach {
            require(it.radiusMetres!! > spec.widthMetres + CLEARANCE) {
                "Coping is too wide for an inside curve. Reduce its width or open the curve"
            }
        }
        val origin = boundary.nodes.first().point
        val points = boundary.sample(CHORD_ERROR_METRES, MAX_SAMPLES)
        val coordinates = points.map { Coordinate(it.x - origin.x, it.y - origin.y) }
        require(coordinates.all { abs(it.x) <= 1000.0 && abs(it.y) <= 1000.0 }) {
            "Pool extent exceeds this coping preview's supported range"
        }
        try {
            val pool = factory.createPolygon((coordinates + coordinates.first().copy()).toTypedArray())
            validSingle(pool, "Pool boundary crosses, touches or folds back on itself")
            require(MinimumClearance.getDistance(pool) > CLEARANCE) {
                "Pool edges are too close to validate safely. Separate or simplify the tight detail"
            }
            val parameters = BufferParameters(32, BufferParameters.CAP_FLAT, BufferParameters.JOIN_MITRE, 4.0).apply {
                simplifyFactor = 0.0 // Do not simplify away a small recess to make the result succeed.
            }
            val outer = BufferOp.bufferOp(pool, spec.widthMetres, parameters)
            validSingle(outer, "Coping would split or enclose an unsupported pocket")
            require(outer.numPoints <= MAX_SAMPLES * 2 && outer.covers(pool)) {
                "Coping could not be generated without losing the pool boundary"
            }
            // Offset then inset must recover the same boundary within this explicit numerical guard.
            // This rejects bridged narrow bays / clipped sharp corners, not just invalid final rings.
            val recovered = BufferOp.bufferOp(outer, -spec.widthMetres, parameters)
            validSingle(recovered, "Coping width closes a narrow recess or changes the pool outline")
            val originalEdge = pool.boundary
            val recoveredEdge = recovered.boundary
            require(originalEdge.buffer(RECOVERY_TOLERANCE, 16).covers(recoveredEdge) &&
                recoveredEdge.buffer(RECOVERY_TOLERANCE, 16).covers(originalEdge)) {
                "Coping width would close a recess or clip a tight corner. Reduce the width or reshape it"
            }
            val ring = outer.difference(pool)
            require(ring is Polygon && ring.numInteriorRing == 1 && IsValidOp(ring).isValid && ring.area > 0.0) {
                "Coping is not a single valid band around this pool"
            }
            val shell = (outer as Polygon).exteriorRing.coordinates.dropLast(1).map {
                DesignPoint(it.x + origin.x, it.y + origin.y)
            }
            return CopingFootprint(shell)
        } catch (error: TopologyException) {
            throw IllegalArgumentException("This pool/coping result is numerically uncertain. Simplify the tight detail", error)
        }
    }

    private fun validSingle(geometry: Geometry, message: String) {
        require(geometry is Polygon && !geometry.isEmpty && geometry.numInteriorRing == 0 &&
            geometry.area.isFinite() && IsValidOp(geometry).isValid) { message }
    }
}
