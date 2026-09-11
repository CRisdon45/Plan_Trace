package com.example

import com.example.model.design.*
import kotlin.math.*
import java.util.Random

/** Original synthetic cases. Not derived from REF-FF-01 or any client drawing. */
object TangentProofCases {
    fun fixture(notch:Double=0.0, count:Int=8, balance:Double=1.0):DesignBoundary {
        val points=(0 until count).map { i ->
            val a=2*PI*i/count;DesignPoint(6+4*(cos(a)+notch*cos(2*a)),4+2.5*sin(a)) }
        val tangents=(0 until count).map { i ->
            val a=2*PI*i/count;DesignPoint(-4*(sin(a)+2*notch*sin(2*a)),2.5*cos(a)) }
        return DesignBoundary(points.indices.flatMap { i ->
            val j=(i+1)%count
            val pair=TangentBiarc.connect(points[i],tangents[i],points[j],tangents[j],balance)
            listOf(BoundaryNode("v${2*i}",points[i],"e${2*i}",pair.firstBulge),
                BoundaryNode("v${2*i+1}",pair.point,"e${2*i+1}",pair.secondBulge))
        })
    }
    fun near(a:Double,b:Double,tolerance:Double=1e-8) { check(abs(a-b)<=tolerance) { "$a != $b (tolerance $tolerance)" } }
    fun smooth(b:DesignBoundary) { b.nodes.forEach { near(0.0,b.joinDeflectionRadians(it.vertexId)) } }
    fun rejects(block:()->Unit) { var rejected=false;try { block() } catch(e:IllegalArgumentException) { rejected=true };check(rejected) { "Expected explicit rejection" } }
    fun sameEdge(a:BoundaryEdge,b:BoundaryEdge)=a.id==b.id && a.start.point==b.start.point && a.end==b.end && a.start.bulge==b.start.bulge

    fun seededBiarcs():Double {
        val random=Random(20260911L);var maxError=0.0
        repeat(5000) {
            val origin=DesignPoint(random.nextDouble()*40-20,random.nextDouble()*40-20)
            val heading=random.nextDouble()*2*PI-PI;val length=.1+random.nextDouble()*19.9
            val q=DesignPoint(origin.x+length*cos(heading),origin.y+length*sin(heading))
            val alpha=heading+(random.nextDouble()*2-1);val beta=heading+(random.nextDouble()*2-1)
            val t=DesignPoint(cos(alpha),sin(alpha));val u=DesignPoint(cos(beta),sin(beta))
            val pair=TangentBiarc.connect(origin,t,q,u,.5+random.nextDouble()*1.5)
            val first=BoundaryEdge(BoundaryNode("a",origin,"a",pair.firstBulge),pair.point)
            val second=BoundaryEdge(BoundaryNode("b",pair.point,"b",pair.secondBulge),q)
            for(error in listOf(TangentBiarc.angle(t,first.tangentAt(0.0)),TangentBiarc.angle(u,second.tangentAt(1.0)),
                TangentBiarc.angle(first.tangentAt(1.0),second.tangentAt(0.0)))) { maxError=max(maxError,error);near(0.0,error) }
            // Check points against the actual canonical circle, not a second arc class.
            for(e in listOf(first,second)) if(!e.isLine) {
                for(f in listOf(.2,.5,.8)) near(e.radiusMetres!!,e.pointAt(f).distanceTo(e.centre!!),1e-7)
            }
        }
        return maxError
    }
    fun specialCases() {
        val p=DesignPoint(0.0,0.0);val q=DesignPoint(4.0,0.0)
        for((t,u) in listOf(DesignPoint(1.0,0.0) to DesignPoint(1.0,0.0),
            DesignPoint(0.0,1.0) to DesignPoint(0.0,1.0),DesignPoint(0.0,1.0) to DesignPoint(0.0,-1.0))) {
            val s=TangentBiarc.connect(p,t,q,u);near(2.0,s.point.x)
        }
        rejects { TangentBiarc.connect(p,DesignPoint(0.0,0.0),q,DesignPoint(1.0,0.0)) }
        rejects { TangentBiarc.connect(p,DesignPoint(1.0,0.0),p,DesignPoint(1.0,0.0)) }
        rejects { TangentBiarc.connect(p,DesignPoint(-1.0,0.0),q,DesignPoint(-1.0,0.0)) }
        rejects { TangentBiarc.connect(p,DesignPoint(1.0,0.0),q,DesignPoint(1.0,0.0),Double.NaN) }
        rejects { TangentBiarc.connect(p,DesignPoint(1.0,0.0),q,DesignPoint(1.0,0.0),0.0) }
    }
    fun families() { for(q in listOf(0.0,.15,.30,.36)) for(n in listOf(4,6,8,10)) smooth(fixture(q,n)) }
    fun localAnchor() {
        for(q in listOf(0.0,.3)) {
            val b=fixture(q);val p=b.nodes[4].point
            val edited=TangentSpanEditing.moveAnchor(b,"v4",p.translated(.35,.4))
            smooth(edited);check(edited.nodes[4].point==p.translated(.35,.4))
            check(b.edges().zip(edited.edges()).count { (a,c)->sameEdge(a,c) }==12)
            check(b.nodes.map { it.vertexId to it.edgeId }==edited.nodes.map { it.vertexId to it.edgeId })
            near(0.0,TangentBiarc.angle(b.edges()[2].tangentAt(0.0),edited.edges()[2].tangentAt(0.0)))
            near(0.0,TangentBiarc.angle(b.edges()[5].tangentAt(1.0),edited.edges()[5].tangentAt(1.0)))
        }
    }
    fun wraparoundAnchor() {
        val b=fixture(.3);val next=TangentSpanEditing.moveAnchor(b,"v0",b.nodes[0].point.translated(.1,.15))
        smooth(next);check(b.edges().zip(next.edges()).count { (a,c)->sameEdge(a,c) }==12)
    }
    fun noInitialJump() {
        val b=fixture(.3,balance=1.6)
        check(b===TangentSpanEditing.moveAnchor(b,"v4",b.nodes[4].point))
        val next=TangentSpanEditing.moveAnchor(b,"v4",b.nodes[4].point.translated(1e-7,1e-7))
        check(b.nodes.zip(next.nodes).maxOf { (a,c)->a.point.distanceTo(c.point) }<1e-6)
        smooth(next)
    }
    fun radius() {
        for(q in listOf(0.0,.3)) {
            val b=fixture(q);val radius=b.edges()[0].radiusMetres!!*.92
            val next=TangentSpanEditing.setFirstRadius(b,"e0",radius)
            smooth(next);near(radius,next.edges()[0].radiusMetres!!)
            check(b.edges().zip(next.edges()).count { (a,c)->sameEdge(a,c) }==14)
            check(b.nodes[0].point==next.nodes[0].point && b.nodes[2].point==next.nodes[2].point)
            near(0.0,TangentBiarc.angle(b.edges()[0].tangentAt(0.0),next.edges()[0].tangentAt(0.0)))
            near(0.0,TangentBiarc.angle(b.edges()[1].tangentAt(1.0),next.edges()[1].tangentAt(1.0)))
            check(b===TangentSpanEditing.setFirstRadius(b,"e0",b.edges()[0].radiusMetres!!))
        }
    }
    fun rejection() {
        val b=fixture();val original=b.nodes.toList()
        rejects { TangentSpanEditing.moveAnchor(b,"missing",DesignPoint(1.0,2.0)) }
        rejects { TangentSpanEditing.moveAnchor(b,"v4",b.nodes[2].point) }
        rejects { TangentSpanEditing.moveAnchor(b.changedBulge("e3",.7),"v4",DesignPoint(1.0,2.0)) }
        rejects { TangentSpanEditing.setFirstRadius(b,"e0",-1.0) }
        rejects { TangentSpanEditing.setFirstRadius(b,"e0",Double.NaN) }
        rejects { TangentSpanEditing.setFirstRadius(b,"missing",2.0) }
        check(b.nodes==original)
    }
    fun translation() {
        val b=fixture(.3);val p=b.nodes[4].point.translated(.2,.3)
        val first=TangentSpanEditing.moveAnchor(b,"v4",p)
        val next=TangentSpanEditing.moveAnchor(b.translated(1e6,-1e6),"v4",p.translated(1e6,-1e6)).translated(-1e6,1e6)
        for((a,c) in first.nodes.zip(next.nodes)) { near(a.point.x,c.point.x,1e-7);near(a.point.y,c.point.y,1e-7);near(a.bulge,c.bulge,1e-7) }
        near(first.signedAreaSquareMetres,next.signedAreaSquareMetres,1e-7)
    }
    fun reversed() {
        val b=fixture(.3);val p=b.nodes[4].point.translated(.2,.3)
        val first=TangentSpanEditing.moveAnchor(b,"v4",p)
        val next=TangentSpanEditing.moveAnchor(b.reversed(),"v4",p).reversed()
        for((a,c) in first.nodes.zip(next.nodes)) { near(a.point.x,c.point.x);near(a.point.y,c.point.y);near(a.bulge,c.bulge) }
    }
    fun independentArea() {
        val b=TangentSpanEditing.setFirstRadius(fixture(.3),"e0",fixture(.3).edges()[0].radiusMetres!!*.92)
        val points=b.sample(1e-6)
        val sum=points.indices.sumOf { i -> val p=points[i];val q=points[(i+1)%points.size];(p.x*q.y-p.y*q.x)/2 }
        near(b.signedAreaSquareMetres,sum,b.perimeterMetres*2e-6)
    }
    fun runAll():Double {
        val error=seededBiarcs();specialCases();families();localAnchor();wraparoundAnchor();noInitialJump();radius();rejection();translation();reversed();independentArea()
        return error
    }
}
