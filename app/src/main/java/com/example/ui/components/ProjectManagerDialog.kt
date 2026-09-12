package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.TraceProject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectManagerDialog(
    currentProject: TraceProject,
    projects: List<TraceProject>,
    onDismiss: () -> Unit,
    onSelectProject: (TraceProject) -> Unit,
    onCreateNewProject: (title: String, sampleKey: String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onImportFile: () -> Unit
) {
    var isCreating by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf("sample_pool") }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(440.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Plan Projects",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (isCreating) {
                    // Create Project Form
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Project Title") },
                            placeholder = { Text("e.g. Hillside Pool & Deck Plan") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Choose Base Plan Template:", style = MaterialTheme.typography.labelMedium)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        if (selectedTemplate == "sample_pool") 2.dp else 1.dp,
                                        if (selectedTemplate == "sample_pool") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedTemplate = "sample_pool" },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedTemplate == "sample_pool") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Curved Pool", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Grading & Coping", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        if (selectedTemplate == "sample_deck") 2.dp else 1.dp,
                                        if (selectedTemplate == "sample_deck") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedTemplate = "sample_deck" },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedTemplate == "sample_deck") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("CAD Deck", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Timber Framing", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isCreating = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                onCreateNewProject(newTitle, selectedTemplate)
                                isCreating = false
                            }) {
                                Text("Create")
                            }
                        }
                    }
                } else {
                    // Actions: New Project & Import Plan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isCreating = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Sketch")
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onImportFile()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Plan")
                        }
                    }

                    OutlinedButton(onClick = {
                        onCreateNewProject("Courtyard study", com.example.model.LandscapeExample.TEMPLATE_KEY)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open editable landscape example")
                    }

                    HorizontalDivider()

                    // Existing Projects List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(projects) { p ->
                            val isCurrent = p.id == currentProject.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        if (isCurrent) 1.5.dp else 1.dp,
                                        if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        onSelectProject(p)
                                        onDismiss()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = p.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            if (isCurrent) {
                                                Text(
                                                    text = "Active",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${p.elements.size} vector objects • ${dateFormat.format(Date(p.updatedAt))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    if (projects.size > 1) {
                                        IconButton(
                                            onClick = { onDeleteProject(p.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
