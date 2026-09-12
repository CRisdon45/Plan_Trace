package com.example.model

import kotlin.math.abs

enum class RectangleCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

fun RectangleElement.withExactSize(width: Float, height: Float, scale: ScaleCalibration): RectangleElement {
    require(width.isFinite() && height.isFinite() && width > 0 && height > 0)
    val factor = if (scale.isCalibrated) scale.pixelsPerUnit else 1f
    require(factor.isFinite() && factor > 0 && (width * factor).isFinite() && (height * factor).isFinite())
    val bounds = boundingBox()
    return copy(left = bounds.left, top = bounds.top, right = bounds.left + width * factor, bottom = bounds.top + height * factor)
}

/** Keeps the opposite corner fixed; pointer offset prevents a jump when grabbing the padded handle. */
data class RectangleResize(val original: RectangleElement, val corner: RectangleCorner, val pointerStart: Point2D) {
    fun at(pointer: Point2D): RectangleElement {
        val bounds = original.boundingBox()
        val dx = pointer.x - pointerStart.x
        val dy = pointer.y - pointerStart.y
        val leftHandle = corner == RectangleCorner.TOP_LEFT || corner == RectangleCorner.BOTTOM_LEFT
        val topHandle = corner == RectangleCorner.TOP_LEFT || corner == RectangleCorner.TOP_RIGHT
        return original.copy(
            left = if (leftHandle) (bounds.left + dx).coerceAtMost(bounds.right - 1f) else bounds.left,
            right = if (!leftHandle) (bounds.right + dx).coerceAtLeast(bounds.left + 1f) else bounds.right,
            top = if (topHandle) (bounds.top + dy).coerceAtMost(bounds.bottom - 1f) else bounds.top,
            bottom = if (!topHandle) (bounds.bottom + dy).coerceAtLeast(bounds.top + 1f) else bounds.bottom)
    }

    companion object {
        fun hit(element: RectangleElement, pointer: Point2D, zoom: Float): RectangleResize? {
            val b = element.boundingBox()
            val tolerance = maxOf(10f, 20f / zoom)
            val handles = listOf(
                Triple(RectangleCorner.TOP_LEFT, b.left - 8f, b.top - 8f),
                Triple(RectangleCorner.TOP_RIGHT, b.right + 8f, b.top - 8f),
                Triple(RectangleCorner.BOTTOM_LEFT, b.left - 8f, b.bottom + 8f),
                Triple(RectangleCorner.BOTTOM_RIGHT, b.right + 8f, b.bottom + 8f))
            val hit = handles.firstOrNull { abs(pointer.x - it.second) <= tolerance && abs(pointer.y - it.third) <= tolerance }
            return hit?.let { RectangleResize(element, it.first, pointer) }
        }
    }
}
