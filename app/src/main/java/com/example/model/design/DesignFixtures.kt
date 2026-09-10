package com.example.model.design

/** Original synthetic geometry, not copies or redactions of client plans. Small kernel cases only. */
object DesignFixtures {
    fun rectangle(): DesignBoundary = boundary(listOf(DesignPoint(0.0, 0.0), DesignPoint(10.0, 0.0),
        DesignPoint(10.0, 6.0), DesignPoint(0.0, 6.0)))
    fun circle(radius: Double = 3.0): DesignBoundary = boundary(
        listOf(DesignPoint(-radius, 0.0), DesignPoint(radius, 0.0)), listOf(1.0, 1.0))
    fun organic(): DesignBoundary = boundary(listOf(DesignPoint(0.0, 0.0), DesignPoint(10.0, 0.0),
        DesignPoint(10.0, 6.0), DesignPoint(6.0, 6.0), DesignPoint(4.0, 6.0), DesignPoint(0.0, 6.0)),
        listOf(0.0, 1.0, 0.0, -0.4, 0.0, 1.0))
    fun document(): ProjectDesign = ProjectDesign("synthetic-geometry-seam", listOf(
        DesignObject("pool", "Curved pool study", DesignObjectKind.POOL, organic()),
        DesignObject("patio", "Independent patio", DesignObjectKind.PAVING, rectangle().translated(0.0, -10.0))
    ))
    private fun boundary(points: List<DesignPoint>, bulges: List<Double> = List(points.size) { 0.0 }) =
        DesignBoundary(points.mapIndexed { i, point -> BoundaryNode("vertex-$i", point, "edge-$i", bulges[i]) })
}
