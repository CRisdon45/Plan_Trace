package com.example.ui.canvas

import android.graphics.Bitmap
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.HoldToStraightenDetector
import com.example.engine.StraightenedResult
import com.example.engine.WatercolorRenderer
import com.example.model.DimensionMarkup
import com.example.model.EllipseElement
import com.example.model.FreehandPath
import com.example.model.LayerBlendMode
import com.example.model.LineElement
import com.example.model.Point2D
import com.example.model.PolylineElement
import com.example.model.RectangleElement
import com.example.model.StrokeStyle
import com.example.model.TextElement
import com.example.model.TraceProject
import com.example.model.VectorElement
import com.example.ui.DrawingTool
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * High-performance Jetpack Compose Canvas component that captures stylus input events
 * using pointerInput / awaitPointerEventScope to enable drawing strokes on a transparent layer.
 *
 * S Pen & Stylus Suite features:
 * - Air View / Hover Reticle: renders stroke radius, crosshairs, and color badge when hovering
 * - S Pen Pressure Inking: captures pressure values from digitizer for dynamic stroke modulation
 * - S Pen Hardware Eraser Tip: automatically erases when stylus is flipped
 * - S Pen Barrel Button: single press opens Radial Palette; double-click triggers Quick Undo
 * - Palm Rejection: ignores resting palm while stylus draws; smooth 2-finger pan & zoom
 * - Transparent Layer Architecture: active strokes render to an isolated transparent canvas layer
 * - Hold-to-Straighten: geometric snapping for lines, circles, and rectangles
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StylusTransparentCanvas(
    modifier: Modifier = Modifier,
    project: TraceProject,
    backgroundBitmap: Bitmap?,
    activeTool: DrawingTool,
    strokeColor: Long,
    strokeWidth: Float,
    strokeStyle: StrokeStyle,
    stylusOnlyMode: Boolean = true,
    isCalibratingScale: Boolean = false,
    selectedElementId: String? = null,
    showDimensions: Boolean = true,
    onElementCreated: (VectorElement) -> Unit,
    onElementUpdated: (VectorElement) -> Unit,
    onElementsDeleted: (Set<String>) -> Unit,
    onElementDuplicated: (String) -> Unit,
    onElementSelected: (String?) -> Unit,
    onColorSampled: (Long) -> Unit,
    onCalibrationSegmentDrawn: (Point2D, Point2D) -> Unit,
    onTextRequested: (Point2D) -> Unit,
    onShowRadialPalette: (Offset) -> Unit,
    onHideRadialPalette: () -> Unit,
    onQuickUndo: () -> Unit,
    onFeedbackMessage: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Transform State (Pan and Zoom)
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset(0f, 0f)) }

    // Active inking stroke in World Coordinates
    val activeStrokePoints = remember { mutableStateListOf<Point2D>() }
    var currentStartPoint by remember { mutableStateOf<Point2D?>(null) }
    var currentEndPoint by remember { mutableStateOf<Point2D?>(null) }
    var activePointerPressure by remember { mutableFloatStateOf(1.0f) }

    // Polyline ongoing points
    val polylinePoints = remember { mutableStateListOf<Point2D>() }

    // S Pen Hover / Air View state (screen coordinates)
    var hoverScreenOffset by remember { mutableStateOf<Offset?>(null) }
    var isStylusHovering by remember { mutableStateOf(false) }

    // Hold-to-Straighten detection state
    var holdTimerJob by remember { mutableStateOf<Job?>(null) }
    var snappedResult by remember { mutableStateOf<StraightenedResult?>(null) }
    var isHoldLocked by remember { mutableStateOf(false) }

    // S Pen Barrel button double-click detection
    var lastBarrelClickTime by remember { mutableStateOf(0L) }

    // Selection Dragging State
    var dragStartWorldPoint by remember { mutableStateOf<Point2D?>(null) }

    // Eyedropper sampling loupe position & color
    var eyedropperSampleColor by remember { mutableStateOf<Long?>(null) }
    var eyedropperScreenPos by remember { mutableStateOf<Offset?>(null) }

    // Live measurement readout
    var liveMeasurementText by remember { mutableStateOf<String?>(null) }

    // Coordinate transforms
    fun screenToWorld(screen: Offset): Point2D {
        val wx = (screen.x - panOffset.x) / zoomScale
        val wy = (screen.y - panOffset.y) / zoomScale
        return Point2D(wx, wy)
    }

    fun worldToScreen(world: Point2D): Offset {
        val sx = world.x * zoomScale + panOffset.x
        val sy = world.y * zoomScale + panOffset.y
        return Offset(sx, sy)
    }

    fun sampleColorAtWorldPoint(point: Point2D): Long {
        val hitElement = project.elements.asReversed().firstOrNull { it.isPointInside(point) }
        if (hitElement != null) {
            when (hitElement) {
                is RectangleElement -> if (hitElement.isFilled) return hitElement.fillColor
                is EllipseElement -> if (hitElement.isFilled) return hitElement.fillColor
                is FreehandPath -> if (hitElement.fillColor != null) return hitElement.fillColor
                is PolylineElement -> if (hitElement.fillColor != null) return hitElement.fillColor
                else -> Unit
            }
            return hitElement.strokeColor
        }

        if (backgroundBitmap != null) {
            val px = point.x.toInt()
            val py = point.y.toInt()
            if (px in 0 until backgroundBitmap.width && py in 0 until backgroundBitmap.height) {
                val pixel = backgroundBitmap.getPixel(px, py)
                return (pixel.toLong() and 0xFFFFFFFFL)
            }
        }
        return 0xFF0F172A
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9F7F2))
            // 2-finger pan and zoom gesture handler
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newZoom = (zoomScale * zoom).coerceIn(0.20f, 30.0f)
                    zoomScale = newZoom
                    panOffset += pan
                }
            }
            // Jetpack Compose stylus pointer input event capture on transparent layer
            .pointerInput(
                activeTool,
                strokeColor,
                strokeWidth,
                strokeStyle,
                stylusOnlyMode,
                isCalibratingScale,
                zoomScale,
                panOffset
            ) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull() ?: continue
                        val isStylus = change.type == PointerType.Stylus
                        val isEraserTip = change.type == PointerType.Eraser
                        val isTouch = change.type == PointerType.Touch

                        // Palm rejection: If stylus only mode is on and input is single touch, ignore drawing
                        if (stylusOnlyMode && isTouch && event.changes.size == 1) {
                            continue
                        }

                        val isBarrelPressed = event.buttons.isSecondaryPressed

                        // S Pen Barrel button double click detection for Quick Undo
                        if (isBarrelPressed && isStylus) {
                            val now = System.currentTimeMillis()
                            if (now - lastBarrelClickTime in 50..380) {
                                onQuickUndo()
                                onFeedbackMessage("S Pen: Quick Undo")
                                lastBarrelClickTime = 0L
                            } else {
                                lastBarrelClickTime = now
                                onShowRadialPalette(change.position)
                            }
                        }

                        val pressure = change.pressure.coerceIn(0.1f, 1.0f)
                        activePointerPressure = pressure
                        val worldPos = screenToWorld(change.position).copy(pressure = pressure)

                        // Air View / Hover detection
                        if (!change.pressed) {
                            if (isStylus) {
                                hoverScreenOffset = change.position
                                isStylusHovering = true
                            } else {
                                isStylusHovering = false
                                hoverScreenOffset = null
                            }
                            continue
                        }

                        // When pen is touching screen, dismiss hover reticle
                        isStylusHovering = false

                        when (event.type) {
                            PointerEventType.Press -> {
                                change.consume()
                                onHideRadialPalette()

                                // If hardware eraser tip is down, treat as ERASER
                                val effectiveTool = if (isEraserTip) DrawingTool.ERASER else activeTool

                                when (effectiveTool) {
                                    DrawingTool.PEN -> {
                                        activeStrokePoints.clear()
                                        activeStrokePoints.add(worldPos)
                                        snappedResult = null
                                        isHoldLocked = false

                                        // Start Hold-to-Straighten timer (~0.4s)
                                        holdTimerJob?.cancel()
                                        holdTimerJob = coroutineScope.launch {
                                            delay(400)
                                            if (activeStrokePoints.size >= 5 && !isHoldLocked) {
                                                val detected = HoldToStraightenDetector.analyze(activeStrokePoints.toList())
                                                if (detected != null) {
                                                    snappedResult = detected
                                                    isHoldLocked = true
                                                    val label = when (detected) {
                                                        is StraightenedResult.Line -> "Straight Line"
                                                        is StraightenedResult.CircleOrEllipse -> "Circle/Ellipse"
                                                        is StraightenedResult.Rectangle -> "Rectangle"
                                                    }
                                                    onFeedbackMessage("Snapped: $label")
                                                }
                                            }
                                        }
                                    }

                                    DrawingTool.LINE, DrawingTool.RECTANGLE, DrawingTool.ELLIPSE, DrawingTool.MEASURE -> {
                                        currentStartPoint = worldPos
                                        currentEndPoint = worldPos
                                    }

                                    DrawingTool.POLYLINE -> {
                                        if (polylinePoints.isEmpty()) {
                                            polylinePoints.add(worldPos)
                                        }
                                        currentEndPoint = worldPos
                                    }

                                    DrawingTool.ERASER -> {
                                        val hitElements = project.elements.filter { it.distanceToPoint(worldPos) <= 24f }
                                        if (hitElements.isNotEmpty()) {
                                            onElementsDeleted(hitElements.map { it.id }.toSet())
                                        }
                                    }

                                    DrawingTool.SELECT -> {
                                        dragStartWorldPoint = worldPos
                                        val hit = project.elements.asReversed().firstOrNull { it.isPointInside(worldPos) }
                                        onElementSelected(hit?.id)
                                    }

                                    DrawingTool.EYEDROPPER -> {
                                        val sampled = sampleColorAtWorldPoint(worldPos)
                                        eyedropperSampleColor = sampled
                                        eyedropperScreenPos = change.position
                                        onColorSampled(sampled)
                                    }

                                    DrawingTool.WATERCOLOR_FILL -> {
                                        val hit = project.elements.asReversed().firstOrNull { it.isPointInside(worldPos) }
                                        if (hit != null) {
                                            val updated = hit.withFillColor(strokeColor)
                                            onElementUpdated(updated)
                                            onFeedbackMessage("Applied watercolor wash")
                                        }
                                    }

                                    DrawingTool.TEXT -> {
                                        onTextRequested(worldPos)
                                    }
                                }
                            }

                            PointerEventType.Move -> {
                                change.consume()
                                val effectiveTool = if (isEraserTip) DrawingTool.ERASER else activeTool

                                when (effectiveTool) {
                                    DrawingTool.PEN -> {
                                        if (!isHoldLocked) {
                                            activeStrokePoints.add(worldPos)
                                        } else if (snappedResult != null) {
                                            val start = activeStrokePoints.first()
                                            snappedResult = when (val res = snappedResult!!) {
                                                is StraightenedResult.Line -> StraightenedResult.Line(start, worldPos)
                                                is StraightenedResult.Rectangle -> StraightenedResult.Rectangle(
                                                    kotlin.math.min(start.x, worldPos.x), kotlin.math.min(start.y, worldPos.y),
                                                    kotlin.math.max(start.x, worldPos.x), kotlin.math.max(start.y, worldPos.y)
                                                )
                                                is StraightenedResult.CircleOrEllipse -> {
                                                    val rx = kotlin.math.abs(worldPos.x - res.centerX)
                                                    val ry = kotlin.math.abs(worldPos.y - res.centerY)
                                                    StraightenedResult.CircleOrEllipse(res.centerX, res.centerY, kotlin.math.max(rx, 15f), kotlin.math.max(ry, 15f))
                                                }
                                            }
                                        }

                                        if (project.scaleCalibration.isCalibrated && activeStrokePoints.size > 1) {
                                            val first = activeStrokePoints.first()
                                            val last = activeStrokePoints.last()
                                            liveMeasurementText = project.scaleCalibration.formatMeasurement(first.distanceTo(last))
                                        }
                                    }

                                    DrawingTool.LINE, DrawingTool.RECTANGLE, DrawingTool.ELLIPSE, DrawingTool.MEASURE -> {
                                        currentEndPoint = worldPos
                                        val start = currentStartPoint
                                        if (start != null && project.scaleCalibration.isCalibrated) {
                                            val dist = start.distanceTo(worldPos)
                                            liveMeasurementText = project.scaleCalibration.formatMeasurement(dist)
                                        }
                                    }

                                    DrawingTool.POLYLINE -> {
                                        currentEndPoint = worldPos
                                    }

                                    DrawingTool.ERASER -> {
                                        val hitElements = project.elements.filter { it.distanceToPoint(worldPos) <= 24f }
                                        if (hitElements.isNotEmpty()) {
                                            onElementsDeleted(hitElements.map { it.id }.toSet())
                                        }
                                    }

                                    DrawingTool.SELECT -> {
                                        val selId = selectedElementId
                                        val dragStart = dragStartWorldPoint
                                        if (selId != null && dragStart != null) {
                                            val dx = worldPos.x - dragStart.x
                                            val dy = worldPos.y - dragStart.y
                                            val elem = project.elements.find { it.id == selId }
                                            if (elem != null) {
                                                onElementUpdated(elem.translate(dx, dy))
                                                dragStartWorldPoint = worldPos
                                            }
                                        }
                                    }

                                    DrawingTool.EYEDROPPER -> {
                                        val sampled = sampleColorAtWorldPoint(worldPos)
                                        eyedropperSampleColor = sampled
                                        eyedropperScreenPos = change.position
                                        onColorSampled(sampled)
                                    }

                                    else -> Unit
                                }
                            }

                            PointerEventType.Release -> {
                                change.consume()
                                holdTimerJob?.cancel()
                                liveMeasurementText = null
                                eyedropperScreenPos = null
                                eyedropperSampleColor = null

                                val effectiveTool = if (isEraserTip) DrawingTool.ERASER else activeTool

                                when (effectiveTool) {
                                    DrawingTool.PEN -> {
                                        if (snappedResult != null) {
                                            val element: com.example.model.VectorElement = when (val res = snappedResult!!) {
                                                is StraightenedResult.Line -> com.example.model.LineElement(
                                                    layerId = project.activeLayerId,
                                                    start = res.start,
                                                    end = res.end,
                                                    strokeColor = strokeColor,
                                                    strokeWidth = strokeWidth,
                                                    style = strokeStyle
                                                )
                                                is StraightenedResult.Rectangle -> com.example.model.RectangleElement(
                                                    layerId = project.activeLayerId,
                                                    left = res.left,
                                                    top = res.top,
                                                    right = res.right,
                                                    bottom = res.bottom,
                                                    strokeColor = strokeColor,
                                                    strokeWidth = strokeWidth,
                                                    style = strokeStyle
                                                )
                                                is StraightenedResult.CircleOrEllipse -> com.example.model.EllipseElement(
                                                    layerId = project.activeLayerId,
                                                    centerX = res.centerX,
                                                    centerY = res.centerY,
                                                    radiusX = res.radiusX,
                                                    radiusY = res.radiusY,
                                                    strokeColor = strokeColor,
                                                    strokeWidth = strokeWidth,
                                                    style = strokeStyle
                                                )
                                            }
                                            onElementCreated(element)
                                        } else if (activeStrokePoints.size > 1) {
                                            val element = FreehandPath(
                                                layerId = project.activeLayerId,
                                                points = activeStrokePoints.toList(),
                                                strokeColor = strokeColor,
                                                strokeWidth = strokeWidth,
                                                style = strokeStyle
                                            )
                                            onElementCreated(element)
                                        }
                                        activeStrokePoints.clear()
                                        snappedResult = null
                                        isHoldLocked = false
                                    }

                                    DrawingTool.LINE -> {
                                        val start = currentStartPoint
                                        val end = currentEndPoint
                                        if (start != null && end != null && start.distanceTo(end) > 5f) {
                                            val element = LineElement(
                                                layerId = project.activeLayerId,
                                                start = start,
                                                end = end,
                                                strokeColor = strokeColor,
                                                strokeWidth = strokeWidth,
                                                style = strokeStyle
                                            )
                                            onElementCreated(element)
                                        }
                                        currentStartPoint = null
                                        currentEndPoint = null
                                    }

                                    DrawingTool.RECTANGLE -> {
                                        val start = currentStartPoint
                                        val end = currentEndPoint
                                        if (start != null && end != null && (abs(start.x - end.x) > 5f || abs(start.y - end.y) > 5f)) {
                                            val element = RectangleElement(
                                                layerId = project.activeLayerId,
                                                left = start.x,
                                                top = start.y,
                                                right = end.x,
                                                bottom = end.y,
                                                strokeColor = strokeColor,
                                                strokeWidth = strokeWidth,
                                                style = strokeStyle
                                            )
                                            onElementCreated(element)
                                        }
                                        currentStartPoint = null
                                        currentEndPoint = null
                                    }

                                    DrawingTool.ELLIPSE -> {
                                        val start = currentStartPoint
                                        val end = currentEndPoint
                                        if (start != null && end != null && start.distanceTo(end) > 5f) {
                                            val cx = (start.x + end.x) / 2f
                                            val cy = (start.y + end.y) / 2f
                                            val rx = abs(end.x - start.x) / 2f
                                            val ry = abs(end.y - start.y) / 2f
                                            val element = EllipseElement(
                                                layerId = project.activeLayerId,
                                                centerX = cx,
                                                centerY = cy,
                                                radiusX = rx,
                                                radiusY = ry,
                                                strokeColor = strokeColor,
                                                strokeWidth = strokeWidth,
                                                style = strokeStyle
                                            )
                                            onElementCreated(element)
                                        }
                                        currentStartPoint = null
                                        currentEndPoint = null
                                    }

                                    DrawingTool.MEASURE -> {
                                        val start = currentStartPoint
                                        val end = currentEndPoint
                                        if (start != null && end != null && start.distanceTo(end) > 10f) {
                                            if (isCalibratingScale) {
                                                onCalibrationSegmentDrawn(start, end)
                                            } else {
                                                val label = project.scaleCalibration.formatMeasurement(start.distanceTo(end))
                                                val dim = DimensionMarkup(
                                                    layerId = project.activeLayerId,
                                                    start = start,
                                                    end = end,
                                                    label = label,
                                                    strokeColor = strokeColor,
                                                    strokeWidth = strokeWidth
                                                )
                                                onElementCreated(dim)
                                            }
                                        }
                                        currentStartPoint = null
                                        currentEndPoint = null
                                    }

                                    DrawingTool.SELECT -> {
                                        dragStartWorldPoint = null
                                    }

                                    else -> Unit
                                }
                            }

                            else -> Unit
                        }
                    }
                }
            }
    ) {
        // Base Architectural Canvas: Renders Base Underlay + Transparent Layers + In-progress Strokes
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("stylus_transparent_canvas")
        ) {
            drawIntoCanvas { composeCanvas ->
                val canvas = composeCanvas.nativeCanvas

                canvas.save()
                canvas.translate(panOffset.x, panOffset.y)
                canvas.scale(zoomScale, zoomScale)

                // 1. Base Construction Plan / PDF Underlay
                if (backgroundBitmap != null) {
                    val bgPaint = Paint().apply {
                        alpha = (project.backgroundOpacity * 255).toInt().coerceIn(0, 255)
                        isFilterBitmap = true
                    }
                    val src = android.graphics.Rect(0, 0, backgroundBitmap.width, backgroundBitmap.height)
                    val dst = RectF(0f, 0f, 2048f, 1536f)
                    canvas.drawBitmap(backgroundBitmap, src, dst, bgPaint)
                }

                // 2. Render Completed Vector Elements Layer-by-Layer
                for (layer in project.layers) {
                    if (!layer.isVisible) continue
                    val layerElements = project.elements.filter { it.layerId == layer.id }
                    val layerAlpha = layer.opacity

                    for (element in layerElements) {
                        WatercolorRenderer.render(
                            canvas = canvas,
                            element = element,
                            layerAlpha = layerAlpha,
                            scale = project.scaleCalibration,
                            showDimensions = showDimensions
                        )
                    }
                }

                // 3. Selection Highlight Bounding Box
                val selectedElem = project.elements.find { it.id == selectedElementId }
                if (selectedElem != null) {
                    val bbox = selectedElem.boundingBox()
                    val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#3B82F6")
                        style = Paint.Style.STROKE
                        setStrokeWidth(2.5f / zoomScale)
                        pathEffect = DashPathEffect(floatArrayOf(8f / zoomScale, 6f / zoomScale), 0f)
                    }
                    canvas.drawRect(bbox, selPaint)

                    // Corner grip dots
                    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#3B82F6")
                        style = Paint.Style.FILL
                    }
                    val r = 5f / zoomScale
                    canvas.drawCircle(bbox.left, bbox.top, r, dotPaint)
                    canvas.drawCircle(bbox.right, bbox.top, r, dotPaint)
                    canvas.drawCircle(bbox.right, bbox.bottom, r, dotPaint)
                    canvas.drawCircle(bbox.left, bbox.bottom, r, dotPaint)
                }

                // 4. In-Progress Drawing Stroke on Transparent Layer
                val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = strokeColor.toInt()
                    setStrokeWidth(strokeWidth * (0.35f + 1.15f * activePointerPressure))
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }

                if (snappedResult != null) {
                    // Snapped preview geometry
                    val snapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#10B981")
                        setStrokeWidth(strokeWidth)
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                    }
                    when (val res = snappedResult!!) {
                        is StraightenedResult.Line -> canvas.drawLine(res.start.x, res.start.y, res.end.x, res.end.y, snapPaint)
                        is StraightenedResult.Rectangle -> canvas.drawRect(RectF(res.left, res.top, res.right, res.bottom), snapPaint)
                        is StraightenedResult.CircleOrEllipse -> canvas.drawOval(
                            RectF(res.centerX - res.radiusX, res.centerY - res.radiusY, res.centerX + res.radiusX, res.centerY + res.radiusY),
                            snapPaint
                        )
                    }
                } else if (activeStrokePoints.size > 1) {
                    val path = Path()
                    path.moveTo(activeStrokePoints[0].x, activeStrokePoints[0].y)
                    for (i in 1 until activeStrokePoints.size) {
                        path.lineTo(activeStrokePoints[i].x, activeStrokePoints[i].y)
                    }
                    canvas.drawPath(path, activePaint)
                }

                // In-progress Line / Measure
                val start = currentStartPoint
                val end = currentEndPoint
                if (start != null && end != null) {
                    canvas.drawLine(start.x, start.y, end.x, end.y, activePaint)
                }

                canvas.restore()
            }
        }

        // 5. S Pen Air View / Hover Reticle Overlay
        if (isStylusHovering && hoverScreenOffset != null) {
            val hoverPos = hoverScreenOffset!!
            val cursorRadiusPx = (strokeWidth * zoomScale / 2f).coerceIn(4f, 80f)
            val density = androidx.compose.ui.platform.LocalDensity.current
            val diameterDp = with(density) { (cursorRadiusPx * 2).toDp() }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (hoverPos.x - cursorRadiusPx).roundToInt(),
                            (hoverPos.y - cursorRadiusPx).roundToInt()
                        )
                    }
                    .size(diameterDp)
            ) {
                // Crosshair Reticle & Stroke Radius Circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    // Brush boundary circle
                    drawCircle(
                        color = Color(strokeColor).copy(alpha = 0.85f),
                        radius = size.width / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                    // Precision center crosshair
                    val hairLen = 6.dp.toPx()
                    drawLine(
                        color = Color.DarkGray.copy(alpha = 0.8f),
                        start = Offset(center.x - hairLen, center.y),
                        end = Offset(center.x + hairLen, center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.DarkGray.copy(alpha = 0.8f),
                        start = Offset(center.x, center.y - hairLen),
                        end = Offset(center.x, center.y + hairLen),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        // 6. Live Architectural Distance Badge
        if (liveMeasurementText != null && hoverScreenOffset != null) {
            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            hoverScreenOffset!!.x.roundToInt() + 16,
                            hoverScreenOffset!!.y.roundToInt() - 36
                        )
                    }
                    .shadow(4.dp, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ) {
                Text(
                    text = liveMeasurementText!!,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
