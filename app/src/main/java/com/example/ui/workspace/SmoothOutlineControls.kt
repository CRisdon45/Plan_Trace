package com.example.ui.workspace

import androidx.compose.runtime.Composable
import com.example.model.design.SmoothPoolDraft

@Composable
fun SmoothOutlineControls(draft: SmoothPoolDraft, model: DesignWorkspaceViewModel) {
    val count = draft.points.size
    OutlineControlStrip(
        label = "Smooth pool",
        countText = count.toString() + if (count == 1) " point" else " points",
        canClose = count >= 3,
        canBack = count > 0,
        closeLabel = "Close",
        tag = "smooth-outline",
        onClose = model::finishSiteOutline,
        onBack = model::backSiteCorner,
        onCancel = model::stopSiteTool,
    )
}
