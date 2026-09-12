package com.example.model

/** Explicit Graphic surface choices. Never inferred from legacy colors or layer names. */
enum class SurfaceMaterial(val label: String, val fill: Long, val outline: Long) {
    WATER("Water", 0xFFD6E9DE, 0xFF294743),
    PAVING("Paving", 0xFFE9E0CD, 0xFF55594D),
    TURF("Turf", 0xFFDDE8A6, 0xFF656949),
    GRAVEL("Gravel", 0xFFD6D1BF, 0xFF88816C),
    SOIL("Planting bed", 0xFFD6C2A3, 0xFF656949),
    MASONRY("Masonry", 0xFFDED5C5, 0xFF30362F)
}

fun VectorElement.supportsSurface(): Boolean = when (this) {
    is RectangleElement, is EllipseElement -> true
    is FreehandPath -> isClosed && points.size >= 3
    is PolylineElement -> isClosed && points.size >= 3
    else -> false
}

fun VectorElement.withMaterial(value: SurfaceMaterial?): VectorElement = when (this) {
    is RectangleElement -> copy(material = value)
    is EllipseElement -> copy(material = value)
    is FreehandPath -> copy(material = value)
    is PolylineElement -> copy(material = value)
    is LineElement -> copy(material = value)
    is TextElement -> copy(material = value)
    is DimensionMarkup -> copy(material = value)
}
