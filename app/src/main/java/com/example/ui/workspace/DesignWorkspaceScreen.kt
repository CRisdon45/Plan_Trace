package com.example.ui.workspace

import android.content.Context
import android.view.MotionEvent
import com.example.MainActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.geometry.Rect
import kotlin.math.floor
import kotlin.math.ceil
import android.view.ScaleGestureDetector
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.WatercolorRenderer
import com.example.export.DesignOutput
import com.example.export.DesignOutputSettings
import com.example.export.ExportManager
import com.example.model.PolylineElement
import com.example.model.design.*
import com.example.ui.components.ExportDialog
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Opt-in draft workspace in the real application; no duplicate renderer or legacy-data migration. */
@Composable
fun DesignWorkspaceScreen(onBack: () -> Unit, model: DesignWorkspaceViewModel = viewModel()) {
    val state by model.state.collectAsState()
    val preferences = LocalContext.current.getSharedPreferences("workspace-view", Context.MODE_PRIVATE)
    var touchEdit by rememberSaveable { mutableStateOf(preferences.getBoolean("touch", false)) }
    var showGrid by rememberSaveable { mutableStateOf(preferences.getBoolean("grid", false)) }
    var gridSnap by rememberSaveable { mutableStateOf(preferences.getBoolean("snap", false)) }
    var commandRequest by remember { mutableStateOf(0) }
    var inspector by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(touchEdit, showGrid, gridSnap) {
        preferences.edit().putBoolean("touch",touchEdit).putBoolean("grid",showGrid).putBoolean("snap",gridSnap).apply()
    }
    LaunchedEffect(state.selectedId) { inspector=null }
    fun command(action: RadialAction) {
        val doc=model.state.value.document ?: return
        val selected=doc.objects.firstOrNull { it.id==model.state.value.selectedId }
        when(action) {
            RadialAction.POOL -> model.addOutline(false)
            RadialAction.CURVED -> model.addOutline(true)
            RadialAction.PAVING -> model.addOutline(false,DesignObjectKind.PAVING)
            RadialAction.SIZE -> if(selected!=null && !selected.locked) inspector="size"
            RadialAction.COPING -> if(selected!=null && !selected.locked) inspector="coping"
            RadialAction.DELETE -> selected?.let { model.execute(DesignCommand.Remove(it.id)) }
            RadialAction.FIT -> model.fit()
            RadialAction.GRID -> showGrid=!showGrid
            RadialAction.TOUCH -> { model.cancelPreview();touchEdit=!touchEdit }
            RadialAction.SNAP -> gridSnap=!gridSnap
            RadialAction.UNDO -> model.undo()
            RadialAction.REDO -> model.redo()
            RadialAction.CLEAR -> model.select(null)
            RadialAction.NEXT -> if(doc.objects.isNotEmpty()) {
                val index=doc.objects.indexOfFirst { it.id==selected?.id }
                model.select(doc.objects[(index+1)%doc.objects.size].id)
            }
        }
    }
    var exporting by remember { mutableStateOf(false) }
    var leaveUnsaved by remember { mutableStateOf(false) }
    var exportBusy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { model.cancelPreview();inspector=null }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); model.cancelPreview() }
    }
    fun close() {
        model.cancelPreview()
        if (state.document == null || state.saved || state.document!!.revision == 0L) onBack()
        else leaveUnsaved = true
    }
    BackHandler { close() }
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        Column(Modifier.statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { close() }, modifier = Modifier.testTag("workspace-back")) { Text("Back to plans") }
                Text("Design workspace", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { inspector=null;commandRequest++ }, enabled=state.document!=null,
                    modifier=Modifier.testTag("workspace-commands")) { Text("Commands") }
                TextButton(onClick=model::undo,enabled=state.canUndo && state.preview==null,modifier=Modifier.testTag("workspace-undo")) { Text("Undo") }
                TextButton(onClick=model::redo,enabled=state.canRedo && state.preview==null,modifier=Modifier.testTag("workspace-redo")) { Text("Redo") }
                val saveText = when {
                    state.loading -> "Opening…"
                    state.loadError != null -> "Could not open"
                    state.saveError != null -> "Not saved"
                    state.preview != null -> "Previewing edit"
                    state.saved -> "Saved on device"
                    state.document?.revision == 0L -> "One local draft"
                    else -> "Saving…"
                }
                Text(saveText, modifier = Modifier.testTag("workspace-save-status"), style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { exporting = true }, enabled = state.document?.objects?.isNotEmpty() == true &&
                    state.preview == null && !exportBusy, modifier = Modifier.testTag("workspace-export")) { Text("Export") }
            }

        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).navigationBarsPadding()) {
            state.loadError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            state.saveError?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(it, modifier = Modifier.weight(1f).padding(8.dp), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = model::retrySave) { Text("Retry save") }
                }
            }
            val doc = state.shownDocument
            if (doc != null) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${doc.objects.size} objects", modifier = Modifier.testTag("workspace-object-count"), style = MaterialTheme.typography.labelMedium)
                    doc.objects.forEachIndexed { index, obj ->
                        FilterChip(selected = obj.id == state.selectedId, onClick = { model.select(obj.id) },
                            label = { Text(obj.name) }, modifier = Modifier.testTag("workspace-object-$index"))
                    }
                }
                WorkspaceCanvas(state, model, touchEdit, showGrid, gridSnap, commandRequest, inspector,
                    !exporting && !leaveUnsaved, { inspector=null }, ::command,
                    Modifier.weight(1f).fillMaxWidth())
                val selected = doc.objects.firstOrNull { it.id == state.selectedId }

                Text(selected?.let { String.format(Locale.US, "%s · perimeter %.2f ft", it.name, it.boundary.perimeterMetres / 0.3048) }
                    ?: "Add an outline, then select its edge to edit.", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("workspace-selection"))
                Text(state.message ?: "Commands opens the tool wheel. " + (if(touchEdit) "Touch editing on. " else "Pen edits; fingers navigate. ") + (if(gridSnap) "1 ft snap: vertices and moves." else "Round handles reshape; amber handles change curves."),
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Text("Coping follows the pool. Surface checks are resolution-limited; steps, site clearances and Northstar styling are still in development.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            } else if (state.loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
    if (leaveUnsaved) AlertDialog(onDismissRequest = { leaveUnsaved = false }, title = { Text("This change is not saved yet") },
        text = { Text("Stay here to let saving finish or retry a failed save. Leaving does not guarantee unsaved work will survive closing the app.") },
        confirmButton = { TextButton(onClick = { leaveUnsaved = false }) { Text("Stay") } },
        dismissButton = { TextButton(onClick = { leaveUnsaved = false; onBack() }) { Text("Leave") } })
    if (exporting) ExportDialog(projectTitle = "Design workspace", onDismiss = { exporting = false }, onExport = { options, asPdf ->
        val snapshot = state.document
        if (snapshot != null && !exportBusy) {
            exporting = false; exportBusy = true
            scope.launch {
                try {
                    val file = if (asPdf) DesignOutput.pdf(context, snapshot, options)
                        else DesignOutput.png(context, snapshot, settings = DesignOutputSettings(includeMeasurements = options.includeDimensions))
                    if (file == null || !ExportManager.shareExportedFile(context, file, if (asPdf) "application/pdf" else "image/png", "Design workspace"))
                        model.feedback("Export could not be shared. Your editable draft is unchanged.")
                } catch (error: kotlinx.coroutines.CancellationException) { throw error }
                catch (error: Exception) { model.feedback("Export failed: ${error.message.orEmpty()}") }
                finally { exportBusy = false }
            }
        }
    })
}


/** Width entry commits on keyboard Done, without a separate Apply/Accept dialog. */
@Composable
private fun CopingWidthControl(obj: DesignObject, model: DesignWorkspaceViewModel, ready: Boolean) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var text by remember(obj.id, obj.coping) { mutableStateOf(String.format(Locale.US, "%.2f", obj.coping?.widthInches ?: 12.0)) }
    val enabled = ready && !obj.locked
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (obj.coping == null) {
            TextButton(onClick = { model.execute(DesignCommand.SetCoping(obj.id, CopingSpec())) }, enabled = enabled,
                modifier = Modifier.testTag("workspace-attach-coping")) { Text("Add following coping") }
            Text("Existing outline left unchanged until you choose this.", style = MaterialTheme.typography.labelSmall)
        } else {
            OutlinedTextField(value = text, onValueChange = { if (it.length <= 8) text = it },
                label = { Text("Coping width (in)") }, singleLine = true, enabled = enabled,
                modifier = Modifier.width(175.dp).testTag("workspace-coping-width"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val inches = text.trim().toDoubleOrNull()
                    if (inches == null || !inches.isFinite() || inches !in 2.0..48.0) {
                        model.feedback("Enter a coping width from 2 to 48 inches")
                    } else model.execute(DesignCommand.SetCoping(obj.id, CopingSpec(inches * CopingSpec.INCH)))
                    val actual = model.state.value.document?.objects?.firstOrNull { it.id == obj.id }?.coping
                    text = String.format(Locale.US, "%.2f", actual?.widthInches ?: obj.coping.widthInches)
                    focus.clearFocus(); keyboard?.hide()
                }))
            Text(String.format(Locale.US, "Following coping · %.2f in", obj.coping.widthInches),
                style = MaterialTheme.typography.labelMedium, modifier = Modifier.testTag("workspace-coping-status"))
        }
    }
}

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
    fun clear() { target = null; document = null; down = null; startView = null; blockedEdit = false; moved = false }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WorkspaceCanvas(state: WorkspaceState, model: DesignWorkspaceViewModel, touchEdit: Boolean,
    showGrid: Boolean, gridSnap: Boolean, commandRequest: Int, inspector: String?, allowCommands: Boolean,
    onCloseInspector: () -> Unit, onAction: (RadialAction) -> Unit, modifier: Modifier) {
    val document = state.shownDocument ?: return
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var size by remember { mutableStateOf(IntSize.Zero) }
    var viewport by remember { mutableStateOf(DesignViewport(60.0, 100.0, 300.0)) }
    val pointer = remember { PointerSession() }
    var menuAnchor by remember { mutableStateOf<Offset?>(null) }
    var lastPosition by remember { mutableStateOf<Offset?>(null) }
    var windowBounds by remember { mutableStateOf(Rect.Zero) }
    fun openCommands(point:Offset) {
        if(!allowCommands) return
        model.cancelPreview();pointer.clear();onCloseInspector();menuAnchor=point
    }
    LaunchedEffect(commandRequest) {
        if(commandRequest>0) openCommands(lastPosition ?: Offset(size.width/2f,size.height/2f))
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
            if (event == Lifecycle.Event.ON_PAUSE) { pointer.clear();pointer.buttonLatched=false;menuAnchor=null;model.cancelPreview() }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); pointer.clear(); model.cancelPreview() }
    }
    LaunchedEffect(size, state.fitRequest) {
        menuAnchor=null
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
    val projectionResult = remember(document) { runCatching { DesignOutput.drawing(document, DesignOutputSettings(includeMeasurements = false)) } }
    val projection = projectionResult.getOrNull()
    val selected = document.objects.firstOrNull { it.id == state.selectedId }
    val radius = 6f * density
    Box(modifier.onSizeChanged { size = it }.onGloballyPositioned { windowBounds=it.boundsInWindow() }.clipToBounds().background(Color(0xFFFBFAF5))
        .testTag("workspace-canvas").pointerInteropFilter { event ->
            if(menuAnchor!=null || inspector!=null || !allowCommands) return@pointerInteropFilter false
            val x = event.x.toDouble(); val y = event.y.toDouble()
            lastPosition=Offset(event.x,event.y)
            if(event.actionMasked==MotionEvent.ACTION_DOWN && event.getToolType(0)==MotionEvent.TOOL_TYPE_STYLUS &&
                event.isButtonPressed(MotionEvent.BUTTON_STYLUS_PRIMARY)) {
                openCommands(Offset(event.x,event.y));pointer.blockedEdit=true;return@pointerInteropFilter true
            }
            if (pointer.document != null && pointer.document?.revision != state.document?.revision) {
                pointer.clear(); model.cancelPreview()
            }
            if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                model.cancelPreview(); pointer.target = null; pointer.blockedEdit = true
            }
            detector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pointer.clear(); model.cancelPreview()
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
                        if (pointer.moved) model.preview(DesignPicking.drag(pointer.document!!, target, pointer.down!!, pointer.startView!!.toWorld(x, y)).let { if(gridSnap) GridAssist.snap(it) else it }, pointer.document!!.revision)
                    } else if (!detector.isInProgress && event.pointerCount == 1 && !pointer.blockedEdit) {
                        viewport = viewport.panned(x - pointer.lastX, y - pointer.lastY)
                    }
                    pointer.lastX = x; pointer.lastY = y
                }
                MotionEvent.ACTION_UP -> {
                    if (pointer.target != null && !pointer.blockedEdit &&
                        (pointer.moved || hypot(x - pointer.downX, y - pointer.downY) > 3 * density)) {
                        // Pen-up can contain the last position even without a final move event.
                        model.preview(DesignPicking.drag(pointer.document!!, pointer.target!!, pointer.down!!, pointer.startView!!.toWorld(x, y)).let { if(gridSnap) GridAssist.snap(it) else it }, pointer.document!!.revision)
                        model.commitPreview()
                    } else model.cancelPreview()
                    pointer.clear()
                }
                MotionEvent.ACTION_CANCEL -> { model.cancelPreview(); pointer.clear() }
            }
            true
        }) {
        Canvas(Modifier.fillMaxSize()) {
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
                        val outline = (element as PolylineElement).copy(strokeWidth = 1.6f * density / zoom,
                            strokeColor = if (element.id == "${state.selectedId}:outline") 0xFF21546D else 0xFF3B4548)
                        WatercolorRenderer.render(native, outline, 1f, projection.scaleCalibration, showDimensions = false)
                    }
                } finally { native.restore() }
            }
            if (selected != null && !selected.locked) {
                selected.boundary.nodes.forEach { n ->
                    val p = viewport.toScreen(n.point)
                    drawCircle(Color.White, radius + 2f * density, Offset(p.x.toFloat(), p.y.toFloat()))
                    drawCircle(Color(0xFF21546D), radius, Offset(p.x.toFloat(), p.y.toFloat()))
                }
                selected.boundary.edges().forEach { edge ->
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
            selected.boundary.edges().forEachIndexed { index, edge ->
                val p = viewport.toScreen(edge.pointAt(0.5))
                Box(Modifier.offset { IntOffset((p.x - 22 * density).roundToInt(), (p.y - 22 * density).roundToInt()) }
                    .size(44.dp).testTag("workspace-curve-$index").semantics { contentDescription = "Curve handle ${index + 1}" })
            }
        }
        if(inspector!=null && selected!=null) {
            Box(Modifier.fillMaxSize().pointerInput(inspector) { detectTapGestures { onCloseInspector() } })
            BackHandler { onCloseInspector() }
            Box(Modifier.align(Alignment.TopEnd).imePadding().padding(12.dp).widthIn(max=350.dp)) {
                if(inspector=="size") WorkspaceSizePanel(selected,model,onCloseInspector)
                else Surface(shape=MaterialTheme.shapes.large,shadowElevation=5.dp,tonalElevation=3.dp,
                    modifier=Modifier.testTag("workspace-coping-panel")) {
                    Column(Modifier.padding(vertical=8.dp)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text("Following coping",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                            TextButton(onClick=onCloseInspector,modifier=Modifier.testTag("workspace-coping-close")) { Text("Close") }
                        }
                        CopingWidthControl(selected,model,state.preview==null)
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
