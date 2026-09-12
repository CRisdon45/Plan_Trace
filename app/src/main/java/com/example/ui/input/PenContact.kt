package com.example.ui.input

import android.view.MotionEvent
import com.example.ui.DrawingTool

/** A contact keeps its starting tool until pen-up, regardless of later button changes. */
class PenContact {
    var tool: DrawingTool? = null
        private set

    fun begin(base: DrawingTool, barrelHeld: Boolean, hardwareEraser: Boolean,
        calibrating: Boolean, barrelTool: DrawingTool = DrawingTool.SELECT): DrawingTool {
        tool = when {
            calibrating -> DrawingTool.MEASURE
            hardwareEraser -> DrawingTool.ERASER
            barrelHeld -> if (barrelTool == DrawingTool.ERASER) DrawingTool.ERASER else DrawingTool.SELECT
            else -> base
        }
        return tool!!
    }
    fun current(base: DrawingTool) = tool ?: base
    fun end() { tool = null }
}

data class PenPointer(val index: Int, val action: Int, val isStylus: Boolean, val ignore: Boolean, val canceled: Boolean = false)

/** Other fingers joining/leaving must not end, convert or navigate an active pen contact. */
fun routePenPointer(event: MotionEvent): PenPointer {
    var index = 0
    var stylus = false
    for (i in 0 until event.pointerCount) {
        if (event.getToolType(i) == MotionEvent.TOOL_TYPE_STYLUS || event.getToolType(i) == MotionEvent.TOOL_TYPE_ERASER) {
            index = i
            stylus = true
            break
        }
    }
    var action = event.actionMasked
    val boundary = action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP
    if (stylus && boundary) {
        if (event.actionIndex != index) return PenPointer(index, action, true, true)
        action = if (action == MotionEvent.ACTION_POINTER_DOWN) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_UP
    }
    return PenPointer(index, action, stylus, false, action == MotionEvent.ACTION_CANCEL ||
        (action == MotionEvent.ACTION_UP && event.flags and MotionEvent.FLAG_CANCELED != 0))
}

/** Compose may reconstruct UP without FLAG_CANCELED; preserve rejection as a cancellation event. */
fun cancellationForCompose(event: MotionEvent): MotionEvent? {
    if (event.flags and MotionEvent.FLAG_CANCELED == 0 || !routePenPointer(event).canceled) return null
    return MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
}
