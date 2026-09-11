package com.example.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Commands is always visible: never require horizontal scrolling to reach the navigation fallback. */
@Composable
fun WorkspaceHeader(state: WorkspaceState, exportBusy: Boolean, onBack: () -> Unit,
    onCommands: () -> Unit, onUndo: () -> Unit, onRedo: () -> Unit, onExport: () -> Unit) {
    val saveText = when {
        state.loading -> "Opening…"
        state.loadError != null -> "Could not open"
        state.saveError != null -> "Not saved"
        state.isDrawing -> "Drawing outline"
        state.preview != null -> "Previewing edit"
        state.saved -> "Saved on device"
        state.document?.revision == 0L -> "One local draft"
        else -> "Saving…"
    }
    val canExport = state.document?.let { it.objects.isNotEmpty() || it.siteImage != null } == true && state.preview == null && !state.isDrawing && !exportBusy
    BoxWithConstraints(Modifier.fillMaxWidth().statusBarsPadding()) {
        val compact = maxWidth.value / LocalDensity.current.fontScale < 600f
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("workspace-back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to plans")
                }
                if (!compact) Text("Design workspace", Modifier.weight(1f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onCommands, enabled = state.document != null,
                    modifier = Modifier.testTag("workspace-commands")) { Text("Commands", maxLines = 1) }
                if (compact) Spacer(Modifier.weight(1f))
                else {
                    IconButton(onClick = onUndo, enabled = (state.drawingPoints?.isNotEmpty() ?: state.canUndo) && state.preview == null,
                        modifier = Modifier.testTag("workspace-undo")) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo") }
                    IconButton(onClick = onRedo, enabled = state.canRedo && state.preview == null && !state.isDrawing,
                        modifier = Modifier.testTag("workspace-redo")) { Icon(Icons.AutoMirrored.Filled.Redo, "Redo") }
                    Text(saveText, Modifier.testTag("workspace-save-status"), maxLines = 1,
                        style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = onExport, enabled = canExport, modifier = Modifier.testTag("workspace-export")) {
                    Icon(Icons.Default.Share, "Export design")
                }
            }
            if (compact) Row(Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Design workspace", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium)
                Text(saveText, Modifier.testTag("workspace-save-status"), maxLines = 1,
                    style = MaterialTheme.typography.labelMedium)
                // Undo/Redo are also in History on the same command menu. Do not shrink touch targets.
            }
        }
    }
}
