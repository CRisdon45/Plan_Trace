package com.example.ui.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.design.TraceRegistration

/** Notices reflect committed facts. Preview must not insert new footer rows and resize the
 * canvas underneath a live pointer; otherwise its size-change handler cancels that gesture. */
@Composable
fun WorkspaceTraceStatus(state: WorkspaceState) {
    val document = state.document ?: return
    val selected = document.objects.firstOrNull { it.id == state.selectedId }
    Column {
        selected?.siteTrace?.let {
            Text(it.notice(document.siteImage), style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp).testTag("workspace-trace-status"))
        }
        if (document.objects.any { it.siteTrace?.registration(document.siteImage) == TraceRegistration.CHANGED }) {
            Text("Source registration changed. Existing traced geometry was kept in place; review alignment.",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp).testTag("workspace-registration-warning"))
        }
    }
}
