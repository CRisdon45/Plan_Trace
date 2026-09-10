package com.example.data

import com.example.data.db.ProjectEntity
import com.example.model.BackgroundType
import com.example.model.DimensionMarkup
import com.example.model.DrawingLayer
import com.example.model.EllipseElement
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
import com.example.model.PageDrawing
import com.example.model.VectorElement
import org.json.JSONArray
import org.json.JSONObject

object ProjectJsonConverter {

    fun toEntity(project: TraceProject): ProjectEntity {
        return ProjectEntity(
            id = project.id,
            title = project.title,
            createdAt = project.createdAt,
            updatedAt = System.currentTimeMillis(),
            backgroundType = project.backgroundType.name,
            backgroundResourceOrUri = project.backgroundResourceOrUri,
            backgroundOpacity = project.backgroundOpacity,
            isBackgroundLocked = project.isBackgroundLocked,
            pdfPageNumber = project.pdfPageNumber,
            pdfTotalPages = project.pdfTotalPages,
            isCalibrated = project.scaleCalibration.isCalibrated,
            pixelDistance = project.scaleCalibration.pixelDistance,
            realWorldUnits = project.scaleCalibration.realWorldUnits,
            unit = project.scaleCalibration.unit,
            layersJson = serializeLayers(project.layers),
            activeLayerId = project.activeLayerId,
            elementsJson = serializeElements(project.elements),
            pageDrawingsJson = serializePages(project.pageDrawings + (project.pageKey to project.currentPageDrawing()))
        )
    }

    fun fromEntity(entity: ProjectEntity): TraceProject {
        val bgType = try {
            BackgroundType.valueOf(entity.backgroundType)
        } catch (e: Exception) {
            BackgroundType.SAMPLE
        }

        val layers = deserializeLayers(entity.layersJson)
        val elements = deserializeElements(entity.elementsJson)

        return TraceProject(
            id = entity.id,
            title = entity.title,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            backgroundType = bgType,
            backgroundResourceOrUri = entity.backgroundResourceOrUri,
            backgroundOpacity = entity.backgroundOpacity,
            isBackgroundLocked = entity.isBackgroundLocked,
            pdfPageNumber = entity.pdfPageNumber,
            pdfTotalPages = entity.pdfTotalPages,
            scaleCalibration = ScaleCalibration(
                isCalibrated = entity.isCalibrated,
                pixelDistance = entity.pixelDistance,
                realWorldUnits = entity.realWorldUnits,
                unit = entity.unit
            ),
            layers = if (layers.isNotEmpty()) layers else TraceProject.defaultLayers(),
            activeLayerId = entity.activeLayerId,
            elements = elements,
            pageDrawings = deserializePages(entity.pageDrawingsJson)
        )
    }

    fun serializePages(pages: Map<String, PageDrawing>): String = JSONObject().apply {
        pages.forEach { (key, page) -> put(key, JSONObject().apply {
            put("layers", JSONArray(serializeLayers(page.layers)))
            put("activeLayerId", page.activeLayerId)
            put("elements", JSONArray(serializeElements(page.elements)))
            put("isCalibrated", page.scale.isCalibrated)
            put("pixelDistance", page.scale.pixelDistance.toDouble())
            put("realWorldUnits", page.scale.realWorldUnits.toDouble())
            put("unit", page.scale.unit)
            put("backgroundOpacity", page.backgroundOpacity.toDouble())
            put("isBackgroundLocked", page.isBackgroundLocked)
        }) }
    }.toString()

    fun deserializePages(json: String): Map<String, PageDrawing> {
        val root = JSONObject(json)
        return root.keys().asSequence().associateWith { key ->
            val page = root.getJSONObject(key)
            PageDrawing(
                layers = deserializeLayers(page.getJSONArray("layers").toString()),
                activeLayerId = page.getString("activeLayerId"),
                elements = deserializeElements(page.getJSONArray("elements").toString()),
                scale = ScaleCalibration(page.getBoolean("isCalibrated"), page.getDouble("pixelDistance").toFloat(),
                    page.getDouble("realWorldUnits").toFloat(), page.getString("unit")),
                backgroundOpacity = page.getDouble("backgroundOpacity").toFloat(),
                isBackgroundLocked = page.getBoolean("isBackgroundLocked")
            )
        }
    }

    fun serializeLayers(layers: List<DrawingLayer>): String {
        val arr = JSONArray()
        for (l in layers) {
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("name", l.name)
            obj.put("isVisible", l.isVisible)
            obj.put("isLocked", l.isLocked)
            obj.put("opacity", l.opacity.toDouble())
            obj.put("blendMode", l.blendMode.name)
            obj.put("colorTag", l.colorTag)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeLayers(json: String): List<DrawingLayer> {
        if (json.isBlank()) return TraceProject.defaultLayers()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<DrawingLayer>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val blendMode = try {
                    LayerBlendMode.valueOf(obj.optString("blendMode", "NORMAL"))
                } catch (e: Exception) {
                    LayerBlendMode.NORMAL
                }
                list.add(
                    DrawingLayer(
                        id = obj.optString("id"),
                        name = obj.optString("name", "Layer ${i + 1}"),
                        isVisible = obj.optBoolean("isVisible", true),
                        isLocked = obj.optBoolean("isLocked", false),
                        opacity = obj.optDouble("opacity", 1.0).toFloat(),
                        blendMode = blendMode,
                        colorTag = obj.optLong("colorTag", 0xFF2563EB)
                    )
                )
            }
            if (list.isEmpty()) TraceProject.defaultLayers() else list
        } catch (e: Exception) {
            TraceProject.defaultLayers()
        }
    }

    fun serializeElements(elements: List<VectorElement>): String {
        val arr = JSONArray()
        for (el in elements) {
            val obj = JSONObject()
            obj.put("id", el.id)
            obj.put("layerId", el.layerId)
            obj.put("strokeColor", el.strokeColor)
            obj.put("strokeWidth", el.strokeWidth.toDouble())
            obj.put("style", el.style.name)
            obj.put("alpha", el.alpha.toDouble())

            when (el) {
                is FreehandPath -> {
                    obj.put("type", "FreehandPath")
                    obj.put("isClosed", el.isClosed)
                    if (el.fillColor != null) obj.put("fillColor", el.fillColor)
                    val pts = JSONArray()
                    for (p in el.points) {
                        pts.put(JSONObject().put("x", p.x.toDouble()).put("y", p.y.toDouble()).put("p", p.pressure.toDouble()))
                    }
                    obj.put("points", pts)
                }
                is LineElement -> {
                    obj.put("type", "Line")
                    obj.put("showDimension", el.showDimension)
                    obj.put("x1", el.start.x.toDouble())
                    obj.put("y1", el.start.y.toDouble())
                    obj.put("x2", el.end.x.toDouble())
                    obj.put("y2", el.end.y.toDouble())
                }
                is PolylineElement -> {
                    obj.put("type", "Polyline")
                    obj.put("isClosed", el.isClosed)
                    if (el.fillColor != null) obj.put("fillColor", el.fillColor)
                    val pts = JSONArray()
                    for (p in el.points) {
                        pts.put(JSONObject().put("x", p.x.toDouble()).put("y", p.y.toDouble()).put("p", p.pressure.toDouble()))
                    }
                    obj.put("points", pts)
                }
                is RectangleElement -> {
                    obj.put("type", "Rectangle")
                    obj.put("left", el.left.toDouble())
                    obj.put("top", el.top.toDouble())
                    obj.put("right", el.right.toDouble())
                    obj.put("bottom", el.bottom.toDouble())
                    obj.put("isFilled", el.isFilled)
                    obj.put("fillColor", el.fillColor)
                }
                is EllipseElement -> {
                    obj.put("type", "Ellipse")
                    obj.put("centerX", el.centerX.toDouble())
                    obj.put("centerY", el.centerY.toDouble())
                    obj.put("radiusX", el.radiusX.toDouble())
                    obj.put("radiusY", el.radiusY.toDouble())
                    obj.put("isFilled", el.isFilled)
                    obj.put("fillColor", el.fillColor)
                }
                is TextElement -> {
                    obj.put("type", "Text")
                    obj.put("text", el.text)
                    obj.put("fontSizeSp", el.fontSizeSp.toDouble())
                    obj.put("posX", el.position.x.toDouble())
                    obj.put("posY", el.position.y.toDouble())
                }
                is DimensionMarkup -> {
                    obj.put("type", "Dimension")
                    obj.put("label", el.label)
                    obj.put("x1", el.start.x.toDouble())
                    obj.put("y1", el.start.y.toDouble())
                    obj.put("x2", el.end.x.toDouble())
                    obj.put("y2", el.end.y.toDouble())
                }
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeElements(json: String): List<VectorElement> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<VectorElement>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val type = obj.optString("type")
                val id = obj.optString("id")
                val layerId = obj.optString("layerId")
                val strokeColor = obj.optLong("strokeColor", 0xFF1E293B)
                val strokeWidth = obj.optDouble("strokeWidth", 3.0).toFloat()
                val style = try {
                    StrokeStyle.valueOf(obj.optString("style", "INK"))
                } catch (e: Exception) {
                    StrokeStyle.INK
                }
                val alpha = obj.optDouble("alpha", 1.0).toFloat()

                when (type) {
                    "FreehandPath" -> {
                        val ptsArr = obj.optJSONArray("points") ?: JSONArray()
                        val pts = mutableListOf<Point2D>()
                        for (pIdx in 0 until ptsArr.length()) {
                            val pObj = ptsArr.getJSONObject(pIdx)
                            pts.add(
                                Point2D(
                                    x = pObj.getDouble("x").toFloat(),
                                    y = pObj.getDouble("y").toFloat(),
                                    pressure = pObj.optDouble("p", 1.0).toFloat()
                                )
                            )
                        }
                        val isClosed = obj.optBoolean("isClosed", false)
                        val fillColor = if (obj.has("fillColor")) obj.getLong("fillColor") else null
                        list.add(
                            FreehandPath(
                                id = id,
                                layerId = layerId,
                                points = pts,
                                strokeColor = strokeColor,
                                strokeWidth = strokeWidth,
                                style = style,
                                alpha = alpha,
                                isClosed = isClosed,
                                fillColor = fillColor
                            )
                        )
                    }
                    "Line" -> {
                        val start = Point2D(obj.getDouble("x1").toFloat(), obj.getDouble("y1").toFloat())
                        val end = Point2D(obj.getDouble("x2").toFloat(), obj.getDouble("y2").toFloat())
                        list.add(
                            LineElement(
                                id = id,
                                layerId = layerId,
                                start = start,
                                end = end,
                                strokeColor = strokeColor,
                                strokeWidth = strokeWidth,
                                style = style,
                                alpha = alpha,
                                showDimension = obj.optBoolean("showDimension", false)
                            )
                        )
                    }
                    "Polyline" -> {
                        val ptsArr = obj.optJSONArray("points") ?: JSONArray()
                        val pts = mutableListOf<Point2D>()
                        for (pIdx in 0 until ptsArr.length()) {
                            val pObj = ptsArr.getJSONObject(pIdx)
                            pts.add(
                                Point2D(
                                    x = pObj.getDouble("x").toFloat(),
                                    y = pObj.getDouble("y").toFloat(),
                                    pressure = pObj.optDouble("p", 1.0).toFloat()
                                )
                            )
                        }
                        val isClosed = obj.optBoolean("isClosed", false)
                        val fillColor = if (obj.has("fillColor")) obj.getLong("fillColor") else null
                        list.add(
                            PolylineElement(
                                id = id,
                                layerId = layerId,
                                points = pts,
                                isClosed = isClosed,
                                strokeColor = strokeColor,
                                strokeWidth = strokeWidth,
                                style = style,
                                alpha = alpha,
                                fillColor = fillColor
                            )
                        )
                    }
                    "Rectangle" -> {
                        list.add(
                            RectangleElement(
                                id = id,
                                layerId = layerId,
                                left = obj.getDouble("left").toFloat(),
                                top = obj.getDouble("top").toFloat(),
                                right = obj.getDouble("right").toFloat(),
                                bottom = obj.getDouble("bottom").toFloat(),
                                strokeColor = strokeColor,
                                strokeWidth = strokeWidth,
                                style = style,
                                alpha = alpha,
                                isFilled = obj.optBoolean("isFilled", false),
                                fillColor = obj.optLong("fillColor", 0x333B82F6)
                            )
                        )
                    }
                    "Ellipse" -> {
                        list.add(
                            EllipseElement(
                                id = id,
                                layerId = layerId,
                                centerX = obj.getDouble("centerX").toFloat(),
                                centerY = obj.getDouble("centerY").toFloat(),
                                radiusX = obj.getDouble("radiusX").toFloat(),
                                radiusY = obj.getDouble("radiusY").toFloat(),
                                strokeColor = strokeColor,
                                strokeWidth = strokeWidth,
                                style = style,
                                alpha = alpha,
                                isFilled = obj.optBoolean("isFilled", false),
                                fillColor = obj.optLong("fillColor", 0x333B82F6)
                            )
                        )
                    }
                    "Text" -> {
                        val pos = Point2D(obj.getDouble("posX").toFloat(), obj.getDouble("posY").toFloat())
                        list.add(
                            TextElement(
                                id = id,
                                layerId = layerId,
                                text = obj.optString("text", ""),
                                position = pos,
                                fontSizeSp = obj.optDouble("fontSizeSp", 14.0).toFloat(),
                                strokeColor = strokeColor,
                                strokeWidth = strokeWidth,
                                style = style,
                                alpha = alpha
                            )
                        )
                    }
                    "Dimension" -> {
                        val start = Point2D(obj.getDouble("x1").toFloat(), obj.getDouble("y1").toFloat())
                        val end = Point2D(obj.getDouble("x2").toFloat(), obj.getDouble("y2").toFloat())
                        list.add(
                            DimensionMarkup(
                                id = id,
                                layerId = layerId,
                                start = start,
                                end = end,
                                label = obj.optString("label", ""),
                                strokeColor = strokeColor,
                                strokeWidth = strokeWidth,
                                style = style,
                                alpha = alpha
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
