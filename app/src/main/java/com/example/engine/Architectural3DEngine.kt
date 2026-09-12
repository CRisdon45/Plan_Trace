package com.example.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import com.example.model.DimensionMarkup
import com.example.model.DrawingLayer
import com.example.model.EllipseElement
import com.example.model.FreehandPath
import com.example.model.LineElement
import com.example.model.Point2D
import com.example.model.PolylineElement
import com.example.model.RectangleElement
import com.example.model.TraceProject
import com.example.model.VectorElement
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class Point3D(val x: Float, val y: Float, val z: Float)

data class Face3D(
    val vertices: List<Point3D>,
    val baseColor: Int,
    val normal: Point3D,
    val isWater: Boolean = false,
    val isTopFace: Boolean = false
)

enum class Render3DMode {
    SOLID_SHADED,
    ARCHITECTURAL_CLAY,
    WIREFRAME
}

object Architectural3DEngine {

    // Default directional sunlight vector (pointing down and towards camera)
    private val LIGHT_DIR = normalize(Point3D(0.5f, -0.6f, 0.7f))

    /**
     * Determines extrusion height and elevation for a layer based on its name/preset.
     */
    fun getLayer3DSettings(layer: DrawingLayer, heightMultiplier: Float): Pair<Float, Float> {
        val lowerName = layer.name.lowercase()
        val baseElev: Float
        val extrudeHeight: Float

        when {
            lowerName.contains("pool") || lowerName.contains("water") -> {
                baseElev = 0f
                extrudeHeight = 22f * heightMultiplier
            }
            lowerName.contains("deck") || lowerName.contains("hardscape") || lowerName.contains("patio") -> {
                baseElev = 0f
                extrudeHeight = 16f * heightMultiplier
            }
            lowerName.contains("wall") || lowerName.contains("base") || lowerName.contains("structure") -> {
                baseElev = 0f
                extrudeHeight = 70f * heightMultiplier
            }
            lowerName.contains("plant") || lowerName.contains("landscape") || lowerName.contains("garden") -> {
                baseElev = 0f
                extrudeHeight = 35f * heightMultiplier
            }
            lowerName.contains("dimension") || lowerName.contains("note") -> {
                baseElev = 5f
                extrudeHeight = 2f
            }
            else -> {
                baseElev = 0f
                extrudeHeight = 45f * heightMultiplier
            }
        }
        return Pair(baseElev, extrudeHeight)
    }

    /**
     * Generates 3D polygonal faces from all 2D vector elements in the project.
     */
    fun build3DFaces(
        project: TraceProject,
        heightMultiplier: Float = 1.0f
    ): List<Face3D> {
        val faces = mutableListOf<Face3D>()
        val layerMap = project.layers.associateBy { it.id }

        for (el in project.elements) {
            val layer = layerMap[el.layerId] ?: continue
            if (!layer.isVisible) continue

            val (baseElev, extrudeHeight) = getLayer3DSettings(layer, heightMultiplier)
            val isWater = layer.name.lowercase().contains("water") || layer.name.lowercase().contains("pool")
            val rawColor = (if (el.strokeColor != 0L) el.strokeColor else 0xFF1E293BL).toInt()

            when (el) {
                is RectangleElement -> {
                    val x1 = min(el.left, el.right)
                    val x2 = max(el.left, el.right)
                    val y1 = min(el.top, el.bottom)
                    val y2 = max(el.top, el.bottom)
                    val z0 = baseElev
                    val z1 = baseElev + extrudeHeight

                    val p1_bot = Point3D(x1, y1, z0)
                    val p2_bot = Point3D(x2, y1, z0)
                    val p3_bot = Point3D(x2, y2, z0)
                    val p4_bot = Point3D(x1, y2, z0)

                    val p1_top = Point3D(x1, y1, z1)
                    val p2_top = Point3D(x2, y1, z1)
                    val p3_top = Point3D(x2, y2, z1)
                    val p4_top = Point3D(x1, y2, z1)

                    // Top Face
                    faces.add(Face3D(listOf(p1_top, p2_top, p3_top, p4_top), rawColor, Point3D(0f, 0f, 1f), isWater, isTopFace = true))

                    // 4 Side Walls
                    if (extrudeHeight > 1f) {
                        faces.add(Face3D(listOf(p1_bot, p2_bot, p2_top, p1_top), rawColor, Point3D(0f, -1f, 0f)))
                        faces.add(Face3D(listOf(p2_bot, p3_bot, p3_top, p2_top), rawColor, Point3D(1f, 0f, 0f)))
                        faces.add(Face3D(listOf(p3_bot, p4_bot, p4_top, p3_top), rawColor, Point3D(0f, 1f, 0f)))
                        faces.add(Face3D(listOf(p4_bot, p1_bot, p1_top, p4_top), rawColor, Point3D(-1f, 0f, 0f)))
                    }
                }

                is EllipseElement -> {
                    val segments = 24
                    val topVerts = mutableListOf<Point3D>()
                    val botVerts = mutableListOf<Point3D>()
                    val z0 = baseElev
                    val z1 = baseElev + extrudeHeight

                    for (i in 0 until segments) {
                        val angle = (2.0 * PI * i / segments).toFloat()
                        val vx = el.centerX + cos(angle) * el.radiusX
                        val vy = el.centerY + sin(angle) * el.radiusY
                        topVerts.add(Point3D(vx, vy, z1))
                        botVerts.add(Point3D(vx, vy, z0))
                    }

                    // Top Cap
                    faces.add(Face3D(topVerts, rawColor, Point3D(0f, 0f, 1f), isWater, isTopFace = true))

                    // Side Cylindrical Quads
                    if (extrudeHeight > 1f) {
                        for (i in 0 until segments) {
                            val next = (i + 1) % segments
                            val b1 = botVerts[i]
                            val b2 = botVerts[next]
                            val t2 = topVerts[next]
                            val t1 = topVerts[i]
                            val normAngle = (2.0 * PI * (i + 0.5f) / segments).toFloat()
                            val normal = Point3D(cos(normAngle), sin(normAngle), 0f)
                            faces.add(Face3D(listOf(b1, b2, t2, t1), rawColor, normal))
                        }
                    }
                }

                is LineElement -> {
                    // Extrude line into a vertical 3D wall
                    val z0 = baseElev
                    val z1 = baseElev + extrudeHeight
                    val dx = el.end.x - el.start.x
                    val dy = el.end.y - el.start.y
                    val len = hypot(dx, dy)
                    if (len > 2f) {
                        val thickness = (el.strokeWidth * 1.5f).coerceIn(4f, 18f)
                        val nx = (-dy / len) * (thickness / 2f)
                        val ny = (dx / len) * (thickness / 2f)

                        val p1 = Point3D(el.start.x + nx, el.start.y + ny, z0)
                        val p2 = Point3D(el.end.x + nx, el.end.y + ny, z0)
                        val p3 = Point3D(el.end.x - nx, el.end.y - ny, z0)
                        val p4 = Point3D(el.start.x - nx, el.start.y - ny, z0)

                        val t1 = Point3D(p1.x, p1.y, z1)
                        val t2 = Point3D(p2.x, p2.y, z1)
                        val t3 = Point3D(p3.x, p3.y, z1)
                        val t4 = Point3D(p4.x, p4.y, z1)

                        // Top face
                        faces.add(Face3D(listOf(t1, t2, t3, t4), rawColor, Point3D(0f, 0f, 1f), isTopFace = true))
                        // Long sides
                        faces.add(Face3D(listOf(p1, p2, t2, t1), rawColor, Point3D(nx / (thickness / 2f), ny / (thickness / 2f), 0f)))
                        faces.add(Face3D(listOf(p3, p4, t4, t3), rawColor, Point3D(-nx / (thickness / 2f), -ny / (thickness / 2f), 0f)))
                    }
                }

                is PolylineElement -> {
                    val z0 = baseElev
                    val z1 = baseElev + extrudeHeight
                    for (i in 0 until el.points.size - 1) {
                        val pA = el.points[i]
                        val pB = el.points[i + 1]
                        val b1 = Point3D(pA.x, pA.y, z0)
                        val b2 = Point3D(pB.x, pB.y, z0)
                        val t2 = Point3D(pB.x, pB.y, z1)
                        val t1 = Point3D(pA.x, pA.y, z1)
                        faces.add(Face3D(listOf(b1, b2, t2, t1), rawColor, Point3D(0f, 0f, 1f)))
                    }
                }

                is FreehandPath -> {
                    val z0 = baseElev
                    val z1 = baseElev + extrudeHeight
                    for (i in 0 until el.points.size - 1) {
                        val pA = el.points[i]
                        val pB = el.points[i + 1]
                        val b1 = Point3D(pA.x, pA.y, z0)
                        val b2 = Point3D(pB.x, pB.y, z0)
                        val t2 = Point3D(pB.x, pB.y, z1)
                        val t1 = Point3D(pA.x, pA.y, z1)
                        faces.add(Face3D(listOf(b1, b2, t2, t1), rawColor, Point3D(0f, 0f, 1f)))
                    }
                }

                else -> Unit
            }
        }
        return faces
    }

    /**
     * Projects and renders the 3D scene onto an Android Canvas.
     */
    fun render3DScene(
        canvas: Canvas,
        faces: List<Face3D>,
        viewCenter: Point2D,
        screenSize: Offset,
        yawDeg: Float,
        pitchDeg: Float,
        zoom: Float,
        panOffset: Offset,
        mode: Render3DMode = Render3DMode.SOLID_SHADED,
        showGrid: Boolean = true
    ) {
        val radYaw = Math.toRadians(yawDeg.toDouble()).toFloat()
        val radPitch = Math.toRadians(pitchDeg.toDouble()).toFloat()

        val cosY = cos(radYaw)
        val sinY = sin(radYaw)
        val cosP = cos(radPitch)
        val sinP = sin(radPitch)

        val screenMidX = screenSize.x / 2f + panOffset.x
        val screenMidY = screenSize.y / 2f + panOffset.y

        fun projectPoint(p: Point3D): Pair<Offset, Float> {
            val rx = p.x - viewCenter.x
            val ry = p.y - viewCenter.y
            val rz = p.z

            // Yaw around Z
            val x1 = rx * cosY - ry * sinY
            val y1 = rx * sinY + ry * cosY
            val z1 = rz

            // Pitch around X
            val y2 = y1 * cosP - z1 * sinP
            val z2 = y1 * sinP + z1 * cosP

            val sx = screenMidX + x1 * zoom
            val sy = screenMidY + y2 * zoom
            return Pair(Offset(sx, sy), z2)
        }

        // 1. Draw Architectural Ground Grid in 3D
        if (showGrid) {
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x2294A3B8 // Subtle light blue-gray
                strokeWidth = 1.2f
            }
            val gridStep = 80f
            val gridExtent = 600f

            for (gx in -gridExtent.toInt()..gridExtent.toInt() step gridStep.toInt()) {
                val p1 = projectPoint(Point3D(viewCenter.x + gx, viewCenter.y - gridExtent, 0f)).first
                val p2 = projectPoint(Point3D(viewCenter.x + gx, viewCenter.y + gridExtent, 0f)).first
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, gridPaint)
            }
            for (gy in -gridExtent.toInt()..gridExtent.toInt() step gridStep.toInt()) {
                val p1 = projectPoint(Point3D(viewCenter.x - gridExtent, viewCenter.y + gy, 0f)).first
                val p2 = projectPoint(Point3D(viewCenter.x + gridExtent, viewCenter.y + gy, 0f)).first
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, gridPaint)
            }
        }

        // 2. Project and sort faces back-to-front (Painter's Algorithm)
        data class ProjectedFace(
            val face: Face3D,
            val screenPoints: List<Offset>,
            val depth: Float
        )

        val projectedList = faces.mapNotNull { face ->
            if (face.vertices.size < 3) return@mapNotNull null
            val projected = face.vertices.map { projectPoint(it) }
            val avgDepth = projected.map { it.second }.average().toFloat()
            ProjectedFace(face, projected.map { it.first }, avgDepth)
        }.sortedBy { it.depth }

        // 3. Render Projected Faces
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        for (item in projectedList) {
            val face = item.face
            val pts = item.screenPoints

            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) {
                    lineTo(pts[i].x, pts[i].y)
                }
                close()
            }

            when (mode) {
                Render3DMode.WIREFRAME -> {
                    strokePaint.color = 0xFF0F172A.toInt()
                    canvas.drawPath(path, strokePaint)
                }

                Render3DMode.ARCHITECTURAL_CLAY -> {
                    val dot = max(0.2f, dotProduct(face.normal, LIGHT_DIR))
                    val shade = (180 + dot * 70).toInt().coerceIn(0, 255)
                    fillPaint.color = Color.rgb(shade, shade, shade)
                    strokePaint.color = 0x5564748B
                    canvas.drawPath(path, fillPaint)
                    canvas.drawPath(path, strokePaint)
                }

                Render3DMode.SOLID_SHADED -> {
                    val dot = max(0.25f, dotProduct(face.normal, LIGHT_DIR))
                    val base = face.baseColor
                    val r = (Color.red(base) * (0.45f + 0.55f * dot)).toInt().coerceIn(0, 255)
                    val g = (Color.green(base) * (0.45f + 0.55f * dot)).toInt().coerceIn(0, 255)
                    val b = (Color.blue(base) * (0.45f + 0.55f * dot)).toInt().coerceIn(0, 255)

                    if (face.isWater) {
                        fillPaint.color = Color.argb(190, 14, 165, 233) // Translucent cyan pool water
                    } else {
                        fillPaint.color = Color.rgb(r, g, b)
                    }

                    strokePaint.color = if (face.isWater) 0x660284C7 else 0x440F172A
                    canvas.drawPath(path, fillPaint)
                    canvas.drawPath(path, strokePaint)
                }
            }
        }
    }

    private fun dotProduct(a: Point3D, b: Point3D): Float = a.x * b.x + a.y * b.y + a.z * b.z

    private fun normalize(p: Point3D): Point3D {
        val len = sqrt(p.x * p.x + p.y * p.y + p.z * p.z)
        return if (len > 0f) Point3D(p.x / len, p.y / len, p.z / len) else p
    }

    private fun hypot(x: Float, y: Float): Float = sqrt(x * x + y * y)
}
