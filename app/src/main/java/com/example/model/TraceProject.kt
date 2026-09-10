package com.example.model

import java.util.UUID

data class TraceProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Plan Sketch",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val backgroundType: BackgroundType = BackgroundType.SAMPLE,
    val backgroundResourceOrUri: String = "sample_pool", // "sample_pool", "sample_deck", or content:// URI
    val backgroundOpacity: Float = 0.85f,
    val isBackgroundLocked: Boolean = true,
    val pdfPageNumber: Int = 0,
    val pdfTotalPages: Int = 1,
    val scaleCalibration: ScaleCalibration = ScaleCalibration(),
    val layers: List<DrawingLayer> = defaultLayers(),
    val activeLayerId: String = DEFAULT_LAYER_1_ID,
    val elements: List<VectorElement> = emptyList(),
    val pageDrawings: Map<String, PageDrawing> = emptyMap()
) {
    val pageKey: String get() = pageKey(backgroundType, backgroundResourceOrUri, pdfPageNumber)

    fun currentPageDrawing() = PageDrawing(layers, activeLayerId, elements, scaleCalibration,
        backgroundOpacity, isBackgroundLocked)

    /** The live fields are authoritative; archive them before leaving a sheet. */
    fun openSheet(type: BackgroundType, source: String, page: Int = 0, pageCount: Int = 1): TraceProject {
        require(pageCount > 0 && page in 0 until pageCount)
        val saved = pageDrawings + (pageKey to currentPageDrawing())
        val drawing = saved[pageKey(type, source, page)] ?: PageDrawing()
        return copy(backgroundType = type, backgroundResourceOrUri = source,
            pdfPageNumber = page, pdfTotalPages = pageCount, pageDrawings = saved,
            layers = drawing.layers, activeLayerId = drawing.activeLayerId, elements = drawing.elements,
            scaleCalibration = drawing.scale, backgroundOpacity = drawing.backgroundOpacity,
            isBackgroundLocked = drawing.isBackgroundLocked, updatedAt = System.currentTimeMillis())
    }

    companion object {
        fun pageKey(type: BackgroundType, source: String, page: Int): String =
            "${type.name}:${source.length}:$source:${if (type == BackgroundType.PDF_URI) page else 0}"
        const val DEFAULT_LAYER_1_ID = "layer_tracing_base"
        const val DEFAULT_LAYER_2_ID = "layer_geometry"
        const val DEFAULT_LAYER_3_ID = "layer_annotations"

        fun defaultLayers(): List<DrawingLayer> = listOf(
            DrawingLayer(
                id = DEFAULT_LAYER_1_ID,
                name = "Layer 1: Base Linework",
                isVisible = true,
                isLocked = false,
                opacity = 1f
            ),
            DrawingLayer(
                id = DEFAULT_LAYER_2_ID,
                name = "Layer 2: Features & Geometry",
                isVisible = true,
                isLocked = false,
                opacity = 1f
            ),
            DrawingLayer(
                id = DEFAULT_LAYER_3_ID,
                name = "Layer 3: Notes & Dimensions",
                isVisible = true,
                isLocked = false,
                opacity = 1f
            )
        )
    }
}

data class PageDrawing(
    val layers: List<DrawingLayer> = TraceProject.defaultLayers(),
    val activeLayerId: String = TraceProject.DEFAULT_LAYER_1_ID,
    val elements: List<VectorElement> = emptyList(),
    val scale: ScaleCalibration = ScaleCalibration(),
    val backgroundOpacity: Float = 0.85f,
    val isBackgroundLocked: Boolean = true
)

enum class BackgroundType {
    SAMPLE,
    IMAGE_URI,
    PDF_URI,
    BLANK_GRID,
    BLANK_PAPER
}
