package com.example.export

import com.example.model.StrokeStyle
import com.example.model.SurfaceMaterial
import com.example.model.design.DesignObject
import com.example.model.design.DesignObjectKind

/** Presentation-only tokens. They never enter authoritative project storage. */
enum class DesignAppearance { TECHNICAL, GRAPHIC, NORTHSTAR }

internal data class ProjectElementStyle(
    val material: SurfaceMaterial?,
    val strokeColor: Long,
    val strokeWidth: Float,
    val strokeStyle: StrokeStyle,
)

internal object ProjectVisualStyle {
    fun forObject(obj: DesignObject, appearance: DesignAppearance): ProjectElementStyle {
        val material = when (obj.kind) {
            DesignObjectKind.POOL, DesignObjectKind.SPA -> SurfaceMaterial.WATER
            DesignObjectKind.PAVING -> SurfaceMaterial.PAVING
            DesignObjectKind.TURF -> SurfaceMaterial.TURF
            DesignObjectKind.GRAVEL -> SurfaceMaterial.GRAVEL
            DesignObjectKind.WALL -> SurfaceMaterial.MASONRY
            DesignObjectKind.SITE_OUTLINE -> null
        }.takeUnless { appearance == DesignAppearance.TECHNICAL }
        val color = when {
            obj.siteTrace != null -> 0xFF596257
            material != null -> material.outline
            obj.kind == DesignObjectKind.WALL -> 0xFF30362F
            else -> 0xFF303733
        }
        val width = when {
            obj.siteTrace != null -> 2.8f
            obj.kind == DesignObjectKind.WALL -> 2.5f
            obj.kind == DesignObjectKind.POOL || obj.kind == DesignObjectKind.SPA -> 2.0f
            else -> 1.6f
        }
        return ProjectElementStyle(
            material = material,
            strokeColor = color,
            strokeWidth = width,
            strokeStyle = if (appearance == DesignAppearance.NORTHSTAR && material != null)
                StrokeStyle.WATERCOLOR_WASH else StrokeStyle.INK,
        )
    }

    fun forCoping(appearance: DesignAppearance) = ProjectElementStyle(
        material = SurfaceMaterial.PAVING.takeUnless { appearance == DesignAppearance.TECHNICAL },
        strokeColor = if (appearance == DesignAppearance.TECHNICAL) 0xFF4D5149 else SurfaceMaterial.PAVING.outline,
        strokeWidth = 1.3f,
        strokeStyle = if (appearance == DesignAppearance.NORTHSTAR)
            StrokeStyle.WATERCOLOR_WASH else StrokeStyle.INK,
    )

    fun renderOrder(kind: DesignObjectKind): Int = when (kind) {
        DesignObjectKind.SITE_OUTLINE -> 0
        DesignObjectKind.PAVING, DesignObjectKind.TURF, DesignObjectKind.GRAVEL -> 1
        DesignObjectKind.POOL, DesignObjectKind.SPA -> 2
        DesignObjectKind.WALL -> 3
    }
}
