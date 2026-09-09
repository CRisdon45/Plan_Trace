package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.DimensionMarkup
import com.example.model.EllipseElement
import com.example.model.FreehandPath
import com.example.model.LineElement
import com.example.model.Point2D
import com.example.model.PolylineElement
import com.example.model.RectangleElement
import com.example.model.ScaleCalibration
import com.example.model.StrokeStyle
import com.example.model.TextElement
import com.example.model.TraceProject
import com.example.model.VectorElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class PdfSheetSize(val displayName: String, val widthPt: Int, val heightPt: Int) {
    TABLOID("Tabloid (11\" × 17\")", 1224, 792),
    ARCH_D("Arch D (24\" × 36\")", 2592, 1728),
    LETTER("Letter (8.5\" × 11\")", 792, 612),
    A3("ISO A3 (297 × 420 mm)", 1190, 842),
    A4("ISO A4 (210 × 297 mm)", 842, 595)
}

data class PdfExportOptions(
    val sheetSize: PdfSheetSize = PdfSheetSize.TABLOID,
    val isLandscape: Boolean = true,
    val includeBackground: Boolean = true,
    val includeTitleBlock: Boolean = true,
    val includeScaleBar: Boolean = true,
    val includeNorthArrow: Boolean = true,
    val includeDimensions: Boolean = true,
    val includeLayerLegend: Boolean = true,
    val sheetTitle: String = "SCHEMATIC TRACE PLAN",
    val sheetNumber: String = "A-101",
    val author: String = "Design Studio"
)

object ExportManager {

    /**
     * Renders the project onto a Canvas (shared by PNG and basic export)
     */
    fun renderProjectToCanvas(
        canvas: Canvas,
        project: TraceProject,
        backgroundBitmap: Bitmap?,
        includeBackground: Boolean = true,
        width: Float,
        height: Float,
        showDimensions: Boolean = true
    ) {
        // Draw paper background
        if (includeBackground && backgroundBitmap != null) {
            val bgPaint = Paint().apply {
                alpha = (project.backgroundOpacity * 255).toInt().coerceIn(0, 255)
                isFilterBitmap = true
            }
            val src = android.graphics.Rect(0, 0, backgroundBitmap.width, backgroundBitmap.height)
            val dst = RectF(0f, 0f, width, height)
            canvas.drawBitmap(backgroundBitmap, src, dst, bgPaint)
        } else {
            // Draw clean architectural tracing paper off-white
            canvas.drawColor(Color.parseColor("#FBFBF8"))
        }

        // Draw elements layer by layer
        for (layer in project.layers) {
            if (!layer.isVisible) continue
            val layerElements = project.elements.filter { it.layerId == layer.id }
            val layerAlpha = layer.opacity

            for (element in layerElements) {
                com.example.engine.WatercolorRenderer.render(
                    canvas = canvas,
                    element = element,
                    layerAlpha = layerAlpha,
                    scale = project.scaleCalibration,
                    showDimensions = showDimensions
                )
            }
        }
    }

    /**
     * Renders a complete professional architectural drawing sheet onto a PDF canvas
     */
    fun renderArchitecturalSheet(
        canvas: Canvas,
        project: TraceProject,
        backgroundBitmap: Bitmap?,
        options: PdfExportOptions,
        pageWidth: Float,
        pageHeight: Float
    ) {
        // 1. Pure crisp white paper background for printing
        canvas.drawColor(Color.WHITE)

        val margin = 28f
        val titleBlockHeight = if (options.includeTitleBlock) 64f else 0f
        val drawingRect = RectF(
            margin,
            margin,
            pageWidth - margin,
            pageHeight - margin - titleBlockHeight
        )

        // 2. Architectural Sheet Frame / Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        // Outer margin line
        canvas.drawRect(margin, margin, pageWidth - margin, pageHeight - margin, borderPaint)
        // Secondary drafting inner border
        canvas.drawRect(margin + 4f, margin + 4f, pageWidth - margin - 4f, pageHeight - margin - 4f, innerBorderPaint)

        // 3. Render the Plan Artwork inside drawing area
        canvas.save()
        canvas.clipRect(drawingRect)

        // Fill drawing area with subtle drafting paper tone if background not included
        if (!options.includeBackground) {
            val paperPaint = Paint().apply { color = Color.parseColor("#FAFAF9") }
            canvas.drawRect(drawingRect, paperPaint)
        }

        // Calculate scale to fit plan proportionally inside drawing area
        val contentW = 2048f
        val contentH = 1536f
        val scaleX = drawingRect.width() / contentW
        val scaleY = drawingRect.height() / contentH
        val scale = min(scaleX, scaleY) * 0.96f

        val offsetX = drawingRect.left + (drawingRect.width() - contentW * scale) / 2f
        val offsetY = drawingRect.top + (drawingRect.height() - contentH * scale) / 2f

        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        renderProjectToCanvas(
            canvas = canvas,
            project = project,
            backgroundBitmap = backgroundBitmap,
            includeBackground = options.includeBackground,
            width = contentW,
            height = contentH,
            showDimensions = options.includeDimensions
        )
        canvas.restore()

        // 4. Professional Architectural Title Block
        if (options.includeTitleBlock) {
            val tbTop = pageHeight - margin - titleBlockHeight
            val tbBottom = pageHeight - margin
            val tbLeft = margin
            val tbRight = pageWidth - margin

            // Title block separator line
            canvas.drawLine(tbLeft, tbTop, tbRight, tbTop, borderPaint)

            // Section widths: Scale bar & North (28%), Project & Sheet (42%), Metadata & Sign-off (30%)
            val totalW = tbRight - tbLeft
            val col1Right = tbLeft + totalW * 0.28f
            val col2Right = tbLeft + totalW * 0.70f

            canvas.drawLine(col1Right, tbTop, col1Right, tbBottom, innerBorderPaint)
            canvas.drawLine(col2Right, tbTop, col2Right, tbBottom, innerBorderPaint)

            val textPaintPrimary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                isFakeBoldText = true
            }
            val textPaintSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
            }

            // --- Column 1: Graphic Scale & North Arrow ---
            if (options.includeNorthArrow) {
                drawNorthArrow(canvas, tbLeft + 26f, tbTop + titleBlockHeight / 2f, 16f)
            }

            val scaleBarLeft = if (options.includeNorthArrow) tbLeft + 62f else tbLeft + 16f
            if (options.includeScaleBar) {
                drawGraphicScaleBar(
                    canvas = canvas,
                    x = scaleBarLeft,
                    y = tbTop + 34f,
                    width = col1Right - scaleBarLeft - 16f,
                    scaleCalibration = project.scaleCalibration
                )
            }

            val scaleText = if (project.scaleCalibration.isCalibrated) {
                "SCALE: CALIBRATED (${project.scaleCalibration.realWorldUnits} ${project.scaleCalibration.unit} = ${project.scaleCalibration.pixelDistance.roundToInt()} px)"
            } else {
                "SCALE: NOT TO SCALE (N.T.S.)"
            }
            textPaintSecondary.textSize = 8.5f
            canvas.drawText(scaleText, scaleBarLeft, tbTop + 54f, textPaintSecondary)

            // --- Column 2: Project Title & Sheet Identity ---
            textPaintPrimary.textSize = 14f
            canvas.drawText(project.title.uppercase(Locale.ROOT), col1Right + 16f, tbTop + 24f, textPaintPrimary)

            textPaintSecondary.textSize = 10f
            textPaintSecondary.isFakeBoldText = true
            canvas.drawText("${options.sheetNumber} - ${options.sheetTitle}", col1Right + 16f, tbTop + 42f, textPaintSecondary)

            val activeLayerNames = project.layers.filter { it.isVisible }.joinToString(", ") { it.name }
            textPaintSecondary.textSize = 8f
            textPaintSecondary.isFakeBoldText = false
            val displayLayers = if (activeLayerNames.length > 55) activeLayerNames.take(52) + "..." else activeLayerNames
            canvas.drawText("LAYERS: $displayLayers", col1Right + 16f, tbTop + 56f, textPaintSecondary)

            // --- Column 3: Professional Stamp & Date ---
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val dateStr = dateFormat.format(Date())

            textPaintSecondary.textSize = 8.5f
            canvas.drawText("DATE: $dateStr", col2Right + 14f, tbTop + 18f, textPaintSecondary)
            canvas.drawText("AUTHOR: ${options.author}", col2Right + 14f, tbTop + 32f, textPaintSecondary)

            val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#DC2626")
                textSize = 9f
                isFakeBoldText = true
            }
            canvas.drawText("PRELIMINARY / NOT FOR CONSTRUCTION", col2Right + 14f, tbTop + 52f, stampPaint)
        }
    }

    private fun drawNorthArrow(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawCircle(cx, cy, radius, circlePaint)

        // Upward arrow
        val arrowPath = Path().apply {
            moveTo(cx, cy - radius + 2f)
            lineTo(cx - radius * 0.45f, cy + radius * 0.55f)
            lineTo(cx, cy + radius * 0.15f)
            close()
        }
        val arrowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        canvas.drawPath(arrowPath, arrowFill)

        val arrowRightPath = Path().apply {
            moveTo(cx, cy - radius + 2f)
            lineTo(cx + radius * 0.45f, cy + radius * 0.55f)
            lineTo(cx, cy + radius * 0.15f)
            close()
        }
        val arrowStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawPath(arrowRightPath, arrowStroke)

        val nPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 9f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("N", cx, cy - radius - 3f, nPaint)
    }

    private fun drawGraphicScaleBar(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        scaleCalibration: ScaleCalibration
    ) {
        val barHeight = 6f
        val segments = 4
        val segW = (width / segments).coerceIn(24f, 50f)
        val totalBarW = segW * segments

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val fillBlack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        val fillWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 7.5f
            textAlign = Paint.Align.CENTER
        }

        // Draw segmented alternating blocks: [Black][White][Black][White]
        for (i in 0 until segments) {
            val sx = x + i * segW
            val rect = RectF(sx, y, sx + segW, y + barHeight)
            canvas.drawRect(rect, if (i % 2 == 0) fillBlack else fillWhite)
            canvas.drawRect(rect, strokePaint)
        }

        // Labels above ticks
        canvas.drawText("0", x, y - 3f, textPaint)
        val unitStr = if (scaleCalibration.isCalibrated) scaleCalibration.unit else "u"
        val stepVal = if (scaleCalibration.isCalibrated) {
            (scaleCalibration.realWorldUnits / 2f).roundToInt().coerceAtLeast(1)
        } else 5

        canvas.drawText("${stepVal}", x + segW, y - 3f, textPaint)
        canvas.drawText("${stepVal * 2}", x + segW * 2, y - 3f, textPaint)
        canvas.drawText("${stepVal * 4} $unitStr", x + totalBarW, y - 3f, textPaint)
    }

    suspend fun exportToPng(
        context: Context,
        project: TraceProject,
        backgroundBitmap: Bitmap?,
        includeBackground: Boolean = true,
        width: Int = 2048,
        height: Int = 1536
    ): File? = withContext(Dispatchers.IO) {
        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            renderProjectToCanvas(canvas, project, backgroundBitmap, includeBackground, width.toFloat(), height.toFloat())

            val cacheDir = File(context.cacheDir, "exports")
            cacheDir.mkdirs()
            val fileName = "${project.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")}_sketch.png"
            val file = File(cacheDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports a complete PDF drawing document according to [PdfExportOptions]
     */
    suspend fun exportToPdf(
        context: Context,
        project: TraceProject,
        backgroundBitmap: Bitmap?,
        options: PdfExportOptions = PdfExportOptions()
    ): File? = withContext(Dispatchers.IO) {
        try {
            val (pageW, pageH) = if (options.isLandscape) {
                options.sheetSize.widthPt to options.sheetSize.heightPt
            } else {
                options.sheetSize.heightPt to options.sheetSize.widthPt
            }

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            renderArchitecturalSheet(
                canvas = page.canvas,
                project = project,
                backgroundBitmap = backgroundBitmap,
                options = options,
                pageWidth = pageW.toFloat(),
                pageHeight = pageH.toFloat()
            )

            pdfDocument.finishPage(page)

            val cacheDir = File(context.cacheDir, "exports")
            cacheDir.mkdirs()
            val cleanTitle = project.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "${cleanTitle}_${options.sheetNumber}.pdf"
            val file = File(cacheDir, fileName)

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareExportedFile(context: Context, file: File, mimeType: String, subject: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share $subject"))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
