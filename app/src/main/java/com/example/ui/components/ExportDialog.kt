package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.export.PdfExportOptions
import com.example.export.PdfSheetSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    projectTitle: String,
    onDismiss: () -> Unit,
    onExport: (options: PdfExportOptions, asPdf: Boolean) -> Unit
) {
    var exportAsPdf by rememberSaveable { mutableStateOf(true) }
    var selectedSheetSize by rememberSaveable { mutableStateOf(PdfSheetSize.TABLOID) }
    var sheetSizeExpanded by rememberSaveable { mutableStateOf(false) }
    var isLandscape by rememberSaveable { mutableStateOf(true) }
    var includeBackground by rememberSaveable { mutableStateOf(true) }
    var includeTitleBlock by rememberSaveable { mutableStateOf(true) }
    var includeScaleBar by rememberSaveable { mutableStateOf(true) }
    var includeDimensions by rememberSaveable { mutableStateOf(true) }
    var sheetNumber by rememberSaveable { mutableStateOf("A-101") }
    var sheetTitle by rememberSaveable { mutableStateOf("CONCEPT DESIGN PLAN") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(440.dp).clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text("Export design",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close export")
                    }
                }
                Text("Project: $projectTitle", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = exportAsPdf,
                        onClick = { exportAsPdf = true },
                        label = { Text("PDF sheet") },
                        modifier = Modifier.weight(1f).testTag("export_format_pdf")
                    )
                    FilterChip(
                        selected = !exportAsPdf,
                        onClick = { exportAsPdf = false },
                        label = { Text("PNG image") },
                        modifier = Modifier.weight(1f).testTag("export_format_png")
                    )
                }

                // Shared artwork controls must not live inside the PDF-only section.
                // One state follows format changes and feeds the existing export callback.
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        ExportToggle(
                            label = "Include measurements",
                            description = "Automatic measurements on plan geometry",
                            checked = includeDimensions,
                            onCheckedChange = { includeDimensions = it },
                            tag = "switch_include_dimensions"
                        )
                        ExportToggle(
                            label = "Include source underlay",
                            description = "Show the imported plan or image beneath the design",
                            checked = includeBackground,
                            onCheckedChange = { includeBackground = it },
                            tag = "switch_include_bg"
                        )
                    }
                }

                if (exportAsPdf) {
                    HorizontalDivider()
                    ExposedDropdownMenuBox(
                        expanded = sheetSizeExpanded,
                        onExpandedChange = { sheetSizeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSheetSize.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sheet size") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sheetSizeExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = sheetSizeExpanded,
                            onDismissRequest = { sheetSizeExpanded = false }
                        ) {
                            PdfSheetSize.values().forEach { size ->
                                DropdownMenuItem(
                                    text = { Text(size.displayName) },
                                    onClick = { selectedSheetSize = size; sheetSizeExpanded = false }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = isLandscape, onClick = { isLandscape = true },
                            label = { Text("Landscape") }, modifier = Modifier.weight(1f))
                        FilterChip(selected = !isLandscape, onClick = { isLandscape = false },
                            label = { Text("Portrait") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sheetNumber, onValueChange = { sheetNumber = it },
                            label = { Text("Sheet number") },
                            modifier = Modifier.weight(0.35f), singleLine = true
                        )
                        OutlinedTextField(
                            value = sheetTitle, onValueChange = { sheetTitle = it },
                            label = { Text("Sheet title") },
                            modifier = Modifier.weight(0.65f), singleLine = true
                        )
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            ExportToggle(
                                label = "Include title block",
                                description = "Project, sheet details and date",
                                checked = includeTitleBlock,
                                onCheckedChange = { includeTitleBlock = it },
                                tag = "switch_include_title"
                            )
                            ExportToggle(
                                label = "Graphic scale and north arrow",
                                description = "Shown within the PDF title block",
                                checked = includeScaleBar,
                                onCheckedChange = { includeScaleBar = it },
                                tag = "switch_include_scale"
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("btn_cancel_export")) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onExport(PdfExportOptions(
                                sheetSize = selectedSheetSize,
                                isLandscape = isLandscape,
                                includeBackground = includeBackground,
                                includeTitleBlock = includeTitleBlock,
                                includeScaleBar = includeScaleBar,
                                includeNorthArrow = includeScaleBar,
                                includeDimensions = includeDimensions,
                                sheetTitle = sheetTitle,
                                sheetNumber = sheetNumber
                            ), exportAsPdf)
                        },
                        modifier = Modifier.testTag("btn_confirm_export")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (exportAsPdf) "Share PDF" else "Share PNG")
                    }
                }
            }
        }
    }
}

/** A single labeled accessibility/touch target; the visual switch does not toggle twice. */
@Composable
private fun ExportToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag(tag)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(description, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
