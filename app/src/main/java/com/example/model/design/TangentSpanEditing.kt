package com.example.model.design

import kotlin.math.*

/**
 * Original Kotlin implementation of endpoint/tangent biarc mathematics described by
 * Ryan Juckett, Biarc Interpolation (2014): https://www.ryanjuckett.com/biarc-interpolation/
 * No upstream source code copied. Positive-distance, two-minor-arc branch only.
 * A construction helper, not a persisted second geometry model or topology validator.
 */
object TangentBiarc {
    const val ANGLE_TOLERANCE = 1e-7
    data class Join(val point: DesignPoint, val firstBulge: Double, val secondBulge: Double)

    fun connect(p: DesignPoint, tangentP: DesignPoint, q: DesignPoint, tangentQ: DesignPoint,
                balance: Double = 1.0): Join {
        require(balance.isFinite() && balance in 1e-4..1e4) { "Unsupported biarc balance" }
        val t = unit(tangentP); val u = unit(tangentQ)
        val length = p.distanceTo(q)
        require(length.isFinite() && length in 1e-5..1000.0) { "Unsupported tangent span length" }
        // Solve in a translated, unit-chord frame, not squared world coordinates.
        val v = DesignPoint((q.x-p.x)/length, (q.y-p.y)/length)
        val a = 2*balance*max(0.0, 1-dot(t,u))
        val b = 2*(dot(v,t)+balance*dot(v,u))
        val m = if (a < 1e-14) {
            if (abs(b) < 1e-14) {
                require(abs(balance-1.0) < 1e-12) { "Perpendicular equal-tangent case requires symmetric balance" }
                DesignPoint(v.x/2, v.y/2)
            } else {
                require(b > 0) { "Backward equal-tangent branch is unsupported" }
                val d = 1/b
                DesignPoint((v.x+balance*d*(t.x-u.x))/(1+balance),
                    (v.y+balance*d*(t.y-u.y))/(1+balance))
            }
        } else {
            val root = sqrt(b*b+4*a)
            val d = if (b >= 0) 2/(b+root) else (root-b)/(2*a)
            DesignPoint((v.x+balance*d*(t.x-u.x))/(1+balance),
                (v.y+balance*d*(t.y-u.y))/(1+balance))
        }
        return checked(p,t,q,u,DesignPoint(p.x+length*m.x,p.y+length*m.y))
    }

    internal fun checked(p: DesignPoint, t: DesignPoint, q: DesignPoint, u: DesignPoint,
                         middle: DesignPoint): Join {
        val b0 = bulge(p,t,middle)
        val backward = DesignPoint(-u.x,-u.y)
        val b1 = -bulge(q,backward,middle)
        val first = BoundaryEdge(BoundaryNode("first",p,"first",b0),middle)
        val second = BoundaryEdge(BoundaryNode("second",middle,"second",b1),q)
        require(angle(first.tangentAt(0.0),t) <= ANGLE_TOLERANCE &&
            angle(second.tangentAt(1.0),u) <= ANGLE_TOLERANCE &&
            angle(first.tangentAt(1.0),second.tangentAt(0.0)) <= ANGLE_TOLERANCE) {
            "The requested span cannot preserve directed tangency"
        }
        return Join(middle,b0,b1)
    }

    private fun bulge(p: DesignPoint, t: DesignPoint, q: DesignPoint): Double {
        val dx=q.x-p.x; val dy=q.y-p.y
        require(hypot(dx,dy) >= 1e-6) { "Tangent edit collapses an edge" }
        val halfSweep=atan2(t.x*dy-t.y*dx,t.x*dx+t.y*dy)
        require(abs(halfSweep) <= PI/2+1e-12) { "This edit needs a major arc or a deliberate segment split" }
        val b=tan(halfSweep/2).coerceIn(-1.0,1.0)
        // Remove only trigonometric round-off for an exactly straight direction. Do not
        // silently flatten shallow curves unsupported by the existing canonical model.
        if(abs(b)<1e-14) return 0.0
        require(abs(b)>=1e-8) { "Curve is below the current boundary model's supported bulge" }
        return b
    }

    internal fun unit(v: DesignPoint): DesignPoint {
        val n=hypot(v.x,v.y)
        require(n.isFinite() && n>1e-12) { "A finite nonzero tangent is required" }
        return DesignPoint(v.x/n,v.y/n)
    }
    internal fun dot(a:DesignPoint,b:DesignPoint)=a.x*b.x+a.y*b.y
    internal fun angle(a:DesignPoint,b:DesignPoint)=abs(atan2(a.x*b.y-a.y*b.x,dot(a,b)))
}

/**
 * Bounded fixed-topology edits of existing pairs of minor arcs. All IDs and unrelated
 * edge geometry survive. Pairing is edit scope, not stored pool structure. Whole-pool
 * acceptance belongs to DesignCommands / ProjectDesign, including the existing coping.
 */
object TangentSpanEditing {
    private fun index(boundary:DesignBoundary,vertexId:String):Int =
        boundary.nodes.indexOfFirst { it.vertexId==vertexId }.also { require(it>=0) { "Unknown tangent anchor" } }
    private fun at(i:Int,n:Int) = (i%n+n)%n

    private fun requireSmooth(boundary:DesignBoundary,i:Int) {
        require(boundary.joinDeflectionRadians(boundary.nodes[at(i,boundary.nodes.size)].vertexId)
            <= TangentBiarc.ANGLE_TOLERANCE) { "Selected span is not already tangent-continuous" }
    }

    private fun balance(first:BoundaryEdge,second:BoundaryEdge):Double {
        val c0=cos(first.sweepRadians/2);val c1=cos(second.sweepRadians/2)
        require(c0>1e-7 && c1>1e-7) { "A semicircular span requires a different explicit edit scope" }
        // d = chord / (2 cos(sweep/2)); retaining d2/d1 avoids an initial shape jump.
        val r=second.chordMetres*c0/(first.chordMetres*c1)
        require(r.isFinite() && r in 1e-4..1e4) { "This span is too imbalanced for a local edit" }
        return r
    }

    fun moveAnchor(boundary:DesignBoundary,vertexId:String,target:DesignPoint):DesignBoundary {
        val n=boundary.nodes.size
        require(n>=6) { "Local tangent anchor editing needs a four-edge span and an unchanged remainder" }
        val i=index(boundary,vertexId)
        if(target==boundary.nodes[i].point) return boundary
        for(j in i-1..i+1) requireSmooth(boundary,j)
        val edges=boundary.edges()
        val l0=edges[at(i-2,n)];val l1=edges[at(i-1,n)]
        val r0=edges[i];val r1=edges[at(i+1,n)]
        val t=edges[i].tangentAt(0.0)
        val left=TangentBiarc.connect(l0.start.point,l0.tangentAt(0.0),target,t,balance(l0,l1))
        val right=TangentBiarc.connect(target,t,r1.end,r1.tangentAt(1.0),balance(r0,r1))
        val nodes=boundary.nodes.toMutableList()
        val a=at(i-2,n);val b=at(i-1,n);val d=at(i+1,n)
        nodes[a]=nodes[a].copy(bulge=left.firstBulge)
        nodes[b]=nodes[b].copy(point=left.point,bulge=left.secondBulge)
        nodes[i]=nodes[i].copy(point=target,bulge=right.firstBulge)
        nodes[d]=nodes[d].copy(point=right.point,bulge=right.secondBulge)
        val result=DesignBoundary(nodes)
        for(j in i-1..i+1) requireSmooth(result,j)
        return result
    }

    fun setFirstRadius(boundary:DesignBoundary,edgeId:String,radiusMetres:Double):DesignBoundary {
        require(radiusMetres.isFinite() && radiusMetres in 1e-4..1e6) { "Unsupported radius" }
        val n=boundary.nodes.size
        require(n>=4) { "Radius editing needs an unchanged remainder" }
        val edges=boundary.edges();val i=edges.indexOfFirst { it.id==edgeId }
        require(i>=0) { "Unknown radius edge" }
        val j=at(i+1,n);val first=edges[i];val second=edges[j]
        require(!first.isLine && !second.isLine) { "This radius operation needs two existing circular arcs" }
        requireSmooth(boundary,j)
        if(radiusMetres==first.radiusMetres) return boundary
        val p=first.start.point;val q=second.end;val t=first.tangentAt(0.0);val u=second.tangentAt(1.0)
        val length=p.distanceTo(q)
        require(length in 1e-5..1000.0) { "Unsupported radius span length" }
        val s0=sign(first.sweepRadians)*radiusMetres/length
        val n0=DesignPoint(-t.y,t.x);val n1=DesignPoint(-u.y,u.x)
        val c0=DesignPoint(s0*n0.x,s0*n0.y)
        val v=DesignPoint((q.x-p.x)/length-c0.x,(q.y-p.y)/length-c0.y)
        // Circle centres satisfy |C1-C0|^2=(s1-s0)^2 for a directed tangent join.
        // Solve the remaining signed radius with the first radius fixed.
        val denominator=2*(TangentBiarc.dot(v,n1)+s0)
        require(abs(denominator)>1e-12*max(1.0,abs(s0))) { "Fixed endpoints and tangents do not determine this radius edit" }
        val s1=(s0*s0-TangentBiarc.dot(v,v))/denominator
        require(s1.isFinite() && sign(s1)==sign(second.sweepRadians) && abs(s1)>1e-10) {
            "Requested radius would collapse or reverse the adjacent arc"
        }
        require(abs(s0-s1)>1e-12*max(1.0,max(abs(s0),abs(s1)))) { "Coincident-circle radius edit is underdetermined" }
        val c1=DesignPoint((q.x-p.x)/length+s1*n1.x,(q.y-p.y)/length+s1*n1.y)
        val middle=DesignPoint(p.x+length*(s0*c1.x-s1*c0.x)/(s0-s1),
            p.y+length*(s0*c1.y-s1*c0.y)/(s0-s1))
        val join=TangentBiarc.checked(p,t,q,u,middle)
        require(sign(join.firstBulge)==sign(first.start.bulge) && sign(join.secondBulge)==sign(second.start.bulge)) {
            "Requested radius changes the selected arc branch"
        }
        val nodes=boundary.nodes.toMutableList()
        nodes[i]=nodes[i].copy(bulge=join.firstBulge)
        nodes[j]=nodes[j].copy(point=join.point,bulge=join.secondBulge)
        val result=DesignBoundary(nodes)
        require(abs(result.edges()[i].radiusMetres!!-radiusMetres)<=max(1e-8,radiusMetres*1e-8)) {
            "Radius result is numerically uncertain"
        }
        return result
    }
}
