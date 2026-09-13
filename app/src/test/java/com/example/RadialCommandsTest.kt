package com.example

import com.example.export.DesignAppearance
import com.example.ui.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RadialCommandsTest {
    @Test fun `main directions stay fixed for every category and selection state`() {
        assertEquals(listOf(-90.0,-30.0,30.0,90.0,150.0,210.0),RadialCategory.values().map { RadialGeometry.angle(it) })
        for(c in RadialCategory.values()) for(expanded in RadialCategory.values()) {
            val p=RadialGeometry.point(RadialGeometry.angle(c),70.0)
            assertEquals(RadialHit.Category(c),RadialGeometry.hit(p.first,p.second,expanded))
        }
    }
    @Test fun `outer ring only targets actions in the expanded category`() {
        for(c in RadialCategory.values()) RadialCommands.actions(c).forEachIndexed { i,a ->
            val p=RadialGeometry.point(RadialGeometry.childAngle(c,i),136.0)
            assertEquals(RadialHit.Action(a),RadialGeometry.hit(p.first,p.second,c))
            assertEquals(RadialHit.Gap,RadialGeometry.hit(p.first,p.second,null))
        }
    }
    @Test fun `centre gap and outside do not target destructive actions`() {
        assertEquals(RadialHit.Centre,RadialGeometry.hit(0.0,0.0,RadialCategory.EDIT))
        assertEquals(RadialHit.Gap,RadialGeometry.hit(104.0,0.0,RadialCategory.EDIT))
        assertEquals(RadialHit.Outside,RadialGeometry.hit(200.0,0.0,RadialCategory.EDIT))
        val p=RadialGeometry.point(-60.0,70.0)
        assertEquals(RadialHit.Gap,RadialGeometry.hit(p.first,p.second,null))
    }
    @Test fun `edge placement shifts the origin without rotation or shrinking targets`() {
        assertEquals(166.0 to 166.0,RadialGeometry.centre(1.0,1.0,900.0,500.0))
        assertEquals(734.0 to 334.0,RadialGeometry.centre(899.0,499.0,900.0,500.0))
        assertEquals(400.0 to 250.0,RadialGeometry.centre(400.0,250.0,900.0,500.0))
        assertThrows(IllegalArgumentException::class.java) { RadialGeometry.centre(10.0,10.0,300.0,400.0) }
    }
    @Test fun `selection and history disable commands without moving or removing slots`() {
        val empty=RadialAvailability(true,false,false,false,false,false,false)
        assertFalse(empty.enabled(RadialAction.SIZE));assertFalse(empty.enabled(RadialAction.DELETE))
        assertFalse(empty.enabled(RadialAction.COPING));assertFalse(empty.enabled(RadialAction.UNDO))
        assertTrue(empty.enabled(RadialAction.POOL));assertTrue(empty.enabled(RadialAction.GRID))
        assertEquals(listOf(RadialAction.SIZE,RadialAction.COPING,RadialAction.DELETE,RadialAction.SIDES,RadialAction.SMOOTH,RadialAction.RADIUS),RadialCommands.actions(RadialCategory.EDIT))
        val locked=empty.copy(hasSelection=true,hasCopingTarget=true,hasObjects=true)
        assertFalse(locked.enabled(RadialAction.COPING));assertTrue(locked.enabled(RadialAction.CLEAR))
    }
    @Test fun `smooth actions preserve learned directions and require an editable pool`() {
        assertEquals(listOf(-120.0,-90.0,-60.0), (0..2).map { RadialGeometry.childAngle(RadialCategory.DRAW,it) })
        assertEquals(listOf(-60.0,-30.0,0.0,30.0), (0..3).map { RadialGeometry.childAngle(RadialCategory.EDIT,it) })
        val no=RadialAvailability(true,true,false,true,false,false,true,canEditSmooth=true)
        assertFalse(no.enabled(RadialAction.SMOOTH));assertFalse(no.enabled(RadialAction.RADIUS))
        assertTrue(no.copy(editable=true).enabled(RadialAction.SMOOTH))
    }
    @Test fun `grid display and grid snapping are separate explicit toggle states`() {
        val state=RadialAvailability(true,true,true,true,true,false,true,grid=true,snap=false)
        assertEquals(true,state.checked(RadialAction.GRID));assertEquals(false,state.checked(RadialAction.SNAP))
        assertNull(state.checked(RadialAction.SIZE))
    }
    @Test fun `appearance appends to view without moving learned directions`() {
        assertEquals(listOf(RadialAction.FIT,RadialAction.GRID,RadialAction.SITE,RadialAction.APPEARANCE),
            RadialCommands.actions(RadialCategory.VIEW))
        assertEquals(listOf(15.0,45.0,75.0,105.0),
            (0..3).map { RadialGeometry.childAngle(RadialCategory.VIEW,it) })
        val state=RadialAvailability(true,false,false,false,false,false,false,
            appearance=DesignAppearance.GRAPHIC)
        assertTrue(state.enabled(RadialAction.APPEARANCE))
        assertEquals("Graphic",state.detail(RadialAction.APPEARANCE))
        assertNull(state.checked(RadialAction.APPEARANCE))
    }
}
