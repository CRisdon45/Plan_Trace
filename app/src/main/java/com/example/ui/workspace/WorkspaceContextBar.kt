package com.example.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.design.DesignObject
import java.util.Locale

/** Compact persistent context. Instructions and development notes do not own canvas height. */
@Composable
fun WorkspaceContextBar(selected: DesignObject?, message: String?, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth().testTag("workspace-context"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(selected?.let {
                String.format(Locale.US, "%s · %.2f ft perimeter", it.name, it.boundary.perimeterMetres / 0.3048)
            } ?: "No object selected", style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).testTag("workspace-selection"))
            message?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1.15f).testTag("workspace-message"))
            }
        }
    }
}
