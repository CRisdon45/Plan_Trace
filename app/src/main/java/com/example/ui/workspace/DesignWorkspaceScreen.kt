package com.example.ui.workspace

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.export.DesignOutput
import com.example.export.DesignOutputSettings
import com.example.export.ExportManager
import com.example.model.design.*
import com.example.ui.components.ExportDialog
import kotlinx.coroutines.launch
import java.util.Locale

/** Opt-in draft workspace in the real application; no duplicate renderer or legacy-data migration. */
@Composable
fun DesignWorkspaceScreen(onBack: () -> Unit, model: DesignWorkspaceViewModel = viewModel()) {
    val state by model.state.collectAsState()
    val preferences = LocalContext.current.getSharedPreferences("workspace-view", Context.MODE_PRIVATE)
    var touchEdit by rememberSaveable { mutableStateOf(preferences.getBoolean("touch", false)) }
    var showGrid by rememberSaveable { mutableStateOf(preferences.getBoolean("grid", false)) }
    var gridSnap by rememberSaveable { mutableStateOf(preferences.getBoolean("snap", false)) }
    var geometrySnap by rememberSaveable { mutableStateOf(preferences.getBoolean("geometry-snap", false)) }
    var commandRequest by remember { mutableStateOf(0) }
    var inspector by remember { mutableStateOf<String?>(null) }
    val sitePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { model.importSiteImage(it) }
    }
    LaunchedEffect(touchEdit, showGrid, gridSnap, geometrySnap) {
        preferences.edit().putBoolean("touch",touchEdit).putBoolean("grid",showGrid).putBoolean("snap",gridSnap).putBoolean("geometry-snap",geometrySnap).apply()
    }
    LaunchedEffect(state.selectedId) { inspector=null }
    fun command(action: RadialAction) {
        val doc=model.state.value.document ?: return
        val selected=doc.objects.firstOrNull { it.id==model.state.value.selectedId }
        when(action) {
            RadialAction.SITE -> { model.stopSiteTool(); inspector="site" }
            RadialAction.POOL -> model.beginProposedOutline(DesignObjectKind.POOL)
            RadialAction.CURVED -> model.addOutline(true)
            RadialAction.SMOOTH_POOL -> model.beginSmoothPool()
            RadialAction.SMOOTH -> model.setSmoothMode(if(state.smoothMode==SmoothEditMode.SHAPE) SmoothEditMode.OFF else SmoothEditMode.SHAPE)
            RadialAction.RADIUS -> model.setSmoothMode(if(state.smoothMode==SmoothEditMode.RADIUS) SmoothEditMode.OFF else SmoothEditMode.RADIUS)
            RadialAction.PAVING -> model.beginProposedOutline(DesignObjectKind.PAVING)
            RadialAction.SIDES -> model.toggleSideEditing()
            RadialAction.SIZE -> if(selected!=null && !selected.locked) inspector="size"
            RadialAction.COPING -> if(selected!=null && !selected.locked) inspector="coping"
            RadialAction.DELETE -> selected?.let { model.execute(DesignCommand.Remove(it.id)) }
            RadialAction.FIT -> model.fit()
            RadialAction.GRID -> showGrid=!showGrid
            RadialAction.TOUCH -> { model.cancelPreview();touchEdit=!touchEdit }
            RadialAction.SNAP -> gridSnap=!gridSnap
            RadialAction.GEOMETRY -> geometrySnap=!geometrySnap
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
            if (event == Lifecycle.Event.ON_PAUSE) { model.stopSideEditing(); model.stopSiteTool();inspector=null }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer); model.stopSideEditing(); model.stopSiteTool() }
    }
    fun close() {
        model.stopSideEditing(); model.stopSiteTool()
        if (state.document == null || state.saved || state.document!!.revision == 0L) onBack()
        else leaveUnsaved = true
    }
    BackHandler { close() }
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        WorkspaceHeader(state, exportBusy, onBack = { close() },
            onCommands = { model.stopSiteTool(); inspector = null; commandRequest++ }, onUndo = model::undo,
            onRedo = model::redo, onExport = { exporting = true })
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
                            label = { Text(obj.name + if(obj.locked) " · Locked" else "") }, modifier = Modifier.testTag("workspace-object-$index"))
                    }
                }
                WorkspaceCanvas(state, model, touchEdit, showGrid, gridSnap, geometrySnap, commandRequest, inspector,
                    !exporting && !leaveUnsaved, { inspector=null; model.stopSiteTool() }, ::command,
                    { sitePicker.launch(arrayOf("image/png","image/jpeg")) }, { inspector="site" },
                    Modifier.weight(1f).fillMaxWidth())
                state.siteDraft?.let { draft -> SiteOutlineControls(draft,state.draftOrthogonal,model) }
                state.proposedDraft?.let { draft -> ProposedOutlineControls(draft,state.draftOrthogonal,model) }
                state.smoothDraft?.let { draft -> SmoothOutlineControls(draft,model) }
                val selected = doc.objects.firstOrNull { it.id == state.selectedId }
                WorkspaceTraceStatus(state)

                Text(selected?.let { String.format(Locale.US, "%s · perimeter %.2f ft", it.name, it.boundary.perimeterMetres / 0.3048) }
                    ?: "Add an outline, then select its edge to edit.", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("workspace-selection"))
                Text(state.message ?: "Commands opens the tool wheel. " + (if(touchEdit) "Touch editing on. " else "Pen edits; fingers navigate. ") + (if(gridSnap) "1 ft snap: vertices and moves." else "Round handles reshape; amber handles change curves."),
                    style = MaterialTheme.typography.labelMedium, maxLines=1, overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
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
                        else DesignOutput.png(context, snapshot, settings = DesignOutputSettings(includeMeasurements = options.includeDimensions, includeSourceImage = options.includeBackground))
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
internal fun CopingWidthControl(obj: DesignObject, model: DesignWorkspaceViewModel, ready: Boolean) {
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
