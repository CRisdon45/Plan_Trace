package com.example.model

import java.util.UUID

enum class LayerBlendMode {
    NORMAL,
    MULTIPLY, // Architectural drafting ink overlay
    SCREEN
}

data class DrawingLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f, // 0.1f - 1.0f for tracing paper translucency
    val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    val colorTag: Long = 0xFF2563EB // Visual color indicator for CAD layer
)
