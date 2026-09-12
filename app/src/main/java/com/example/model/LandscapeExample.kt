package com.example.model

/** An original editable practice plan, independent of reference artwork and bundled underlays. */
object LandscapeExample {
    const val TEMPLATE_KEY = "editable_landscape"

    fun create(title: String = "Courtyard study"): TraceProject {
        val ground = "example-ground"
        val features = "example-features"
        val notes = "example-notes"
        val elements = mutableListOf<VectorElement>()
        fun rect(id: String, x: Float, y: Float, w: Float, h: Float, material: SurfaceMaterial, layer: String = ground) {
            elements += RectangleElement(id = id, layerId = layer, left = x, top = y, right = x + w, bottom = y + h,
                material = material, strokeWidth = if (material == SurfaceMaterial.MASONRY) 2.5f else 1.4f)
        }
        fun label(id: String, text: String, x: Float, y: Float, size: Float = 16f) {
            elements += TextElement(id = id, layerId = notes, text = text, position = Point2D(x, y),
                fontSizeSp = size, strokeColor = 0xFF343832)
        }
        // Twenty world units represent one foot. Pool is exactly 20 by 12 feet.
        rect("patio", 160f, 180f, 680f, 520f, SurfaceMaterial.PAVING)
        rect("lawn", 880f, 180f, 360f, 520f, SurfaceMaterial.TURF)
        rect("gravel", 160f, 740f, 1080f, 100f, SurfaceMaterial.GRAVEL)
        rect("bed-west", 80f, 180f, 50f, 660f, SurfaceMaterial.SOIL)
        rect("bed-east", 1270f, 180f, 60f, 660f, SurfaceMaterial.SOIL)
        rect("wall", 80f, 140f, 1250f, 20f, SurfaceMaterial.MASONRY, features)
        rect("coping", 274f, 284f, 432f, 272f, SurfaceMaterial.PAVING, features)
        rect("pool", 290f, 300f, 400f, 240f, SurfaceMaterial.WATER, features)
        // Steps are separate editable objects rather than simulated shading.
        rect("step-1", 290f, 300f, 80f, 240f, SurfaceMaterial.WATER, features)
        rect("step-2", 290f, 300f, 52f, 240f, SurfaceMaterial.WATER, features)
        rect("step-3", 290f, 300f, 24f, 240f, SurfaceMaterial.WATER, features)
        for (i in 0..7) {
            elements += LineElement(id = "patio-joint-$i", layerId = ground,
                start = Point2D(200f + i * 80f, 180f), end = Point2D(200f + i * 80f, 700f),
                strokeColor = 0xFFB4AA95, strokeWidth = 0.6f, showDimension = false)
        }
        // Simple editable canopy studies; botanical silhouettes are a later milestone.
        listOf(Point2D(105f, 230f), Point2D(105f, 400f), Point2D(105f, 650f),
            Point2D(1300f, 260f), Point2D(1300f, 510f), Point2D(1300f, 740f)).forEachIndexed { i, center ->
            elements += EllipseElement(id = "canopy-$i", layerId = features, centerX = center.x, centerY = center.y,
                radiusX = 42f, radiusY = 46f, strokeColor = 0xFF3D6347, strokeWidth = 1.2f,
                isFilled = true, fillColor = 0xFFBED0A8)
        }
        label("title", "COURTYARD / 01", 80f, 70f, 28f)
        label("subtitle", "Editable landscape study", 80f, 105f)
        label("pool-label", "POOL   20 × 12 ft", 400f, 410f, 18f)
        label("patio-label", "PAVING", 450f, 635f)
        label("lawn-label", "LAWN", 1020f, 425f)
        label("gravel-label", "GRAVEL WALK", 580f, 800f)
        label("caption", "Select any shape to edit its size, material or position.", 160f, 915f)
        return TraceProject(title = title.ifBlank { "Courtyard study" }, backgroundType = BackgroundType.BLANK_PAPER,
            backgroundResourceOrUri = TEMPLATE_KEY, backgroundOpacity = 1f,
            layers = listOf(DrawingLayer(id = ground, name = "Ground surfaces"),
                DrawingLayer(id = features, name = "Pool, wall & planting"), DrawingLayer(id = notes, name = "Labels")),
            activeLayerId = features, elements = elements,
            scaleCalibration = ScaleCalibration(true, 20f, 1f, "ft"))
    }
}
