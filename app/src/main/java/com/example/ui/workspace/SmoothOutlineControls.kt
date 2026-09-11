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
import com.example.model.design.SmoothPoolDraft

@Composable
fun SmoothOutlineControls(draft: SmoothPoolDraft, model: DesignWorkspaceViewModel) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=12.dp)
        .testTag("smooth-outline-controls"),verticalAlignment=Alignment.CenterVertically) {
        Text("${draft.points.size} shape points",style=MaterialTheme.typography.labelMedium)
        TextButton(onClick=model::backSiteCorner,enabled=draft.points.isNotEmpty(),modifier=Modifier.testTag("smooth-outline-back")) { Text("Back") }
        TextButton(onClick=model::finishSiteOutline,enabled=draft.points.size>=3,modifier=Modifier.testTag("smooth-outline-finish")) { Text("Close pool") }
        TextButton(onClick=model::stopSiteTool,modifier=Modifier.testTag("smooth-outline-cancel")) { Text("Cancel") }
    }
}
