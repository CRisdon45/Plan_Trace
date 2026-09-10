package com.example.ui.workspace

import android.app.Application
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
    val fitRequest: Int = 0
) {
    val shownDocument: ProjectDesign? get() = preview ?: document
    val saved: Boolean get() = document != null && savedRevision == document.revision && saveError == null
}

class DesignWorkspaceViewModel(application: Application) : AndroidViewModel(application) {
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
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                _state.value = WorkspaceState(loading = false,
                    loadError = "The saved design could not be opened. Its files have been kept unchanged. ${error.message.orEmpty()}")
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
                    _state.update { it.copy(saveError = "Not saved: ${error.message.orEmpty()}") }
                }
            }
        }
    }
    fun select(id: String?) { cancelPreview(); _state.update { it.copy(selectedId = id, message = null) } }
    fun addOutline(curved: Boolean, kind: DesignObjectKind = DesignObjectKind.POOL) {
        val doc = session?.document ?: return
        val value = DesignStartingShapes.create(curved, doc.objects.size, kind)
        execute(DesignCommand.Add(value))
        if (session?.document?.objects?.any { it.id == value.id } == true) {
            _state.update { it.copy(selectedId = value.id, fitRequest = it.fitRequest + 1) }
        }
    }
    fun preview(command: DesignCommand) {
        val current = session ?: return
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
            selectedId = it.selectedId?.takeIf { id -> current.document.objects.any { obj -> obj.id == id } }) }
        check(writes.trySend(current.document).isSuccess)
    }
    override fun onCleared() { writes.close(); super.onCleared() }
}
