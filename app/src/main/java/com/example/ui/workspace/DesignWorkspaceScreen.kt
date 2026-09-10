package com.example.ui.workspace

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
    var touchEdit by rememberSaveable { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var leaveUnsaved by remember { mutableStateOf(false) }
    var exportBusy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) model.cancelPreview()
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
                Text("Preview", style = MaterialTheme.typography.labelMedium)
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
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                val ready = state.document != null && state.preview == null
                OutlinedButton(onClick = { model.addOutline(false) }, enabled = ready, modifier = Modifier.testTag("workspace-add-pool")) { Text("Pool outline") }
                OutlinedButton(onClick = { model.addOutline(true) }, enabled = ready, modifier = Modifier.testTag("workspace-add-curved")) { Text("Curved outline") }
                OutlinedButton(onClick = { model.addOutline(false, DesignObjectKind.PAVING) }, enabled = ready,
                    modifier = Modifier.testTag("workspace-add-paving")) { Text("Paving outline") }
                TextButton(onClick = model::undo, enabled = state.canUndo && state.preview == null, modifier = Modifier.testTag("workspace-undo")) { Text("Undo") }
                TextButton(onClick = model::redo, enabled = state.canRedo && state.preview == null, modifier = Modifier.testTag("workspace-redo")) { Text("Redo") }
                TextButton(onClick = {
                    state.selectedId?.let { model.execute(DesignCommand.Remove(it)) }
                }, enabled = ready && state.document?.objects?.any { it.id == state.selectedId && !it.locked } == true,
                    modifier = Modifier.testTag("workspace-delete")) { Text("Delete") }
                TextButton(onClick = model::fit, modifier = Modifier.testTag("workspace-fit")) { Text("Fit") }
                FilterChip(selected = touchEdit, onClick = { model.cancelPreview(); touchEdit = !touchEdit },
                    label = { Text("Touch edit") }, modifier = Modifier.testTag("workspace-touch-edit"))
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
                WorkspaceCanvas(state, model, touchEdit, Modifier.weight(1f).fillMaxWidth())
                val selected = doc.objects.firstOrNull { it.id == state.selectedId }
                Text(selected?.let { String.format(Locale.US, "%s · perimeter %.2f ft", it.name, it.boundary.perimeterMetres / 0.3048) }
                    ?: "Add an outline, then select its edge to edit.", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("workspace-selection"))
                Text(state.message ?: "Drag round handles for vertices, amber handles for curves. Pen edits; fingers pan unless Touch edit is on.",
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Text("Outline preview: coping, surface validation and the final Northstar appearance are not implemented here yet.",
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
    fun clear() { target = null; document = null; down = null; startView = null; blockedEdit = false; moved = false }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WorkspaceCanvas(state: WorkspaceState, model: DesignWorkspaceViewModel, touchEdit: Boolean, modifier: Modifier) {
    val document = state.shownDocument ?: return
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var size by remember { mutableStateOf(IntSize.Zero) }
    var viewport by remember { mutableStateOf(DesignViewport(60.0, 100.0, 300.0)) }
    val pointer = remember { PointerSession() }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { pointer.clear(); model.cancelPreview() }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); pointer.clear(); model.cancelPreview() }
    }
    LaunchedEffect(size, state.fitRequest) {
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
    Box(modifier.onSizeChanged { size = it }.clipToBounds().background(Color(0xFFFBFAF5))
        .testTag("workspace-canvas").pointerInteropFilter { event ->
            val x = event.x.toDouble(); val y = event.y.toDouble()
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
                        if (pointer.moved) model.preview(DesignPicking.drag(pointer.document!!, target, pointer.down!!, pointer.startView!!.toWorld(x, y)), pointer.document!!.revision)
                    } else if (!detector.isInProgress && event.pointerCount == 1 && !pointer.blockedEdit) {
                        viewport = viewport.panned(x - pointer.lastX, y - pointer.lastY)
                    }
                    pointer.lastX = x; pointer.lastY = y
                }
                MotionEvent.ACTION_UP -> {
                    if (pointer.target != null && !pointer.blockedEdit &&
                        (pointer.moved || hypot(x - pointer.downX, y - pointer.downY) > 3 * density)) {
                        // Pen-up can contain the last position even without a final move event.
                        model.preview(DesignPicking.drag(pointer.document!!, pointer.target!!, pointer.down!!, pointer.startView!!.toWorld(x, y)), pointer.document!!.revision)
                        model.commitPreview()
                    } else model.cancelPreview()
                    pointer.clear()
                }
                MotionEvent.ACTION_CANCEL -> { model.cancelPreview(); pointer.clear() }
            }
            true
        }) {
        Canvas(Modifier.fillMaxSize()) {
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
    }
}
