package com.example.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.PdfManager
import com.example.data.ProjectRepository
import com.example.export.ExportManager
import com.example.export.PdfExportOptions
import com.example.model.BackgroundType
import com.example.model.DimensionMarkup
import com.example.model.DrawingLayer
import com.example.model.EllipseElement
import com.example.model.EditHistory
import com.example.model.FreehandPath
import com.example.model.LayerBlendMode
import com.example.model.LineElement
import com.example.model.Point2D
import com.example.model.PolylineElement
import com.example.model.RectangleElement
import com.example.model.ScaleCalibration
import com.example.model.StrokeStyle
import com.example.model.TextElement
import com.example.model.TraceProject
import com.example.model.VectorElement
import com.example.engine.SnapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class DrawingTool {
    PAN,
    PEN,
    LINE,
    POLYLINE,
    RECTANGLE,
    ELLIPSE,
    ERASER,
    SELECT,
    EYEDROPPER,
    WATERCOLOR_FILL,
    TEXT,
    MEASURE,
    DIMENSION
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    private val pendingSaves = Channel<TraceProject>(Channel.UNLIMITED)

    private val _project = MutableStateFlow(TraceProject())
    val project: StateFlow<TraceProject> = _project.asStateFlow()

    private val _projectsList = MutableStateFlow<List<TraceProject>>(emptyList())
    val projectsList: StateFlow<List<TraceProject>> = _projectsList.asStateFlow()

    private val toolPreferences = application.getSharedPreferences("drawing_preferences", Application.MODE_PRIVATE)

    // Tool state
    private val _activeTool = MutableStateFlow(runCatching { DrawingTool.valueOf(toolPreferences.getString("tool", "PEN")!!) }.getOrDefault(DrawingTool.PEN))
    val activeTool: StateFlow<DrawingTool> = _activeTool.asStateFlow()

    private val _strokeColor = MutableStateFlow(toolPreferences.getLong("color", 0xFF0F172A)) // Drafting Charcoal Black
    val strokeColor: StateFlow<Long> = _strokeColor.asStateFlow()

    private val _strokeWidth = MutableStateFlow(toolPreferences.getFloat("width", 3.5f).takeIf { it.isFinite() && it > 0f } ?: 3.5f)
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _strokeStyle = MutableStateFlow(runCatching { StrokeStyle.valueOf(toolPreferences.getString("style", "INK")!!) }.getOrDefault(StrokeStyle.INK))
    val strokeStyle: StateFlow<StrokeStyle> = _strokeStyle.asStateFlow()

    private val _barrelTool = MutableStateFlow(if (toolPreferences.getString("barrelTool", "SELECT") == "ERASER") DrawingTool.ERASER else DrawingTool.SELECT)
    val barrelTool: StateFlow<DrawingTool> = _barrelTool.asStateFlow()
    fun setBarrelTool(tool: DrawingTool) {
        if (tool != DrawingTool.SELECT && tool != DrawingTool.ERASER) return
        _barrelTool.value = tool
        toolPreferences.edit().putString("barrelTool", tool.name).apply()
        showToast(if (tool == DrawingTool.SELECT) "Hold the pen button before touching to select" else "Hold the pen button before touching to erase objects")
    }

    // Undo / Redo history
    private val pageHistories = mutableMapOf<String, EditHistory>()
    private val editHistory: EditHistory get() = pageHistories.getOrPut(_project.value.pageKey) { EditHistory() }

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Background bitmap
    private val _backgroundBitmap = MutableStateFlow<Bitmap?>(null)
    private var backgroundLoadJob: Job? = null
    val backgroundBitmap: StateFlow<Bitmap?> = _backgroundBitmap.asStateFlow()

    // Scale calibration mode
    private val _isCalibratingScale = MutableStateFlow(false)
    val isCalibratingScale: StateFlow<Boolean> = _isCalibratingScale.asStateFlow()

    private val _calibrationPoints = MutableStateFlow<Pair<Point2D, Point2D>?>(null)
    val calibrationPoints: StateFlow<Pair<Point2D, Point2D>?> = _calibrationPoints.asStateFlow()

    private val _showCalibrationDialog = MutableStateFlow(false)
    val showCalibrationDialog: StateFlow<Boolean> = _showCalibrationDialog.asStateFlow()

    // Dialog states
    private val _showLayersSheet = MutableStateFlow(false)
    val showLayersSheet: StateFlow<Boolean> = _showLayersSheet.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _showProjectsDialog = MutableStateFlow(false)
    val showProjectsDialog: StateFlow<Boolean> = _showProjectsDialog.asStateFlow()

    private val _showTextDialog = MutableStateFlow<Point2D?>(null)
    val showTextDialog: StateFlow<Point2D?> = _showTextDialog.asStateFlow()

    // Snapped feedback toast
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Stylus palm rejection mode
    private val _stylusOnlyMode = MutableStateFlow(toolPreferences.getBoolean("stylusOnly", false))
    val stylusOnlyMode: StateFlow<Boolean> = _stylusOnlyMode.asStateFlow()

    // Object selection state
    private val _selectedElementId = MutableStateFlow<String?>(null)
    val selectedElementId: StateFlow<String?> = _selectedElementId.asStateFlow()

    // Snapping configuration
    private val _snapSettings = MutableStateFlow(SnapSettings())
    val snapSettings: StateFlow<SnapSettings> = _snapSettings.asStateFlow()

    fun toggleSnapping() {
        val current = _snapSettings.value
        val updated = current.copy(snapEnabled = !current.snapEnabled)
        _snapSettings.value = updated
        showToast(if (updated.snapEnabled) "Snapping: ON" else "Snapping: OFF")
    }

    fun updateSnapSettings(newSettings: SnapSettings) {
        _snapSettings.value = newSettings
    }

    // 3D View Dialog State
    private val _show3DDialog = MutableStateFlow(false)
    val show3DDialog: StateFlow<Boolean> = _show3DDialog.asStateFlow()

    fun open3DView() {
        _show3DDialog.value = true
    }

    fun close3DView() {
        _show3DDialog.value = false
    }

    // Held Element for Layer Assignment Dialog
    private val _heldElementForLayer = MutableStateFlow<VectorElement?>(null)
    val heldElementForLayer: StateFlow<VectorElement?> = _heldElementForLayer.asStateFlow()

    fun setHeldElementForLayer(element: VectorElement?) {
        _heldElementForLayer.value = element
    }

    fun assignElementToLayer(elementId: String, targetLayerId: String) {
        val current = _project.value.elements
        val targetLayer = _project.value.layers.find { it.id == targetLayerId } ?: return
        val el = current.find { it.id == elementId } ?: return
        if (!canEditLayer(el.layerId) || !canEditLayer(targetLayerId)) return
        if (el.layerId == targetLayerId) return

        pushUndo(current)
        val updated = when (el) {
            is FreehandPath -> el.copy(layerId = targetLayerId)
            is LineElement -> el.copy(layerId = targetLayerId)
            is PolylineElement -> el.copy(layerId = targetLayerId)
            is RectangleElement -> el.copy(layerId = targetLayerId)
            is EllipseElement -> el.copy(layerId = targetLayerId)
            is TextElement -> el.copy(layerId = targetLayerId)
            is DimensionMarkup -> el.copy(layerId = targetLayerId)
        }
        val updatedList = current.map { if (it.id == elementId) updated else it }
        _project.value = _project.value.copy(elements = updatedList, updatedAt = System.currentTimeMillis())
        saveProjectDebounced()
        showToast("Assigned to \"${targetLayer.name}\"")
    }

    fun addDimensionToElement(element: VectorElement) {
        val scale = _project.value.scaleCalibration
        val activeLayer = _project.value.activeLayerId
        val dimElements = mutableListOf<DimensionMarkup>()

        when (element) {
            is LineElement -> {
                val dist = element.start.distanceTo(element.end)
                val label = if (scale.isCalibrated) scale.formatMeasurement(dist) else "${dist.toInt()} px"
                dimElements.add(
                    DimensionMarkup(
                        layerId = activeLayer,
                        start = element.start,
                        end = element.end,
                        label = label,
                        strokeColor = 0xFFDC2626
                    )
                )
            }
            is RectangleElement -> {
                val minX = kotlin.math.min(element.left, element.right)
                val maxX = kotlin.math.max(element.left, element.right)
                val minY = kotlin.math.min(element.top, element.bottom)
                val maxY = kotlin.math.max(element.top, element.bottom)
                val w = maxX - minX
                val h = maxY - minY
                val wLabel = if (scale.isCalibrated) scale.formatMeasurement(w) else "${w.toInt()} px"
                val hLabel = if (scale.isCalibrated) scale.formatMeasurement(h) else "${h.toInt()} px"
                dimElements.add(
                    DimensionMarkup(
                        layerId = activeLayer,
                        start = Point2D(minX, minY - 15f),
                        end = Point2D(maxX, minY - 15f),
                        label = wLabel,
                        strokeColor = 0xFFDC2626
                    )
                )
                dimElements.add(
                    DimensionMarkup(
                        layerId = activeLayer,
                        start = Point2D(maxX + 15f, minY),
                        end = Point2D(maxX + 15f, maxY),
                        label = hLabel,
                        strokeColor = 0xFFDC2626
                    )
                )
            }
            is EllipseElement -> {
                val dia = element.radiusX * 2f
                val label = if (scale.isCalibrated) "Ø " + scale.formatMeasurement(dia) else "Ø ${dia.toInt()} px"
                dimElements.add(
                    DimensionMarkup(
                        layerId = activeLayer,
                        start = Point2D(element.centerX - element.radiusX, element.centerY),
                        end = Point2D(element.centerX + element.radiusX, element.centerY),
                        label = label,
                        strokeColor = 0xFFDC2626
                    )
                )
            }
            else -> Unit
        }

        if (dimElements.isNotEmpty()) {
            for (dim in dimElements) {
                addVectorElement(dim)
            }
            showToast("Added architectural dimension")
        }
    }

    // Show persistent dimensions on calibrated plans
    private val _showDimensions = MutableStateFlow(toolPreferences.getBoolean("dimensions", true))
    val showDimensions: StateFlow<Boolean> = _showDimensions.asStateFlow()

    fun toggleShowDimensions() {
        _showDimensions.value = !_showDimensions.value
        toolPreferences.edit().putBoolean("dimensions", _showDimensions.value).apply()
    }

    fun selectElement(id: String?) {
        _selectedElementId.value = id
    }

    fun updateElement(updatedElement: VectorElement) {
        if (_isCalibratingScale.value) return
        val current = _project.value.elements
        val original = current.find { it.id == updatedElement.id } ?: return
        if (original == updatedElement || !canEditLayer(original.layerId) || !canEditLayer(updatedElement.layerId)) return
        pushUndo(current)
        val updatedList = current.map { if (it.id == updatedElement.id) updatedElement else it }
        _project.value = _project.value.copy(elements = updatedList, updatedAt = System.currentTimeMillis())
        saveProjectDebounced()
    }

    fun duplicateElement(id: String) {
        val element = _project.value.elements.find { it.id == id } ?: return
        if (!canEditLayer(element.layerId)) return
        val duplicated = when (element) {
            is FreehandPath -> element.copy(id = UUID.randomUUID().toString(), points = element.points.map { Point2D(it.x + 30f, it.y + 30f) })
            is LineElement -> element.copy(id = UUID.randomUUID().toString(), start = Point2D(element.start.x + 30f, element.start.y + 30f), end = Point2D(element.end.x + 30f, element.end.y + 30f))
            is PolylineElement -> element.copy(id = UUID.randomUUID().toString(), points = element.points.map { Point2D(it.x + 30f, it.y + 30f) })
            is RectangleElement -> element.copy(id = UUID.randomUUID().toString(), left = element.left + 30f, right = element.right + 30f, top = element.top + 30f, bottom = element.bottom + 30f)
            is EllipseElement -> element.copy(id = UUID.randomUUID().toString(), centerX = element.centerX + 30f, centerY = element.centerY + 30f)
            is TextElement -> element.copy(id = UUID.randomUUID().toString(), position = Point2D(element.position.x + 30f, element.position.y + 30f))
            is DimensionMarkup -> element.copy(id = UUID.randomUUID().toString(), start = Point2D(element.start.x + 30f, element.start.y + 30f), end = Point2D(element.end.x + 30f, element.end.y + 30f))
        }
        addVectorElement(duplicated)
        _selectedElementId.value = duplicated.id
        showToast("Object duplicated")
    }

    fun sampleColor(color: Long) {
        _strokeColor.value = color
        toolPreferences.edit().putLong("color", color).apply()
        val hex = String.format("#%06X", (0xFFFFFF and color.toInt()))
        showToast("Eyedropper: sampled $hex")
    }

    init {
        viewModelScope.launch {
            for (snapshot in pendingSaves) {
                try {
                    repository.saveProject(snapshot)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    showToast("Could not save this edit. Please keep the app open and try again.")
                }
            }
        }
        viewModelScope.launch {
            repository.projectsFlow.collect { list ->
                _projectsList.value = list
            }
        }
        viewModelScope.launch {
            val initial = repository.getOrCreateInitialProject()
            loadProject(initial)
        }
    }

    fun loadProject(p: TraceProject) {
        cancelEditGesture()
        cancelScaleCalibration()
        pageHistories.clear()
        _project.value = p
        _selectedElementId.value = null
        updateUndoRedoStates()
        loadBackgroundForProject(p)
    }

    private fun loadBackgroundForProject(p: TraceProject) {
        backgroundLoadJob?.cancel()
        _backgroundBitmap.value = null
        backgroundLoadJob = viewModelScope.launch {
            val context = getApplication<Application>()
            val bitmap = withContext(Dispatchers.IO) { when (p.backgroundType) {
                BackgroundType.SAMPLE -> {
                    val resId = if (p.backgroundResourceOrUri.contains("deck")) {
                        R.drawable.sample_plan_deck
                    } else {
                        R.drawable.sample_plan_pool
                    }
                    BitmapFactory.decodeResource(context.resources, resId)
                }
                BackgroundType.IMAGE_URI -> {
                    try {
                        val uri = Uri.parse(p.backgroundResourceOrUri)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                BackgroundType.PDF_URI -> {
                    try {
                        val uri = Uri.parse(p.backgroundResourceOrUri)
                        PdfManager.renderPdfPage(context, uri, p.pdfPageNumber)
                    } catch (e: Exception) {
                        null
                    }
                }
                BackgroundType.BLANK_GRID -> null
            } }
            if (_project.value.id != p.id || _project.value.pageKey != p.pageKey) return@launch
            _backgroundBitmap.value = bitmap
            if (bitmap == null && p.backgroundType in listOf(BackgroundType.IMAGE_URI, BackgroundType.PDF_URI)) {
                showToast("Cannot open this plan's source file. Import it again to restore the underlay.")
            }
        }
    }

    fun setTool(tool: DrawingTool) {
        _activeTool.value = tool
        if (tool != DrawingTool.MEASURE) toolPreferences.edit().putString("tool", tool.name).apply()
        if (tool != DrawingTool.MEASURE) {
            _isCalibratingScale.value = false
        }
    }

    fun setStrokeColor(color: Long) {
        _strokeColor.value = color
        toolPreferences.edit().putLong("color", color).apply()
    }

    fun setStrokeWidth(width: Float) {
        if (!width.isFinite() || width <= 0f) return
        _strokeWidth.value = width
        toolPreferences.edit().putFloat("width", width).apply()
    }

    fun setStrokeStyle(style: StrokeStyle) {
        _strokeStyle.value = style
        toolPreferences.edit().putString("style", style.name).apply()
    }

    fun toggleStylusOnlyMode() {
        _stylusOnlyMode.value = !_stylusOnlyMode.value
        toolPreferences.edit().putBoolean("stylusOnly", _stylusOnlyMode.value).apply()
        _toastMessage.value = if (_stylusOnlyMode.value) {
            "S Pen Mode: Stylus draws, fingers pan & zoom"
        } else {
            "Touch & Stylus Mode: Drawing with touch and stylus enabled"
        }
    }

    fun addVectorElement(element: VectorElement) {
        if (_isCalibratingScale.value || !canEditLayer(element.layerId)) return
        val currentElements = _project.value.elements
        pushUndo(currentElements)
        val updated = currentElements + element
        _project.value = _project.value.copy(elements = updated, updatedAt = System.currentTimeMillis())
        saveProjectDebounced()
    }

    fun removeVectorElements(idsToRemove: Set<String>) {
        if (idsToRemove.isEmpty()) return
        val currentElements = _project.value.elements
        val removable = currentElements.filter { it.id in idsToRemove && canEditLayer(it.layerId) }.map { it.id }.toSet()
        if (_isCalibratingScale.value || removable.isEmpty()) return
        pushUndo(currentElements)
        val updated = currentElements.filterNot { it.id in removable }
        _project.value = _project.value.copy(elements = updated, updatedAt = System.currentTimeMillis())
        saveProjectDebounced()
    }

    private fun pushUndo(@Suppress("UNUSED_PARAMETER") elements: List<VectorElement> = _project.value.elements) {
        editHistory.record(_project.value)
        updateUndoRedoStates()
    }

    fun undo() {
        _project.value = editHistory.undo(_project.value)
        _selectedElementId.value = null
        updateUndoRedoStates()
        saveProjectDebounced()
    }

    fun redo() {
        _project.value = editHistory.redo(_project.value)
        _selectedElementId.value = null
        updateUndoRedoStates()
        saveProjectDebounced()
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = editHistory.canUndo
        _canRedo.value = editHistory.canRedo
    }

    fun beginEditGesture() { editHistory.begin(_project.value) }
    fun endEditGesture() { editHistory.commit(_project.value); updateUndoRedoStates(); saveProjectDebounced() }
    fun cancelEditGesture() {
        if (!editHistory.isGestureActive) return
        _project.value = editHistory.cancel(_project.value)
        updateUndoRedoStates()
        saveProjectDebounced()
    }
    private fun canEditLayer(id: String): Boolean = _project.value.layers.any { it.id == id && !it.isLocked && it.isVisible }

    // Layer management
    fun setActiveLayer(layerId: String) {
        if (_project.value.layers.none { it.id == layerId }) return
        _project.value = _project.value.copy(activeLayerId = layerId)
        saveProjectDebounced()
    }

    fun toggleLayerVisibility(layerId: String) {
        pushUndo()
        val updated = _project.value.layers.map {
            if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
        }
        _project.value = _project.value.copy(layers = updated)
        saveProjectDebounced()
    }

    fun toggleLayerLock(layerId: String) {
        pushUndo()
        val updated = _project.value.layers.map {
            if (it.id == layerId) it.copy(isLocked = !it.isLocked) else it
        }
        _project.value = _project.value.copy(layers = updated)
        saveProjectDebounced()
    }

    fun setLayerOpacity(layerId: String, opacity: Float) {
        if (!canEditLayer(layerId)) return
        if (_project.value.layers.first { it.id == layerId }.opacity == opacity.coerceIn(0.1f, 1f)) return
        pushUndo()
        val updated = _project.value.layers.map {
            if (it.id == layerId) it.copy(opacity = opacity.coerceIn(0.1f, 1f)) else it
        }
        _project.value = _project.value.copy(layers = updated)
        saveProjectDebounced()
    }

    fun addLayer(name: String) {
        pushUndo()
        val newLayer = DrawingLayer(
            id = UUID.randomUUID().toString(),
            name = if (name.isBlank()) "Layer ${_project.value.layers.size + 1}" else name
        )
        val updated = _project.value.layers + newLayer
        _project.value = _project.value.copy(layers = updated, activeLayerId = newLayer.id)
        saveProjectDebounced()
    }

    fun deleteLayer(layerId: String) {
        if (!canEditLayer(layerId)) return
        if (_project.value.layers.size <= 1) {
            _toastMessage.value = "Cannot delete the only layer"
            return
        }
        pushUndo()
        val updatedLayers = _project.value.layers.filterNot { it.id == layerId }
        val updatedElements = _project.value.elements.filterNot { it.layerId == layerId }
        val newActiveId = if (_project.value.activeLayerId == layerId) {
            updatedLayers.first().id
        } else {
            _project.value.activeLayerId
        }
        _project.value = _project.value.copy(
            layers = updatedLayers,
            elements = updatedElements,
            activeLayerId = newActiveId
        )
        saveProjectDebounced()
    }

    fun moveLayerUp(layerId: String) {
        val layers = _project.value.layers.toMutableList()
        val index = layers.indexOfFirst { it.id == layerId }
        if (index > 0) {
            pushUndo()
            val item = layers.removeAt(index)
            layers.add(index - 1, item)
            _project.value = _project.value.copy(layers = layers)
            saveProjectDebounced()
        }
    }

    fun moveLayerDown(layerId: String) {
        val layers = _project.value.layers.toMutableList()
        val index = layers.indexOfFirst { it.id == layerId }
        if (index in 0 until layers.size - 1) {
            pushUndo()
            val item = layers.removeAt(index)
            layers.add(index + 1, item)
            _project.value = _project.value.copy(layers = layers)
            saveProjectDebounced()
        }
    }

    fun duplicateLayer(layerId: String) {
        val originalLayer = _project.value.layers.find { it.id == layerId } ?: return
        pushUndo()
        val newId = UUID.randomUUID().toString()
        val newLayer = originalLayer.copy(
            id = newId,
            name = "${originalLayer.name} (Copy)"
        )
        val originalElements = _project.value.elements.filter { it.layerId == layerId }
        val clonedElements = originalElements.map { elem ->
            when (elem) {
                is FreehandPath -> elem.copy(id = UUID.randomUUID().toString(), layerId = newId)
                is LineElement -> elem.copy(id = UUID.randomUUID().toString(), layerId = newId)
                is PolylineElement -> elem.copy(id = UUID.randomUUID().toString(), layerId = newId)
                is RectangleElement -> elem.copy(id = UUID.randomUUID().toString(), layerId = newId)
                is EllipseElement -> elem.copy(id = UUID.randomUUID().toString(), layerId = newId)
                is DimensionMarkup -> elem.copy(id = UUID.randomUUID().toString(), layerId = newId)
                is TextElement -> elem.copy(id = UUID.randomUUID().toString(), layerId = newId)
            }
        }
        val layers = _project.value.layers.toMutableList()
        val originalIndex = layers.indexOfFirst { it.id == layerId }
        layers.add(originalIndex + 1, newLayer)

        _project.value = _project.value.copy(
            layers = layers,
            elements = _project.value.elements + clonedElements,
            activeLayerId = newId
        )
        saveProjectDebounced()
        _toastMessage.value = "Duplicated ${originalLayer.name}"
    }

    fun mergeLayerDown(layerId: String) {
        val layers = _project.value.layers
        val index = layers.indexOfFirst { it.id == layerId }
        if (index >= 0 && index < layers.size - 1 && canEditLayer(layerId) && canEditLayer(layers[index + 1].id)) {
            pushUndo()
            val targetLayer = layers[index + 1]
            val updatedElements = _project.value.elements.map {
                if (it.layerId == layerId) {
                    when (it) {
                        is FreehandPath -> it.copy(layerId = targetLayer.id)
                        is LineElement -> it.copy(layerId = targetLayer.id)
                        is PolylineElement -> it.copy(layerId = targetLayer.id)
                        is RectangleElement -> it.copy(layerId = targetLayer.id)
                        is EllipseElement -> it.copy(layerId = targetLayer.id)
                        is DimensionMarkup -> it.copy(layerId = targetLayer.id)
                        is TextElement -> it.copy(layerId = targetLayer.id)
                    }
                } else it
            }
            val remainingLayers = layers.filterNot { it.id == layerId }
            _project.value = _project.value.copy(
                layers = remainingLayers,
                elements = updatedElements,
                activeLayerId = targetLayer.id
            )
            saveProjectDebounced()
            _toastMessage.value = "Merged layer down into ${targetLayer.name}"
        }
    }

    fun clearLayer(layerId: String) {
        if (!canEditLayer(layerId)) return
        val currentElements = _project.value.elements
        pushUndo(currentElements)
        val updatedElements = currentElements.filterNot { it.layerId == layerId }
        _project.value = _project.value.copy(elements = updatedElements)
        saveProjectDebounced()
        _toastMessage.value = "Cleared contents of active layer"
    }

    fun setLayerBlendMode(layerId: String, blendMode: LayerBlendMode) {
        if (!canEditLayer(layerId)) return
        pushUndo()
        val updated = _project.value.layers.map {
            if (it.id == layerId) it.copy(blendMode = blendMode) else it
        }
        _project.value = _project.value.copy(layers = updated)
        saveProjectDebounced()
    }

    fun setLayerColorTag(layerId: String, colorTag: Long) {
        if (!canEditLayer(layerId)) return
        pushUndo()
        val updated = _project.value.layers.map {
            if (it.id == layerId) it.copy(colorTag = colorTag) else it
        }
        _project.value = _project.value.copy(layers = updated)
        saveProjectDebounced()
    }

    fun addPresetLayers(names: List<String>) {
        pushUndo()
        val colors = listOf(0xFF2563EB, 0xFF059669, 0xFFD97706, 0xFFDC2626, 0xFF7C3AED)
        val newLayers = names.mapIndexed { idx, name ->
            DrawingLayer(
                id = UUID.randomUUID().toString(),
                name = name,
                colorTag = colors[idx % colors.size]
            )
        }
        _project.value = _project.value.copy(layers = _project.value.layers + newLayers)
        saveProjectDebounced()
        _toastMessage.value = "Added architectural layers preset"
    }

    // Background controls
    fun setBackgroundOpacity(opacity: Float) {
        _project.value = _project.value.copy(backgroundOpacity = opacity.coerceIn(0f, 1f))
        saveProjectDebounced()
    }

    fun toggleBackgroundLock() {
        _project.value = _project.value.copy(isBackgroundLocked = !_project.value.isBackgroundLocked)
        saveProjectDebounced()
    }

    fun selectSamplePlan(sampleKey: String) {
        openSheet(BackgroundType.SAMPLE, sampleKey)
        _toastMessage.value = "Loaded architectural sample plan"
    }

    fun importPlanUri(uri: Uri) {
        val importingProjectId = _project.value.id
        viewModelScope.launch {
            val context = getApplication<Application>()
            val isPdf: Boolean
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                isPdf = context.contentResolver.getType(uri) == "application/pdf" ||
                    uri.lastPathSegment?.endsWith(".pdf", ignoreCase = true) == true
                val preview = if (isPdf) PdfManager.renderPdfPage(context, uri, 0) else withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
                if (preview == null) {
                    showToast("Could not read this plan. The current drawing has been kept.")
                    return@launch
                }
                preview.recycle()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showToast("Could not retain access to this file. Try importing it from Files.")
                return@launch
            }
            val totalPages = if (isPdf) PdfManager.getPdfPageCount(context, uri) else 1
            if (_project.value.id != importingProjectId) return@launch
            openSheet(if (isPdf) BackgroundType.PDF_URI else BackgroundType.IMAGE_URI, uri.toString(), 0, totalPages)
            _toastMessage.value = if (isPdf) "Imported PDF plan" else "Imported image plan"
        }
    }

    fun nextPdfPage() {
        val p = _project.value
        if (p.backgroundType == BackgroundType.PDF_URI && p.pdfPageNumber < p.pdfTotalPages - 1) {
            openSheet(p.backgroundType, p.backgroundResourceOrUri, p.pdfPageNumber + 1, p.pdfTotalPages)
        }
    }

    fun prevPdfPage() {
        val p = _project.value
        if (p.backgroundType == BackgroundType.PDF_URI && p.pdfPageNumber > 0) {
            openSheet(p.backgroundType, p.backgroundResourceOrUri, p.pdfPageNumber - 1, p.pdfTotalPages)
        }
    }

    private fun openSheet(type: BackgroundType, source: String, page: Int = 0, count: Int = 1) {
        cancelEditGesture()
        cancelScaleCalibration()
        _selectedElementId.value = null
        _project.value = _project.value.openSheet(type, source, page, count)
        updateUndoRedoStates()
        loadBackgroundForProject(_project.value)
        saveProjectDebounced()
    }

    // Scale calibration
    fun startScaleCalibration() {
        _isCalibratingScale.value = true
        _calibrationPoints.value = null
        _toastMessage.value = "Tap or drag between two points of known distance"
    }

    fun setCalibrationSegment(p1: Point2D, p2: Point2D) {
        val dist = p1.distanceTo(p2)
        if (dist > 15f) {
            _calibrationPoints.value = Pair(p1, p2)
            _showCalibrationDialog.value = true
        }
    }

    fun applyScaleCalibration(realUnits: Float, unit: String) {
        if (!realUnits.isFinite() || realUnits <= 0f) {
            showToast("Enter a positive reference distance")
            return
        }
        val pts = _calibrationPoints.value
        if (pts != null) {
            val pixelDist = pts.first.distanceTo(pts.second)
            val updatedScale = ScaleCalibration(
                isCalibrated = true,
                pixelDistance = pixelDist,
                realWorldUnits = realUnits,
                unit = unit
            )
            _project.value = _project.value.copy(scaleCalibration = updatedScale)
            saveProjectDebounced()
            _toastMessage.value = "Scale set: $pixelDist px = $realUnits $unit"
        }
        _isCalibratingScale.value = false
        _showCalibrationDialog.value = false
    }

    fun cancelScaleCalibration() {
        _isCalibratingScale.value = false
        _showCalibrationDialog.value = false
        _calibrationPoints.value = null
    }

    // Project management
    fun createNewProject(title: String, sampleKey: String = "sample_pool") {
        val newProj = TraceProject(
            title = if (title.isBlank()) "Untitled Plan Sketch" else title,
            backgroundType = BackgroundType.SAMPLE,
            backgroundResourceOrUri = sampleKey,
            scaleCalibration = ScaleCalibration(
                isCalibrated = false,
                pixelDistance = 240f,
                realWorldUnits = 20f,
                unit = "ft"
            )
        )
        loadProject(newProj)
        viewModelScope.launch {
            repository.saveProject(newProj)
        }
        _showProjectsDialog.value = false
        _toastMessage.value = "Created project: ${newProj.title}"
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            if (_project.value.id == id) {
                val list = repository.getOrCreateInitialProject()
                loadProject(list)
            }
        }
    }

    fun setProjectTitle(title: String) {
        _project.value = _project.value.copy(title = title)
        saveProjectDebounced()
    }

    fun clearAllDrawings() {
        removeVectorElements(_project.value.elements.map { it.id }.toSet())
        _toastMessage.value = "Cleared editable vector linework"
    }

    private fun saveProjectDebounced() {
        if (editHistory.isGestureActive) return
        val snapshot = _project.value
        pendingSaves.trySend(snapshot)
    }

    // Export operations
    fun exportProjectWithArchitecturalOptions(options: PdfExportOptions, asPdf: Boolean) {
        if (backgroundLoadJob?.isActive == true) {
            showToast("Wait for this sheet to finish loading before exporting.")
            return
        }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val file: File? = if (asPdf) {
                ExportManager.exportToPdf(
                    context = context,
                    project = _project.value,
                    backgroundBitmap = _backgroundBitmap.value,
                    options = options
                )
            } else {
                ExportManager.exportToPng(
                    context = context,
                    project = _project.value,
                    backgroundBitmap = _backgroundBitmap.value,
                    includeBackground = options.includeBackground
                )
            }

            if (file != null) {
                val mimeType = if (asPdf) "application/pdf" else "image/png"
                val shared = ExportManager.shareExportedFile(context, file, mimeType, _project.value.title)
                _toastMessage.value = if (shared) "Ready to share ${file.name}" else "File created, but sharing could not open. Please try again."
            } else {
                _toastMessage.value = "Export failed"
            }
            _showExportDialog.value = false
        }
    }

    fun exportProject(asPdf: Boolean, includeBackground: Boolean) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val file: File? = if (asPdf) {
                ExportManager.exportToPdf(
                    context = context,
                    project = _project.value,
                    backgroundBitmap = _backgroundBitmap.value,
                    options = PdfExportOptions(includeBackground = includeBackground)
                )
            } else {
                ExportManager.exportToPng(
                    context = context,
                    project = _project.value,
                    backgroundBitmap = _backgroundBitmap.value,
                    includeBackground = includeBackground
                )
            }

            if (file != null) {
                val mimeType = if (asPdf) "application/pdf" else "image/png"
                val shared = ExportManager.shareExportedFile(context, file, mimeType, _project.value.title)
                _toastMessage.value = if (shared) "Ready to share ${file.name}" else "File created, but sharing could not open. Please try again."
            } else {
                _toastMessage.value = "Export failed"
            }
            _showExportDialog.value = false
        }
    }

    // Dialog toggles
    fun setShowLayersSheet(show: Boolean) { _showLayersSheet.value = show }
    fun setShowExportDialog(show: Boolean) { _showExportDialog.value = show }
    fun setShowProjectsDialog(show: Boolean) { _showProjectsDialog.value = show }
    fun setShowTextDialog(point: Point2D?) { _showTextDialog.value = point }
    fun showToast(msg: String) { _toastMessage.value = msg }
    fun clearToast() { _toastMessage.value = null }
}
