package com.example.engine

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.example.model.DimensionMarkup
import com.example.model.EllipseElement
import com.example.model.FreehandPath
import com.example.model.LineElement
import com.example.model.Point2D
import com.example.model.PolylineElement
import com.example.model.RectangleElement
import com.example.model.ScaleCalibration
import com.example.model.StrokeStyle
import com.example.model.TextElement
import com.example.model.VectorElement
import com.example.model.supportsSurface
import com.example.model.withMaterial
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object WatercolorRenderer {

    /**
     * Renders a vector element with authentic architectural hand-drafted linework
     * and rich dynamic watercolor washes that pool along contour edges.
     */
    fun render(
        canvas: Canvas,
        element: VectorElement,
        layerAlpha: Float,
        scale: ScaleCalibration,
        showDimensions: Boolean = true,
        isSelected: Boolean = false
    ) {
        val material = element.material
        if (material != null && element.supportsSurface()) {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (material == com.example.model.SurfaceMaterial.WATER &&
                    element.style == StrokeStyle.WATERCOLOR_WASH) Color.rgb(105, 189, 207)
                    else material.fill.toInt()
                alpha = (element.alpha * layerAlpha * 255).toInt().coerceIn(0, 255)
                style = Paint.Style.FILL
            }
            val outline = when (element) {
                is RectangleElement -> {
                    canvas.drawRect(element.boundingBox(), fillPaint)
                    if (element.style == StrokeStyle.WATERCOLOR_WASH) {
                        drawNorthstarSurfaceCue(canvas, element.id, surfaceGeometryFingerprint(element), rectPath(element.boundingBox()), element.boundingBox(), material, element.alpha * layerAlpha)
                    }
                    element.copy(isFilled = false, material = null, strokeColor = material.outline, style = StrokeStyle.INK)
                }
                is EllipseElement -> {
                    canvas.drawOval(element.boundingBox(), fillPaint)
                    if (element.style == StrokeStyle.WATERCOLOR_WASH) {
                        drawNorthstarSurfaceCue(canvas, element.id, surfaceGeometryFingerprint(element), ovalPath(element.boundingBox()), element.boundingBox(), material, element.alpha * layerAlpha)
                    }
                    element.copy(isFilled = false, material = null, strokeColor = material.outline, style = StrokeStyle.INK)
                }
                is FreehandPath -> {
                    val path = surfacePath(element.points)
                    canvas.drawPath(path, fillPaint)
                    if (element.style == StrokeStyle.WATERCOLOR_WASH) {
                        drawNorthstarSurfaceCue(canvas, element.id, surfaceGeometryFingerprint(element), path, element.boundingBox(), material, element.alpha * layerAlpha)
                    }
                    PolylineElement(id = element.id, layerId = element.layerId, points = element.points, isClosed = true, strokeColor = material.outline, strokeWidth = element.strokeWidth, alpha = element.alpha)
                }
                is PolylineElement -> {
                    val path = surfacePath(element.points)
                    canvas.drawPath(path, fillPaint)
                    if (element.style == StrokeStyle.WATERCOLOR_WASH) {
                        drawNorthstarSurfaceCue(canvas, element.id, surfaceGeometryFingerprint(element), path, element.boundingBox(), material, element.alpha * layerAlpha)
                    }
                    element.copy(fillColor = null, material = null, strokeColor = material.outline, style = StrokeStyle.INK)
                }
                else -> element.withMaterial(null)
            }
            render(canvas, outline, layerAlpha, scale, showDimensions, isSelected)
            return
        }
        val totalAlpha = (element.alpha * layerAlpha * 255).toInt().coerceIn(0, 255)
        val baseColor = element.strokeColor.toInt()
        val colorWithAlpha = Color.argb(
            (Color.alpha(baseColor) * (totalAlpha / 255f)).toInt(),
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor)
        )

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorWithAlpha
            strokeWidth = element.strokeWidth
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND

            when (element.style) {
                StrokeStyle.CONSTRUCTION -> {
                    pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
                }
                StrokeStyle.WATERCOLOR_WASH -> {
                    strokeWidth = element.strokeWidth * 2.8f
                    alpha = (totalAlpha * 0.45f).toInt()
                }
                StrokeStyle.HIGHLIGHTER -> {
                    strokeWidth = element.strokeWidth * 4.5f
                    alpha = (totalAlpha * 0.35f).toInt()
                }
                StrokeStyle.INK -> Unit
            }
        }

        when (element) {
            is FreehandPath -> {
                if (element.points.size > 1) {
                    val path = Path()
                    path.moveTo(element.points[0].x, element.points[0].y)
                    for (i in 1 until element.points.size) {
                        path.lineTo(element.points[i].x, element.points[i].y)
                    }

                    // Dynamic Watercolor Infill if closed or filled
                    if (element.isClosed && element.fillColor != null) {
                        path.close()
                        drawWatercolorPathFill(canvas, path, element.boundingBox(), element.fillColor, totalAlpha)
                    }

                    val hasVaryingPressure = element.points.any { it.pressure != 1.0f }
                    if (hasVaryingPressure && element.points.size > 2) {
                        for (i in 0 until element.points.size - 1) {
                            val p1 = element.points[i]
                            val p2 = element.points[i + 1]
                            val segPressure = (p1.pressure + p2.pressure) / 2f
                            val segPaint = Paint(strokePaint).apply {
                                strokeWidth = element.strokeWidth * (0.35f + 1.3f * segPressure)
                            }
                            canvas.drawLine(p1.x, p1.y, p2.x, p2.y, segPaint)
                        }
                    } else {
                        canvas.drawPath(path, strokePaint)
                    }
                }
            }

            is LineElement -> {
                // Architectural hand-drafting overshoot tick
                drawLineWithArchitecturalOvershoot(
                    canvas, element.start, element.end, strokePaint, element.style
                )

                // Render dynamic dimension readout if scale is calibrated
                if (showDimensions && scale.isCalibrated && element.showDimension) {
                    val label = element.getMeasurementLabel(scale)
                    if (label != null) {
                        drawDimensionBadge(canvas, element.start, element.end, label)
                    }
                }
            }

            is PolylineElement -> {
                if (element.points.size > 1) {
                    val path = Path()
                    path.moveTo(element.points[0].x, element.points[0].y)
                    for (i in 1 until element.points.size) {
                        path.lineTo(element.points[i].x, element.points[i].y)
                    }
                    if (element.isClosed) {
                        path.close()
                        if (element.fillColor != null) {
                            drawWatercolorPathFill(canvas, path, element.boundingBox(), element.fillColor, totalAlpha)
                        }
                    }
                    canvas.drawPath(path, strokePaint)
                }
            }

            is RectangleElement -> {
                val rect = RectF(
                    min(element.left, element.right),
                    min(element.top, element.bottom),
                    max(element.left, element.right),
                    max(element.top, element.bottom)
                )

                // Dynamic Watercolor Infill
                if (element.isFilled) {
                    drawWatercolorRectFill(canvas, rect, element.fillColor, totalAlpha)
                }

                // Architectural corner overshoots for rectangles
                drawRectWithArchitecturalOvershoots(canvas, rect, strokePaint, element.style)

                // Show dimension badge if calibrated
                if (showDimensions && scale.isCalibrated) {
                    val label = element.getMeasurementLabel(scale)
                    if (label != null) {
                        drawCenterBadge(canvas, Point2D(rect.centerX(), rect.centerY()), label)
                    }
                }
            }

            is EllipseElement -> {
                val rect = RectF(
                    element.centerX - element.radiusX,
                    element.centerY - element.radiusY,
                    element.centerX + element.radiusX,
                    element.centerY + element.radiusY
                )

                // Dynamic Watercolor Infill
                if (element.isFilled) {
                    drawWatercolorOvalFill(canvas, rect, element.fillColor, totalAlpha)
                }

                canvas.drawOval(rect, strokePaint)

                // Show dimension badge if calibrated
                if (showDimensions && scale.isCalibrated) {
                    val label = element.getMeasurementLabel(scale)
                    if (label != null) {
                        drawCenterBadge(canvas, Point2D(element.centerX, element.centerY), label)
                    }
                }
            }

            is TextElement -> {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorWithAlpha
                    textSize = element.fontSizeSp * 2.2f
                    isFakeBoldText = true
                }
                element.text.lines().forEachIndexed { index, line ->
                    canvas.drawText(line, element.position.x, element.position.y + index * textPaint.fontSpacing, textPaint)
                }
            }

            is DimensionMarkup -> {
                drawArchitecturalDimensionLine(canvas, element.start, element.end, element.label, colorWithAlpha)
            }
        }

        // Selection boundary & handles
        if (isSelected) {
            drawSelectionHighlight(canvas, element.boundingBox())
        }
    }

    /**
     * Simulates dynamic watercolor wash for arbitrary paths with organic edge pooling
     */
    private fun surfacePath(points: List<Point2D>) = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
    }

    private fun rectPath(bounds: RectF) = Path().apply { addRect(bounds, Path.Direction.CW) }
    private fun ovalPath(bounds: RectF) = Path().apply { addOval(bounds, Path.Direction.CW) }

    /**
     * Broad, deterministic tonal structure clipped to exact surface geometry.
     * The cues are anchored to sheet-space bounds rather than frame time, zoom,
     * or render order. Water receives the strongest depth hierarchy; paving is
     * intentionally quiet so the pool and its coping remain easy to read.
     */
    private fun drawNorthstarSurfaceCue(
        canvas: Canvas,
        stableId: String,
        geometryFingerprint: Long,
        path: Path,
        bounds: RectF,
        material: com.example.model.SurfaceMaterial,
        alpha: Float,
    ) {
        if (bounds.width() <= 0f || bounds.height() <= 0f) return
        canvas.save()
        try {
            canvas.clipPath(path)
            when (material) {
                com.example.model.SurfaceMaterial.WATER -> drawWaterDepthCue(canvas, stableId, geometryFingerprint, path, bounds, material.outline.toInt(), alpha)
                com.example.model.SurfaceMaterial.PAVING -> drawQuietPavingCue(canvas, path, bounds, material.outline.toInt(), alpha)
                else -> drawBroadMaterialCue(canvas, bounds, material.outline.toInt(), alpha)
            }
        } finally {
            canvas.restore()
        }
    }

    private fun drawWaterDepthCue(
        canvas: Canvas,
        stableId: String,
        geometryFingerprint: Long,
        path: Path,
        bounds: RectF,
        outline: Int,
        alpha: Float,
    ) {
        // Northstar's blue pool reference is a presentation palette, not a change
        // to saved materials or Graphic mode's established flat fill.
        val waterPigment = Color.rgb(24, 91, 130)
        val deep = colorWithScaledAlpha(waterPigment, 130, alpha)
        val clear = colorWithScaledAlpha(waterPigment, 0, alpha)
        val depthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                bounds.left,
                bounds.top,
                bounds.right,
                bounds.bottom,
                deep,
                clear,
                Shader.TileMode.CLAMP,
            )
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, depthPaint)

        NorthstarWatercolorField.draw(
            canvas = canvas,
            stableId = stableId,
            geometryFingerprint = geometryFingerprint,
            path = path,
            bounds = bounds,
            pigmentColor = waterPigment,
            alpha = alpha,
        )

        NorthstarWaterDetails.draw(
            canvas = canvas,
            stableId = stableId,
            path = path,
            bounds = bounds,
            outline = outline,
            alpha = alpha,
        )
    }

    /** Translation is intentionally excluded so moving an object does not repaint its wash. */
    private fun surfaceGeometryFingerprint(element: VectorElement): Long {
        val bounds = element.boundingBox()
        var hash = 0xCBF29CE484222325uL.toLong()
        fun mix(value: Float) {
            hash = (hash xor value.toRawBits().toLong()) * 0x100000001B3uL.toLong()
        }
        mix(bounds.width())
        mix(bounds.height())
        when (element) {
            is FreehandPath -> element.points.forEach { point -> mix(point.x - bounds.left); mix(point.y - bounds.top) }
            is PolylineElement -> element.points.forEach { point -> mix(point.x - bounds.left); mix(point.y - bounds.top) }
            is RectangleElement, is EllipseElement -> Unit
            else -> mix(element.strokeWidth)
        }
        return hash
    }

    private fun drawQuietPavingCue(canvas: Canvas, path: Path, bounds: RectF, outline: Int, alpha: Float) {
        val shade = colorWithScaledAlpha(outline, 14, alpha)
        val clear = colorWithScaledAlpha(outline, 0, alpha)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                bounds.left,
                bounds.top,
                bounds.right,
                bounds.bottom,
                shade,
                clear,
                Shader.TileMode.CLAMP,
            )
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, paint)
    }

    private fun drawBroadMaterialCue(canvas: Canvas, bounds: RectF, outline: Int, alpha: Float) {
        val cue = colorWithScaledAlpha(outline, 24, alpha)
        val washBounds = RectF(
            bounds.left - bounds.width() * 0.10f,
            bounds.top - bounds.height() * 0.16f,
            bounds.left + bounds.width() * 0.72f,
            bounds.top + bounds.height() * 0.72f,
        )
        val shader = RadialGradient(
            washBounds.centerX(),
            washBounds.centerY(),
            max(washBounds.width(), washBounds.height()) * 0.52f,
            cue,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
            style = Paint.Style.FILL
        }
        canvas.drawOval(washBounds, paint)
    }

    private fun colorWithScaledAlpha(color: Int, strength: Int, alpha: Float) = Color.argb(
        (strength * alpha).toInt().coerceIn(0, 255),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )

    private fun drawWatercolorPathFill(
        canvas: Canvas,
        path: Path,
        bounds: RectF,
        fillColor: Long,
        totalAlpha: Int
    ) {
        val base = fillColor.toInt()
        val r = Color.red(base)
        val g = Color.green(base)
        val b = Color.blue(base)

        // 1. Base watercolor wash (semi-transparent, 35-40% opacity)
        val washPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((totalAlpha * 0.38f).toInt(), r, g, b)
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, washPaint)

        // 2. Pigment pooling border: watercolor naturally concentrates slightly at dry edges
        val edgePoolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((totalAlpha * 0.65f).toInt(), (r * 0.85f).toInt(), (g * 0.85f).toInt(), (b * 0.85f).toInt())
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        canvas.drawPath(path, edgePoolPaint)
    }

    /**
     * Simulates dynamic watercolor wash for rectangles with subtle organic gradients and edge pooling
     */
    private fun drawWatercolorRectFill(
        canvas: Canvas,
        rect: RectF,
        fillColor: Long,
        totalAlpha: Int
    ) {
        val base = fillColor.toInt()
        val r = Color.red(base)
        val g = Color.green(base)
        val b = Color.blue(base)

        // Watercolor gradient wash across rectangle
        val c1 = Color.argb((totalAlpha * 0.42f).toInt(), r, g, b)
        val c2 = Color.argb((totalAlpha * 0.32f).toInt(), (r * 0.95f).toInt(), (g * 0.95f).toInt(), (b * 0.95f).toInt())
        val shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, c1, c2, Shader.TileMode.CLAMP)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
            style = Paint.Style.FILL
        }
        canvas.drawRect(rect, fillPaint)

        // Subtle pigment pooling border
        val edgePool = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((totalAlpha * 0.6f).toInt(), (r * 0.82f).toInt(), (g * 0.82f).toInt(), (b * 0.82f).toInt())
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(rect, edgePool)
    }

    /**
     * Simulates dynamic watercolor wash for circles/ellipses (e.g. curved pools, trees, spas)
     */
    private fun drawWatercolorOvalFill(
        canvas: Canvas,
        rect: RectF,
        fillColor: Long,
        totalAlpha: Int
    ) {
        val base = fillColor.toInt()
        val r = Color.red(base)
        val g = Color.green(base)
        val b = Color.blue(base)

        // Radial wash (darker pool edge, translucent center)
        val cCenter = Color.argb((totalAlpha * 0.30f).toInt(), r, g, b)
        val cEdge = Color.argb((totalAlpha * 0.48f).toInt(), (r * 0.88f).toInt(), (g * 0.88f).toInt(), (b * 0.88f).toInt())
        val radius = max(rect.width(), rect.height()) / 2f

        if (radius > 1f) {
            val shader = RadialGradient(rect.centerX(), rect.centerY(), radius, cCenter, cEdge, Shader.TileMode.CLAMP)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.shader = shader
                style = Paint.Style.FILL
            }
            canvas.drawOval(rect, fillPaint)
        }

        // Edge pooling stroke
        val edgePool = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((totalAlpha * 0.65f).toInt(), (r * 0.8f).toInt(), (g * 0.8f).toInt(), (b * 0.8f).toInt())
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        canvas.drawOval(rect, edgePool)
    }

    /**
     * Hand-drafted architectural line: crisp linework with classic corner extension overshoot
     */
    private fun drawLineWithArchitecturalOvershoot(
        canvas: Canvas,
        start: Point2D,
        end: Point2D,
        paint: Paint,
        style: StrokeStyle
    ) {
        if (style != StrokeStyle.INK) {
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
            return
        }

        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble()).toFloat()
        val overshoot = 5.0f // 5px architectural pen extension
        val cosA = cos(angle) * overshoot
        val sinA = sin(angle) * overshoot

        canvas.drawLine(
            start.x - cosA, start.y - sinA,
            end.x + cosA, end.y + sinA,
            paint
        )
    }

    /**
     * Architectural rectangle with classic draftsman cross-corners
     */
    private fun drawRectWithArchitecturalOvershoots(
        canvas: Canvas,
        rect: RectF,
        paint: Paint,
        style: StrokeStyle
    ) {
        if (style != StrokeStyle.INK) {
            canvas.drawRect(rect, paint)
            return
        }

        val overshoot = 6f
        // Top edge
        canvas.drawLine(rect.left - overshoot, rect.top, rect.right + overshoot, rect.top, paint)
        // Bottom edge
        canvas.drawLine(rect.left - overshoot, rect.bottom, rect.right + overshoot, rect.bottom, paint)
        // Left edge
        canvas.drawLine(rect.left, rect.top - overshoot, rect.left, rect.bottom + overshoot, paint)
        // Right edge
        canvas.drawLine(rect.right, rect.top - overshoot, rect.right, rect.bottom + overshoot, paint)
    }

    /**
     * Floating measurement badge for lines
     */
    private fun drawDimensionBadge(canvas: Canvas, start: Point2D, end: Point2D, label: String) {
        val midX = (start.x + end.x) / 2f
        val midY = (start.y + end.y) / 2f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 22f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val bounds = android.graphics.Rect()
        textPaint.getTextBounds(label, 0, label.length, bounds)

        val bgPaint = Paint().apply {
            color = Color.argb(230, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
        }

        val badgeRect = RectF(
            midX - bounds.width() / 2f - 7f,
            midY - bounds.height() / 2f - 5f,
            midX + bounds.width() / 2f + 7f,
            midY + bounds.height() / 2f + 5f
        )
        canvas.drawRoundRect(badgeRect, 6f, 6f, bgPaint)
        canvas.drawRoundRect(badgeRect, 6f, 6f, borderPaint)
        canvas.drawText(label, midX, midY + bounds.height() / 2f - 1f, textPaint)
    }

    /**
     * Center badge for rectangles / circles
     */
    private fun drawCenterBadge(canvas: Canvas, center: Point2D, label: String) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 22f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val bounds = android.graphics.Rect()
        textPaint.getTextBounds(label, 0, label.length, bounds)

        val bgPaint = Paint().apply {
            color = Color.argb(225, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1.2f
            style = Paint.Style.STROKE
        }
        val badgeRect = RectF(
            center.x - bounds.width() / 2f - 8f,
            center.y - bounds.height() / 2f - 5f,
            center.x + bounds.width() / 2f + 8f,
            center.y + bounds.height() / 2f + 5f
        )
        canvas.drawRoundRect(badgeRect, 6f, 6f, bgPaint)
        canvas.drawRoundRect(badgeRect, 6f, 6f, borderPaint)
        canvas.drawText(label, center.x, center.y + bounds.height() / 2f - 1f, textPaint)
    }

    /**
     * Classic architectural dimension string with tick marks and label
     */
    fun drawArchitecturalDimensionLine(
        canvas: Canvas,
        start: Point2D,
        end: Point2D,
        label: String,
        color: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 26f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawLine(start.x, start.y, end.x, end.y, paint)

        // Draw tick marks at 45 degree architectural angle
        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble()).toFloat()
        val tickAngle = angle + (Math.PI / 4.0).toFloat()
        val tickLen = 14f

        val cosT = cos(tickAngle) * tickLen
        val sinT = sin(tickAngle) * tickLen

        canvas.drawLine(start.x - cosT, start.y - sinT, start.x + cosT, start.y + sinT, paint)
        canvas.drawLine(end.x - cosT, end.y - sinT, end.x + cosT, end.y + sinT, paint)

        val midX = (start.x + end.x) / 2f
        val midY = (start.y + end.y) / 2f - 6f

        val bounds = android.graphics.Rect()
        textPaint.getTextBounds(label, 0, label.length, bounds)
        val bgPaint = Paint().apply {
            this.color = Color.argb(230, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val pillRect = RectF(
            midX - bounds.width() / 2f - 8f,
            midY - bounds.height() - 4f,
            midX + bounds.width() / 2f + 8f,
            midY + 6f
        )
        canvas.drawRoundRect(pillRect, 6f, 6f, bgPaint)
        canvas.drawText(label, midX, midY, textPaint)
    }

    /**
     * Bounding box and corner handle highlight for selected vector object
     */
    private fun drawSelectionHighlight(canvas: Canvas, bounds: RectF) {
        val pad = 8f
        val selRect = RectF(bounds.left - pad, bounds.top - pad, bounds.right + pad, bounds.bottom + pad)

        val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2563EB") // Blueprint accent blue
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        }
        canvas.drawRoundRect(selRect, 4f, 4f, selPaint)

        // Draw corner handles
        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2563EB")
            style = Paint.Style.FILL
        }
        val handleWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val corners = listOf(
            Point2D(selRect.left, selRect.top),
            Point2D(selRect.right, selRect.top),
            Point2D(selRect.right, selRect.bottom),
            Point2D(selRect.left, selRect.bottom)
        )
        for (c in corners) {
            canvas.drawCircle(c.x, c.y, 6f, handlePaint)
            canvas.drawCircle(c.x, c.y, 4f, handleWhite)
        }
    }
}
