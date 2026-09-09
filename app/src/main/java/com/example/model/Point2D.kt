package com.example.model

import kotlin.math.hypot

data class Point2D(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f,
    val tilt: Float = 0f
) {
    fun distanceTo(other: Point2D): Float = hypot(x - other.x, y - other.y)

    operator fun plus(other: Point2D): Point2D = Point2D(x + other.x, y + other.y, pressure, tilt)
    operator fun minus(other: Point2D): Point2D = Point2D(x - other.x, y - other.y, pressure, tilt)
    operator fun times(factor: Float): Point2D = Point2D(x * factor, y * factor, pressure, tilt)
}
