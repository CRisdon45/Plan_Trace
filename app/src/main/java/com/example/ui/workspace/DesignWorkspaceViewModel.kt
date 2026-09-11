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

enum class SiteTool { NONE, CALIBRATE, CHECK, MOVE }

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
    val referencePoints: List<ImagePoint> = emptyList(),
    val referencePurpose: SiteTool = SiteTool.CALIBRATE,
    val sideEditing: Boolean = false,
    val smoothMode: SmoothEditMode = SmoothEditMode.OFF,
    val smoothFocusId: String? = null,
    val smoothDraft: SmoothPoolDraft? = null,
    val smoothPreview: ProjectDesign? = null,
    val siteDraft: SiteOutlineDraft? = null,
    val draftCursor: DesignPoint? = null,
    val draftTarget: DrawingTarget? = null,
    val proposedDraft: ProposedOutlineDraft? = null,
    val draftOrthogonal: Boolean = false
) {
    val isDrawing: Boolean get() = siteDraft != null || proposedDraft != null || smoothDraft != null
    val drawingPoints: List<DesignPoint>? get() = siteDraft?.points ?: proposedDraft?.points ?: smoothDraft?.points
    val shownDocument: ProjectDesign? get() = preview ?: smoothPreview ?: document
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
        stopSideEditing()
        if (session?.document?.siteImage?.visible != true || state.value.siteBitmap == null) return
        if (tool == SiteTool.CHECK && session?.document?.siteImage?.calibration == null) {
            feedback("Set the image scale before checking another distance"); return
        }
        stopSiteTool()
        // Checking and calibration collect the same two-pointer reference geometry. Purpose is
        // retained separately when the gesture completes and the distance panel reopens.
        _state.update { it.copy(siteTool=if(tool == SiteTool.CHECK) SiteTool.CALIBRATE else tool,
            referencePurpose=tool, referencePoints=emptyList(), message=null) }
    }
    fun stopSiteTool() { cancelPreview(); _state.update { it.copy(siteTool=SiteTool.NONE,referencePoints=emptyList(),siteDraft=null,proposedDraft=null,smoothDraft=null,smoothPreview=null,draftCursor=null,draftTarget=null) } }
    fun beginSiteOutline(role: SiteOutlineRole) {
        stopSideEditing(); stopSiteTool()
        val doc=session?.document ?: return
        val source=doc.siteImage
        if(source?.calibration==null || !source.visible || state.value.siteBitmap==null) {
            feedback("Show a calibrated source before drawing a measured site outline"); return
        }
        val draft=SiteOutlineDraft(role,source,doc.id,doc.revision)
        _state.update { it.copy(siteDraft=draft,selectedId=null,draftOrthogonal=role==SiteOutlineRole.HOUSE,message=null) }
    }
    fun beginProposedOutline(kind: DesignObjectKind) {
        stopSideEditing(); stopSiteTool()
        val doc=session?.document ?: return
        _state.update { it.copy(proposedDraft=ProposedOutlineDraft(kind,doc.id,doc.revision),
            selectedId=null,draftOrthogonal=true,message=null) }
    }
    fun beginSmoothPool() {
        stopSideEditing(); stopSiteTool()
        val doc=session?.document ?: return
        _state.update { it.copy(smoothDraft=SmoothPoolDraft(doc.id,doc.revision),selectedId=null,draftOrthogonal=false,message=null) }
    }
    fun setSmoothMode(mode: SmoothEditMode) {
        stopSiteTool()
        val obj=session?.document?.objects?.firstOrNull { it.id==state.value.selectedId }
        if(mode!=SmoothEditMode.OFF && !SmoothPoolEditing.canEdit(obj)) { feedback("Select an unlocked smooth pool"); return }
        val focus=obj?.boundary?.nodes?.firstOrNull()
        _state.update { it.copy(sideEditing=false,smoothMode=mode,
            smoothFocusId=if(mode==SmoothEditMode.RADIUS) focus?.edgeId else focus?.vertexId,message=null) }
    }
    fun focusSmooth(hit: DesignHit) { _state.update { it.copy(smoothFocusId=when(hit) {
        is DesignHit.SmoothAnchor -> hit.vertexId
        is DesignHit.SmoothRadius -> hit.edgeId
        else -> it.smoothFocusId
    }) } }
    private fun refreshSmoothDraft() {
        val s=state.value; val draft=s.smoothDraft ?: return
        val doc=session?.document ?: return
        if(doc.id!=draft.documentId || doc.revision!=draft.revision) { stopSiteTool();feedback("The design changed. Start the pool again");return }
        try {
            val t=s.draftTarget
            val candidate=if(t!=null && !t.closing && (draft.points.lastOrNull()?.distanceTo(t.point) ?: 1.0)>=0.02) draft.append(t.point) else draft
            val next=if(candidate.points.size>=3) ProjectDesign(doc.id,doc.objects+candidate.finish(),doc.revision,doc.siteImage) else null
            _state.update { it.copy(smoothPreview=next,message=null) }
        } catch(e:IllegalArgumentException) { _state.update { it.copy(smoothPreview=null,message=e.message) } }
    }
    fun toggleSiteOrthogonal() { _state.update { it.copy(draftOrthogonal=!it.draftOrthogonal,draftCursor=null,draftTarget=null) } }
    fun previewSiteCorner(point: DesignPoint?, grid: Boolean=false, closeToleranceMetres: Double=0.0,
                          geometry: GeometrySnapIndex?=null, snapToleranceMetres: Double=0.1) {
        val points=state.value.drawingPoints ?: return
        val target=point?.let { CornerTarget.resolve(points,it,state.value.draftOrthogonal,grid,closeToleranceMetres,geometry,snapToleranceMetres,state.value.draftTarget?.snap?.key) }
        _state.update { it.copy(draftCursor=target?.point,draftTarget=target) }; refreshSmoothDraft()
    }
    fun markSiteCorner(point: DesignPoint, closeToleranceMetres: Double, grid: Boolean=false,
                       geometry: GeometrySnapIndex?=null, snapToleranceMetres: Double=0.1) {
        val current=state.value; val points=current.drawingPoints ?: return
        val id=current.siteDraft?.documentId ?: current.proposedDraft?.documentId ?: current.smoothDraft!!.documentId
        val revision=current.siteDraft?.revision ?: current.proposedDraft?.revision ?: current.smoothDraft!!.revision
        if(session?.document?.revision!=revision || session?.document?.id!=id) {
            stopSiteTool(); feedback("The design changed. Start the outline again"); return
        }
        val target=CornerTarget.resolve(points,point,current.draftOrthogonal,grid,closeToleranceMetres,geometry,snapToleranceMetres,current.draftTarget?.snap?.key)
        if(target.closing) { finishSiteOutline();return }
        try {
            val site=current.siteDraft?.append(target.point)
            val proposed=current.proposedDraft?.append(target.point)
            _state.update { it.copy(siteDraft=site,proposedDraft=proposed,smoothDraft=current.smoothDraft?.append(target.point),draftCursor=null,draftTarget=null,message=null) }
        } catch(e:IllegalArgumentException) { feedback(e.message) }
        refreshSmoothDraft()
    }
    fun backSiteCorner() {
        _state.update { it.copy(siteDraft=it.siteDraft?.back(),proposedDraft=it.proposedDraft?.back(),smoothDraft=it.smoothDraft?.back(),
            draftCursor=null,draftTarget=null,message=null) }; refreshSmoothDraft()
    }
    fun finishSiteOutline() {
        val current=state.value
        if(!current.isDrawing) return
        val doc=session?.document ?: return
        val site=current.siteDraft;val proposed=current.proposedDraft;val smooth=current.smoothDraft
        try {
            require(doc.id==(site?.documentId ?: proposed?.documentId ?: smooth!!.documentId) &&
                doc.revision==(site?.revision ?: proposed?.revision ?: smooth!!.revision) && (site==null || doc.siteImage==site.source)) {
                "The source or design changed. Cancel and restart the outline"
            }
            val objectToAdd=site?.finish() ?: proposed?.finish() ?: smooth!!.finish()
            execute(DesignCommand.Add(objectToAdd))
            if(session?.document?.objects?.any { it.id==objectToAdd.id }==true) {
                _state.update { it.copy(siteDraft=null,proposedDraft=null,smoothDraft=null,smoothPreview=null,draftCursor=null,draftTarget=null,selectedId=objectToAdd.id,
                    smoothMode=if(smooth!=null) SmoothEditMode.SHAPE else SmoothEditMode.OFF,
                    smoothFocusId=if(smooth!=null) objectToAdd.boundary.nodes.first().vertexId else null,
                    message=if(site!=null) "${objectToAdd.name} created as traced and locked. Use Site controls to unlock it deliberately"
                        else if(objectToAdd.coping!=null) "Pool created with following coping" else "Deck outline created. Shared surface cutouts are not implemented yet") }
            }
        } catch(e:IllegalArgumentException) { feedback(e.message) }
    }
    fun markSiteReference(point: DesignPoint) {
        val source = session?.document?.siteImage ?: return
        if (state.value.siteTool !in setOf(SiteTool.CALIBRATE, SiteTool.CHECK)) return
        val pixel = source.toImage(point)
        if (!source.contains(pixel)) { feedback("Choose a point inside the source image"); return }
        val points = state.value.referencePoints + pixel
        if (points.size==2 && points[0].distanceTo(points[1])<8.0) { feedback("Choose points farther apart"); return }
        _state.update { it.copy(referencePoints=points,siteTool=if(points.size==2) SiteTool.NONE else state.value.siteTool) }
    }
    fun calibrateSite(distanceMetres: Double) {
        val source = session?.document?.siteImage ?: return
        val points = state.value.referencePoints
        if(points.size!=2) return
        try {
            val checking = state.value.referencePurpose == SiteTool.CHECK
            val next = if (checking) source.checked(points[0], points[1], distanceMetres)
                else source.calibrated(points[0], points[1], distanceMetres)
            execute(DesignCommand.SetSiteImage(next,source))
            _state.update { it.copy(referencePoints=emptyList()) }
            if (!checking) fit()
        } catch (error: IllegalArgumentException) { feedback(error.message) }
    }
    fun select(id: String?) {
        stopSiteTool()
        _state.update { it.copy(selectedId=id, sideEditing=it.sideEditing && it.selectedId==id, smoothMode=if(it.selectedId==id) it.smoothMode else SmoothEditMode.OFF, smoothFocusId=if(it.selectedId==id) it.smoothFocusId else null, message=null) }
    }
    fun toggleSideEditing() {
        stopSiteTool()
        val obj=session?.document?.objects?.firstOrNull { it.id==state.value.selectedId }
        if(!StraightSideEditing.canEdit(obj)) { feedback("Select an unlocked pool with supported straight sides"); return }
        _state.update { it.copy(sideEditing=!it.sideEditing,smoothMode=SmoothEditMode.OFF,smoothFocusId=null,message=null) }
    }
    fun stopSideEditing() { cancelPreview(); _state.update { it.copy(sideEditing=false,smoothMode=SmoothEditMode.OFF,smoothFocusId=null) } }
    fun addOutline(curved: Boolean, kind: DesignObjectKind = DesignObjectKind.POOL) {
        stopSideEditing()
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
    fun undo() { if(state.value.isDrawing) { backSiteCorner();return }; cancelPreview(); if (session?.canUndo == true) { session!!.undo(); publishAndSave() } }
    fun redo() { if(state.value.isDrawing) return; cancelPreview(); if (session?.canRedo == true) { session!!.redo(); publishAndSave() } }
    fun retrySave() { session?.document?.let { _state.update { s -> s.copy(saveError = null) }; writes.trySend(it) } }
    fun fit() { _state.update { it.copy(fitRequest = it.fitRequest + 1) } }
    fun feedback(message: String?) { _state.update { it.copy(message = message) } }
    private fun publishAndSave() {
        val current = session ?: return
        _state.update { it.copy(document = current.document, preview = null, message = null, saveError = null,
            canUndo = current.canUndo, canRedo = current.canRedo,
            sideEditing = it.sideEditing && StraightSideEditing.canEdit(current.document.objects.firstOrNull { obj -> obj.id==it.selectedId }),
            siteTool = SiteTool.NONE, referencePoints = emptyList(), siteDraft=null, proposedDraft=null, smoothDraft=null, smoothPreview=null, draftCursor=null, draftTarget=null,
            smoothMode=if(SmoothPoolEditing.canEdit(current.document.objects.firstOrNull { obj -> obj.id==it.selectedId })) it.smoothMode else SmoothEditMode.OFF,
            selectedId = it.selectedId?.takeIf { id -> current.document.objects.any { obj -> obj.id == id } }) }
        refreshSource()
        check(writes.trySend(current.document).isSuccess)
    }
    override fun onCleared() { writes.close(); super.onCleared() }
}
