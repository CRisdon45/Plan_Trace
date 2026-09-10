package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BackgroundType
import com.example.model.TraceProject

@Composable
fun TopBar(
    project: TraceProject,
    canUndo: Boolean,
    canRedo: Boolean,
    stylusOnlyMode: Boolean,
    isCalibratingScale: Boolean,
    showDimensions: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleStylusMode: () -> Unit,
    onToggleShowDimensions: () -> Unit,
    onStartCalibration: () -> Unit,
    onCancelCalibration: () -> Unit,
    onOpenLayers: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenProjects: () -> Unit,
    onSelectSample: (String) -> Unit,
    onClearCanvas: () -> Unit,
    onImportFile: () -> Unit,
    onPrevPdfPage: () -> Unit,
    onNextPdfPage: () -> Unit,
    onEditTitle: () -> Unit,
    onFitDrawing: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    @Composable fun ProjectHeading(modifier: Modifier = Modifier) {
// Left: Project title & project manager button
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onOpenProjects,
                    modifier = Modifier.testTag("btn_projects")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Projects",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onEditTitle)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit title",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                // If PDF has multiple pages, show page switcher
                if (project.backgroundType == BackgroundType.PDF_URI && project.pdfTotalPages > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 4.dp)
                    ) {
                        IconButton(
                            onClick = onPrevPdfPage,
                            enabled = project.pdfPageNumber > 0,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.NavigateBefore, contentDescription = "Prev Page", modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "${project.pdfPageNumber + 1} / ${project.pdfTotalPages}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        IconButton(
                            onClick = onNextPdfPage,
                            enabled = project.pdfPageNumber < project.pdfTotalPages - 1,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.NavigateNext, contentDescription = "Next Page", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
    }
    @Composable fun EditingActions() {
// Center: Undo / Redo & Scale Calibration Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Undo
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.testTag("btn_undo")
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                }

                // Redo
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.testTag("btn_redo")
                ) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Scale Calibration Pill
                if (isCalibratingScale) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable(onClick = onCancelCalibration)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Drawing Scale... Tap to Cancel",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (project.scaleCalibration.isCalibrated) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(onClick = onStartCalibration)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("scale_calibration_pill"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Scale",
                            modifier = Modifier.size(16.dp),
                            tint = if (project.scaleCalibration.isCalibrated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (project.scaleCalibration.isCalibrated) {
                                "Scale: ${project.scaleCalibration.realWorldUnits.toString().removeSuffix(".0")} ${project.scaleCalibration.unit} (${project.scaleCalibration.pixelDistance.toInt()}px)"
                            } else {
                                "Set Scale ⌖"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (project.scaleCalibration.isCalibrated) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (project.scaleCalibration.isCalibrated) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (showDimensions) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable(onClick = onToggleShowDimensions)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("btn_toggle_dimensions"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showDimensions) "Dims: ON" else "Dims: OFF",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (showDimensions) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
    }
    @Composable fun FileActions() {
            // Right: Layers, Stylus Mode, Export, Overflow Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Layers Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onOpenLayers)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("btn_layers"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Layers",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Layers (${project.layers.size})",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // S Pen / Touch mode toggle
                IconButton(
                    onClick = onToggleStylusMode,
                    modifier = Modifier.testTag("btn_stylus_mode")
                ) {
                    Icon(
                        imageVector = if (stylusOnlyMode) Icons.Default.Create else Icons.Default.TouchApp,
                        contentDescription = if (stylusOnlyMode) "S Pen Only (Palm Rejection Active)" else "Touch & Stylus Mode",
                        tint = if (stylusOnlyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Export Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onOpenExport)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("btn_export"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Export",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Overflow Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("btn_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Import Plan (PDF / Image)") },
                            onClick = {
                                showMenu = false
                                onImportFile()
                            },
                            leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Sample: Curved Pool & Patio") },
                            onClick = {
                                showMenu = false
                                onSelectSample("sample_pool")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sample: CAD Deck & Structure") },
                            onClick = {
                                showMenu = false
                                onSelectSample("sample_deck")
                            }
                        )
                        DropdownMenuItem(text = { Text("Fit drawing") }, onClick = { showMenu = false; onFitDrawing() })
                        DropdownMenuItem(
                            text = { Text("Clear All Linework") },
                            onClick = {
                                showMenu = false
                                onClearCanvas()
                            },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
                        )
                    }
                }
            }
    }
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), tonalElevation = 4.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            val compact = maxWidth < 1000.dp
            val narrow = maxWidth < 600.dp
            Column {
                if (compact) {
                    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProjectHeading(Modifier.weight(1f))
                        if (!narrow) FileActions()
                    }
                    Row(Modifier.fillMaxWidth().height(56.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                        EditingActions()
                    }
                    if (narrow) Row(Modifier.fillMaxWidth().height(56.dp).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) { FileActions() }
                } else {
                    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProjectHeading(Modifier.weight(1f))
                        EditingActions()
                        FileActions()
                    }
                }
            }
        }
    }
}
