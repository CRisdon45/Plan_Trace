package com.example.ui.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    OutlineControlStrip(
        label = label,
        countText = count.toString() + if (count == 1) " corner" else " corners",
        canClose = count >= 3,
        canBack = count > 0,
        closeLabel = "Close",
        tag = tag,
        modeLabel = "Right angles",
        modeSelected = orthogonal,
        onMode = model::toggleSiteOrthogonal,
        onClose = model::finishSiteOutline,
        onBack = model::backSiteCorner,
        onCancel = model::stopSiteTool,
    )
}

/** One compact command strip shared by straight and smooth pen authoring. */
@Composable
internal fun OutlineControlStrip(
    label: String,
    countText: String,
    canClose: Boolean,
    canBack: Boolean,
    closeLabel: String,
    tag: String,
    modeLabel: String? = null,
    modeSelected: Boolean = false,
    onMode: (() -> Unit)? = null,
    onClose: () -> Unit,
    onBack: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp).testTag(tag + "-controls"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label + " · " + countText, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.testTag(tag + "-count"))
        if (modeLabel != null && onMode != null) {
            FilterChip(
                selected = modeSelected,
                onClick = onMode,
                label = { Text(modeLabel) },
                modifier = Modifier.semantics {
                    contentDescription = modeLabel + ", " + if (modeSelected) "on" else "off"
                }.testTag(tag + "-ortho"),
            )
        }
        Button(
            onClick = onClose,
            enabled = canClose,
            modifier = Modifier.semantics { contentDescription = "Close outline" }
                .testTag(tag + "-finish"),
        ) { Text(closeLabel) }
        TextButton(
            onClick = onBack,
            enabled = canBack,
            modifier = Modifier.semantics { contentDescription = "Back one point" }
                .testTag(tag + "-back"),
        ) { Text("Back") }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.semantics { contentDescription = "Cancel drawing" }
                .testTag(tag + "-cancel"),
        ) { Text("Cancel") }
    }
}
