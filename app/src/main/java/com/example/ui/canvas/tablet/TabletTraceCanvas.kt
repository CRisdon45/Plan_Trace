package com.example.ui.canvas.tablet

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import com.example.model.StrokeStyle
import com.example.model.TraceProject
import com.example.model.VectorElement
import com.example.ui.DrawingTool
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Tablet-first drawing surface.
 *
 * Input ownership is intentionally explicit:
 * - A real stylus/eraser owns the gesture whenever one is present. Finger/palm contacts
 *   arriving during that stroke are consumed and never become pan gestures.
 * - In Pen-only mode, one finger pans and two fingers pan/zoom.
 * - With finger drawing enabled, one finger draws and two fingers pan/zoom.
 * - The explicit Pan tool pans with either pen or finger.
 *
 * This avoids Compose transform detection competing with the S Pen for the same gesture.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TraceCanvas(
    modifier: Modifier = Modifier,
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
    onElementsDeleted: (Set<String>) -> Unit,
    onElementDuplicated: (String) -> Unit,
    onElementSelected: (String?) -> Unit,
    onElementHeldForLayer: (VectorElement) -> Unit = {},
    onColorSampled: (Long) -> Unit,
    onCalibrationSegmentDrawn: (Point2D, Point2D) -> Unit,
    onTextRequested: (Point2D) -> Unit,
    onShowRadialPalette: (Offset) -> Unit,
    onHideRadialPalette: () -> Unit,
    onQuickUndo: () -> Unit = {},
    onFeedbackMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var navSinglePrevious by remember { mutableStateOf<Offset?>(null) }
    var navCentroidPrevious by remember { mutableStateOf<Offset?>(null) }
    var navSpanPrevious by remember { mutableFloatStateOf(0f) }

    val currentPoints = remember { mutableStateListOf<Point2D>() }
    val polylinePoints = remember { mutableStateListOf<Point2D>() }
    var currentStart by remember { mutableStateOf<Point2D?>(null) }
    var currentEnd by remember { mutableStateOf<Point2D?>(null) }
    var dragPrevious by remember { mutableStateOf<Point2D?>(null) }

    var holdJob by remember { mutableStateOf<Job?>(null) }
    var snappedResult by remember { mutableStateOf<StraightenedResult?>(null) }
    var holdLocked by remember { mutableStateOf(false) }
    var activeSnap by remember { mutableStateOf<SnapResult?>(null) }

    var hoverScreen by remember { mutableStateOf<Offset?>(null) }
    var liveMeasurement by remember { mutableStateOf<String?>(null) }
    var lastBarrelPressMs by remember { mutableStateOf(0L) }
    var barrelWasDown by remember { mutableStateOf(false) }
    var lastPolylineTapMs by remember { mutableStateOf(0L) }

    fun screenToWorld(screen: Offset, pressure: Float = 1f): Point2D = Point2D(
        x = (screen.x - panOffset.x) / zoomScale,
        y = (screen.y - panOffset.y) / zoomScale,
        pressure = pressure
    )

    fun worldToScreen(point: Point2D): Offset = Offset(
        point.x * zoomScale + panOffset.x,
        point.y * zoomScale + panOffset.y
    )

    fun resetNavigation() {
        navSinglePrevious = null
        navCentroidPrevious = null
        navSpanPrevious = 0f
    }

    fun cancelDrawing() {
        holdJob?.cancel()
        currentPoints.clear()
        currentStart = null
        currentEnd = null
        snappedResult = null
        holdLocked = false
        activeSnap = null
        dragPrevious = null
        liveMeasurement = null
    }

    fun centroid(event: MotionEvent, indices: List<Int>): Offset {
        var x = 0f
        var y = 0f
        indices.forEach { index ->
            x += event.getX(index)
            y += event.getY(index)
        }
        return Offset(x / indices.size, y / indices.size)
    }

    fun span(event: MotionEvent, indices: List<Int>, center: Offset): Float {
        if (indices.size < 2) return 0f
        var total = 0f
        indices.forEach { index ->
            total += hypot(event.getX(index) - center.x, event.getY(index) - center.y)
        }
        return total / indices.size
    }

    fun navigationIndices(event: MotionEvent, requested: List<Int>): List<Int> {
        if (event.actionMasked != MotionEvent.ACTION_POINTER_UP) return requested
        return requested.filter { it != event.actionIndex }
    }

    fun handleNavigation(event: MotionEvent, requestedIndices: List<Int>): Boolean {
        val indices = navigationIndices(event, requestedIndices)
        val action = event.actionMasked

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP || indices.isEmpty()) {
            resetNavigation()
            return true
        }

        if (indices.size == 1) {
            val index = indices.first()
            val position = Offset(event.getX(index), event.getY(index))
            when (action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN,
                MotionEvent.ACTION_POINTER_UP -> navSinglePrevious = position

                MotionEvent.ACTION_MOVE -> {
                    val previous = navSinglePrevious
                    if (previous != null) {
                        panOffset = Offset(
                            panOffset.x + position.x - previous.x,
                            panOffset.y + position.y - previous.y
                        )
                    }
                    navSinglePrevious = position
                }
            }
            navCentroidPrevious = null
            navSpanPrevious = 0f
            return true
        }

        val center = centroid(event, indices)
        val currentSpan = span(event, indices, center)
        when (action) {
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_DOWN -> {
                navCentroidPrevious = center
                navSpanPrevious = currentSpan
                navSinglePrevious = null
            }

            MotionEvent.ACTION_MOVE -> {
                val previousCenter = navCentroidPrevious
                val previousSpan = navSpanPrevious
                if (previousCenter != null && previousSpan > 0.01f && currentSpan > 0.01f) {
                    val oldZoom = zoomScale
                    val ratio = (currentSpan / previousSpan).coerceIn(0.5f, 2f)
                    val newZoom = (oldZoom * ratio).coerceIn(0.20f, 30f)
                    val worldX = (previousCenter.x - panOffset.x) / oldZoom
                    val worldY = (previousCenter.y - panOffset.y) / oldZoom
                    panOffset = Offset(
                        center.x - worldX * newZoom,
                        center.y - worldY * newZoom
                    )
                    zoomScale = newZoom
                }
                navCentroidPrevious = center
                navSpanPrevious = currentSpan
                navSinglePrevious = null
            }
        }
        return true
    }

    fun sampleColor(point: Point2D): Long {
        val element = project.elements.asReversed().firstOrNull { it.isPointInside(point) }
        if (element != null) {
            when (element) {
                is RectangleElement -> if (element.isFilled) return element.fillColor
                is EllipseElement -> if (element.isFilled) return element.fillColor
                is FreehandPath -> element.fillColor?.let { return it }
                is PolylineElement -> element.fillColor?.let { return it }
                else -> Unit
            }
            return element.strokeColor
        }
        backgroundBitmap?.let { bitmap ->
            val x = point.x.toInt()
            val y = point.y.toInt()
            if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                return bitmap.getPixel(x, y).toLong() and 0xffffffffL
            }
        }
        return 0xff0f172aL
    }

    fun snapped(point: Point2D, start: Point2D? = null): Point2D {
        val visibleLayers = project.layers.filter { it.isVisible }.map { it.id }.toSet()
        val result = GeometrySnapEngine.findBestSnap(
            rawPoint = point,
            elements = project.elements,
            visibleLayerIds = visibleLayers,
            zoomScale = zoomScale,
            settings = snapSettings,
            strokeStart = start
        )
        activeSnap = result
        return result?.snappedPoint?.copy(pressure = point.pressure) ?: point
    }

    fun beginHoldToStraighten() {
        holdJob?.cancel()
        holdJob = scope.launch {
            delay(400)
            if (currentPoints.size >= 5 && !holdLocked) {
                HoldToStraightenDetector.analyze(currentPoints.toList())?.let { result ->
                    snappedResult = result
                    holdLocked = true
                    onFeedbackMessage(
                        when (result) {
                            is StraightenedResult.Line -> "Straightened line"
                            is StraightenedResult.CircleOrEllipse -> "Straightened ellipse"
                            is StraightenedResult.Rectangle -> "Straightened rectangle"
                        }
                    )
                }
            }
        }
    }

    fun activeLayerLocked(): Boolean = project.layers.firstOrNull { it.id == project.activeLayerId }?.isLocked == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xfff9f7f2))
            .pointerInteropFilter { event ->
                val action = event.actionMasked
                val pointerCount = event.pointerCount
                if (pointerCount <= 0) return@pointerInteropFilter false

                val stylusIndex = (0 until pointerCount).firstOrNull { index ->
                    val type = event.getToolType(index)
                    type == MotionEvent.TOOL_TYPE_STYLUS || type == MotionEvent.TOOL_TYPE_ERASER
                }
                val actionIndex = event.actionIndex.coerceIn(0, pointerCount - 1)
                val actionType = event.getToolType(actionIndex)
                val actionIsStylus = actionType == MotionEvent.TOOL_TYPE_STYLUS || actionType == MotionEvent.TOOL_TYPE_ERASER

                if (action == MotionEvent.ACTION_HOVER_ENTER || action == MotionEvent.ACTION_HOVER_MOVE) {
                    if (stylusIndex != null) hoverScreen = Offset(event.getX(stylusIndex), event.getY(stylusIndex))
                    return@pointerInteropFilter true
                }
                if (action == MotionEvent.ACTION_HOVER_EXIT) {
                    hoverScreen = null
                    return@pointerInteropFilter true
                }

                if (stylusIndex != null &&
                    (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP) &&
                    !actionIsStylus
                ) {
                    return@pointerInteropFilter true
                }

                val buttonDown = stylusIndex != null &&
                    (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
                if (buttonDown && !barrelWasDown) {
                    barrelWasDown = true
                    val now = System.currentTimeMillis()
                    if (now - lastBarrelPressMs in 50..380) {
                        onQuickUndo()
                        lastBarrelPressMs = 0L
                        onHideRadialPalette()
                    } else {
                        lastBarrelPressMs = now
                        onShowRadialPalette(Offset(event.getX(stylusIndex!!), event.getY(stylusIndex)))
                    }
                } else if (!buttonDown && barrelWasDown) {
                    barrelWasDown = false
                    onHideRadialPalette()
                }

                val touchIndices = (0 until pointerCount).filter { index ->
                    val type = event.getToolType(index)
                    type != MotionEvent.TOOL_TYPE_STYLUS && type != MotionEvent.TOOL_TYPE_ERASER
                }

                if (stylusIndex == null && touchIndices.size >= 2) {
                    if (action == MotionEvent.ACTION_POINTER_DOWN) cancelDrawing()
                    return@pointerInteropFilter handleNavigation(event, touchIndices)
                }

                if (stylusIndex == null && (stylusOnlyMode || activeTool == DrawingTool.PAN)) {
                    cancelDrawing()
                    return@pointerInteropFilter handleNavigation(event, touchIndices.ifEmpty { listOf(0) })
                }

                if (stylusIndex != null && activeTool == DrawingTool.PAN) {
                    cancelDrawing()
                    return@pointerInteropFilter handleNavigation(event, listOf(stylusIndex))
                }

                resetNavigation()
                val inputIndex = stylusIndex ?: 0
                val isHardwareEraser = event.getToolType(inputIndex) == MotionEvent.TOOL_TYPE_ERASER
                val effectiveTool = if (isHardwareEraser) DrawingTool.ERASER else activeTool
                val pressure = event.getPressure(inputIndex).coerceIn(0.1f, 1f)
                val screen = Offset(event.getX(inputIndex), event.getY(inputIndex))
                val rawWorld = screenToWorld(screen, pressure)

                val drawingAction = when {
                    action == MotionEvent.ACTION_POINTER_DOWN && actionIsStylus -> MotionEvent.ACTION_DOWN
                    action == MotionEvent.ACTION_POINTER_UP && actionIsStylus -> MotionEvent.ACTION_UP
                    else -> action
                }

                when (drawingAction) {
                    MotionEvent.ACTION_DOWN -> {
                        hoverScreen = null
                        activeSnap = null
                        snappedResult = null
                        holdLocked = false
                        currentPoints.clear()

                        if (activeLayerLocked() && effectiveTool !in setOf(
                                DrawingTool.SELECT, DrawingTool.EYEDROPPER, DrawingTool.PAN
                            )) {
                            onFeedbackMessage("Active layer is locked")
                            return@pointerInteropFilter true
                        }

                        val startPoint = if (effectiveTool in setOf(
                                DrawingTool.LINE, DrawingTool.RECTANGLE, DrawingTool.ELLIPSE,
                                DrawingTool.MEASURE, DrawingTool.DIMENSION, DrawingTool.POLYLINE
                            )) snapped(rawWorld) else rawWorld

                        currentStart = startPoint
                        currentEnd = startPoint
                        currentPoints.add(startPoint)

                        when (effectiveTool) {
                            DrawingTool.PEN -> beginHoldToStraighten()
                            DrawingTool.TEXT -> onTextRequested(startPoint)
                            DrawingTool.EYEDROPPER -> onColorSampled(sampleColor(startPoint))
                            DrawingTool.WATERCOLOR_FILL -> {
                                project.elements.asReversed().firstOrNull { it.isPointInside(startPoint) }?.let {
                                    onElementUpdated(it.withFillColor(strokeColor))
                                }
                            }
                            DrawingTool.SELECT -> {
                                val hit = project.elements.asReversed().firstOrNull { it.isPointInside(startPoint) }
                                onElementSelected(hit?.id)
                                dragPrevious = startPoint
                            }
                            DrawingTool.ERASER -> {
                                val ids = project.elements.filter { it.distanceToPoint(startPoint) <= 26f }.map { it.id }.toSet()
                                if (ids.isNotEmpty()) onElementsDeleted(ids)
                            }
                            else -> Unit
                        }
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val world = if (effectiveTool in setOf(
                                DrawingTool.LINE, DrawingTool.RECTANGLE, DrawingTool.ELLIPSE,
                                DrawingTool.MEASURE, DrawingTool.DIMENSION, DrawingTool.POLYLINE
                            )) snapped(rawWorld, currentStart) else rawWorld
                        currentEnd = world

                        when (effectiveTool) {
                            DrawingTool.PEN -> {
                                val last = currentPoints.lastOrNull()
                                if (last == null || last.distanceTo(world) > (2.5f / zoomScale).coerceAtLeast(0.6f)) {
                                    currentPoints.add(world)
                                    if (holdLocked && snappedResult != null) {
                                        val first = currentPoints.first()
                                        snappedResult = when (val result = snappedResult!!) {
                                            is StraightenedResult.Line -> StraightenedResult.Line(first, world)
                                            is StraightenedResult.Rectangle -> StraightenedResult.Rectangle(
                                                min(first.x, world.x), min(first.y, world.y),
                                                max(first.x, world.x), max(first.y, world.y)
                                            )
                                            is StraightenedResult.CircleOrEllipse -> StraightenedResult.CircleOrEllipse(
                                                result.centerX, result.centerY,
                                                max(abs(world.x - result.centerX), 10f),
                                                max(abs(world.y - result.centerY), 10f)
                                            )
                                        }
                                    } else {
                                        beginHoldToStraighten()
                                    }
                                }
                            }
                            DrawingTool.SELECT -> {
                                val previous = dragPrevious
                                val selected = selectedElementId?.let { id -> project.elements.firstOrNull { it.id == id } }
                                if (previous != null && selected != null) {
                                    onElementUpdated(selected.translate(world.x - previous.x, world.y - previous.y))
                                    dragPrevious = world
                                }
                            }
                            DrawingTool.ERASER -> {
                                val ids = project.elements.filter { it.distanceToPoint(world) <= 26f }.map { it.id }.toSet()
                                if (ids.isNotEmpty()) onElementsDeleted(ids)
                            }
                            DrawingTool.EYEDROPPER -> onColorSampled(sampleColor(world))
                            else -> Unit
                        }

                        val start = currentStart
                        if (start != null && project.scaleCalibration.isCalibrated) {
                            liveMeasurement = when (effectiveTool) {
                                DrawingTool.LINE, DrawingTool.MEASURE, DrawingTool.DIMENSION ->
                                    project.scaleCalibration.formatMeasurement(start.distanceTo(world))
                                DrawingTool.RECTANGLE ->
                                    project.scaleCalibration.formatMeasurement(abs(world.x - start.x)) + " × " +
                                        project.scaleCalibration.formatMeasurement(abs(world.y - start.y))
                                DrawingTool.ELLIPSE ->
                                    "Ø " + project.scaleCalibration.formatMeasurement(start.distanceTo(world) * 2f)
                                else -> null
                            }
                        }
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        holdJob?.cancel()
                        activeSnap = null
                        val start = currentStart
                        val end = currentEnd ?: rawWorld

                        if (isCalibratingScale && start != null && start.distanceTo(end) > 8f) {
                            onCalibrationSegmentDrawn(start, end)
                        } else if (start != null) {
                            when (effectiveTool) {
                                DrawingTool.PEN -> {
                                    val straight = snappedResult
                                    if (straight != null) {
                                        onElementCreated(straight.toElement(project.activeLayerId, strokeColor, strokeWidth, strokeStyle))
                                    } else if (currentPoints.size > 1) {
                                        val closed = currentPoints.size >= 8 && currentPoints.first().distanceTo(currentPoints.last()) < 28f
                                        onElementCreated(
                                            FreehandPath(
                                                layerId = project.activeLayerId,
                                                points = currentPoints.toList(),
                                                strokeColor = strokeColor,
                                                strokeWidth = strokeWidth,
                                                style = strokeStyle,
                                                isClosed = closed,
                                                fillColor = if (closed && strokeStyle == StrokeStyle.WATERCOLOR_WASH) strokeColor else null
                                            )
                                        )
                                    }
                                }
                                DrawingTool.LINE -> if (start.distanceTo(end) > 4f) {
                                    onElementCreated(LineElement(layerId = project.activeLayerId, start = start, end = end,
                                        strokeColor = strokeColor, strokeWidth = strokeWidth, style = strokeStyle))
                                }
                                DrawingTool.RECTANGLE -> if (abs(end.x - start.x) > 4f && abs(end.y - start.y) > 4f) {
                                    onElementCreated(RectangleElement(layerId = project.activeLayerId,
                                        left = min(start.x, end.x), top = min(start.y, end.y),
                                        right = max(start.x, end.x), bottom = max(start.y, end.y),
                                        strokeColor = strokeColor, strokeWidth = strokeWidth, style = strokeStyle,
                                        isFilled = strokeStyle == StrokeStyle.WATERCOLOR_WASH,
                                        fillColor = strokeColor))
                                }
                                DrawingTool.ELLIPSE -> if (start.distanceTo(end) > 4f) {
                                    onElementCreated(EllipseElement(layerId = project.activeLayerId,
                                        centerX = start.x, centerY = start.y,
                                        radiusX = max(abs(end.x - start.x), 4f),
                                        radiusY = max(abs(end.y - start.y), 4f),
                                        strokeColor = strokeColor, strokeWidth = strokeWidth, style = strokeStyle,
                                        isFilled = strokeStyle == StrokeStyle.WATERCOLOR_WASH,
                                        fillColor = strokeColor))
                                }
                                DrawingTool.MEASURE, DrawingTool.DIMENSION -> if (start.distanceTo(end) > 4f) {
                                    val label = if (project.scaleCalibration.isCalibrated)
                                        project.scaleCalibration.formatMeasurement(start.distanceTo(end))
                                    else "${start.distanceTo(end).roundToInt()} px"
                                    onElementCreated(DimensionMarkup(layerId = project.activeLayerId, start = start, end = end, label = label))
                                }
                                DrawingTool.POLYLINE -> {
                                    val now = System.currentTimeMillis()
                                    val point = end
                                    val finish = polylinePoints.size >= 2 &&
                                        now - lastPolylineTapMs in 40..360 &&
                                        polylinePoints.last().distanceTo(point) < (28f / zoomScale).coerceAtLeast(5f)
                                    if (finish) {
                                        onElementCreated(PolylineElement(layerId = project.activeLayerId,
                                            points = polylinePoints.toList(), strokeColor = strokeColor,
                                            strokeWidth = strokeWidth, style = strokeStyle))
                                        polylinePoints.clear()
                                        currentEnd = null
                                    } else {
                                        polylinePoints.add(point)
                                        currentEnd = point
                                    }
                                    lastPolylineTapMs = now
                                }
                                else -> Unit
                            }
                        }

                        if (effectiveTool != DrawingTool.POLYLINE) {
                            currentStart = null
                            currentEnd = null
                        }
                        currentPoints.clear()
                        snappedResult = null
                        holdLocked = false
                        dragPrevious = null
                        liveMeasurement = null
                        true
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        cancelDrawing()
                        true
                    }

                    else -> true
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { composeCanvas ->
                val canvas = composeCanvas.nativeCanvas
                canvas.save()
                canvas.translate(panOffset.x, panOffset.y)
                canvas.scale(zoomScale, zoomScale)

                backgroundBitmap?.let { bitmap ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        alpha = (project.backgroundOpacity * 255).toInt().coerceIn(0, 255)
                        isFilterBitmap = true
                    }
                    canvas.drawBitmap(bitmap, 0f, 0f, paint)
                } ?: drawGrid(canvas, 3200f, 2400f)

                project.layers.filter { it.isVisible }.forEach { layer ->
                    project.elements.filter { it.layerId == layer.id }.forEach { element ->
                        WatercolorRenderer.render(
                            canvas = canvas,
                            element = element,
                            layerAlpha = layer.opacity,
                            scale = project.scaleCalibration,
                            showDimensions = showDimensions,
                            isSelected = element.id == selectedElementId
                        )
                    }
                }

                when {
                    snappedResult != null -> drawStraightened(canvas, snappedResult!!, strokeWidth)
                    activeTool == DrawingTool.PEN && currentPoints.size > 1 ->
                        drawFreehand(canvas, currentPoints, strokeColor, strokeWidth)
                    currentStart != null && currentEnd != null ->
                        drawShapePreview(canvas, activeTool, currentStart!!, currentEnd!!,
                            strokeColor, strokeWidth, strokeStyle, isCalibratingScale)
                }

                if (polylinePoints.isNotEmpty()) {
                    val paint = previewPaint(strokeColor, strokeWidth, strokeStyle)
                    val path = Path().apply {
                        moveTo(polylinePoints.first().x, polylinePoints.first().y)
                        polylinePoints.drop(1).forEach { lineTo(it.x, it.y) }
                        currentEnd?.let { lineTo(it.x, it.y) }
                    }
                    canvas.drawPath(path, paint)
                }

                activeSnap?.let { result ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = AndroidColor.rgb(2, 132, 199)
                        style = Paint.Style.STROKE
                        this.strokeWidth = 2f / zoomScale
                    }
                    canvas.drawCircle(result.snappedPoint.x, result.snappedPoint.y, 9f / zoomScale, paint)
                }

                canvas.restore()
            }
        }

        AnimatedVisibility(
            visible = liveMeasurement != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            ) {
                Text(
                    text = liveMeasurement ?: "",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        selectedElementId?.let { selectedId ->
            if (activeTool == DrawingTool.SELECT) {
                project.elements.firstOrNull { it.id == selectedId }?.let { selected ->
                    val bounds = selected.boundingBox()
                    val anchor = worldToScreen(Point2D(bounds.centerX(), bounds.top))
                    Surface(
                        modifier = Modifier.offset {
                            IntOffset((anchor.x - 100).roundToInt().coerceAtLeast(8),
                                (anchor.y - 64).roundToInt().coerceAtLeast(8))
                        },
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp
                    ) {
                        Row {
                            IconButton(onClick = { onElementsDeleted(setOf(selectedId)) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                            IconButton(onClick = { onElementDuplicated(selectedId) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                            }
                            IconButton(onClick = { onElementUpdated(selected.withStrokeColor(strokeColor)) }) {
                                Icon(Icons.Default.Palette, contentDescription = "Use current color")
                            }
                            IconButton(onClick = { onElementUpdated(selected.withFillColor(strokeColor)) }) {
                                Icon(Icons.Default.FormatColorFill, contentDescription = "Fill")
                            }
                        }
                    }
                }
            }
        }

        hoverScreen?.let { screen ->
            val radiusPx = (strokeWidth * zoomScale / 2f).coerceIn(5f, 30f)
            val density = LocalDensity.current
            val sizeDp = with(density) { (radiusPx * 2f).toDp() }
            Box(
                modifier = Modifier
                    .offset { IntOffset((screen.x - radiusPx).roundToInt(), (screen.y - radiusPx).roundToInt()) }
                    .size(sizeDp)
                    .clip(CircleShape)
                    .border(1.dp, Color(strokeColor).copy(alpha = 0.8f), CircleShape)
            )
        }
    }
}

private fun StraightenedResult.toElement(
    layerId: String,
    color: Long,
    width: Float,
    style: StrokeStyle
): VectorElement = when (this) {
    is StraightenedResult.Line -> LineElement(layerId = layerId, start = start, end = end,
        strokeColor = color, strokeWidth = width, style = style)
    is StraightenedResult.Rectangle -> RectangleElement(layerId = layerId,
        left = left, top = top, right = right, bottom = bottom,
        strokeColor = color, strokeWidth = width, style = style,
        isFilled = style == StrokeStyle.WATERCOLOR_WASH, fillColor = color)
    is StraightenedResult.CircleOrEllipse -> EllipseElement(layerId = layerId,
        centerX = centerX, centerY = centerY, radiusX = radiusX, radiusY = radiusY,
        strokeColor = color, strokeWidth = width, style = style,
        isFilled = style == StrokeStyle.WATERCOLOR_WASH, fillColor = color)
}

private fun previewPaint(color: Long, width: Float, style: StrokeStyle): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toInt()
        strokeWidth = width
        this.style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        if (style == StrokeStyle.CONSTRUCTION) pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

private fun drawFreehand(canvas: android.graphics.Canvas, points: List<Point2D>, color: Long, width: Float) {
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    canvas.drawPath(path, previewPaint(color, width, StrokeStyle.INK))
}

private fun drawStraightened(canvas: android.graphics.Canvas, result: StraightenedResult, width: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(2, 132, 199)
        strokeWidth = width * 1.25f
        style = Paint.Style.STROKE
    }
    when (result) {
        is StraightenedResult.Line -> canvas.drawLine(result.start.x, result.start.y, result.end.x, result.end.y, paint)
        is StraightenedResult.Rectangle -> canvas.drawRect(RectF(result.left, result.top, result.right, result.bottom), paint)
        is StraightenedResult.CircleOrEllipse -> canvas.drawOval(RectF(
            result.centerX - result.radiusX, result.centerY - result.radiusY,
            result.centerX + result.radiusX, result.centerY + result.radiusY), paint)
    }
}

private fun drawShapePreview(
    canvas: android.graphics.Canvas,
    tool: DrawingTool,
    start: Point2D,
    end: Point2D,
    color: Long,
    width: Float,
    style: StrokeStyle,
    calibrating: Boolean
) {
    val paint = previewPaint(if (calibrating) 0xffe11d48L else color, width, style)
    when (tool) {
        DrawingTool.LINE, DrawingTool.MEASURE, DrawingTool.DIMENSION -> canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        DrawingTool.RECTANGLE -> canvas.drawRect(RectF(min(start.x, end.x), min(start.y, end.y),
            max(start.x, end.x), max(start.y, end.y)), paint)
        DrawingTool.ELLIPSE -> {
            val rx = abs(end.x - start.x)
            val ry = abs(end.y - start.y)
            canvas.drawOval(RectF(start.x - rx, start.y - ry, start.x + rx, start.y + ry), paint)
        }
        else -> Unit
    }
}

private fun drawGrid(canvas: android.graphics.Canvas, width: Float, height: Float) {
    val minor = Paint().apply { color = AndroidColor.rgb(241, 245, 249); strokeWidth = 0.8f }
    val major = Paint().apply { color = AndroidColor.rgb(226, 232, 240); strokeWidth = 1.2f }
    var x = 0f
    while (x <= width) {
        canvas.drawLine(x, 0f, x, height, if ((x % 100f).roundToInt() == 0) major else minor)
        x += 25f
    }
    var y = 0f
    while (y <= height) {
        canvas.drawLine(0f, y, width, y, if ((y % 100f).roundToInt() == 0) major else minor)
        y += 25f
    }
}
