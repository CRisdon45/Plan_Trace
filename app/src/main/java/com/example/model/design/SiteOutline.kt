package com.example.model.design

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.operation.valid.IsValidOp
import java.util.UUID
import kotlin.math.*

enum class SiteOutlineRole(val label: String) { HOUSE("Existing house"), PROPERTY("Property outline") }
enum class TraceRegistration { CURRENT, CHANGED, UNAVAILABLE }

/** Authored provenance. The original registration remains evidence, never a live transform of the outline. */
data class SiteTrace(val role: SiteOutlineRole, val source: SiteImage, val adjusted: Boolean = false) {
    init { require(source.calibration != null) { "Calibrate the source before creating a measured site outline" } }
    fun registration(current: SiteImage?): TraceRegistration = when {
        current == null || current.asset != source.asset -> TraceRegistration.UNAVAILABLE
        current.topLeft != source.topLeft || current.metresPerPixel != source.metresPerPixel -> TraceRegistration.CHANGED
        else -> TraceRegistration.CURRENT // Visibility and check evidence do not move the image.
    }
    fun notice(current: SiteImage?): String = "Traced, not field-verified" +
        (if (adjusted) " · adjusted after tracing" else "") + when (registration(current)) {
            TraceRegistration.CURRENT -> ""
            TraceRegistration.CHANGED -> " · source registration changed; review alignment"
            TraceRegistration.UNAVAILABLE -> " · original source not attached"
        }
}

/** Closed straight-edged site outlines only in this slice. No survey, setback or area certification. */
object SiteOutlineGeometry {
    const val MAX_CORNERS = 128
    fun validate(boundary: DesignBoundary) {
        require(boundary.nodes.size in 3..MAX_CORNERS && boundary.nodes.all { it.bulge == 0.0 }) {
            "Site outlines currently support 3–128 straight-edged corners"
        }
        require(boundary.edges().all { it.lengthMetres >= 0.01 }) { "Separate site corners by at least 1 cm" }
        val origin = boundary.nodes.first().point
        val points = boundary.nodes.map { Coordinate(it.point.x-origin.x, it.point.y-origin.y) }
        require(points.all { abs(it.x)<=2000.0 && abs(it.y)<=2000.0 }) { "Site outline exceeds the supported extent" }
        val polygon = GeometryFactory().createPolygon((points + points.first().copy()).toTypedArray())
        require(IsValidOp(polygon).isValid && polygon.area.isFinite() && polygon.area > 0.0001) {
            "Outline crosses or touches itself, or has no usable enclosed shape. Move back a corner and try again"
        }
    }
}

/** Uncommitted corner sequence. It is not written to the document or consumed as multiple Undo entries. */
class SiteOutlineDraft(
    val role: SiteOutlineRole, val source: SiteImage, val documentId: String, val revision: Long,
    val objectId: String = UUID.randomUUID().toString(), points: List<DesignPoint> = emptyList()
) {
    val points: List<DesignPoint> = java.util.Collections.unmodifiableList(points.toList())
    init {
        require(source.calibration != null && source.visible) { "Show and calibrate the source first" }
        require(documentId.isNotBlank() && objectId.isNotBlank() && revision >= 0)
        require(points.size <= SiteOutlineGeometry.MAX_CORNERS)
    }
    fun candidate(raw: DesignPoint, orthogonal: Boolean, grid: Boolean): DesignPoint {
        return CornerTarget.resolve(points,raw,orthogonal,grid).point
    }

    fun append(point: DesignPoint): SiteOutlineDraft {
        require(source.contains(source.toImage(point))) { "Choose the corner inside the source image" }
        require(points.size < SiteOutlineGeometry.MAX_CORNERS) { "Finish the outline before adding more corners" }
        require(points.isEmpty() || points.last().distanceTo(point)>=0.01) { "Choose a different corner" }
        return SiteOutlineDraft(role,source,documentId,revision,objectId,points+point)
    }
    fun back() = SiteOutlineDraft(role,source,documentId,revision,objectId,points.dropLast(1))
    fun finish(): DesignObject {
        require(points.size>=3) { "Mark at least three corners before closing" }
        val boundary=DesignBoundary(points.mapIndexed { i,p -> BoundaryNode("$objectId:v$i",p,"$objectId:e$i") })
        SiteOutlineGeometry.validate(boundary)
        return DesignObject(objectId,role.label,DesignObjectKind.SITE_OUTLINE,boundary,locked=true,
            confidence=GeometryConfidence.TRACED,sourceReference=source.asset.sha256,siteTrace=SiteTrace(role,source))
    }
}
