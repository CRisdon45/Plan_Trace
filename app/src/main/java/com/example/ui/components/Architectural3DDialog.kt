package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.Architectural3DEngine
import com.example.engine.Render3DMode
import com.example.model.Point2D
import com.example.model.TraceProject
import kotlin.math.max
import kotlin.math.min

@Composable
fun Architectural3DDialog(
    project: TraceProject,
    onDismiss: () -> Unit
) {
    // 3D Camera Orbit & Zoom State
    var yawDeg by remember { mutableFloatStateOf(45f) }
    var pitchDeg by remember { mutableFloatStateOf(35f) }
    var zoomScale by remember { mutableFloatStateOf(0.85f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Render configuration
    var renderMode by remember { mutableStateOf(Render3DMode.SOLID_SHADED) }
    var heightMultiplier by remember { mutableFloatStateOf(1.0f) }
    var showGroundGrid by remember { mutableStateOf(true) }

    // Layer active filter for 3D
    var disabledLayerIds by remember { mutableStateOf(setOf<String>()) }

    // Compute centroid of project elements for camera pivot
    val viewCenter = remember(project.elements) {
        if (project.elements.isEmpty()) {
            Point2D(1000f, 800f)
        } else {
            val boxes = project.elements.map { it.boundingBox() }
            val minX = boxes.minOf { it.left }
            val maxX = boxes.maxOf { it.right }
            val minY = boxes.minOf { it.top }
            val maxY = boxes.maxOf { it.bottom }
            Point2D((minX + maxX) / 2f, (minY + maxY) / 2f)
        }
    }

    // Filter project with temporary 3D layer visibility
    val active3DProject = remember(project, disabledLayerIds) {
        project.copy(
            layers = project.layers.map { layer ->
                if (layer.id in disabledLayerIds) layer.copy(isVisible = false) else layer
            }
        )
    }

    // Build polygonal 3D model
    val faces = remember(active3DProject, heightMultiplier) {
        Architectural3DEngine.build3DFaces(active3DProject, heightMultiplier)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dialog_3d_view"),
            color = Color(0xFF0F172A) // Sleek CAD dark slate backdrop
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Interactive 3D Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                // If zooming
                                if (zoom != 1.0f) {
                                    zoomScale = (zoomScale * zoom).coerceIn(0.15f, 6.0f)
                                }
                                // If dragging with single or multiple pointers, orbit angles
                                yawDeg = (yawDeg + pan.x * 0.45f) % 360f
                                if (yawDeg < 0f) yawDeg += 360f
                                pitchDeg = (pitchDeg - pan.y * 0.35f).coerceIn(10f, 88f)
                            }
                        }
                ) {
                    drawIntoCanvas { composeCanvas ->
                        val native = composeCanvas.nativeCanvas
                        Architectural3DEngine.render3DScene(
                            canvas = native,
                            faces = faces,
                            viewCenter = viewCenter,
                            screenSize = Offset(size.width, size.height),
                            yawDeg = yawDeg,
                            pitchDeg = pitchDeg,
                            zoom = zoomScale,
                            panOffset = panOffset,
                            mode = renderMode,
                            showGrid = showGroundGrid
                        )
                    }
                }

                // Top Floating Control Glass Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.92f),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = "3D",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "3D Architectural Projection",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "${faces.size} polygons • Drag to Orbit • Pinch to Zoom",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Preset Perspective Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = renderMode == Render3DMode.SOLID_SHADED,
                                onClick = { renderMode = Render3DMode.SOLID_SHADED },
                                label = { Text("Shaded", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White,
                                    labelColor = Color(0xFFCBD5E1)
                                )
                            )
                            FilterChip(
                                selected = renderMode == Render3DMode.ARCHITECTURAL_CLAY,
                                onClick = { renderMode = Render3DMode.ARCHITECTURAL_CLAY },
                                label = { Text("Clay", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF64748B),
                                    selectedLabelColor = Color.White,
                                    labelColor = Color(0xFFCBD5E1)
                                )
                            )
                            FilterChip(
                                selected = renderMode == Render3DMode.WIREFRAME,
                                onClick = { renderMode = Render3DMode.WIREFRAME },
                                label = { Text("Wire", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF475569),
                                    selectedLabelColor = Color.White,
                                    labelColor = Color(0xFFCBD5E1)
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close 3D View",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Bottom Floating Control Bar (Presets, Height Multiplier, Layer Toggles)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.92f),
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Camera presets row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Camera Views:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF94A3B8)
                                )
                                // SW Isometric
                                FilterChip(
                                    selected = yawDeg == 45f && pitchDeg == 35f,
                                    onClick = { yawDeg = 45f; pitchDeg = 35f; panOffset = Offset.Zero },
                                    label = { Text("Iso SW", fontSize = 11.sp) }
                                )
                                // SE Isometric
                                FilterChip(
                                    selected = yawDeg == 135f && pitchDeg == 35f,
                                    onClick = { yawDeg = 135f; pitchDeg = 35f; panOffset = Offset.Zero },
                                    label = { Text("Iso SE", fontSize = 11.sp) }
                                )
                                // Top Down (Plan)
                                FilterChip(
                                    selected = pitchDeg >= 85f,
                                    onClick = { yawDeg = 0f; pitchDeg = 88f; panOffset = Offset.Zero },
                                    label = { Text("Top Plan", fontSize = 11.sp) }
                                )
                                // Front Elevation
                                FilterChip(
                                    selected = yawDeg == 0f && pitchDeg == 15f,
                                    onClick = { yawDeg = 0f; pitchDeg = 15f; panOffset = Offset.Zero },
                                    label = { Text("Elevation", fontSize = 11.sp) }
                                )
                            }

                            // Height Multiplier & Grid
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = showGroundGrid,
                                    onClick = { showGroundGrid = !showGroundGrid },
                                    label = { Text("Grid", fontSize = 11.sp) }
                                )

                                FilterChip(
                                    selected = heightMultiplier == 0.5f,
                                    onClick = { heightMultiplier = 0.5f },
                                    label = { Text("0.5x", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = heightMultiplier == 1.0f,
                                    onClick = { heightMultiplier = 1.0f },
                                    label = { Text("1.0x", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = heightMultiplier == 2.0f,
                                    onClick = { heightMultiplier = 2.0f },
                                    label = { Text("2.0x", fontSize = 11.sp) }
                                )
                            }
                        }

                        // Layer visibility chips row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "3D Layers:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            for (layer in project.layers) {
                                val isEnabled = layer.id !in disabledLayerIds
                                FilterChip(
                                    selected = isEnabled,
                                    onClick = {
                                        disabledLayerIds = if (isEnabled) {
                                            disabledLayerIds + layer.id
                                        } else {
                                            disabledLayerIds - layer.id
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    label = { Text(layer.name, fontSize = 10.sp, maxLines = 1) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
