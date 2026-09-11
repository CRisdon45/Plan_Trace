package com.example.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.model.design.DesignObject
import com.example.model.design.DesignObjectKind

/**
 * Fast one-tap object switching without allowing object names to become a second toolbar.
 * Full names remain available to accessibility and in the selected-object context bar.
 */
@Composable
fun WorkspaceObjectNavigator(
    objects: List<DesignObject>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            "${objects.size} objects",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 3.dp).testTag("workspace-object-count"),
        )
        objects.forEachIndexed { index, obj ->
            val selected = obj.id == selectedId
            Surface(
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .size(34.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onSelect(obj.id) }
                    .testTag("workspace-object-$index")
                    .semantics {
                        contentDescription = obj.name + if (obj.locked) ", locked" else ""
                    },
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconFor(obj),
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    if (obj.locked) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).size(9.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun iconFor(obj: DesignObject): ImageVector = when (obj.kind) {
    DesignObjectKind.POOL, DesignObjectKind.SPA -> Icons.Default.Pool
    DesignObjectKind.PAVING -> Icons.Default.Texture
    DesignObjectKind.TURF, DesignObjectKind.GRAVEL -> Icons.Default.Landscape
    DesignObjectKind.WALL -> Icons.Default.Texture
    DesignObjectKind.SITE_OUTLINE -> if (obj.siteTrace?.role?.name == "HOUSE") Icons.Default.Home else Icons.Default.Landscape
}
