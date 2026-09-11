package com.example.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.design.SiteOutlineDraft

/** Temporary construction controls. Closure is completion of a path, not an Apply dialog. */
@Composable
fun SiteOutlineControls(draft: SiteOutlineDraft, orthogonal: Boolean, model: DesignWorkspaceViewModel) {
    Column(Modifier.fillMaxWidth().padding(horizontal=12.dp).testTag("site-outline-controls")) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("${draft.role.label} · ${draft.points.size} corners",style=MaterialTheme.typography.labelLarge,
                modifier=Modifier.testTag("site-outline-count"))
            FilterChip(selected=orthogonal,onClick=model::toggleSiteOrthogonal,label={Text("Right angles")},
                modifier=Modifier.testTag("site-outline-ortho"))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick=model::finishSiteOutline,enabled=draft.points.size>=3,modifier=Modifier.testTag("site-outline-finish")) { Text("Close outline") }
            TextButton(onClick=model::backSiteCorner,enabled=draft.points.isNotEmpty(),modifier=Modifier.testTag("site-outline-back")) { Text("Back a corner") }
            TextButton(onClick=model::stopSiteTool,modifier=Modifier.testTag("site-outline-cancel")) { Text("Cancel") }
        }
        Text("Tap or drag the pen to place corners. Tap the first corner to close. Fingers navigate unless Touch edit is on.",
            style=MaterialTheme.typography.labelSmall,maxLines=2)
    }
}
