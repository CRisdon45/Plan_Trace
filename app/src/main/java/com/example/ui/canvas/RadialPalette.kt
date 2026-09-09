package com.example.ui.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DrawingTool
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Context-aware S Pen Radial Menu appearing directly at stylus tip position:
 * - Erase
 * - Draw Line
 * - Select Object
 * - Eyedropper (color sampler)
 * - Pen (freehand drafting)
 * - Watercolor Fill (organic architectural wash)
 */
@Composable
fun RadialPalette(
    visible: Boolean,
    position: Offset,
    activeTool: DrawingTool,
    onSelectTool: (DrawingTool) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.4f),
        exit = fadeOut() + scaleOut(targetScale = 0.4f)
    ) {
        val radiusDp = 74.dp
        val menuDiameterDp = 210.dp

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (position.x - (menuDiameterDp.value / 2f * 2.75f)).roundToInt().coerceAtLeast(16),
                        (position.y - (menuDiameterDp.value / 2f * 2.75f)).roundToInt().coerceAtLeast(16)
                    )
                }
                .size(menuDiameterDp)
                .testTag("spen_radial_menu")
        ) {
            // Backdrop frosted ring
            Surface(
                modifier = Modifier
                    .size(menuDiameterDp)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), CircleShape),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 10.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Central close indicator hub
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    // 6 Context-aware Radial Tool Petals
                    // 1. Erase (top-left, 210°)
                    RadialToolItem(
                        icon = Icons.Default.Delete,
                        label = "Erase",
                        angleDeg = 210.0,
                        radiusDp = radiusDp.value,
                        isSelected = activeTool == DrawingTool.ERASER,
                        testTag = "radial_erase",
                        onClick = {
                            onSelectTool(DrawingTool.ERASER)
                            onDismiss()
                        }
                    )

                    // 2. Draw Line (right, 0°)
                    RadialToolItem(
                        icon = Icons.Default.HorizontalRule,
                        label = "Line",
                        angleDeg = 0.0,
                        radiusDp = radiusDp.value,
                        isSelected = activeTool == DrawingTool.LINE,
                        testTag = "radial_line",
                        onClick = {
                            onSelectTool(DrawingTool.LINE)
                            onDismiss()
                        }
                    )

                    // 3. Select Object (top-right, 330°)
                    RadialToolItem(
                        icon = Icons.Default.NearMe,
                        label = "Select",
                        angleDeg = 315.0,
                        radiusDp = radiusDp.value,
                        isSelected = activeTool == DrawingTool.SELECT,
                        testTag = "radial_select",
                        onClick = {
                            onSelectTool(DrawingTool.SELECT)
                            onDismiss()
                        }
                    )

                    // 4. Eyedropper (bottom-right, 45°)
                    RadialToolItem(
                        icon = Icons.Default.Colorize,
                        label = "Sample",
                        angleDeg = 45.0,
                        radiusDp = radiusDp.value,
                        isSelected = activeTool == DrawingTool.EYEDROPPER,
                        testTag = "radial_eyedropper",
                        onClick = {
                            onSelectTool(DrawingTool.EYEDROPPER)
                            onDismiss()
                        }
                    )

                    // 5. Pen (top, 270°)
                    RadialToolItem(
                        icon = Icons.Default.Create,
                        label = "Pen",
                        angleDeg = 270.0,
                        radiusDp = radiusDp.value,
                        isSelected = activeTool == DrawingTool.PEN,
                        testTag = "radial_pen",
                        onClick = {
                            onSelectTool(DrawingTool.PEN)
                            onDismiss()
                        }
                    )

                    // 6. Watercolor Fill (bottom-left, 135°)
                    RadialToolItem(
                        icon = Icons.Default.FormatColorFill,
                        label = "Wash",
                        angleDeg = 135.0,
                        radiusDp = radiusDp.value,
                        isSelected = activeTool == DrawingTool.WATERCOLOR_FILL,
                        testTag = "radial_fill",
                        onClick = {
                            onSelectTool(DrawingTool.WATERCOLOR_FILL)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RadialToolItem(
    icon: ImageVector,
    label: String,
    angleDeg: Double,
    radiusDp: Float,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val angleRad = Math.toRadians(angleDeg)
    val offsetX = (cos(angleRad) * radiusDp).dp
    val offsetY = (sin(angleRad) * radiusDp).dp

    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .testTag(testTag)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bg)
                .border(
                    1.5.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
