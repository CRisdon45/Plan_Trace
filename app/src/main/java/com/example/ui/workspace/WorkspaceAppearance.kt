package com.example.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.export.DesignAppearance

internal object WorkspaceAppearancePreference {
    const val KEY = "appearance"

    fun decode(value: String?): DesignAppearance =
        DesignAppearance.values().firstOrNull { it.name == value } ?: DesignAppearance.NORTHSTAR
}

internal val DesignAppearance.workspaceLabel: String
    get() = when (this) {
        DesignAppearance.TECHNICAL -> "Technical"
        DesignAppearance.GRAPHIC -> "Graphic"
        DesignAppearance.NORTHSTAR -> "Northstar"
    }

private val DesignAppearance.workspaceDescription: String
    get() = when (this) {
        DesignAppearance.TECHNICAL -> "Precise linework without material fill"
        DesignAppearance.GRAPHIC -> "Clean flat material color"
        DesignAppearance.NORTHSTAR -> "Ink with restrained tonal material depth"
    }

/** A view choice, never a project edit. Selecting a row commits immediately. */
@Composable
internal fun WorkspaceAppearanceDialog(
    selected: DesignAppearance,
    onSelect: (DesignAppearance) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Drawing appearance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DesignAppearance.values().forEach { appearance ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = appearance == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(appearance) },
                            )
                            .testTag("workspace-appearance-${appearance.name.lowercase()}")
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = appearance == selected, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(appearance.workspaceLabel, style = MaterialTheme.typography.bodyLarge)
                            Text(appearance.workspaceDescription, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("workspace-appearance-close")) {
                Text("Close")
            }
        },
        modifier = Modifier.testTag("workspace-appearance-dialog"),
    )
}
