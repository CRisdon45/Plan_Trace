package com.example.ui.components

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import com.example.model.DrawingLayer
import com.example.model.LayerBlendMode
import com.example.model.TraceProject

val CadLayerColors = listOf(
    0xFF2563EB to "Blue",
    0xFF059669 to "Green",
    0xFFD97706 to "Amber",
    0xFFDC2626 to "Red",
    0xFF7C3AED to "Purple",
    0xFF0F172A to "Charcoal"
)

@Composable
fun LayersDialog(
    project: TraceProject,
    onDismiss: () -> Unit,
    onSetActiveLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onSetOpacity: (String, Float) -> Unit,
    onAddLayer: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onMergeDown: (String) -> Unit,
    onClearLayer: (String) -> Unit,
    onSetBlendMode: (String, LayerBlendMode) -> Unit,
    onSetColorTag: (String, Long) -> Unit,
    onAddPresetLayers: (List<String>) -> Unit,
    onSetBackgroundOpacity: (Float) -> Unit,
    onToggleBackgroundLock: () -> Unit
) {
    var newLayerName by remember { mutableStateOf("") }
    var showAddRow by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Architectural Layers",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${project.layers.size} layers • ${project.elements.size} vector objects",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Base Plan / PDF Underlay Control
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Base Construction Plan / PDF Underlay",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            IconButton(
                                onClick = onToggleBackgroundLock,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (project.isBackgroundLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock Underlay",
                                    tint = if (project.isBackgroundLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Opacity,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Opacity: ${(project.backgroundOpacity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Slider(
                                value = project.backgroundOpacity,
                                onValueChange = onSetBackgroundOpacity,
                                valueRange = 0.05f..1.0f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Drawing Layers List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(project.layers) { index, layer ->
                        LayerCardRow(
                            layer = layer,
                            isActive = layer.id == project.activeLayerId,
                            isFirst = index == 0,
                            isLast = index == project.layers.size - 1,
                            elementCount = project.elements.count { it.layerId == layer.id },
                            onSelect = { onSetActiveLayer(layer.id) },
                            onToggleVisibility = { onToggleVisibility(layer.id) },
                            onToggleLock = { onToggleLock(layer.id) },
                            onSetOpacity = { onSetOpacity(layer.id, it) },
                            onDelete = { onDeleteLayer(layer.id) },
                            onMoveUp = { onMoveUp(layer.id) },
                            onMoveDown = { onMoveDown(layer.id) },
                            onDuplicate = { onDuplicateLayer(layer.id) },
                            onMergeDown = { onMergeDown(layer.id) },
                            onClear = { onClearLayer(layer.id) },
                            onSetBlendMode = { onSetBlendMode(layer.id, it) },
                            onSetColorTag = { onSetColorTag(layer.id, it) }
                        )
                    }
                }

                // Add Layer / Preset Row
                if (showAddRow) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newLayerName,
                            onValueChange = { newLayerName = it },
                            placeholder = { Text("e.g. Hardscape & Pavers") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                onAddLayer(newLayerName)
                                newLayerName = ""
                                showAddRow = false
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Add",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddRow = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Layer")
                        }

                        TextButton(
                            onClick = {
                                onAddPresetLayers(listOf("Hardscaping & Pavers", "Water Features & Pool", "Planting & Turf", "Dimensions & Callouts"))
                            }
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Architectural Set")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerCardRow(
    layer: DrawingLayer,
    isActive: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    elementCount: Int,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onSetOpacity: (Float) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onMergeDown: () -> Unit,
    onClear: () -> Unit,
    onSetBlendMode: (LayerBlendMode) -> Unit,
    onSetColorTag: (Long) -> Unit
) {
    var showActions by remember(layer.id) { mutableStateOf(false) }
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Card(
        modifier = Modifier.fillMaxWidth().border(if (isActive) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp)).clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(Color(layer.colorTag)))
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(layer.name, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium)
                    Text("$elementCount objects" + if (isActive) " - Active" else "", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onToggleVisibility) {
                    Icon(if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle visibility: ${layer.name}")
                }
                IconButton(onClick = onToggleLock) {
                    Icon(if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Toggle lock: ${layer.name}")
                }
                Box {
                    IconButton(onClick = { showActions = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Layer actions: ${layer.name}")
                    }
                    DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                        DropdownMenuItem(text = { Text("Move up") }, enabled = !isFirst, onClick = { showActions = false; onMoveUp() })
                        DropdownMenuItem(text = { Text("Move down") }, enabled = !isLast, onClick = { showActions = false; onMoveDown() })
                        DropdownMenuItem(text = { Text("Duplicate layer") }, onClick = { showActions = false; onDuplicate() })
                        DropdownMenuItem(text = { Text("Merge into layer below") }, enabled = !isLast && !layer.isLocked,
                            onClick = { showActions = false; onMergeDown() })
                        DropdownMenuItem(text = { Text("Clear layer") }, enabled = !layer.isLocked && layer.isVisible && elementCount > 0,
                            onClick = { showActions = false; onClear() })
                        DropdownMenuItem(text = { Text("Delete layer", color = MaterialTheme.colorScheme.error) },
                            onClick = { showActions = false; onDelete() })
                    }
                }
            }
            if (isActive) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Opacity ${(layer.opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    Slider(value = layer.opacity, onValueChange = onSetOpacity, valueRange = 0.1f..1f,
                        modifier = Modifier.weight(1f).padding(start = 12.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = layer.blendMode == LayerBlendMode.NORMAL, onClick = { onSetBlendMode(LayerBlendMode.NORMAL) }, label = { Text("Normal") })
                    FilterChip(selected = layer.blendMode == LayerBlendMode.MULTIPLY, onClick = { onSetBlendMode(LayerBlendMode.MULTIPLY) }, label = { Text("Multiply") })
                }
            }
        }
    }
}
