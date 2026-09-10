package com.example.ui.workspace

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.SiteImageStore
import kotlinx.coroutines.Job
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DesignWorkspaceStore
import com.example.model.design.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class SiteTool { NONE, CALIBRATE, MOVE }

data class WorkspaceState(
    val document: ProjectDesign? = null,
    val preview: ProjectDesign? = null,
    val selectedId: String? = null,
    val loading: Boolean = true,
    val loadError: String? = null,
    val message: String? = null,
    val saveError: String? = null,
    val savedRevision: Long? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val fitRequest: Int = 0,
    val siteBitmap: Bitmap? = null,
    val siteError: String? = null,
    val siteImporting: Boolean = false,
    val siteTool: SiteTool = SiteTool.NONE,
    val referencePoints: List<ImagePoint> = emptyList()
) {
    val shownDocument: ProjectDesign? get() = preview ?: document
    val saved: Boolean get() = document != null && savedRevision == document.revision && saveError == null
}

class DesignWorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val assets = SiteImageStore.inFiles(application.filesDir)
    private var imageLoad: Job? = null
    private var loadedAsset: String? = null
    private val store = DesignWorkspaceStore(DesignWorkspaceStore.fileIn(application.filesDir))
    private val _state = MutableStateFlow(WorkspaceState())
    val state = _state.asStateFlow()
    private var session: DesignSession? = null
    private var pendingCommand: DesignCommand? = null
    private val writes = Channel<ProjectDesign>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { store.load() }
                val doc = loaded ?: ProjectDesign(UUID.randomUUID().toString())
                session = DesignSession(doc)
                _state.value = WorkspaceState(document = doc, loading = false, savedRevision = loaded?.revision)
                refreshSource()
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                _state.value = WorkspaceState(loading = false,
                    loadError = "The saved design could not be opened. It has not been replaced with a new design. ${error.message.orEmpty().take(220)}")
            }
        }
        viewModelScope.launch {
            for (document in writes) {
                try {
                    withContext(Dispatchers.IO) { store.save(document) }
                    _state.update { it.copy(savedRevision = document.revision,
                        saveError = if (it.document?.revision == document.revision) null else it.saveError) }
                } catch (error: CancellationException) { throw error }
                catch (error: Exception) {
                    _state.update { it.copy(saveError = "Not saved: ${error.message.orEmpty().take(220)}") }
                }
            }
        }
    }
    /** The system picker and synthetic import tests call this same bounded intake path. */
    fun importSiteImage(uri: Uri) {
        val before = session?.document ?: return
        if (state.value.siteImporting) return
        // Replacing a registered source is a separate operation. Remove/Undo is explicit in the panel.
        if (before.siteImage != null) { feedback("Remove the current source first. Undo can restore it"); return }
        _state.update { it.copy(siteImporting=true, message=null) }
        viewModelScope.launch {
            try {
                val asset = withContext(Dispatchers.IO) { assets.ingest(getApplication(), uri) }
                if (session?.document?.id == before.id && session?.document?.siteImage == null) {
                    execute(DesignCommand.SetSiteImage(SiteImage.unscaled(asset), null)); fit()
                    feedback("Source imported. Mark a known distance before relying on its scale")
                } else feedback("The source changed during import. Existing work was preserved")
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { feedback("Source not imported: ${error.message.orEmpty().take(180)}") }
            finally { _state.update { it.copy(siteImporting=false) } }
        }
    }
    private fun refreshSource() {
        val source = session?.document?.siteImage
        if (source?.asset?.sha256 == loadedAsset && (source == null || state.value.siteBitmap != null)) return
        imageLoad?.cancel(); loadedAsset = source?.asset?.sha256
        _state.update { it.copy(siteBitmap=null,siteError=null) }
        if (source == null) return
        imageLoad = viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) { assets.load(source.asset) }
                if (session?.document?.siteImage?.asset == source.asset) _state.update { it.copy(siteBitmap=bitmap) }
                else bitmap.recycle()
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { _state.update { it.copy(siteError=error.message.orEmpty().take(180)) } }
        }
    }
    fun startSiteTool(tool: SiteTool) {
        if (session?.document?.siteImage?.visible != true || state.value.siteBitmap == null) return
        cancelPreview()
        _state.update { it.copy(siteTool=tool, referencePoints=emptyList(), message=null) }
    }
    fun stopSiteTool() { cancelPreview(); _state.update { it.copy(siteTool=SiteTool.NONE,referencePoints=emptyList()) } }
    fun markSiteReference(point: DesignPoint) {
        val source = session?.document?.siteImage ?: return
        if (state.value.siteTool != SiteTool.CALIBRATE) return
        val pixel = source.toImage(point)
        if (!source.contains(pixel)) { feedback("Choose a point inside the source image"); return }
        val points = state.value.referencePoints + pixel
        if (points.size==2 && points[0].distanceTo(points[1])<8.0) { feedback("Choose points farther apart"); return }
        _state.update { it.copy(referencePoints=points,siteTool=if(points.size==2) SiteTool.NONE else SiteTool.CALIBRATE) }
    }
    fun calibrateSite(distanceMetres: Double) {
        val source = session?.document?.siteImage ?: return
        val points = state.value.referencePoints
        if(points.size!=2) return
        try {
            execute(DesignCommand.SetSiteImage(source.calibrated(points[0],points[1],distanceMetres),source))
            _state.update { it.copy(referencePoints=emptyList()) }; fit()
        } catch (error: IllegalArgumentException) { feedback(error.message) }
    }
    fun select(id: String?) { stopSiteTool(); _state.update { it.copy(selectedId = id, message = null) } }
    fun addOutline(curved: Boolean, kind: DesignObjectKind = DesignObjectKind.POOL) {
        val doc = session?.document ?: return
        val value = DesignStartingShapes.create(curved, doc.objects.size, kind)
        execute(DesignCommand.Add(value))
        if (session?.document?.objects?.any { it.id == value.id } == true) {
            _state.update { it.copy(selectedId = value.id, fitRequest = it.fitRequest + 1) }
        }
    }
    fun preview(command: DesignCommand, expectedRevision: Long? = null) {
        val current = session ?: return
        if (expectedRevision != null && expectedRevision != current.document.revision) {
            cancelPreview()
            feedback("The design changed while this gesture was open. Start the edit again.")
            return
        }
        try {
            val next = current.preview(command)
            pendingCommand = command
            _state.update { it.copy(preview = next, message = null) }
        } catch (error: IllegalArgumentException) {
            pendingCommand = null
            _state.update { it.copy(preview = null, message = error.message ?: "This edit is not valid") }
        }
    }
    fun commitPreview() {
        val command = pendingCommand
        cancelPreview()
        if (command != null) execute(command)
    }
    fun cancelPreview() { pendingCommand = null; _state.update { it.copy(preview = null) } }
    fun execute(command: DesignCommand) {
        val current = session ?: return
        cancelPreview()
        try {
            val before = current.document
            val next = current.execute(command)
            if (before != next) publishAndSave()
        } catch (error: IllegalArgumentException) { feedback(error.message ?: "This edit is not valid") }
    }
    fun undo() { cancelPreview(); if (session?.canUndo == true) { session!!.undo(); publishAndSave() } }
    fun redo() { cancelPreview(); if (session?.canRedo == true) { session!!.redo(); publishAndSave() } }
    fun retrySave() { session?.document?.let { _state.update { s -> s.copy(saveError = null) }; writes.trySend(it) } }
    fun fit() { _state.update { it.copy(fitRequest = it.fitRequest + 1) } }
    fun feedback(message: String?) { _state.update { it.copy(message = message) } }
    private fun publishAndSave() {
        val current = session ?: return
        _state.update { it.copy(document = current.document, preview = null, message = null, saveError = null,
            canUndo = current.canUndo, canRedo = current.canRedo,
            siteTool = SiteTool.NONE, referencePoints = emptyList(),
            selectedId = it.selectedId?.takeIf { id -> current.document.objects.any { obj -> obj.id == id } }) }
        refreshSource()
        check(writes.trySend(current.document).isSuccess)
    }
    override fun onCleared() { writes.close(); super.onCleared() }
}
