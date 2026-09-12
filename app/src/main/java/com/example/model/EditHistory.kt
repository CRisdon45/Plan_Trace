package com.example.model

/** History owns the complete layer/element state, including active-layer membership. */
class EditHistory(private val capacity: Int = 50) {
    data class State(val layers: List<DrawingLayer>, val elements: List<VectorElement>, val activeLayerId: String) {
        fun restore(project: TraceProject) = project.copy(layers = layers, elements = elements,
            activeLayerId = activeLayerId, updatedAt = System.currentTimeMillis())
        companion object {
            fun of(project: TraceProject) = State(project.layers, project.elements, project.activeLayerId)
        }
    }
    private val undo = mutableListOf<State>()
    private val redo = mutableListOf<State>()
    private var gestureStart: State? = null
    val isGestureActive get() = gestureStart != null
    val canUndo get() = undo.isNotEmpty()
    val canRedo get() = redo.isNotEmpty()

    fun clear() { undo.clear(); redo.clear(); gestureStart = null }
    fun begin(project: TraceProject) { if (gestureStart == null) gestureStart = State.of(project) }
    fun record(project: TraceProject) {
        if (gestureStart != null) return
        push(State.of(project))
    }
    private fun push(state: State) {
        undo.add(state)
        if (undo.size > capacity) undo.removeAt(0)
        redo.clear()
    }
    fun commit(project: TraceProject) {
        val start = gestureStart ?: return
        gestureStart = null
        if (start != State.of(project)) push(start)
    }
    fun cancel(project: TraceProject): TraceProject {
        val start = gestureStart ?: return project
        gestureStart = null
        return start.restore(project)
    }
    fun undo(project: TraceProject): TraceProject {
        commit(project)
        if (undo.isEmpty()) return project
        redo.add(State.of(project))
        return undo.removeAt(undo.lastIndex).restore(project)
    }
    fun redo(project: TraceProject): TraceProject {
        if (redo.isEmpty()) return project
        undo.add(State.of(project))
        return redo.removeAt(redo.lastIndex).restore(project)
    }
}
