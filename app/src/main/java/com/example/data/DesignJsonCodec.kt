package com.example.data

import com.example.model.design.*
import org.json.JSONArray
import org.json.JSONObject

/** A separate, versioned document format. Does not reinterpret or migrate existing TraceProject data. */
object DesignJsonCodec {
    private const val VERSION = 1
    fun encode(document: ProjectDesign): String = JSONObject().apply {
        put("format", "plan-trace-project-design")
        put("version", VERSION)
        put("coordinateUnit", "metre")
        put("yAxis", "up")
        put("id", document.id)
        put("revision", document.revision)
        put("objects", JSONArray().apply {
            document.objects.forEach { item -> put(JSONObject().apply {
                put("id", item.id); put("name", item.name); put("kind", item.kind.name)
                put("locked", item.locked); put("confidence", item.confidence.name)
                item.sourceReference?.let { put("sourceReference", it) }
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
        require(root.get("version") is Int && root.getInt("version") == VERSION) { "Unsupported design version" }
        require(root.getString("coordinateUnit") == "metre" && root.getString("yAxis") == "up") {
            "Unsupported coordinate system"
        }
        require(root.get("revision") is Int || root.get("revision") is Long) { "Revision must be an integer" }
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
                sourceReference = if (obj.has("sourceReference")) obj.getString("sourceReference") else null
            )
        }
        return ProjectDesign(root.getString("id"), result, root.getLong("revision"))
    }
}
