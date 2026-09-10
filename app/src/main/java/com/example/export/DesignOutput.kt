package com.example.export

import android.content.Context
import android.graphics.RectF
import com.example.data.SiteImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.model.*
import com.example.model.design.*
import java.io.File
import java.util.Locale

/** Explicit metric-to-drawing projection; sampled output is never saved back into authority. */
data class DesignOutputSettings(
    val origin: DesignPoint = DesignPoint(0.0, 0.0),
    val drawingUnitsPerMetre: Double = 100.0,
    val maxChordErrorMetres: Double = 0.001,
    val imperialLabels: Boolean = true,
    val includeMeasurements: Boolean = true,
    val includeSourceImage: Boolean = true,
    val includeSourceNotice: Boolean = true
) {
    init {
        require(drawingUnitsPerMetre.isFinite() && drawingUnitsPerMetre in 1.0..10000.0)
        require(maxChordErrorMetres.isFinite() && maxChordErrorMetres > 0.0)
    }
}

object DesignOutput {
    /** Same display/export path for the pool and its derived coping. No independent editable band. */
    fun drawing(document: ProjectDesign, settings: DesignOutputSettings = DesignOutputSettings()): TraceProject {
        val layer = DrawingLayer(id = "project-design-projection", name = "Project design (output)", isLocked = true)
        fun projected(points: List<DesignPoint>): List<Point2D> = points.map { point ->
            val wx = (point.x - settings.origin.x) * settings.drawingUnitsPerMetre
            val wy = -(point.y - settings.origin.y) * settings.drawingUnitsPerMetre
            val x = wx.toFloat(); val y = wy.toFloat()
            require(x.isFinite() && y.isFinite()) { "Design exceeds the renderer's coordinate range" }
            require(kotlin.math.hypot(x.toDouble() - wx, y.toDouble() - wy) / settings.drawingUnitsPerMetre <= settings.maxChordErrorMetres / 2) {
                "Projection precision budget exceeded; choose an origin near the design"
            }
            Point2D(x, y)
        }
        val elements = document.objects.flatMap { obj ->
            val footprint = obj.copingFootprint
            require(footprint == null || settings.maxChordErrorMetres >= 2 * PoolCoping.CHORD_ERROR_METRES) {
                "Requested output tolerance is finer than the coping generator supports"
            }
            val points = projected(obj.boundary.sample(settings.maxChordErrorMetres / 2.0))
            val result = mutableListOf<VectorElement>()
            if (footprint != null) {
                // Opaque water covers the interior of the opaque outer field in this object's group.
                // SurfaceMaterial's existing renderer draws exact sampled boundaries without wash bleed.
                result.add(PolylineElement(id = "${obj.id}:coping-outer", layerId = layer.id,
                    points = projected(footprint.outerBoundary), isClosed = true,
                    strokeWidth = 1.3f, material = SurfaceMaterial.PAVING))
            }
            result.add(PolylineElement(id = "${obj.id}:outline", layerId = layer.id,
                points = points, isClosed = true, strokeWidth = 2f,
                material = if (footprint != null) SurfaceMaterial.WATER else null))
            if (settings.includeMeasurements) {
                val units = if (settings.imperialLabels) "ft" else "m"
                val length = obj.boundary.perimeterMetres / if (settings.imperialLabels) 0.3048 else 1.0
                val copingLabel = obj.coping?.let { String.format(Locale.US, "\nCoping %.2f in", it.widthInches) }.orEmpty()
                result.add(TextElement(id = "${obj.id}:measurement", layerId = layer.id,
                    text = String.format(Locale.US, "%s\nPerimeter %.2f %s", obj.name, length, units) + copingLabel,
                    position = Point2D(points.minOf { it.x }, points.minOf { it.y } - 90f), fontSizeSp = 9f))
            }
            result
        }
        val notice = document.siteImage?.takeIf { it.visible && settings.includeSourceImage && settings.includeSourceNotice }?.let { source ->
            val r = sourceRect(source, settings)
            listOf(TextElement(id = "site-source-notice", layerId = layer.id,
                text = if (source.calibration == null) "SOURCE SCALE NOT SET - reference image only"
                    else "Source scaled from reference distance - site accuracy unverified",
                position = Point2D(r.left, r.top - 25f), fontSizeSp = 8f))
        } ?: emptyList()
        return TraceProject(id = document.id, title = "Project geometry study", createdAt = 0L, updatedAt = document.revision,
            backgroundType = BackgroundType.BLANK_PAPER, backgroundResourceOrUri = "",
            layers = listOf(layer), activeLayerId = layer.id, elements = elements + notice,
            scaleCalibration = ScaleCalibration(true,
                (settings.drawingUnitsPerMetre * if (settings.imperialLabels) 0.3048 else 1.0).toFloat(),
                1f, if (settings.imperialLabels) "ft" else "m"))
    }

    /** Upright source image registration expressed in the renderer's common drawing coordinates. */
    fun sourceRect(source: SiteImage, settings: DesignOutputSettings = DesignOutputSettings()): RectF {
        val a = source.corners()[0]; val b = source.corners()[1]
        val coordinates = listOf(a.x-settings.origin.x, -(a.y-settings.origin.y), b.x-settings.origin.x, -(b.y-settings.origin.y))
            .map { it * settings.drawingUnitsPerMetre }
        require(coordinates.all { it.isFinite() && it.toFloat().isFinite() }) { "Source is outside the renderer range" }
        require(coordinates.all { kotlin.math.abs(it.toFloat().toDouble()-it)/settings.drawingUnitsPerMetre <= settings.maxChordErrorMetres/2 }) {
            "Source projection precision exceeded; choose a local origin"
        }
        return RectF(coordinates[0].toFloat(),coordinates[1].toFloat(),coordinates[2].toFloat(),coordinates[3].toFloat())
    }

    suspend fun png(context: Context, document: ProjectDesign, width: Int = 1200, height: Int = 900,
                    settings: DesignOutputSettings = DesignOutputSettings()): File? = withContext(Dispatchers.IO) {
        val source = document.siteImage?.takeIf { it.visible && settings.includeSourceImage }
        val bitmap = source?.let { SiteImageStore.inFiles(context.filesDir).load(it.asset) }
        try {
            ExportManager.exportToPng(context, drawing(document, settings), bitmap, includeBackground = bitmap != null,
                width = width, height = height, showDimensions = false, registeredBackground = source?.let { sourceRect(it,settings) })
        } finally { bitmap?.recycle() }
    }

    suspend fun pdf(context: Context, document: ProjectDesign, options: PdfExportOptions = PdfExportOptions(),
                    settings: DesignOutputSettings = DesignOutputSettings()): File? = withContext(Dispatchers.IO) {
        val applied = settings.copy(includeMeasurements=options.includeDimensions, includeSourceImage=options.includeBackground)
        val source = document.siteImage?.takeIf { it.visible && applied.includeSourceImage }
        val bitmap = source?.let { SiteImageStore.inFiles(context.filesDir).load(it.asset) }
        try {
            ExportManager.exportToPdf(context, drawing(document, applied), bitmap,
                options.copy(includeBackground=bitmap!=null, includeDimensions=false,
                    includeNorthArrow=options.includeNorthArrow && document.siteImage==null),
                registeredBackground=source?.let { sourceRect(it,applied) })
        } finally { bitmap?.recycle() }
    }
}
