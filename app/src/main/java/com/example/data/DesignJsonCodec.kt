package com.example.data

import com.example.model.design.*
import org.json.JSONArray
import org.json.JSONObject

/** A separate, versioned document format. Does not reinterpret or migrate existing TraceProject data. */
object DesignJsonCodec {
    private const val VERSION = 4
    fun encode(document: ProjectDesign): String = JSONObject().apply {
        put("format", "plan-trace-project-design")
        put("version", VERSION)
        put("coordinateUnit", "metre")
        put("yAxis", "up")
        put("id", document.id)
        put("revision", document.revision)
        document.siteImage?.let { image ->
            put("siteImage", JSONObject().apply {
                put("sha256", image.asset.sha256); put("width", image.asset.width); put("height", image.asset.height)
                put("x", image.topLeft.x); put("y", image.topLeft.y); put("metresPerPixel", image.metresPerPixel)
                put("visible", image.visible)
                image.calibration?.let { c -> put("calibration", JSONObject().apply {
                    put("x1", c.first.x); put("y1", c.first.y); put("x2", c.second.x); put("y2", c.second.y)
                    put("distanceMetres", c.distanceMetres)
                }) }
                image.distanceCheck?.let { c -> put("distanceCheck", JSONObject().apply {
                    put("x1", c.first.x); put("y1", c.first.y); put("x2", c.second.x); put("y2", c.second.y)
                    put("distanceMetres", c.distanceMetres)
                }) }
            })
        }
        put("objects", JSONArray().apply {
            document.objects.forEach { item -> put(JSONObject().apply {
                put("id", item.id); put("name", item.name); put("kind", item.kind.name)
                put("locked", item.locked); put("confidence", item.confidence.name)
                item.sourceReference?.let { put("sourceReference", it) }
                item.coping?.let { spec ->
                    put("coping", JSONObject().put("widthMetres", spec.widthMetres).put("generatorVersion", spec.generatorVersion))
                }
                put("nodes", JSONArray().apply {
                    item.boundary.nodes.forEach { node -> put(JSONObject().apply {
                        put("vertexId", node.vertexId); put("edgeId", node.edgeId)
                        put("x", node.point.x); put("y", node.point.y); put("bulge", node.bulge)
                    }) }
                })
            }) }
        })
    }.toString().also { require(it.length <= 2_000_000) { "Design document exceeds the initial encode budget" } }

    /** Fail the complete read on unsupported or corrupt data; never return a silently truncated yard. */
    fun decode(json: String): ProjectDesign {
        require(json.length <= 2_000_000) { "Design document exceeds the initial decode budget" }
        val root = JSONObject(json)
        require(root.getString("format") == "plan-trace-project-design") { "Not a project-design document" }
        require(root.get("version") is Int && root.getInt("version") in 1..VERSION) { "Unsupported design version" }
        require(root.getString("coordinateUnit") == "metre" && root.getString("yAxis") == "up") {
            "Unsupported coordinate system"
        }
        require(root.get("revision") is Int || root.get("revision") is Long) { "Revision must be an integer" }
        val version = root.getInt("version")
        val objects = root.getJSONArray("objects")
        require(objects.length() <= 512)
        val result = (0 until objects.length()).map { i ->
            val obj = objects.getJSONObject(i)
            require(obj.get("locked") is Boolean) { "Lock state must be Boolean" }
            val nodes = obj.getJSONArray("nodes")
            require(nodes.length() in 2..4096)
            DesignObject(
                id = obj.getString("id"), name = obj.getString("name"),
                kind = DesignObjectKind.valueOf(obj.getString("kind")),
                boundary = DesignBoundary((0 until nodes.length()).map { j ->
                    val n = nodes.getJSONObject(j)
                    require(listOf("x", "y", "bulge").all { n.get(it) is Number }) { "Coordinates and bulge must be numeric" }
                    BoundaryNode(n.getString("vertexId"), DesignPoint(n.getDouble("x"), n.getDouble("y")),
                        n.getString("edgeId"), n.getDouble("bulge"))
                }),
                locked = obj.getBoolean("locked"),
                confidence = GeometryConfidence.valueOf(obj.getString("confidence")),
                sourceReference = if (obj.has("sourceReference")) obj.getString("sourceReference") else null,
                coping = if (obj.has("coping")) {
                    require(version >= 2) { "Version 1 cannot contain unrecognized coping intent" }
                    val spec = obj.getJSONObject("coping")
                    require(spec.get("widthMetres") is Number && spec.get("generatorVersion") is Int) { "Invalid coping specification" }
                    CopingSpec(spec.getDouble("widthMetres"), spec.getInt("generatorVersion"))
                } else null
            )
        }
        val image = if (root.has("siteImage")) {
            require(version >= 3) { "An older document cannot contain unrecognized source registration" }
            val i = root.getJSONObject("siteImage")
            require(i.get("width") is Int && i.get("height") is Int && i.get("visible") is Boolean)
            fun numeric(o: JSONObject, name: String): Double {
                require(o.get(name) is Number) { "Source coordinates must be numeric" }
                return o.getDouble(name)
            }
            val calibration = if (i.has("calibration")) i.getJSONObject("calibration").let { c ->
                ImageCalibration(ImagePoint(numeric(c,"x1"),numeric(c,"y1")),
                    ImagePoint(numeric(c,"x2"),numeric(c,"y2")),numeric(c,"distanceMetres"))
            } else null
            val check = if (i.has("distanceCheck")) {
                require(version >= 4) { "An older document cannot contain unrecognized distance-check evidence" }
                i.getJSONObject("distanceCheck").let { c ->
                    ImageDistanceCheck(ImagePoint(numeric(c,"x1"),numeric(c,"y1")),
                        ImagePoint(numeric(c,"x2"),numeric(c,"y2")),numeric(c,"distanceMetres"))
                }
            } else null
            SiteImage(SiteImageAsset(i.getString("sha256"), i.getInt("width"), i.getInt("height")),
                DesignPoint(numeric(i,"x"), numeric(i,"y")), numeric(i,"metresPerPixel"), calibration, i.getBoolean("visible"), check)
        } else null
        return ProjectDesign(root.getString("id"), result, root.getLong("revision"), image)
    }
}
