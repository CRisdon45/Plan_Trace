package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.canvas.RadialPalette
import com.example.ui.canvas.tablet.TraceCanvas
import com.example.ui.components.ArchitecturalToolbar
import com.example.ui.components.EditTitleDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.LayersDialog
import com.example.ui.components.ProjectManagerDialog
import com.example.ui.components.ScaleCalibrationDialog
import com.example.ui.components.TextAnnotationDialog
import com.example.ui.components.TopBar
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PlanTraceApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PlanTraceApp(viewModel: MainViewModel) {
    val project by viewModel.project.collectAsState()
    val projectsList by viewModel.projectsList.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()
    val strokeColor by viewModel.strokeColor.collectAsState()
    val strokeWidth by viewModel.strokeWidth.collectAsState()
    val strokeStyle by viewModel.strokeStyle.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val backgroundBitmap by viewModel.backgroundBitmap.collectAsState()
    val isCalibratingScale by viewModel.isCalibratingScale.collectAsState()
    val calibrationPoints by viewModel.calibrationPoints.collectAsState()
    val showCalibrationDialog by viewModel.showCalibrationDialog.collectAsState()
    val showLayersSheet by viewModel.showLayersSheet.collectAsState()
    val showExportDialog by viewModel.showExportDialog.collectAsState()
    val showProjectsDialog by viewModel.showProjectsDialog.collectAsState()
    val textRequestPoint by viewModel.showTextDialog.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val stylusOnlyMode by viewModel.stylusOnlyMode.collectAsState()
    val selectedElementId by viewModel.selectedElementId.collectAsState()
    val showDimensions by viewModel.showDimensions.collectAsState()
    val snapSettings by viewModel.snapSettings.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showRadialMenu by remember { mutableStateOf(false) }
    var radialMenuPosition by remember { mutableStateOf(Offset(200f, 200f)) }
    var showEditTitle by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            }
            viewModel.clearToast()
        }
    }

    // This is a tablet/S Pen tool first: by default the pen draws while fingers navigate.
    // Users can still tap the Pen-only control to enable one-finger drawing when desired.
    LaunchedEffect(Unit) {
        if (!stylusOnlyMode) viewModel.toggleStylusOnlyMode()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // OpenDocument is specifically chosen so Android can grant long-lived access to the
            // selected document. Keep that grant because projects store the content URI and may
            // reopen it days later after a process restart or tablet reboot.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val mimeType = context.contentResolver.getType(uri)?.lowercase()
            val isPdf = mimeType == "application/pdf" ||
                uri.toString().lowercase().endsWith(".pdf") ||
                uri.path?.lowercase()?.endsWith(".pdf") == true
            viewModel.importPlanUri(uri, isPdf)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier.padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                )
            ) {
                TopBar(
                    project = project,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    stylusOnlyMode = stylusOnlyMode,
                    isCalibratingScale = isCalibratingScale,
                    showDimensions = showDimensions,
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onToggleStylusMode = { viewModel.toggleStylusOnlyMode() },
                    onToggleShowDimensions = { viewModel.toggleShowDimensions() },
                    onStartCalibration = { viewModel.startScaleCalibration() },
                    onCancelCalibration = { viewModel.cancelScaleCalibration() },
                    onOpenLayers = { viewModel.setShowLayersSheet(true) },
                    onOpenExport = { viewModel.setShowExportDialog(true) },
                    onOpenProjects = { viewModel.setShowProjectsDialog(true) },
                    onSelectSample = { viewModel.selectSamplePlan(it) },
                    onClearCanvas = { viewModel.clearAllDrawings() },
                    onImportFile = {
                        filePickerLauncher.launch(arrayOf("application/pdf", "image/*"))
                    },
                    onPrevPdfPage = { viewModel.prevPdfPage() },
                    onNextPdfPage = { viewModel.nextPdfPage() },
                    onEditTitle = { showEditTitle = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TraceCanvas(
                modifier = Modifier.fillMaxSize(),
                project = project,
                backgroundBitmap = backgroundBitmap,
                activeTool = activeTool,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                strokeStyle = strokeStyle,
                stylusOnlyMode = stylusOnlyMode,
                isCalibratingScale = isCalibratingScale,
                selectedElementId = selectedElementId,
                showDimensions = showDimensions,
                snapSettings = snapSettings,
                onElementCreated = { viewModel.addVectorElement(it) },
                onElementUpdated = { viewModel.updateElement(it) },
                onElementsDeleted = { viewModel.removeVectorElements(it) },
                onElementDuplicated = { viewModel.duplicateElement(it) },
                onElementSelected = { viewModel.selectElement(it) },
                onColorSampled = { viewModel.sampleColor(it) },
                onCalibrationSegmentDrawn = { p1, p2 -> viewModel.setCalibrationSegment(p1, p2) },
                onTextRequested = { viewModel.setShowTextDialog(it) },
                onShowRadialPalette = { offset ->
                    radialMenuPosition = offset
                    showRadialMenu = true
                },
                onHideRadialPalette = {
                    showRadialMenu = false
                },
                onQuickUndo = { viewModel.undo() },
                onFeedbackMessage = { viewModel.showToast(it) }
            )

            ArchitecturalToolbar(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp),
                activeTool = activeTool,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                strokeStyle = strokeStyle,
                onSelectTool = { viewModel.setTool(it) },
                onSelectColor = { viewModel.setStrokeColor(it) },
                onSelectWidth = { viewModel.setStrokeWidth(it) },
                onSelectStyle = { viewModel.setStrokeStyle(it) }
            )

            RadialPalette(
                visible = showRadialMenu,
                position = radialMenuPosition,
                activeTool = activeTool,
                onSelectTool = { viewModel.setTool(it) },
                onDismiss = { showRadialMenu = false }
            )
        }
    }

    if (showLayersSheet) {
        LayersDialog(
            project = project,
            onDismiss = { viewModel.setShowLayersSheet(false) },
            onSetActiveLayer = { viewModel.setActiveLayer(it) },
            onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
            onToggleLock = { viewModel.toggleLayerLock(it) },
            onSetOpacity = { id, op -> viewModel.setLayerOpacity(id, op) },
            onAddLayer = { viewModel.addLayer(it) },
            onDeleteLayer = { viewModel.deleteLayer(it) },
            onMoveUp = { viewModel.moveLayerUp(it) },
            onMoveDown = { viewModel.moveLayerDown(it) },
            onDuplicateLayer = { viewModel.duplicateLayer(it) },
            onMergeDown = { viewModel.mergeLayerDown(it) },
            onClearLayer = { viewModel.clearLayer(it) },
            onSetBlendMode = { id, mode -> viewModel.setLayerBlendMode(id, mode) },
            onSetColorTag = { id, tag -> viewModel.setLayerColorTag(id, tag) },
            onAddPresetLayers = { viewModel.addPresetLayers(it) },
            onSetBackgroundOpacity = { viewModel.setBackgroundOpacity(it) },
            onToggleBackgroundLock = { viewModel.toggleBackgroundLock() }
        )
    }

    if (showCalibrationDialog && calibrationPoints != null) {
        ScaleCalibrationDialog(
            points = calibrationPoints!!,
            onDismiss = { viewModel.cancelScaleCalibration() },
            onApplyCalibration = { units, unit -> viewModel.applyScaleCalibration(units, unit) }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            projectTitle = project.title,
            onDismiss = { viewModel.setShowExportDialog(false) },
            onExport = { options, asPdf -> viewModel.exportProjectWithArchitecturalOptions(options, asPdf) }
        )
    }

    if (showProjectsDialog) {
        ProjectManagerDialog(
            currentProject = project,
            projects = projectsList,
            onDismiss = { viewModel.setShowProjectsDialog(false) },
            onSelectProject = { viewModel.loadProject(it) },
            onCreateNewProject = { title, sample -> viewModel.createNewProject(title, sample) },
            onDeleteProject = { viewModel.deleteProject(it) },
            onImportFile = {
                filePickerLauncher.launch(arrayOf("application/pdf", "image/*"))
            }
        )
    }

    if (textRequestPoint != null) {
        TextAnnotationDialog(
            position = textRequestPoint!!,
            activeLayerId = project.activeLayerId,
            strokeColor = strokeColor,
            onDismiss = { viewModel.setShowTextDialog(null) },
            onAddText = { viewModel.addVectorElement(it) }
        )
    }

    if (showEditTitle) {
        EditTitleDialog(
            initialTitle = project.title,
            onDismiss = { showEditTitle = false },
            onConfirm = { viewModel.setProjectTitle(it) }
        )
    }
}
