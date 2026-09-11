package com.example.ui.workspace

import android.view.MotionEvent
import com.example.MainActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import kotlin.math.ceil
import android.view.ScaleGestureDetector
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.engine.WatercolorRenderer
import com.example.export.DesignOutput
import com.example.export.DesignOutputSettings
import com.example.model.PolylineElement
import com.example.model.design.*
import kotlin.math.hypot
import kotlin.math.roundToInt

private class PointerSession {
    var target: DesignHit? = null
    var document: ProjectDesign? = null
    var down: DesignPoint? = null
    var startView: DesignViewport? = null
    var lastX = 0.0
    var lastY = 0.0
    var downX = 0.0
    var downY = 0.0
    var blockedEdit = false
    var moved = false
    var buttonLatched = false
    var traceAuthoring = false
    fun clear() { target = null; document = null; down = null; startView = null; blockedEdit = false; moved = false; traceAuthoring=false }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun WorkspaceCanvas(state: WorkspaceState, model: DesignWorkspaceViewModel, touchEdit: Boolean,
    showGrid: Boolean, gridSnap: Boolean, commandRequest: Int, inspector: String?, allowCommands: Boolean,
    onCloseInspector: () -> Unit, onAction: (RadialAction) -> Unit,
    onImportSite: () -> Unit, onSiteComplete: () -> Unit, modifier: Modifier) {
    val document = state.shownDocument ?: return
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var size by remember { mutableStateOf(IntSize.Zero) }
    var viewport by remember { mutableStateOf(DesignViewport(60.0, 100.0, 300.0)) }
    val pointer = remember { PointerSession() }
    var menuAnchor by remember { mutableStateOf<Offset?>(null) }
    var editMeasure by remember { mutableStateOf<LiveMeasure?>(null) }
    var lastPosition by remember { mutableStateOf<Offset?>(null) }
    var windowBounds by remember { mutableStateOf(Rect.Zero) }
    fun openCommands(point:Offset) {
        if(!allowCommands) return
        editMeasure=null;model.cancelPreview();pointer.clear();onCloseInspector();menuAnchor=point
    }
    LaunchedEffect(commandRequest) {
        if(commandRequest>0) openCommands(lastPosition ?: Offset(size.width/2f,size.height/2f))
    }
    fun previewEdit(command: DesignCommand, rawTarget: DesignPoint) {
        model.preview(command,pointer.document!!.revision)
        val next=model.state.value.preview
        editMeasure=if(next!=null) LiveMeasurements.editing(pointer.document!!,next,pointer.target!!,pointer.down!!)
            else LiveMeasure(rawTarget,listOf("Invalid edit · not placed"),invalid=true)
    }
    // Generic events are routed only while this canvas is active, using window-local bounds.
    // Hover itself never edits. The button opens a latched, tap-to-choose wheel; release does not execute.
    val genericHandler by rememberUpdatedState<(MotionEvent)->Boolean>({ event ->
        val stylus=event.pointerCount>0 && event.getToolType(0)==MotionEvent.TOOL_TYPE_STYLUS
        if(!stylus || !allowCommands || inspector!=null) false else {
            val down=event.isButtonPressed(MotionEvent.BUTTON_STYLUS_PRIMARY)
            val point=Offset(event.x,event.y)
            val inside=windowBounds.contains(point)
            val invoke=inside && pointer.document==null && !pointer.buttonLatched &&
                ((event.actionMasked==MotionEvent.ACTION_BUTTON_PRESS && event.actionButton==MotionEvent.BUTTON_STYLUS_PRIMARY) || down)
            if(event.actionMasked==MotionEvent.ACTION_HOVER_EXIT) pointer.buttonLatched=false
            else pointer.buttonLatched=down
            if(inside) lastPosition=point-windowBounds.topLeft
            if(inside && !down && menuAnchor==null && state.isDrawing) {
                val at=point-windowBounds.topLeft
                model.previewSiteCorner(viewport.toWorld(at.x.toDouble(),at.y.toDouble()),gridSnap,18.0*density/viewport.pixelsPerMetre)
            }
            if(event.actionMasked==MotionEvent.ACTION_HOVER_EXIT && state.isDrawing) model.previewSiteCorner(null)
            if(invoke) { openCommands(point-windowBounds.topLeft);true }
            else inside && (event.actionMasked==MotionEvent.ACTION_BUTTON_RELEASE || down)
        }
    })
    DisposableEffect(context) {
        val activity=context as? MainActivity
        val handler:(MotionEvent)->Boolean={ genericHandler(it) }
        activity?.workspaceGenericMotionHandler=handler
        onDispose { activity?.let { if(it.workspaceGenericMotionHandler===handler) it.workspaceGenericMotionHandler=null } }
    }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { pointer.clear();pointer.buttonLatched=false;menuAnchor=null;editMeasure=null;model.cancelPreview() }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); pointer.clear(); model.cancelPreview() }
    }
    LaunchedEffect(size, state.fitRequest) {
        menuAnchor=null;editMeasure=null
        if (size.width > 0 && size.height > 0) {
            model.cancelPreview(); pointer.clear()
            viewport = DesignViewport.fit(state.document!!, size.width.toDouble(), size.height.toDouble())
        }
    }
    val detector = remember(context) { ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            viewport = viewport.zoomed(detector.scaleFactor.toDouble(), detector.focusX.toDouble(), detector.focusY.toDouble())
            return true
        }
    }) }
    val projectionResult = remember(document) { runCatching { DesignOutput.drawing(document, DesignOutputSettings(includeMeasurements = false, includeSourceNotice = false)) } }
    val projection = projectionResult.getOrNull()
    val selected = document.objects.firstOrNull { it.id == state.selectedId }
    val radius = 6f * density
    // Route design contacts only through the artwork layer. Overlay controls must receive their
    // own touches rather than becoming source-move/calibration gestures on the parent Box.
    val artworkInput = Modifier.pointerInteropFilter { event ->
            if(menuAnchor!=null || inspector!=null || !allowCommands) return@pointerInteropFilter false
            val x = event.x.toDouble(); val y = event.y.toDouble()
            lastPosition=Offset(event.x,event.y)
            if(state.isDrawing) {
                detector.onTouchEvent(event)
                when(event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        pointer.clear();pointer.document=state.document;pointer.startView=viewport
                        pointer.down=viewport.toWorld(x,y);pointer.lastX=x;pointer.lastY=y
                        pointer.traceAuthoring=touchEdit || event.getToolType(0)==MotionEvent.TOOL_TYPE_STYLUS
                        if(pointer.traceAuthoring) model.previewSiteCorner(pointer.down,gridSnap,18.0*density/viewport.pixelsPerMetre)
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> { pointer.blockedEdit=true;model.previewSiteCorner(null) }
                    MotionEvent.ACTION_MOVE -> {
                        if(pointer.traceAuthoring && !pointer.blockedEdit && event.pointerCount==1) {
                            model.previewSiteCorner(viewport.toWorld(x,y),gridSnap,18.0*density/viewport.pixelsPerMetre)
                        } else if(!pointer.traceAuthoring && !pointer.blockedEdit && !detector.isInProgress && event.pointerCount==1) {
                            viewport=viewport.panned(x-pointer.lastX,y-pointer.lastY)
                        }
                        pointer.lastX=x;pointer.lastY=y
                    }
                    MotionEvent.ACTION_UP -> {
                        if(pointer.traceAuthoring && !pointer.blockedEdit && pointer.document?.revision==state.document?.revision) {
                            model.markSiteCorner(viewport.toWorld(x,y),18.0*density/viewport.pixelsPerMetre,gridSnap)
                        }
                        model.previewSiteCorner(null);pointer.clear()
                    }
                    MotionEvent.ACTION_CANCEL -> { model.previewSiteCorner(null);pointer.clear() }
                }
                return@pointerInteropFilter true
            }
            if(state.siteTool != SiteTool.NONE) {
                val source = state.document?.siteImage ?: return@pointerInteropFilter false
                when(event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        pointer.clear(); model.cancelPreview(); pointer.document=state.document; pointer.startView=viewport
                        pointer.down=viewport.toWorld(x,y);pointer.downX=x;pointer.downY=y;pointer.lastX=x;pointer.lastY=y
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> { pointer.blockedEdit=true;model.cancelPreview() }
                    MotionEvent.ACTION_MOVE -> if(pointer.down!=null && !pointer.blockedEdit) {
                        if(hypot(x-pointer.downX,y-pointer.downY)>3*density) pointer.moved=true
                        if(state.siteTool==SiteTool.MOVE && pointer.moved) {
                            val current=pointer.startView!!.toWorld(x,y)
                            model.preview(DesignCommand.SetSiteImage(source.moved(current.x-pointer.down!!.x,current.y-pointer.down!!.y),source),pointer.document!!.revision)
                        } else if(state.siteTool==SiteTool.CALIBRATE && pointer.moved) {
                            viewport=viewport.panned(x-pointer.lastX,y-pointer.lastY)
                        }
                        pointer.lastX=x;pointer.lastY=y
                    }
                    MotionEvent.ACTION_UP -> {
                        if(!pointer.blockedEdit && pointer.down!=null) {
                            if(state.siteTool==SiteTool.CALIBRATE && !pointer.moved) {
                                model.markSiteReference(viewport.toWorld(x,y))
                                if(model.state.value.siteTool==SiteTool.NONE) onSiteComplete()
                            } else if(state.siteTool==SiteTool.MOVE && pointer.moved) {
                                val current=pointer.startView!!.toWorld(x,y)
                                model.preview(DesignCommand.SetSiteImage(source.moved(current.x-pointer.down!!.x,current.y-pointer.down!!.y),source),pointer.document!!.revision)
                                model.commitPreview();model.stopSiteTool();onSiteComplete()
                            }
                        }
                        pointer.clear()
                    }
                    MotionEvent.ACTION_CANCEL -> { pointer.clear();model.cancelPreview() }
                }
                return@pointerInteropFilter true
            }
            if(event.actionMasked==MotionEvent.ACTION_DOWN && event.getToolType(0)==MotionEvent.TOOL_TYPE_STYLUS &&
                event.isButtonPressed(MotionEvent.BUTTON_STYLUS_PRIMARY)) {
                openCommands(Offset(event.x,event.y));pointer.blockedEdit=true;return@pointerInteropFilter true
            }
            if (pointer.document != null && pointer.document?.revision != state.document?.revision) {
                pointer.clear(); model.cancelPreview()
            }
            if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                editMeasure=null;model.cancelPreview(); pointer.target = null; pointer.blockedEdit = true
            }
            detector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    editMeasure=null;pointer.clear(); model.cancelPreview()
                    pointer.document = state.document; pointer.startView = viewport
                    pointer.down = viewport.toWorld(x, y)
                    pointer.lastX = x; pointer.lastY = y; pointer.downX = x; pointer.downY = y
                    val editing = touchEdit || event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS || event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER
                    if (editing && state.document != null) {
                        pointer.target = DesignPicking.hit(state.document, state.selectedId, pointer.down!!, 22.0 * density / viewport.pixelsPerMetre)
                        model.select(pointer.target?.objectId)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val target = pointer.target
                    if (!pointer.blockedEdit && target != null && event.pointerCount == 1) {
                        if (hypot(x - pointer.downX, y - pointer.downY) > 3 * density) pointer.moved = true
                        if (pointer.moved) previewEdit(DesignPicking.drag(pointer.document!!, target, pointer.down!!, pointer.startView!!.toWorld(x, y)).let { if(gridSnap) GridAssist.snap(it) else it },pointer.startView!!.toWorld(x,y))
                    } else if (!detector.isInProgress && event.pointerCount == 1 && !pointer.blockedEdit) {
                        viewport = viewport.panned(x - pointer.lastX, y - pointer.lastY)
                    }
                    pointer.lastX = x; pointer.lastY = y
                }
                MotionEvent.ACTION_UP -> {
                    if (pointer.target != null && !pointer.blockedEdit &&
                        (pointer.moved || hypot(x - pointer.downX, y - pointer.downY) > 3 * density)) {
                        // Pen-up can contain the last position even without a final move event.
                        previewEdit(DesignPicking.drag(pointer.document!!, pointer.target!!, pointer.down!!, pointer.startView!!.toWorld(x, y)).let { if(gridSnap) GridAssist.snap(it) else it },pointer.startView!!.toWorld(x,y))
                        model.commitPreview()
                    } else model.cancelPreview()
                    editMeasure=null;pointer.clear()
                }
                MotionEvent.ACTION_CANCEL -> { editMeasure=null;model.cancelPreview(); pointer.clear() }
            }
            true
        }
    Box(modifier.onSizeChanged { size = it }.onGloballyPositioned { windowBounds=it.boundsInWindow() }
        .clipToBounds().background(Color(0xFFFBFAF5)).testTag("workspace-canvas")) {
        Canvas(Modifier.fillMaxSize().then(artworkInput)) {
            document.siteImage?.takeIf { it.visible }?.let { source -> state.siteBitmap?.let { bitmap ->
                val a=viewport.toScreen(source.corners()[0]);val b=viewport.toScreen(source.corners()[1])
                drawIntoCanvas { canvas -> canvas.nativeCanvas.drawBitmap(bitmap,null,
                    android.graphics.RectF(a.x.toFloat(),a.y.toFloat(),b.x.toFloat(),b.y.toFloat()),
                    android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)) }
            } }
            if(showGrid) {
                var step=GridAssist.SPACING_METRES*viewport.pixelsPerMetre
                while(step<16*density) step*=2
                val startX=((viewport.offsetX%step)+step)%step
                val startY=((viewport.offsetY%step)+step)%step
                for(i in 0..ceil(size.width/step).toInt().coerceAtMost(250)) {
                    val x=(startX+i*step).toFloat();drawLine(Color(0xFFE1E4DE),Offset(x,0f),Offset(x,size.height.toFloat()),1f)
                }
                for(i in 0..ceil(size.height/step).toInt().coerceAtMost(250)) {
                    val y=(startY+i*step).toFloat();drawLine(Color(0xFFE1E4DE),Offset(0f,y),Offset(size.width.toFloat(),y),1f)
                }
            }
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.save()
                try {
                    native.translate(viewport.offsetX.toFloat(), viewport.offsetY.toFloat())
                    val zoom = (viewport.pixelsPerMetre / 100).toFloat()
                    native.scale(zoom, zoom)
                    projection?.elements?.forEach { element ->
                        val outline = (element as PolylineElement).copy(strokeWidth =
                            (if(element.strokeColor==0xFF596257) 2.3f else 1.6f) * density / zoom,
                            strokeColor = if (element.id == "${state.selectedId}:outline") 0xFF21546D else element.strokeColor)
                        WatercolorRenderer.render(native, outline, 1f, projection.scaleCalibration, showDimensions = false)
                    }
                } finally { native.restore() }
            }
            state.drawingPoints?.let { points ->
                val vertices=points.map { viewport.toScreen(it) }.map { Offset(it.x.toFloat(),it.y.toFloat()) }
                vertices.zipWithNext().forEach { (a,b) -> drawLine(Color(0xFF2F665F),a,b,2*density) }
                state.draftCursor?.let { point -> if(vertices.isNotEmpty()) {
                    val cursor=viewport.toScreen(point)
                    drawLine(Color(0xFF598B84),vertices.last(),Offset(cursor.x.toFloat(),cursor.y.toFloat()),2*density)
                } }
                if(vertices.size>=3) drawLine(Color(0x88637A72),vertices.last(),vertices.first(),density,
                    pathEffect=androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6*density,5*density)))
                vertices.forEachIndexed { i,p ->
                    drawCircle(Color.White,7*density,p);drawCircle(if(i==0) Color(0xFFAD762B) else Color(0xFF2F665F),4.5f*density,p)
                }
            }
            document.siteImage?.let { source -> state.referencePoints.forEach { pixel ->
                val p=viewport.toScreen(source.toWorld(pixel));val at=Offset(p.x.toFloat(),p.y.toFloat())
                drawCircle(Color.White,8*density,at);drawCircle(Color(0xFFC64D22),5*density,at)
            } }
            if (selected != null && !selected.locked && state.siteTool==SiteTool.NONE) {
                selected.boundary.nodes.forEach { n ->
                    val p = viewport.toScreen(n.point)
                    drawCircle(Color.White, radius + 2f * density, Offset(p.x.toFloat(), p.y.toFloat()))
                    drawCircle(Color(0xFF21546D), radius, Offset(p.x.toFloat(), p.y.toFloat()))
                }
                if(selected.siteTrace==null) selected.boundary.edges().forEach { edge ->
                    val p = viewport.toScreen(edge.pointAt(0.5))
                    drawCircle(Color.White, radius + density, Offset(p.x.toFloat(), p.y.toFloat()))
                    drawCircle(Color(0xFFAD762B), radius * 0.75f, Offset(p.x.toFloat(), p.y.toFloat()))
                }
            }
        }
        if (projection == null) Text("This outline cannot be displayed at the current precision: ${projectionResult.exceptionOrNull()?.message.orEmpty()}", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
        // Semantic hit targets coincide with the handles. Pointer handling remains on the parent.
        if (selected != null && !selected.locked) {
            selected.boundary.nodes.forEachIndexed { index, node ->
                val p = viewport.toScreen(node.point)
                Box(Modifier.offset { IntOffset((p.x - 22 * density).roundToInt(), (p.y - 22 * density).roundToInt()) }
                    .size(44.dp).testTag("workspace-vertex-$index").semantics { contentDescription = "Vertex ${index + 1}" })
            }
            if(selected.siteTrace==null) selected.boundary.edges().forEachIndexed { index, edge ->
                val p = viewport.toScreen(edge.pointAt(0.5))
                Box(Modifier.offset { IntOffset((p.x - 22 * density).roundToInt(), (p.y - 22 * density).roundToInt()) }
                    .size(44.dp).testTag("workspace-curve-$index").semantics { contentDescription = "Curve handle ${index + 1}" })
            }
        }
        if(document.siteImage!=null && inspector==null && !state.isDrawing) {
            Surface(Modifier.align(Alignment.TopStart).padding(8.dp),color=Color(0xEEFFFFFF)) {
                Text(state.siteError ?: if(document.siteImage.calibration==null) "Source scale not set · reference only"
                    else "Source scaled from reference · site unverified",modifier=Modifier.padding(8.dp).testTag("site-canvas-status"),
                    style=MaterialTheme.typography.labelMedium)
            }
        }
        if(state.isDrawing) BackHandler { model.stopSiteTool() }
        if(state.siteTool!=SiteTool.NONE) {
            BackHandler { model.stopSiteTool() }
            Surface(Modifier.align(Alignment.BottomCenter).padding(8.dp),tonalElevation=3.dp) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(if(state.siteTool==SiteTool.MOVE) "Drag to move only the source image"
                        else "Tap reference point ${state.referencePoints.size+1} of 2 · drag to pan",Modifier.padding(12.dp))
                    TextButton(onClick={model.stopSiteTool();onSiteComplete()},modifier=Modifier.testTag("site-tool-cancel")) { Text("Cancel") }
                }
            }
        }
        if(menuAnchor==null && inspector==null && allowCommands) {
            val live=state.draftTarget?.let { target -> state.drawingPoints?.let { LiveMeasurements.segment(it,target) } }
                ?: editMeasure?.takeIf { state.preview!=null || it.invalid }
            live?.let { measure ->
                val screen=viewport.toScreen(measure.target)
                val at=Offset(screen.x.toFloat(),screen.y.toFloat())
                Canvas(Modifier.fillMaxSize()) {
                    val color=if(measure.invalid) Color(0xFFAE332A) else Color(0xFF205D60)
                    drawCircle(Color.White,7*density,at)
                    drawLine(color,at-Offset(10*density,0f),at+Offset(10*density,0f),1.5f*density)
                    drawLine(color,at-Offset(0f,10*density),at+Offset(0f,10*density),1.5f*density)
                }
                LiveMeasureOverlay(measure,at)
            }
        }
        if(inspector=="site" || (inspector!=null && selected!=null)) {
            Box(Modifier.fillMaxSize().pointerInput(inspector) { detectTapGestures { onCloseInspector() } })
            BackHandler { onCloseInspector() }
            Box(Modifier.align(Alignment.TopEnd).imePadding().padding(12.dp).widthIn(max=350.dp)) {
                if(inspector=="site") SiteImagePanel(state,model,onImportSite,onCloseInspector,onTool={ tool ->
                    onCloseInspector();model.startSiteTool(tool)
                },onTrace={ role -> onCloseInspector();model.beginSiteOutline(role) })
                else if(inspector=="size") WorkspaceSizePanel(selected!!,model,onCloseInspector)
                else Surface(shape=MaterialTheme.shapes.large,shadowElevation=5.dp,tonalElevation=3.dp,
                    modifier=Modifier.testTag("workspace-coping-panel")) {
                    Column(Modifier.padding(vertical=8.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text("Following coping",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                            TextButton(onClick=onCloseInspector,modifier=Modifier.testTag("workspace-coping-close")) { Text("Close") }
                        }
                        CopingWidthControl(selected!!,model,state.preview==null)
                    }
                }
            }
        }
        menuAnchor?.let { anchor ->
            WorkspaceRadialMenu(anchor,size,RadialAvailability(state.document!=null,selected!=null,
                selected!=null && !selected.locked,
                selected?.kind==DesignObjectKind.POOL || selected?.kind==DesignObjectKind.SPA,
                state.canUndo,state.canRedo,document.objects.isNotEmpty(),showGrid,touchEdit,gridSnap),
                onDismiss={ menuAnchor=null },onAction=onAction)
        }

    }
}
