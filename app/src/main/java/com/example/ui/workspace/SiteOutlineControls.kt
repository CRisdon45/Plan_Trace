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
    OutlineControls(draft.role.label,draft.points.size,orthogonal,model,"site-outline")
}

@Composable
fun ProposedOutlineControls(draft: com.example.model.design.ProposedOutlineDraft, orthogonal: Boolean, model: DesignWorkspaceViewModel) {
    OutlineControls(draft.label,draft.points.size,orthogonal,model,"proposed-outline")
}

@Composable
private fun OutlineControls(label: String, count: Int, orthogonal: Boolean, model: DesignWorkspaceViewModel, tag: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal=12.dp).testTag("$tag-controls")) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("$label · $count corners",style=MaterialTheme.typography.labelLarge,
                modifier=Modifier.testTag("$tag-count"))
            FilterChip(selected=orthogonal,onClick=model::toggleSiteOrthogonal,label={Text("Right angles")},
                modifier=Modifier.testTag("$tag-ortho"))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick=model::finishSiteOutline,enabled=count>=3,modifier=Modifier.testTag("$tag-finish")) { Text("Close outline") }
            TextButton(onClick=model::backSiteCorner,enabled=count>0,modifier=Modifier.testTag("$tag-back")) { Text("Back a corner") }
            TextButton(onClick=model::stopSiteTool,modifier=Modifier.testTag("$tag-cancel")) { Text("Cancel") }
        }
        Text("Tap or drag the pen to place corners. Tap the first corner to close. Fingers navigate unless Touch edit is on.",
            style=MaterialTheme.typography.labelSmall,maxLines=2)
    }
}
