package com.example.ui.canvas

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.example.export.ExportGeometry
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GeometrySnapEngine
import com.example.engine.HoldToStraightenDetector
import com.example.engine.SnapResult
import com.example.engine.SnapSettings
import com.example.engine.StraightenedResult
import com.example.engine.WatercolorRenderer
import com.example.model.DimensionMarkup
import com.example.model.EllipseElement
import com.example.model.FreehandPath
import com.example.model.LineElement
import com.example.model.Point2D
import com.example.model.PolylineElement
import com.example.model.RectangleElement
import com.example.model.RectangleResize
import com.example.model.StrokeStyle
import com.example.model.TextElement
import com.example.model.TraceProject
import com.example.model.VectorElement
import com.example.ui.DrawingTool
import com.example.ui.components.ShapeSizeDialog
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TraceCanvas(
    modifier: Modifier = Modifier,
    fitRequest: Int = 0,
    project: TraceProject,
    backgroundBitmap: Bitmap?,
    activeTool: DrawingTool,
    strokeColor: Long,
    strokeWidth: Float,
    strokeStyle: StrokeStyle,
    stylusOnlyMode: Boolean,
    isCalibratingScale: Boolean,
    selectedElementId: String?,
    showDimensions: Boolean,
    snapSettings: SnapSettings = SnapSettings(),
    onElementCreated: (VectorElement) -> Unit,
    onElementUpdated: (VectorElement) -> Unit,
    onEditGestureStarted: () -> Unit = {},
    onEditGestureEnded: () -> Unit = {},
    onEditGestureCancelled: () -> Unit = {},
    onElementsDeleted: (Set<String>) -> Unit,
    onElementDuplicated: (String) -> Unit,
    onElementSelected: (String?) -> Unit,
    onElementHeldForLayer: (VectorElement) -> Unit = {},
    onColorSampled: (Long) -> Unit,
    onCalibrationSegmentDrawn: (Point2D, Point2D) -> Unit,
    onTextRequested: (Point2D) -> Unit,
    onTextEditRequested: (TextElement) -> Unit = {},
    onShowRadialPalette: (Offset) -> Unit,
    onHideRadialPalette: () -> Unit,
    onQuickUndo: () -> Unit = {},
    onFeedbackMessage: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Transform State (Infinite Pan and Zoom)
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val fitPadding = with(LocalDensity.current) { 24.dp.toPx() }
    val toolDockWidth = with(LocalDensity.current) { 86.dp.toPx() }
    var zoomScale by remember(project.id, project.pageKey) { mutableFloatStateOf(1.0f) }
    var panOffset by remember(project.id, project.pageKey) { mutableStateOf(Offset(0f, 0f)) }

    // Multi-touch Pan & Pinch Zoom tracking
    var prevCentroid by remember { mutableStateOf<Offset?>(null) }
    var prevSpan by remember { mutableFloatStateOf(0f) }
    var singlePanPrevPos by remember { mutableStateOf<Offset?>(null) }
    var navigating by remember { mutableStateOf(false) }

    // Active Geometry Snap result
    var activeSnapResult by remember { mutableStateOf<SnapResult?>(null) }

    // Hold-to-Assign Layer timer
    var elementHoldTimerJob by remember { mutableStateOf<Job?>(null) }

    // Active in-progress drawing points in World Coordinates
    val currentPoints = remember { mutableStateListOf<Point2D>() }
    var currentStartPoint by remember { mutableStateOf<Point2D?>(null) }
    var currentEndPoint by remember { mutableStateOf<Point2D?>(null) }

    // Polyline ongoing points
    val polylinePoints = remember { mutableStateListOf<Point2D>() }
    var lastPolylineTap by remember { mutableStateOf(0L) }

    fun finishPolyline(closed: Boolean = false) {
        if (polylinePoints.size >= (if (closed) 3 else 2)) {
            onElementCreated(PolylineElement(layerId = project.activeLayerId, points = polylinePoints.toList(),
                isClosed = closed, strokeColor = strokeColor, strokeWidth = strokeWidth, style = strokeStyle))
        }
        polylinePoints.clear()
        lastPolylineTap = 0L
    }

    LaunchedEffect(project.id, project.pdfPageNumber, project.activeLayerId, activeTool, isCalibratingScale) {
        if (polylinePoints.isNotEmpty()) {
            polylinePoints.clear()
            onFeedbackMessage("Unfinished polyline cancelled")
        }
    }

    // S Pen Air View / Hover Reticle state
    var hoverScreenPos by remember { mutableStateOf<Offset?>(null) }
    var isStylusHovering by remember { mutableStateOf(false) }

    // Hold-to-Straighten detection state
    var holdTimerJob by remember { mutableStateOf<Job?>(null) }
    var snappedResult by remember { mutableStateOf<StraightenedResult?>(null) }
    var isHoldLocked by remember { mutableStateOf(false) }

    // Barrel button state & double-click tracking for Quick Undo
    var isBarrelButtonPressed by remember { mutableStateOf(false) }
    var lastBarrelClickTime by remember { mutableStateOf(0L) }

    // Selection Dragging State
    var dragStartWorldPoint by remember { mutableStateOf<Point2D?>(null) }
    var rectangleResize by remember { mutableStateOf<RectangleResize?>(null) }
    var sizingRectangle by remember(project.id) { mutableStateOf<RectangleElement?>(null) }

    // Eyedropper sampling loupe position & color
    var eyedropperSampleColor by remember { mutableStateOf<Long?>(null) }
    var eyedropperScreenPos by remember { mutableStateOf<Offset?>(null) }

    // Live measurement readout
    var liveMeasurementText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fitRequest, viewportSize, backgroundBitmap) {
        if (fitRequest > 0 && viewportSize.width > toolDockWidth + fitPadding * 2 && viewportSize.height > fitPadding * 2) {
            onEditGestureCancelled()
            currentPoints.clear()
            currentStartPoint = null
            currentEndPoint = null
            rectangleResize = null
            val bounds = ExportGeometry.contentBounds(project, backgroundBitmap?.width, backgroundBitmap?.height)
            val fit = ExportGeometry.fit(bounds, RectF(toolDockWidth + fitPadding, fitPadding,
                viewportSize.width - fitPadding, viewportSize.height - fitPadding))
            zoomScale = fit.scale
            panOffset = Offset(fit.translateX, fit.translateY)
        }
    }

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

    // Color sampling helper (from vector elements or background plan bitmap)
    fun sampleColorAtWorldPoint(point: Point2D): Long {
        // 1. Check topmost vector elements
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

        // 2. Sample from background plan bitmap
        if (backgroundBitmap != null) {
            val px = point.x.toInt()
            val py = point.y.toInt()
            if (px in 0 until backgroundBitmap.width && py in 0 until backgroundBitmap.height) {
                val pixel = backgroundBitmap.getPixel(px, py)
                return (pixel.toLong() and 0xFFFFFFFFL)
            }
        }
        return 0xFF0F172A // Default charcoal
    }

    sizingRectangle?.let { rectangle ->
        ShapeSizeDialog(rectangle, project.scaleCalibration, onDismiss = { sizingRectangle = null }, onApply = onElementUpdated)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF9F7F2))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().onSizeChanged { viewportSize = it }
            // Input belongs to the canvas, not the parent of the action toolbar.
            // Stylus, barrel button, pressure & hold-to-straighten pointer filter
            .pointerInteropFilter { motionEvent ->
                val pointerCount = motionEvent.pointerCount
                val action = motionEvent.actionMasked

                // If 2 or more fingers are down, allow pan/zoom and don't draw
                if (pointerCount > 1) {
                    onEditGestureCancelled()
                    navigating = true
                    currentPoints.clear()
                    currentStartPoint = null
                    currentEndPoint = null
                    snappedResult = null
                    isHoldLocked = false
                    holdTimerJob?.cancel()
                    eyedropperScreenPos = null
                    val centroid = Offset((motionEvent.getX(0) + motionEvent.getX(1)) / 2f,
                        (motionEvent.getY(0) + motionEvent.getY(1)) / 2f)
                    val span = hypot(motionEvent.getX(1) - motionEvent.getX(0), motionEvent.getY(1) - motionEvent.getY(0))
                    val previous = prevCentroid
                    if (action == MotionEvent.ACTION_MOVE && previous != null && prevSpan > 0f) {
                        val nextZoom = (zoomScale * span / prevSpan).coerceIn(.01f, 30f)
                        panOffset = centroid - (previous - panOffset) * (nextZoom / zoomScale)
                        zoomScale = nextZoom
                    }
                    prevCentroid = if (action == MotionEvent.ACTION_POINTER_UP) null else centroid
                    prevSpan = if (action == MotionEvent.ACTION_POINTER_UP) 0f else span
                    singlePanPrevPos = null
                    return@pointerInteropFilter true
                }

                val toolType = motionEvent.getToolType(0)
                val isStylus = toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER
                val isHardwareEraser = toolType == MotionEvent.TOOL_TYPE_ERASER
                val buttonState = motionEvent.buttonState

                // S Pen Hover tracking (Air View)
                if (action == MotionEvent.ACTION_HOVER_MOVE || action == MotionEvent.ACTION_HOVER_ENTER) {
                    if (isStylus) {
                        hoverScreenPos = Offset(motionEvent.x, motionEvent.y)
                        isStylusHovering = true
                    }
                    return@pointerInteropFilter true
                } else if (action == MotionEvent.ACTION_HOVER_EXIT) {
                    hoverScreenPos = null
                    isStylusHovering = false
                    return@pointerInteropFilter true
                }

                // S Pen Barrel button: single press opens radial menu, double-click triggers Quick Undo
                val barrelActive = isStylus && (buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0)
                if (barrelActive) {
                    if (!isBarrelButtonPressed) {
                        isBarrelButtonPressed = true
                        val now = System.currentTimeMillis()
                        if (now - lastBarrelClickTime in 50..380) {
                            onQuickUndo()
                            onFeedbackMessage("S Pen: Quick Undo")
                            lastBarrelClickTime = 0L
                            onHideRadialPalette()
                        } else {
                            lastBarrelClickTime = now
                            onShowRadialPalette(Offset(motionEvent.x, motionEvent.y))
                        }
                    }
                } else if (isBarrelButtonPressed) {
                    isBarrelButtonPressed = false
                    onHideRadialPalette()
                }

                // If stylusOnlyMode is active and user touches with finger, pan/zoom instead of drawing
                if (!isStylus && (stylusOnlyMode || navigating || activeTool == DrawingTool.PAN)) {
                    val position = Offset(motionEvent.x, motionEvent.y)
                    if (action == MotionEvent.ACTION_MOVE) {
                        singlePanPrevPos?.let { panOffset += position - it }
                    }
                    singlePanPrevPos = if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) null else position
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        navigating = false
                        prevCentroid = null
                        prevSpan = 0f
                    }
                    return@pointerInteropFilter true
                }

                val screenPos = Offset(motionEvent.x, motionEvent.y)
                val pressure = motionEvent.getPressure(0).coerceIn(0.1f, 1.0f)
                val worldPoint = screenToWorld(screenPos).copy(pressure = pressure)
                val effectiveTool = when {
                    isCalibratingScale -> DrawingTool.MEASURE
                    isHardwareEraser -> DrawingTool.ERASER
                    else -> activeTool
                }

                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        onEditGestureStarted()
                        hoverScreenPos = null
                        isStylusHovering = false
                        snappedResult = null
                        isHoldLocked = false
                        currentPoints.clear()
                        currentPoints.add(worldPoint)
                        currentStartPoint = worldPoint
                        currentEndPoint = worldPoint

                        // Text Note tool
                        if (effectiveTool == DrawingTool.TEXT) {
                            onTextRequested(worldPoint)
                            return@pointerInteropFilter true
                        }

                        // Eyedropper tool
                        if (effectiveTool == DrawingTool.EYEDROPPER) {
                            val sampled = sampleColorAtWorldPoint(worldPoint)
                            eyedropperSampleColor = sampled
                            eyedropperScreenPos = screenPos
                            onColorSampled(sampled)
                            return@pointerInteropFilter true
                        }

                        // Watercolor Wash / Infill tool: tap on shape to fill
                        if (effectiveTool == DrawingTool.WATERCOLOR_FILL) {
                            val target = project.elements.asReversed().firstOrNull { it.isPointInside(worldPoint) }
                            if (target != null) {
                                val filled = target.withFillColor(strokeColor)
                                onElementUpdated(filled)
                                onFeedbackMessage("Applied Watercolor Wash")
                            }
                            return@pointerInteropFilter true
                        }

                        // Select Object tool
                        if (effectiveTool == DrawingTool.SELECT) {
                            val selected = project.elements.find { it.id == selectedElementId } as? RectangleElement
                            rectangleResize = selected?.takeIf { rectangle -> project.layers.any { it.id == rectangle.layerId && it.isVisible && !it.isLocked } }
                                ?.let { RectangleResize.hit(it, worldPoint, zoomScale) }
                            if (rectangleResize != null) return@pointerInteropFilter true
                            val hit = project.elements.asReversed().firstOrNull { it.isPointInside(worldPoint) }
                            onElementSelected(hit?.id)
                            dragStartWorldPoint = worldPoint
                            return@pointerInteropFilter true
                        }

                        // Eraser tool
                        if (effectiveTool == DrawingTool.ERASER) {
                            eraseAtPoint(worldPoint, project.elements, onElementsDeleted)
                            return@pointerInteropFilter true
                        }

                        // Freehand Pen: initialize ~0.4s Hold-to-Straighten detection
                        if (effectiveTool == DrawingTool.PEN) {
                            holdTimerJob?.cancel()
                            holdTimerJob = coroutineScope.launch {
                                delay(400) // 0.4 seconds hold
                                if (currentPoints.size >= 5) {
                                    val result = HoldToStraightenDetector.analyze(currentPoints)
                                    if (result != null) {
                                        snappedResult = result
                                        isHoldLocked = true
                                        when (result) {
                                            is StraightenedResult.Line -> onFeedbackMessage("Snapped to Straight Line")
                                            is StraightenedResult.CircleOrEllipse -> onFeedbackMessage("Snapped to Circle/Ellipse")
                                            is StraightenedResult.Rectangle -> onFeedbackMessage("Snapped to Rectangle")
                                        }
                                    }
                                }
                            }
                        }
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        currentEndPoint = worldPoint
                        if (effectiveTool == DrawingTool.SELECT && rectangleResize != null) {
                            onElementUpdated(rectangleResize!!.at(worldPoint))
                            return@pointerInteropFilter true
                        }

                        // Eyedropper sampling during move
                        if (effectiveTool == DrawingTool.EYEDROPPER) {
                            val sampled = sampleColorAtWorldPoint(worldPoint)
                            eyedropperSampleColor = sampled
                            eyedropperScreenPos = screenPos
                            onColorSampled(sampled)
                            return@pointerInteropFilter true
                        }

                        // Select Object: drag to translate selected element
                        if (effectiveTool == DrawingTool.SELECT && selectedElementId != null && dragStartWorldPoint != null) {
                            val dx = worldPoint.x - dragStartWorldPoint!!.x
                            val dy = worldPoint.y - dragStartWorldPoint!!.y
                            val el = project.elements.find { it.id == selectedElementId }
                            if (el != null) {
                                onElementUpdated(el.translate(dx, dy))
                                dragStartWorldPoint = worldPoint
                            }
                            return@pointerInteropFilter true
                        }

                        // Eraser continuous stroke
                        if (effectiveTool == DrawingTool.ERASER) {
                            eraseAtPoint(worldPoint, project.elements, onElementsDeleted)
                            return@pointerInteropFilter true
                        }

                        // Freehand Pen: track points and update hold timer
                        if (effectiveTool == DrawingTool.PEN) {
                            val lastPoint = currentPoints.lastOrNull()
                            if (lastPoint == null || lastPoint.distanceTo(worldPoint) > 3.5f) {
                                currentPoints.add(worldPoint)

                                // If already snapped and locked, adjust the snapped geometry with the tip
                                if (isHoldLocked && snappedResult != null) {
                                    val start = currentPoints.first()
                                    snappedResult = when (val res = snappedResult!!) {
                                        is StraightenedResult.Line -> StraightenedResult.Line(start, worldPoint)
                                        is StraightenedResult.Rectangle -> StraightenedResult.Rectangle(
                                            min(start.x, worldPoint.x), min(start.y, worldPoint.y),
                                            max(start.x, worldPoint.x), max(start.y, worldPoint.y)
                                        )
                                        is StraightenedResult.CircleOrEllipse -> {
                                            val rx = abs(worldPoint.x - res.centerX)
                                            val ry = abs(worldPoint.y - res.centerY)
                                            StraightenedResult.CircleOrEllipse(res.centerX, res.centerY, max(rx, 15f), max(ry, 15f))
                                        }
                                    }
                                } else {
                                    // Reset hold timer on significant movement
                                    snappedResult = null
                                    holdTimerJob?.cancel()
                                    holdTimerJob = coroutineScope.launch {
                                        delay(400) // ~0.4s hold
                                        if (currentPoints.size >= 5) {
                                            val result = HoldToStraightenDetector.analyze(currentPoints)
                                            if (result != null) {
                                                snappedResult = result
                                                isHoldLocked = true
                                                when (result) {
                                                    is StraightenedResult.Line -> onFeedbackMessage("Snapped to Straight Line")
                                                    is StraightenedResult.CircleOrEllipse -> onFeedbackMessage("Snapped to Circle/Ellipse")
                                                    is StraightenedResult.Rectangle -> onFeedbackMessage("Snapped to Rectangle")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Live architectural dimension feedback
                        if (project.scaleCalibration.isCalibrated) {
                            val start = currentStartPoint
                            if (start != null) {
                                val dist = start.distanceTo(worldPoint)
                                val formatted = project.scaleCalibration.formatMeasurement(dist)
                                liveMeasurementText = when (effectiveTool) {
                                    DrawingTool.LINE -> "Length: $formatted"
                                    DrawingTool.RECTANGLE -> {
                                        val w = project.scaleCalibration.formatMeasurement(abs(worldPoint.x - start.x))
                                        val h = project.scaleCalibration.formatMeasurement(abs(worldPoint.y - start.y))
                                        "$w × $h"
                                    }
                                    DrawingTool.ELLIPSE -> "Ø " + project.scaleCalibration.formatMeasurement(dist * 2f)
                                    DrawingTool.MEASURE -> "Distance: $formatted"
                                    else -> if (isCalibratingScale) "Ref: $formatted" else null
                                }
                            }
                        }
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        rectangleResize?.let { onElementUpdated(it.at(worldPoint)) }
                        rectangleResize = null
                        holdTimerJob?.cancel()
                        eyedropperScreenPos = null
                        dragStartWorldPoint = null

                        if (isCalibratingScale) {
                            val start = currentStartPoint
                            val end = currentEndPoint
                            if (start != null && end != null && start.distanceTo(end) > 15f) {
                                onCalibrationSegmentDrawn(start, end)
                            }
                            currentPoints.clear()
                            currentStartPoint = null
                            currentEndPoint = null
                            liveMeasurementText = null
                            onEditGestureEnded()
                            return@pointerInteropFilter true
                        }

                        if (effectiveTool == DrawingTool.POLYLINE) {
                            val last = polylinePoints.lastOrNull()
                            val now = motionEvent.eventTime
                            if (polylinePoints.size >= 3 && polylinePoints.first().distanceTo(worldPoint) < 18f / zoomScale) {
                                finishPolyline(closed = true)
                            } else if (polylinePoints.size >= 2 && now - lastPolylineTap < 350L && last != null && last.distanceTo(worldPoint) < 18f / zoomScale) {
                                finishPolyline()
                            } else {
                                if (last == null || last.distanceTo(worldPoint) > 1f) polylinePoints.add(worldPoint)
                                lastPolylineTap = now
                            }
                            currentPoints.clear()
                            currentStartPoint = null
                            currentEndPoint = null
                            onEditGestureEnded()
                            return@pointerInteropFilter true
                        }

                        // Commit drawn element
                        val start = currentStartPoint
                        val end = currentEndPoint
                        if (start != null && end != null) {
                            handleStrokeCompleted(
                                activeTool = effectiveTool,
                                start = start,
                                end = end,
                                points = currentPoints.toList(),
                                snappedResult = snappedResult,
                                project = project,
                                strokeColor = strokeColor,
                                strokeWidth = strokeWidth,
                                strokeStyle = strokeStyle,
                                onElementCreated = onElementCreated
                            )
                        }

                        currentPoints.clear()
                        currentStartPoint = null
                        currentEndPoint = null
                        snappedResult = null
                        isHoldLocked = false
                        liveMeasurementText = null
                        onEditGestureEnded()
                        true
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        rectangleResize = null
                        onEditGestureCancelled()
                        holdTimerJob?.cancel()
                        eyedropperScreenPos = null
                        currentPoints.clear()
                        currentStartPoint = null
                        currentEndPoint = null
                        snappedResult = null
                        isHoldLocked = false
                        liveMeasurementText = null
                        true
                    }

                    else -> false
                }
            }
        ) {
            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas

                nativeCanvas.save()
                // Apply Pan and Zoom
                nativeCanvas.translate(panOffset.x, panOffset.y)
                nativeCanvas.scale(zoomScale, zoomScale)

                // 1. Draw Background Construction Plan (PDF or Image)
                if (backgroundBitmap != null) {
                    val bgPaint = Paint().apply {
                        alpha = (project.backgroundOpacity * 255).toInt().coerceIn(0, 255)
                        isFilterBitmap = true
                    }
                    val src = android.graphics.Rect(0, 0, backgroundBitmap.width, backgroundBitmap.height)
                    val dst = RectF(0f, 0f, backgroundBitmap.width.toFloat(), backgroundBitmap.height.toFloat())
                    nativeCanvas.drawBitmap(backgroundBitmap, src, dst, bgPaint)
                } else {
                    // Draw clean architectural blueprint grid
                    drawArchitecturalGrid(nativeCanvas, 3200f, 2400f)
                }

                // 2. Draw Vector Layers & Elements with Architectural / Watercolor rendering
                for (layer in project.layers) {
                    if (!layer.isVisible) continue
                    val layerElements = project.elements.filter { it.layerId == layer.id }
                    val layerAlpha = layer.opacity

                    for (el in layerElements) {
                        val isSelected = el.id == selectedElementId
                        WatercolorRenderer.render(
                            canvas = nativeCanvas,
                            element = el,
                            layerAlpha = layerAlpha,
                            scale = project.scaleCalibration,
                            showDimensions = showDimensions,
                            isSelected = isSelected
                        )
                    }
                }

                if (polylinePoints.isNotEmpty()) {
                    val path = Path().apply {
                        moveTo(polylinePoints.first().x, polylinePoints.first().y)
                        polylinePoints.drop(1).forEach { lineTo(it.x, it.y) }
                        currentEndPoint?.let { lineTo(it.x, it.y) }
                    }
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = AndroidColor.rgb(36, 99, 181)
                        style = Paint.Style.STROKE
                        this.strokeWidth = strokeWidth
                    }
                    nativeCanvas.drawPath(path, paint)
                    polylinePoints.forEach { nativeCanvas.drawCircle(it.x, it.y, 5f / zoomScale, paint) }
                }

                // 3. Draw Active In-Progress Gesture / Stroke Preview
                if (snappedResult != null) {
                    // Snapped clean geometry preview (Hold-to-Straighten)
                    renderSnappedResultPreview(nativeCanvas, snappedResult!!, strokeColor, strokeWidth, strokeStyle)
                } else if (currentPoints.size > 1 && activeTool == DrawingTool.PEN) {
                    renderCurrentFreehand(nativeCanvas, currentPoints, strokeColor, strokeWidth, strokeStyle)
                } else if (currentStartPoint != null && currentEndPoint != null) {
                    renderActiveShapePreview(
                        canvas = nativeCanvas,
                        tool = activeTool,
                        start = currentStartPoint!!,
                        end = currentEndPoint!!,
                        color = strokeColor,
                        strokeWidth = strokeWidth,
                        style = strokeStyle,
                        isCalibrating = isCalibratingScale
                    )
                }

                nativeCanvas.restore()
            }
        }

        if (polylinePoints.isNotEmpty()) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp), tonalElevation = 4.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${polylinePoints.size} points · unfinished", modifier = Modifier.padding(12.dp))
                    TextButton(onClick = { finishPolyline() }, enabled = polylinePoints.size >= 2) { Text("Finish") }
                    TextButton(onClick = { finishPolyline(true) }, enabled = polylinePoints.size >= 3) { Text("Close shape") }
                    TextButton(onClick = { polylinePoints.clear(); lastPolylineTap = 0L }) { Text("Cancel") }
                }
            }
        }

        // Live Dimension readout pill floating over canvas
        AnimatedVisibility(
            visible = liveMeasurementText != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 74.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = liveMeasurementText ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Eyedropper Magnifier Loupe Indicator
        if (eyedropperScreenPos != null && eyedropperSampleColor != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (eyedropperScreenPos!!.x - 40.dp.value * 2.5f).roundToInt(),
                            (eyedropperScreenPos!!.y - 80.dp.value * 2.5f).roundToInt()
                        )
                    }
                    .size(72.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(eyedropperSampleColor!!))
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        // Selected Object Action Bar (Delete, Duplicate, Recolor, Watercolor Wash)
        if (selectedElementId != null && activeTool == DrawingTool.SELECT) {
            val selectedEl = project.elements.find { it.id == selectedElementId }
            if (selectedEl != null) {
                val bounds = selectedEl.boundingBox()
                val screenCenter = worldToScreen(Point2D(bounds.centerX(), bounds.top))

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (screenCenter.x - 110.dp.value * 2.2f).roundToInt().coerceAtLeast(16),
                                (screenCenter.y - 58.dp.value * 2.5f).roundToInt().coerceAtLeast(16)
                            )
                        }
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Delete
                            if (selectedEl is RectangleElement) {
                                IconButton(onClick = {
                                    if (project.layers.any { it.id == selectedEl.layerId && !it.isLocked && it.isVisible }) sizingRectangle = selectedEl
                                    else onFeedbackMessage("Unlock this layer to resize its objects")
                                }) { Icon(Icons.Default.Straighten, contentDescription = "Edit size") }
                            }
                            if (selectedEl is TextElement) {
                                IconButton(onClick = { onTextEditRequested(selectedEl) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit note")
                                }
                            }
                            IconButton(onClick = { onElementsDeleted(setOf(selectedElementId)) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                            // 2. Duplicate
                            IconButton(onClick = { onElementDuplicated(selectedElementId) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = MaterialTheme.colorScheme.primary)
                            }
                            // 3. Recolor
                            IconButton(onClick = { onElementUpdated(selectedEl.withStrokeColor(strokeColor)) }) {
                                Icon(Icons.Default.Palette, contentDescription = "Recolor", tint = Color(strokeColor))
                            }
                            // 4. Watercolor Wash Infill
                            IconButton(onClick = {
                                val updated = selectedEl.withFillColor(strokeColor)
                                onElementUpdated(updated)
                            }) {
                                Icon(Icons.Default.FormatColorFill, contentDescription = "Watercolor Wash", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }

        // 4. S Pen Air View / Hover Reticle Overlay
        if (isStylusHovering && hoverScreenPos != null) {
            val hoverPos = hoverScreenPos!!
            val cursorRadiusPx = (strokeWidth * zoomScale / 2f).coerceIn(5f, 60f)
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
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    // Stroke footprint circle
                    drawCircle(
                        color = Color(strokeColor).copy(alpha = 0.75f),
                        radius = size.width / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                    // Precision center crosshair
                    val hairLen = 5.dp.toPx()
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
    }
}

/**
 * Commits a completed vector stroke into the project layer
 */
private fun handleStrokeCompleted(
    activeTool: DrawingTool,
    start: Point2D,
    end: Point2D,
    points: List<Point2D>,
    snappedResult: StraightenedResult?,
    project: TraceProject,
    strokeColor: Long,
    strokeWidth: Float,
    strokeStyle: StrokeStyle,
    onElementCreated: (VectorElement) -> Unit
) {
    val activeLayerId = project.activeLayerId

    // 1. If snapped by Hold-to-Straighten
    if (snappedResult != null) {
        when (snappedResult) {
            is StraightenedResult.Line -> {
                onElementCreated(
                    LineElement(
                        layerId = activeLayerId,
                        start = snappedResult.start,
                        end = snappedResult.end,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth,
                        style = strokeStyle,
                        showDimension = true
                    )
                )
            }
            is StraightenedResult.CircleOrEllipse -> {
                onElementCreated(
                    EllipseElement(
                        layerId = activeLayerId,
                        centerX = snappedResult.centerX,
                        centerY = snappedResult.centerY,
                        radiusX = snappedResult.radiusX,
                        radiusY = snappedResult.radiusY,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth,
                        style = strokeStyle,
                        isFilled = strokeStyle == StrokeStyle.WATERCOLOR_WASH,
                        fillColor = strokeColor
                    )
                )
            }
            is StraightenedResult.Rectangle -> {
                onElementCreated(
                    RectangleElement(
                        layerId = activeLayerId,
                        left = snappedResult.left,
                        top = snappedResult.top,
                        right = snappedResult.right,
                        bottom = snappedResult.bottom,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth,
                        style = strokeStyle,
                        isFilled = strokeStyle == StrokeStyle.WATERCOLOR_WASH,
                        fillColor = strokeColor
                    )
                )
            }
        }
        return
    }

    // 2. Normal tool completions
    when (activeTool) {
        DrawingTool.PEN -> {
            if (points.size > 1) {
                // Auto-detect closed loop for dynamic watercolor fill
                val isClosed = points.size >= 8 && points.first().distanceTo(points.last()) < 35f
                onElementCreated(
                    FreehandPath(
                        layerId = activeLayerId,
                        points = points,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth,
                        style = strokeStyle,
                        isClosed = isClosed,
                        fillColor = if (isClosed && strokeStyle == StrokeStyle.WATERCOLOR_WASH) strokeColor else null
                    )
                )
            }
        }

        DrawingTool.LINE -> {
            if (start.distanceTo(end) > 6f) {
                onElementCreated(
                    LineElement(
                        layerId = activeLayerId,
                        start = start,
                        end = end,
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth,
                        style = strokeStyle,
                        showDimension = true
                    )
                )
            }
        }

        DrawingTool.RECTANGLE -> {
            if (abs(end.x - start.x) > 6f && abs(end.y - start.y) > 6f) {
                onElementCreated(
                    RectangleElement(
                        layerId = activeLayerId,
                        left = min(start.x, end.x),
                        top = min(start.y, end.y),
                        right = max(start.x, end.x),
                        bottom = max(start.y, end.y),
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth,
                        style = strokeStyle,
                        isFilled = strokeStyle == StrokeStyle.WATERCOLOR_WASH,
                        fillColor = strokeColor
                    )
                )
            }
        }

        DrawingTool.ELLIPSE -> {
            val dist = start.distanceTo(end)
            if (dist > 6f) {
                val rx = abs(end.x - start.x)
                val ry = abs(end.y - start.y)
                onElementCreated(
                    EllipseElement(
                        layerId = activeLayerId,
                        centerX = start.x,
                        centerY = start.y,
                        radiusX = max(rx, 6f),
                        radiusY = max(ry, 6f),
                        strokeColor = strokeColor,
                        strokeWidth = strokeWidth,
                        style = strokeStyle,
                        isFilled = strokeStyle == StrokeStyle.WATERCOLOR_WASH,
                        fillColor = strokeColor
                    )
                )
            }
        }

        DrawingTool.MEASURE -> {
            val dist = start.distanceTo(end)
            if (dist > 6f) {
                val label = if (project.scaleCalibration.isCalibrated) {
                    project.scaleCalibration.formatMeasurement(dist)
                } else {
                    "${dist.roundToInt()} px"
                }
                onElementCreated(
                    DimensionMarkup(
                        layerId = activeLayerId,
                        start = start,
                        end = end,
                        label = label,
                        strokeColor = 0xFFDC2626 // Drafting red
                    )
                )
            }
        }

        else -> Unit
    }
}

/**
 * Erases any vector element touched by the eraser
 */
private fun eraseAtPoint(
    point: Point2D,
    elements: List<VectorElement>,
    onElementsDeleted: (Set<String>) -> Unit
) {
    val eraseRadius = 26f
    val hitIds = elements.filter { it.distanceToPoint(point) <= eraseRadius }.map { it.id }.toSet()
    if (hitIds.isNotEmpty()) {
        onElementsDeleted(hitIds)
    }
}

/**
 * Renders the snapped geometric shape while holding the pen down
 */
private fun renderSnappedResultPreview(
    canvas: android.graphics.Canvas,
    snapped: StraightenedResult,
    color: Long,
    strokeWidth: Float,
    style: StrokeStyle
) {
    val snapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = AndroidColor.parseColor("#0284C7") // Clean cyan snap highlight
        this.strokeWidth = strokeWidth * 1.25f
        this.style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    when (snapped) {
        is StraightenedResult.Line -> {
            canvas.drawLine(snapped.start.x, snapped.start.y, snapped.end.x, snapped.end.y, snapPaint)
        }
        is StraightenedResult.CircleOrEllipse -> {
            val rect = RectF(
                snapped.centerX - snapped.radiusX,
                snapped.centerY - snapped.radiusY,
                snapped.centerX + snapped.radiusX,
                snapped.centerY + snapped.radiusY
            )
            canvas.drawOval(rect, snapPaint)
        }
        is StraightenedResult.Rectangle -> {
            val rect = RectF(snapped.left, snapped.top, snapped.right, snapped.bottom)
            canvas.drawRect(rect, snapPaint)
        }
    }
}

/**
 * Renders in-progress freehand stroke
 */
private fun renderCurrentFreehand(
    canvas: android.graphics.Canvas,
    points: List<Point2D>,
    color: Long,
    strokeWidth: Float,
    style: StrokeStyle
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toInt()
        this.strokeWidth = strokeWidth
        this.style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val path = Path()
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        path.lineTo(points[i].x, points[i].y)
    }
    canvas.drawPath(path, paint)
}

/**
 * Renders interactive shape preview (line, rect, ellipse, caliper)
 */
private fun renderActiveShapePreview(
    canvas: android.graphics.Canvas,
    tool: DrawingTool,
    start: Point2D,
    end: Point2D,
    color: Long,
    strokeWidth: Float,
    style: StrokeStyle,
    isCalibrating: Boolean
) {
    if (isCalibrating) {
        // Calibration Caliper line with crosshairs
        val calibPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.parseColor("#E11D48") // Rose red
            this.strokeWidth = 3f
            this.style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }
        canvas.drawLine(start.x, start.y, end.x, end.y, calibPaint)
        canvas.drawCircle(start.x, start.y, 7f, calibPaint)
        canvas.drawCircle(end.x, end.y, 7f, calibPaint)
        return
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toInt()
        this.strokeWidth = strokeWidth
        this.style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        if (style == StrokeStyle.CONSTRUCTION) {
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }
    }

    when (tool) {
        DrawingTool.LINE, DrawingTool.MEASURE -> {
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
        DrawingTool.RECTANGLE -> {
            val rect = RectF(min(start.x, end.x), min(start.y, end.y), max(start.x, end.x), max(start.y, end.y))
            canvas.drawRect(rect, paint)
        }
        DrawingTool.ELLIPSE -> {
            val rx = abs(end.x - start.x)
            val ry = abs(end.y - start.y)
            val rect = RectF(start.x - rx, start.y - ry, start.x + rx, start.y + ry)
            canvas.drawOval(rect, paint)
        }
        else -> Unit
    }
}

/**
 * Draws blueprint grid on blank tracing canvas
 */
private fun drawArchitecturalGrid(canvas: android.graphics.Canvas, width: Float, height: Float) {
    val majorPaint = Paint().apply {
        color = AndroidColor.parseColor("#E2E8F0")
        strokeWidth = 1.2f
    }
    val minorPaint = Paint().apply {
        color = AndroidColor.parseColor("#F1F5F9")
        strokeWidth = 0.8f
    }

    val minorStep = 25f
    val majorStep = 100f

    var x = 0f
    while (x <= width) {
        val paint = if ((x % majorStep).roundToInt() == 0) majorPaint else minorPaint
        canvas.drawLine(x, 0f, x, height, paint)
        x += minorStep
    }

    var y = 0f
    while (y <= height) {
        val paint = if ((y % majorStep).roundToInt() == 0) majorPaint else minorPaint
        canvas.drawLine(0f, y, width, y, paint)
        y += minorStep
    }
}
