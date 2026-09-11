package com.example.model.design

import java.util.Locale
import kotlin.math.*

enum class SizeAxis { LENGTH, WIDTH }
data class DesignSize(val lengthMetres: Double, val widthMetres: Double, val rectangular: Boolean)

/** Exact values belong to the boundary, not its tessellation or a text annotation. */
object DesignDimensions {
    const val FOOT = 0.3048
    private fun rectangle(b: DesignBoundary): Boolean {
        if (b.nodes.size != 4 || b.nodes.any { it.bulge != 0.0 }) return false
        val p = b.nodes.map { it.point }
        val u = DesignPoint(p[1].x-p[0].x, p[1].y-p[0].y)
        val v = DesignPoint(p[3].x-p[0].x, p[3].y-p[0].y)
        val scale = max(1.0, hypot(u.x,u.y)*hypot(v.x,v.y))
        return abs(u.x*v.x+u.y*v.y) <= 1e-9*scale &&
            p[2].distanceTo(DesignPoint(p[0].x+u.x+v.x,p[0].y+u.y+v.y)) <= 1e-8
    }
    fun measure(b: DesignBoundary): DesignSize {
        if (rectangle(b)) return DesignSize(b.nodes[0].point.distanceTo(b.nodes[1].point),
            b.nodes[0].point.distanceTo(b.nodes[3].point),true)
        // Include exact circular extrema, not just the vertices or a sampled approximation.
        val points = b.nodes.map { it.point }.toMutableList()
        b.edges().filterNot { it.isLine }.forEach { e ->
            val start = atan2(e.end.y-e.start.point.y,e.end.x-e.start.point.x) -
                e.sweepRadians/2 - if (e.sweepRadians > 0) PI/2 else -PI/2
            for (i in 0..3) {
                val angle = i*PI/2
                fun positive(a: Double) = ((a % (2*PI)) + 2*PI) % (2*PI)
                val distance = if(e.sweepRadians>0) positive(angle-start) else positive(start-angle)
                if (distance <= abs(e.sweepRadians)+1e-12) points.add(e.pointAt((distance/abs(e.sweepRadians)).coerceIn(0.0,1.0)))
            }
        }
        return DesignSize(points.maxOf { it.x }-points.minOf { it.x },points.maxOf { it.y }-points.minOf { it.y },false)
    }
    /** Rectangles retain their first corner and axes. Other shapes scale uniformly about vertex 1.
     * Nonuniformly stretching a circular arc while retaining its bulge would be incorrect. */
    fun resize(b: DesignBoundary, axis: SizeAxis, metres: Double): DesignBoundary {
        require(metres.isFinite() && metres in FOOT/12..1000*FOOT) { "Enter a size from 1 inch to 1000 feet" }
        val size = measure(b)
        val old = if (axis==SizeAxis.LENGTH) size.lengthMetres else size.widthMetres
        require(old>1e-6) { "This outline has no usable span in that direction" }
        if(abs(old-metres)<1e-10) return b
        val origin = b.nodes.first().point
        val ratio = metres/old
        if(!size.rectangular) return DesignBoundary(b.nodes.map { n -> n.copy(point=DesignPoint(
            origin.x+(n.point.x-origin.x)*ratio,origin.y+(n.point.y-origin.y)*ratio)) })
        val u = b.nodes[1].point; val v = b.nodes[3].point
        val sx = if(axis==SizeAxis.LENGTH) ratio else 1.0
        val sy = if(axis==SizeAxis.WIDTH) ratio else 1.0
        val x = DesignPoint(origin.x+(u.x-origin.x)*sx,origin.y+(u.y-origin.y)*sx)
        val y = DesignPoint(origin.x+(v.x-origin.x)*sy,origin.y+(v.y-origin.y)*sy)
        val corners = listOf(origin,x,DesignPoint(x.x+y.x-origin.x,x.y+y.y-origin.y),y)
        return DesignBoundary(b.nodes.mapIndexed { i,n -> n.copy(point=corners[i]) })
    }
}

/** Unmarked values mean feet; inches require a mark. Unsupported input fails, never guesses. */
object FeetInchesInput {
    private fun number(s: String): Double {
        val t=s.trim()
        Regex("^(\\d+(?:\\.\\d+)?|\\.\\d+)$").matchEntire(t)?.let { return t.toDouble() }
        val f=Regex("^(?:(\\d+)\\s+)?(\\d+)/(\\d+)$").matchEntire(t)
            ?: throw IllegalArgumentException("Use feet, or feet and inches such as 30' 6\\\" or 30' 6 1/2\\\"")
        val denominator=f.groupValues[3].toDouble()
        require(denominator>0) { "Fraction denominator cannot be zero" }
        return (f.groupValues[1].toDoubleOrNull()?:0.0)+f.groupValues[2].toDouble()/denominator
    }
    fun parseMetres(value: String): Double {
        val text=value.trim().lowercase(Locale.US).replace('′','\'').replace('″','"')
            .replace(Regex("\\bfeet\\b|\\bfoot\\b|\\bft\\b"),"'")
            .replace(Regex("\\binches\\b|\\binch\\b|\\bin\\b"),"\"")
        require(text.isNotBlank() && text.length<=48) { "Enter a dimension" }
        val feet: Double
        if(text.contains('\'')) {
            val parts=text.split('\''); require(parts.size==2) { "Use one feet mark" }
            val whole=number(parts[0]); var rest=parts[1].trim().removePrefix("-").trim()
            val inches=if(rest.isEmpty()) 0.0 else {
                require(rest.endsWith('"')) { "Mark the inches, for example 30' 6\\\"" }
                rest=rest.dropLast(1).trim(); number(rest)
            }
            require(inches<12) { "Inches after feet must be less than 12" }
            feet=whole+inches/12
        } else feet=if(text.endsWith('"')) number(text.dropLast(1))/12 else number(text)
        require(feet.isFinite() && feet in 1.0/12..1000.0) { "Enter a size from 1 inch to 1000 feet" }
        return feet*DesignDimensions.FOOT
    }
    fun format(metres: Double): String = String.format(Locale.US,"%.4f",metres/DesignDimensions.FOOT).trimEnd('0').trimEnd('.')
}

object GridAssist {
    const val SPACING_METRES = DesignDimensions.FOOT
    fun snapPoint(p: DesignPoint)=DesignPoint(round(p.x/SPACING_METRES)*SPACING_METRES,round(p.y/SPACING_METRES)*SPACING_METRES)
    fun snap(command: DesignCommand): DesignCommand = when(command) {
        is DesignCommand.MoveVertex -> command.copy(point=snapPoint(command.point))
        is DesignCommand.Translate -> { val d=snapPoint(DesignPoint(command.dxMetres,command.dyMetres));command.copy(dxMetres=d.x,dyMetres=d.y) }
        else -> command // Curves and explicit dimensions are not silently quantized.
    }
}
