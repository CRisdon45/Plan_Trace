package com.example.export

import android.content.Context
import com.example.model.*
import com.example.model.design.*
import java.io.File
import java.util.Locale

/** Explicit metric-to-drawing projection. Samples are output-only, never saved back into the authority. */
data class DesignOutputSettings(
    val origin: DesignPoint = DesignPoint(0.0, 0.0),
    val drawingUnitsPerMetre: Double = 100.0,
    val maxChordErrorMetres: Double = 0.001,
    val imperialLabels: Boolean = true,
    val includeMeasurements: Boolean = true
) {
    init {
        require(drawingUnitsPerMetre.isFinite() && drawingUnitsPerMetre in 1.0..10000.0)
        require(maxChordErrorMetres.isFinite() && maxChordErrorMetres > 0.0)
    }
}

object DesignOutput {
    /**
     * Disposable read-only projection for the EXISTING canvas/export renderer, not another renderer
     * or an editable copy of the project. Use DesignSession for edits and DesignJsonCodec for storage.
     * Automatic chord-based dimensions must be disabled: generated labels use analytic perimeter.
     */
    fun drawing(document: ProjectDesign, settings: DesignOutputSettings = DesignOutputSettings()): TraceProject {
        val layer = DrawingLayer(id = "project-design-projection", name = "Project design (output)", isLocked = true)
        val elements = document.objects.flatMap { obj ->
            val points = obj.boundary.sample(settings.maxChordErrorMetres).map { point ->
                val x = ((point.x - settings.origin.x) * settings.drawingUnitsPerMetre).toFloat()
                val y = (-(point.y - settings.origin.y) * settings.drawingUnitsPerMetre).toFloat()
                require(x.isFinite() && y.isFinite()) { "Design exceeds the renderer's coordinate range" }
                Point2D(x, y)
            }
            val outline = PolylineElement(id = "${obj.id}:outline", layerId = layer.id,
                points = points, isClosed = true, strokeWidth = 2f)
            // No filled surface/area promise until topology and holes are validated.
            val result = mutableListOf<VectorElement>(outline)
            if (settings.includeMeasurements) {
                val units = if (settings.imperialLabels) "ft" else "m"
                val length = obj.boundary.perimeterMetres / if (settings.imperialLabels) 0.3048 else 1.0
                result.add(TextElement(id = "${obj.id}:measurement", layerId = layer.id,
                    text = String.format(Locale.US, "%s\nPerimeter %.2f %s", obj.name, length, units),
                    position = Point2D(points.minOf { it.x }, points.minOf { it.y } - 45f), fontSizeSp = 9f))
            }
            result
        }
        return TraceProject(id = document.id, title = "Project geometry study", createdAt = 0L, updatedAt = document.revision,
            backgroundType = BackgroundType.BLANK_PAPER, backgroundResourceOrUri = "",
            layers = listOf(layer), activeLayerId = layer.id, elements = elements,
            scaleCalibration = ScaleCalibration(true,
                (settings.drawingUnitsPerMetre * if (settings.imperialLabels) 0.3048 else 1.0).toFloat(),
                1f, if (settings.imperialLabels) "ft" else "m"))
    }

    suspend fun png(context: Context, document: ProjectDesign, width: Int = 1200, height: Int = 900,
                    settings: DesignOutputSettings = DesignOutputSettings()): File? =
        ExportManager.exportToPng(context, drawing(document, settings), null, includeBackground = false,
            width = width, height = height, showDimensions = false)

    suspend fun pdf(context: Context, document: ProjectDesign, options: PdfExportOptions = PdfExportOptions(),
                    settings: DesignOutputSettings = DesignOutputSettings()): File? =
        ExportManager.exportToPdf(context,
            drawing(document, settings.copy(includeMeasurements = options.includeDimensions)), null,
            options.copy(includeBackground = false, includeDimensions = false))
}
