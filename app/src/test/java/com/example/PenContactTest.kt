package com.example

import android.view.MotionEvent
import com.example.ui.DrawingTool
import com.example.ui.input.PenContact
import com.example.ui.input.routePenPointer
import com.example.ui.input.cancellationForCompose
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PenContactTest {
    @Test fun `temporary selection lasts until pen up and next contact resumes drawing`() {
        val contact = PenContact()
        assertEquals(DrawingTool.SELECT, contact.begin(DrawingTool.PEN, true, false, false))
        // Releasing the button does not begin another contact or switch its latched tool.
        assertEquals(DrawingTool.SELECT, contact.current(DrawingTool.PEN))
        contact.end()
        assertEquals(DrawingTool.PEN, contact.begin(DrawingTool.PEN, false, false, false))
    }
    @Test fun `press during ink does not convert it and applies only on next contact`() {
        val contact = PenContact()
        contact.begin(DrawingTool.PEN, false, false, false)
        assertEquals(DrawingTool.PEN, contact.current(DrawingTool.SELECT))
        contact.end()
        assertEquals(DrawingTool.SELECT, contact.begin(DrawingTool.PEN, true, false, false))
    }
    @Test fun `calibration overrides buttons and eraser and cancellation resets the tool`() {
        val contact = PenContact()
        assertEquals(DrawingTool.MEASURE, contact.begin(DrawingTool.PEN, true, true, true, DrawingTool.ERASER))
        contact.end()
        assertNull(contact.tool)
        assertEquals(DrawingTool.PEN, contact.current(DrawingTool.PEN))
        assertEquals(DrawingTool.ERASER, contact.begin(DrawingTool.PEN, true, false, false, DrawingTool.ERASER))
    }
    private fun event(action: Int, types: IntArray, flags: Int = 0): MotionEvent {
        val properties = types.mapIndexed { index, type -> MotionEvent.PointerProperties().apply { id = index; toolType = type } }.toTypedArray()
        val coords = types.map { MotionEvent.PointerCoords().apply { x = 100f; y = 200f; pressure = 0.6f } }.toTypedArray()
        return MotionEvent.obtain(1, 2, action, types.size, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, flags)
    }
    @Test fun `system rejected pen up cancels contact while rejected finger up leaves the pen intact`() {
        val pen = event(MotionEvent.ACTION_UP, intArrayOf(MotionEvent.TOOL_TYPE_STYLUS), MotionEvent.FLAG_CANCELED)
        val finger = event(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            intArrayOf(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_FINGER), MotionEvent.FLAG_CANCELED)
        try {
            assertTrue(routePenPointer(pen).canceled)
            assertTrue(routePenPointer(finger).ignore)
            assertFalse(routePenPointer(finger).canceled)
        } finally { pen.recycle(); finger.recycle() }
    }
    @Test fun `rejection is preserved before Compose conversion without mutating the original event`() {
        val event = event(MotionEvent.ACTION_UP, intArrayOf(MotionEvent.TOOL_TYPE_STYLUS), MotionEvent.FLAG_CANCELED)
        val cancel = cancellationForCompose(event)
        try {
            assertNotNull(cancel)
            assertEquals(MotionEvent.ACTION_CANCEL, cancel!!.actionMasked)
            assertEquals(MotionEvent.ACTION_UP, event.actionMasked)
        } finally { cancel?.recycle(); event.recycle() }
    }
    @Test fun `finger joining or leaving does not interrupt an existing stylus`() {
        for (action in listOf(MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP)) {
            val event = event(action or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), intArrayOf(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_FINGER))
            try { assertTrue(routePenPointer(event).ignore) } finally { event.recycle() }
        }
    }
    @Test fun `pen is found after a finger and receives its own down move and up`() {
        val types = intArrayOf(MotionEvent.TOOL_TYPE_FINGER, MotionEvent.TOOL_TYPE_STYLUS)
        for ((input, expected) in listOf(MotionEvent.ACTION_POINTER_DOWN to MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE to MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_UP to MotionEvent.ACTION_UP)) {
            val event = event(if (input == MotionEvent.ACTION_MOVE) input else input or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), types)
            try {
                val route = routePenPointer(event)
                assertEquals(1, route.index)
                assertEquals(expected, route.action)
                assertTrue(route.isStylus)
                assertFalse(route.ignore)
            } finally { event.recycle() }
        }
    }
}
