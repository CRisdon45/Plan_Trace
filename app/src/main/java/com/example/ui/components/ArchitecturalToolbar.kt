package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StrokeStyle
import com.example.ui.DrawingTool

val ArchitecturalColors = listOf(
    0xFF0F172A to "Charcoal Ink",
    0xFF1E3A8A to "Blueprint Blue",
    0xFFDC2626 to "Drafting Red",
    0xFF15803D to "Turf Green",
    0xFF38BDF8 to "Pool Azure",
    0xFFD97706 to "Teak Wood",
    0xFF64748B to "Slate Concrete",
    0xFFFFFFFF to "White Correction"
)

val StrokeWidths = listOf(
    1.5f to "Fine (1.5px)",
    3.5f to "Medium (3.5px)",
    7.0f to "Bold (7px)",
    14.0f to "Wash (14px)"
)

@Composable
fun ArchitecturalToolbar(
    modifier: Modifier = Modifier,
    activeTool: DrawingTool,
    strokeColor: Long,
    strokeWidth: Float,
    strokeStyle: StrokeStyle,
    onSelectTool: (DrawingTool) -> Unit,
    onSelectColor: (Long) -> Unit,
    onSelectWidth: (Float) -> Unit,
    onSelectStyle: (StrokeStyle) -> Unit
) {
    var showColorMenu by remember { mutableStateOf(false) }
    var showWidthMenu by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Select Object
            ToolButton(
                icon = Icons.Default.NearMe,
                label = "Select",
                isSelected = activeTool == DrawingTool.SELECT,
                onClick = { onSelectTool(DrawingTool.SELECT) },
                testTag = "tool_select"
            )

            // 2. Freehand Pen (with hold-to-straighten)
            ToolButton(
                icon = Icons.Default.Create,
                label = "Pen",
                isSelected = activeTool == DrawingTool.PEN,
                onClick = { onSelectTool(DrawingTool.PEN) },
                testTag = "tool_pen"
            )

            // 3. Straight Line
            ToolButton(
                icon = Icons.Default.HorizontalRule,
                label = "Line",
                isSelected = activeTool == DrawingTool.LINE,
                onClick = { onSelectTool(DrawingTool.LINE) },
                testTag = "tool_line"
            )

            // 4. Polyline
            ToolButton(
                icon = Icons.Default.Gesture,
                label = "Poly",
                isSelected = activeTool == DrawingTool.POLYLINE,
                onClick = { onSelectTool(DrawingTool.POLYLINE) },
                testTag = "tool_polyline"
            )

            // 5. Rectangle
            ToolButton(
                icon = Icons.Default.Square,
                label = "Rect",
                isSelected = activeTool == DrawingTool.RECTANGLE,
                onClick = { onSelectTool(DrawingTool.RECTANGLE) },
                testTag = "tool_rect"
            )

            // 6. Ellipse / Circle
            ToolButton(
                icon = Icons.Default.Circle,
                label = "Circle",
                isSelected = activeTool == DrawingTool.ELLIPSE,
                onClick = { onSelectTool(DrawingTool.ELLIPSE) },
                testTag = "tool_circle"
            )

            // 7. Dynamic Watercolor Wash / Fill
            ToolButton(
                icon = Icons.Default.FormatColorFill,
                label = "Wash",
                isSelected = activeTool == DrawingTool.WATERCOLOR_FILL,
                onClick = { onSelectTool(DrawingTool.WATERCOLOR_FILL) },
                testTag = "tool_watercolor_fill"
            )

            // 8. Eyedropper Color Sampler
            ToolButton(
                icon = Icons.Default.Colorize,
                label = "Drop",
                isSelected = activeTool == DrawingTool.EYEDROPPER,
                onClick = { onSelectTool(DrawingTool.EYEDROPPER) },
                testTag = "tool_eyedropper"
            )

            // 9. Eraser
            ToolButton(
                icon = Icons.Default.Delete,
                label = "Erase",
                isSelected = activeTool == DrawingTool.ERASER,
                onClick = { onSelectTool(DrawingTool.ERASER) },
                testTag = "tool_eraser"
            )

            // 10. Dimension Ruler
            ToolButton(
                icon = Icons.Default.Calculate,
                label = "Dim",
                isSelected = activeTool == DrawingTool.MEASURE,
                onClick = { onSelectTool(DrawingTool.MEASURE) },
                testTag = "tool_measure"
            )

            // 11. Text Note
            ToolButton(
                icon = Icons.Default.TextFields,
                label = "Note",
                isSelected = activeTool == DrawingTool.TEXT,
                onClick = { onSelectTool(DrawingTool.TEXT) },
                testTag = "tool_text"
            )

            HorizontalDivider(
                modifier = Modifier
                    .width(36.dp)
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Color Selector Swatch Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(strokeColor))
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { showColorMenu = !showColorMenu }
                    .semantics { contentDescription = "Choose color" }
                    .testTag("btn_palette_swatch"),
                contentAlignment = Alignment.Center
            ) {
                if (showColorMenu) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Palette",
                        tint = if (Color(strokeColor).luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Expanded Color Palette Popout
            AnimatedVisibility(
                visible = showColorMenu,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    ArchitecturalColors.forEach { (colorLong, name) ->
                        val isColorSelected = strokeColor == colorLong
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorLong))
                                .semantics { contentDescription = name; selected = isColorSelected }
                                .border(
                                    if (isColorSelected) 2.5.dp else 1.dp,
                                    if (isColorSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape
                                )
                                .clickable {
                                    onSelectColor(colorLong)
                                    showColorMenu = false
                                }
                        )
                    }
                }
            }

            // Stroke Width Selector
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showWidthMenu = !showWidthMenu }
                    .semantics { contentDescription = "Choose stroke width" }
                    .testTag("btn_stroke_width"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((strokeWidth * 1.6f).coerceIn(4f, 22f).dp)
                        .clip(CircleShape)
                        .background(Color(strokeColor))
                )
            }

            // Expanded Stroke Width Popout
            AnimatedVisibility(
                visible = showWidthMenu,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    StrokeWidths.forEach { (widthVal, label) ->
                        val isSelected = strokeWidth == widthVal
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .semantics { contentDescription = label; selected = isSelected }
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    onSelectWidth(widthVal)
                                    showWidthMenu = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((widthVal * 1.5f).coerceIn(3f, 18f).dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }
            }

            // Stroke Style / Wash Mode Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (strokeStyle != StrokeStyle.INK) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { showStyleMenu = !showStyleMenu }
                    .semantics { contentDescription = "Choose stroke style" }
                    .testTag("btn_stroke_style"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (strokeStyle) {
                        StrokeStyle.INK -> "Ink"
                        StrokeStyle.CONSTRUCTION -> "CAD"
                        StrokeStyle.WATERCOLOR_WASH -> "Wash"
                        StrokeStyle.HIGHLIGHTER -> "Hi"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Expanded Stroke Style Popout
            AnimatedVisibility(
                visible = showStyleMenu,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    StrokeStyle.values().forEach { style ->
                        val isStyleSelected = strokeStyle == style
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isStyleSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    onSelectStyle(style)
                                    showStyleMenu = false
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when (style) {
                                    StrokeStyle.INK -> "Ink"
                                    StrokeStyle.CONSTRUCTION -> "CAD"
                                    StrokeStyle.WATERCOLOR_WASH -> "Wash"
                                    StrokeStyle.HIGHLIGHTER -> "Mark"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isStyleSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .semantics { selected = isSelected }
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

private fun Color.luminance(): Float = (red * 0.299f + green * 0.587f + blue * 0.114f)
