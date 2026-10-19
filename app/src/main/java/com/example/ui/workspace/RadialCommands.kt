package com.example.ui.workspace

import com.example.export.DesignAppearance
import kotlin.math.*

/** Stable categories and ordering; disable actions rather than rearranging learned directions. */
enum class RadialCategory(val label: String) { DRAW("Draw"), EDIT("Edit"), VIEW("View"), ASSIST("Assist"), HISTORY("History"), SELECT("Select") }
enum class RadialAction(val label: String) {
    POOL("Pool"), CURVED("Curved"), PAVING("Paving"), SIZE("Size"), COPING("Coping"), DELETE("Delete"),
    FIT("Fit"), GRID("Grid"), APPEARANCE("Appearance"), TOUCH("Touch edit"), SNAP("1 ft snap"), UNDO("Undo"), REDO("Redo"), NEXT("Next"), CLEAR("Clear"), SITE("Site image"), GEOMETRY("Object snap"), SIDES("Move sides"), SMOOTH_POOL("Smooth pool"), SMOOTH("Keep smooth"), RADIUS("Radius")
}
data class RadialAvailability(val hasDocument: Boolean, val hasSelection: Boolean, val editable: Boolean,
    val hasCopingTarget: Boolean, val canUndo: Boolean, val canRedo: Boolean, val hasObjects: Boolean,
    val grid: Boolean=false, val touch: Boolean=false, val snap: Boolean=false, val geometry: Boolean=false, val canEditSides: Boolean=false, val sideEditing: Boolean=false, val canEditSmooth: Boolean=false, val smoothMode: com.example.model.design.SmoothEditMode=com.example.model.design.SmoothEditMode.OFF,
    val appearance: DesignAppearance=DesignAppearance.NORTHSTAR) {
    fun enabled(a: RadialAction)=when(a) {
        RadialAction.SIZE, RadialAction.DELETE -> editable
        RadialAction.COPING -> editable && hasCopingTarget
        RadialAction.SIDES -> editable && canEditSides
        RadialAction.SMOOTH, RadialAction.RADIUS -> editable && canEditSmooth
        RadialAction.UNDO -> canUndo
        RadialAction.REDO -> canRedo
        RadialAction.NEXT -> hasObjects
        RadialAction.CLEAR -> hasSelection
        else -> hasDocument
    }
    fun checked(a: RadialAction): Boolean?=when(a) { RadialAction.GRID->grid;RadialAction.TOUCH->touch;RadialAction.SNAP->snap;RadialAction.GEOMETRY->geometry;RadialAction.SIDES->sideEditing;RadialAction.SMOOTH->smoothMode==com.example.model.design.SmoothEditMode.SHAPE;RadialAction.RADIUS->smoothMode==com.example.model.design.SmoothEditMode.RADIUS;else->null }
    fun detail(a: RadialAction): String?=if(a==RadialAction.APPEARANCE) appearance.workspaceLabel else null
}
object RadialCommands {
    fun actions(c: RadialCategory): List<RadialAction> = when(c) {
        RadialCategory.DRAW -> listOf(RadialAction.POOL,RadialAction.CURVED,RadialAction.PAVING,RadialAction.SMOOTH_POOL)
        RadialCategory.EDIT -> listOf(RadialAction.SIZE,RadialAction.COPING,RadialAction.DELETE,RadialAction.SIDES,RadialAction.SMOOTH,RadialAction.RADIUS)
        RadialCategory.VIEW -> listOf(RadialAction.FIT,RadialAction.GRID,RadialAction.SITE,RadialAction.APPEARANCE)
        RadialCategory.ASSIST -> listOf(RadialAction.TOUCH,RadialAction.SNAP,RadialAction.GEOMETRY)
        RadialCategory.HISTORY -> listOf(RadialAction.UNDO,RadialAction.REDO)
        RadialCategory.SELECT -> listOf(RadialAction.NEXT,RadialAction.CLEAR)
    }
}
sealed interface RadialHit {
    data class Category(val value: RadialCategory): RadialHit
    data class Action(val value: RadialAction): RadialHit
    data object Centre: RadialHit
    data object Outside: RadialHit
    data object Gap: RadialHit
}
/** All distances here are dp. Child fans use the same origin and never reorder their parent wheel. */
object RadialGeometry {
    const val EXTENT=166.0
    fun angle(c: RadialCategory)= -90.0+c.ordinal*60.0
    fun childAngle(c:RadialCategory,index:Int): Double {
        val count=RadialCommands.actions(c).size
        require(index in 0 until count)
        // Append side editing without moving the learned Size/Coping/Delete directions.
        if(c==RadialCategory.EDIT) return angle(c)+listOf(-30.0,0.0,30.0,60.0,-60.0,90.0)[index]
        if(c==RadialCategory.DRAW) return angle(c)+listOf(-30.0,0.0,30.0,60.0)[index]
        // Preserve the existing Fit/Grid/Site directions when adding appearance.
        if(c==RadialCategory.VIEW) return angle(c)+listOf(-15.0,15.0,45.0,75.0)[index]
        if(c==RadialCategory.ASSIST) return angle(c)+listOf(-15.0,15.0,45.0)[index]
        return angle(c)+(index-(count-1)/2.0)*30.0
    }
    fun point(degrees: Double,radius: Double): Pair<Double,Double> {
        val a=degrees*PI/180;return cos(a)*radius to sin(a)*radius
    }
    fun centre(x:Double,y:Double,width:Double,height:Double): Pair<Double,Double> {
        require(width>=2*EXTENT && height>=2*EXTENT) { "Use the compact command list when the wheel cannot fit" }
        return x.coerceIn(EXTENT,width-EXTENT) to y.coerceIn(EXTENT,height-EXTENT)
    }
    private fun difference(a:Double,b:Double)=abs(((a-b+540)%360)-180)
    fun hit(x:Double,y:Double,expanded:RadialCategory?): RadialHit {
        val radius=hypot(x,y);val a=(atan2(y,x)*180/PI+360)%360
        if(radius<=34) return RadialHit.Centre
        if(radius in 40.0..98.0) {
            val c=RadialCategory.values().minBy { difference(a,(angle(it)+360)%360) }
            return if(difference(a,(angle(c)+360)%360)<=28) RadialHit.Category(c) else RadialHit.Gap
        }
        if(expanded!=null && radius in 110.0..162.0) {
            RadialCommands.actions(expanded).forEachIndexed { i, action ->
                if(difference(a,(childAngle(expanded,i)+360)%360)<=14) return RadialHit.Action(action)
            }
        }
        return if(radius>EXTENT) RadialHit.Outside else RadialHit.Gap
    }
}
