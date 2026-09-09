package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
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
    var exportAsPdf by remember { mutableStateOf(true) }
    var selectedSheetSize by remember { mutableStateOf(PdfSheetSize.TABLOID) }
    var sheetSizeExpanded by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(true) }
    var includeBackground by remember { mutableStateOf(true) }
    var includeTitleBlock by remember { mutableStateOf(true) }
    var includeScaleBar by remember { mutableStateOf(true) }
    var includeDimensions by remember { mutableStateOf(true) }
    var sheetNumber by remember { mutableStateOf("A-101") }
    var sheetTitle by remember { mutableStateOf("CONCEPT TRACE PLAN") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
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
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Architectural PDF & Export",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Project: $projectTitle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Format Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Export Format:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = exportAsPdf,
                            onClick = { exportAsPdf = true },
                            label = { Text("PDF Architectural Sheet") },
                            modifier = Modifier.testTag("export_format_pdf")
                        )
                        FilterChip(
                            selected = !exportAsPdf,
                            onClick = { exportAsPdf = false },
                            label = { Text("PNG High-Res Image") },
                            modifier = Modifier.testTag("export_format_png")
                        )
                    }
                }

                if (exportAsPdf) {
                    HorizontalDivider()

                    // Sheet Size Dropdown
                    ExposedDropdownMenuBox(
                        expanded = sheetSizeExpanded,
                        onExpandedChange = { sheetSizeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSheetSize.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Standard Sheet Size") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sheetSizeExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = sheetSizeExpanded,
                            onDismissRequest = { sheetSizeExpanded = false }
                        ) {
                            PdfSheetSize.values().forEach { size ->
                                DropdownMenuItem(
                                    text = { Text(size.displayName) },
                                    onClick = {
                                        selectedSheetSize = size
                                        sheetSizeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Orientation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isLandscape,
                            onClick = { isLandscape = true },
                            label = { Text("Landscape") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isLandscape,
                            onClick = { isLandscape = false },
                            label = { Text("Portrait") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Sheet Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sheetNumber,
                            onValueChange = { sheetNumber = it },
                            label = { Text("Sheet No.") },
                            modifier = Modifier.weight(0.35f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sheetTitle,
                            onValueChange = { sheetTitle = it },
                            label = { Text("Sheet Title") },
                            modifier = Modifier.weight(0.65f),
                            singleLine = true
                        )
                    }

                    // Toggles for architectural elements
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Architectural Title Block & Border",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Sheet border, project title, date stamp",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Switch(
                                    checked = includeTitleBlock,
                                    onCheckedChange = { includeTitleBlock = it }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Graphic Scale Bar & North Arrow",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Calibrated physical drafting scale",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Switch(
                                    checked = includeScaleBar,
                                    onCheckedChange = { includeScaleBar = it }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Persistent Dimension Callouts",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Include measurements on plan geometry",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Switch(
                                    checked = includeDimensions,
                                    onCheckedChange = { includeDimensions = it }
                                )
                            }
                        }
                    }
                }

                // Underlay toggle
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Include Base Construction Underlay",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (includeBackground) "Full plan with sketch" else "Transparent CAD linework overlay only",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = includeBackground,
                            onCheckedChange = { includeBackground = it },
                            modifier = Modifier.testTag("switch_include_bg")
                        )
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val options = PdfExportOptions(
                                sheetSize = selectedSheetSize,
                                isLandscape = isLandscape,
                                includeBackground = includeBackground,
                                includeTitleBlock = includeTitleBlock,
                                includeScaleBar = includeScaleBar,
                                includeNorthArrow = includeScaleBar,
                                includeDimensions = includeDimensions,
                                sheetTitle = sheetTitle,
                                sheetNumber = sheetNumber
                            )
                            onExport(options, exportAsPdf)
                        },
                        modifier = Modifier.testTag("btn_confirm_export")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (exportAsPdf) "Export & Share PDF" else "Export & Share PNG")
                    }
                }
            }
        }
    }
}
